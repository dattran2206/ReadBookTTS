package com.vietreader.tts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vietreader.tts.domain.model.LibrarySortOption
import com.vietreader.tts.domain.model.LibraryViewMode
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.Surface
import com.vietreader.tts.ui.theme.SurfaceContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh

/**
 * Filter & Sort Bottom Sheet (`#filter-bottom-sheet`) extracted from `stitch/01-thu-vien.html`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterBottomSheet(
    viewMode: LibraryViewMode,
    sortOption: LibrarySortOption,
    onViewModeSelected: (LibraryViewMode) -> Unit,
    onSortOptionSelected: (LibrarySortOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row: "Bộ lọc & Sắp xếp" + close button
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bộ lọc & Sắp xếp",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = OnSurface
                    )

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Đóng",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = SurfaceContainerHigh.copy(alpha = 0.6f)
                )
            }

            // Section 1: KIỂU XEM
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "KIỂU XEM",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    ),
                    color = OnSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViewModeButton(
                        mode = LibraryViewMode.GRID,
                        isSelected = viewMode == LibraryViewMode.GRID,
                        onClick = { onViewModeSelected(LibraryViewMode.GRID) },
                        modifier = Modifier.weight(1f)
                    )
                    ViewModeButton(
                        mode = LibraryViewMode.LIST,
                        isSelected = viewMode == LibraryViewMode.LIST,
                        onClick = { onViewModeSelected(LibraryViewMode.LIST) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 2: SẮP XẾP THEO
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "SẮP XẾP THEO",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    ),
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LibrarySortOption.entries.forEach { option ->
                    val isSelected = option == sortOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) PrimaryContainer.copy(alpha = 0.15f) else Color.Transparent
                            )
                            .clickable { onSortOptionSelected(option) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            ),
                            color = if (isSelected) PrimaryContainer else OnSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = PrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeButton(
    mode: LibraryViewMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (mode) {
        LibraryViewMode.GRID -> Icons.Outlined.GridView
        LibraryViewMode.LIST -> Icons.AutoMirrored.Outlined.ViewList
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryContainer else SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) OnPrimary else OnSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = mode.label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            color = if (isSelected) OnPrimary else OnSurfaceVariant
        )
    }
}
