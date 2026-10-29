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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vietreader.tts.ui.theme.HeadlineSerifFamily
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerLowest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeOnboardingDialog(
    initialName: String = "",
    initialRole: String = "Độc giả",
    onConfirm: (name: String, role: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameText by remember { mutableStateOf(initialName) }
    var selectedRole by remember { mutableStateOf(initialRole) }
    val focusManager = LocalFocusManager.current

    val roles = listOf(
        "Độc giả",
        "Người yêu sách",
        "Mọt sách tinh hoa",
        "Nhà thông thái"
    )

    Dialog(
        onDismissRequest = {
            onConfirm(nameText.trim().ifBlank { "Độc giả" }, selectedRole)
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceContainerLowest,
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Logo
                AppLogo(size = 56.dp, cornerRadius = 14.dp)

                // Welcome Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Chào mừng bạn đến với Wordwave! ✨",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = HeadlineSerifFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = OnSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ứng dụng đọc và nghe sách bằng giọng đọc AI tự nhiên tiếng Việt.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Name Input
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Bạn muốn ứng dụng gọi bạn là gì?",
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
                                text = "Nhập tên của bạn (ví dụ: Đạt, Minh, Lan...)",
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

                // Role Chips
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Chọn danh hiệu khởi đầu của bạn:",
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
                        roles.forEach { role ->
                            val isSelected = selectedRole == role
                            Surface(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { selectedRole = role },
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

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onConfirm(nameText.trim().ifBlank { "Độc giả" }, selectedRole)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Bắt đầu khám phá ngay",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = OnPrimary
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            onConfirm("Độc giả", "Độc giả")
                        }
                    ) {
                        Text(
                            text = "Để sau (dùng tên mặc định)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                color = OnSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
