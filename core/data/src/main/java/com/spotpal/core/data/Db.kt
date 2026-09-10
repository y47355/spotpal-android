package com.spotpal.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import com.spotpal.core.model.Negotiation
import com.spotpal.core.model.PoolItem
import com.spotpal.core.model.WsMessage
import kotlinx.coroutines.flow.Flow

/**
 * 本地存储（详细设计 §2.5 core/data）。
 * 三表：ws_messages（seq 红线落盘）/ candidate_pool（离线镜像）/ negotiations（详情秒开）。
 * 策略：联网以服务端为准，离线可读。
 */

@Entity(tableName = "ws_messages")
data class WsMessageEntity(
    @PrimaryKey val seq: Long,
    val type: String,
    val payload: String,
    val ts: Long,
    val acked: Boolean = false,
)

@Entity(tableName = "candidate_pool")
data class CandidatePoolEntity(
    @PrimaryKey val id: String,           // 服务端 candidate_pool.id
    val candidateId: String,
    val nickname: String,
    val status: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "negotiations")
data class NegotiationEntity(
    @PrimaryKey val id: String,
    val squadId: String?,
    val candidateId: String,
    val round: Int,
    val status: String,
    val expireAt: String?,
    val roundsJson: String,               // 轮次 JSON 快照
    val updatedAt: Long = System.currentTimeMillis(),
)

@Database(
    entities = [WsMessageEntity::class, CandidatePoolEntity::class, NegotiationEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SpotPalDb : RoomDatabase() {
    abstract fun wsDao(): WsMessageDao
    abstract fun poolDao(): CandidatePoolDao
    abstract fun ngtDao(): NegotiationDao
}

@androidx.room.Dao
interface WsMessageDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(msg: WsMessageEntity)

    @androidx.room.Query("SELECT MAX(seq) FROM ws_messages")
    suspend fun lastSeq(): Long?

    @androidx.room.Query("SELECT MAX(seq) FROM ws_messages")
    fun lastSeqFlow(): Flow<Long?>

    @androidx.room.Query("DELETE FROM ws_messages WHERE ts < :before")
    suspend fun purgeBefore(before: Long)

    @androidx.room.Query("SELECT * FROM ws_messages WHERE type = :type ORDER BY seq DESC LIMIT 50")
    fun byType(type: String): Flow<List<WsMessageEntity>>
}

@androidx.room.Dao
interface CandidatePoolDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CandidatePoolEntity>)

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CandidatePoolEntity)

    @androidx.room.Delete
    suspend fun remove(item: CandidatePoolEntity)

    @androidx.room.Query("DELETE FROM candidate_pool WHERE candidateId = :candidateId")
    suspend fun removeByCandidate(candidateId: String)

    @androidx.room.Query("SELECT * FROM candidate_pool ORDER BY updatedAt DESC")
    fun all(): Flow<List<CandidatePoolEntity>>

    @androidx.room.Query("SELECT COUNT(*) FROM candidate_pool")
    fun count(): Flow<Int>
}

@androidx.room.Dao
interface NegotiationDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(item: NegotiationEntity)

    @androidx.room.Query("SELECT * FROM negotiations WHERE id = :id")
    fun byId(id: String): Flow<NegotiationEntity?>

    @androidx.room.Query("DELETE FROM negotiations WHERE id = :id")
    suspend fun delete(id: String)
}

/** DB 单例工厂（demo 级：进程单例；生产由 Hilt provide）。 */
object DbHolder {
    @Volatile private var db: SpotPalDb? = null

    fun get(context: Context): SpotPalDb =
        db ?: synchronized(this) {
            db ?: Room.databaseBuilder(context, SpotPalDb::class.java, "spotpal.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { db = it }
        }
}
