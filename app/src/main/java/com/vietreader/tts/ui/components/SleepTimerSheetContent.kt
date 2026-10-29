package com.vietreader.tts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnPrimaryContainer
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.SurfaceContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerLowest
import com.vietreader.tts.ui.theme.Tertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheetContent(
    title: String,
    subtitle: String,
    currentMinutes: Int,
    remainingSeconds: Int? = null,
    showEndOfChapterOption: Boolean = false,
    stopAtEndOfChapter: Boolean = false,
    onToggleStopAtEndOfChapter: ((Boolean) -> Unit)? = null,
    onSelectMinutes: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickPresets = listOf(
        0 to "Tắt",
        5 to "5p",
        10 to "10p",
        15 to "15p",
        20 to "20p",
        30 to "30p",
        45 to "45p",
        60 to "60p",
        90 to "90p",
        120 to "120p"
    )

    var customInputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bedtime,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OnSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
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

        HorizontalDivider(thickness = 1.dp, color = SurfaceContainer)

        // 2. Remaining Time Countdown Banner (if timer is actively running)
        if (currentMinutes > 0 && remainingSeconds != null && remainingSeconds > 0) {
            val remMin = remainingSeconds / 60
            val remSec = remainingSeconds % 60
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryContainer.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Đang đếm ngược: còn %02d:%02d".format(remMin, remSec),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = Primary
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(CircleShape)
                            .clickable { onSelectMinutes(0) },
                        shape = CircleShape,
                        color = SurfaceContainerHigh
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "Tắt ngay",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = OnSurface
                            )
                        }
                    }
                }
            }
        }

        // 3. Stop at End of Chapter Option (for reading screen)
        if (showEndOfChapterOption && onToggleStopAtEndOfChapter != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceContainerLowest,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (stopAtEndOfChapter) Primary.copy(alpha = 0.35f) else OutlineVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            tint = if (stopAtEndOfChapter) Primary else OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Dừng khi hết chương hiện tại",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = OnSurface
                            )
                            Text(
                                text = "Tự động tắt audio khi đọc xong chương này",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = stopAtEndOfChapter,
                        onCheckedChange = onToggleStopAtEndOfChapter,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnPrimary,
                            checkedTrackColor = PrimaryContainer,
                            uncheckedThumbColor = OnSurfaceVariant,
                            uncheckedTrackColor = SurfaceContainerHigh
                        )
                    )
                }
            }
        }

        // 4. Quick Preset Chips
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Mốc thời gian chọn nhanh",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = OnSurface
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPresets.forEach { (mins, label) ->
                    val isSelected = currentMinutes == mins
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectMinutes(mins) },
                        shape = CircleShape,
                        color = if (isSelected) PrimaryContainer else SurfaceContainer,
                        border = if (isSelected) null else BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = OnPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (isSelected) OnPrimary else OnSurface
                            )
                        }
                    }
                }
            }
        }

        // 5. Custom Minutes Input Field
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Hoặc nhập số phút tùy ý",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = OnSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customInputText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(3)
                        customInputText = filtered
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Ví dụ: 25, 40, 75 phút...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = customInputText.toIntOrNull()
                            if (parsed != null && parsed in 1..480) {
                                onSelectMinutes(parsed)
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = OutlineVariant.copy(alpha = 0.5f),
                        focusedContainerColor = SurfaceContainerLowest,
                        unfocusedContainerColor = SurfaceContainerLowest
                    )
                )

                val parsedVal = customInputText.toIntOrNull()
                val isApplyEnabled = parsedVal != null && parsedVal in 1..480

                Surface(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = isApplyEnabled) {
                            if (parsedVal != null) {
                                onSelectMinutes(parsedVal)
                                focusManager.clearFocus()
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isApplyEnabled) Primary else SurfaceContainerHigh
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Áp dụng",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = if (isApplyEnabled) OnPrimary else OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
