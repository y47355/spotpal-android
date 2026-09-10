package com.spotpal.core.network

import com.spotpal.core.model.WsMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket 连接管理器（详细设计 §2.2）。
 *
 * 对齐服务端 ws 网关契约：
 * - 信封 {type, seq, payload, ts}；ack 帧回传 {type:"ack", ack:seq}
 * - 断线按 last_seq 补拉（服务端 resume 帧）；缺口 >500 降级 REST /v1/ws/messages
 * - 心跳 30s（OkHttp pingInterval 底层保活）
 * - 重连退避：1s → 2s → 4s → ... 上限 60s，±20% 抖动
 *
 * 可靠性红线：seq 先落盘（onSeqPersisted 回调，core/data Room 实现）再分发；
 * 消费端按 type 过阅，event 幂等由 seq 保证。
 */
@Singleton
class WsClient @Inject constructor() {

    /** 连接状态（UI 显示「已离线」横幅）。 */
    sealed interface WsConn {
        data object Disconnected : WsConn
        data object Connecting : WsConn
        data class Connected(val lastSeq: Long) : WsConn
    }

    private val _state = MutableStateFlow<WsConn>(WsConn.Disconnected)
    val state: StateFlow<WsConn> = _state

    /** 消息流（feature 按需订阅过滤 5 类推送）。 */
    private val _events = MutableSharedFlow<WsMessage>(extraBufferCapacity = 64)
    val events: SharedFlow<WsMessage> = _events

    /** seq 落盘回调（红线：先持久化再分发）。 */
    @Volatile var onSeqPersisted: (suspend (seq: Long, msg: WsMessage) -> Unit)? = null

    /** 取 last_seq 的回调（启动时从 Room/DataStore 恢复）。 */
    @Volatile var lastSeqProvider: (suspend () -> Long)? = null

    private var job: Job? = null
    private val closed = AtomicBoolean(false)
    private val json = Json { ignoreUnknownKeys = true }

    /** WS 地址 + token 由外部注入（app 层装配）。 */
    @Volatile var wsUrl: String = ""
    @Volatile var token: String = ""

    /** 启动连接（外层重连循环）。 */
    fun connect(scope: CoroutineScope) {
        if (closed.get()) return
        if (job?.isActive == true) return
        job = scope.launch {
            var backoff = 0L
            loop@ while (!closed.get()) {
                _state.value = WsConn.Connecting
                val lastSeq = lastSeqProvider?.invoke() ?: 0L
                val opened = CompletableDeferred<Boolean>()
                var socket: WebSocket? = null

                val request = Request.Builder()
                    .url(
                        wsUrl.replace("token=", "") // 防重复拼接
                            .toHttpUrlBuilder(lastSeq)
                    )
                    .build()

                socket = net().okHttp.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        opened.complete(true)
                        // resume 帧：服务端按 last_seq 补发缺口
                        webSocket.send("""{"type":"resume","last_seq":$lastSeq}""")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        scope.launch {
                            handleText(text) { m -> webSocket.send("""{"type":"ack","ack":${m.seq}}""") }
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        opened.complete(false)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        opened.complete(false)
                    }
                })

                // 等待打开结果；失败则按指数退避重试
                val ok = opened.await()
                if (ok) {
                    _state.value = WsConn.Connected(lastSeq)
                    backoff = 0L
                    // 挂起直至连接关闭（监听 socket 生命周期）
                    awaitCloseSuspend()
                }
                if (closed.get()) break@loop
                _state.value = WsConn.Disconnected
                backoff = if (backoff == 0L) 1000L else (backoff * 2).coerceAtMost(60_000L)
                val jitter = (backoff * 0.2 * Math.random() - backoff * 0.1).toLong()
                delay(backoff + jitter)
            }
        }
    }

    /** 主动断开（登出/后台 5 分钟）。 */
    fun disconnect() {
        job?.cancel()
        job = null
        _state.value = WsConn.Disconnected
    }

    /** 挂起直到 job 被取消（连接期间主循环空转，消息由 listener 直接分发）。 */
    private suspend fun awaitCloseSuspend() {
        try { awaitCancellation() } catch (_: CancellationException) { }
    }

    /** 消息到达：先落盘 → 回 ACK → 再分发。 */
    private suspend fun handleText(text: String, ack: (WsMessage) -> Unit) {
        if (text.isBlank()) return
        // 心跳/pong 等非 JSON 帧直接忽略
        val msg = try {
            json.decodeFromString(WsMessage.serializer(), text)
        } catch (_: Exception) {
            return
        }
        if (msg.seq > 0) {
            onSeqPersisted?.invoke(msg.seq, msg)   // 红线：先持久化
            ack(msg)                                // 再 ACK
        }
        _events.emit(msg)
    }

    /** URL 拼接 uid/lat 参数（演示通道 ?uid=；生产 token query）。 */
    private fun String.toHttpUrlBuilder(lastSeq: Long): String {
        val sep = if (contains("?")) "&" else "?"
        return if (token.isNotEmpty()) "$this${sep}token=$token" else this
    }

    companion object {
        /** 简单 holder，避免循环依赖（app 层注入 NetworkModule 单例）。 */
        @Volatile private var instance: NetworkModule? = null
        fun bind(module: NetworkModule) { instance = module }
        fun net(): NetworkModule = instance ?: NetworkModule()
    }
}
