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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.GameResult
import com.ashutosh.mindfultennis.domain.model.Opponent
import com.ashutosh.mindfultennis.domain.model.WinLossMode
import com.ashutosh.mindfultennis.domain.model.WinLossRecord
import com.ashutosh.mindfultennis.ui.components.ErrorRetryCard
import com.ashutosh.mindfultennis.ui.components.LoadingShimmer
import com.ashutosh.mindfultennis.ui.theme.SessionGood
import com.ashutosh.mindfultennis.ui.theme.SessionPoor
import com.ashutosh.mindfultennis.ui.theme.SessionUnrated
import com.ashutosh.mindfultennis.ui.theme.Spacing

/**
 * Win/Loss record card for the dashboard.
 * Displays wins, losses, and win percentage with optional opponent filter.
 */
@Composable
fun WinLossCard(
    record: WinLossRecord?,
    opponents: List<Opponent>,
    selectedOpponentIds: Set<String>,
    onOpponentFilterChanged: (Set<String>) -> Unit,
    mode: WinLossMode,
    onModeChanged: (WinLossMode) -> Unit,
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
                    text = "Win / Loss",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (opponents.isNotEmpty()) {
                        OpponentFilterButton(
                            opponents = opponents,
                            selectedIds = selectedOpponentIds,
                            onSelectionChanged = onOpponentFilterChanged,
                        )
                    }
                }
            }

            // Matches / Sets toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                WinLossMode.entries.forEach { entry ->
                    FilterChip(
                        selected = mode == entry,
                        onClick = { onModeChanged(entry) },
                        label = { Text(entry.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            when {
                isLoading -> {
                    LoadingShimmer(height = 48.dp, barCount = 1)
                }
                error != null -> {
                    ErrorRetryCard(message = error, onRetry = onRetry)
                }
                record == null || record.total == 0 -> {
                    Text(
                        text = "No sessions with set scores yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    WinLossContent(record = record)
                }
            }
        }
    }
}

@Composable
private fun WinLossContent(
    record: WinLossRecord,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Stats: W / L / D
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = "W: ${record.wins}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "L: ${record.losses}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            if (record.draws > 0) {
                Text(
                    text = "D: ${record.draws}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Faint vertical divider
        if (record.recentResults.isNotEmpty()) {
            Spacer(modifier = Modifier.width(Spacing.md))
            VerticalDivider(
                modifier = Modifier.height(24.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(modifier = Modifier.width(Spacing.md))
        }

        // Recent results domino strip
        if (record.recentResults.isNotEmpty()) {
            RecentResultsDominos(results = record.recentResults)
        }
    }
}

/**
 * A row of colored tiles/dominos representing recent game results.
 * Green = Win, Red = Loss, Grey = Draw.
 */
@Composable
private fun RecentResultsDominos(
    results: List<GameResult>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        results.forEach { result ->
            val color = when (result) {
                GameResult.WIN -> SessionGood
                GameResult.LOSS -> SessionPoor
                GameResult.DRAW -> SessionUnrated
            }
            val label = when (result) {
                GameResult.WIN -> "W"
                GameResult.LOSS -> "L"
                GameResult.DRAW -> "D"
            }
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpponentFilterButton(
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
            // "All" option to clear filter
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
