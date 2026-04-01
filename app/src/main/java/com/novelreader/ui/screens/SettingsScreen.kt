package com.novelreader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novelreader.data.repository.BookRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: BookRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var themeMode by remember { mutableStateOf("system") }
    var useGlass by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(16f) }
    var lineHeight by remember { mutableStateOf(1.6f) }
    var forcedEncoding by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.settingsFlow.collectLatest { settings ->
            themeMode = settings.themeMode
            useGlass = settings.useGlassTheme
            fontSize = settings.fontSize
            lineHeight = settings.lineHeight
            forcedEncoding = settings.forcedEncoding
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme section
            SettingsSection(title = "主题") {
                // Theme mode
                Text("外观模式", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = themeMode == "system",
                        onClick = {
                            themeMode = "system"
                            scope.launch { repository.updateThemeMode("system") }
                        },
                        label = { Text("跟随系统") }
                    )
                    FilterChip(
                        selected = themeMode == "light",
                        onClick = {
                            themeMode = "light"
                            scope.launch { repository.updateThemeMode("light") }
                        },
                        label = { Text("浅色") }
                    )
                    FilterChip(
                        selected = themeMode == "dark",
                        onClick = {
                            themeMode = "dark"
                            scope.launch { repository.updateThemeMode("dark") }
                        },
                        label = { Text("深色") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Glass / Material theme toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("玻璃主题", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "iOS 风格毛玻璃效果",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useGlass,
                        onCheckedChange = {
                            useGlass = it
                            scope.launch { repository.updateUseGlass(it) }
                        }
                    )
                }
            }

            // Reading section
            SettingsSection(title = "阅读") {
                // Font size
                Text("字号: ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 12f..28f,
                    steps = 15,
                    onValueChangeFinished = {
                        scope.launch { repository.updateFontSize(fontSize) }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Line height
                Text("行距: ${String.format("%.1f", lineHeight)}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = lineHeight,
                    onValueChange = { lineHeight = it },
                    valueRange = 1.2f..2.5f,
                    steps = 12,
                    onValueChangeFinished = {
                        scope.launch { repository.updateLineHeight(lineHeight) }
                    }
                )
            }

            // Encoding section
            SettingsSection(title = "编码") {
                Text("强制编码", style = MaterialTheme.typography.labelLarge)
                Text(
                    "默认自动检测，可手动覆盖",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = forcedEncoding == null,
                        onClick = {
                            forcedEncoding = null
                            scope.launch { repository.updateForcedEncoding(null) }
                        },
                        label = { Text("自动") }
                    )
                    FilterChip(
                        selected = forcedEncoding == "UTF-8",
                        onClick = {
                            forcedEncoding = "UTF-8"
                            scope.launch { repository.updateForcedEncoding("UTF-8") }
                        },
                        label = { Text("UTF-8") }
                    )
                    FilterChip(
                        selected = forcedEncoding == "GBK",
                        onClick = {
                            forcedEncoding = "GBK"
                            scope.launch { repository.updateForcedEncoding("GBK") }
                        },
                        label = { Text("GBK") }
                    )
                }
            }

            // About section
            SettingsSection(title = "关于") {
                Text(
                    "小说阅读器 v1.0",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "支持 TXT / EPUB 格式",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}
