package com.novelreader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novelreader.util.StoragePermissionChecker
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class FileItem(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    initialPath: String? = null,
    browseMode: BrowseMode = BrowseMode.FILES_AND_FOLDERS,
    onFileSelected: (File) -> Unit,
    onFolderSelected: (File) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    StoragePermissionChecker {
        FileBrowserContent(
            initialPath = initialPath,
            browseMode = browseMode,
            onFileSelected = onFileSelected,
            onFolderSelected = onFolderSelected,
            onBack = onBack
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileBrowserContent(
    initialPath: String? = null,
    browseMode: BrowseMode = BrowseMode.FILES_AND_FOLDERS,
    onFileSelected: (File) -> Unit,
    onFolderSelected: (File) -> Unit,
    onBack: () -> Unit
) {
    // Get initial directory
    val startDir = remember {
        when {
            initialPath != null -> File(initialPath)
            else -> File("/storage/emulated/0") // Default to internal storage
        }
    }
    
    var currentDir by remember { mutableStateOf(startDir) }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load files when directory changes
    LaunchedEffect(currentDir) {
        isLoading = true
        files = loadDirectory(currentDir)
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "选择${if (browseMode == BrowseMode.FOLDERS_ONLY) "文件夹" else "文件"}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            currentDir.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentDir.parentFile != null) {
                            currentDir = currentDir.parentFile!!
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // Quick access to common directories
                    IconButton(onClick = { 
                        currentDir = File("/storage/emulated/0")
                    }) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = "内部存储")
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
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                files.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FolderOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "此文件夹为空",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(files) { fileItem ->
                            FileListItem(
                                item = fileItem,
                                browseMode = browseMode,
                                onClick = {
                                    if (fileItem.isDirectory) {
                                        currentDir = fileItem.file
                                    } else {
                                        onFileSelected(fileItem.file)
                                    }
                                },
                                onLongClick = {
                                    if (fileItem.isDirectory && browseMode != BrowseMode.FILES_ONLY) {
                                        onFolderSelected(fileItem.file)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListItem(
    item: FileItem,
    browseMode: BrowseMode,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDirectory) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File/Folder icon
            Icon(
                imageVector = when {
                    item.isDirectory -> Icons.Default.Folder
                    item.name.endsWith(".txt", ignoreCase = true) -> Icons.Default.Description
                    item.name.endsWith(".epub", ignoreCase = true) -> Icons.Default.AutoStories
                    item.name.endsWith(".zip", ignoreCase = true) -> Icons.Outlined.FolderZip
                    item.name.endsWith(".md", ignoreCase = true) -> Icons.Default.Article
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = if (item.isDirectory) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!item.isDirectory) {
                        Text(
                            text = formatFileSize(item.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(item.lastModified)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Arrow indicator for folders
            if (item.isDirectory) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun loadDirectory(dir: File): List<FileItem> {
    if (!dir.exists() || !dir.isDirectory) return emptyList()
    
    val files = dir.listFiles() ?: return emptyList()
    
    return files
        .filter { it.canRead() }
        .map { file ->
            FileItem(
                file = file,
                name = file.name,
                isDirectory = file.isDirectory,
                size = if (file.isFile) file.length() else 0L,
                lastModified = file.lastModified()
            )
        }
        .sortedWith(compareByDescending<FileItem> { it.isDirectory }
            .thenBy { it.name.lowercase() })
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

enum class BrowseMode {
    FILES_ONLY,
    FOLDERS_ONLY,
    FILES_AND_FOLDERS
}
