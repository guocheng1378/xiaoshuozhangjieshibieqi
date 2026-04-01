package com.novelreader.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novelreader.data.model.Chapter
import com.novelreader.data.parser.EpubParser
import com.novelreader.data.parser.TxtParser
import com.novelreader.data.repository.BookRepository
import com.novelreader.ui.components.ChapterItem
import com.novelreader.ui.components.CopyToast
import com.novelreader.ui.components.FloatingToolbar
import com.novelreader.util.ClipboardHelper
import com.novelreader.util.EncodingDetector
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    var toolbarVisible by remember { mutableStateOf(true) }
    var toastMessage by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(16f) }
    var lineHeight by remember { mutableStateOf(1.6f) }
    var forcedEncoding by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

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
        try {
            val uri = Uri.parse(filePath)
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                chapters = if (fileName.endsWith(".epub", ignoreCase = true)) {
                    EpubParser.parse(inputStream)
                } else {
                    val (text, charset) = EncodingDetector.detectAndRead(inputStream, forcedEncoding)
                    TxtParser.parse(text)
                }

                // Save to recent files
                repository.addRecentFile(filePath, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Save reading progress periodically
    LaunchedEffect(listState.firstVisibleItemIndex) {
        repository.saveReadingProgress(
            filePath = filePath,
            chapterIndex = listState.firstVisibleItemIndex,
            scrollOffset = listState.firstVisibleItemScrollOffset
        )
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
            if (chapters.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .wrapContentSize()
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chapters) { chapter ->
                        ChapterItem(
                            chapter = chapter,
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

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // Floating toolbar
            FloatingToolbar(
                visible = toolbarVisible,
                onFileClick = { /* Open file - navigate or show dialog */ },
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

            // Copy toast
            CopyToast(
                message = toastMessage,
                visible = toastVisible,
                onDismiss = { toastVisible = false }
            )
        }
    }
}
