package com.example.agentmemory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val subsystemManager = remember { LinuxSubsystemManager(context) }
    val installationState by subsystemManager.installationState.collectAsState(initial = LinuxSubsystemManager.InstallationState.Idle)
    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf("home") }

    LaunchedEffect(Unit) {
        subsystemManager.setupSubsystemIfNeeded()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = {
                        currentRoute = "home"
                        navController.navigate("home")
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = currentRoute == "files",
                    onClick = {
                        currentRoute = "files"
                        navController.navigate("files")
                    },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "文件") },
                    label = { Text("文件") }
                )
                NavigationBarItem(
                    selected = currentRoute == "terminal",
                    onClick = {
                        currentRoute = "terminal"
                        navController.navigate("terminal")
                    },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "终端") },
                    label = { Text("终端") }
                )
                NavigationBarItem(
                    selected = currentRoute == "editor",
                    onClick = {
                        currentRoute = "editor"
                        navController.navigate("editor")
                    },
                    icon = { Icon(Icons.Default.Code, contentDescription = "编辑器") },
                    label = { Text("编辑器") }
                )
                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = {
                        currentRoute = "settings"
                        navController.navigate("settings")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                    label = { Text("设置") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomeScreen(
                        installationState = installationState,
                        onRetry = {
                            scope.launch {
                                subsystemManager.setupSubsystemIfNeeded()
                            }
                        },
                        onNavigateToFiles = {
                            currentRoute = "files"
                            navController.navigate("files")
                        },
                        onNavigateToTerminal = {
                            currentRoute = "terminal"
                            navController.navigate("terminal")
                        },
                        onNavigateToEditor = {
                            currentRoute = "editor"
                            navController.navigate("editor")
                        },
                        onNavigateToHistory = {
                            currentRoute = "history"
                            navController.navigate("history")
                        },
                        onNavigateToRules = {
                            currentRoute = "rules"
                            navController.navigate("rules")
                        },
                        onNavigateToMemPalace = {
                            currentRoute = "mempalace"
                            navController.navigate("mempalace")
                        }
                    )
                }
                composable("files") {
                    FileBrowserScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenFile = { path ->
                            // 这里可以处理打开文件的逻辑
                        }
                    )
                }
                composable("terminal") {
                    TerminalScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("verify") {
                    VerifyScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("editor") {
                    EditorScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("rules") {
                    RulesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("history") {
                    VersionHistoryScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("mempalace") {
                    MemPalaceScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    installationState: LinuxSubsystemManager.InstallationState,
    onRetry: () -> Unit,
    onNavigateToFiles: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToMemPalace: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "AgentMemory",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "AI Memory System for Android",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        when (installationState) {
            is LinuxSubsystemManager.InstallationState.Idle -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("检查系统...")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            is LinuxSubsystemManager.InstallationState.Extracting -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("解压中...")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(installationState.currentFile, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            is LinuxSubsystemManager.InstallationState.Installing -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("安装中...")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(installationState.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            is LinuxSubsystemManager.InstallationState.Complete -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "系统已就绪！",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Linux 子系统和 MemPalace 正在运行。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            is LinuxSubsystemManager.InstallationState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "错误",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            installationState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text("重试")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "快速访问",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionCard(
                icon = Icons.Default.Folder,
                title = "文件浏览器",
                description = "管理文件和目录",
                onClick = onNavigateToFiles
            )
            QuickActionCard(
                icon = Icons.Default.Terminal,
                title = "终端",
                description = "执行命令和脚本",
                onClick = onNavigateToTerminal
            )
            QuickActionCard(
                icon = Icons.Default.Code,
                title = "代码编辑器",
                description = "编辑和查看代码",
                onClick = onNavigateToEditor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.History,
                    title = "历史记录",
                    description = "版本控制",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToHistory
                )
                QuickActionCard(
                    icon = Icons.Default.Rule,
                    title = "规则",
                    description = "AI 行为规则",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToRules
                )
            }
            QuickActionCard(
                icon = Icons.Default.Memory,
                title = "MemPalace",
                description = "AI 记忆宫殿管理",
                onClick = onNavigateToMemPalace
            )
        }
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
