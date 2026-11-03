package com.vietreader.tts

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Main Application class for Wordwave.
 * Initializes essential singletons and third-party native libraries like PDFBox-Android.
 */
class WordwaveApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initPdfBox()
    }

    private fun initPdfBox() {
        runCatching {
            // Initializes font tables, glyphs, and character encodings for PDF parsing
            PDFBoxResourceLoader.init(applicationContext)
            Log.d(TAG, "PDFBoxResourceLoader initialized successfully")
        }.onFailure { e ->
            Log.e(TAG, "Failed to initialize PDFBoxResourceLoader", e)
        }
    }

    companion object {
        private const val TAG = "WordwaveApp"
    }
}
