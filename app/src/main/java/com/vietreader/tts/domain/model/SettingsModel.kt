package com.vietreader.tts.domain.model

import androidx.compose.runtime.Immutable

enum class ReadingThemeMode(
    val label: String,
    val previewBgHex: Long,
    val previewTextHex: Long
) {
    IVORY(
        label = "Trắng ngà",
        previewBgHex = 0xFFFFF8F5,
        previewTextHex = 0xFF1F1B17
    ),
    SEPIA(
        label = "Giấy cổ Sepia",
        previewBgHex = 0xFFF4E8D1,
        previewTextHex = 0xFF3B2F2F
    ),
    NIGHT(
        label = "Tối dịu mắt",
        previewBgHex = 0xFF1E1B18,
        previewTextHex = 0xFFEAE1DA
    )
}

enum class SettingsSheetType {
    NONE,
    APPEARANCE_AND_FONT,
    SPEED_AND_PRONUNCIATION,
    AUDIO_PLAYBACK,
    READING_STATS,
    DEFAULT_SLEEP_TIMER,
    REMINDER_SETTINGS,
    EDIT_PROFILE,
    BACKUP_RESTORE
}

@Immutable
data class AppSettings(
    val userName: String = "Độc giả",
    val userRoleBadge: String = "Độc giả",
    val streakDays: Int = 0,
    val totalListeningTimeLabel: String = "0 phút",
    val totalBooksRead: Int = 0,
    val themeMode: ReadingThemeMode = ReadingThemeMode.IVORY,
    val fontSizePx: Int = 17,
    val autoScrollEnabled: Boolean = true,
    val stopAtEndOfChapter: Boolean = false,
    val defaultSpeed: Float = 1.0f,
    val dictionaryWordCount: Int = 0,
    val cloudSyncEnabled: Boolean = true,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val reminderTimeLabel: String = "Nhắc đọc 21:00 (Hàng ngày)",
    val defaultSleepTimerMinutes: Int = 30,
    val highlightLevel: HighlightLevel = HighlightLevel.SENTENCE,
    val backupAccountEmail: String = "",
    val lastBackupTimestamp: Long = 0L,
    val appVersion: String = "1.2.0"
) {
    val appearanceSummary: String
        get() = "${themeMode.label} • ${fontSizePx}px"

    val audioPlaybackSummary: String
        get() = buildList {
            if (autoScrollEnabled) add("Tự cuộn") else add("Cuộn thủ công")
            if (stopAtEndOfChapter) add("Dừng hết chương") else add("Chuyển chương tự động")
        }.joinToString(", ")

    val voiceAndSpeedSummary: String
        get() = "${"%.1f".format(defaultSpeed)}x • Từ điển $dictionaryWordCount từ"

    val cloudSyncSummary: String
        get() = if (backupAccountEmail.isNotBlank()) {
            backupAccountEmail
        } else if (lastBackupTimestamp > 0L) {
            val date = java.time.Instant.ofEpochMilli(lastBackupTimestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))
            "Đã lưu $date"
        } else {
            "Google Drive & Tệp"
        }

    val reminderSummary: String
        get() = if (!reminderEnabled) {
            "Đã tắt"
        } else {
            val daysLabel = when (reminderDays.size) {
                7 -> "Hàng ngày"
                5 -> if (reminderDays == setOf(1, 2, 3, 4, 5)) "T2-T6" else "${reminderDays.size} ngày"
                2 -> if (reminderDays == setOf(6, 7)) "T7, CN" else "${reminderDays.size} ngày"
                0 -> "Không lặp lại"
                else -> "${reminderDays.size} ngày/tuần"
            }
            "Nhắc đọc %02d:%02d (%s)".format(reminderHour, reminderMinute, daysLabel)
        }

    val sleepTimerSummary: String
        get() = if (defaultSleepTimerMinutes > 0) "$defaultSleepTimerMinutes phút" else "Tắt"
}
