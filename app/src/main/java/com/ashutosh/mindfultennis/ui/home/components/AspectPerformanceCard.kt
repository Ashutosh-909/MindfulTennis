package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.RatingType
import com.ashutosh.mindfultennis.ui.components.ErrorRetryCard
import com.ashutosh.mindfultennis.ui.components.LoadingShimmer
import com.ashutosh.mindfultennis.ui.theme.Spacing
import java.util.Locale

/**
 * Card displaying average ratings for each of the 8 tennis aspects.
 * Includes independent date range filter chips and rating type filter chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AspectPerformanceCard(
    aspectAverages: Map<Aspect, Float>,
    selectedDuration: DurationFilter,
    onDurationSelected: (DurationFilter) -> Unit,
    selectedRatingType: RatingType,
    onRatingTypeSelected: (RatingType) -> Unit,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Aspect Performance",
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Duration filter chips
            DurationFilterChips(
                selectedDuration = selectedDuration,
                onDurationSelected = onDurationSelected,
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Rating type filter chips
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
                aspectAverages.isEmpty() -> {
                    Text(
                        text = "No rated sessions yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Aspect.entries.forEach { aspect ->
                        val avg = aspectAverages[aspect] ?: 0f
                        AspectRow(
                            aspect = aspect,
                            average = avg,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectRow(
    aspect: Aspect,
    average: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = aspect.name.lowercase()
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(90.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        StarBar(rating = average)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = String.format(Locale.US, "%.1f", average),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StarBar(
    rating: Float,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
) {
    val starColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.outlineVariant

    Row(modifier = modifier) {
        repeat(maxStars) { index ->
            val filled = (index + 1) <= rating
            Icon(
                imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (filled) starColor else emptyColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
