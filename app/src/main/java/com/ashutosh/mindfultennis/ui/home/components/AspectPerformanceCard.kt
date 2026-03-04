package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.domain.model.Opponent
import com.ashutosh.mindfultennis.ui.components.ErrorRetryCard
import com.ashutosh.mindfultennis.ui.components.LoadingShimmer
import com.ashutosh.mindfultennis.ui.theme.Spacing
import java.util.Locale

/**
 * Card displaying average self-ratings for each of the 8 tennis aspects.
 * Each row shows the aspect name, a star bar (1-5), and numeric average.
 */
@Composable
fun AspectPerformanceCard(
    aspectAverages: Map<Aspect, Float>,
    opponents: List<Opponent>,
    selectedOpponentIds: Set<String>,
    onOpponentFilterChanged: (Set<String>) -> Unit,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Title row with filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Aspect Performance",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (opponents.isNotEmpty()) {
                    AspectOpponentFilter(
                        opponents = opponents,
                        selectedIds = selectedOpponentIds,
                        onSelectionChanged = onOpponentFilterChanged,
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

@Composable
private fun AspectOpponentFilter(
    opponents: List<Opponent>,
    selectedIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter by opponent",
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
        if (opponents.isEmpty()) {
            DropdownMenuItem(
                text = { Text("No opponents recorded yet.") },
                onClick = { },
                enabled = false,
            )
        } else {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "All opponents",
                        fontWeight = if (selectedIds.isEmpty()) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                onClick = {
                    onSelectionChanged(emptySet())
                    expanded = false
                },
            )
            opponents.forEach { opponent ->
                val isSelected = opponent.id in selectedIds
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opponent.name,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        val newSet = if (isSelected) {
                            selectedIds - opponent.id
                        } else {
                            selectedIds + opponent.id
                        }
                        onSelectionChanged(newSet)
                    },
                )
            }
        }
        }
    }
}
