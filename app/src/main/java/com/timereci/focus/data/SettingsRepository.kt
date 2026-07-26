package com.timereci.focus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * How photos are cropped for display across the feed and carousel.
 * [ratio] is width / height, matching Compose's `Modifier.aspectRatio`.
 */
enum class PhotoAspect(val ratio: Float, val label: String) {
    SQUARE(1f, "1:1"),
    PORTRAIT(3f / 4f, "3:4");

    companion object {
        fun from(name: String?): PhotoAspect = entries.firstOrNull { it.name == name } ?: PORTRAIT
    }
}

/** Preset durations offered before a session starts (minutes). User-editable via long-press. */
val DEFAULT_DURATION_PRESETS = listOf(15, 25, 45, 60, 90)

/** User-facing behavior settings, backed by Preferences DataStore. */
data class FocusSettings(
    /** Default timer duration in ms (settings screen default value). */
    val defaultDurationMs: Long = 25 * 60_000L,
    /** true = timer keeps running while the comment sheet is open (honest focus time). */
    val keepRunningWhileCommenting: Boolean = true,
    /** Crop ratio used when displaying photos in the feed and carousel. */
    val photoAspect: PhotoAspect = PhotoAspect.PORTRAIT,
    /** Quick-pick minute presets shown on the timer, settings and todo screens. */
    val durationPresets: List<Int> = DEFAULT_DURATION_PRESETS,
    /** true = Session Complete opens the photo picker automatically instead of waiting for a tap. */
    val askForPhotoAfterSession: Boolean = false,
    /** true = a local notification fires when a running session ends. */
    val alertWhenSessionEnds: Boolean = true,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val settings: Flow<FocusSettings> = context.dataStore.data.map { prefs ->
        FocusSettings(
            defaultDurationMs = prefs[KEY_DEFAULT_MS] ?: (25 * 60_000L),
            keepRunningWhileCommenting = prefs[KEY_KEEP_RUNNING] ?: true,
            photoAspect = PhotoAspect.from(prefs[KEY_PHOTO_ASPECT]),
            durationPresets = parsePresets(prefs[KEY_DURATION_PRESETS]),
            askForPhotoAfterSession = prefs[KEY_ASK_PHOTO] ?: false,
            alertWhenSessionEnds = prefs[KEY_ALERT_ON_END] ?: true,
        )
    }

    suspend fun setDefaultDuration(ms: Long) = edit { it[KEY_DEFAULT_MS] = ms }
    suspend fun setKeepRunningWhileCommenting(value: Boolean) = edit { it[KEY_KEEP_RUNNING] = value }
    suspend fun setPhotoAspect(value: PhotoAspect) = edit { it[KEY_PHOTO_ASPECT] = value.name }
    suspend fun setAskForPhotoAfterSession(value: Boolean) = edit { it[KEY_ASK_PHOTO] = value }
    suspend fun setAlertWhenSessionEnds(value: Boolean) = edit { it[KEY_ALERT_ON_END] = value }

    private fun parsePresets(raw: String?): List<Int> {
        val parsed = raw?.split(",")?.mapNotNull { it.toIntOrNull() }
        return if (parsed != null && parsed.size == DEFAULT_DURATION_PRESETS.size) parsed else DEFAULT_DURATION_PRESETS
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private companion object {
        val KEY_DEFAULT_MS = longPreferencesKey("default_duration_ms")
        val KEY_KEEP_RUNNING = booleanPreferencesKey("keep_running_commenting")
        val KEY_PHOTO_ASPECT = stringPreferencesKey("photo_aspect")
        val KEY_DURATION_PRESETS = stringPreferencesKey("duration_presets")
        val KEY_ASK_PHOTO = booleanPreferencesKey("ask_photo_after_session")
        val KEY_ALERT_ON_END = booleanPreferencesKey("alert_when_session_ends")
    }
}
