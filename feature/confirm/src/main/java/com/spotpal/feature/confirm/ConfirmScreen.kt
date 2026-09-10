package com.spotpal.feature.confirm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.spotpal.core.design.SpotPalColor
import com.spotpal.core.model.CheckinData
import com.spotpal.core.network.ApiGraph
import com.spotpal.core.network.SafeApi
import kotlinx.coroutines.launch

/**
 * ④ 搭局确认页（详细设计 §3.3）：
 * 费用明细三模式切换（AA/轮流/一人请）+ 「确认锁单」；
 * 初版无支付拉起：结算纯记账，文案「费用 AA · 人均 ¥45（线下自理）」。
 */

private val costModes = listOf("AA", "轮流", "一人请")

data class ConfirmUiState(
    val loading: Boolean = false,
    val mode: String = "AA",
    val perHead: Double = 45.0,
    val confirming: Boolean = false,
    val confirmed: Boolean = false,
    val orderNote: String? = null,
)

class ConfirmViewModel(private val api: SafeApi = ApiGraph.api) : ViewModel() {

    var state by mutableStateOf(ConfirmUiState())
        private set

    fun switchMode(mode: String) {
        state = state.copy(
            mode = mode,
            perHead = when (mode) {
                "AA" -> 45.0
                "轮流" -> 45.0
                "一人请" -> 90.0
                else -> 45.0
            },
        )
    }

    /** 确认锁单 → POST /v1/squad/{id}/confirm。 */
    fun confirm(id: String) {
        viewModelScope.launch {
            state = state.copy(confirming = true)
            val result = api.confirm(id)
            state = if (result.isSuccess) {
                state.copy(confirming = false, confirmed = true)
            } else {
                state.copy(confirming = false)
            }
        }
    }

    /** 打卡结算（纯记账）→ POST /v1/squad/{id}/checkin。 */
    fun checkin(id: String) {
        viewModelScope.launch {
            val result = api.checkin(id)
            if (result.isSuccess) {
                state = state.copy(orderNote = (result.getOrThrow() as CheckinData).note)
            }
        }
    }
}

@Composable
fun ConfirmScreen(nav: NavHostController, id: String) {
    val vm = remember { ConfirmViewModel() }
    val state = vm.state

    Scaffold(containerColor = SpotPalColor.Bg) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { nav.popBackStack() }) { Text("返回") }
                Spacer(Modifier.width(4.dp))
                Text("搭局确认", style = MaterialTheme.typography.titleLarge)
            }

            // 搭局信息卡
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotPalColor.Line),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("羽毛球 · 周六 20:00", style = MaterialTheme.typography.titleMedium)
                    Text("深圳 · 福田体育公园（3 号场）", style = MaterialTheme.typography.bodyMedium, color = SpotPalColor.Sub)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("阿哲", "小鹿").forEach { name ->
                            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp), color = SpotPalColor.BadgeBlueBg) {
                                Text(name, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = SpotPalColor.Primary)
                            }
                        }
                        Text("已到 2 人", style = MaterialTheme.typography.labelSmall, color = SpotPalColor.Sub)
                    }
                }
            }

            // 费用明细（三模式切换，即时联动预览）
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotPalColor.Line),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("费用明细", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        costModes.forEach { mode ->
                            FilterChip(
                                selected = state.mode == mode,
                                onClick = { vm.switchMode(mode) },
                                label = { Text(mode) },
                            )
                        }
                    }
                    // 结算金额预览（纯记账，无支付拉起）
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "¥${state.perHead.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SpotPalColor.Primary,
                        )
                        Text(
                            when (state.mode) {
                                "AA" -> "人均 · 线下自理"
                                "轮流" -> "本轮 TA 请 · 下轮你请"
                                else -> "本次你请 · 会计入信用"
                            },
                            Modifier.padding(bottom = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = SpotPalColor.Sub,
                        )
                    }
                    if (state.confirmed) {
                        Text(
                            "✓ 已锁定 · 费用 ${state.mode} · 人均 ¥${state.perHead.toInt()}（线下自理）",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpotPalColor.Success,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 主 CTA
            when {
                !state.confirmed -> Button(
                    onClick = { vm.confirm(id) },
                    enabled = !state.confirming,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp),
                ) { Text(if (state.confirming) "锁定中…" else "确认锁单") }

                state.orderNote != null -> Text(
                    "打卡成功 · ${state.orderNote}",
                    Modifier.fillMaxWidth(),
                    color = SpotPalColor.Success,
                    style = MaterialTheme.typography.bodyMedium,
                )

                else -> Button(
                    onClick = { vm.checkin(id) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp),
                ) { Text("打卡结算") }
            }
        }
    }
}
