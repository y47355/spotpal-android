package com.spotpal.core.design.gesture

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 上推托付手势参数表（详细设计 §2.3，与《技术设计文档》2.2 一致）。
 * 全部收口在此，便于调参 A/B。
 */
object GestureSpec {
    /** 长按弹出微调面板的等待时长（毫秒）。 */
    const val LONG_PRESS_TIMEOUT_MS = 600L

    /** 上推过阈值即触发托付（按密度归一化）。 */
    val PushThreshold: Dp = 80.dp

    /** 位移容差：未超过视为"没动"，不取消长按计时。 */
    val TouchSlop: Dp = 12.dp

    /** 阈值过后卡片视觉缩小比例。 */
    const val SNAPPED_SCALE = 0.6f
}

/**
 * 手势状态机（与 UiState.gesture 同构，对应设计稿 4 帧：
 * 静置 → 长按微调 → 上推托付 → 释放锁定）。
 */
sealed interface GesturePhase {
    /** ① 静置浏览。 */
    data object Idle : GesturePhase

    /** ② 长按面板已弹出。 */
    data class Adjusting(val cardId: String) : GesturePhase

    /** ③ 上推中（含实时位移与过阈值标记）。 */
    data class Pushing(val dy: Float, val passed: Boolean) : GesturePhase

    /** ④ 松手锁定。 */
    data object Released : GesturePhase
}
