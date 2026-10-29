package com.vietreader.tts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vietreader.tts.domain.model.HighlightLevel
import com.vietreader.tts.domain.model.ReaderMode
import com.vietreader.tts.ui.theme.OnPrimaryContainer
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.Outline
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.PrimaryFixed
import com.vietreader.tts.ui.theme.SurfaceContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerLowest
import com.vietreader.tts.ui.theme.Tertiary

/**
 * Reusable Top Bar component across screens (Library header & Reader sub-screen header).
 * Faithfully matches the minimal header in `stitch/01-thu-vien.html` and `stitch/02-dang-nghe-doc-dong-bo.html`.
 */
@Composable
fun CustomTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    centerContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            if (centerContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    if (navigationIcon != null) {
                        Box(modifier = Modifier.align(Alignment.CenterStart)) {
                            navigationIcon()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 48.dp)
                    ) {
                        centerContent()
                    }

                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        content = actions
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (navigationIcon != null) {
                            navigationIcon()
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = OnSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        content = actions
                    )
                }
            }

            if (bottomContent != null) {
                bottomContent()
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = OutlineVariant.copy(alpha = 0.35f)
            )
        }
    }
}

/**
 * Circular icon button used in the top bar (Search, Filter/Tune, Back, MoreVert, etc.).
 */
@Composable
fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OnSurfaceVariant,
    buttonSize: Dp = 36.dp,
    iconSize: Dp = 21.dp
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Pill-shaped "+ Thêm" action button in the Library header (`h-8 px-3 rounded-full bg-primary-container`).
 */
@Composable
fun TopBarImportButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = PrimaryContainer,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = OnPrimaryContainer,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = OnPrimaryContainer
            )
        }
    }
}

/**
 * Specialized Library Header wrapping [CustomTopBar] with Search, Filter/Sort, and "+ Thêm" actions.
 */
@Composable
fun LibraryTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    onImportClick: () -> Unit,
    filterChipsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    CustomTopBar(
        title = "Thư viện",
        modifier = modifier,
        navigationIcon = {
            AppLogo(
                size = 28.dp,
                cornerRadius = 8.dp
            )
        },
        actions = {
            TopBarIconButton(
                icon = if (isSearchActive) Icons.Outlined.Close else Icons.Outlined.Search,
                contentDescription = "Tìm kiếm sách",
                onClick = onToggleSearch
            )
            TopBarIconButton(
                icon = Icons.Outlined.Tune,
                contentDescription = "Bộ lọc và sắp xếp",
                onClick = onOpenFilterSheet
            )
            TopBarImportButton(
                text = "Thêm",
                onClick = onImportClick
            )
        },
        bottomContent = {
            AnimatedVisibility(visible = isSearchActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Tìm theo tên sách, tác giả, giọng đọc...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
                                cursorBrush = SolidColor(PrimaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Xóa từ khóa",
                                tint = OnSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onSearchQueryChange("") }
                            )
                        }
                    }
                }
            }

            filterChipsContent()
        }
    )
}

/**
 * Specialized Book Detail Header built on top of [CustomTopBar] matching `stitch/03-chi-tiet-sach.html`.
 */
@Composable
fun BookDetailTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Chi tiết sách",
    onDeleteBookClick: (() -> Unit)? = null,
    onNavigateToVoiceManagement: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null
) {
    CustomTopBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            TopBarIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Quay lại",
                onClick = onBackClick,
                tint = OnSurface,
                buttonSize = 40.dp,
                iconSize = 24.dp
            )
        },
        actions = {
            if (onDeleteBookClick != null) {
                TopBarIconButton(
                    icon = Icons.Outlined.DeleteOutline,
                    contentDescription = "Xóa sách",
                    onClick = onDeleteBookClick,
                    tint = OnSurfaceVariant
                )
            }
            if (onNavigateToVoiceManagement != null) {
                TopBarIconButton(
                    icon = Icons.Outlined.RecordVoiceOver,
                    contentDescription = "Quản lý giọng đọc",
                    onClick = onNavigateToVoiceManagement
                )
            }
            if (onNavigateToSettings != null) {
                TopBarIconButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = "Cài đặt",
                    onClick = onNavigateToSettings
                )
            }
        }
    )
}

