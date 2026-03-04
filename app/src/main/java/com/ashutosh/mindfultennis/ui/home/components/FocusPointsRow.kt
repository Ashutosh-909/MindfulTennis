package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ashutosh.mindfultennis.domain.model.FocusPoint
import com.ashutosh.mindfultennis.ui.theme.Spacing

/**
 * Horizontal scrollable row of focus point chips for the dashboard.
 */
@Composable
fun FocusPointsRow(
    focusPoints: List<FocusPoint>,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Focus Points",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            if (focusPoints.isEmpty()) {
                Text(
                    text = "Add focus points when you start a session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    items(
                        items = focusPoints,
                        key = { it.id },
                    ) { focusPoint ->
                        AssistChip(
                            onClick = { },
                            label = { Text(focusPoint.text) },
                        )
                    }
                }
            }
        }
    }
}
