package com.vietreader.tts.domain.model

import android.speech.tts.Voice
import androidx.compose.runtime.Immutable

enum class VoiceGroup(
    val title: String,
    val subtitle: String,
    val badgeText: String
) {
    SYSTEM(
        title = "Giọng hệ thống",
        subtitle = "Dữ liệu trực tiếp từ Text-to-Speech trên thiết bị",
        badgeText = "Có sẵn"
    ),
    AI_HIGH_QUALITY(
        title = "Giọng AI chất lượng cao",
        subtitle = "Cần máy từ 4 GB RAM • Chạy ngoại tuyến sau khi tải",
        badgeText = "Chất lượng cao"
    )
}

enum class VoiceInstallStatus {
    ACTIVE_DEFAULT,
    DOWNLOADED,
    DOWNLOADING,
    NOT_DOWNLOADED
}

enum class VoiceDownloadFilter(val label: String) {
    ALL("Tất cả"),
    DOWNLOADED("Đã tải"),
    NOT_DOWNLOADED("Chưa tải")
}

enum class VoiceRegionFilter(val label: String) {
    ALL("Tất cả")
}

@Immutable
data class VoiceItem(
    val id: String,
    val initials: String,
    val name: String,
    val group: VoiceGroup,
    val region: String,
    val gender: String,
    val sizeMb: Int,
    val installStatus: VoiceInstallStatus,
    val downloadProgressPercent: Int = 100,
    val isDownloadPaused: Boolean = false,
    val isDefault: Boolean = false,
    val systemVoiceName: String? = null,
    val qualityLabel: String = "Chất lượng cao",
    val modelDownloadUrl: String? = null,
    val expectedMinBytes: Long = 50_000_000L
) {
    val isAiVoice: Boolean
        get() = group == VoiceGroup.AI_HIGH_QUALITY

    val metaLine: String
        get() = if (isAiVoice) {
            buildList {
                if (gender.isNotBlank()) add(gender)
                if (qualityLabel.isNotBlank()) add(qualityLabel)
                if (sizeMb > 0) add("$sizeMb MB")
            }.joinToString(" • ")
        } else {
            buildList {
                if (region.isNotBlank()) add(region)
                if (gender.isNotBlank()) add(gender)
                if (qualityLabel.isNotBlank()) add(qualityLabel)
            }.joinToString(" • ")
        }

    val downloadedMbFormatted: String
        get() {
            val currentMb = (sizeMb * (downloadProgressPercent / 100f))
            return "%.1f / %d MB (%d%%)".format(currentMb, sizeMb, downloadProgressPercent)
        }

    fun toVoiceOption(): VoiceOption {
        return VoiceOption(
            id = id,
            name = name,
            regionTag = if (isAiVoice) "AI • $gender" else gender.ifBlank { "Ngoại tuyến" },
            description = metaLine,
            isAiVoice = isAiVoice
        )
    }
}

@Immutable
data class VoiceTuningSettings(
    val speed: Float = 1.05f,
    val pitch: Int = 0,
    val sentencePauseSeconds: Float = 0.4f
)

@Immutable
data class PronunciationRule(
    val id: String,
    val originalWord: String,
    val spokenReplacement: String
)

/**
 * Catalog mapping both:
 * 1. High-quality Offline AI Voices (`Sherpa-ONNX` / Piper VITS Vietnamese models hosted on Hugging Face)
 * 2. Real Android system TTS voices (`android.speech.tts.Voice`) discovered on the device.
 */
object VoiceCatalog {

    private const val HF_SHERPA_BASE_URL =
        "https://huggingface.co/doof-ferb/nghitts-copy/resolve/main/sherpa-onnx"

    const val SHARED_TOKENS_URL = "$HF_SHERPA_BASE_URL/tokens.txt"

