package com.spotpal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spotpal.core.design.SpotPalTheme
import com.spotpal.feature.confirm.ConfirmScreen
import com.spotpal.feature.discover.DiscoverScreen
import com.spotpal.feature.me.MeScreen
import com.spotpal.feature.negotiate.NegotiateScreen
import com.spotpal.feature.profile.ProfileScreen
import com.spotpal.feature.squads.SquadsScreen

/**
 * 主入口：3 Tab + 6 屏（详细设计 §3.2 导航结构）。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpotPalTheme { RootNavHost() }
        }
    }
}

private data class Tab(val route: String, val label: String)

private val tabs = listOf(
    Tab("discover", "发现"),
    Tab("squads", "搭局"),
    Tab("me", "我的"),
)

@Composable
fun RootNavHost() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {},
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = "discover",
            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
        ) {
            composable("discover") { DiscoverScreen(nav) }
            composable("profile/{uid}") { ProfileScreen(nav, it.arguments?.getString("uid") ?: "") }
            composable("squads") { SquadsScreen(nav) }
            composable("negotiate/{id}") { NegotiateScreen(nav, it.arguments?.getString("id") ?: "") }
            composable("confirm/{id}") { ConfirmScreen(nav, it.arguments?.getString("id") ?: "") }
            composable("me") { MeScreen(nav) }
        }
    }
}
