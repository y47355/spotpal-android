package com.spotpal.feature.me

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.spotpal.core.design.SpotPalColor

/**
 * ⑥ 我的（设计稿 me 屏）：头像/昵称/信用分暗卡 + 设置行 + 登出。
 */
@Composable
fun MeScreen(nav: NavHostController) {
    Scaffold(containerColor = SpotPalColor.Bg) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 用户卡
            Surface(shape = MaterialTheme.shapes.medium, color = Color.White) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp),
                            color = SpotPalColor.Primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("趣", style = MaterialTheme.typography.headlineMedium, color = SpotPalColor.Primary)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("阿夜", style = MaterialTheme.typography.titleMedium)
                            Text("深圳 · 福田", style = MaterialTheme.typography.labelMedium, color = SpotPalColor.Sub)
                        }
                    }
                    // 信用分暗卡
                    Surface(shape = MaterialTheme.shapes.small, color = SpotPalColor.DarkCard) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Bottom) {
                            Text("4.9", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                            Text(" / 5.0 信用分 · 12 次成局 · 0 跳票", Modifier.padding(bottom = 4.dp, start = 6.dp), style = MaterialTheme.typography.labelMedium, color = SpotPalColor.DarkSub)
                        }
                    }
                }
            }

            // 设置行
            Surface(shape = MaterialTheme.shapes.medium, color = Color.White) {
                Column {
                    listOf(
                        "我的兴趣标签",
                        "时间窗偏好（周末 · 20:00-22:00）",
                        "支付方式（线下自理）",
                        "隐私与权限",
                        "关于搭趣",
                    ).forEachIndexed { i, title ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(title, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            Text("›", color = SpotPalColor.Sub)
                        }
                        if (i < 4) HorizontalDivider(color = SpotPalColor.Sep)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                "搭趣 SpotPal v0.1.0 · AI 代协商 · 费用线下自理",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = SpotPalColor.Sub,
            )
        }
    }
}
