package com.spotpal.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设计系统（详细设计 §3.1，映射自 Ardot 设计稿 723889208012364）。
 *
 * 原则：1px 细边框无阴影（海拔 0）、18dp 卡片圆角、11dp 内嵌条圆角。
 */
object SpotPalColor {
    val Primary = Color(0xFF0066CC)      // 品牌蓝（设计稿 #0066CC）
    val PrimaryLt = Color(0xFF29A3FF)    // 暗卡亮蓝
    val BadgeBlueBg = Color(0xFFE8F1FF)  // 浅蓝徽章底
    val Ink = Color(0xFF1D1D1F)          // 主文字
    val Sub = Color(0xFF86868B)         // 次文字
    val DarkCard = Color(0xFF272729)     // 暗卡（AI 推荐/信用分卡）
    val DarkSub = Color(0xFF999999)     // 暗卡次文字
    val Success = Color(0xFF34C759)      // 成功绿（确认状态）
    val Bg = Color(0xFFF5F5F7)           // 屏幕底
    val Line = Color(0xFFE0E0E0)         // 1px 细边框
    val Sep = Color(0xFFF0F0F0)         // 分隔线
}

/** 统一 Shape：18dp 卡片 / 11dp 内嵌条 / 全胶囊。 */
object SpotPalShapes {
    val Card = RoundedCornerShape(18.dp)
    val Inner = RoundedCornerShape(11.dp)
    val Pill = RoundedCornerShape(9999.dp)
}

/** 头像渐变（设计稿三组）。 */
object SpotPalGradients {
    val Blue = listOf(Color(0xFF669EEF), Color(0xFF2E66CC))
    val Orange = listOf(Color(0xFFF2C784), Color(0xFFD98047))
    val Green = listOf(Color(0xFF8CD9A6), Color(0xFF339973))
}

private val LightColors = lightColorScheme(
    primary = SpotPalColor.Primary,
    onPrimary = Color.White,
    background = SpotPalColor.Bg,
    onBackground = SpotPalColor.Ink,
    secondary = SpotPalColor.Sub,
    surface = Color.White,
    onSurface = SpotPalColor.Ink,
    outline = SpotPalColor.Line,
    error = Color(0xFFFF3B30),
)

/**
 * 全局主题。
 * 字体：默认 Roboto + 系统中文字体（不打包字体，首包体积红线 25MB）。
 */
@Composable
fun SpotPalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = Shapes(
            small = SpotPalShapes.Inner,
            medium = SpotPalShapes.Card,
            large = SpotPalShapes.Card,
        ),
        typography = Typography(
            // 数字/评分：SemiBold（设计稿 Inter SemiBold 语义）
            titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp),
            titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
            bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
            bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
            labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
            labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp),
        ),
        content = content,
    )
}
