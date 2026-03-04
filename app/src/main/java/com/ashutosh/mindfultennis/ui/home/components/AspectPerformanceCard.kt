package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.domain.model.RatingType
import com.ashutosh.mindfultennis.ui.components.ErrorRetryCard
import com.ashutosh.mindfultennis.ui.components.LoadingShimmer
import com.ashutosh.mindfultennis.ui.theme.Spacing

/**
 * Card displaying a spider-web (radar) chart of aspect performance.
 * Supports showing Self ratings, Partner feedback, or both overlaid
 * with distinct colors plus a legend.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AspectPerformanceCard(
    selfAspectAverages: Map<Aspect, Float>,
    partnerAspectAverages: Map<Aspect, Float>,
    selectedRatingType: RatingType,
    onRatingTypeSelected: (RatingType) -> Unit,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selfColor = MaterialTheme.colorScheme.primary
    val partnerColor = MaterialTheme.colorScheme.tertiary

    // Determine which data to show based on toggle
    val showSelf = selectedRatingType == RatingType.SELF || selectedRatingType == RatingType.BOTH
    val showPartner = selectedRatingType == RatingType.PARTNER || selectedRatingType == RatingType.BOTH

    val visibleSelf = if (showSelf) selfAspectAverages else emptyMap()
    val visiblePartner = if (showPartner) partnerAspectAverages else emptyMap()
    val hasData = visibleSelf.isNotEmpty() || visiblePartner.isNotEmpty()

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Aspect Performance",
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Rating type toggle chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                RatingType.entries.forEach { ratingType ->
                    FilterChip(
                        selected = ratingType == selectedRatingType,
                        onClick = { onRatingTypeSelected(ratingType) },
                        label = { Text(text = ratingType.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            when {
                isLoading -> {
                    LoadingShimmer(height = 200.dp, barCount = 4)
                }
                error != null -> {
                    ErrorRetryCard(message = error, onRetry = onRetry)
                }
                !hasData -> {
                    Text(
                        text = "No rated sessions yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    // Radar / spider-web chart
                    RadarChart(
                        selfData = visibleSelf,
                        partnerData = visiblePartner,
                        selfColor = selfColor,
                        partnerColor = partnerColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.sm),
                    )

                    // Performance color scale legend
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    PerformanceColorScale(modifier = Modifier.fillMaxWidth())

                    // Legend
                    if (selectedRatingType == RatingType.BOTH) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LegendDot(color = selfColor)
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = "Self",
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            LegendDot(color = partnerColor)
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = "Partner's Feedback",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * Horizontal gradient bar showing the performance color scale: Low → Mid → High.
 */
@Composable
private fun PerformanceColorScale(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Low",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE53935), // red
                            Color(0xFFFFA726), // amber
                            Color(0xFF43A047), // green
                        ),
                    ),
                ),
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = "High",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
