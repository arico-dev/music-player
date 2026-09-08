package com.musicplayer.app.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicplayer.app.BuildConfig
import com.musicplayer.app.R
import com.musicplayer.app.data.settings.ThemeMode

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val refreshMessage by viewModel.refreshMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var confirmRecents by remember { mutableStateOf(false) }
    var confirmUsage by remember { mutableStateOf(false) }

    LaunchedEffect(refreshMessage) {
        when (refreshMessage) {
            "ok" -> {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_library_done))
                viewModel.consumeRefreshMessage()
            }
            "error" -> {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_library_error))
                viewModel.consumeRefreshMessage()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall
        )

        // Apariencia
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.settings_appearance_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip(label = stringResource(R.string.settings_theme_system), selected = settings.themeMode == ThemeMode.SYSTEM, onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) })
                    ThemeChip(label = stringResource(R.string.settings_theme_light), selected = settings.themeMode == ThemeMode.LIGHT, onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) })
                    ThemeChip(label = stringResource(R.string.settings_theme_dark), selected = settings.themeMode == ThemeMode.DARK, onClick = { viewModel.setThemeMode(ThemeMode.DARK) })
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(text = stringResource(R.string.settings_dynamic_color), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                stringResource(R.string.settings_dynamic_color_desc)
                            else stringResource(R.string.settings_dynamic_color_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    )
                }
            }
        }

        // Biblioteca
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.settings_library), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.settings_library_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(
                    onClick = viewModel::refreshLibrary,
                    enabled = !isRefreshing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.settings_library_refreshing))
                    } else {
                        Text(stringResource(R.string.settings_library_refresh))
                    }
                }
            }
        }

        // Datos
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.settings_data), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.settings_data_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { confirmRecents = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_clear_recents))
                }
                Text(text = stringResource(R.string.settings_clear_recents_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { confirmUsage = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_clear_usage))
                }
                Text(text = stringResource(R.string.settings_clear_usage_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Acerca de
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "com.musicplayer.app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState)
    }

    if (confirmRecents) {
        AlertDialog(
            onDismissRequest = { confirmRecents = false },
            title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRecents = false
                    viewModel.clearRecents()
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_clear_recents_done)) }
                }) { Text(stringResource(R.string.playlist_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRecents = false }) { Text(stringResource(R.string.playlist_cancel)) }
            }
        )
    }
    if (confirmUsage) {
        AlertDialog(
            onDismissRequest = { confirmUsage = false },
            title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmUsage = false
                    viewModel.clearUsage()
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_clear_usage_done)) }
                }) { Text(stringResource(R.string.playlist_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUsage = false }) { Text(stringResource(R.string.playlist_cancel)) }
            }
        )
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}
