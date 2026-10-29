package com.vietreader.tts.data.local

import android.content.Context
import com.vietreader.tts.data.parser.EpubAndTxtParser
import com.vietreader.tts.data.tts.AiVoiceModelManager
import com.vietreader.tts.domain.model.AmbientIconType
import com.vietreader.tts.domain.model.AmbientMusicSettings
import com.vietreader.tts.domain.model.AmbientTrack
import com.vietreader.tts.domain.model.AppSettings
import com.vietreader.tts.domain.model.Book
import com.vietreader.tts.domain.model.BookBookmark
import com.vietreader.tts.domain.model.BookFormat
import com.vietreader.tts.domain.model.CoverGradient
import com.vietreader.tts.domain.model.HighlightLevel
import com.vietreader.tts.domain.model.PronunciationRule
import com.vietreader.tts.domain.model.ReadingThemeMode
import com.vietreader.tts.domain.model.TranscriptSentence
import com.vietreader.tts.domain.model.VoiceCatalog
import com.vietreader.tts.domain.model.VoiceGroup
import com.vietreader.tts.domain.model.VoiceInstallStatus
import com.vietreader.tts.domain.model.VoiceItem
import com.vietreader.tts.domain.model.VoiceTuningSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Lightweight, fast local storage persisting imported books, reading progress, bookmarks,
 * voice management state, pronunciation dictionary, and appearance/audio settings across app restarts.
 */
class WordwaveLocalStorage(private val context: Context) {

    private val prefs = context.getSharedPreferences("wordwave_preferences", Context.MODE_PRIVATE)
    private val stateFile = File(context.filesDir, "wordwave_library_state.json")

    companion object {
        private val REMOVED_SAMPLE_IDS = setOf("demen", "sodo", "kieu", "tatden", "vangbong")
    }

