package com.cznwiki.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object CharacterList : Screen("characters", "角色图鉴", Icons.Default.Person)
    data object MyCollection : Screen("collection", "我的收藏", Icons.Default.Star)
    data object CharacterDetail : Screen("character/{characterId}", "角色详情") {
        fun createRoute(characterId: Int) = "character/$characterId"
    }
    data object Events : Screen("events", "活动列表")
    data object Banners : Screen("banners", "卡池一览")
    data object Teams : Screen("teams", "队伍构筑")
    data object Backup : Screen("backup", "数据备份")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.CharacterList,
    Screen.MyCollection
)
