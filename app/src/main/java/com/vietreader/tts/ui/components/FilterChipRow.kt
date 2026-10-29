package com.vietreader.tts.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vietreader.tts.domain.model.LibraryFilter
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.SurfaceContainer

data class FilterChipUiModel(
    val filter: LibraryFilter,
    val label: String
)

/**
 * Horizontal scrollable filter chip row extracted from `stitch/01-thu-vien.html`.
 */
@Composable
fun FilterChipRow(
    chips: List<FilterChipUiModel>,
    selectedFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips, key = { it.filter.id }) { chip ->
            AppFilterChip(
                label = chip.label,
                selected = chip.filter == selectedFilter,
                onClick = { onFilterSelected(chip.filter) }
            )
        }
    }
}

@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) PrimaryContainer else SurfaceContainer,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            color = if (selected) OnPrimary else OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
