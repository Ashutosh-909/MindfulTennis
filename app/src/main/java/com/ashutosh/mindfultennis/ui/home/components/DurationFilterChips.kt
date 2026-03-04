package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.ui.theme.Spacing

/**
 * A row of filter chips for selecting the dashboard time duration.
 * Options: 1W, 1M, 3M, 6M, 1Y
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationFilterChips(
    selectedDuration: DurationFilter,
    onDurationSelected: (DurationFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        DurationFilter.entries.forEach { duration ->
            FilterChip(
                selected = duration == selectedDuration,
                onClick = { onDurationSelected(duration) },
                label = { Text(text = duration.label) },
            )
        }
    }
}
