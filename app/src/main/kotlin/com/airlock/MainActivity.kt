package com.airlock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.IntentCompat
import com.airlock.data.SettingsRepository
import com.airlock.ui.AppRoot
import com.airlock.ui.Screen
import com.airlock.ui.theme.AirlockTheme

class MainActivity : ComponentActivity() {

    // Reactive so a share delivered to an already-running instance (singleTop) re-routes the UI.
    private val entry = mutableStateOf<Screen>(Screen.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        entry.value = parseEntry(intent)
        val settingsRepo = SettingsRepository(applicationContext)
        setContent {
            AirlockTheme {
                AppRoot(initial = entry.value, settingsRepo = settingsRepo)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        entry.value = parseEntry(intent)
    }

    /** Map an incoming intent (launcher / share / process-text) to a starting screen. */
    private fun parseEntry(intent: Intent?): Screen {
        if (intent == null) return Screen.Home
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val type = intent.type ?: ""
                if (type.startsWith("text/")) {
                    Screen.TextScrub(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
                } else if (type.startsWith("image/")) {
                    val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    if (uri != null) Screen.ImageScrub(listOf(uri)) else Screen.Home
                } else Screen.Home
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (!uris.isNullOrEmpty()) Screen.ImageScrub(uris.toList()) else Screen.Home
            }

            Intent.ACTION_PROCESS_TEXT -> {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
                Screen.TextScrub(text)
            }

            else -> Screen.Home
        }
    }
}
