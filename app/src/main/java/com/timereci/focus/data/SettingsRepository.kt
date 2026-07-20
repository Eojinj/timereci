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

/** User-facing behavior settings from PRD §6 & §10, backed by Preferences DataStore. */
data class FocusSettings(
    /** Default timer duration in ms (settings screen default value). */
    val defaultDurationMs: Long = 25 * 60_000L,
    /** true = receipt length ∝ focus time (signature); false = uniform ratio. */
    val proportionalLength: Boolean = true,
    /** true = timer keeps running while the comment sheet is open (honest focus time). */
    val keepRunningWhileCommenting: Boolean = true,
    /** Crop ratio used when displaying photos in the feed and carousel. */
    val photoAspect: PhotoAspect = PhotoAspect.PORTRAIT,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val settings: Flow<FocusSettings> = context.dataStore.data.map { prefs ->
        FocusSettings(
            defaultDurationMs = prefs[KEY_DEFAULT_MS] ?: (25 * 60_000L),
            proportionalLength = prefs[KEY_PROPORTIONAL] ?: true,
            keepRunningWhileCommenting = prefs[KEY_KEEP_RUNNING] ?: true,
            photoAspect = PhotoAspect.from(prefs[KEY_PHOTO_ASPECT]),
        )
    }

    suspend fun setDefaultDuration(ms: Long) = edit { it[KEY_DEFAULT_MS] = ms }
    suspend fun setProportionalLength(value: Boolean) = edit { it[KEY_PROPORTIONAL] = value }
    suspend fun setKeepRunningWhileCommenting(value: Boolean) = edit { it[KEY_KEEP_RUNNING] = value }
    suspend fun setPhotoAspect(value: PhotoAspect) = edit { it[KEY_PHOTO_ASPECT] = value.name }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private companion object {
        val KEY_DEFAULT_MS = longPreferencesKey("default_duration_ms")
        val KEY_PROPORTIONAL = booleanPreferencesKey("proportional_length")
        val KEY_KEEP_RUNNING = booleanPreferencesKey("keep_running_commenting")
        val KEY_PHOTO_ASPECT = stringPreferencesKey("photo_aspect")
    }
}
