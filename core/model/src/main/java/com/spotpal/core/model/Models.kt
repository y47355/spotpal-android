package com.spotpal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 纯数据类：DTO / 领域模型（无 Android 依赖，core/model）。
 * 字段与服务端 spotpal-server 实际 JSON 一一对应（以运行代码为准）。
 */

/** 服务端响应信封（详细设计 §2.1：{code, msg, data, req_id}）。 */
@Serializable
data class Envelope<T>(
    val code: Int,
    val msg: String,
    val data: T? = null,
    @SerialName("req_id") val reqId: String? = null,
)

// ---- 各端点 data 响应体（键名以服务端 api.go 为准）----

/** POST /v1/auth/login。 */
@Serializable
data class LoginData(
    val token: String,
    @SerialName("user_id") val userId: String,
    val nickname: String = "",
    @SerialName("credit_score") val creditScore: Int = 80,
)

/** GET /v1/feed/recommend。 */
@Serializable
data class RecommendPage(
    val cards: List<RecommendCard> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

/** GET /v1/pool。 */
@Serializable
data class PoolData(val items: List<PoolItem> = emptyList())

/** GET /v1/me/squads。 */
@Serializable
data class SquadsData(val squads: List<Squad> = emptyList())

/** GET /v1/ws/messages。 */
@Serializable
data class MessagesData(val messages: List<WsMessage> = emptyList())

/** POST /v1/negotiation/release。 */
@Serializable
data class ReleaseData(@SerialName("negotiation_id") val negotiationId: String)

/** POST /v1/squad/{id}/confirm。 */
@Serializable
data class ConfirmData(
    @SerialName("squad_id") val squadId: String,
    val status: String,
)

/** POST /v1/squad/{id}/checkin。 */
@Serializable
data class CheckinData(
    @SerialName("order_id") val orderId: String,
    val status: String,
    val note: String? = null,
)

// ---- 领域模型 ----

/** 推荐卡（发现页）。 */
@Serializable
data class RecommendCard(
    @SerialName("user_id") val userId: String,
    val nickname: String,
    @SerialName("tag_overlap") val tagOverlap: List<String> = emptyList(),
    val score: Double = 0.0,
    val reason: String = "",
    val distance: Double = 0.0,
)

/** 候选池项（服务端 Pool() 返回：id/candidate_id/nickname/intent/status）。 */
@Serializable
data class PoolItem(
    val id: String,
    @SerialName("candidate_id") val candidateId: String,
    val nickname: String = "",
    val status: String = "WAITING",
)

/** 协商会话（服务端 negotiate.Get() 返回形状）。 */
@Serializable
data class Negotiation(
    val id: String,
    @SerialName("squad_id") val squadId: String? = null,
    @SerialName("candidate_id") val candidateId: String = "",
    val round: Int = 0,
    val status: String = "ACTIVE",
    @SerialName("expire_at") val expireAt: String? = null,
    val rounds: List<Round> = emptyList(),
)

/** 协商轮次（proposal 为嵌套对象；feedback 为 {from, ok, note}）。 */
@Serializable
data class Round(
    val round: Int,
    val proposal: Proposal? = null,
    val feedback: Feedback? = null,
)

/** 方案卡（LLM 产出：time/venue/cost_mode/per_head/reason）。 */
@Serializable
data class Proposal(
    val time: String = "",
    val venue: String = "",
    @SerialName("cost_mode") val costMode: String = "AA",
    @SerialName("per_head") val perHead: Double = 0.0,
    val reason: String = "",
)

/** 双方反馈。 */
@Serializable
data class Feedback(
    val from: String = "",
    val ok: Boolean = false,
    val note: String? = null,
)

/** 搭局（服务端 MySquads() 返回：id/status/activity/time_window/cost_mode/expire_at）。 */
@Serializable
data class Squad(
    val id: String,
    val status: String,
    val activity: String = "",
    @SerialName("time_window") val timeWindow: String? = null,
    @SerialName("cost_mode") val costMode: String? = null,
    @SerialName("expire_at") val expireAt: String? = null,
)

/** 结算单（初版纯记账：账面记录，费用线下自理）。 */
@Serializable
data class SettleOrder(
    @SerialName("order_id") val id: String,
    val status: String,
    val note: String? = null,
)

// ---- 请求体 ----

/** POST /v1/auth/login。 */
@Serializable
data class LoginReq(
    val phone: String,
    val code: String = "000000",
    val nickname: String? = null,
)

/** POST /v1/pool/push。 */
@Serializable
data class PushReq(
    @SerialName("candidate_id") val candidateId: String,
    val intent: String = "",
)

/** POST /v1/negotiation/release。 */
@Serializable
data class ReleaseReq(
    val activity: String,
    val hours: List<Int> = emptyList(),
    val weekdays: List<String> = emptyList(),
    @SerialName("cost_mode") val costMode: String = "AA",
)

/** POST /v1/negotiation/{id}/feedback。 */
@Serializable
data class FeedbackReq(
    val ok: Boolean,
    val note: String = "",
)

// ---- WS ----

/** WS 消息信封（详细设计 §2.2：{type, seq, payload, ts}）。 */
@Serializable
data class WsMessage(
    val type: String,
    val seq: Long = 0,
    val payload: String = "",
    val ts: Long = 0,
)

/** 领域异常：错误码段 → 领域语义（1xxx 通用 / 2xxx 用户 / 3xxx 搭局 / 4xxx 协商 / 5xxx 结算预留）。 */
class BizError(val code: Int, override val message: String) : Exception(message)
