package com.vietreader.tts.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vietreader.tts.domain.model.Book
import java.io.File
import com.vietreader.tts.ui.theme.Amber200
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.SurfaceContainerLow

/**
 * Reusable Book Cover Card matching `<article class="book-card">` in `stitch/01-thu-vien.html`.
 *
 * Includes:
 * - 3 : 4.2 aspect ratio cover with diagonal 3-stop gradient
 * - Left book-spine shadow strip (`w-1.5 bg-black/25`)
 * - Top metadata row: "EPUB" badge + Playing speaker icon (`Amber200`) or "MỚI" badge
 * - Centered serif title (`Literata` 17sp, max 2 lines)
 * - Bottom progress percentage + chapter label + thin progress bar
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: Book,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val coverBrush = Brush.linearGradient(
        colors = listOf(
            Color(book.coverGradient.startColorHex),
            Color(book.coverGradient.centerColorHex),
            Color(book.coverGradient.endColorHex)
        ),
        start = Offset.Zero,
        end = Offset(600f, 840f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4.2f)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(brush = coverBrush)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Optional real cover image from extracted EPUB
        BookCoverBackdrop(
            coverImagePath = book.coverImagePath,
            modifier = Modifier.fillMaxSize()
        )

        // Left book spine overlay: w-1.5 bg-black/25
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(6.dp)
                .background(Color.Black.copy(alpha = 0.25f))
        )

        // Cover content: p-4 flex flex-col justify-between
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: format label + active speaker / NEW badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = book.format.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.80f)
                )

                when {
                    isCurrentlyPlaying -> {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Đang phát",
                                tint = Amber200,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    book.isNew -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.20f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MỚI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White.copy(alpha = 0.90f)
                            )
                        }
                    }
                }
            }

            // Middle: Book Title (my-auto py-2)
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Bottom: Progress text + thin progress bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${book.progressPercent}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        text = book.currentChapterShortLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                // Progress bar track & indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f))
                ) {
                    val fraction = (book.progressPercent.coerceIn(0, 100)) / 100f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                if (isCurrentlyPlaying) Amber200 else Color.White.copy(alpha = 0.70f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Compact List variant of [BookCard] used when the user selects "Dạng danh sách" in Filter & Sort.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookListItemCard(
    book: Book,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val coverBrush = Brush.linearGradient(
        colors = listOf(
            Color(book.coverGradient.startColorHex),
            Color(book.coverGradient.centerColorHex),
            Color(book.coverGradient.endColorHex)
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(coverBrush),
                contentAlignment = Alignment.BottomStart
            ) {
                BookCoverBackdrop(
                    coverImagePath = book.coverImagePath,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = book.format.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 15.sp),
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCurrentlyPlaying) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Đang phát",
                            tint = PrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "${book.author} • Giọng ${book.voiceName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${book.progressPercent}% • ${book.currentChapterShortLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = book.remainingTimeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(OutlineVariant.copy(alpha = 0.4f))
                ) {
                    val fraction = (book.progressPercent.coerceIn(0, 100)) / 100f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(PrimaryContainer)
                    )
                }
            }
        }
    }
}

/**
 * Renders the extracted EPUB cover image when [coverImagePath] is available on disk,
 * with a dark gradient scrim so overlay badges and typography stay crisp.
 */
@Composable
fun BookCoverBackdrop(
    coverImagePath: String?,
    modifier: Modifier = Modifier
) {
    if (coverImagePath.isNullOrBlank()) return
    val imageBitmap = remember(coverImagePath) {
        runCatching {
            val file = File(coverImagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else {
                null
            }
        }.getOrNull()
    }
    if (imageBitmap != null) {
        Box(modifier = modifier) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.48f),
                                Color.Black.copy(alpha = 0.32f),
                                Color.Black.copy(alpha = 0.68f)
                            )
                        )
                    )
            )
        }
    }
}