    val defaultAiVoiceItems: List<VoiceItem> = listOf(
        VoiceItem(
            id = "ai_ngocngan",
            initials = "NN",
            name = "Nguyễn Ngọc Ngạn",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Kể chuyện",
            gender = "Nam",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/ngocngan3701.onnx"
        ),
        VoiceItem(
            id = "ai_ngochuyen",
            initials = "NH",
            name = "Ngọc Huyền",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Tự nhiên",
            gender = "Nữ",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/ngochuyennew.onnx"
        ),
        VoiceItem(
            id = "ai_haidang",
            initials = "HĐ",
            name = "Hải Đăng",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Trầm ấm",
            gender = "Nam",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/deepman3909.onnx"
        ),
        VoiceItem(
            id = "ai_adam",
            initials = "AD",
            name = "Adam",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Thuyết minh",
            gender = "Nam",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/adam1.onnx"
        ),
        VoiceItem(
            id = "ai_doantrang",
            initials = "ĐT",
            name = "Đoan Trang",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Dịu dàng",
            gender = "Nữ",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/phuongtrang.onnx"
        ),
        VoiceItem(
            id = "ai_ductri",
            initials = "ĐT",
            name = "Đức Trí",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Rõ ràng",
            gender = "Nam",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/duyoryx3175.onnx"
        ),
        VoiceItem(
            id = "ai_kimthanh",
            initials = "KT",
            name = "Kim Thanh",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Trong trẻo",
            gender = "Nữ",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/chieuthanh.onnx"
        ),
        VoiceItem(
            id = "ai_maianh",
            initials = "MA",
            name = "Mai Anh",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Mượt mà",
            gender = "Nữ",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/banmai.onnx"
        ),
        VoiceItem(
            id = "ai_minhduc",
            initials = "MĐ",
            name = "Minh Đức",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Điềm đạm",
            gender = "Nam",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/manhdung.onnx"
        ),
        VoiceItem(
            id = "ai_minhtriet",
            initials = "MT",
            name = "Minh Triết",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Truyền cảm",
            gender = "Nam",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/minhkhang.onnx"
        ),
        VoiceItem(
            id = "ai_myduyen",
            initials = "MD",
            name = "Mỹ Duyên",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Cảm xúc",
            gender = "Nữ",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/lacphi.onnx"
        ),
        VoiceItem(
            id = "ai_ngoclinh",
            initials = "NL",
            name = "Ngọc Linh",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Thanh lịch",
            gender = "Nữ",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/ngochuyen.onnx"
        ),
        VoiceItem(
            id = "ai_ngoctran",
            initials = "NT",
            name = "Ngọc Trân",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Nhẹ nhàng",
            gender = "Nữ",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/maiphuong.onnx"
        ),
        VoiceItem(
            id = "ai_phamtuyen",
            initials = "PT",
            name = "Phạm Tuyên",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Vững chãi",
            gender = "Nam",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/minhquang.onnx"
        ),
        VoiceItem(
            id = "ai_vietthao",
            initials = "VT",
            name = "Việt Thảo",
            group = VoiceGroup.AI_HIGH_QUALITY,
            region = "Đọc truyện",
            gender = "Nam",
            sizeMb = 61,
            installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
            downloadProgressPercent = 0,
            isDefault = false,
            qualityLabel = "Chất lượng cao",
            modelDownloadUrl = "$HF_SHERPA_BASE_URL/vietthao3886.onnx"
        )
    )

