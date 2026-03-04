package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.FocusPoint
import com.ashutosh.mindfultennis.ui.theme.Spacing
import com.ashutosh.mindfultennis.util.ScoreCalculator
import kotlinx.coroutines.launch

/**
 * Horizontal scrollable row of focus point chips for the dashboard.
 * Each chip is color-coded by the average performance score across
 * sessions that used that focus point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusPointsRow(
    focusPoints: List<FocusPoint>,
    modifier: Modifier = Modifier,
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Focus Points",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(
                                text = "Chips are color-coded by your average " +
                                    "performance when using each focus point.\n" +
                                    "Green = Great (≥70)\n" +
                                    "Amber = Average (40–69)\n" +
                                    "Red = Needs Work (<40)\n" +
                                    "Only focus points with rated sessions are shown.",
                            )
                        }
                    },
                    state = tooltipState,
                ) {
                    IconButton(
                        onClick = { scope.launch { tooltipState.show() } },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Focus point color info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))

            if (focusPoints.isEmpty()) {
                Text(
                    text = "Add focus points when you start a session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val ratedFocusPoints = focusPoints.filter { it.averageScore != null }
                if (ratedFocusPoints.isEmpty()) {
                    Text(
                        text = "No focus points with rated sessions yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    items(
                        items = ratedFocusPoints,
                        key = { it.id },
                    ) { focusPoint ->
                        val chipColor = ScoreCalculator.sessionColor(focusPoint.averageScore)

                        AssistChip(
                            onClick = { },
                            label = {
                                Text(focusPoint.text)
                            },
                            border = BorderStroke(1.dp, chipColor),
                        )
                    }
                }
                }
            }
        }
    }
}
