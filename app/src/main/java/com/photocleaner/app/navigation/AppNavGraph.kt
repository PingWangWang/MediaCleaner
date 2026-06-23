/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * Compose 导航图，定义所有页面路由
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.photocleaner.app.ui.detail.DetailScreen
import com.photocleaner.app.ui.home.HomeScreen
import com.photocleaner.app.ui.recyclebin.RecycleBinScreen
import com.photocleaner.app.ui.settings.SettingsScreen
import com.photocleaner.app.utils.PermissionGate
import kotlinx.coroutines.launch

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.HOME, "首页", Icons.Default.Home),
    BottomNavItem(NavRoutes.RECYCLE_BIN, "回收站", Icons.Default.Delete),
    BottomNavItem(NavRoutes.SETTINGS, "设置", Icons.Default.Settings)
)

private val routesWithoutBottomBar = setOf(NavRoutes.DETAIL)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppNavGraph(
    agreementAccepted: Boolean? = null,
    onAcceptAgreement: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute !in routesWithoutBottomBar

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    PermissionGate(agreementAccepted = agreementAccepted, onAcceptAgreement = onAcceptAgreement) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            // Detail page uses NavHost, main tabs use HorizontalPager
            if (currentRoute == NavRoutes.DETAIL) {
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.DETAIL,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(
                        route = NavRoutes.DETAIL,
                        enterTransition = { fadeIn(tween(300)) },
                        exitTransition = { fadeOut(tween(300)) },
                        arguments = listOf(navArgument("groupId") { type = NavType.LongType })
                    ) {
                        DetailScreen(onBack = { navController.popBackStack() })
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                    userScrollEnabled = true
                ) { page ->
                    when (page) {
                        0 -> HomeScreen()
                        1 -> RecycleBinScreen()
                        2 -> SettingsScreen(onNavigateBack = { scope.launch { pagerState.animateScrollToPage(0) } })
                    }
                }
            }
        }
    }
}
