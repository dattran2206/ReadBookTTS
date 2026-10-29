package com.vietreader.tts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh

enum class AppBottomTab(
    val route: String,
    val label: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
) {
    LIBRARY(
        route = "library",
        label = "Thư viện",
        outlinedIcon = Icons.Outlined.LocalLibrary,
        filledIcon = Icons.Filled.LocalLibrary
    ),
    AMBIENT_MUSIC(
        route = "ambient_music",
        label = "Nhạc nền",
        outlinedIcon = Icons.Outlined.MusicNote,
        filledIcon = Icons.Filled.MusicNote
    ),
    VOICES(
        route = "voices",
        label = "Giọng đọc",
        outlinedIcon = Icons.Outlined.RecordVoiceOver,
        filledIcon = Icons.Filled.RecordVoiceOver
    ),
    SETTINGS(
        route = "settings",
        label = "Cài đặt",
        outlinedIcon = Icons.Outlined.Settings,
        filledIcon = Icons.Filled.Settings
    )
}

/**
 * 4-Tab Bottom Navigation Bar.
 * Tabs: Thư viện, Nhạc nền, Giọng đọc, Cài đặt.
 */
@Composable
fun BottomNavBar(
    selectedTab: AppBottomTab,
    onTabSelected: (AppBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            HorizontalDivider(
                thickness = 1.dp,
                color = SurfaceContainerHigh.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppBottomTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    BottomNavTabItem(
                        tab = tab,
                        isSelected = isSelected,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavTabItem(
    tab: AppBottomTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (isSelected) PrimaryContainer else OnSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) PrimaryContainer.copy(alpha = 0.15f) else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            ),
            color = contentColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