    fun loadBooks(): List<Book> {
        if (!stateFile.exists()) return emptyList()

        return runCatching {
            val root = JSONObject(stateFile.readText(Charsets.UTF_8))
            val booksArray = root.optJSONArray("books") ?: return emptyList()
            val loadedBooks = mutableListOf<Book>()
            var purgedLegacyEntries = false

            for (i in 0 until booksArray.length()) {
                val obj = booksArray.getJSONObject(i)
                val id = obj.optString("id")
                if (id.isBlank()) continue
                if (id in REMOVED_SAMPLE_IDS || id.startsWith("mybook_")) {
                    purgedLegacyEntries = true
                    continue
                }

                // User-imported EPUB / TXT book
                val epubFilePath = obj.optString("epubFilePath").takeIf { it.isNotBlank() }
                if (epubFilePath == null || !File(epubFilePath).exists()) {
                    purgedLegacyEntries = true
                    continue
                }

                val bookmarks = parseBookmarksJson(obj.optJSONArray("bookmarks")).orEmpty()
                val currentChapterNum = obj.optInt("currentChapterNumber", 1)
                val progressPercent = obj.optInt("progressPercent", 0)
                val currentPositionSeconds = obj.optInt("currentPositionSeconds", 0)
                val currentChapterTitle = obj.optString("currentChapterTitle", "Chương 1")
                val currentChapterShort = obj.optString("currentChapterShortLabel", "Chương 1")
                val remainingLabel = obj.optString("remainingTimeLabel", "00:00")
                val rawVoiceName = obj.optString("voiceName", "Giọng hệ thống 1")
                val matchedVoice = VoiceCatalog.defaultVoiceItems.find {
                    it.name.equals(rawVoiceName, ignoreCase = true) ||
                        (!it.systemVoiceName.isNullOrBlank() &&
                            rawVoiceName.contains(VoiceCatalog.extractVariantKey(it.systemVoiceName), ignoreCase = true))
                } ?: VoiceCatalog.defaultSystemVoiceItems.first()
                val voiceName = matchedVoice.name
                val voiceRegion = if (matchedVoice.isAiVoice) "AI • ${matchedVoice.gender}" else matchedVoice.gender
                val isCompleted = obj.optBoolean("isCompleted", false)
                val isNew = obj.optBoolean("isNew", false)
                val lastReadEpoch = obj.optLong("lastReadEpochMillis", 0L)
                val coverImagePath = obj.optString("coverImagePath").takeIf { it.isNotBlank() }
                val title = obj.optString("title", "Sách đã nhập")
                val author = obj.optString("author", "Tác giả chưa rõ")
                val categoryBadge = obj.optString("categoryBadge", "Sách EPUB cá nhân")
                val genreLabel = obj.optString("genreLabel", "Tiểu thuyết")
                val totalDurationFormatted = obj.optString("totalDurationFormatted", "00:00")
                val totalChaptersCount = obj.optInt("totalChaptersCount", 1)
                val synopsis = obj.optString("synopsis", "")
                val formatName = obj.optString("format", BookFormat.EPUB.name)
                val format = runCatching { BookFormat.valueOf(formatName) }.getOrDefault(BookFormat.EPUB)
                val gradient = CoverGradient(
                    startColorHex = obj.optLong("gradStart", 0xFF5D2A0C),
                    centerColorHex = obj.optLong("gradCenter", 0xFF3E1B07),
                    endColorHex = obj.optLong("gradEnd", 0xFF210D03)
                )

                val chapters = EpubAndTxtParser.loadCachedChapters(
                    epubFilePath = epubFilePath,
                    currentChapterNumber = currentChapterNum,
                    currentProgressPercent = progressPercent
                )

                val activeChapter = chapters.find { it.number == currentChapterNum } ?: chapters.firstOrNull()
                val (sentences, totalDurationSec) = if (activeChapter != null && activeChapter.entryHref.isNotBlank()) {
                    EpubAndTxtParser.loadEpubChapterSentences(
                        epubFilePath = epubFilePath,
                        entryHref = activeChapter.entryHref,
                        chapterTitle = activeChapter.title
                    )
                } else {
                    emptyList<TranscriptSentence>() to obj.optInt("totalDurationSeconds", 0)
                }

                loadedBooks.add(
                    Book(
                        id = id,
                        title = title,
                        author = author,
                        categoryBadge = categoryBadge,
                        genreLabel = genreLabel,
                        totalDurationFormatted = totalDurationFormatted,
                        totalChaptersCount = totalChaptersCount.coerceAtLeast(chapters.size),
                        synopsis = synopsis,
                        currentChapterTitle = currentChapterTitle,
                        currentChapterShortLabel = currentChapterShort,
                        progressPercent = progressPercent,
                        remainingTimeLabel = remainingLabel,
                        voiceName = voiceName,
                        voiceRegion = voiceRegion,
                        format = format,
                        coverGradient = gradient,
                        isNew = isNew,
                        isImported = true,
                        isCompleted = isCompleted,
                        addedAtEpochMillis = obj.optLong("addedAtEpochMillis", System.currentTimeMillis()),
                        lastReadEpochMillis = lastReadEpoch,
                        totalDurationSeconds = totalDurationSec,
                        currentPositionSeconds = currentPositionSeconds,
                        sentences = sentences,
                        chapters = chapters,
                        bookmarks = bookmarks,
                        epubFilePath = epubFilePath,
                        coverImagePath = coverImagePath,
                        currentChapterNumber = currentChapterNum
                    )
                )
            }

            if (purgedLegacyEntries) {
                saveBooks(loadedBooks)
            }
            loadedBooks
        }.getOrElse { emptyList() }
    }

