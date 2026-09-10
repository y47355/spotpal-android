package com.spotpal.core.data

import com.spotpal.core.model.Negotiation
import com.spotpal.core.model.PoolItem
import com.spotpal.core.model.WsMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 数据仓库（core/data → feature 桥）：
 * Room 三表 + WS seq 红线落盘 + 乐观更新模式（push 先写库再补服务端）。
 */
class SpotPalRepo(
    private val db: SpotPalDb,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // ---- WS seq 红线 ----

    /** seq 先落盘再分发（WsClient.onSeqPersisted 回调目标）。 */
    suspend fun persistWs(msg: WsMessage) {
        db.wsDao().insert(WsMessageEntity(msg.seq, msg.type, msg.payload, msg.ts))
    }

    suspend fun lastSeq(): Long = db.wsDao().lastSeq() ?: 0L

    /** 7 天滚动清理。 */
    suspend fun purgeWs() {
        db.wsDao().purgeBefore(System.currentTimeMillis() - 7L * 24 * 3600 * 1000)
    }

    fun wsByType(type: String): Flow<List<WsMessage>> =
        db.wsDao().byType(type).map { list -> list.map { WsMessage(it.type, it.seq, it.payload, it.ts) } }

    // ---- 候选池 ----

    fun poolFlow(): Flow<List<PoolItem>> = db.poolDao().all().map { list ->
        list.map { PoolItem(id = it.id, candidateId = it.candidateId, nickname = it.nickname, status = it.status) }
    }

    fun poolCountFlow(): Flow<Int> = db.poolDao().count()

    /** 服务端权威数据覆盖本地镜像。 */
    suspend fun replacePool(items: List<PoolItem>) {
        // 全量覆盖（演示规模小；生产按 version 合并）
        val all = db.poolDao().all()
        // 简化：先读后写（单用户场景安全）
        db.poolDao().upsertAll(items.map {
            CandidatePoolEntity(it.id, it.candidateId, it.nickname, it.status)
        })
    }

    /** 上推成功后的乐观插入。 */
    suspend fun optimisticPush(item: PoolItem) {
        db.poolDao().upsert(CandidatePoolEntity(it.id, it.candidateId, it.nickname, it.status))
    }

    /** 池满回滚。 */
    suspend fun rollbackPush(candidateId: String) {
        db.poolDao().removeByCandidate(candidateId)
    }

    suspend fun syncPool(items: List<PoolItem>) = replacePool(items)

    // ---- 协商缓存 ----

    suspend fun cacheNegotiation(n: Negotiation) {
        db.ngtDao().upsert(
            NegotiationEntity(
                id = n.id, squadId = n.squadId, candidateId = n.candidateId,
                round = n.round, status = n.status, expireAt = n.expireAt,
                roundsJson = json.encodeToString(n.rounds),
            )
        )
    }

    fun negotiationFlow(id: String): Flow<Negotiation?> =
        db.ngtDao().byId(id).map { e ->
            e?.let {
                Negotiation(
                    id = it.id, squadId = it.squadId, candidateId = it.candidateId,
                    round = it.round, status = it.status, expireAt = it.expireAt,
                    rounds = try { json.decodeFromString(it.roundsJson) } catch (_: Exception) { emptyList() },
                )
            }
        }
}
