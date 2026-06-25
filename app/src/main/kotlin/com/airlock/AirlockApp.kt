package com.airlock

import android.app.Application
import com.airlock.scrub.ImageScrubber
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class AirlockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Loads PDFBox's bundled resources from app assets — fully offline.
        PDFBoxResourceLoader.init(applicationContext)
        // Start each run with a clean export cache — Airlock never accumulates your files.
        ImageScrubber.clearExports(this)
    }
}
