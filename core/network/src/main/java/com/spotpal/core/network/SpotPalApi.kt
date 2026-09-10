package com.spotpal.core.network

import com.spotpal.core.model.*
import retrofit2.http.*

/**
 * 11 端点 Retrofit 接口（与服务端 api.go 一一对应）。
 * 响应为 Envelope 信封；data 键名以服务端实际实现为准。
 */
interface SpotPalApi {

    // 1. 登录（验证码演示通道：任意 6 位码通过）
    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginReq): Envelope<LoginData>

    // 2. 上推入池（幂等：Idempotency-Key + UNIQUE 兜底）
    @POST("v1/pool/push")
    suspend fun push(@Body body: PushReq): Envelope<Unit>

    // 3. 候选池
    @GET("v1/pool")
    suspend fun pool(): Envelope<PoolData>

    // 4. 移除候选
    @DELETE("v1/pool/{candidateId}")
    suspend fun remove(@Path("candidateId") id: String): Envelope<Unit>

    // 5. 释放委托 → 协商会话 + 首轮方案（事件 pool.joined）
    @POST("v1/negotiation/release")
    suspend fun release(@Body body: ReleaseReq): Envelope<ReleaseData>

    // 6. 协商详情（会话 + 轮次）
    @GET("v1/negotiation/{id}")
    suspend fun negotiation(@Path("id") id: String): Envelope<Negotiation>

    // 7. 方案反馈：ok → 定稿；否则下一轮
    @POST("v1/negotiation/{id}/feedback")
    suspend fun feedback(@Path("id") id: String, @Body body: FeedbackReq): Envelope<Unit>

    // 8. 确认成局（乐观锁兜底竞态）
    @POST("v1/squad/{id}/confirm")
    suspend fun confirm(@Path("id") id: String): Envelope<ConfirmData>

    // 9. 打卡结算（纯记账：PENDING → SUCCESS，费用线下自理）
    @POST("v1/squad/{id}/checkin")
    suspend fun checkin(@Path("id") id: String): Envelope<CheckinData>

    // 10. 推荐流（cursor 分页；位置只进网络参数不落盘）
    @GET("v1/feed/recommend")
    suspend fun recommend(
        @Query("cursor") cursor: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
    ): Envelope<RecommendPage>

    // 11. WS 消息断线补拉
    @GET("v1/ws/messages")
    suspend fun pullAfter(@Query("after_seq") seq: Long): Envelope<MessagesData>

    // 12. 我的搭局（服务端已实现 GET /v1/me/squads）
    @GET("v1/me/squads")
    suspend fun mySquads(): Envelope<SquadsData>
}
