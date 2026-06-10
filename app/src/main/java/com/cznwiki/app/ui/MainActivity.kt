package com.cznwiki.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.cznwiki.app.ui.navigation.Screen
import com.cznwiki.app.ui.navigation.bottomNavItems
import com.cznwiki.app.ui.screens.*
import com.cznwiki.app.ui.theme.CznWikiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CznWikiTheme {
                CznWikiApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CznWikiApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
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
    ) { innerPadding ->
        val enterTr: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            fadeIn(animationSpec = tween(300)) +
                slideInHorizontally(animationSpec = tween(300)) { it / 4 }
        }
        val exitTr: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            fadeOut(animationSpec = tween(300)) +
                slideOutHorizontally(animationSpec = tween(300)) { -it / 4 }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = enterTr,
            exitTransition = exitTr,
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCharacter = { id ->
                        navController.navigate(Screen.CharacterDetail.createRoute(id))
                    },
                    onNavigateToCharacterList = {
                        navController.navigate(Screen.CharacterList.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToEvents = { navController.navigate(Screen.Events.route) },
                    onNavigateToBanners = { navController.navigate(Screen.Banners.route) },
                    onNavigateToTeams = { navController.navigate(Screen.Teams.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) }
                )
            }
            composable(Screen.CharacterList.route) {
                CharacterListScreen(onNavigateToDetail = { id ->
                    navController.navigate(Screen.CharacterDetail.createRoute(id))
                })
            }
            composable(Screen.MyCollection.route) {
                CollectionScreen(onNavigateToDetail = { id ->
                    navController.navigate(Screen.CharacterDetail.createRoute(id))
                })
            }
            composable(
                route = Screen.CharacterDetail.route,
                arguments = listOf(navArgument("characterId") { type = NavType.IntType })
            ) { backStackEntry ->
                val characterId = backStackEntry.arguments?.getInt("characterId") ?: 1
                CharacterDetailScreen(
                    characterId = characterId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCharacter = { id ->
                        navController.navigate(Screen.CharacterDetail.createRoute(id)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
            composable(Screen.Events.route) {
                EventsScreen()
            }
            composable(Screen.Banners.route) {
                BannersScreen()
            }
            composable(Screen.Teams.route) {
                TeamScreen(onNavigateToDetail = { id ->
                    navController.navigate(Screen.CharacterDetail.createRoute(id))
                })
            }
            composable(Screen.Backup.route) {
                BackupScreen()
            }
        }
    }
}
