package com.novelreader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.novelreader.NovelReaderApp
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
    val uriHandler = LocalUriHandler.current
    var themeMode by remember { mutableStateOf("system") }
    var forcedEncoding by remember { mutableStateOf<String?>(null) }
    var customPatterns by remember { mutableStateOf<List<String>>(emptyList()) }
    var newPattern by remember { mutableStateOf("") }
    var showPatternHelp by remember { mutableStateOf(false) }
    var patternError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.settingsFlow.collectLatest { settings ->
            themeMode = settings.themeMode
            forcedEncoding = settings.forcedEncoding
            customPatterns = settings.customChapterPatterns
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

            // Custom chapter patterns
            SettingsSection(title = "自定义章节规则") {
                Text(
                    "添加正则表达式来自定义章节识别",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pattern input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPattern,
                        onValueChange = {
                            newPattern = it
                            patternError = null
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("""^卷\d+.*""", fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        isError = patternError != null,
                        supportingText = patternError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                    )
                    IconButton(
                        onClick = {
                            if (newPattern.isBlank()) return@IconButton
                            try {
                                Regex(newPattern)
                                val updated = customPatterns + newPattern.trim()
                                customPatterns = updated
                                scope.launch { repository.updateCustomChapterPatterns(updated) }
                                newPattern = ""
                                patternError = null
                            } catch (e: Exception) {
                                patternError = "正则表达式无效"
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }

                // Help toggle
                TextButton(
                    onClick = { showPatternHelp = !showPatternHelp },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("查看示例", style = MaterialTheme.typography.labelMedium)
                }

                if (showPatternHelp) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val examples = listOf(
                            """^卷\d+.*""" to "卷01 开端",
                            """^#+\s+序章.*""" to "# 序章",
                            """^\d+[.、].*$""" to "1. 第一节",
                            """^【.+章.+】$""" to "【第一章 觉醒】",
                            """^—{3,}.*$""" to "———— 第一幕 ————",
                        )
                        for ((pattern, example) in examples) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    pattern,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "→ $example",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Existing patterns list
                if (customPatterns.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("已添加的规则:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    for ((index, pattern) in customPatterns.withIndex()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                pattern,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val updated = customPatterns.toMutableList().apply { removeAt(index) }
                                    customPatterns = updated
                                    scope.launch { repository.updateCustomChapterPatterns(updated) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "删除",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // About section
            SettingsSection(title = "关于") {
                Text(
                    "小说章节识别器 v${NovelReaderApp.versionName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "支持 TXT / EPUB / ZIP 格式，自动识别章节",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            uriHandler.openUri("https://qun.qq.com/universal-share/share?ac=1&authKey=U6H3i4hoAJq4wS%2FPON2W1VZqllWlvqwvR2JIIoluuYy1vb0UN6QryKTG5VZjXJAA&busi_data=eyJncm91cENvZGUiOiI5NjM0MTA1NSIsInRva2VuIjoickNqcnh3QXU3WCtycFlSSHZiNDNjYjFkU0tTdElydytvL0xzbUhRC82SU4xYnBxMDBjRkxJV3dPL1M5dE9lTCIsInVpbiI6Ijk2MzQ4NDM1MCJ9&data=SoU871UPiE7vVgnr2mFzYwmv-JGCKCvgDkKEiN2VGQfZuYUDi7_hh41SO6o3ljDbhap277SHrHkXWTSUUmDcJg&svctype=4&tempid=h5_group_info")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("加入QQ群")
                    }
                    OutlinedButton(
                        onClick = {
                            uriHandler.openUri("https://github.com/guocheng1378/xiaoshuozhangjieshibieqi")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("关于更新")
                    }
                }
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
