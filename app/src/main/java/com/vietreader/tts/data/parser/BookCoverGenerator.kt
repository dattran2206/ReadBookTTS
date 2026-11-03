package com.vietreader.tts.data.parser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.vietreader.tts.domain.model.CoverGradient
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Generates aesthetic typography-based book cover artwork for TXT and document files
 * that lack embedded cover art.
 */
object BookCoverGenerator {

    private data class PaletteTheme(
        val startColorHex: Long,
        val centerColorHex: Long,
        val endColorHex: Long,
        val borderColor: Int,
        val textColor: Int,
        val accentColor: Int
    )

    private val PRESET_PALETTES = listOf(
        // 1. Deep Emerald
        PaletteTheme(
            startColorHex = 0xFF144D3B,
            centerColorHex = 0xFF0D3327,
            endColorHex = 0xFF071E17,
            borderColor = 0xFFC9A86A.toInt(),
            textColor = 0xFFFBF8F2.toInt(),
            accentColor = 0xFFD4AF37.toInt()
        ),
        // 2. Royal Sapphire
        PaletteTheme(
            startColorHex = 0xFF153E5C,
            centerColorHex = 0xFF0C263A,
            endColorHex = 0xFF06141F,
            borderColor = 0xFFD8B26E.toInt(),
            textColor = 0xFFF7FAFC.toInt(),
            accentColor = 0xFFECC94B.toInt()
        ),
        // 3. Vintage Burgundy / Wine
        PaletteTheme(
            startColorHex = 0xFF581C25,
            centerColorHex = 0xFF381017,
            endColorHex = 0xFF1F080C,
            borderColor = 0xFFE0C080.toInt(),
            textColor = 0xFFFFF5F5.toInt(),
            accentColor = 0xFFF6E05E.toInt()
        ),
        // 4. Warm Sepia / Leather
        PaletteTheme(
            startColorHex = 0xFF5D2A0C,
            centerColorHex = 0xFF3E1B07,
            endColorHex = 0xFF210D03,
            borderColor = 0xFFDEB887.toInt(),
            textColor = 0xFFFFFDF5.toInt(),
            accentColor = 0xFFFBD38D.toInt()
        ),
        // 5. Midnight Obsidian
        PaletteTheme(
            startColorHex = 0xFF2D3748,
            centerColorHex = 0xFF1A202C,
            endColorHex = 0xFF10141D,
            borderColor = 0xFFCBD5E0.toInt(),
            textColor = 0xFFF7FAFC.toInt(),
            accentColor = 0xFFE2E8F0.toInt()
        )
    )

    /**
     * Generates a high-resolution, beautifully formatted book cover image and saves it to [outputFile].
     * Returns the [CoverGradient] representing the theme's colors.
     * Guarantees [Bitmap.recycle] invocation to eliminate native graphic memory leaks.
     */
    fun generateCover(
        title: String,
        author: String,
        category: String,
        outputFile: File,
        width: Int = 600,
        height: Int = 900
    ): CoverGradient {
        val paletteIndex = abs(title.hashCode()) % PRESET_PALETTES.size
        val theme = PRESET_PALETTES[paletteIndex]

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)

            // 1. Background Gradient
            val bgShader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(theme.startColorHex.toInt(), theme.centerColorHex.toInt(), theme.endColorHex.toInt()),
                floatArrayOf(0.0f, 0.5f, 1.0f),
                Shader.TileMode.CLAMP
            )
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = bgShader }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // 2. Ornamental Double Border
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.borderColor
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            val outerInset = 28f
            val innerInset = 36f
            canvas.drawRect(RectF(outerInset, outerInset, width - outerInset, height - outerInset), borderPaint)
            borderPaint.strokeWidth = 1.2f
            canvas.drawRect(RectF(innerInset, innerInset, width - innerInset, height - innerInset), borderPaint)

            // Corner ornaments
            val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accentColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(outerInset, outerInset, 4f, cornerPaint)
            canvas.drawCircle(width - outerInset, outerInset, 4f, cornerPaint)
            canvas.drawCircle(outerInset, height - outerInset, 4f, cornerPaint)
            canvas.drawCircle(width - outerInset, height - outerInset, 4f, cornerPaint)

            // 3. Category / Tag Pill
            val tagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accentColor
                textSize = 22f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.2f
            }
            canvas.drawText(category.uppercase(), width / 2f, 100f, tagPaint)

            // Decorative horizontal rule under category
            borderPaint.strokeWidth = 1.5f
            canvas.drawLine(width / 2f - 60f, 125f, width / 2f + 60f, 125f, borderPaint)
            canvas.drawCircle(width / 2f, 125f, 3.5f, cornerPaint)

            // 4. Book Title Layout (Auto-scaling multi-line)
            val titleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.textColor
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }

            val availableWidth = (width - 120).coerceAtLeast(200)
            var titleSize = 48f
            titleTextPaint.textSize = titleSize

            // Adjust font size based on title length
            if (title.length > 50) {
                titleSize = 34f
            } else if (title.length > 25) {
                titleSize = 40f
            }
            titleTextPaint.textSize = titleSize

            val titleLayout = StaticLayout.Builder.obtain(
                title,
                0,
                title.length,
                titleTextPaint,
                availableWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(6f, 1.15f)
                .setMaxLines(4)
                .build()

            val titleY = ((height / 2f) - (titleLayout.height / 2f) - 30f).coerceAtLeast(180f)
            canvas.save()
            canvas.translate(60f, titleY)
            titleLayout.draw(canvas)
            canvas.restore()

            // Decorative rule below title
            val dividerY = titleY + titleLayout.height + 40f
            canvas.drawLine(width / 2f - 75f, dividerY, width / 2f + 75f, dividerY, borderPaint)
            canvas.drawCircle(width / 2f, dividerY, 4f, cornerPaint)

            // 5. Author Name
            val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.textColor
                textSize = 26f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
                alpha = 230
            }
            val cleanAuthor = author.ifBlank { "Tác giả vô danh" }
            canvas.drawText(cleanAuthor, width / 2f, (height - 110f).coerceAtLeast(dividerY + 60f), authorPaint)

            // 6. Footer Subtitle
            val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.accentColor
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.15f
                alpha = 180
            }
            canvas.drawText("WORDWAVE AI AUDIOBOOK", width / 2f, height - 60f, footerPaint)

            // 7. Save to JPEG file
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
        } finally {
            bitmap.recycle()
        }

        return CoverGradient(
            startColorHex = theme.startColorHex,
            centerColorHex = theme.centerColorHex,
            endColorHex = theme.endColorHex
        )
    }
}
