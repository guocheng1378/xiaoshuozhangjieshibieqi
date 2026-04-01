package com.novelreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.novelreader.data.repository.BookRepository
import com.novelreader.navigation.NavGraph
import com.novelreader.ui.theme.NovelReaderTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private lateinit var repository: BookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = BookRepository(applicationContext)

        setContent {
            var themeMode by remember { mutableStateOf("system") }
            var useGlass by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                repository.settingsFlow.collectLatest { settings ->
                    themeMode = settings.themeMode
                    useGlass = settings.useGlassTheme
                }
            }

            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            NovelReaderTheme(
                darkTheme = darkTheme,
                useGlass = useGlass
            ) {
                NavGraph(repository = repository)
            }
        }
    }
}
