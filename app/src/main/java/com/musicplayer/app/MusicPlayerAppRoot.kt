package com.musicplayer.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musicplayer.app.feature.home.HomeScreen
import com.musicplayer.app.feature.library.LibraryScreen
import com.musicplayer.app.feature.miniplayer.MiniPlayer
import com.musicplayer.app.feature.player.PlayerScreen
import com.musicplayer.app.feature.search.SearchScreen
import com.musicplayer.app.feature.settings.SettingsScreen

object Routes {
    const val ROOT = "root"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PLAYER = "player"

    val bottomTabs = listOf(
        BottomTab(HOME, Icons.Filled.Home, R.string.nav_home),
        BottomTab(LIBRARY, Icons.Filled.LibraryMusic, R.string.nav_library),
        BottomTab(SEARCH, Icons.Filled.Search, R.string.nav_search),
        BottomTab(SETTINGS, Icons.Filled.Settings, R.string.nav_settings)
    )
}

data class BottomTab(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int
)

@Composable
fun MusicPlayerAppRoot() {
    val navController = rememberNavController()
    val rootViewModel: RootViewModel = hiltViewModel()
    val currentSong by rootViewModel.playbackController.currentSong.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onPlayer = currentRoute == Routes.PLAYER

    Scaffold(
        bottomBar = {
            if (!onPlayer) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (currentSong != null) {
                        MiniPlayer(
                            playbackController = rootViewModel.playbackController,
                            onClick = { navController.navigate(Routes.PLAYER) }
                        )
                    }
                    BottomBar(
                        currentRoute = currentRoute,
                        onSelect = { route ->
                            navController.navigate(route) {
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
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.HOME) { HomeScreen() }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onSongClick = { navController.navigate(Routes.PLAYER) }
                )
            }
            composable(Routes.SEARCH) { SearchScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.PLAYER) { PlayerScreen() }
        }
    }
}

@Composable
private fun BottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit
) {
    NavigationBar {
        Routes.bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(text = stringResource(tab.labelRes))
                }
            )
        }
    }
}
