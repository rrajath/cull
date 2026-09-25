package com.rrajath.cull.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rrajath.cull.ui.component.SourceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val dataStore = context.dataStore

    object Keys {
        val SOURCE_MODE = stringPreferencesKey("source_mode")
        val LIBRARY_FOLDER_URI = stringPreferencesKey("library_folder_uri")
        val IMMICH_URL = stringPreferencesKey("immich_url")
        val IMMICH_API_KEY = stringPreferencesKey("immich_api_key")
        val LONG_PRESS_THRESHOLD = intPreferencesKey("long_press_threshold")
        val DRY_RUN = booleanPreferencesKey("dry_run")
        val MIRROR_DELETES = booleanPreferencesKey("mirror_deletes")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ACCENT_HUE = intPreferencesKey("accent_hue")
        val LAST_PHOTO_INDEX = intPreferencesKey("last_photo_index")
        val LAST_FOLDER_URI = stringPreferencesKey("last_folder_uri")
        val MARKED_IDS = stringPreferencesKey("marked_ids")
        val PINNED_ID = stringPreferencesKey("pinned_id")
        val GROUPING_WINDOW_MINUTES = intPreferencesKey("grouping_window_minutes")
        val DONE_STACK_KEYS = stringPreferencesKey("done_stack_keys")
    }

    val sourceMode: Flow<SourceMode> = dataStore.data.map { prefs ->
        val mode = prefs[Keys.SOURCE_MODE] ?: SourceMode.Hybrid.name
        // Tolerate unexpected persisted values (e.g. from an old import) instead
        // of crashing on every read via SourceMode.valueOf
        SourceMode.entries.find { it.name == mode } ?: SourceMode.Hybrid
    }

    val libraryFolderUri: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.LIBRARY_FOLDER_URI]
    }

    val immichUrl: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.IMMICH_URL]
    }

    val immichApiKey: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.IMMICH_API_KEY]
    }

    val longPressThreshold: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.LONG_PRESS_THRESHOLD] ?: 220
    }

    val dryRun: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DRY_RUN] ?: false
    }

    val mirrorDeletes: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.MIRROR_DELETES] ?: false
    }

    val darkTheme: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true
    }

    val accentHue: Flow<Int> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.ACCENT_HUE] ?: 0
        if (raw in 0..7) raw else 0
    }

    val lastPhotoIndex: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_PHOTO_INDEX] ?: 0
    }

    val lastFolderUri: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_FOLDER_URI]
    }

    val markedIds: Flow<Set<String>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.MARKED_IDS] ?: ""
        if (raw.isEmpty()) emptySet() else raw.split(",").toSet()
    }

    val pinnedId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.PINNED_ID]
    }

    val groupingWindowMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.GROUPING_WINDOW_MINUTES] ?: 2
    }

    // Fingerprints of stacks the user marked done (see core/grouping stackKey)
    val doneStackKeys: Flow<Set<String>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.DONE_STACK_KEYS] ?: ""
        if (raw.isEmpty()) emptySet() else raw.split(",").toSet()
    }

    suspend fun addDoneStackKey(key: String) {
        dataStore.edit { prefs ->
            val raw = prefs[Keys.DONE_STACK_KEYS] ?: ""
            val keys = if (raw.isEmpty()) emptySet() else raw.split(",").toSet()
            prefs[Keys.DONE_STACK_KEYS] = (keys + key).joinToString(",")
        }
    }

    suspend fun setDoneStackKeys(keys: Set<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.DONE_STACK_KEYS] = keys.joinToString(",")
        }
    }

    suspend fun setSourceMode(mode: SourceMode) {
        dataStore.edit { prefs ->
            prefs[Keys.SOURCE_MODE] = mode.name
        }
    }

    suspend fun setLibraryFolderUri(uri: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LIBRARY_FOLDER_URI] = uri
        }
    }

    suspend fun setImmichUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[Keys.IMMICH_URL] = url
        }
    }

    suspend fun setImmichApiKey(key: String) {
        dataStore.edit { prefs ->
            prefs[Keys.IMMICH_API_KEY] = key
        }
    }

    suspend fun setLongPressThreshold(ms: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.LONG_PRESS_THRESHOLD] = ms
        }
    }

    suspend fun setDryRun(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DRY_RUN] = enabled
        }
    }

    suspend fun setMirrorDeletes(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.MIRROR_DELETES] = enabled
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun setAccentHue(hue: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCENT_HUE] = hue
        }
    }

    suspend fun setLastPhotoIndex(index: Int, folderUri: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_PHOTO_INDEX] = index
            prefs[Keys.LAST_FOLDER_URI] = folderUri
        }
    }

    suspend fun setMarkedIds(ids: Set<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.MARKED_IDS] = ids.joinToString(",")
        }
    }

    suspend fun setPinnedId(id: String?) {
        dataStore.edit { prefs ->
            if (id != null) {
                prefs[Keys.PINNED_ID] = id
            } else {
                prefs.remove(Keys.PINNED_ID)
            }
        }
    }

    suspend fun setGroupingWindowMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GROUPING_WINDOW_MINUTES] = minutes
        }
    }

    suspend fun exportSettings(): SettingsExport {
        val prefs = dataStore.data.first()
        return SettingsExport(
            sourceMode = prefs[Keys.SOURCE_MODE] ?: SourceMode.Hybrid.name,
            immichUrl = prefs[Keys.IMMICH_URL],
            longPressThresholdMs = prefs[Keys.LONG_PRESS_THRESHOLD] ?: 220,
            dryRun = prefs[Keys.DRY_RUN] ?: false,
            mirrorDeletes = prefs[Keys.MIRROR_DELETES] ?: false,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            accentHue = prefs[Keys.ACCENT_HUE] ?: 0,
            groupingWindowMinutes = prefs[Keys.GROUPING_WINDOW_MINUTES] ?: 2,
        )
    }

    suspend fun importSettings(rawExport: SettingsExport) {
        val export = rawExport.sanitized()
        dataStore.edit { prefs ->
            prefs[Keys.SOURCE_MODE] = export.sourceMode
            if (export.immichUrl != null) {
                prefs[Keys.IMMICH_URL] = export.immichUrl
            }
            prefs[Keys.LONG_PRESS_THRESHOLD] = export.longPressThresholdMs
            prefs[Keys.DRY_RUN] = export.dryRun
            prefs[Keys.MIRROR_DELETES] = export.mirrorDeletes
            prefs[Keys.DARK_THEME] = export.darkTheme
            prefs[Keys.ACCENT_HUE] = export.accentHue
            prefs[Keys.GROUPING_WINDOW_MINUTES] = export.groupingWindowMinutes
        }
    }
}
