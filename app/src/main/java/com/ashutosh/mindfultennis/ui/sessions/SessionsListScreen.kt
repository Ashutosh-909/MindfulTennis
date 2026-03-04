package com.ashutosh.mindfultennis.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.ui.components.ErrorRetryCard
import com.ashutosh.mindfultennis.ui.components.LoadingShimmer
import com.ashutosh.mindfultennis.ui.home.components.DurationFilterChips
import com.ashutosh.mindfultennis.ui.theme.Spacing
import com.ashutosh.mindfultennis.util.DateTimeUtils
import com.ashutosh.mindfultennis.util.ScoreCalculator

/**
 * Sessions List screen showing all completed sessions with color-coded performance indicators.
 */
@Composable
fun SessionsListScreen(
    viewModel: SessionsListViewModel,
    onSessionClicked: (sessionId: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SessionsListContent(
        state = uiState,
        onEvent = { event ->
            when (event) {
                is SessionsListUiEvent.SessionClicked -> onSessionClicked(event.sessionId)
                else -> viewModel.onEvent(event)
            }
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionsListContent(
    state: SessionsListUiState,
    onEvent: (SessionsListUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Sessions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
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
            // Duration filter chips
            DurationFilterChips(
                selectedDuration = state.selectedDuration,
                onDurationSelected = { onEvent(SessionsListUiEvent.DurationChanged(it)) },
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )

            when {
                state.isLoading -> {
                    LoadingShimmer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                    )
                }
                state.error != null -> {
                    ErrorRetryCard(
                        message = state.error,
                        onRetry = { onEvent(SessionsListUiEvent.RetryClicked) },
                        modifier = Modifier.padding(Spacing.md),
                    )
                }
                state.sessions.isEmpty() -> {
                    EmptySessionsMessage(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.xl),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Spacing.md,
                            end = Spacing.md,
                            top = Spacing.sm,
                            bottom = Spacing.md,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(
                            items = state.sessions,
                            key = { it.id },
                        ) { session ->
                            SessionCard(
                                session = session,
                                onClick = { onEvent(SessionsListUiEvent.SessionClicked(session.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySessionsMessage(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "No sessions yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Start your first session from the Dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A session card with a color strip on the left edge indicating performance level.
 * Displays date, duration, score, win/loss (from set scores embedded in the overallScore),
 * and focus note.
 */
@Composable
private fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoreColor = ScoreCalculator.sessionColor(session.overallScore)
    val scoreLabel = ScoreCalculator.sessionLabel(session.overallScore)
    val scoreIcon = sessionIcon(session.overallScore)

    val dateText = DateTimeUtils.formatDate(session.startedAt, session.timeZoneId)

    val durationText = if (session.endedAt != null) {
        DateTimeUtils.formatDuration(session.endedAt - session.startedAt)
    } else {
        ""
    }

    val accessibilityDescription = buildString {
        append("Session on $dateText")
        if (durationText.isNotBlank()) append(", duration $durationText")
        append(", performance: $scoreLabel")
        if (session.overallScore != null) append(", score ${session.overallScore}")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = accessibilityDescription },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Color strip on left edge
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.small)
                    .background(scoreColor),
            )

            // Card content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.md),
            ) {
                // Row 1: Date + Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (durationText.isNotBlank()) {
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                // Row 2: Score label + icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = scoreIcon,
                        contentDescription = scoreLabel,
                        tint = scoreColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = if (session.overallScore != null) {
                            "Score: ${session.overallScore} • $scoreLabel"
                        } else {
                            scoreLabel
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = scoreColor,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Row 3: Focus note (if present)
                if (session.focusNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Focus: ${session.focusNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Returns appropriate icon for the session's performance score range.
 * Provides non-color visual cue for accessibility.
 */
private fun sessionIcon(overallScore: Int?): ImageVector {
    return when {
        overallScore == null -> Icons.AutoMirrored.Filled.HelpOutline
        overallScore >= 70 -> Icons.Default.CheckCircle
        overallScore >= 40 -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.Default.Error
    }
}
