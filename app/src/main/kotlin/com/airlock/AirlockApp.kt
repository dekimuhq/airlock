package com.airlock

import android.app.Application
import com.airlock.scrub.ImageScrubber

class AirlockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Start each run with a clean export cache — Airlock never accumulates your images.
        ImageScrubber.clearExports(this)
    }
}
