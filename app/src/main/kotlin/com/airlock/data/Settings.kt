package com.airlock.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airlock.core.PiiType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore is a key-value file, not a database. No Room, no SQLite.
private val Context.dataStore by preferencesDataStore(name = "airlock_settings")

/** User-tunable scrub configuration. Tracker stripping is always on; the rest are toggles. */
data class ScrubSettings(
    val redactPiiInText: Boolean = true,
    val enabledPiiTypes: Set<PiiType> = PiiType.entries.toSet(),
    val customParams: Set<String> = emptySet(),
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val REDACT_PII = booleanPreferencesKey("redact_pii")
        val PII_TYPES = stringSetPreferencesKey("pii_types")
        val CUSTOM_PARAMS = stringSetPreferencesKey("custom_params")
    }

    val settings: Flow<ScrubSettings> = context.dataStore.data.map { p ->
        val types = p[Keys.PII_TYPES]
            ?.mapNotNull { runCatching { PiiType.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: PiiType.entries.toSet()
        ScrubSettings(
            redactPiiInText = p[Keys.REDACT_PII] ?: true,
            enabledPiiTypes = types,
            customParams = p[Keys.CUSTOM_PARAMS] ?: emptySet(),
        )
    }

    suspend fun setRedactPii(value: Boolean) {
        context.dataStore.edit { it[Keys.REDACT_PII] = value }
    }

    suspend fun togglePiiType(type: PiiType, enabled: Boolean) {
        context.dataStore.edit { p ->
            val current = p[Keys.PII_TYPES]?.toMutableSet()
                ?: PiiType.entries.map { it.name }.toMutableSet()
            if (enabled) current.add(type.name) else current.remove(type.name)
            p[Keys.PII_TYPES] = current
        }
    }

    suspend fun addCustomParam(param: String) {
        val clean = param.trim().lowercase()
        if (clean.isEmpty()) return
        context.dataStore.edit { p ->
            p[Keys.CUSTOM_PARAMS] = (p[Keys.CUSTOM_PARAMS] ?: emptySet()) + clean
        }
    }

    suspend fun removeCustomParam(param: String) {
        context.dataStore.edit { p ->
            p[Keys.CUSTOM_PARAMS] = (p[Keys.CUSTOM_PARAMS] ?: emptySet()) - param
        }
    }
}
