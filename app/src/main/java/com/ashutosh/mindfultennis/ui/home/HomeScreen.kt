package com.ashutosh.mindfultennis.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashutosh.mindfultennis.ui.home.components.AspectPerformanceCard
import com.ashutosh.mindfultennis.ui.home.components.FocusPointsRow
import com.ashutosh.mindfultennis.ui.home.components.PerformanceChart
import com.ashutosh.mindfultennis.ui.home.components.TimeRangeSegmentedControl
import com.ashutosh.mindfultennis.ui.home.components.WinLossCard
import com.ashutosh.mindfultennis.ui.theme.Spacing
import com.ashutosh.mindfultennis.util.DateTimeUtils

/**
 * Home / Dashboard screen.
 * Displays performance trend chart, win/loss record, focus points,
 * and aspect performance averages. Bottom buttons for sessions navigation.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartSessionClicked: () -> Unit,
    onEndSessionClicked: (sessionId: String) -> Unit,
    onShowSessionsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        state = uiState,
        onEvent = { event ->
            when (event) {
                is HomeUiEvent.StartSessionClicked -> onStartSessionClicked()
                is HomeUiEvent.EndSessionClicked -> {
                    uiState.activeSession?.let { onEndSessionClicked(it.id) }
                }
                is HomeUiEvent.ShowSessionsClicked -> onShowSessionsClicked()
                is HomeUiEvent.CancelSessionClicked,
                is HomeUiEvent.CancelSessionConfirmed,
                is HomeUiEvent.CancelSessionDismissed -> viewModel.onEvent(event)
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasActiveSession = state.activeSession != null

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { /* TODO: settings */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Main scrollable content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = Spacing.sm,
                    bottom = Spacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // Global time-range selector
                item(key = "time_range") {
                    TimeRangeSegmentedControl(
                        selected = state.selectedDuration,
                        onSelected = { onEvent(HomeUiEvent.DurationChanged(it)) },
                    )
                }

                // Active session banner
                if (hasActiveSession) {
                    item(key = "active_banner") {
                        ActiveSessionBanner(
                            startedAt = state.activeSession!!.startedAt,
                            elapsedMs = state.activeSessionElapsedMs,
                        )
                    }
                }

                // Performance Trend Chart
                item(key = "performance_chart") {
                    PerformanceChart(
                        trend = state.performanceTrend,
                        selectedDuration = state.selectedDuration,
                        isLoading = state.isLoading,
                        error = state.trendError,
                        onRetry = { onEvent(HomeUiEvent.RetryClicked) },
                    )
                }

                // Win / Loss Card
                item(key = "win_loss") {
                    WinLossCard(
                        record = state.winLossRecord,
                        opponents = state.opponents,
                        selectedOpponentIds = state.selectedWinLossOpponentIds,
                        onOpponentFilterChanged = {
                            onEvent(HomeUiEvent.WinLossOpponentFilterChanged(it))
                        },
                        isLoading = state.isLoading,
                        error = state.winLossError,
                        onRetry = { onEvent(HomeUiEvent.RetryClicked) },
                    )
                }

                // Focus Points
                item(key = "focus_points") {
                    FocusPointsRow(focusPoints = state.focusPoints)
                }

                // Aspect Performance Card
                item(key = "aspect_performance") {
                    AspectPerformanceCard(
                        aspectAverages = state.aspectAverages,
                        selectedRatingType = state.selectedAspectRatingType,
                        onRatingTypeSelected = { onEvent(HomeUiEvent.AspectRatingTypeChanged(it)) },
                        isLoading = state.isLoading,
                        error = state.aspectError,
                        onRetry = { onEvent(HomeUiEvent.RetryClicked) },
                    )
                }
            }

            // Bottom button area
            BottomButtonBar(
                hasActiveSession = hasActiveSession,
                onShowSessions = { onEvent(HomeUiEvent.ShowSessionsClicked) },
                onStartOrEnd = {
                    if (hasActiveSession) {
                        onEvent(HomeUiEvent.EndSessionClicked)
                    } else {
                        onEvent(HomeUiEvent.StartSessionClicked)
                    }
                },
                onCancel = { onEvent(HomeUiEvent.CancelSessionClicked) },
            )
        }

        // Cancel session confirmation dialog
        if (state.showCancelSessionDialog) {
            CancelSessionDialog(
                onConfirm = { onEvent(HomeUiEvent.CancelSessionConfirmed) },
                onDismiss = { onEvent(HomeUiEvent.CancelSessionDismissed) },
            )
        }
    }
}

@Composable
private fun ActiveSessionBanner(
    startedAt: Long,
    elapsedMs: Long,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(Spacing.md),
    ) {
        Column {
            Text(
                text = "Session in progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Started at ${DateTimeUtils.formatTime(startedAt)} \u2022 ${DateTimeUtils.formatDuration(elapsedMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun BottomButtonBar(
    hasActiveSession: Boolean,
    onShowSessions: () -> Unit,
    onStartOrEnd: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.md,
                vertical = Spacing.sm,
            ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedButton(
            onClick = onShowSessions,
            modifier = Modifier.weight(1f),
        ) {
            Text("Show Sessions")
        }

        if (hasActiveSession) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onStartOrEnd,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("End Session")
            }
        } else {
            Button(
                onClick = onStartOrEnd,
                modifier = Modifier.weight(1f),
            ) {
                Text("Start New Session")
            }
        }
    }
}

@Composable
private fun CancelSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Session?") },
        text = {
            Text("This will discard the current session and all its data. This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Cancel Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Playing")
            }
        },
    )
}
