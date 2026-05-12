package com.example.agentmemory

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

    if (showToolDialog) {
        ToolDialog(
            initialTool = currentEditItem as? ToolConfig,
            onDismiss = { showToolDialog = false },
            onSave = { tool ->
                if (currentEditItem != null) {
                    tools = tools.map { if (it == currentEditItem) tool else it }
                } else {
                    tools = tools + tool
                }
                scope.launch { viewModel.saveToolsConfig(tools) }
                showToolDialog = false
            }
        )
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
    onDismiss: () -> Unit,
    onSave: (ToolConfig) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialTool?.name ?: "") }
    var description by remember { mutableStateOf(initialTool?.description ?: "") }
    var parameters by remember { mutableStateOf(initialTool?.parameters ?: emptyList()) }
    var code by remember { mutableStateOf(initialTool?.code ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTool == null) "Add Tool" else "Edit Tool") },
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
                    Text("Parameters:", style = MaterialTheme.typography.labelLarge)
                }
                items(parameters) { param ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("${param.name} (${param.type})")
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(context.getString(R.string.code)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 15
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && description.isNotBlank() && code.isNotBlank()) {
                    onSave(ToolConfig(name, description, parameters, code))
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
