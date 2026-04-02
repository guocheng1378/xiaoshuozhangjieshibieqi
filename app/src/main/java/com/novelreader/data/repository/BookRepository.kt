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
import java.security.MessageDigest

data class AppSettings(
    val themeMode: String = "system", // "system", "light", "dark"
    val useGlassTheme: Boolean = true,
    val fontSize: Float = 16f,
    val lineHeight: Float = 1.6f,
    val forcedEncoding: String? = null,
    val recentFiles: List<String> = emptyList(),
    val customChapterPatterns: List<String> = emptyList()
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
        private val CUSTOM_PATTERNS = stringPreferencesKey("custom_chapter_patterns")
        private val READING_PREFIX = "reading_chapter_"
        private val SCROLL_PREFIX = "scroll_offset_"
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
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
                recentFiles = prefs[RECENT_FILES]?.split("||")?.filter { it.isNotEmpty() } ?: emptyList(),
                customChapterPatterns = prefs[CUSTOM_PATTERNS]?.split("||")?.filter { it.isNotEmpty() } ?: emptyList()
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

    suspend fun updateCustomChapterPatterns(patterns: List<String>) {
        context.dataStore.edit { prefs ->
            if (patterns.isEmpty()) {
                prefs.remove(CUSTOM_PATTERNS)
            } else {
                prefs[CUSTOM_PATTERNS] = patterns.joinToString("||")
            }
        }
    }

    suspend fun saveReadingProgress(filePath: String, chapterIndex: Int, scrollOffset: Int) {
        val key = md5(filePath)
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(READING_PREFIX + key)] = chapterIndex.toString()
            prefs[stringPreferencesKey(SCROLL_PREFIX + key)] = scrollOffset.toString()
        }
    }

    fun getReadingProgress(filePath: String): Flow<Pair<Int, Int>> {
        val key = md5(filePath)
        return context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                val chapter = prefs[stringPreferencesKey(READING_PREFIX + key)]?.toIntOrNull() ?: 0
                val scroll = prefs[stringPreferencesKey(SCROLL_PREFIX + key)]?.toIntOrNull() ?: 0
                chapter to scroll
            }
    }

    suspend fun addRecentFile(filePath: String, fileName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_FILES]?.split("||")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
            val entry = "$filePath::$fileName"
            current.remove(entry)
            current.add(0, entry)
            prefs[RECENT_FILES] = current.joinToString("||")
        }
    }

    suspend fun addRecentFiles(files: List<Pair<String, String>>) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_FILES]?.split("||")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
            for ((filePath, fileName) in files) {
                val entry = "$filePath::$fileName"
                current.remove(entry)
                current.add(0, entry)
            }
            prefs[RECENT_FILES] = current.joinToString("||")
        }
    }

    suspend fun removeRecentFile(filePath: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_FILES]?.split("||")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
            current.removeAll { it.startsWith("$filePath::") }
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

    /**
     * 清理内部导入目录中已不在最近记录中的文件，释放磁盘空间。
     * 仅清理 books/ 和 zip_books/，不影响 content:// URI。
     * 应在获取到 recentFiles 后调用。
     */
    fun cleanOrphanFiles(activePaths: Set<String>) {
        try {
            val booksDir = java.io.File(context.filesDir, "books")
            val zipDir = java.io.File(context.filesDir, "zip_books")
            for (dir in listOf(booksDir, zipDir)) {
                if (dir.isDirectory) {
                    dir.walkTopDown().filter { it.isFile }.forEach { file ->
                        if (file.absolutePath !in activePaths) {
                            file.delete()
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
