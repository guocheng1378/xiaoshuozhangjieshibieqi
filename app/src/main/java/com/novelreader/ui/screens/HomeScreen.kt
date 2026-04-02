package com.novelreader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novelreader.data.model.BookFile
import com.novelreader.data.parser.ZipParser
import com.novelreader.data.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: BookRepository,
    onBookClick: (String, String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recentFiles by remember { mutableStateOf<List<BookFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var importing by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repository.getRecentFiles().collect { files ->
            recentFiles = files
            isLoading = false
        }
    }

    // Single file picker
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "未知文件"

        if (fileName.endsWith(".zip", ignoreCase = true)) {
            importing = true
            importProgress = "正在解压..."
            scope.launch {
                try {
                    importZipFile(context, uri, repository, onBookClick)
                } catch (e: Exception) {
                    e.printStackTrace()
                    importProgress = "解压出错，请重试"
                } finally {
                    importing = false
                    importProgress = ""
                }
            }
        } else {
            var localPath: String? = null
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                localPath = uri.toString()
            } catch (_: Exception) {
                try {
                    val localFile = File(context.filesDir, "books/$fileName")
                    localFile.parentFile?.mkdirs()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        localFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    localPath = localFile.absolutePath
                } catch (_: Exception) {}
            }
            localPath?.let { onBookClick(it, fileName) }
        }
    }

    // Folder picker
    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}
        importing = true
        importProgress = "正在扫描文件夹..."
        scope.launch {
            try {
                importFolder(context, uri, repository, onBookClick)
            } catch (e: Exception) {
                e.printStackTrace()
                importProgress = "导入出错，请重试"
            } finally {
                importing = false
                importProgress = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("小说章节识别器", style = MaterialTheme.typography.titleLarge)
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Import buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Open file button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            openDocumentLauncher.launch(arrayOf(
                                "text/plain", "text/markdown",
                                "application/epub+zip",
                                "application/zip", "application/octet-stream", "*/*"
                            ))
                        },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "打开文件",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "TXT / EPUB / ZIP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Import folder button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { openFolderLauncher.launch(null) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "导入文件夹",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "批量导入小说",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Import progress
            if (importing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            importProgress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                recentFiles.isNotEmpty() -> {
                    Text(
                        text = "最近阅读",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentFiles) { file ->
                            RecentFileItem(
                                file = file,
                                onClick = { onBookClick(file.filePath, file.fileName) }
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "暂无阅读记录",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                "点击上方按钮导入小说",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentFileItem(
    file: BookFile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isZip = file.filePath.contains("/zip_books/")
            Icon(
                if (isZip) Icons.Outlined.FolderZip else Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (file.filePath.startsWith("content://")) "外部文件"
                           else if (isZip) "来自压缩包"
                           else "本地文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Import a zip file: extract txt/epub, save to local, open first one
private suspend fun importZipFile(
    context: android.content.Context,
    uri: Uri,
    repository: BookRepository,
    onBookClick: (String, String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext
        val entries = ZipParser.scan(inputStream)
        inputStream.close()

        if (entries.isEmpty()) return@withContext

        val zipDir = File(context.filesDir, "zip_books")
        zipDir.mkdirs()

        var firstFile: Pair<String, String>? = null

        for (entry in entries) {
            val localFile = File(zipDir, entry.name)
            localFile.parentFile?.mkdirs()
            entry.inputStreamProvider().use { input ->
                localFile.outputStream().use { output -> input.copyTo(output) }
            }
            repository.addRecentFile(localFile.absolutePath, entry.name)
            if (firstFile == null) {
                firstFile = localFile.absolutePath to entry.name
            }
        }

        firstFile?.let { (path, name) ->
            withContext(Dispatchers.Main) { onBookClick(path, name) }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Import a folder: scan for txt/epub/zip, import all
private suspend fun importFolder(
    context: android.content.Context,
    folderUri: Uri,
    repository: BookRepository,
    onBookClick: (String, String) -> Unit
) = withContext(Dispatchers.IO) {
    var firstFile: Pair<String, String>? = null
    val zipDir = File(context.filesDir, "zip_books")
    val bookDir = File(context.filesDir, "books")
    zipDir.mkdirs()
    bookDir.mkdirs()

    suspend fun scanAndImport(dirUri: Uri, depth: Int = 0) {
        if (depth > 5) return
        val dir = try {
            androidx.documentfile.provider.DocumentFile.fromTreeUri(context, dirUri)
        } catch (_: Exception) { null } ?: return

        val children = try {
            dir.listFiles()
        } catch (_: Exception) { return }

        for (file in children) {
            try {
                if (file.isDirectory) {
                    scanAndImport(file.uri, depth + 1)
                } else if (file.canRead()) {
                    val name = file.name ?: continue
                    val lower = name.lowercase()
                    val mime = file.type ?: ""
                    val isZip = lower.endsWith(".zip") || mime == "application/zip" || mime == "application/x-zip-compressed"
                    val isTxt = lower.endsWith(".txt") || mime == "text/plain"
                    val isEpub = lower.endsWith(".epub") || mime == "application/epub+zip"
                    val isMd = lower.endsWith(".md") || mime == "text/markdown" || mime == "text/x-markdown"
                    if (isZip) {
                        context.contentResolver.openInputStream(file.uri)?.use { zipInput ->
                            val entries = ZipParser.scan(zipInput)
                            for (entry in entries) {
                                val safeName = name.substringBeforeLast(".") + "_" + entry.name
                                val localFile = File(zipDir, safeName.replace("/", "_"))
                                localFile.parentFile?.mkdirs()
                                entry.inputStreamProvider().use { input ->
                                    localFile.outputStream().use { output -> input.copyTo(output) }
                                }
                                repository.addRecentFile(localFile.absolutePath, entry.name)
                                if (firstFile == null) {
                                    firstFile = localFile.absolutePath to entry.name
                                }
                            }
                        }
                    } else if (isTxt || isEpub || isMd) {
                        val localFile = File(bookDir, name)
                        localFile.parentFile?.mkdirs()
                        context.contentResolver.openInputStream(file.uri)?.use { input ->
                            localFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        repository.addRecentFile(localFile.absolutePath, name)
                        if (firstFile == null) {
                            firstFile = localFile.absolutePath to name
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    withTimeoutOrNull(30_000) {
        scanAndImport(folderUri)
    }

    firstFile?.let { (path, name) ->
        withContext(Dispatchers.Main) { onBookClick(path, name) }
    }
}
