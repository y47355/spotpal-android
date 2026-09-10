package com.spotpal.feature.discover

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.spotpal.core.design.SpotPalColor
import com.spotpal.core.design.gesture.PushableCard
import com.spotpal.core.model.BizError
import com.spotpal.core.model.RecommendCard
import com.spotpal.core.network.ApiGraph
import com.spotpal.core.network.SafeApi
import kotlinx.coroutines.launch

/**
 * ① 发现页（详细设计 §2.4）：推荐流 + 上推托付手势 + 候选池乐观更新。
 */

// ---- ViewModel（MVI）----

sealed interface DiscoverIntent {
    data object Refresh : DiscoverIntent
    data class Released(val card: RecommendCard, val passed: Boolean) : DiscoverIntent
}

sealed interface DiscoverEffect {
    data class Toast(val text: String) : DiscoverEffect
    data class NavigateToNegotiation(val id: String) : DiscoverEffect
}

data class DiscoverUiState(
    val loading: Boolean = false,
    val cards: List<RecommendCard> = emptyList(),
    val poolCount: Int = 0,
    val error: String? = null,
)

class DiscoverViewModel(private val api: SafeApi = ApiGraph.api) : ViewModel() {

    var state by mutableStateOf(DiscoverUiState())
        private set

    val effects = androidx.compose.runtime.mutableStateListOf<DiscoverEffect>()

    fun dispatch(intent: DiscoverIntent) {
        when (intent) {
            is DiscoverIntent.Refresh -> refresh()
            is DiscoverIntent.Released -> if (intent.passed) pushToPool(intent.card)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            val result = api.recommend()
            state = if (result.isSuccess) {
                state.copy(loading = false, cards = result.getOrThrow().cards)
            } else {
                state.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.let {
                        (it as? BizError)?.message ?: "加载失败，下拉重试"
                    } ?: "加载失败，下拉重试",
                )
            }
        }
    }

    private fun pushToPool(card: RecommendCard) {
        viewModelScope.launch {
            val result = api.pushToPool(card.userId)
            if (result.isSuccess) {
                state = state.copy(poolCount = state.poolCount + 1)
                effects += DiscoverEffect.Toast("已加入候选池（${state.poolCount} 人）")
                consumeTopCard()
            } else {
                when (val err = result.exceptionOrNull()) {
                    is BizError -> if (err.code == 3002) {
                        effects += DiscoverEffect.Toast("候选池已满（5 人）")
                    } else {
                        effects += DiscoverEffect.Toast(err.message)
                    }
                    else -> effects += DiscoverEffect.Toast("网络不给力，稍后再试")
                }
            }
        }
    }

    private fun consumeTopCard() {
        state = state.copy(cards = state.cards.drop(1))
    }
}

// ---- UI ----

@Composable
fun DiscoverScreen(nav: NavHostController) {
    val vm = remember { DiscoverViewModel() }
    LaunchedEffect(Unit) { vm.dispatch(DiscoverIntent.Refresh) }
    val state = vm.state
    val snackbar = remember { SnackbarHostState() }

    // effect 消费（一次性副作用）
    LaunchedEffect(Unit) {
        snapshotFlow { vm.effects.lastOrNull() }.collect { effect ->
            if (effect != null) {
                when (effect) {
                    is DiscoverEffect.Toast -> snackbar.showSnackbar(effect.text)
                    is DiscoverEffect.NavigateToNegotiation -> nav.navigate("negotiate/${effect.id}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = SpotPalColor.Bg,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // 顶部：标题 + 候选池入口（池计数实时反馈，单手拇指热区）
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("发现搭子", style = MaterialTheme.typography.titleLarge)
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp), color = SpotPalColor.BadgeBlueBg) {
                    Text(
                        "候选池 ${state.poolCount}",
                        Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = SpotPalColor.Primary,
                    )
                }
            }

            when {
                state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.dispatch(DiscoverIntent.Refresh) }) { Text("重试") }
                    }
                }
                else -> Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    state.cards.firstOrNull()?.let { card ->
                        DiscoverCard(card, vm)
                    } ?: Text("暂无推荐，稍后再来", color = SpotPalColor.Sub)
                }
            }
        }
    }
}

@Composable
private fun DiscoverCard(card: RecommendCard, vm: DiscoverViewModel) {
    PushableCard(
        cardId = card.userId,
        onLongPress = { /* 微调面板（②）后续版本接入 */ },
        onReleased = { passed -> vm.dispatch(DiscoverIntent.Released(card, passed)) },
    ) {
        CardContent(card)
    }
}

/** 卡片内容（照设计稿：头像+昵称+距离 / 标签胶囊 / AI 推荐理由暗卡）。 */
@Composable
private fun CardContent(card: RecommendCard) {
    Surface(shape = MaterialTheme.shapes.medium, color = androidx.compose.ui.graphics.Color.White) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp),
                    color = SpotPalColor.Primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(card.nickname.take(1), style = MaterialTheme.typography.titleMedium, color = SpotPalColor.Primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.nickname, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (card.distance > 0) {
                            if (card.distance < 1000) "福田 · ${card.distance.toInt()}m"
                            else "福田 · ${"%.1f".format(card.distance / 1000)}km"
                        } else "福田 · 同商圈",
                        style = MaterialTheme.typography.labelMedium,
                        color = SpotPalColor.Sub,
                    )
                }
            }
            // 标签胶囊行
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                card.tagOverlap.take(3).forEach { tag ->
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp), color = SpotPalColor.BadgeBlueBg) {
                        Text(
                            tag,
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = SpotPalColor.Primary,
                        )
                    }
                }
            }
            // AI 推荐理由（暗卡）
            Surface(shape = MaterialTheme.shapes.small, color = SpotPalColor.DarkCard) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("AI 推荐理由", style = MaterialTheme.typography.labelSmall, color = SpotPalColor.DarkSub)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        card.reason.ifEmpty { "你们都喜欢${card.tagOverlap.firstOrNull() ?: "运动"}，时间窗重合度高" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }
        }
    }
}
