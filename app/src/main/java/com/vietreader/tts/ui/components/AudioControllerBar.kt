package com.vietreader.tts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vietreader.tts.domain.model.PlaybackState
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.Outline
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.ReadingInkMuted
import com.vietreader.tts.ui.theme.Surface
import com.vietreader.tts.ui.theme.SurfaceContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerHighest
import com.vietreader.tts.ui.theme.SurfaceContainerLowest
import com.vietreader.tts.ui.theme.Tertiary
import kotlin.math.roundToInt

/**
 * Floating Mini Player (`#mini-player`) extracted from `stitch/01-thu-vien.html`.
 */
@Composable
fun AudioControllerBar(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbBrush = Brush.linearGradient(
        colors = listOf(
            Color(playbackState.coverGradient.startColorHex),
            Color(playbackState.coverGradient.endColorHex)
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onBarClick),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLowest.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, SurfaceContainerHigh.copy(alpha = 0.8f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Mini Cover Thumbnail (w-9 h-9 rounded-lg)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush = thumbBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Center: Clean 2-Line Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = OnSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                append(playbackState.title)
                            }
                            withStyle(
                                SpanStyle(
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.Normal
                                )
                            ) {
                                append(" • ${playbackState.chapterShortLabel}")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Giọng ${playbackState.voiceName} • ${playbackState.remainingLabel}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Right: Play/Pause Circular Button (w-9 h-9 rounded-full bg-primary-container)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer)
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = "Phát hoặc tạm dừng",
                        tint = OnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Thin progress strip attached to bottom edge of miniplayer
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh)
            ) {
                val progressFraction = (playbackState.progressPercent.coerceIn(0, 100)) / 100f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(PrimaryContainer)
                )
            }
        }
    }
}

/**
 * Full-featured Bottom Audio Controller Dock (`<footer id="bottom-controls">`)
 * extracted from `stitch/02-dang-nghe-doc-dong-bo.html`.
 *
 * When collapsed (`collapseProgress` -> 1f), slides down by the exact height of the lower controls
 * section so the Timeline Seek Bar & Timestamps remain visible at the bottom of the screen.
 */
@Composable
fun ExpandedAudioControllerBar(
    playbackState: PlaybackState,
    availableVoices: List<String>,
    collapseProgress: Float,
    isCollapsed: Boolean,
    onToggleCollapseControls: () -> Unit,
    onSeekToSeconds: (Int) -> Unit,
    onPrevSentenceClick: () -> Unit,
    onRewind15Click: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onForward15Click: () -> Unit,
    onNextSentenceClick: () -> Unit,
    onCycleSpeedClick: () -> Unit,
    onSelectVoice: (String) -> Unit,
    onTocClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVoiceDropdownExpanded by remember { mutableStateOf(false) }
    var lowerControlsHeightPx by remember { mutableIntStateOf(0) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = lowerControlsHeightPx * collapseProgress
            },
        color = Surface.copy(alpha = 0.98f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = OutlineVariant.copy(alpha = 0.40f)
            )

            // TOP SECTION: Always stays visible (Handle + Timeline Seek Bar + Timestamps)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 6.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Drag / Collapse / Expand Handle (#handle-collapse) - supports tap & swipe up/down
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleCollapseControls
                        )
                        .pointerInput(isCollapsed) {
                            var accumulatedDrag = 0f
                            detectVerticalDragGestures(
                                onDragStart = { accumulatedDrag = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    accumulatedDrag += dragAmount
                                    if (!isCollapsed && accumulatedDrag > 16f) {
                                        accumulatedDrag = 0f
                                        onToggleCollapseControls()
                                    } else if (isCollapsed && accumulatedDrag < -16f) {
                                        accumulatedDrag = 0f
                                        onToggleCollapseControls()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(OutlineVariant.copy(alpha = 0.75f))
                    )
                }

                // 2. Playback Seek Bar & Time Display (#seek-container)
                Slider(
                    value = playbackState.currentSeconds.toFloat(),
                    onValueChange = { onSeekToSeconds(it.roundToInt()) },
                    valueRange = 0f..playbackState.totalSeconds.coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                        inactiveTrackColor = SurfaceContainerHighest
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = playbackState.currentTime,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Primary
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceContainer)
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${playbackState.chapterShortLabel} • ${playbackState.remainingLabel}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = OnSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }

                    Text(
                        text = playbackState.totalTime,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = ReadingInkMuted
                    )
                }
            }

            // LOWER SECTION: Transport Controls + Secondary Controls (Measured & slid below screen when collapsed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { lowerControlsHeightPx = it.height }
                    .graphicsLayer {
                        alpha = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
                    }
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3. Core Transport Bar (Prev Sentence, -15s, Primary Squircle Play/Pause, +15s, Next Sentence)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Sentence (#btn-prev-sentence)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isCollapsed, onClick = onPrevSentenceClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Câu trước",
                            tint = OnSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Rewind 15s (#btn-replay-15)
                    Column(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isCollapsed, onClick = onRewind15Click),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Tua lùi 15 giây",
                            tint = OnSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "15s",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            ),
                            color = OnSurface,
                            modifier = Modifier.offset(y = (-2).dp)
                        )
                    }

                    // Prominent Primary Play / Pause Squircle Button (w-15 h-15 rounded-2xl bg-primary)
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Primary)
                            .clickable(enabled = !isCollapsed, onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = if (playbackState.isPlaying) "Tạm dừng" else "Phát",
                            tint = OnPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Forward 15s (#btn-forward-15)
                    Column(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isCollapsed, onClick = onForward15Click),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Tua tới 15 giây",
                            tint = OnSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "15s",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            ),
                            color = OnSurface,
                            modifier = Modifier.offset(y = (-2).dp)
                        )
                    }

                    // Next Sentence (#btn-next-sentence)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isCollapsed, onClick = onNextSentenceClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Câu tiếp theo",
                            tint = OnSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 4. Secondary Controls Bar: Speed, Voice Selector, Table of Contents, Bookmark
                HorizontalDivider(
                    thickness = 1.dp,
                    color = OutlineVariant.copy(alpha = 0.30f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed toggle (#btn-speed)
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerHigh)
                            .clickable(enabled = !isCollapsed, onClick = onCycleSpeedClick)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = "Tốc độ đọc",
                            tint = Primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "%.1fx".format(playbackState.playbackSpeed),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = OnSurface
                        )
                    }

                    // Voice Selector (#btn-voice-selector)
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 200.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerHigh)
                                .clickable(enabled = !isCollapsed) { isVoiceDropdownExpanded = true }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = Tertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = playbackState.voiceName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = OnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = Outline,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isVoiceDropdownExpanded,
                            onDismissRequest = { isVoiceDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceContainerLowest)
                        ) {
                            availableVoices.forEach { voice ->
                                val isCurrent = voice == playbackState.voiceName
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = voice,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (isCurrent) Primary else OnSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectVoice(voice)
                                        isVoiceDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Table of Contents (#btn-toc)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerHigh)
                            .clickable(enabled = !isCollapsed, onClick = onTocClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = "Mục lục sách",
                            tint = OnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Bookmark (#btn-bookmark)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerHigh)
                            .clickable(enabled = !isCollapsed, onClick = onBookmarkClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isBookmarked) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.Outlined.BookmarkBorder
                            },
                            contentDescription = "Đánh dấu trang",
                            tint = if (playbackState.isBookmarked) Primary else OnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // System navigation bar spacer at the very bottom
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