/**
 * Specialized Voice Management Header built on top of [CustomTopBar] matching `stitch/04-quan-ly-giong-doc.html`.
 */
@Composable
fun VoiceManagementTopBar(
    onOpenTuningSheet: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Giọng đọc"
) {
    CustomTopBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            AppLogo(
                size = 28.dp,
                cornerRadius = 8.dp
            )
        },
        actions = {
            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenTuningSheet),
                shape = CircleShape,
                color = SurfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Tinh chỉnh",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    )
}

/**
 * Specialized Ambient Music Header built on top of [CustomTopBar] matching Library, Voice Management, and Settings headers.
 */
@Composable
fun AmbientMusicTopBar(
    onAddMusicClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Nhạc nền"
) {
    CustomTopBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            AppLogo(
                size = 28.dp,
                cornerRadius = 8.dp
            )
        },
        actions = {
            TopBarImportButton(
                text = "Thêm nhạc",
                onClick = onAddMusicClick
            )
        }
    )
}

/**
 * Specialized Settings Header built on top of [CustomTopBar] matching `stitch/06-cai-dat.html`.
 */
@Composable
fun SettingsTopBar(
    modifier: Modifier = Modifier,
    title: String = "Cài đặt"
) {
    CustomTopBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            AppLogo(
                size = 28.dp,
                cornerRadius = 8.dp
            )
        }
    )
}

/**
 * Specialized Reader & Now-Playing Header built on top of [CustomTopBar] matching `stitch/02-dang-nghe-doc-dong-bo.html`.
 */
