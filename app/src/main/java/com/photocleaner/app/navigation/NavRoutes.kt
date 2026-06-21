package com.photocleaner.app.navigation

/**
 * 导航路由常量定义。
 *
 * 集中管理所有路由路径，避免各 Composable 中硬编码字符串。
 * 带参数的路由提供对应的函数以生成具体导航路径。
 */
object NavRoutes {
    const val HOME = "home"
    const val SCAN = "scan"
    const val RESULT = "result"
    const val DETAIL = "detail/{groupId}"
    const val RECYCLE_BIN = "recycle_bin"
    const val SETTINGS = "settings"

    /** 生成带 groupId 的详情页路由，例如 detail/42 */
    fun detail(groupId: Long): String = "detail/$groupId"
}
