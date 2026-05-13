package com.example.agentmemory

import android.widget.Toast
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryScreen(
    contextManager: ContextManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val snapshots by contextManager.snapshotManager.snapshots.collectAsState()
    val isLoading by contextManager.snapshotManager.isLoading.collectAsState()
    val currentSnapshot by contextManager.snapshotManager.currentSnapshot.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDiffDialog by remember { mutableStateOf(false) }
    var selectedSnapshot by remember { mutableStateOf<GitSnapshotManager.Snapshot?>(null) }
    var snapshotDescription by remember { mutableStateOf("") }
    var snapshotTags by remember { mutableStateOf("") }
    
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("创建快照") },
            text = {
                Column {
                    OutlinedTextField(
                        value = snapshotDescription,
                        onValueChange = { snapshotDescription = it },
                        label = { Text("描述") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = snapshotTags,
                        onValueChange = { snapshotTags = it },
                        label = { Text("标签（用逗号分隔）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val tags = snapshotTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            contextManager.createSnapshot(
                                description = snapshotDescription.ifEmpty { "Manual snapshot" },
                                tags = tags
                            ).onSuccess {
                                Toast.makeText(context, "快照创建成功", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, "创建失败: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showCreateDialog = false
                        snapshotDescription = ""
                        snapshotTags = ""
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showRestoreDialog && selectedSnapshot != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            title = { Text("确认回退") },
            text = {
                Column {
                    Text("确定要回退到以下版本吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                selectedSnapshot!!.description,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Commit: ${selectedSnapshot!!.commitHash}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "消息数: ${selectedSnapshot!!.messageCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ 当前未保存的更改将会丢失",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            contextManager.restoreSnapshot(selectedSnapshot!!.id)
                                .onSuccess {
                                    Toast.makeText(context, "已成功回退到版本", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                                .onFailure {
                                    Toast.makeText(context, "回退失败: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        showRestoreDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认回退")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📜 版本历史") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val stats = contextManager.snapshotManager.getStatistics()
                            Toast.makeText(
                                context,
                                "快照: ${stats.totalSnapshots} | 消息: ${stats.totalMessages}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Icon(Icons.Default.Info, contentDescription = "统计")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("创建快照") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (snapshots.isEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "暂无版本快照",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "点击右下角按钮创建第一个快照",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "共 ${snapshots.size} 个快照",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    items(snapshots) { snapshot ->
                        SnapshotCard(
                            snapshot = snapshot,
                            isCurrent = snapshot.id == currentSnapshot?.id,
                            onRestore = {
                                selectedSnapshot = snapshot
                                showRestoreDialog = true
                            },
                            onViewDiff = {
                                scope.launch {
                                    contextManager.snapshotManager.getSnapshotDiff(
                                        snapshot.id,
                                        currentSnapshot?.id ?: snapshots.firstOrNull()?.id ?: ""
                                    ).onSuccess { diff ->
                                        showDiffDialog = true
                                    }.onFailure {
                                        Toast.makeText(context, "无法获取差异", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onExport = {
                                scope.launch {
                                    contextManager.snapshotManager.exportSnapshot(snapshot.id)
                                        .onSuccess { content ->
                                            val file = java.io.File(
                                                context.getExternalFilesDir(null),
                                                "snapshot_${snapshot.id}.md"
                                            )
                                            file.writeText(content)
                                            Toast.makeText(
                                                context,
                                                "已导出到: ${file.absolutePath}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SnapshotCard(
    snapshot: GitSnapshotManager.Snapshot,
    isCurrent: Boolean,
    onRestore: () -> Unit,
    onViewDiff: () -> Unit,
    onExport: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            snapshot.description,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(
                                onClick = {},
                                label = { Text("当前版本") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        "Commit: ${snapshot.commitHash}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        dateFormat.format(Date(snapshot.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("${snapshot.messageCount} 条消息") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                
                AssistChip(
                    onClick = {},
                    label = { Text("${snapshot.characterCount} 字符") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            if (snapshot.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    snapshot.tags.take(3).forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag) }
                        )
                    }
                    if (snapshot.tags.size > 3) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("+${snapshot.tags.size - 3}") }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onExport,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("导出")
                }
                
                TextButton(
                    onClick = onViewDiff,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.Difference,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("差异")
                }
                
                TextButton(
                    onClick = onRestore,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("回退")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitRollbackScreen(
    contextManager: ContextManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val snapshots by contextManager.snapshotManager.snapshots.collectAsState()
    val isLoading by contextManager.snapshotManager.isLoading.collectAsState()
    
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedSnapshot by remember { mutableStateOf<GitSnapshotManager.Snapshot?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⏪ Git 回退") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Git 回退说明",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "选择一个之前的版本快照进行回退。回退后，当前的对话历史将被该版本替换。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ 注意：此操作不可撤销，请确认后再操作。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "可用快照 (${snapshots.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (snapshots.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("暂无快照")
                        Text(
                            "请先在对话中创建快照",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(snapshots) { snapshot ->
                        val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedSnapshot = snapshot
                                showConfirmDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        snapshot.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        dateFormat.format(Date(snapshot.timestamp)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${snapshot.messageCount} 条消息 · ${snapshot.commitHash}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Icon(
                                    Icons.Default.Restore,
                                    contentDescription = "回退到此版本",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showConfirmDialog && selectedSnapshot != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("确认回退到版本") },
            text = {
                Column {
                    Text("即将回退到：")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                selectedSnapshot!!.description,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Commit: ${selectedSnapshot!!.commitHash}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(selectedSnapshot!!.timestamp))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "⚠️ 此操作将会：",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text("• 删除当前的对话历史")
                    Text("• 恢复到选定的版本")
                    Text("• 无法恢复被删除的内容")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            contextManager.restoreSnapshot(selectedSnapshot!!.id)
                                .onSuccess {
                                    Toast.makeText(context, "已成功回退到版本", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                                .onFailure {
                                    Toast.makeText(context, "回退失败: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认回退")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
