package com.vietreader.tts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vietreader.tts.ui.theme.Amber200
import com.vietreader.tts.ui.theme.HeadlineSerifFamily
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnPrimaryFixed
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.PrimaryFixed
import com.vietreader.tts.ui.theme.SurfaceContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerLow
import com.vietreader.tts.ui.theme.SurfaceContainerLowest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileSheetContent(
    currentName: String,
    currentRoleBadge: String,
    onApply: (name: String, role: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nameText by remember { mutableStateOf(currentName) }
    var roleBadge by remember { mutableStateOf(currentRoleBadge) }
    val focusManager = LocalFocusManager.current

    val suggestedRoles = listOf(
        "Độc giả",
        "Người yêu sách",
        "Mọt sách tinh hoa",
        "Nhà thông thái",
        "Thính giả say mê",
        "Khám phá tri thức"
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
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Hồ sơ độc giả",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "Tùy chỉnh tên hiển thị và danh hiệu của bạn",
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

        // 2. Avatar Preview Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerLow,
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val initialLetter = nameText.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "W"
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF7A3700),
                                    Color(0xFF451E03)
                                )
                            )
                        )
                        .border(1.dp, Amber200.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialLetter,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Amber200
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nameText.ifBlank { "Độc giả thân mến" },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = HeadlineSerifFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryFixed)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = roleBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = OnPrimaryFixed
                        )
                    }
                }
            }
        }

        // 3. Name Input Field
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Tên hiển thị của bạn",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = OnSurface
            )

            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Nhập tên của bạn...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OutlineVariant.copy(alpha = 0.5f),
                    focusedContainerColor = SurfaceContainerLowest,
                    unfocusedContainerColor = SurfaceContainerLowest
                )
            )
        }

        // 4. Role Title Chips
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Danh hiệu yêu thích",
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
                suggestedRoles.forEach { role ->
                    val isSelected = roleBadge == role
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { roleBadge = role },
                        shape = CircleShape,
                        color = if (isSelected) PrimaryContainer else SurfaceContainerHigh,
                        border = if (isSelected) null else BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = OnPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = role,
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

        Spacer(modifier = Modifier.height(4.dp))

        // 5. Save Button
        val isSaveEnabled = nameText.isNotBlank()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = isSaveEnabled) {
                    onApply(nameText.trim(), roleBadge.trim())
                    focusManager.clearFocus()
                },
            shape = RoundedCornerShape(12.dp),
            color = if (isSaveEnabled) Primary else SurfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Lưu hồ sơ",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = if (isSaveEnabled) OnPrimary else OnSurfaceVariant
                )
            }
        }
    }
}
