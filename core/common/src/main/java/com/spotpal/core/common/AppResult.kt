package com.spotpal.core.common

/**
 * Result 封装（详细设计 §1.4 core/common）：
 * Ok / Err(Io) / Err(Biz) 三态 + onOk/onErr 组合子。
 */

sealed interface AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>
    data class Err(val error: AppError) : AppResult<Nothing>

    /** 成功则映射，失败透传。 */
    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

    /** 成功则切换，失败透传。 */
    fun <R> flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
        is Ok -> transform(value)
        is Err -> this
    }

    fun getOrNull(): T? = (this as? Ok)?.value

    companion object {
        fun <T> ok(value: T): AppResult<T> = Ok(value)
        fun err(error: AppError): AppResult<Nothing> = Err(error)
    }
}

/** 错误分类：IO（可重试/离线提示）与 Biz（业务码：池满/轮次超限…）。 */
sealed interface AppError {
    data class Io(val cause: Throwable? = null, val message: String = "网络不给力，稍后再试") : AppError
    data class Biz(val code: Int, val message: String) : AppError {
        /** 3002 池满专用文案（设计稿 toast）。 */
        fun isPoolFull() = code == 3002
    }
}

/** 消费端便捷：AppResult → 数据或 null。 */
fun <T> AppResult<T>.orNull(): T? = getOrNull()

/** 消费端便捷：错误 → 用户文案。 */
fun AppError.userMessage(): String = when (this) {
    is AppError.Io -> message
    is AppError.Biz -> when (code) {
        3002 -> "候选池已满（5 人）"
        4001, 4005 -> "协商轮次已达上限"
        1001 -> "登录已过期，请重新登录"
        else -> message
    }
}
