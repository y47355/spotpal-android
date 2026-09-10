package com.spotpal.core.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 会话与偏好（详细设计 §2.5 DataStore 列：token / last_seq / guide_shown / user_pref）。
 * 初版用内存 + SharedPreferences 轻实现（token 详见安全矩阵：M4 前迁 EncryptedSharedPreferences）。
 */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences("spotpal_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class UserPref(
        val timeWindow: String = "周末 · 20:00-22:00",
        val costMode: String = "AA",
        val role: String = "随便都行",
    )

    data class Session(
        val token: String,
        val userId: String,
        val nickname: String,
        val creditScore: Int,
    )

    private val _session = MutableStateFlow<Session?>(null)
    val session = _session.asStateFlow()

    private val _pref = MutableStateFlow(restorePref())
    val pref = _pref.asStateFlow()

    init {
        // 冷启动恢复 token（App 常驻期间直接用）
        val token = prefs.getString(KEY_TOKEN, null)
        if (token != null) {
            _session.value = Session(
                token = token,
                userId = prefs.getString(KEY_UID, "") ?: "",
                nickname = prefs.getString(KEY_NICK, "") ?: "",
                creditScore = prefs.getInt(KEY_CREDIT, 80),
            )
        }
    }

    fun saveLogin(token: String, userId: String, nickname: String, creditScore: Int) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_UID, userId)
            .putString(KEY_NICK, nickname)
            .putInt(KEY_CREDIT, creditScore)
            .apply()
        _session.value = Session(token, userId, nickname, creditScore)
    }

    fun logout() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    val token: String get() = _session.value?.token ?: ""
    val userId: String get() = _session.value?.userId ?: ""

    /** 首次引导一次性标记。 */
    var guideShown: Boolean
        get() = prefs.getBoolean(KEY_GUIDE, false)
        set(value) { prefs.edit().putBoolean(KEY_GUIDE, value).apply() }

    var lastSeq: Long
        get() = prefs.getLong(KEY_LAST_SEQ, 0L)
        set(value) { prefs.edit().putLong(KEY_LAST_SEQ, value).apply() }

    fun savePref(pref: UserPref) {
        _pref.value = pref
        prefs.edit().putString(KEY_PREF, json.encodeToString(pref)).apply()
    }

    private fun restorePref(): UserPref = try {
        prefs.getString(KEY_PREF, null)?.let { json.decodeFromString(it) } ?: UserPref()
    } catch (_: Exception) {
        UserPref()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_UID = "user_id"
        const val KEY_NICK = "nickname"
        const val KEY_CREDIT = "credit_score"
        val KEY_GUIDE = "guide_shown"
        val KEY_PREF = "user_pref"
        val KEY_LAST_SEQ = "last_seq"
    }
}
