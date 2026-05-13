package com.example.agentmemory

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var modelConfig by remember { mutableStateOf(ModelConfig()) }
    var mcpConfig by remember { mutableStateOf(McpConfig()) }
    var tools by remember { mutableStateOf<List<ToolConfig>>(emptyList()) }
    var skills by remember { mutableStateOf<List<SkillConfig>>(emptyList()) }
    var knowledgeBases by remember { mutableStateOf<List<KnowledgeBaseConfig>>(emptyList()) }
    var rules by remember { mutableStateOf<List<RuleConfig>>(emptyList()) }
    
    var showToolDialog by remember { mutableStateOf(false) }
    var showSkillDialog by remember { mutableStateOf(false) }
    var showKnowledgeBaseDialog by remember { mutableStateOf(false) }
    var showRuleDialog by remember { mutableStateOf(false) }
    var currentEditItem by remember { mutableStateOf<Any?>(null) }
    var isRegisteringTool by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.modelConfig.collect { modelConfig = it }
    }
    LaunchedEffect(Unit) {
        viewModel.mcpConfig.collect { mcpConfig = it }
    }
    LaunchedEffect(Unit) {
        viewModel.toolsConfig.collect { tools = it }
    }
    LaunchedEffect(Unit) {
        viewModel.skillsConfig.collect { skills = it }
    }
    LaunchedEffect(Unit) {
        viewModel.knowledgeBasesConfig.collect { knowledgeBases = it }
    }
    LaunchedEffect(Unit) {
        viewModel.rulesConfig.collect { rules = it }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    val success = viewModel.exportConfig(os)
                    Toast.makeText(context, if (success) "导出成功" else "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openInputStream(it)?.use { is ->
                    val success = viewModel.importConfig(is)
                    Toast.makeText(context, if (success) "导入成功" else "导入失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        val success = viewModel.exportConfig(os)
                        if (success) {
                            Toast.makeText(context, "配置已保存，正在准备分享...", Toast.LENGTH_SHORT).show()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, it)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "分享配置"))
                        } else {
                            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { exportLauncher.launch("agent_memory_config.json") }) {
                        Text(context.getString(R.string.export_config))
                    }
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Text(context.getString(R.string.import_config))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ModelConfigCard(
                    config = modelConfig,
                    onConfigChanged = { newConfig -> 
                        modelConfig = newConfig
                        scope.launch { viewModel.saveModelConfig(newConfig) }
                    }
                )
            }
            item {
                McpConfigCard(
                    config = mcpConfig,
                    onConfigChanged = { newConfig -> 
                        mcpConfig = newConfig
                        scope.launch { viewModel.saveMcpConfig(newConfig) }
                    }
                )
            }
            item {
                ToolsSection(
                    tools = tools,
                    onAddTool = { 
                        currentEditItem = null
                        showToolDialog = true 
                    },
                    onEditTool = { tool ->
                        currentEditItem = tool
                        showToolDialog = true
                    },
                    onDeleteTool = { tool ->
                        tools = tools - tool
                        scope.launch { viewModel.saveToolsConfig(tools) }
                    },
                    onToggleTool = { tool, enabled ->
                        tools = tools.map { if (it == tool) it.copy(enabled = enabled) else it }
                        scope.launch { viewModel.saveToolsConfig(tools) }
                    }
                )
            }
            
            if (showToolDialog) {
                ToolDialog(
                    initialTool = currentEditItem as? ToolConfig,
                    isRegistering = isRegisteringTool,
                    onDismiss = { showToolDialog = false },
                    onSave = { tool ->
                        scope.launch {
                            isRegisteringTool = true
                            try {
                                // Try to register the tool with palace-daemon
                                val registerResult = viewModel.registerTool(tool)
                                if (registerResult.isSuccess) {
                                    Toast.makeText(context, "工具注册成功", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context, 
                                        "工具注册失败: ${registerResult.exceptionOrNull()?.message}", 
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context, 
                                    "工具注册出错: ${e.message}", 
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            // Always save to local config
                            val existingIndex = tools.indexOfFirst { it.name == tool.name }
                            tools = if (existingIndex >= 0) {
                                tools.toMutableList().also { it[existingIndex] = tool }
                            } else {
                                tools + tool
                            }
                            viewModel.saveToolsConfig(tools)
                            isRegisteringTool = false
                            showToolDialog = false
                        }
                    }
                )
            }
            item {
                SkillsSection(
                    skills = skills,
                    onAddSkill = {
                        currentEditItem = null
                        showSkillDialog = true
                    },
                    onEditSkill = { skill ->
                        currentEditItem = skill
                        showSkillDialog = true
                    },
                    onDeleteSkill = { skill ->
                        skills = skills - skill
                        scope.launch { viewModel.saveSkillsConfig(skills) }
                    },
                    onToggleSkill = { skill, enabled ->
                        skills = skills.map { if (it == skill) it.copy(enabled = enabled) else it }
                        scope.launch { viewModel.saveSkillsConfig(skills) }
                    }
                )
            }
            item {
                KnowledgeBasesSection(
                    bases = knowledgeBases,
                    onAddBase = {
                        currentEditItem = null
                        showKnowledgeBaseDialog = true
                    },
                    onEditBase = { base ->
                        currentEditItem = base
                        showKnowledgeBaseDialog = true
                    },
                    onDeleteBase = { base ->
                        knowledgeBases = knowledgeBases - base
                        scope.launch { viewModel.saveKnowledgeBasesConfig(knowledgeBases) }
                    },
                    onToggleBase = { base, enabled ->
                        knowledgeBases = knowledgeBases.map { if (it == base) it.copy(enabled = enabled) else it }
                        scope.launch { viewModel.saveKnowledgeBasesConfig(knowledgeBases) }
                    }
                )
            }
            item {
                RulesSection(
                    rules = rules,
                    onAddRule = {
                        currentEditItem = null
                        showRuleDialog = true
                    },
                    onEditRule = { rule ->
                        currentEditItem = rule
                        showRuleDialog = true
                    },
                    onDeleteRule = { rule ->
                        rules = rules - rule
                        scope.launch { viewModel.saveRulesConfig(rules) }
                    },
                    onToggleRule = { rule, enabled ->
                        rules = rules.map { if (it == rule) it.copy(enabled = enabled) else it }
                        scope.launch { viewModel.saveRulesConfig(rules) }
                    }
                )
            }
        }
    }

    if (showSkillDialog) {
        SkillDialog(
            initialSkill = currentEditItem as? SkillConfig,
            onDismiss = { showSkillDialog = false },
            onSave = { skill ->
                if (currentEditItem != null) {
                    skills = skills.map { if (it == currentEditItem) skill else it }
                } else {
                    skills = skills + skill
                }
                scope.launch { viewModel.saveSkillsConfig(skills) }
                showSkillDialog = false
            }
        )
    }

    if (showKnowledgeBaseDialog) {
        KnowledgeBaseDialog(
            initialBase = currentEditItem as? KnowledgeBaseConfig,
            onDismiss = { showKnowledgeBaseDialog = false },
            onSave = { base ->
                if (currentEditItem != null) {
                    knowledgeBases = knowledgeBases.map { if (it == currentEditItem) base else it }
                } else {
                    knowledgeBases = knowledgeBases + base
                }
                scope.launch { viewModel.saveKnowledgeBasesConfig(knowledgeBases) }
                showKnowledgeBaseDialog = false
            }
        )
    }

    if (showRuleDialog) {
        RuleDialog(
            initialRule = currentEditItem as? RuleConfig,
            onDismiss = { showRuleDialog = false },
            onSave = { rule ->
                if (currentEditItem != null) {
                    rules = rules.map { if (it == currentEditItem) rule else it }
                } else {
                    rules = rules + rule
                }
                scope.launch { viewModel.saveRulesConfig(rules) }
                showRuleDialog = false
            }
        )
    }
}

