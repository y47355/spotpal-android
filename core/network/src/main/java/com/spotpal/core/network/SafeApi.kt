package com.spotpal.core.network

import com.spotpal.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

/**
 * 安全 API 门面：Envelop 展开为领域结果（core/network）。
 * feature 层不直接碰 Retrofit，统一走本门面（+ Hilt 注入）。
 */
class SafeApi(
    private val api: SpotPalApi,
    val baseUrl: String,
) {

    private suspend fun <T> call(block: suspend SpotPalApi.() -> Envelope<T>): Result<T> = try {
        Result.success(block(api).unwrap())
    } catch (e: BizError) {
        Result.failure(e)
    } catch (e: HttpException) {
        Result.failure(BizError(-e.code(), "http ${e.code()}"))
    } catch (e: IOException) {
        Result.failure(BizError(1005, "network unavailable"))
    }

    fun login(phone: String, nickname: String?): Result<LoginData> =
        kotlinx.coroutines.runBlocking { call { login(LoginReq(phone, nickname = nickname)) } }

    suspend fun loginSuspend(phone: String, nickname: String?): Result<LoginData> =
        call { login(LoginReq(phone, nickname = nickname)) }

    suspend fun pushToPool(candidateId: String, intent: String = ""): Result<Unit> =
        call { push(PushReq(candidateId, intent)) }

    suspend fun pool(): Result<PoolData> = call { pool() }

    suspend fun removeFromPool(candidateId: String): Result<Unit> =
        call { remove(candidateId) }

    suspend fun release(activity: String, hours: List<Int>, weekdays: List<String>, costMode: String): Result<ReleaseData> =
        call { release(ReleaseReq(activity, hours, weekdays, costMode)) }

    suspend fun negotiation(id: String): Result<Negotiation> = call { negotiation(id) }

    suspend fun feedback(id: String, ok: Boolean, note: String = ""): Result<Unit> =
        call { feedback(id, FeedbackReq(ok, note)) }

    suspend fun confirm(id: String): Result<ConfirmData> = call { confirm(id) }

    suspend fun checkin(id: String): Result<CheckinData> = call { checkin(id) }

    suspend fun recommend(cursor: String? = null, lat: Double? = null, lng: Double? = null): Result<RecommendPage> =
        call { recommend(cursor, lat, lng) }

    suspend fun pullAfter(seq: Long): Result<MessagesData> = call { pullAfter(seq) }

    /** GET /v1/me/squads。 */
    suspend fun mySquads(): Result<SquadsData> = call { mySquads() }
}
