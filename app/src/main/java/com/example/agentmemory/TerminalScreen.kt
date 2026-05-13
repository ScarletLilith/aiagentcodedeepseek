package com.example.agentmemory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val commandExecutor = remember { CommandExecutor(context) }
    val scope = rememberCoroutineScope()

    val commandState by commandExecutor.commandState.collectAsState()
    val commandOutput by commandExecutor.commandOutput.collectAsState()
    val commandHistory by commandExecutor.commandHistory.collectAsState()

    var commandInput by remember { mutableStateOf("") }
    var useLinuxSubsystem by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(commandOutput) {
        if (commandOutput.isNotEmpty()) {
            listState.animateScrollToItem(commandOutput.lines().size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("终端") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "历史记录")
                    }
                    IconButton(onClick = { commandExecutor.clearOutput() }) {
                        Icon(Icons.Default.Clear, contentDescription = "清屏")
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !useLinuxSubsystem,
                    onClick = { useLinuxSubsystem = false },
                    label = { Text("本地 Shell") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = useLinuxSubsystem,
                    onClick = { useLinuxSubsystem = true },
                    label = { Text("Linux 子系统") }
                )
                Spacer(modifier = Modifier.weight(1f))
                when (commandState) {
                    is CommandExecutor.CommandState.Running -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("执行中...")
                        }
                    }
                    else -> {}
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                color = Color(0xFF1E1E1E)
            ) {
                if (commandOutput.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "输入命令开始执行",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            items(commandOutput.lines()) { line ->
                                Text(
                                    text = line,
                                    color = Color(0xFFD4D4D4),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            when (val state = commandState) {
                is CommandExecutor.CommandState.Completed -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (state.exitCode == 0) {
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        } else {
                            Color(0xFFF44336).copy(alpha = 0.1f)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (state.exitCode == 0) "✓ 完成" else "✗ 退出码: ${state.exitCode}",
                                color = if (state.exitCode == 0) {
                                    Color(0xFF4CAF50)
                                } else {
                                    Color(0xFFF44336)
                                }
                            )
                            Text(
                                "耗时: ${commandExecutor.formatDuration(state.duration)}",
                                color = Color.Gray
                            )
                        }
                    }
                }
                is CommandExecutor.CommandState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF44336).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "错误: ${state.message}",
                            color = Color(0xFFF44336),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                else -> {}
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (useLinuxSubsystem) "linux$ " else "$ ",
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    TextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入命令...") },
                        singleLine = true,
                        enabled = !commandExecutor.isRunning()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (commandExecutor.isRunning()) {
                        Button(
                            onClick = { commandExecutor.stopCurrentCommand() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Text("停止")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (commandInput.isNotBlank()) {
                                    val cmd = commandInput
                                    commandInput = ""
                                    scope.launch {
                                        if (useLinuxSubsystem) {
                                            commandExecutor.executeInLinuxSubsystem(cmd)
                                        } else {
                                            commandExecutor.executeCommand(cmd)
                                        }
                                    }
                                }
                            },
                            enabled = commandInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "执行")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("执行")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickCommandButton("ls -la") {
                    scope.launch {
                        if (useLinuxSubsystem) {
                            commandExecutor.executeInLinuxSubsystem("ls -la")
                        } else {
                            commandExecutor.executeCommand("ls -la")
                        }
                    }
                }
                QuickCommandButton("pwd") {
                    scope.launch {
                        if (useLinuxSubsystem) {
                            commandExecutor.executeInLinuxSubsystem("pwd")
                        } else {
                            commandExecutor.executeCommand("pwd")
                        }
                    }
                }
                QuickCommandButton("whoami") {
                    scope.launch {
                        if (useLinuxSubsystem) {
                            commandExecutor.executeInLinuxSubsystem("whoami")
                        } else {
                            commandExecutor.executeCommand("whoami")
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("命令历史") },
            text = {
                if (commandHistory.isEmpty()) {
                    Text("暂无历史记录")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(commandHistory) { historyItem ->
                            HistoryItem(
                                historyItem = historyItem,
                                onClick = {
                                    commandInput = historyItem.command
                                    showHistoryDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("关闭")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        commandExecutor.clearHistory()
                        showHistoryDialog = false
                    }
                ) {
                    Text("清空")
                }
            }
        )
    }
}

@Composable
fun QuickCommandButton(
    command: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp)
    ) {
        Text(command, fontSize = 12.sp)
    }
}

@Composable
fun HistoryItem(
    historyItem: CommandExecutor.CommandHistory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = historyItem.command,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                Text(
                    text = sdf.format(Date(historyItem.timestamp)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            historyItem.exitCode?.let { exitCode ->
                Text(
                    text = if (exitCode == 0) "✓ 成功" else "✗ 失败 (退出码: $exitCode)",
                    fontSize = 12.sp,
                    color = if (exitCode == 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}