    val defaultSystemVoiceItems: List<VoiceItem> = listOf(
        VoiceItem(
            id = "sys_gft",
            initials = "1",
            name = "Giọng hệ thống 1",
            group = VoiceGroup.SYSTEM,
            region = "Tiếng Việt",
            gender = "Ngoại tuyến",
            sizeMb = 0,
            installStatus = VoiceInstallStatus.ACTIVE_DEFAULT,
            isDefault = true,
            systemVoiceName = "vi-vn-x-gft-local",
            qualityLabel = "Chất lượng cao"
        ),
        VoiceItem(
            id = "sys_vie",
            initials = "2",
            name = "Giọng hệ thống 2",
            group = VoiceGroup.SYSTEM,
            region = "Tiếng Việt",
            gender = "Ngoại tuyến",
            sizeMb = 0,
            installStatus = VoiceInstallStatus.DOWNLOADED,
            isDefault = false,
            systemVoiceName = "vi-vn-x-vie-local",
            qualityLabel = "Chất lượng cao"
        ),
        VoiceItem(
            id = "sys_vid",
            initials = "3",
            name = "Giọng hệ thống 3",
            group = VoiceGroup.SYSTEM,
            region = "Tiếng Việt",
            gender = "Ngoại tuyến",
            sizeMb = 0,
            installStatus = VoiceInstallStatus.DOWNLOADED,
            isDefault = false,
            systemVoiceName = "vi-vn-x-vid-local",
            qualityLabel = "Chất lượng cao"
        ),
        VoiceItem(
            id = "sys_vif",
            initials = "4",
            name = "Giọng hệ thống 4",
            group = VoiceGroup.SYSTEM,
            region = "Tiếng Việt",
            gender = "Ngoại tuyến",
            sizeMb = 0,
            installStatus = VoiceInstallStatus.DOWNLOADED,
            isDefault = false,
            systemVoiceName = "vi-vn-x-vif-local",
            qualityLabel = "Chất lượng cao"
        ),
        VoiceItem(
            id = "sys_vig",
            initials = "5",
            name = "Giọng hệ thống 5",
            group = VoiceGroup.SYSTEM,
            region = "Tiếng Việt",
            gender = "Ngoại tuyến",
            sizeMb = 0,
            installStatus = VoiceInstallStatus.DOWNLOADED,
            isDefault = false,
            systemVoiceName = "vi-vn-x-vig-local",
            qualityLabel = "Chất lượng cao"
        )
    )

    val defaultVoiceItems: List<VoiceItem>
        get() = defaultSystemVoiceItems + defaultAiVoiceItems

    val defaultVoiceOptions: List<VoiceOption>
        get() = defaultSystemVoiceItems.map { it.toVoiceOption() }

    val defaultAvailableVoiceNames: List<String>
        get() = defaultSystemVoiceItems.map { it.name }

    /**
     * Builds the combined list of discovered Android System TTS voices (placed first)
     * and AI Voices (checking which `.onnx` models are physically downloaded on disk),
     * applying [activeDefaultId] as the single `ACTIVE_DEFAULT`.
     */
    fun buildCombinedVoiceItems(
        discoveredVoices: List<Voice>,
        installedAiVoiceIds: Set<String>,
        activeDefaultId: String? = null,
        existingItems: List<VoiceItem> = emptyList()
    ): List<VoiceItem> {
        val systemMapped = buildSystemVoiceObjects(discoveredVoices)

        val existingById = existingItems.associateBy { it.id }
        val aiMapped = defaultAiVoiceItems.map { template ->
            val current = existingById[template.id]
            val isOnDisk = template.id in installedAiVoiceIds
            when {
                current?.installStatus == VoiceInstallStatus.DOWNLOADING -> current
                isOnDisk -> template.copy(
                    installStatus = VoiceInstallStatus.DOWNLOADED,
                    downloadProgressPercent = 100,
                    isDownloadPaused = false,
                    isDefault = false
                )
                else -> template.copy(
                    installStatus = VoiceInstallStatus.NOT_DOWNLOADED,
                    downloadProgressPercent = 0,
                    isDownloadPaused = false,
                    isDefault = false
                )
            }
        }

        val allItems = systemMapped + aiMapped
        val validDefaultId = activeDefaultId?.takeIf { id ->
            allItems.any {
                it.id == id && (it.group == VoiceGroup.SYSTEM || it.id in installedAiVoiceIds)
            }
        } ?: systemMapped.firstOrNull()?.id ?: "sys_gft"

        return allItems.map { item ->
            if (item.id == validDefaultId) {
                item.copy(
                    isDefault = true,
                    installStatus = VoiceInstallStatus.ACTIVE_DEFAULT,
                    downloadProgressPercent = 100
                )
            } else if (item.installStatus == VoiceInstallStatus.ACTIVE_DEFAULT) {
                item.copy(
                    isDefault = false,
                    installStatus = VoiceInstallStatus.DOWNLOADED
                )
            } else {
                item.copy(isDefault = false)
            }
        }
    }

