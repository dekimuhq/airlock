package com.airlock.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.airlock.data.ScrubSettings
import com.airlock.data.SettingsRepository
import com.airlock.ui.theme.Bg

/** Where the user is. A plain state machine — no nav library, no fragile arg passing. */
sealed interface Screen {
    data object Home : Screen
    data class TextScrub(val initial: String) : Screen
    data class ImageScrub(val uris: List<Uri>) : Screen
    data object Settings : Screen
}

/** Counts for the current run only; never persisted. */
class SessionStats {
    var imagesCleaned by mutableIntStateOf(0)
    var linksCleaned by mutableIntStateOf(0)
    var paramsRemoved by mutableIntStateOf(0)
    var piiRedacted by mutableIntStateOf(0)
}

@Composable
fun AppRoot(initial: Screen, settingsRepo: SettingsRepository) {
    // Keyed on `initial` so a new share intent (delivered via onNewIntent) resets the flow.
    var screen by remember(initial) { mutableStateOf(initial) }
    val settings by settingsRepo.settings.collectAsState(initial = ScrubSettings())
    val stats = remember { SessionStats() }

    Surface(color = Bg, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            when (val s = screen) {
                is Screen.Home -> HomeScreen(
                    stats = stats,
                    onCleanText = { screen = Screen.TextScrub("") },
                    onImagesPicked = { uris -> if (uris.isNotEmpty()) screen = Screen.ImageScrub(uris) },
                    onSettings = { screen = Screen.Settings },
                )
                is Screen.TextScrub -> TextScrubScreen(
                    initialText = s.initial,
                    settings = settings,
                    stats = stats,
                    onBack = { screen = Screen.Home },
                )
                is Screen.ImageScrub -> ImageScrubScreen(
                    uris = s.uris,
                    stats = stats,
                    onBack = { screen = Screen.Home },
                )
                is Screen.Settings -> SettingsScreen(
                    settings = settings,
                    repo = settingsRepo,
                    onBack = { screen = Screen.Home },
                )
            }
        }
    }
}
