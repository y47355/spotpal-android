package com.spotpal.core.network

import com.spotpal.core.model.BizError
import com.spotpal.core.model.Envelope
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络栈装配（详细设计 §2.1）：
 * OkHttp（鉴权/幂等/trace 拦截器）+ Retrofit + kotlinx-serialization。
 */
@Singleton
class NetworkModule @Inject constructor() {

    val json = Json {
        ignoreUnknownKeys = true     // 服务端加字段不炸客户端
        explicitNulls = false
        coerceInputValues = true     // null → 默认值
    }

    /** token 供给（由 core/data 的 TokenStore 实现）。 */
    @Volatile var tokenProvider: (() -> String?)? = null

    /** 401 后回调（触发登出/重登）。 */
    @Volatile var onUnauthorized: (() -> Unit)? = null

    private val authInterceptor = Interceptor { chain ->
        val token = tokenProvider?.invoke()
        val req = chain.request().newBuilder().apply {
            token?.takeIf { it.isNotEmpty() }?.let { header("Authorization", "Bearer $it") }
        }.build()
        val resp = chain.proceed(req)
        if (resp.code == 401) onUnauthorized?.invoke()
        resp
    }

    /** 写操作自动挂 Idempotency-Key=UUID，重试复用同 key（服务端 UNIQUE 兜底）。 */
    private val idempotencyInterceptor = Interceptor { chain ->
        val req = chain.request()
        val isWrite = req.method == "POST" || req.method == "PUT" || req.method == "DELETE"
        val tagged = if (isWrite && req.header("Idempotency-Key") == null) {
            req.newBuilder().header("Idempotency-Key", UUID.randomUUID().toString()).build()
        } else req
        chain.proceed(tagged)
    }

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)   // WS 心跳（与服务端 30s 对齐）
        .addInterceptor(authInterceptor)
        .addInterceptor(idempotencyInterceptor)
        .build()

    fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    fun api(baseUrl: String): SpotPalApi = retrofit(baseUrl).create(SpotPalApi::class.java)
}

/**
 * 统一响应展开：code != 0 抛 BizError（错误码段见 Models.kt 注释）。
 * 网络失败抛 IOException，由调用方 Result 封装兜底。
 */
suspend fun <T> Envelope<T>.unwrap(): T {
    if (code != 0) throw BizError(code, msg)
    @Suppress("UNCHECKED_CAST")
    return data ?: throw BizError(-1, "empty data")
}