    fun saveBooks(books: List<Book>) {
        runCatching {
            val booksArray = JSONArray()
            for (book in books) {
                val obj = JSONObject().apply {
                    put("id", book.id)
                    put("title", book.title)
                    put("author", book.author)
                    put("categoryBadge", book.categoryBadge)
                    put("genreLabel", book.genreLabel)
                    put("totalDurationFormatted", book.totalDurationFormatted)
                    put("totalChaptersCount", book.totalChaptersCount)
                    put("synopsis", book.synopsis)
                    put("currentChapterTitle", book.currentChapterTitle)
                    put("currentChapterShortLabel", book.currentChapterShortLabel)
                    put("currentChapterNumber", book.currentChapterNumber)
                    put("progressPercent", book.progressPercent)
                    put("currentPositionSeconds", book.currentPositionSeconds)
                    put("totalDurationSeconds", book.totalDurationSeconds)
                    put("remainingTimeLabel", book.remainingTimeLabel)
                    put("voiceName", book.voiceName)
                    put("voiceRegion", book.voiceRegion)
                    put("format", book.format.name)
                    put("gradStart", book.coverGradient.startColorHex)
                    put("gradCenter", book.coverGradient.centerColorHex)
                    put("gradEnd", book.coverGradient.endColorHex)
                    put("isNew", book.isNew)
                    put("isImported", book.isImported)
                    put("isCompleted", book.isCompleted)
                    put("addedAtEpochMillis", book.addedAtEpochMillis)
                    put("lastReadEpochMillis", book.lastReadEpochMillis)
                    if (book.epubFilePath != null) put("epubFilePath", book.epubFilePath)
                    if (book.coverImagePath != null) put("coverImagePath", book.coverImagePath)

                    val bmArray = JSONArray()
                    for (bm in book.bookmarks) {
                        bmArray.put(
                            JSONObject().apply {
                                put("id", bm.id)
                                put("chapterTitle", bm.chapterTitle)
                                put("quoteText", bm.quoteText)
                                put("timestampLabel", bm.timestampLabel)
                                put("chapterNumber", bm.chapterNumber)
                                put("sentenceIndex", bm.sentenceIndex)
                            }
                        )
                    }
                    put("bookmarks", bmArray)
                }
                booksArray.put(obj)
            }
            val root = JSONObject().apply {
                put("books", booksArray)
            }
            stateFile.writeText(root.toString(), Charsets.UTF_8)
        }
    }

    fun recordReadingSession(durationSeconds: Long) {
        if (durationSeconds <= 0) return
        val currentTotal = prefs.getLong("totalReadingSeconds", 0L)
        val newTotal = currentTotal + durationSeconds

        val today = java.time.LocalDate.now().toString()
        val lastRecorded = prefs.getString("streakLastRecordedDate", "") ?: ""
        var streak = prefs.getInt("currentStreakDays", 1)

        if (lastRecorded.isBlank()) {
            streak = 1
        } else if (lastRecorded != today) {
            val yesterday = java.time.LocalDate.now().minusDays(1).toString()
            if (lastRecorded == yesterday) {
                streak += 1
            } else {
                streak = 1
            }
        }

        prefs.edit()
            .putLong("totalReadingSeconds", newTotal)
            .putString("streakLastRecordedDate", today)
            .putInt("currentStreakDays", streak)
            .apply()
    }

