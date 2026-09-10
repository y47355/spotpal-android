package com.spotpal.feature.squads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.spotpal.core.model.Squad
import com.spotpal.core.network.ApiGraph
import com.spotpal.core.network.SafeApi
import kotlinx.coroutines.launch

/**
 * ⑤ 我的搭局（详细设计 §3.3）：
 * 进行中卡倒计时；EXP（超时回流）态置灰 + 回流提示；
 * 状态迁移由 WS squad.status 驱动，REST 兜底刷新。
 */

data class SquadsUiState(
    val loading: Boolean = false,
    val squads: List<Squad> = emptyList(),
    val error: String? = null,
)

class SquadsViewModel(private val api: SafeApi = ApiGraph.api) : ViewModel() {

    var state by mutableStateOf(SquadsUiState())
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            val result = api.mySquads()
            state = if (result.isSuccess) {
                state.copy(loading = false, squads = result.getOrThrow().squads)
            } else {
                state.copy(loading = false, error = "搭局列表加载失败")
            }
        }
    }
}

// ---- UI ----

@Composable
fun SquadsScreen(nav: NavHostController) {
    val vm = remember { SquadsViewModel() }
    val state = vm.state
    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(containerColor = SpotPalColor.Bg) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "我的搭局",
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            when {
                state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.squads.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("还没有搭局", color = SpotPalColor.Sub)
                        Text("去发现页上推一个搭子吧", style = MaterialTheme.typography.bodySmall, color = SpotPalColor.Sub)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.squads, key = { it.id }) { squad ->
                        SquadCard(squad) { nav.navigate("negotiate/ngt_${squad.id.removePrefix("sq_")}") }
                    }
                }
            }
        }
    }
}

/** 搭局卡（状态着色：NEGOTIATING 蓝 / CONFIRMED 绿 / EXPIRED 置灰）。 */
@Composable
private fun SquadCard(squad: Squad, onClick: () -> Unit) {
    val expired = squad.status == "EXPIRED"
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (expired) SpotPalColor.Sep else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpotPalColor.Line),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    squad.activity.ifEmpty { "羽毛球" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (expired) SpotPalColor.Sub else SpotPalColor.Ink,
                )
                Spacer(Modifier.weight(1f))
                StatusBadge(squad.status)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("周六 20:00", style = MaterialTheme.typography.bodySmall, color = SpotPalColor.Sub)
                Text("·", color = SpotPalColor.Sub)
                Text(squad.costMode ?: "AA", style = MaterialTheme.typography.bodySmall, color = SpotPalColor.Sub)
            }
            if (expired) {
                Text("超时未成局 · 已回流发现页", style = MaterialTheme.typography.labelSmall, color = SpotPalColor.Sub)
            } else if (squad.status == "NEGOTIATING") {
                Text("48h 内确认有效", style = MaterialTheme.typography.labelSmall, color = SpotPalColor.Primary)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, bg, fg) = when (status) {
        "NEGOTIATING" -> Triple("协商中", SpotPalColor.BadgeBlueBg, SpotPalColor.Primary)
        "CONFIRMED" -> Triple("已成局", Color(0xFFE8F8EE), SpotPalColor.Success)
        "SETTLED" -> Triple("已结算", Color(0xFFE8F8EE), SpotPalColor.Success)
        "EXPIRED" -> Triple("已超时", SpotPalColor.Sep, SpotPalColor.Sub)
        else -> Triple(status, SpotPalColor.Sep, SpotPalColor.Sub)
    }
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp), color = bg) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = fg)
    }
}
