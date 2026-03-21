package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.DurationFilter

/**
 * Global time-range segmented control for the dashboard.
 * Dark-themed pill selector: 1W | 1M | 3M | 6M | 1Y.
 */
@Composable
fun TimeRangeSegmentedControl(
    selected: DurationFilter,
    onSelected: (DurationFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segmentBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val segmentSelectedBg = MaterialTheme.colorScheme.primary
    val selectedTextColor = MaterialTheme.colorScheme.onPrimary
    val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = segmentBg, shape = RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        DurationFilter.entries.forEach { duration ->
            val isSelected = duration == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) segmentSelectedBg else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelected(duration) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = duration.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) selectedTextColor else unselectedTextColor,
                )
            }
        }
    }
}
