package com.spotpal.core.network

/**
 * 全局 API 装配点（demo 级 ServiceLocator）。
 * app 层 SpotPalApp.onCreate 初始化；feature 层只读。
 * 生产版换 Hilt @Provides（迁移点唯一：本 object 的 init 调用）。
 */
object ApiGraph {

    @Volatile
    var api: SafeApi = SafeApi(StubApi, "http://10.0.2.2:8080")
        private set

    @Volatile
    var ws: WsClient = WsClient()
        private set

    @Volatile
    var network: NetworkModule = NetworkModule()
        private set

    fun init(baseUrl: String, wsUrl: String) {
        network = NetworkModule()
        api = SafeApi(network.api(baseUrl), baseUrl)
        ws = WsClient().apply { this.wsUrl = wsUrl }
    }

    /** 编译期兜底（避免空 api 崩溃；真机连不上也有明确报错）。 */
    private object StubApi : SpotPalApi by stubImpl()

    private fun stubImpl(): SpotPalApi {
        val handler = java.lang.reflect.Proxy.newProxyInstance(
            SpotPalApi::class.java.classLoader,
            arrayOf(SpotPalApi::class.java),
        ) { _, method, _ ->
            throw IllegalStateException("ApiGraph not initialized — call ApiGraph.init() in Application.onCreate")
        }
        @Suppress("UNCHECKED_CAST")
        return handler as SpotPalApi
    }
}
