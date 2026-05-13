package com.example.agentmemory

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.langs.python.PythonLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var codeEditor by remember { mutableStateOf<CodeEditor?>(null) }
    var isDarkMode by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Python") }
    var languageExpanded by remember { mutableStateOf(false) }
    var currentFilePath by remember { mutableStateOf<String?>(null) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    
    val languages = listOf("Python", "Java", "Kotlin", "JavaScript", "TypeScript", "C++", "C", "Go", "Rust")
    
    val languageMap = mapOf(
        "Python" to PythonLanguage(),
        "Java" to JavaLanguage(),
        "Kotlin" to JavaLanguage(),
        "JavaScript" to JavaLanguage(),
        "TypeScript" to JavaLanguage(),
        "C++" to JavaLanguage(),
        "C" to JavaLanguage(),
        "Go" to JavaLanguage(),
        "Rust" to JavaLanguage()
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("代码编辑器") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { showLanguageMenu = true }) {
                            Text(selectedLanguage)
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        selectedLanguage = lang
                                        showLanguageMenu = false
                                        codeEditor?.setEditorLanguage(languageMap[lang] ?: PythonLanguage())
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        codeEditor?.let { editor ->
                            editor.text?.let { content ->
                                android.content.ClipboardManager(context).also {
                                    it.setPrimaryClip(android.content.ClipData.newPlainText("Code", content.toString()))
                                    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { 
                            isDarkMode = it
                            codeEditor?.let { editor ->
                                if (it) {
                                    editor.colorScheme = EditorColorScheme(EditorColorScheme.THEME_DARK)
                                } else {
                                    editor.colorScheme = EditorColorScheme(EditorColorScheme.THEME_LIGHT)
                                }
                            }
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    saveCodeToFile(context, codeEditor, currentFilePath) { savedPath ->
                        currentFilePath = savedPath
                        Toast.makeText(context, "代码已保存到: $savedPath", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = "保存")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AndroidView(
                factory = { ctx ->
                    CodeEditor(ctx).apply {
                        setEditorLanguage(PythonLanguage())
                        colorScheme = EditorColorScheme(EditorColorScheme.THEME_LIGHT)
                        setLineNumberVisible(true)
                        setNonPrintableCharactersVisible(false)
                        setPinLineNumber(true)
                        setWordwrap(false)
                        isEditable = true
                        setText(
                            """
                            # ${selectedLanguage} Code Editor
                            # 在这里编写您的代码
                            
                            def main():
                                print("Hello from AgentMemory!")
                                return "Code is editable"
                            
                            # 开始编程
                            if __name__ == "__main__":
                                result = main()
                                print(result)
                            """.trimIndent()
                        )
                        codeEditor = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { editor ->
                }
            )
        }
    }
}

private fun saveCodeToFile(
    context: Context,
    codeEditor: CodeEditor?,
    currentPath: String?,
    onSaved: (String) -> Unit
) {
    codeEditor?.text?.toString()?.let { code ->
        try {
            val fileName = "code_${System.currentTimeMillis()}.py"
            val file = File(context.getExternalFilesDir("codes"), fileName)
            file.parentFile?.mkdirs()
            file.writeText(code)
            onSaved(file.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
