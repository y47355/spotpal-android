package com.spotpal.core.design.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 上推托付卡片（详细设计 §2.3 核心骨架）。
 *
 * 性能红线：全部动画只动 graphicsLayer 属性（translationY/scale），
 * 不触发重组布局；passed 布尔用 animateFloatAsState 收敛。
 *
 * @param onLongPress 长按回调（弹微调面板 ②）
 * @param onPushProgress 上推进度回调（③）
 * @param onReleased 松手回调（④ passed=true 上飞入池 / false 回弹）
 * @param content 卡片内容
 */
@Composable
fun PushableCard(
    cardId: String,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit = {},
    onPushProgress: (dy: Float, passed: Boolean) -> Unit = { _, _ -> },
    onReleased: (passed: Boolean) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val offsetY = remember { Animatable(0f) }
    var passed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (passed) GestureSpec.SNAPPED_SCALE else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "pushScale",
    )
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { GestureSpec.PushThreshold.toPx() }
    val slopPx = with(density) { GestureSpec.TouchSlop.toPx() }

    Box(
        modifier
            .graphicsLayer {
                translationY = offsetY.value
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(cardId) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    passed = false
                    var longPressJob: Job? = null
                    var totalMove = 0f

                    // —— 长按检测：600ms 内位移 < TouchSlop 才算长按 ——
                    longPressJob = scope.launch {
                        val startAt = System.nanoTime()
                        kotlinx.coroutines.delay(GestureSpec.LONG_PRESS_TIMEOUT_MS)
                        if (totalMove < slopPx) onLongPress() // 弹微调面板（②）
                    }

                    // —— 垂直拖拽主循环：只认上推（负值） ——
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val dy = change.positionChange().y
                            totalMove += kotlin.math.abs(dy)
                            if (totalMove > slopPx) longPressJob?.cancel() // 一动就取消长按
                            if (dy < 0) {
                                scope.launch { offsetY.snapTo((offsetY.value + dy).coerceAtLeast(-thresholdPx * 2)) }
                                val passedNow = -offsetY.value > thresholdPx
                                if (passedNow != passed) {
                                    passed = passedNow
                                    if (passedNow) haptic.performHapticFeedback(
                                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                    )
                                }
                                onPushProgress(offsetY.value, passedNow) // 更新 ③
                            }
                            change.consume()
                        }
                    } finally {
                        longPressJob?.cancel()
                    }

                    // —— 松手：过阈值→上飞；否则回弹（防误触） ——
                    if (passed) {
                        scope.launch {
                            offsetY.animateTo(
                                -size.height * 1.2f,
                                tween(220, easing = FastOutSlowInEasing),
                            )
                            onReleased(true)      // ④ POOLED
                            offsetY.snapTo(0f)
                            passed = false
                        }
                    } else {
                        scope.launch {
                            offsetY.animateTo(
                                0f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                            onReleased(false)     // 取消，卡片回原位
                        }
                    }
                }
            },
    ) { content() }
}
