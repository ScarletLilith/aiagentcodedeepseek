package com.example.agentmemory

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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemPalaceScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val memPalaceManager = remember { MemPalaceManager(context) }
    val state by memPalaceManager.state.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MemPalaceManager.SearchResult>>(emptyList()) }
    var wings by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedWing by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        memPalaceManager.initialize()
        memPalaceManager.listWings().onSuccess { wings = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🧠 记忆宫殿") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            memPalaceManager.initialize()
                            memPalaceManager.listWings().onSuccess { wings = it }
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
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("🔍 搜索记忆") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🏛️ 记忆宫殿") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🤖 智能体") }
                )
            }

            when (selectedTab) {
                0 -> SearchMemoryTab(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearch = {
                        scope.launch {
                            isSearching = true
                            memPalaceManager.search(searchQuery, limit = 10)
                                .onSuccess { searchResults = it }
                            isSearching = false
                        }
                    },
                    searchResults = searchResults,
                    isSearching = isSearching
                )

                1 -> PalaceStructureTab(
                    wings = wings,
                    selectedWing = selectedWing,
                    onWingSelect = { selectedWing = it },
                    onRefresh = {
                        scope.launch {
                            memPalaceManager.listWings().onSuccess { wings = it }
                        }
                    }
                )

                2 -> AgentsTab(
                    onAddAgent = { name, description ->
                        scope.launch {
                            memPalaceManager.addAgent(name, description)
                        }
                    },
                    onWriteDiary = { agent, content ->
                        scope.launch {
                            memPalaceManager.writeDiary(agent, content)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SearchMemoryTab(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchResults: List<MemPalaceManager.SearchResult>,
    isSearching: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("搜索记忆...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onSearch, enabled = searchQuery.isNotBlank()) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (searchResults.isEmpty() && !isSearching) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "输入关键词搜索记忆",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "例如：之前的数据库设计、API 实现方案",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                "找到 ${searchResults.size} 条相关记忆",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { result ->
                    MemoryCard(result)
                }
            }
        }
    }
}

@Composable
fun MemoryCard(result: MemPalaceManager.SearchResult) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text(result.wing) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(result.room) }
                    )
                }
                Text(
                    text = dateFormat.format(Date(result.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "相似度: ${(result.score * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { /* Copy to clipboard */ }) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PalaceStructureTab(
    wings: List<String>,
    selectedWing: String,
    onWingSelect: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "🏛️ 记忆宫殿结构",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (wings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("记忆宫殿尚未初始化")
                    Text(
                        "在设置中配置并初始化 Linux 子系统",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            Text(
                "Wings（项目）",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(wings) { wing ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onWingSelect(wing) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedWing == wing)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        wing,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Wing · 包含多个 Rooms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "查看详情"
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "💡 记忆层级说明",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("🏛️ Wing（侧翼）", style = MaterialTheme.typography.labelMedium)
                Text("项目的顶级分类，如：my-agent、frontend、backend")
                Spacer(modifier = Modifier.height(4.dp))
                Text("🚪 Room（房间）", style = MaterialTheme.typography.labelMedium)
                Text("主题分类，如：database、api、auth")
                Spacer(modifier = Modifier.height(4.dp))
                Text("🗄️ Drawer（抽屉）", style = MaterialTheme.typography.labelMedium)
                Text("原始内容，如：对话片段、文件内容、代码片段")
            }
        }
    }
}

@Composable
fun AgentsTab(
    onAddAgent: (String, String) -> Unit,
    onWriteDiary: (String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var agentName by remember { mutableStateOf("") }
    var agentDescription by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加智能体") },
            text = {
                Column {
                    OutlinedTextField(
                        value = agentName,
                        onValueChange = { agentName = it },
                        label = { Text("智能体名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = agentDescription,
                        onValueChange = { agentDescription = it },
                        label = { Text("描述") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddAgent(agentName, agentDescription)
                        showAddDialog = false
                        agentName = ""
                        agentDescription = ""
                    },
                    enabled = agentName.isNotBlank()
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🤖 智能体",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加智能体")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "智能体功能",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "每个智能体拥有独立的记忆空间",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("• 为每个专业任务创建独立的智能体", style = MaterialTheme.typography.bodySmall)
                Text("• 智能体拥有自己的 wing、房间和日记", style = MaterialTheme.typography.bodySmall)
                Text("• 可通过 MCP 协议在运行时发现智能体", style = MaterialTheme.typography.bodySmall)
                Text("• 智能体之间可以共享记忆", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
