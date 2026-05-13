package com.example.agentmemory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onNavigateBack: () -> Unit,
    onOpenFile: (String) -> Unit
) {
    val context = LocalContext.current
    val fileManager = remember { FileOperationManager(context) }
    val scope = rememberCoroutineScope()

    val currentPath by fileManager.currentPath.collectAsState()
    val fileList by fileManager.fileList.collectAsState()

    var showDialog by remember { mutableStateOf<DialogType?>(null) }
    var selectedFile by remember { mutableStateOf<FileOperationManager.FileItem?>(null) }
    var dialogInput by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        fileManager.listFiles(fileManager.currentPath.value)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件浏览器") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showDialog = DialogType.NewFile
                    }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "新建文件")
                    }
                    IconButton(onClick = {
                        showDialog = DialogType.NewFolder
                    }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "新建文件夹")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            fileManager.listFiles(currentPath)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            fileManager.getParentPath(currentPath)?.let { parentPath ->
                                scope.launch {
                                    fileManager.listFiles(parentPath)
                                }
                            }
                        },
                        enabled = fileManager.getParentPath(currentPath) != null
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "上级目录")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentPath,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }

            if (fileList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("目录为空")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(fileList) { item ->
                        FileListItem(
                            fileItem = item,
                            fileManager = fileManager,
                            onClick = {
                                when (item) {
                                    is FileOperationManager.FileItem.Directory -> {
                                        scope.launch {
                                            fileManager.listFiles(item.path)
                                        }
                                    }
                                    is FileOperationManager.FileItem.FileInfo -> {
                                        if (fileManager.isTextFile(item.path)) {
                                            scope.launch {
                                                when (val result = fileManager.readFile(item.path)) {
                                                    is FileOperationManager.OperationResult.Success -> {
                                                        fileContent = result.data as? String ?: ""
                                                        selectedFile = item
                                                        showDialog = DialogType.EditFile
                                                    }
                                                    is FileOperationManager.OperationResult.Error -> {
                                                        toastMessage = result.message
                                                    }
                                                }
                                            }
                                        } else {
                                            onOpenFile(item.path)
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                selectedFile = item
                                showDialog = DialogType.FileOptions
                            }
                        )
                    }
                }
            }
        }
    }

    DialogHandler(
        dialogType = showDialog,
        selectedFile = selectedFile,
        dialogInput = dialogInput,
        fileContent = fileContent,
        fileManager = fileManager,
        onDismiss = { showDialog = null },
        onInputChange = { dialogInput = it },
        onContentChange = { fileContent = it },
        onShowToast = { toastMessage = it }
    )

    toastMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            toastMessage = null
        }
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(message)
        }
    }
}

@Composable
fun FileListItem(
    fileItem: FileOperationManager.FileItem,
    fileManager: FileOperationManager,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fileManager.getFileIcon(fileItem),
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (fileItem) {
                        is FileOperationManager.FileItem.Directory -> fileItem.name
                        is FileOperationManager.FileItem.FileInfo -> fileItem.name
                    },
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (fileItem) {
                        is FileOperationManager.FileItem.Directory -> "${fileItem.childCount} 项"
                        is FileOperationManager.FileItem.FileInfo -> {
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            "${fileManager.formatFileSize(fileItem.size)} • ${sdf.format(Date(fileItem.lastModified))}"
                        }
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DialogHandler(
    dialogType: DialogType?,
    selectedFile: FileOperationManager.FileItem?,
    dialogInput: String,
    fileContent: String,
    fileManager: FileOperationManager,
    onDismiss: () -> Unit,
    onInputChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onShowToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    when (dialogType) {
        DialogType.NewFile -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("新建文件") },
                text = {
                    TextField(
                        value = dialogInput,
                        onValueChange = onInputChange,
                        label = { Text("文件名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (dialogInput.isNotBlank()) {
                            val path = "${fileManager.currentPath.value}/$dialogInput"
                            scope.launch {
                                when (val result = fileManager.writeFile(path, "")) {
                                    is FileOperationManager.OperationResult.Success -> {
                                        onShowToast("文件创建成功")
                                        onDismiss()
                                    }
                                    is FileOperationManager.OperationResult.Error -> {
                                        onShowToast(result.message)
                                    }
                                }
                            }
                        }
                    }) {
                        Text("创建")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }

        DialogType.NewFolder -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("新建文件夹") },
                text = {
                    TextField(
                        value = dialogInput,
                        onValueChange = onInputChange,
                        label = { Text("文件夹名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (dialogInput.isNotBlank()) {
                            val path = "${fileManager.currentPath.value}/$dialogInput"
                            scope.launch {
                                when (val result = fileManager.createDirectory(path)) {
                                    is FileOperationManager.OperationResult.Success -> {
                                        onShowToast("文件夹创建成功")
                                        onDismiss()
                                    }
                                    is FileOperationManager.OperationResult.Error -> {
                                        onShowToast(result.message)
                                    }
                                }
                            }
                        }
                    }) {
                        Text("创建")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }

        DialogType.FileOptions -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("文件操作") },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                selectedFile?.let { file ->
                                    val path = when (file) {
                                        is FileOperationManager.FileItem.Directory -> file.path
                                        is FileOperationManager.FileItem.FileInfo -> file.path
                                    }
                                    onInputChange(path)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("复制路径")
                        }
                        TextButton(
                            onClick = {
                                selectedFile?.let { file ->
                                    val name = when (file) {
                                        is FileOperationManager.FileItem.Directory -> file.name
                                        is FileOperationManager.FileItem.FileInfo -> file.name
                                    }
                                    onInputChange(name)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("重命名")
                        }
                        TextButton(
                            onClick = {
                                selectedFile?.let { file ->
                                    val path = when (file) {
                                        is FileOperationManager.FileItem.Directory -> file.path
                                        is FileOperationManager.FileItem.FileInfo -> file.path
                                    }
                                    scope.launch {
                                        when (val result = fileManager.delete(path)) {
                                            is FileOperationManager.OperationResult.Success -> {
                                                onShowToast("删除成功")
                                                onDismiss()
                                            }
                                            is FileOperationManager.OperationResult.Error -> {
                                                onShowToast(result.message)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            )
        }

        DialogType.EditFile -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        selectedFile?.let { file ->
                            when (file) {
                                is FileOperationManager.FileItem.FileInfo -> file.name
                                else -> "编辑文件"
                            }
                        } ?: "编辑文件"
                    )
                },
                text = {
                    TextField(
                        value = fileContent,
                        onValueChange = onContentChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        maxLines = 20
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedFile?.let { file ->
                            when (file) {
                                is FileOperationManager.FileItem.FileInfo -> {
                                    scope.launch {
                                        when (val result = fileManager.writeFile(file.path, fileContent)) {
                                            is FileOperationManager.OperationResult.Success -> {
                                                onShowToast("保存成功")
                                                onDismiss()
                                            }
                                            is FileOperationManager.OperationResult.Error -> {
                                                onShowToast(result.message)
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }

        null -> {}
    }
}

enum class DialogType {
    NewFile,
    NewFolder,
    FileOptions,
    EditFile
}
