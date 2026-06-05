package com.cznwiki.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object CharacterList : Screen("characters", "角色图鉴", Icons.Default.Person)
    data object MyCollection : Screen("collection", "我的收藏", Icons.Default.Star)
    data object CharacterDetail : Screen("character/{characterId}", "角色详情", Icons.Default.Person) {
        fun createRoute(characterId: Int) = "character/$characterId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.CharacterList,
    Screen.MyCollection
)
