package com.example.agentmemory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<RuleConfig>>(emptyList()) }
    var showRuleDialog by remember { mutableStateOf(false) }
    var currentEditRule by remember { mutableStateOf<RuleConfig?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.rulesConfig.collect { rules = it }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.rules_management)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        currentEditRule = null
                        showRuleDialog = true 
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Rule")
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rules) { rule ->
                RuleCard(
                    rule = rule,
                    onEdit = { 
                        currentEditRule = rule
                        showRuleDialog = true 
                    },
                    onDelete = { 
                        rules = rules - rule
                        scope.launch { viewModel.saveRulesConfig(rules) }
                    },
                    onToggle = { enabled ->
                        rules = rules.map { if (it == rule) it.copy(enabled = enabled) else it }
                        scope.launch { viewModel.saveRulesConfig(rules) }
                    }
                )
            }
            if (rules.isEmpty()) {
                item {
                    Text(
                        "No rules yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
    
    if (showRuleDialog) {
        RuleEditorDialog(
            initialRule = currentEditRule,
            onDismiss = { showRuleDialog = false },
            onSave = { rule ->
                if (currentEditRule != null) {
                    rules = rules.map { if (it == currentEditRule) rule else it }
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
fun RuleCard(
    rule: RuleConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggle
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                Text("${rule.type}", style = MaterialTheme.typography.labelSmall)
                Text(rule.description, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorDialog(
    initialRule: RuleConfig?,
    onDismiss: () -> Unit,
    onSave: (RuleConfig) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var description by remember { mutableStateOf(initialRule?.description ?: "") }
    var content by remember { mutableStateOf(initialRule?.content ?: "") }
    var type by remember { mutableStateOf(initialRule?.type ?: "global") }
    val types = listOf("global", "project", "dynamic")
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRule == null) "Add Rule" else "Edit Rule") },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Rule Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = type,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rule Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            types.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = {
                                        type = t
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Rule Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 10
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && description.isNotBlank() && content.isNotBlank()) {
                        val id = initialRule?.id ?: System.currentTimeMillis().toString()
                        onSave(RuleConfig(id, name, description, content, type, true))
                    }
                }
            ) {
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
