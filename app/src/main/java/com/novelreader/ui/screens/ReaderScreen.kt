package com.novelreader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novelreader.data.model.Chapter
import com.novelreader.data.parser.EpubParser
import com.novelreader.data.parser.TxtParser
import com.novelreader.data.repository.BookRepository
import com.novelreader.ui.components.CopyToast
import com.novelreader.ui.components.FloatingToolbar
import com.novelreader.util.ClipboardHelper
import com.novelreader.util.EncodingDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    filePath: String,
    fileName: String,
    repository: BookRepository,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var toolbarVisible by remember { mutableStateOf(true) }
    var toastMessage by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(16f) }
    var lineHeight by remember { mutableStateOf(1.6f) }
    var forcedEncoding by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // SAF file picker for re-opening files with lost permissions
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            // Re-parse the file
            isLoading = true
            errorMessage = null
            scope.launch {
                loadFile(context, it, fileName, forcedEncoding)?.let { result ->
                    chapters = result
                    repository.addRecentFile(it.toString(), fileName)
                } ?: run {
                    errorMessage = "无法读取文件，请重新选择"
                }
                isLoading = false
            }
        }
    }

    // Load settings
    LaunchedEffect(Unit) {
        repository.settingsFlow.collectLatest { settings ->
            fontSize = settings.fontSize
            lineHeight = settings.lineHeight
            forcedEncoding = settings.forcedEncoding
        }
    }

    // Load file
    LaunchedEffect(filePath) {
        isLoading = true
        errorMessage = null
        try {
            val uri = Uri.parse(filePath)
            val result = loadFile(context, uri, fileName, forcedEncoding)
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

    // Save reading progress with debounce (2s)
    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(2000L)
            .collect { (index, offset) ->
                if (chapters.isNotEmpty()) {
                    repository.saveReadingProgress(filePath, index, offset)
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        fileName,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    // Error state
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
                                "text/plain",
                                "application/epub+zip",
                                "application/octet-stream",
                                "*/*"
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
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 80.dp
                        )
                    ) {
                        items(chapters) { chapter ->
                            // Chapter content display
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = (fontSize + 4).sp,
                                        lineHeight = ((fontSize + 4) * lineHeight).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = chapter.content,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * lineHeight).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Copy actions row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        ClipboardHelper.copyTitle(context, chapter.title)
                                        toastMessage = "标题已复制"
                                        toastVisible = true
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Title, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("复制标题")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        ClipboardHelper.copyContent(context, chapter.content, chapter.wordCount)
                                        toastMessage = "内容已复制（约${chapter.wordCount}字）"
                                        toastVisible = true
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("复制全文 (${chapter.wordCount}字)")
                                }
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // Floating toolbar (only show when content loaded)
            if (chapters.isNotEmpty()) {
                FloatingToolbar(
                    visible = toolbarVisible,
                    onFileClick = {
                        openDocumentLauncher.launch(arrayOf(
                            "text/plain",
                            "application/epub+zip",
                            "application/octet-stream",
                            "*/*"
                        ))
                    },
                    onCopyClick = {
                        val visibleChapter = chapters.getOrNull(listState.firstVisibleItemIndex)
                        if (visibleChapter != null) {
                            ClipboardHelper.copyContent(context, visibleChapter.content, visibleChapter.wordCount)
                            toastMessage = "内容已复制（约${visibleChapter.wordCount}字）"
                            toastVisible = true
                        }
                    },
                    onBookmarkClick = {
                        scope.launch {
                            repository.saveReadingProgress(
                                filePath,
                                listState.firstVisibleItemIndex,
                                listState.firstVisibleItemScrollOffset
                            )
                            toastMessage = "书签已保存"
                            toastVisible = true
                        }
                    },
                    onThemeClick = { toolbarVisible = !toolbarVisible },
                    onSettingsClick = onSettingsClick
                )
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
    uri: Uri,
    fileName: String,
    forcedEncoding: String?
): List<Chapter>? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
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
