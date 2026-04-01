package com.novelreader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
import java.io.File
import java.io.FileInputStream

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
    var toastMessage by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(16f) }
    var lineHeight by remember { mutableStateOf(1.6f) }
    var forcedEncoding by remember { mutableStateOf<String?>(null) }

    // Chapter reading state: null = chapter list, index = reading that chapter
    var selectedChapter by remember { mutableStateOf<Int?>(null) }

    val listState = rememberLazyListState()

    // SAF file picker for re-opening files
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isLoading = true
        errorMessage = null
        selectedChapter = null
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
            fontSize = settings.fontSize
            lineHeight = settings.lineHeight
            forcedEncoding = settings.forcedEncoding
        }
    }

    // Load file
    LaunchedEffect(filePath) {
        isLoading = true
        errorMessage = null
        selectedChapter = null
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

    // Save reading progress with debounce (2s) — only in reading mode
    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(2000L)
            .collect { (index, offset) ->
                if (chapters.isNotEmpty() && selectedChapter != null) {
                    repository.saveReadingProgress(filePath, selectedChapter!!, index)
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedChapter != null && chapters.isNotEmpty()) {
                        Text(
                            chapters[selectedChapter!!].title,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text(
                            fileName,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedChapter != null) {
                            selectedChapter = null // Back to chapter list
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (selectedChapter != null) {
                        IconButton(onClick = {
                            val chapter = chapters[selectedChapter!!]
                            ClipboardHelper.copyContent(context, chapter.content, chapter.wordCount)
                            toastMessage = "内容已复制（约${chapter.wordCount}字）"
                            toastVisible = true
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制全文")
                        }
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
                // Reading mode: show chapter content
                selectedChapter != null -> {
                    val chapter = chapters[selectedChapter!!]
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        item {
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
                        }
                        // Navigation buttons
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (selectedChapter!! > 0) {
                                    OutlinedButton(onClick = {
                                        selectedChapter = selectedChapter!! - 1
                                    }) {
                                        Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(18.dp))
                                        Text("上一章")
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }
                                if (selectedChapter!! < chapters.size - 1) {
                                    OutlinedButton(onClick = {
                                        selectedChapter = selectedChapter!! + 1
                                    }) {
                                        Text("下一章")
                                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                // Chapter list mode
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
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            itemsIndexed(chapters) { index, chapter ->
                                ChapterListItem(
                                    index = index + 1,
                                    chapter = chapter,
                                    onClick = { selectedChapter = index },
                                    onCopyTitle = {
                                        ClipboardHelper.copyTitle(context, chapter.title)
                                        toastMessage = "标题已复制"
                                        toastVisible = true
                                    },
                                    onCopyContent = {
                                        ClipboardHelper.copyContent(context, chapter.content, chapter.wordCount)
                                        toastMessage = "内容已复制（约${chapter.wordCount}字）"
                                        toastVisible = true
                                    }
                                )
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

@Composable
private fun ChapterListItem(
    index: Int,
    chapter: Chapter,
    onClick: () -> Unit,
    onCopyTitle: () -> Unit,
    onCopyContent: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
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
                        "$index",
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
            // Copy buttons
            IconButton(onClick = onCopyTitle, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Title,
                    contentDescription = "复制标题",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCopyContent, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "复制全文",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
