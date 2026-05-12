package com.example.agentmemory

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
import io.github.rosemoe.sora.langs.python.PythonLanguage
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var codeEditor by remember { mutableStateOf<CodeEditor?>(null) }
    var isDarkMode by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.editor)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        codeEditor?.let { editor ->
                            editor.text?.let { content ->
                                android.content.ClipboardManager(context).also {
                                    it.setPrimaryClip(android.content.ClipData.newPlainText("Code", content.toString()))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
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
                    Toast.makeText(context, "Code saved", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
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
                            # Python Code Editor
                            def hello_world():
                                print("Hello from AgentMemory!")
                                return "Code is editable"
                            
                            # Start coding here
                            result = hello_world()
                            print(result)
                            """.trimIndent()
                        )
                        codeEditor = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { editor ->
                    // Update state if needed
                }
            )
        }
    }
}
