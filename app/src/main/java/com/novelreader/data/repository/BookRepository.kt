package com.novelreader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.novelreader.data.model.BookFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class AppSettings(
    val themeMode: String = "system", // "system", "light", "dark"
    val useGlassTheme: Boolean = true,
    val fontSize: Float = 16f,
    val lineHeight: Float = 1.6f,
    val forcedEncoding: String? = null,
    val recentFiles: List<String> = emptyList()
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "novel_reader_prefs")

class BookRepository(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val USE_GLASS = booleanPreferencesKey("use_glass_theme")
        private val FONT_SIZE = floatPreferencesKey("font_size")
        private val LINE_HEIGHT = floatPreferencesKey("line_height")
        private val FORCED_ENCODING = stringPreferencesKey("forced_encoding")
        private val RECENT_FILES = stringPreferencesKey("recent_files")
        private val READING_PREFIX = "reading_chapter_"
        private val SCROLL_PREFIX = "scroll_offset_"
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[THEME_MODE] ?: "system",
                useGlassTheme = prefs[USE_GLASS] ?: true,
                fontSize = prefs[FONT_SIZE] ?: 16f,
                lineHeight = prefs[LINE_HEIGHT] ?: 1.6f,
                forcedEncoding = prefs[FORCED_ENCODING],
                recentFiles = prefs[RECENT_FILES]?.split("||")?.filter { it.isNotEmpty() } ?: emptyList()
            )
        }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }

    suspend fun updateUseGlass(useGlass: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_GLASS] = useGlass
        }
    }

    suspend fun updateFontSize(size: Float) {
        context.dataStore.edit { prefs ->
            prefs[FONT_SIZE] = size
        }
    }

    suspend fun updateLineHeight(height: Float) {
        context.dataStore.edit { prefs ->
            prefs[LINE_HEIGHT] = height
        }
    }

    suspend fun updateForcedEncoding(encoding: String?) {
        context.dataStore.edit { prefs ->
            if (encoding != null) {
                prefs[FORCED_ENCODING] = encoding
            } else {
                prefs.remove(FORCED_ENCODING)
            }
        }
    }

    suspend fun saveReadingProgress(filePath: String, chapterIndex: Int, scrollOffset: Int) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(READING_PREFIX + filePath.hashCode())] = chapterIndex.toString()
            prefs[stringPreferencesKey(SCROLL_PREFIX + filePath.hashCode())] = scrollOffset.toString()
        }
    }

    fun getReadingProgress(filePath: String): Flow<Pair<Int, Int>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val chapter = prefs[stringPreferencesKey(READING_PREFIX + filePath.hashCode())]?.toIntOrNull() ?: 0
            val scroll = prefs[stringPreferencesKey(SCROLL_PREFIX + filePath.hashCode())]?.toIntOrNull() ?: 0
            chapter to scroll
        }

    suspend fun addRecentFile(filePath: String, fileName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_FILES]?.split("||")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
            val entry = "$filePath::$fileName"
            current.remove(entry)
            current.add(0, entry)
            if (current.size > 20) current.removeLast()
            prefs[RECENT_FILES] = current.joinToString("||")
        }
    }

    fun getRecentFiles(): Flow<List<BookFile>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[RECENT_FILES]?.split("||")
                ?.filter { it.isNotEmpty() }
                ?.mapNotNull { entry ->
                    val parts = entry.split("::")
                    if (parts.size == 2) {
                        BookFile(filePath = parts[0], fileName = parts[1])
                    } else null
                } ?: emptyList()
        }
}