@Composable
fun ReaderTopBar(
    bookTitle: String,
    readerMode: ReaderMode,
    isOverflowMenuExpanded: Boolean,
    sleepTimerMinutes: Int,
    isAutoScrollEnabled: Boolean,
    highlightLevel: HighlightLevel,
    onBackClick: () -> Unit,
    onModeChange: (ReaderMode) -> Unit,
    onToggleOverflowMenu: () -> Unit,
    onDismissOverflowMenu: () -> Unit,
    onCycleSleepTimer: () -> Unit = {},
    onToggleAutoScroll: () -> Unit,
    onHighlightLevelChange: (HighlightLevel) -> Unit,
    modifier: Modifier = Modifier,
    onOpenTocClick: (() -> Unit)? = null,
    onOpenAppearanceClick: (() -> Unit)? = null,
    onNavigateToVoiceManagement: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onOpenSleepTimerSheet: () -> Unit = onCycleSleepTimer
) {
    CustomTopBar(
        title = bookTitle,
        modifier = modifier,
        navigationIcon = {
            TopBarIconButton(
                icon = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Thu nhỏ trình phát",
                onClick = onBackClick,
                tint = OnSurface,
                buttonSize = 40.dp,
                iconSize = 26.dp
            )
        },
        centerContent = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ĐANG PHÁT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    ),
                    color = Primary
                )
                Text(
                    text = bookTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
            }
        },
        actions = {
            if (onOpenTocClick != null) {
                TopBarIconButton(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = "Mục lục chương",
                    onClick = onOpenTocClick,
                    tint = OnSurface,
                    buttonSize = 38.dp,
                    iconSize = 22.dp
                )
            }
            if (onOpenAppearanceClick != null) {
                Surface(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenAppearanceClick),
                    shape = CircleShape,
                    color = SurfaceContainer
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aa",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = OnSurface
                        )
                    }
                }
            }
            Box {
                TopBarIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = "Tùy chọn khác",
                    onClick = onToggleOverflowMenu,
                    tint = OnSurface,
                    buttonSize = 40.dp,
                    iconSize = 24.dp
                )

                ReaderOverflowDropdown(
                    expanded = isOverflowMenuExpanded,
                    sleepTimerMinutes = sleepTimerMinutes,
                    readerMode = readerMode,
                    isAutoScrollEnabled = isAutoScrollEnabled,
                    highlightLevel = highlightLevel,
                    onDismiss = onDismissOverflowMenu,
                    onModeChange = onModeChange,
                    onOpenSleepTimerSheet = onOpenSleepTimerSheet,
                    onCycleSleepTimer = onCycleSleepTimer,
                    onToggleAutoScroll = onToggleAutoScroll,
                    onHighlightLevelChange = onHighlightLevelChange,
                    onNavigateToVoiceManagement = onNavigateToVoiceManagement,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        },
        bottomContent = {
            // Mode Selector: Nghe vs Đọc đồng bộ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReaderModeSegButton(
                        mode = ReaderMode.LISTEN,
                        icon = Icons.Outlined.DirectionsCar,
                        selected = readerMode == ReaderMode.LISTEN,
                        onClick = { onModeChange(ReaderMode.LISTEN) },
                        modifier = Modifier.weight(1f)
                    )
                    ReaderModeSegButton(
                        mode = ReaderMode.READ_ALONG,
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        selected = readerMode == ReaderMode.READ_ALONG,
                        onClick = { onModeChange(ReaderMode.READ_ALONG) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

@Composable
private fun ReaderModeSegButton(
    mode: ReaderMode,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) PrimaryContainer else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) OnPrimaryContainer else OnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (selected) OnPrimaryContainer else OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReaderOverflowDropdown(
    expanded: Boolean,
    sleepTimerMinutes: Int,
    readerMode: ReaderMode = ReaderMode.READ_ALONG,
    isAutoScrollEnabled: Boolean,
    highlightLevel: HighlightLevel,
    onDismiss: () -> Unit,
    onModeChange: (ReaderMode) -> Unit = {},
    onOpenSleepTimerSheet: () -> Unit = {},
    onCycleSleepTimer: () -> Unit = onOpenSleepTimerSheet,
    onToggleAutoScroll: () -> Unit,
    onHighlightLevelChange: (HighlightLevel) -> Unit,
    onNavigateToVoiceManagement: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(286.dp)
            .background(SurfaceContainerLowest)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // 1. Hẹn giờ tắt
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onDismiss()
                    onOpenSleepTimerSheet()
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Hẹn giờ tắt",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurface
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryFixed.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (sleepTimerMinutes > 0) "$sleepTimerMinutes phút" else "Tắt",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Primary
                )
            }
        }

        // 2. Chế độ lái xe (Nút lớn & Giữ sáng màn hình)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onDismiss()
                    onModeChange(if (readerMode == ReaderMode.LISTEN) ReaderMode.READ_ALONG else ReaderMode.LISTEN)
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = if (readerMode == ReaderMode.LISTEN) Primary else Tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Chế độ lái xe",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "Nút lớn & giữ sáng màn hình",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp
                        ),
                        color = OnSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (readerMode == ReaderMode.LISTEN) PrimaryFixed.copy(alpha = 0.55f)
                        else SurfaceContainerHigh
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (readerMode == ReaderMode.LISTEN) "Bật" else "Tắt",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (readerMode == ReaderMode.LISTEN) Primary else OnSurfaceVariant
                )
            }
        }

        // 3. Tự cuộn theo giọng
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleAutoScroll)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = null,
                    tint = Outline,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Tự cuộn theo giọng",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurface
                )
            }
            Icon(
                imageVector = if (isAutoScrollEnabled) Icons.Outlined.ToggleOn else Icons.Outlined.ToggleOff,
                contentDescription = null,
                tint = if (isAutoScrollEnabled) Primary else Outline,
                modifier = Modifier.size(22.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            thickness = 1.dp,
            color = OutlineVariant.copy(alpha = 0.35f)
        )

        // 4. Mức highlight (Đoạn / Câu / Từ)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BorderColor,
                        contentDescription = null,
                        tint = Outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Mức highlight",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnSurfaceVariant
                    )
                }

                Text(
                    text = highlightLevel.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HighlightLevel.entries.forEach { level ->
                    val isSelected = level == highlightLevel
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onHighlightLevelChange(level) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryContainer else SurfaceContainer,
                        border = if (isSelected) null else BorderStroke(0.5.dp, OutlineVariant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = level.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) OnPrimaryContainer else OnSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            thickness = 1.dp,
            color = OutlineVariant.copy(alpha = 0.35f)
        )

        // 5. Quản lý giọng đọc
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onDismiss()
                    onNavigateToVoiceManagement()
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.RecordVoiceOver,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Quản lý giọng đọc",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurface
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(16.dp)
            )
        }

        // 6. Cài đặt đọc & âm thanh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onDismiss()
                    onNavigateToSettings()
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = Outline,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Cài đặt ứng dụng",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = OnSurface
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
