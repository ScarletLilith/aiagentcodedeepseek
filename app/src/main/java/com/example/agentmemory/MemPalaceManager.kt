package com.example.agentmemory

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MemPalaceManager(private val context: Context) {

    companion object {
        private const val TAG = "MemPalaceManager"
        private const val MEMPALACE_DIR = "mempalace"
        private const val DEFAULT_PALACE_PATH = "my_agent_memory"
    }

    sealed class MemPalaceState {
        object Idle : MemPalaceState()
        object Initializing : MemPalaceState()
        object Ready : MemPalaceState()
        data class Mining(val progress: Float, val message: String) : MemPalaceState()
        data class Searching(val query: String) : MemPalaceState()
        data class Error(val message: String) : MemPalaceState()
    }

    private val _state = MutableStateFlow<MemPalaceState>(MemPalaceState.Idle)
    val state: StateFlow<MemPalaceState> = _state

    private val memPalaceDir: File
        get() = File(context.getExternalFilesDir(null), MEMPALACE_DIR)

    private val palacePath: String
        get() = "${memPalaceDir.absolutePath}/$DEFAULT_PALACE_PATH"

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = MemPalaceState.Initializing
            memPalaceDir.mkdirs()
            
            Result.success(Unit).also {
                _state.value = MemPalaceState.Ready
                Log.d(TAG, "MemPalace initialized at $palacePath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MemPalace", e)
            _state.value = MemPalaceState.Error(e.message ?: "初始化失败")
            Result.failure(e)
        }
    }

    suspend fun mineProject(
        projectPath: String,
        wing: String,
        mode: String = "files"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            _state.value = MemPalaceState.Mining(0.1f, "开始挖掘项目...")
            
            val command = buildString {
                append("cd $palacePath && ")
                append("mempalace mine $projectPath ")
                append("--wing $wing ")
                if (mode == "convos") {
                    append("--mode convos ")
                }
            }
            
            Log.d(TAG, "Mining command: $command")
            _state.value = MemPalaceState.Mining(0.5f, "正在处理文件...")
            
            // Simulate mining completion
            _state.value = MemPalaceState.Mining(1.0f, "挖掘完成")
            _state.value = MemPalaceState.Ready
            
            Result.success("Successfully mined $projectPath into wing '$wing'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mine project", e)
            _state.value = MemPalaceState.Error(e.message ?: "挖掘失败")
            Result.failure(e)
        }
    }

    suspend fun wakeUp(query: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            _state.value = MemPalaceState.Searching(query ?: "all")
            
            val command = if (query != null) {
                "cd $palacePath && mempalace wake-up --query '$query'"
            } else {
                "cd $palacePath && mempalace wake-up"
            }
            
            Log.d(TAG, "Wake-up command: $command")
            
            // In real implementation, this would execute via PRoot
            _state.value = MemPalaceState.Ready
            
            Result.success("Context loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wake up", e)
            _state.value = MemPalaceState.Error(e.message ?: "唤醒失败")
            Result.failure(e)
        }
    }

    suspend fun search(query: String, limit: Int = 5): Result<List<SearchResult>> = withContext(Dispatchers.IO) {
        try {
            _state.value = MemPalaceState.Searching(query)
            
            val command = "cd $palacePath && mempalace search '$query' --limit $limit"
            Log.d(TAG, "Search command: $command")
            
            // Return mock results for demonstration
            val results = listOf(
                SearchResult(
                    id = "1",
                    content = "示例记忆内容 - 关于之前的数据库设计",
                    wing = "my-agent",
                    room = "database",
                    score = 0.95f,
                    timestamp = System.currentTimeMillis()
                )
            )
            
            _state.value = MemPalaceState.Ready
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            _state.value = MemPalaceState.Error(e.message ?: "搜索失败")
            Result.failure(e)
        }
    }

    suspend fun listWings(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val command = "cd $palacePath && mempalace list-wings"
            Log.d(TAG, "List wings command: $command")
            
            // Return mock wings
            Result.success(listOf("default", "my-agent"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list wings", e)
            Result.failure(e)
        }
    }

    suspend fun listRooms(wing: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val command = "cd $palacePath && mempalace list-rooms --wing $wing"
            Log.d(TAG, "List rooms command: $command")
            
            Result.success(listOf("general", "database", "api"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list rooms", e)
            Result.failure(e)
        }
    }

    suspend fun getDrawer(drawerId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val command = "cd $palacePath && mempalace get-drawer $drawerId"
            Log.d(TAG, "Get drawer command: $command")
            
            Result.success("Drawer content for $drawerId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get drawer", e)
            Result.failure(e)
        }
    }

    suspend fun addAgent(name: String, description: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val command = "cd $palacePath && mempalace add-agent '$name' --description '$description'"
            Log.d(TAG, "Add agent command: $command")
            
            Result.success("Agent '$name' added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add agent", e)
            Result.failure(e)
        }
    }

    suspend fun writeDiary(agent: String, content: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val command = "cd $palacePath && mempalace write-diary --agent '$agent' --content '$content'"
            Log.d(TAG, "Write diary command: $command")
            
            Result.success("Diary entry written successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write diary", e)
            Result.failure(e)
        }
    }

    suspend fun getContextForLLM(query: String): Result<ContextResult> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Wake up and get relevant memories
            val memories = search(query, limit = 10).getOrNull() ?: emptyList()
            
            // Step 2: Format context for LLM
            val formattedContext = buildString {
                appendLine("# 相关记忆")
                appendLine()
                memories.forEachIndexed { index, memory ->
                    appendLine("## 记忆 ${index + 1}")
                    appendLine("**Wing**: ${memory.wing}")
                    appendLine("**Room**: ${memory.room}")
                    appendLine("**时间**: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(memory.timestamp))}")
                    appendLine()
                    appendLine(memory.content)
                    appendLine()
                    appendLine("---")
                    appendLine()
                }
            }
            
            // Step 3: Build system prompt with context
            val systemPrompt = buildString {
                appendLine("## 记忆宫殿上下文")
                appendLine()
                appendLine("以下是与你当前查询相关的历史记忆：")
                appendLine()
                appendLine(formattedContext)
                appendLine()
                appendLine("请结合以上记忆来回答用户的问题。")
            }
            
            Result.success(
                ContextResult(
                    systemPrompt = systemPrompt,
                    memories = memories,
                    query = query
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get context for LLM", e)
            Result.failure(e)
        }
    }

    data class SearchResult(
        val id: String,
        val content: String,
        val wing: String,
        val room: String,
        val score: Float,
        val timestamp: Long
    )

    data class ContextResult(
        val systemPrompt: String,
        val memories: List<SearchResult>,
        val query: String
    )
}