    fun getReadingStats(booksCount: Int = 0): Triple<Int, String, Int> {
        val totalSeconds = prefs.getLong("totalReadingSeconds", 0L)
        val streak = prefs.getInt("currentStreakDays", if (booksCount > 0) 1 else 0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val timeLabel = when {
            hours > 0 -> "${hours}g ${minutes}p"
            minutes > 0 -> "${minutes} phút"
            totalSeconds > 0 -> "${totalSeconds} giây"
            else -> "0 phút"
        }
        return Triple(streak, timeLabel, booksCount)
    }

    fun loadAppSettings(booksCount: Int = 0, dictionaryWordCount: Int? = null): AppSettings {
        val themeName = prefs.getString("themeMode", ReadingThemeMode.IVORY.name) ?: ReadingThemeMode.IVORY.name
        val theme = runCatching { ReadingThemeMode.valueOf(themeName) }.getOrDefault(ReadingThemeMode.IVORY)
        val (streak, timeLabel, totalBooks) = getReadingStats(booksCount)
        val highlightName = prefs.getString("highlightLevel", HighlightLevel.SENTENCE.name)
            ?: HighlightLevel.SENTENCE.name
        val highlightLevel = runCatching { HighlightLevel.valueOf(highlightName) }
            .getOrDefault(HighlightLevel.SENTENCE)
        val dictCount = dictionaryWordCount ?: loadPronunciationRules().size

        val daysString = prefs.getString("reminderDays", "1,2,3,4,5,6,7") ?: "1,2,3,4,5,6,7"
        val daysSet = daysString.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet().ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
        val reminderEnabled = prefs.getBoolean("reminderEnabled", true)
        val reminderHour = prefs.getInt("reminderHour", 21)
        val reminderMinute = prefs.getInt("reminderMinute", 0)
        val userName = prefs.getString("userName", "Độc giả") ?: "Độc giả"
        val userRoleBadge = prefs.getString("userRoleBadge", "Độc giả") ?: "Độc giả"
        val backupAccountEmail = prefs.getString("backupAccountEmail", "") ?: ""
        val lastBackupTimestamp = prefs.getLong("lastBackupTimestamp", 0L)

        return AppSettings(
            userName = userName,
            userRoleBadge = userRoleBadge,
            streakDays = streak,
            totalListeningTimeLabel = timeLabel,
            totalBooksRead = totalBooks,
            themeMode = theme,
            fontSizePx = prefs.getInt("fontSizePx", 17),
            autoScrollEnabled = prefs.getBoolean("autoScrollEnabled", true),
            stopAtEndOfChapter = prefs.getBoolean("stopAtEndOfChapter", false),
            defaultSpeed = prefs.getFloat("defaultSpeed", 1.15f),
            dictionaryWordCount = dictCount,
            cloudSyncEnabled = prefs.getBoolean("cloudSyncEnabled", true),
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            reminderDays = daysSet,
            reminderTimeLabel = prefs.getString("reminderTimeLabel", "Nhắc đọc 21:00 (Hàng ngày)") ?: "Nhắc đọc 21:00 (Hàng ngày)",
            defaultSleepTimerMinutes = prefs.getInt("defaultSleepTimerMinutes", 30),
            highlightLevel = highlightLevel,
            backupAccountEmail = backupAccountEmail,
            lastBackupTimestamp = lastBackupTimestamp
        )
    }

    fun saveAppSettings(settings: AppSettings) {
        prefs.edit()
            .putString("userName", settings.userName)
            .putString("userRoleBadge", settings.userRoleBadge)
            .putString("themeMode", settings.themeMode.name)
            .putInt("fontSizePx", settings.fontSizePx)
            .putBoolean("autoScrollEnabled", settings.autoScrollEnabled)
            .putBoolean("stopAtEndOfChapter", settings.stopAtEndOfChapter)
            .putFloat("defaultSpeed", settings.defaultSpeed)
            .putBoolean("cloudSyncEnabled", settings.cloudSyncEnabled)
            .putBoolean("reminderEnabled", settings.reminderEnabled)
            .putInt("reminderHour", settings.reminderHour)
            .putInt("reminderMinute", settings.reminderMinute)
            .putString("reminderDays", settings.reminderDays.joinToString(","))
            .putString("reminderTimeLabel", settings.reminderSummary)
            .putInt("defaultSleepTimerMinutes", settings.defaultSleepTimerMinutes)
            .putString("highlightLevel", settings.highlightLevel.name)
            .putString("backupAccountEmail", settings.backupAccountEmail)
            .putLong("lastBackupTimestamp", settings.lastBackupTimestamp)
            .apply()
    }

    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean("has_completed_onboarding", false)

    fun setCompletedOnboarding(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
    }

    fun loadVoiceTuningSettings(): VoiceTuningSettings {
        return VoiceTuningSettings(
            speed = prefs.getFloat("tuning_speed", 1.15f),
            pitch = prefs.getInt("tuning_pitch", 0),
            sentencePauseSeconds = prefs.getFloat("tuning_pause", 0.4f)
        )
    }

    fun saveVoiceTuningSettings(settings: VoiceTuningSettings) {
        prefs.edit()
            .putFloat("tuning_speed", settings.speed)
            .putInt("tuning_pitch", settings.pitch)
            .putFloat("tuning_pause", settings.sentencePauseSeconds)
            .apply()
    }

    fun loadDefaultVoiceId(): String? {
        return prefs.getString("default_system_voice_id", null)
    }

    fun saveDefaultVoiceId(voiceId: String) {
        prefs.edit().putString("default_system_voice_id", voiceId).apply()
    }

    fun loadVoiceItems(): List<VoiceItem> {
        val installedAiIds = AiVoiceModelManager(context).getInstalledAiVoiceIds()
        val defaultId = loadDefaultVoiceId()
        return VoiceCatalog.buildCombinedVoiceItems(
            discoveredVoices = emptyList(),
            installedAiVoiceIds = installedAiIds,
            activeDefaultId = defaultId
        )
    }

    fun saveVoiceItems(voices: List<VoiceItem>) {
        val defaultVoice = voices.firstOrNull { it.isDefault || it.installStatus == VoiceInstallStatus.ACTIVE_DEFAULT }
        if (defaultVoice != null) {
            saveDefaultVoiceId(defaultVoice.id)
        }
    }

    fun loadPronunciationRules(): List<PronunciationRule> {
        val rawJson = prefs.getString("pronunciation_rules_v1", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawJson)
            val list = mutableListOf<PronunciationRule>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val word = obj.optString("originalWord").trim()
                val replacement = obj.optString("spokenReplacement").trim()
                if (word.isNotBlank() && replacement.isNotBlank()) {
                    list.add(
                        PronunciationRule(
                            id = obj.optString("id", "rule_$i"),
                            originalWord = word,
                            spokenReplacement = replacement
                        )
                    )
                }
            }
            list
        }.getOrElse { emptyList() }
    }

    fun savePronunciationRules(rules: List<PronunciationRule>) {
        runCatching {
            val array = JSONArray()
            for (rule in rules) {
                array.put(
                    JSONObject().apply {
                        put("id", rule.id)
                        put("originalWord", rule.originalWord)
                        put("spokenReplacement", rule.spokenReplacement)
                    }
                )
            }
            prefs.edit().putString("pronunciation_rules_v1", array.toString()).apply()
        }
    }

    fun loadLastSelectedBookId(defaultId: String = ""): String {
        val stored = prefs.getString("lastSelectedBookId", defaultId) ?: defaultId
        if (stored in REMOVED_SAMPLE_IDS) {
            saveLastSelectedBookId(defaultId)
            return defaultId
        }
        return stored
    }

    fun saveLastSelectedBookId(bookId: String) {
        prefs.edit().putString("lastSelectedBookId", bookId).apply()
    }

    fun loadAmbientMusicSettings(): AmbientMusicSettings {
        return AmbientMusicSettings(
            isEnabledWhenReading = prefs.getBoolean("ambient_enabled_when_reading", false),
            selectedTrackId = prefs.getString("ambient_selected_track_id", "builtin_piano") ?: "builtin_piano",
            volumePercent = prefs.getInt("ambient_volume_percent", 25).coerceIn(0, 100)
        )
    }

    fun saveAmbientMusicSettings(settings: AmbientMusicSettings) {
        prefs.edit()
            .putBoolean("ambient_enabled_when_reading", settings.isEnabledWhenReading)
            .putString("ambient_selected_track_id", settings.selectedTrackId)
            .putInt("ambient_volume_percent", settings.volumePercent.coerceIn(0, 100))
            .apply()
    }

    fun loadCustomAmbientTracks(): List<AmbientTrack> {
        val rawJson = prefs.getString("custom_ambient_tracks_v1", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(rawJson)
            val list = mutableListOf<AmbientTrack>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val filePath = obj.optString("filePath")
                if (filePath.isBlank() || !File(filePath).exists()) continue
                list.add(
                    AmbientTrack(
                        id = obj.optString("id", "custom_$i"),
                        title = obj.optString("title", "Bản nhạc nền"),
                        subtitle = obj.optString("subtitle", "Nhạc từ thiết bị"),
                        category = obj.optString("category", "Nhạc của bạn"),
                        durationLabel = obj.optString("durationLabel", "Lặp vô tận"),
                        filePath = filePath,
                        isBuiltIn = false,
                        iconType = AmbientIconType.CUSTOM
                    )
                )
            }
            list
        }.getOrElse { emptyList() }
    }

    fun saveCustomAmbientTracks(tracks: List<AmbientTrack>) {
        runCatching {
            val array = JSONArray()
            for (t in tracks.filterNot { it.isBuiltIn }) {
                array.put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("subtitle", t.subtitle)
                        put("category", t.category)
                        put("durationLabel", t.durationLabel)
                        put("filePath", t.filePath)
                    }
                )
            }
            prefs.edit().putString("custom_ambient_tracks_v1", array.toString()).apply()
        }
    }

    private fun parseBookmarksJson(array: JSONArray?): List<BookBookmark>? {
        if (array == null) return null
        val list = mutableListOf<BookBookmark>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            list.add(
                BookBookmark(
                    id = item.optString("id", "bm_$i"),
                    chapterTitle = item.optString("chapterTitle", "Chương 1"),
                    quoteText = item.optString("quoteText", ""),
                    timestampLabel = item.optString("timestampLabel", "00:00"),
                    chapterNumber = item.optInt("chapterNumber", 1),
                    sentenceIndex = item.optInt("sentenceIndex", 0)
                )
            )
        }
        return list
    }
}
