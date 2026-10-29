package com.vietreader.tts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vietreader.tts.ui.theme.HeadlineSerifFamily
import com.vietreader.tts.ui.theme.OnPrimary
import com.vietreader.tts.ui.theme.OnSurface
import com.vietreader.tts.ui.theme.OnSurfaceVariant
import com.vietreader.tts.ui.theme.OutlineVariant
import com.vietreader.tts.ui.theme.Primary
import com.vietreader.tts.ui.theme.PrimaryContainer
import com.vietreader.tts.ui.theme.SurfaceContainer
import com.vietreader.tts.ui.theme.SurfaceContainerHigh
import com.vietreader.tts.ui.theme.SurfaceContainerLowest
import com.vietreader.tts.ui.theme.Tertiary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BackupRestoreSheetContent(
    isSignedInGoogle: Boolean,
    googleAccountEmail: String?,
    googleAccountName: String?,
    isSyncing: Boolean,
    syncMessage: String?,
    lastBackupTimestamp: Long,
    booksCount: Int,
    bookmarksCount: Int,
    rulesCount: Int,
    onSignInGoogleClick: () -> Unit,
    onSignOutGoogleClick: () -> Unit,
    onTriggerCloudBackup: () -> Unit,
    onTriggerCloudRestore: () -> Unit,
    onTriggerLocalExport: () -> Unit,
    onTriggerLocalImport: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. Header Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudSync,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Sao lưu & Khôi phục",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = HeadlineSerifFamily,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnSurface
                    )
                    Text(
                        text = "Tự động đồng bộ đám mây với Google Drive",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OnSurfaceVariant
                    )
                }
            }

            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = SurfaceContainerHigh
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Đóng",
                    tint = OnSurfaceVariant,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 2. Google Drive Account Status Section ---
        Text(
            text = "TÀI KHOẢN GOOGLE DRIVE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            ),
            color = Primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceContainerLowest,
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                if (isSignedInGoogle && !googleAccountEmail.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = googleAccountName?.ifBlank { "Tài khoản Google" } ?: "Tài khoản Google",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = OnSurface
                                )
                                Text(
                                    text = googleAccountEmail,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Primary
                                )
                            }
                        }

                        TextButton(onClick = onSignOutGoogleClick) {
                            Text("Đăng xuất", color = OnSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cloud,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chưa kết nối Google Drive",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = OnSurface
                            )
                            Text(
                                text = "Kết nối để sao lưu 1 chạm không cần file JSON",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSignInGoogleClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = OnPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đăng nhập với Google",
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

        Spacer(modifier = Modifier.height(18.dp))

        // --- 3. Current Data Metric Cards ---
        Text(
            text = "DỮ LIỆU ĐƯỢC SAO LƯU",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            ),
            color = Primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BackupMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AutoStories,
                label = "Sách",
                value = "$booksCount cuốn"
            )
            BackupMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.BookmarkBorder,
                label = "Dấu trang",
                value = "$bookmarksCount thẻ"
            )
            BackupMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.RecordVoiceOver,
                label = "Từ điển",
                value = "$rulesCount từ"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Status Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Primary
                    )
                } else {
                    Icon(
                        imageVector = if (lastBackupTimestamp > 0L) Icons.Filled.CheckCircle else Icons.Outlined.Info,
                        contentDescription = null,
                        tint = if (lastBackupTimestamp > 0L) Tertiary else OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isSyncing) "Đang đồng bộ..." else if (lastBackupTimestamp > 0L) "Bản sao lưu gần nhất" else "Trạng thái",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurface
                    )
                    Text(
                        text = if (!syncMessage.isNullOrBlank()) {
                            syncMessage
                        } else if (lastBackupTimestamp > 0L) {
                            val formatted = Instant.ofEpochMilli(lastBackupTimestamp)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
                            "Đã sao lưu lúc $formatted"
                        } else {
                            "Sẵn sàng sao lưu lên Google Drive"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = OnSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(18.dp))

        // --- 4. Main One-Tap Actions ---
        // Button A: Direct Cloud Backup
        Button(
            onClick = {
                if (isSignedInGoogle) {
                    onTriggerCloudBackup()
                } else {
                    onSignInGoogleClick()
                }
            },
            enabled = !isSyncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = OnPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.CloudUpload,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Sao lưu lên Google Drive ngay",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = OnPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Button B: Direct Cloud Restore
        OutlinedButton(
            onClick = {
                if (isSignedInGoogle) {
                    onTriggerCloudRestore()
                } else {
                    onSignInGoogleClick()
                }
            },
            enabled = !isSyncing,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, Primary)
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudDownload,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Khôi phục từ Google Drive",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 5. Optional Local File Backup Footer ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hoặc: ",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = OnSurfaceVariant
            )
            Text(
                text = "Xuất tệp máy",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onTriggerLocalExport)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
                text = "•",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = "Nhập tệp máy",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onTriggerLocalImport)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun BackupMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerLowest,
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = OnSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = OnSurfaceVariant
            )
        }
    }
}