@Composable
fun ModelConfigCard(
    config: ModelConfig,
    onConfigChanged: (ModelConfig) -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(context.getString(R.string.model_config), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = config.apiUrl,
                onValueChange = { onConfigChanged(config.copy(apiUrl = it)) },
                label = { Text(context.getString(R.string.api_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = config.apiKey,
                onValueChange = { onConfigChanged(config.copy(apiKey = it)) },
                label = { Text(context.getString(R.string.api_key)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = config.modelName,
                onValueChange = { onConfigChanged(config.copy(modelName = it)) },
                label = { Text(context.getString(R.string.model_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("${context.getString(R.string.temperature)}: ${config.temperature}")
            Slider(
                value = config.temperature,
                onValueChange = { onConfigChanged(config.copy(temperature = it)) },
                valueRange = 0f..2f,
                steps = 19
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = config.maxTokens.toString(),
                onValueChange = { onConfigChanged(config.copy(maxTokens = it.toIntOrNull() ?: 2048)) },
                label = { Text(context.getString(R.string.max_tokens)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("${context.getString(R.string.top_p)}: ${config.topP}")
            Slider(
                value = config.topP,
                onValueChange = { onConfigChanged(config.copy(topP = it)) },
                valueRange = 0f..1f,
                steps = 9
            )
        }
    }
}

@Composable
fun McpConfigCard(
    config: McpConfig,
    onConfigChanged: (McpConfig) -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(context.getString(R.string.mcp_config), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(context.getString(R.string.enable_mcp))
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { onConfigChanged(config.copy(enabled = it)) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = config.serviceUrl,
                onValueChange = { onConfigChanged(config.copy(serviceUrl = it)) },
                label = { Text(context.getString(R.string.mcp_service_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = config.enabled
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = config.apiKey,
                onValueChange = { onConfigChanged(config.copy(apiKey = it)) },
                label = { Text(context.getString(R.string.api_key)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = config.enabled
            )
        }
    }
}

@Composable
fun ToolsSection(
    tools: List<ToolConfig>,
    onAddTool: () -> Unit,
    onEditTool: (ToolConfig) -> Unit,
    onDeleteTool: (ToolConfig) -> Unit,
    onToggleTool: (ToolConfig, Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(context.getString(R.string.tools), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddTool) {
                    Icon(Icons.Default.Add, contentDescription = "Add Tool")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            tools.forEach { tool ->
                ToolItem(
                    tool = tool,
                    onEdit = { onEditTool(tool) },
                    onDelete = { onDeleteTool(tool) },
                    onToggle = { enabled -> onToggleTool(tool, enabled) }
                )
            }
        }
    }
}

@Composable
fun ToolItem(
    tool: ToolConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(checked = tool.enabled, onCheckedChange = onToggle)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tool.name, style = MaterialTheme.typography.bodyMedium)
            Text(tool.description, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
fun ToolDialog(
    initialTool: ToolConfig?,
    isRegistering: Boolean,
    onDismiss: () -> Unit,
    onSave: (ToolConfig) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialTool?.name ?: "") }
    var description by remember { mutableStateOf(initialTool?.description ?: "") }
    var parameters by remember { mutableStateOf(initialTool?.parameters ?: emptyList()) }
    var code by remember { mutableStateOf(initialTool?.code ?: "") }
    var showParameterDialog by remember { mutableStateOf(false) }
    var editingParameter by remember { mutableStateOf<ToolParameter?>(null) }
    
    if (showParameterDialog) {
        ParameterDialog(
            initialParameter = editingParameter,
            onDismiss = {
                showParameterDialog = false
                editingParameter = null
            },
            onSave = { param ->
                if (editingParameter != null) {
                    parameters = parameters.map { if (it == editingParameter) param else it }
                } else {
                    parameters = parameters + param
                }
                showParameterDialog = false
                editingParameter = null
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = if (!isRegistering) onDismiss else {},
        title = { Text(if (initialTool == null) "添加工具" else "编辑工具") },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("函数名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isRegistering,
                        supportingText = { Text("英文标识符，例如：get_weather") }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("功能描述") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRegistering,
                        supportingText = { Text("简要描述工具的功能") }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("参数列表", style = MaterialTheme.typography.titleSmall)
                        TextButton(
                            onClick = {
                                editingParameter = null
                                showParameterDialog = true
                            },
                            enabled = !isRegistering
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "添加参数", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加")
                        }
                    }
                }
                if (parameters.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                "暂无参数，点击上方按钮添加",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(parameters.size) { index ->
                        val param = parameters[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        param.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "类型: ${param.type} | ${if (param.required) "必填" else "可选"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        param.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingParameter = param
                                        showParameterDialog = true
                                    },
                                    enabled = !isRegistering
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                                }
                                IconButton(
                                    onClick = {
                                        parameters = parameters - param
                                    },
                                    enabled = !isRegistering
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Python 代码") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 8,
                        maxLines = 15,
                        enabled = !isRegistering,
                        supportingText = { Text("粘贴完整的 Python 函数体") }
                    )
                }
                if (isRegistering) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在注册工具...")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && description.isNotBlank() && code.isNotBlank()) {
                        onSave(ToolConfig(name, description, parameters, code))
                    }
                },
                enabled = !isRegistering && name.isNotBlank() && description.isNotBlank() && code.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRegistering
            ) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParameterDialog(
    initialParameter: ToolParameter?,
    onDismiss: () -> Unit,
    onSave: (ToolParameter) -> Unit
) {
    var name by remember { mutableStateOf(initialParameter?.name ?: "") }
    var type by remember { mutableStateOf(initialParameter?.type ?: "string") }
    var description by remember { mutableStateOf(initialParameter?.description ?: "") }
    var required by remember { mutableStateOf(initialParameter?.required ?: false) }
    var typeExpanded by remember { mutableStateOf(false) }
    val types = listOf("string", "number", "boolean", "object")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialParameter == null) "添加参数" else "编辑参数") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("参数名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("英文标识符") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("参数类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    type = t
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("参数描述") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("描述参数的用途") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("必填参数")
                    Switch(
                        checked = required,
                        onCheckedChange = { required = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && description.isNotBlank()) {
                        onSave(ToolParameter(name, type, description, required))
                    }
                },
                enabled = name.isNotBlank() && description.isNotBlank()
            ) {
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

@Composable
fun SkillsSection(
    skills: List<SkillConfig>,
    onAddSkill: () -> Unit,
    onEditSkill: (SkillConfig) -> Unit,
    onDeleteSkill: (SkillConfig) -> Unit,
    onToggleSkill: (SkillConfig, Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(context.getString(R.string.skills), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddSkill) {
                    Icon(Icons.Default.Add, contentDescription = "Add Skill")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            skills.forEach { skill ->
                SkillItem(
                    skill = skill,
                    onEdit = { onEditSkill(skill) },
                    onDelete = { onDeleteSkill(skill) },
                    onToggle = { enabled -> onToggleSkill(skill, enabled) }
                )
            }
        }
    }
}

@Composable
fun SkillItem(
    skill: SkillConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(checked = skill.enabled, onCheckedChange = onToggle)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(skill.name, style = MaterialTheme.typography.bodyMedium)
            Text(skill.description, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
fun SkillDialog(
    initialSkill: SkillConfig?,
    onDismiss: () -> Unit,
    onSave: (SkillConfig) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialSkill?.name ?: "") }
    var description by remember { mutableStateOf(initialSkill?.description ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSkill == null) "Add Skill" else "Edit Skill") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(context.getString(R.string.function_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(context.getString(R.string.description)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && description.isNotBlank()) {
                    onSave(SkillConfig(name, description))
                }
            }) {
                Text(context.getString(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

@Composable
fun KnowledgeBasesSection(
    bases: List<KnowledgeBaseConfig>,
    onAddBase: () -> Unit,
    onEditBase: (KnowledgeBaseConfig) -> Unit,
    onDeleteBase: (KnowledgeBaseConfig) -> Unit,
    onToggleBase: (KnowledgeBaseConfig, Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(context.getString(R.string.knowledge_bases), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddBase) {
                    Icon(Icons.Default.Add, contentDescription = "Add Knowledge Base")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            bases.forEach { base ->
                KnowledgeBaseItem(
                    base = base,
                    onEdit = { onEditBase(base) },
                    onDelete = { onDeleteBase(base) },
                    onToggle = { enabled -> onToggleBase(base, enabled) }
                )
            }
        }
    }
}

@Composable
fun KnowledgeBaseItem(
    base: KnowledgeBaseConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(checked = base.enabled, onCheckedChange = onToggle)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(base.name, style = MaterialTheme.typography.bodyMedium)
            Text(base.path, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
fun KnowledgeBaseDialog(
    initialBase: KnowledgeBaseConfig?,
    onDismiss: () -> Unit,
    onSave: (KnowledgeBaseConfig) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialBase?.name ?: "") }
    var path by remember { mutableStateOf(initialBase?.path ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBase == null) "Add Knowledge Base" else "Edit Knowledge Base") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(context.getString(R.string.function_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Path") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && path.isNotBlank()) {
                    onSave(KnowledgeBaseConfig(name, path))
                }
            }) {
                Text(context.getString(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

@Composable
fun RulesSection(
    rules: List<RuleConfig>,
    onAddRule: () -> Unit,
    onEditRule: (RuleConfig) -> Unit,
    onDeleteRule: (RuleConfig) -> Unit,
    onToggleRule: (RuleConfig, Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(context.getString(R.string.rules_management), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddRule) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rule")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            rules.forEach { rule ->
                RuleItem(
                    rule = rule,
                    onEdit = { onEditRule(rule) },
                    onDelete = { onDeleteRule(rule) },
                    onToggle = { enabled -> onToggleRule(rule, enabled) }
                )
            }
        }
    }
}

@Composable
fun RuleItem(
    rule: RuleConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(checked = rule.enabled, onCheckedChange = onToggle)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(rule.name, style = MaterialTheme.typography.bodyMedium)
            Text("${rule.type}: ${rule.description}", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

@Composable
fun RuleDialog(
    initialRule: RuleConfig?,
    onDismiss: () -> Unit,
    onSave: (RuleConfig) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var description by remember { mutableStateOf(initialRule?.description ?: "") }
    var content by remember { mutableStateOf(initialRule?.content ?: "") }
    var type by remember { mutableStateOf(initialRule?.type ?: "global") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRule == null) "Add Rule" else "Edit Rule") },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(context.getString(R.string.function_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(context.getString(R.string.description)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Type") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 15
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && description.isNotBlank() && content.isNotBlank()) {
                    val id = initialRule?.id ?: System.currentTimeMillis().toString()
                    onSave(RuleConfig(id, name, description, content, type))
                }
            }) {
                Text(context.getString(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}
