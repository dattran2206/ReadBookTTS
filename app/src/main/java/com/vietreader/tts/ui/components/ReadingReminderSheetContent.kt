package com.vietreader.tts.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.SurfaceContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerLowest

private data class ReminderPreset(
    val hour: Int,
    val minute: Int,
    val label: String,
    val period: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingReminderSheetContent(
    initialEnabled: Boolean,
    initialHour: Int,
    initialMinute: Int,
    initialDays: Set<Int>,
    onApply: (enabled: Boolean, hour: Int, minute: Int, days: Set<Int>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEnabled by remember { mutableStateOf(initialEnabled) }
    var selectedHour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    var selectedDays by remember { mutableStateOf(initialDays.ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }) }

    val quickPresets = listOf(
        ReminderPreset(6, 30, "06:30", "Sáng sớm"),
        ReminderPreset(12, 30, "12:30", "Nghỉ trưa"),
        ReminderPreset(20, 0, "20:00", "Buổi tối"),
        ReminderPreset(21, 0, "21:00", "Trước ngủ"),
        ReminderPreset(22, 0, "22:00", "Đêm muộn")
    )

    val weekDays = listOf(
        1 to "T2",
        2 to "T3",
        3 to "T4",
        4 to "T5",
        5 to "T6",
        6 to "T7",
        7 to "CN"
    )

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
                Surface(
                    shape = CircleShape,
                    color = PrimaryContainer.copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Thông báo nhắc đọc sách",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "Hình thành thói quen đọc sách mỗi ngày",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OnSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                shape = CircleShape,
                color = SurfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Đóng",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = SurfaceContainer)

        // 2. Master Toggle Switch
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceContainerLowest,
            border = BorderStroke(
                width = 1.dp,
                color = if (isEnabled) Primary.copy(alpha = 0.35f) else OutlineVariant.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bật nhắc nhở đọc sách",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = if (isEnabled) "Gửi thông báo vào thời gian bạn đã chọn" else "Tạm ngắt thông báo nhắc đọc",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OnSurfaceVariant
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnPrimary,
                        checkedTrackColor = Primary,
                        uncheckedThumbColor = OnSurfaceVariant,
                        uncheckedTrackColor = SurfaceContainerHigh
                    )
                )
            }
        }

        if (isEnabled) {
            // 3. Quick Preset Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Khung giờ gợi ý nhanh",
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
                    quickPresets.forEach { preset ->
                        val isSelected = selectedHour == preset.hour && selectedMinute == preset.minute
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedHour = preset.hour
                                    selectedMinute = preset.minute
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) PrimaryContainer else SurfaceContainerLowest,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Primary else OutlineVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = OnPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = if (isSelected) OnPrimary else OnSurface
                                    )
                                    Text(
                                        text = preset.period,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isSelected) OnPrimary.copy(alpha = 0.85f) else OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Custom Time Stepper Display
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Hoặc tùy chỉnh giờ & phút",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = OnSurface
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceContainerLowest,
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour Stepper
                        TimeStepperColumn(
                            value = selectedHour,
                            label = "Giờ",
                            onIncrement = { selectedHour = (selectedHour + 1) % 24 },
                            onDecrement = { selectedHour = if (selectedHour == 0) 23 else selectedHour - 1 }
                        )

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            ),
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Minute Stepper (step by 5m on buttons, wrap 0..59)
                        TimeStepperColumn(
                            value = selectedMinute,
                            label = "Phút",
                            onIncrement = { selectedMinute = (selectedMinute + 5) % 60 },
                            onDecrement = { selectedMinute = if (selectedMinute < 5) 55 else selectedMinute - 5 }
                        )
                    }
                }
            }

            // 5. Repeat Days in Week
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lặp lại vào các ngày",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = OnSurface
                    )

                    // Quick All/Weekdays toggle
                    val isAllDays = selectedDays.size == 7
                    Text(
                        text = if (isAllDays) "Chỉ ngày trong tuần" else "Tất cả các ngày",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = Primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                selectedDays = if (isAllDays) setOf(1, 2, 3, 4, 5) else setOf(1, 2, 3, 4, 5, 6, 7)
                            }
                            .padding(4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weekDays.forEach { (dayInt, label) ->
                        val isDaySelected = dayInt in selectedDays
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    selectedDays = if (isDaySelected) {
                                        if (selectedDays.size > 1) selectedDays - dayInt else selectedDays
                                    } else {
                                        selectedDays + dayInt
                                    }
                                },
                            shape = CircleShape,
                            color = if (isDaySelected) Primary else SurfaceContainerHigh,
                            border = if (isDaySelected) null else BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isDaySelected) OnPrimary else OnSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Action Button: Lưu cài đặt
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onApply(isEnabled, selectedHour, selectedMinute, selectedDays)
                },
            shape = RoundedCornerShape(12.dp),
            color = Primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Lưu cài đặt nhắc đọc",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = OnPrimary
                )
            }
        }
    }
}

@Composable
private fun TimeStepperColumn(
    value: Int,
    label: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onIncrement),
            shape = CircleShape,
            color = SurfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = "Tăng $label",
                    tint = OnSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SurfaceContainerHigh,
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
        ) {
            Text(
                text = "%02d".format(value),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 26.sp
                ),
                color = OnSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Surface(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onDecrement),
            shape = CircleShape,
            color = SurfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Giảm $label",
                    tint = OnSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = OnSurfaceVariant
        )
    }
}
