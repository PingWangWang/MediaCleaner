/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * Compose 导航图，定义所有页面路由
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.photocleaner.app.ui.result.ResultScreen
import com.photocleaner.app.ui.scan.ScanScreen
import com.photocleaner.app.ui.settings.SettingsScreen

/**
 * 底部导航栏项定义。
 *
 * @property route  导航路由
 * @property label  显示的标签文本
 * @property icon   显示的图标
 */
private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

/** 底部导航栏显示的四个主屏幕 */
private val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.HOME, "首页", Icons.Default.Home),
    BottomNavItem(NavRoutes.SCAN, "扫描", Icons.Default.ClearAll),
    BottomNavItem(NavRoutes.RECYCLE_BIN, "回收站", Icons.Default.Delete),
    BottomNavItem(NavRoutes.SETTINGS, "设置", Icons.Default.Settings)
)

/** 不显示底部导航栏的路由集合 */
private val routesWithoutBottomBar = setOf(NavRoutes.RESULT, NavRoutes.DETAIL)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 只在主屏幕显示底部导航栏
    val showBottomBar = currentRoute !in routesWithoutBottomBar

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ---------- 主页 ----------
            composable(NavRoutes.HOME) {
                HomeScreen(
                    onStartScan = { navController.navigate(NavRoutes.SCAN) },
                    onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) },
                    onOpenRecycleBin = { navController.navigate(NavRoutes.RECYCLE_BIN) }
                )
            }

            // ---------- 扫描页 ----------
            composable(NavRoutes.SCAN) {
                ScanScreen(
                    onScanComplete = { groups ->
                        // 将扫描结果存入共享单例，跳转结果页
                        ScanResultHolder.groups = groups
                        navController.navigate(NavRoutes.RESULT) {
                            // 移除扫描页，避免按返回键回到扫描状态
                            popUpTo(NavRoutes.SCAN) { inclusive = true }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            // ---------- 结果页 ----------
            composable(NavRoutes.RESULT) {
                ResultScreen(
                    onItemClick = { groupId ->
                        // 找到被点击的分组，存入共享单例供详情页使用
                        ScanResultHolder.selectedGroup =
                            ScanResultHolder.groups.firstOrNull { it.groupId == groupId }
                        navController.navigate(NavRoutes.detail(groupId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ---------- 详情页 ----------
            composable(
                route = NavRoutes.DETAIL,
                arguments = listOf(
                    navArgument("groupId") { type = NavType.LongType }
                )
            ) {
                DetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // ---------- 回收站 ----------
            composable(NavRoutes.RECYCLE_BIN) {
                RecycleBinScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // ---------- 设置页 ----------
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
