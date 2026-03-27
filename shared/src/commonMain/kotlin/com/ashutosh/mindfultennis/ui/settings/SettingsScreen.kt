package com.ashutosh.mindfultennis.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.ui.theme.Spacing
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) {
            onLoggedOut()
        }
    }

    LaunchedEffect(uiState.syncResult) {
        uiState.syncResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(SettingsUiEvent.SyncResultDismissed)
        }
    }

    SettingsScreenContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md),
        ) {
            Spacer(Modifier.height(Spacing.md))

            // User info section
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.sm))

            if (state.displayName != null) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (state.email != null) {
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            // Sync section
            Text(
                text = "Data Sync",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.sm))

            if (state.lastSyncTime != null) {
                Text(
                    text = "Last synced: ${formatSyncTime(state.lastSyncTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
            }

            OutlinedButton(
                onClick = { onEvent(SettingsUiEvent.SyncClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSyncing,
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Syncing…")
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Sync Now")
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            // Sign out button
            Button(
                onClick = { onEvent(SettingsUiEvent.SignOutClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSigningOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                if (state.isSigningOut) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = Spacing.xs / 2,
                    )
                } else {
                    Text("Sign Out")
                }
            }
        }
    }
}

private fun formatSyncTime(epochMs: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(epochMs)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${local.dayOfMonth}, ${local.year} at ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    } catch (_: Exception) {
        "Unknown"
    }
}
