package com.spotpal.feature.negotiate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.spotpal.core.model.BizError
import com.spotpal.core.model.Negotiation
import com.spotpal.core.model.Proposal
import com.spotpal.core.model.Round
import com.spotpal.core.network.ApiGraph
import com.spotpal.core.network.SafeApi
import kotlinx.coroutines.launch

/**
 * ③ AI 协商页（详细设计 §3.3）：
 * 消息流 = LazyColumn（方案卡气泡）；进度条 round/5；「确认 / 微调」双 CTA。
 */

// ---- ViewModel ----

data class NegotiateUiState(
    val loading: Boolean = false,
    val ngt: Negotiation? = null,
    val error: String? = null,
    val feedbackSending: Boolean = false,
)

class NegotiateViewModel(
    private val api: SafeApi = ApiGraph.api,
    private val id: String,
) : ViewModel() {

    var state by mutableStateOf(NegotiateUiState())
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            val result = api.negotiation(id)
            state = if (result.isSuccess) {
                state.copy(loading = false, ngt = result.getOrThrow())
            } else {
                state.copy(loading = false, error = "协商详情加载失败")
            }
        }
    }

    fun feedback(ok: Boolean, note: String, onConfirmed: (squadId: String) -> Unit) {
        viewModelScope.launch {
            state = state.copy(feedbackSending = true)
            val result = api.feedback(id, ok, note)
            state = state.copy(feedbackSending = false)
            if (result.isSuccess) {
                if (ok) {
                    // 双方 ok → 成局 → 跳确认页
                    state.ngt?.squadId?.let(onConfirmed)
                } else {
                    refresh() // 下一轮方案已生成
                }
            }
        }
    }
}

// ---- UI ----

@Composable
fun NegotiateScreen(nav: NavHostController, id: String) {
    val vm = remember(id) { NegotiateViewModel(id = id) }
    val state = vm.state
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = SpotPalColor.Bg,
        bottomBar = {
            if (state.ngt?.status == "ACTIVE") {
                // 双 CTA（设计稿：确认 / 微调）
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { vm.feedback(ok = true, note = "") { squadId -> nav.navigate("confirm/$squadId") } },
                        enabled = !state.feedbackSending,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp),
                    ) { Text("确认方案") }
                    OutlinedButton(
                        onClick = { vm.feedback(ok = false, note = "时间改周六") { _ -> } },
                        enabled = !state.feedbackSending,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp),
                    ) { Text("微调") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // 顶栏：返回 + 协商进度 round/5
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { nav.popBackStack() }) { Text("返回") }
                Spacer(Modifier.width(4.dp))
                Text("AI 协商", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                val round = state.ngt?.round ?: 0
                Text(
                    "第 $round/5 轮 · 48h 内有效",
                    style = MaterialTheme.typography.labelMedium,
                    color = SpotPalColor.Sub,
                )
            }
            // 进度条（round/5）
            LinearProgressIndicator(
                progress = { ((state.ngt?.round ?: 0) / 5f).coerceIn(0.05f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                color = SpotPalColor.Primary,
            )

            when {
                state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    val rounds = state.ngt?.rounds ?: emptyList()
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(rounds, key = { it.round }) { round ->
                            RoundBubble(round)
                        }
                        item {
                            Text(
                                "AI 搭子已就位 · 上来就谈方案，不闲聊",
                                style = MaterialTheme.typography.labelSmall,
                                color = SpotPalColor.Sub,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 一轮 = 方案卡气泡（时间/场地/费用/理由 + AI 头像）。 */
@Composable
private fun RoundBubble(round: Round) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth(0.85f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp),
                color = SpotPalColor.DarkCard,
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("AI", style = MaterialTheme.typography.labelSmall, color = SpotPalColor.PrimaryLt)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("第 ${round.round} 轮方案", style = MaterialTheme.typography.labelSmall, color = SpotPalColor.Sub)
        }
        Spacer(Modifier.height(6.dp))
        round.proposal?.let { ProposalCard(it) }
        round.feedback?.let { fb ->
            Text(
                if (fb.ok) "✓ 双方确认，已锁定方案" else "↻ 你微调了方案（${fb.note ?: ""}），AI 重新协商",
                style = MaterialTheme.typography.labelSmall,
                color = if (fb.ok) SpotPalColor.Success else SpotPalColor.Sub,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 方案卡（设计稿：白卡 + 蓝色高亮字段）。 */
@Composable
fun ProposalCard(p: Proposal) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpotPalColor.Line),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProposalRow("时间", p.time)
            ProposalRow("场地", p.venue)
            ProposalRow("费用", if (p.costMode == "AA") "AA · 人均 ¥${p.perHead.toInt()}" else p.costMode)
            if (p.reason.isNotEmpty()) {
                Text(p.reason, style = MaterialTheme.typography.bodySmall, color = SpotPalColor.Sub)
            }
        }
    }
}

@Composable
private fun ProposalRow(label: String, value: String) {
    Row {
        Text(label, Modifier.width(44.dp), style = MaterialTheme.typography.labelMedium, color = SpotPalColor.Sub)
        Text(value.ifEmpty { "待定" }, style = MaterialTheme.typography.bodyMedium)
    }
}
