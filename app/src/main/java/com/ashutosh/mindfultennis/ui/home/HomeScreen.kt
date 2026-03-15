package com.ashutosh.mindfultennis.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.R
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
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Main scrollable content with pull-to-refresh
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.isSyncing,
                onRefresh = { onEvent(HomeUiEvent.RefreshClicked) },
                state = pullState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                indicator = {
                    TennisBallRefreshIndicator(
                        isRefreshing = state.isSyncing,
                        state = pullState,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                },
            ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
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
                        selfAspectAverages = state.selfAspectAverages,
                        partnerAspectAverages = state.partnerAspectAverages,
                        selectedRatingType = state.selectedAspectRatingType,
                        onRatingTypeSelected = { onEvent(HomeUiEvent.AspectRatingTypeChanged(it)) },
                        isLoading = state.isLoading,
                        error = state.aspectError,
                        onRetry = { onEvent(HomeUiEvent.RetryClicked) },
                    )
                }
            }
            } // end PullToRefreshBox

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
    if (hasActiveSession) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.sm,
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onShowSessions,
                modifier = Modifier.weight(1f),
            ) {
                Text("Sessions")
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text("Cancel")
            }

            FilledIconButton(
                onClick = onStartOrEnd,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "End Session",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.sm,
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onShowSessions,
                modifier = Modifier.weight(1f),
            ) {
                Text("Sessions")
            }

            Button(
                onClick = onStartOrEnd,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text("Log Session")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TennisBallRefreshIndicator(
    isRefreshing: Boolean,
    state: androidx.compose.material3.pulltorefresh.PullToRefreshState,
    modifier: Modifier = Modifier,
) {
    val distanceFraction = state.distanceFraction.coerceIn(0f, 1f)

    // Spin continuously while refreshing
    val infiniteTransition = rememberInfiniteTransition(label = "tennis_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
        ),
        label = "spin",
    )

    // Show when pulling or refreshing
    if (distanceFraction > 0f || isRefreshing) {
        Box(
            modifier = modifier
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val rotation = if (isRefreshing) spinAngle else distanceFraction * 360f
            val scale = if (isRefreshing) 1f else (0.5f + distanceFraction * 0.5f)

            Image(
                painter = painterResource(id = R.drawable.ic_tennis_ball),
                contentDescription = "Refreshing",
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
    }
}
