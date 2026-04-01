package com.novelreader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novelreader.data.model.Chapter
import com.novelreader.data.parser.EpubParser
import com.novelreader.data.parser.TxtParser
import com.novelreader.data.repository.BookRepository
import com.novelreader.ui.components.CopyToast
import com.novelreader.util.ClipboardHelper
import com.novelreader.util.EncodingDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    filePath: String,
    fileName: String,
    repository: BookRepository,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var forcedEncoding by remember { mutableStateOf<String?>(null) }

    // SAF file picker for re-opening files
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isLoading = true
        errorMessage = null
        scope.launch {
            var localPath = uri.toString()
            val pickedName = uri.lastPathSegment?.substringAfterLast('/') ?: fileName
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                try {
                    val localFile = File(context.filesDir, "books/$pickedName")
                    localFile.parentFile?.mkdirs()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        localFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    localPath = localFile.absolutePath
                } catch (_: Exception) {}
            }
            loadFile(context, localPath, pickedName, forcedEncoding)?.let { result ->
                chapters = result
                repository.addRecentFile(localPath, pickedName)
            } ?: run {
                errorMessage = "无法读取文件，请重新选择"
            }
            isLoading = false
        }
    }

    // Load settings
    LaunchedEffect(Unit) {
        repository.settingsFlow.collectLatest { settings ->
            forcedEncoding = settings.forcedEncoding
        }
    }

    // Load file
    LaunchedEffect(filePath) {
        isLoading = true
        errorMessage = null
        try {
            val result = loadFile(context, filePath, fileName, forcedEncoding)
            if (result != null && result.isNotEmpty()) {
                chapters = result
                repository.addRecentFile(filePath, fileName)
            } else {
                errorMessage = if (filePath.startsWith("content://")) {
                    "文件权限已过期，请重新选择文件"
                } else {
                    "无法解析文件内容"
                }
            }
        } catch (e: Exception) {
            errorMessage = "读取失败: ${e.message ?: "未知错误"}"
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(fileName, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            openDocumentLauncher.launch(arrayOf(
                                "text/plain", "application/epub+zip",
                                "application/octet-stream", "*/*"
                            ))
                        }) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("重新选择文件")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = onBack) {
                            Text("返回首页")
                        }
                    }
                }
                chapters.isNotEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Summary bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "共 ${chapters.size} 章",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "约 ${chapters.sumOf { it.wordCount }} 字",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        // Chapter list
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(chapters) { index, chapter ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                ClipboardHelper.copyTitle(context, chapter.title)
                                                toastMessage = "标题已复制"
                                                toastVisible = true
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                ClipboardHelper.copyContent(context, chapter.content, chapter.wordCount)
                                                toastMessage = "内容已复制（约${chapter.wordCount}字）"
                                                toastVisible = true
                                            }
                                        ),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Chapter number
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "${index + 1}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        // Title + word count
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = chapter.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${chapter.wordCount} 字",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Copy toast
            CopyToast(
                message = toastMessage,
                visible = toastVisible,
                onDismiss = { toastVisible = false }
            )
        }
    }
}

private suspend fun loadFile(
    context: android.content.Context,
    filePath: String,
    fileName: String,
    forcedEncoding: String?
): List<Chapter>? = withContext(Dispatchers.IO) {
    try {
        val isLocalFile = !filePath.startsWith("content://")
        val inputStream = if (isLocalFile) {
            FileInputStream(File(filePath))
        } else {
            context.contentResolver.openInputStream(Uri.parse(filePath)) ?: return@withContext null
        }
        val result = if (fileName.endsWith(".epub", ignoreCase = true)) {
            EpubParser.parse(inputStream)
        } else {
            val (text, _) = EncodingDetector.detectAndRead(inputStream, forcedEncoding)
            TxtParser.parse(text)
        }
        inputStream.close()
        result.ifEmpty { null }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
