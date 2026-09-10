package com.spotpal.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * 轻量退避工具：指数退避 + 抖动（供网络重试/WS 重连复用）。
 */
object Backoff {

    /** 1s → 2s → 4s → ... 上限 maxMs；每次 ±20% 抖动。 */
    fun next(current: Long, base: Long = 1000L, maxMs: Long = 60_000L): Long {
        if (current <= 0L) return base
        return min(current * 2, maxMs)
    }

    fun jitter(ms: Long): Long = (ms * 0.2 * Math.random() - ms * 0.1).toLong()

    /** 按退避节奏重试 block，直到成功或 maxAttempts 次。 */
    suspend fun retry(maxAttempts: Int = 3, block: suspend (attempt: Int) -> Boolean): Boolean {
        var backoff = 0L
        repeat(maxAttempts) { attempt ->
            if (block(attempt)) return true
            backoff = next(backoff)
            delay(backoff + jitter(backoff))
        }
        return false
    }
}
