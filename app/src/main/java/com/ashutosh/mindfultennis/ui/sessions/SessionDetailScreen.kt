package com.ashutosh.mindfultennis.ui.sessions

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.domain.model.MatchType
import com.ashutosh.mindfultennis.domain.model.Rating
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.domain.model.SetScore
import com.ashutosh.mindfultennis.ui.components.ErrorRetryCard
import com.ashutosh.mindfultennis.ui.components.LoadingShimmer
import com.ashutosh.mindfultennis.ui.theme.Spacing
import com.ashutosh.mindfultennis.util.DateTimeUtils
import com.ashutosh.mindfultennis.util.ScoreCalculator

/**
 * Read-only detail view of a completed session.
 * Displays all ratings, notes, set scores, and partner ratings.
 */
@Composable
fun SessionDetailScreen(
    viewModel: SessionDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SessionDetailContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailContent(
    state: SessionDetailUiState,
    onEvent: (SessionDetailUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Session Detail") },
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
        when {
            state.isLoading -> {
                LoadingShimmer(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Spacing.md),
                )
            }
            state.error != null -> {
                ErrorRetryCard(
                    message = state.error,
                    onRetry = { onEvent(SessionDetailUiEvent.RetryClicked) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Spacing.md),
                )
            }
            state.session == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Session not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = Spacing.md,
                        end = Spacing.md,
                        top = Spacing.sm,
                        bottom = Spacing.lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    // Session overview card
                    item(key = "overview") {
                        SessionOverviewCard(session = state.session)
                    }

                    // Match info (if opponent/partner set)
                    if (state.opponent1 != null || state.partner != null) {
                        item(key = "match_info") {
                            MatchInfoCard(
                                matchType = state.session.matchType,
                                opponent1Name = state.opponent1?.name,
                                opponent2Name = state.opponent2?.name,
                                partnerName = state.partner?.name,
                            )
                        }
                    }

                    // Self-ratings
                    if (state.selfRatings.isNotEmpty()) {
                        item(key = "self_ratings") {
                            RatingsCard(
                                title = "Self Ratings",
                                ratings = state.selfRatings,
                            )
                        }
                    }

                    // Partner ratings
                    if (state.partnerRatings.isNotEmpty()) {
                        item(key = "partner_ratings") {
                            RatingsCard(
                                title = "Partner's Feedback",
                                ratings = state.partnerRatings,
                            )
                        }
                    }

                    // Set scores
                    if (state.setScores.isNotEmpty()) {
                        item(key = "set_scores") {
                            SetScoresCard(setScores = state.setScores)
                        }
                    }

                    // Notes
                    if (!state.session.notes.isNullOrBlank()) {
                        item(key = "notes") {
                            NotesCard(notes = state.session.notes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionOverviewCard(
    session: Session,
    modifier: Modifier = Modifier,
) {
    val scoreColor = ScoreCalculator.sessionColor(session.overallScore)
    val scoreLabel = ScoreCalculator.sessionLabel(session.overallScore)
    val scoreIcon = sessionScoreIcon(session.overallScore)

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Date and time range
            val dateText = if (session.endedAt != null) {
                DateTimeUtils.formatSessionDateRange(
                    startMs = session.startedAt,
                    endMs = session.endedAt,
                    timeZoneId = session.timeZoneId,
                )
            } else {
                DateTimeUtils.formatDate(session.startedAt, session.timeZoneId)
            }
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            // Duration
            if (session.endedAt != null) {
                val durationMs = session.endedAt - session.startedAt
                Text(
                    text = "Duration: ${DateTimeUtils.formatDuration(durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Overall score with color and icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = scoreIcon,
                    contentDescription = scoreLabel,
                    tint = scoreColor,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = if (session.overallScore != null) {
                        "Score: ${session.overallScore}/100 • $scoreLabel"
                    } else {
                        scoreLabel
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Focus note
            if (session.focusNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Focus: ${session.focusNote}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MatchInfoCard(
    matchType: MatchType,
    opponent1Name: String?,
    opponent2Name: String?,
    partnerName: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Match Info",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "Type: ${matchType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
            )

            opponent1Name?.let {
                InfoRow(
                    icon = Icons.Default.Person,
                    label = if (matchType == MatchType.DOUBLES) "Opponent 1" else "Opponent",
                    value = it,
                )
            }

            opponent2Name?.let {
                InfoRow(
                    icon = Icons.Default.Person,
                    label = "Opponent 2",
                    value = it,
                )
            }

            partnerName?.let {
                InfoRow(
                    icon = Icons.Default.PersonOutline,
                    label = "Partner",
                    value = it,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun RatingsCard(
    title: String,
    ratings: List<Rating>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Sort by aspect enum order for consistency
            val sortedRatings = ratings.sortedBy { it.aspect.ordinal }

            sortedRatings.forEachIndexed { index, rating ->
                RatingRow(
                    aspectName = formatAspectName(rating.aspect),
                    rating = rating.rating,
                )
                if (index < sortedRatings.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Spacing.xs),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingRow(
    aspectName: String,
    rating: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = aspectName,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { index ->
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (index < rating) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                )
            }
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = "$rating/5",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SetScoresCard(
    setScores: List<SetScore>,
    modifier: Modifier = Modifier,
) {
    val winResult = ScoreCalculator.isWin(setScores)
    val resultText = when (winResult) {
        true -> "Win"
        false -> "Loss"
        null -> ""
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsScore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "Set Scores",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (resultText.isNotBlank()) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (winResult == true) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Set",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "You",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Opponent",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.xs),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            setScores.sortedBy { it.setNumber }.forEach { score ->
                val setWon = score.userScore > score.opponentScore
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Set ${score.setNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${score.userScore}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (setWon) FontWeight.Bold else FontWeight.Normal,
                        color = if (setWon) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${score.opponentScore}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!setWon) FontWeight.Bold else FontWeight.Normal,
                        color = if (!setWon && score.userScore != score.opponentScore) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesCard(
    notes: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Formats an Aspect enum value to a user-friendly display name.
 */
private fun formatAspectName(aspect: Aspect): String {
    return aspect.name.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Returns appropriate icon for the session's performance score.
 */
private fun sessionScoreIcon(overallScore: Int?): ImageVector {
    return when {
        overallScore == null -> Icons.AutoMirrored.Filled.HelpOutline
        overallScore >= 70 -> Icons.Default.CheckCircle
        overallScore >= 40 -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.Default.Error
    }
}