    private fun buildSystemVoiceObjects(discoveredVoices: List<Voice>): List<VoiceItem> {
        if (discoveredVoices.isEmpty()) return defaultSystemVoiceItems.map {
            it.copy(isDefault = false, installStatus = VoiceInstallStatus.DOWNLOADED)
        }

        val nonAliasVoices = discoveredVoices.filterNot {
            it.name.equals("vi-VN-language", ignoreCase = true) ||
                it.name.equals("vi-vn-language", ignoreCase = true)
        }
        val pool = nonAliasVoices.ifEmpty { discoveredVoices }
        val localVoices = pool.filter { !it.isNetworkConnectionRequired }
        val selectedPool = localVoices.ifEmpty { pool }

        val variantOrder = listOf("gft", "vie", "vid", "vif", "vig")
        val distinctVoices = selectedPool
            .distinctBy { extractVariantKey(it.name) }
            .sortedWith(
                compareBy<Voice> { voice ->
                    val key = extractVariantKey(voice.name)
                    val idx = variantOrder.indexOf(key)
                    if (idx >= 0) idx else Int.MAX_VALUE
                }.thenBy { it.name }
            )

        val mapped = distinctVoices.mapIndexed { idx, voice ->
            val variantKey = extractVariantKey(voice.name)
            val voiceNumber = idx + 1
            val localeTag = voice.locale?.toLanguageTag().orEmpty().ifBlank { "vi-VN" }
            val localeDisplay = if (localeTag.startsWith("vi", ignoreCase = true)) {
                "Tiếng Việt"
            } else {
                voice.locale?.displayLanguage?.replaceFirstChar { it.uppercase() } ?: "Ngôn ngữ hệ thống"
            }
            val connectionDisplay = if (voice.isNetworkConnectionRequired) "Trực tuyến" else "Ngoại tuyến"
            val qualityDisplay = when {
                voice.quality >= Voice.QUALITY_VERY_HIGH -> "Chất lượng rất cao"
                voice.quality >= Voice.QUALITY_HIGH -> "Chất lượng cao"
                voice.quality >= Voice.QUALITY_NORMAL -> "Chất lượng chuẩn"
                else -> "Chất lượng cơ bản"
            }

            VoiceItem(
                id = "sys_$variantKey",
                initials = "$voiceNumber",
                name = "Giọng hệ thống $voiceNumber",
                group = VoiceGroup.SYSTEM,
                region = localeDisplay,
                gender = connectionDisplay,
                sizeMb = 0,
                installStatus = VoiceInstallStatus.DOWNLOADED,
                downloadProgressPercent = 100,
                isDefault = false,
                systemVoiceName = voice.name,
                qualityLabel = qualityDisplay
            )
        }

        return mapped.ifEmpty {
            defaultSystemVoiceItems.map {
                it.copy(isDefault = false, installStatus = VoiceInstallStatus.DOWNLOADED)
            }
        }
    }

    fun extractVariantKey(voiceName: String): String {
        val lower = voiceName.lowercase()
        return when {
            lower.contains("gft") -> "gft"
            lower.contains("vie") -> "vie"
            lower.contains("vid") -> "vid"
            lower.contains("vif") -> "vif"
            lower.contains("vig") -> "vig"
            else -> lower
                .removePrefix("vi-vn-x-")
                .removeSuffix("-local")
                .removeSuffix("-network")
                .ifBlank { "default" }
        }
    }
}
