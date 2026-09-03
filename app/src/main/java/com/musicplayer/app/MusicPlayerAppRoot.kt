package com.musicplayer.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.musicplayer.app.feature.library.LibraryScreen
import com.musicplayer.app.feature.player.PlayerScreen

object Routes {
    const val ROOT = "root"
    const val PLAYER = "player"
}

@Composable
fun MusicPlayerAppRoot() {
    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ROOT,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ROOT) {
                LibraryScreen(
                    onSongClick = { navController.navigate(Routes.PLAYER) }
                )
            }
            composable(Routes.PLAYER) { PlayerScreen() }
        }
    }
}
