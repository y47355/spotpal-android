package com.spotpal.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.spotpal.core.design.SpotPalColor
import com.spotpal.core.model.RecommendCard

/**
 * ② 搭子主页（设计稿 profile 屏）：头像/昵称/标签 + 信用分暗卡 + 约TA一次 CTA。
 */
@Composable
fun ProfileScreen(nav: NavHostController, uid: String) {
    // demo：从发现页共享卡片数据（生产从 /v1/user/{id} 拉取）
    var card by remember { mutableStateOf<RecommendCard?>(null) }
    val nickname = card?.nickname ?: "搭子 $uid"
    val tags = card?.tagOverlap ?: listOf("羽毛球", "周末党", "AA制")

    Scaffold(containerColor = SpotPalColor.Bg) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 顶部返回
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { nav.popBackStack() }) { Text("返回") }
                Spacer(Modifier.width(8.dp))
                Text("搭子主页", style = MaterialTheme.typography.titleLarge)
            }

            // 头像 + 昵称 + 标签
            Surface(shape = MaterialTheme.shapes.medium, color = Color.White) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp),
                            color = SpotPalColor.Primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(nickname.take(1), style = MaterialTheme.typography.headlineMedium, color = SpotPalColor.Primary)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(nickname, style = MaterialTheme.typography.titleMedium)
                            Text("福田 · 800m", style = MaterialTheme.typography.labelMedium, color = SpotPalColor.Sub)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.take(3).forEach { tag ->
                            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp), color = SpotPalColor.BadgeBlueBg) {
                                Text(tag, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = SpotPalColor.Primary)
                            }
                        }
                    }
                }
            }

            // 信用分暗卡（设计稿 DarkCard）
            Surface(shape = MaterialTheme.shapes.small, color = SpotPalColor.DarkCard) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("信用分", style = MaterialTheme.typography.labelSmall, color = SpotPalColor.DarkSub)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("4.9", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(" / 5.0 · 12 次成局 0 跳票", Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.labelMedium, color = SpotPalColor.DarkSub)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 主 CTA：约TA一次 → 释放委托（协商）
            Button(
                onClick = {
                    // demo：直接释放委托进入协商（生产：确认弹窗 → release → navigate）
                    nav.navigate("negotiate/ngt_demo_1")
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp),
            ) {
                Text("约TA一次", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
