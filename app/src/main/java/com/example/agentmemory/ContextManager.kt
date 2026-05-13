package com.example.agentmemory

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ContextManager(private val context: Context) {

    companion object {
        private const val TAG = "ContextManager"
        private const val CONTEXT_FILE = "context_history.json"
        private const val MAX_HISTORY = 1000
        private const val AUTO_SNAPSHOT_INTERVAL = 10
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val contextFile: File
        get() = File(context.getExternalFilesDir(null), CONTEXT_FILE)

    private val _currentContext = MutableStateFlow<ContextBundle?>(null)
    val currentContext: StateFlow<ContextBundle?> = _currentContext

    private val _contextHistory = MutableStateFlow<List<ContextEntry>>(emptyList())
    val contextHistory: StateFlow<List<ContextEntry>> = _contextHistory

    private val _messageCount = MutableStateFlow(0)
    val messageCount: StateFlow<Int> = _messageCount

    private val gitSnapshotManager = GitSnapshotManager(context)

    val snapshotManager: GitSnapshotManager
        get() = gitSnapshotManager

    data class ContextBundle(
        val query: String,
        val memories: List<MemoryReference>,
        val timestamp: Long = System.currentTimeMillis(),
        val sessionId: String = generateSessionId()
    )

    data class MemoryReference(
        val id: String,
        val content: String,
        val wing: String,
        val room: String,
        val score: Float,
        val timestamp: Long
    )

    @Serializable
    data class ContextEntry(
        val id: String,
        val query: String,
        val memories: List<MemoryEntry>,
        val response: String? = null,
        val timestamp: Long,
        val sessionId: String,
        val tags: List<String> = emptyList()
    )

    @Serializable
    data class MemoryEntry(
        val id: String,
        val content: String,
        val wing: String,
        val room: String,
        val score: Float,
        val timestamp: Long
    )

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun generateEntryId(): String {
        return "entry_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    init {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            loadFromFile()
        }
    }

    suspend fun buildContext(
        query: String,
        memories: List<MemPalaceManager.SearchResult>
    ): String = withContext(Dispatchers.IO) {
        try {
            val memoryRefs = memories.map { result ->
                MemoryReference(
                    id = result.id,
                    content = result.content,
                    wing = result.wing,
                    room = result.room,
                    score = result.score,
                    timestamp = result.timestamp
                )
            }

            val bundle = ContextBundle(
                query = query,
                memories = memoryRefs
            )

            _currentContext.value = bundle

            val contextText = buildContextText(bundle)

            Log.d(TAG, "Built context with ${memories.size} memories")
            contextText
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build context", e)
            ""
        }
    }

    private fun buildContextText(bundle: ContextBundle): String {
        return buildString {
            appendLine("# 记忆宫殿上下文")
            appendLine()
            appendLine("**时间**: ${formatTimestamp(bundle.timestamp)}")
            appendLine("**会话**: ${bundle.sessionId}")
            appendLine()
            appendLine("---")
            appendLine()
            
            appendLine("## 相关记忆 (${bundle.memories.size} 条)")
            appendLine()
            
            bundle.memories.forEachIndexed { index, memory ->
                appendLine("### ${index + 1}. ${memory.wing} / ${memory.room}")
                appendLine()
                appendLine("**相似度**: ${(memory.score * 100).toInt()}%")
                appendLine("**时间**: ${formatTimestamp(memory.timestamp)}")
                appendLine()
                appendLine(memory.content)
                appendLine()
                appendLine("---")
                appendLine()
            }
            
            appendLine()
            appendLine("**提示**: 以上是与你当前问题相关的历史记忆，请结合这些信息回答。")
        }
    }

    suspend fun saveToHistory(
        query: String,
        memories: List<MemPalaceManager.SearchResult>,
        response: String? = null,
        tags: List<String> = emptyList()
    ) = withContext(Dispatchers.IO) {
        try {
            val memoryEntries = memories.map { result ->
                MemoryEntry(
                    id = result.id,
                    content = result.content,
                    wing = result.wing,
                    room = result.room,
                    score = result.score,
                    timestamp = result.timestamp
                )
            }

            val entry = ContextEntry(
                id = generateEntryId(),
                query = query,
                memories = memoryEntries,
                response = response,
                timestamp = System.currentTimeMillis(),
                sessionId = _currentContext.value?.sessionId ?: generateSessionId(),
                tags = tags
            )

            val currentHistory = _contextHistory.value.toMutableList()
            currentHistory.add(0, entry)
            
            if (currentHistory.size > MAX_HISTORY) {
                currentHistory.removeAt(currentHistory.size - 1)
            }
            
            _contextHistory.value = currentHistory
            _messageCount.value = currentHistory.size
            saveToFile()

            Log.d(TAG, "Saved entry to history: ${entry.id}")

            if (currentHistory.size % AUTO_SNAPSHOT_INTERVAL == 0) {
                createAutoSnapshot()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to history", e)
        }
    }

    suspend fun createAutoSnapshot(description: String = "Auto snapshot"): Result<GitSnapshotManager.Snapshot> {
        val messages = _contextHistory.value.map { entry ->
            GitSnapshotManager.ConversationMessage(
                id = entry.id,
                role = "user",
                content = entry.query,
                timestamp = entry.timestamp
            )
        }.toMutableList()

        _contextHistory.value.forEach { entry ->
            entry.response?.let { response ->
                messages.add(
                    GitSnapshotManager.ConversationMessage(
                        id = "${entry.id}_response",
                        role = "assistant",
                        content = response,
                        timestamp = entry.timestamp
                    )
                )
            }
        }

        return gitSnapshotManager.createSnapshot(
            messages = messages,
            description = description,
            tags = listOf("auto")
        )
    }

    suspend fun createSnapshot(
        description: String = "Manual snapshot",
        tags: List<String> = emptyList()
    ): Result<GitSnapshotManager.Snapshot> {
        val messages = _contextHistory.value.flatMap { entry ->
            val msgList = mutableListOf(
                GitSnapshotManager.ConversationMessage(
                    id = entry.id,
                    role = "user",
                    content = entry.query,
                    timestamp = entry.timestamp
                )
            )
            entry.response?.let { response ->
                msgList.add(
                    GitSnapshotManager.ConversationMessage(
                        id = "${entry.id}_response",
                        role = "assistant",
                        content = response,
                        timestamp = entry.timestamp
                    )
                )
            }
            msgList
        }

        return gitSnapshotManager.createSnapshot(
            messages = messages,
            description = description,
            tags = tags
        )
    }

    suspend fun restoreSnapshot(snapshotId: String): Result<Unit> {
        val result = gitSnapshotManager.restoreSnapshot(snapshotId)
        
        return if (result.isSuccess) {
            try {
                val snapshot = result.getOrNull()!!
                val entries = snapshot.messages.map { message ->
                    val memoryEntries = message.content.takeIf { message.role == "user" }?.let {
                        emptyList<MemoryEntry>()
                    } ?: emptyList()

                    ContextEntry(
                        id = message.id,
                        query = if (message.role == "user") message.content else "",
                        memories = memoryEntries,
                        response = if (message.role == "assistant") message.content else null,
                        timestamp = message.timestamp,
                        sessionId = "restored_${snapshot.id}",
                        tags = emptyList()
                    )
                }.filter { it.query.isNotEmpty() || it.response != null }

                _contextHistory.value = entries
                _messageCount.value = entries.size
                saveToFile()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    suspend fun getContextForQuery(
        query: String,
        memPalaceManager: MemPalaceManager,
        limit: Int = 10
    ): String = withContext(Dispatchers.IO) {
        try {
            val memories = memPalaceManager.search(query, limit)
                .getOrNull() ?: emptyList()

            buildContext(query, memories)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get context for query", e)
            ""
        }
    }

    suspend fun getSessionContext(sessionId: String): List<ContextEntry> {
        return _contextHistory.value.filter { it.sessionId == sessionId }
    }

    suspend fun getRecentContexts(limit: Int = 10): List<ContextEntry> {
        return _contextHistory.value.take(limit)
    }

    suspend fun getTimeline(
        startTime: Long? = null,
        endTime: Long? = null
    ): List<ContextEntry> {
        return _contextHistory.value.filter { entry ->
            val inTimeRange = if (startTime != null) entry.timestamp >= startTime else true &&
                    if (endTime != null) entry.timestamp <= endTime else true
            inTimeRange
        }.sortedByDescending { it.timestamp }
    }

    suspend fun searchHistory(query: String): List<ContextEntry> {
        val lowercaseQuery = query.lowercase()
        return _contextHistory.value.filter { entry ->
            entry.query.lowercase().contains(lowercaseQuery) ||
            entry.memories.any { it.content.lowercase().contains(lowercaseQuery) } ||
            entry.response?.lowercase()?.contains(lowercaseQuery) == true
        }
    }

    suspend fun loadFromFile() = withContext(Dispatchers.IO) {
        try {
            if (contextFile.exists()) {
                val content = contextFile.readText()
                val entries = json.decodeFromString<List<ContextEntry>>(content)
                _contextHistory.value = entries
                _messageCount.value = entries.size
                Log.d(TAG, "Loaded ${entries.size} entries from file")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load from file", e)
        }
    }

    private suspend fun saveToFile() = withContext(Dispatchers.IO) {
        try {
            val content = json.encodeToString(_contextHistory.value)
            contextFile.writeText(content)
            Log.d(TAG, "Saved ${_contextHistory.value.size} entries to file")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to file", e)
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        _contextHistory.value = emptyList()
        _currentContext.value = null
        _messageCount.value = 0
        if (contextFile.exists()) {
            contextFile.delete()
        }
        Log.d(TAG, "Cleared all history")
    }

    suspend fun exportContext(): String = withContext(Dispatchers.IO) {
        buildString {
            appendLine("# 记忆宫殿上下文导出")
            appendLine()
            appendLine("**导出时间**: ${formatTimestamp(System.currentTimeMillis())}")
            appendLine("**总条目数**: ${_contextHistory.value.size}")
            appendLine()
            appendLine("---")
            appendLine()
            
            _contextHistory.value.forEachIndexed { index, entry ->
                appendLine("## 条目 ${index + 1}")
                appendLine()
                appendLine("**ID**: ${entry.id}")
                appendLine("**会话**: ${entry.sessionId}")
                appendLine("**时间**: ${formatTimestamp(entry.timestamp)}")
                appendLine("**标签**: ${entry.tags.joinToString(", ")}")
                appendLine()
                appendLine("### 查询")
                appendLine(entry.query)
                appendLine()
                appendLine("### 相关记忆")
                entry.memories.forEach { memory ->
                    appendLine("- ${memory.wing}/${memory.room}: ${memory.content.take(200)}...")
                }
                appendLine()
                if (entry.response != null) {
                    appendLine("### 回复")
                    appendLine(entry.response)
                    appendLine()
                }
                appendLine("---")
                appendLine()
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun getStatistics(): ContextStatistics {
        val totalEntries = _contextHistory.value.size
        val totalMemories = _contextHistory.value.sumOf { it.memories.size }
        val avgMemoriesPerQuery = if (totalEntries > 0) totalMemories.toFloat() / totalEntries else 0f
        
        val wingCounts = mutableMapOf<String, Int>()
        _contextHistory.value.forEach { entry ->
            entry.memories.forEach { memory ->
                wingCounts[memory.wing] = wingCounts.getOrDefault(memory.wing, 0) + 1
            }
        }
        
        return ContextStatistics(
            totalEntries = totalEntries,
            totalMemories = totalMemories,
            avgMemoriesPerQuery = avgMemoriesPerQuery,
            topWings = wingCounts.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        )
    }

    data class ContextStatistics(
        val totalEntries: Int,
        val totalMemories: Int,
        val avgMemoriesPerQuery: Float,
        val topWings: List<Pair<String, Int>>
    )

    private object kotlinx {
        object coroutines {
            object GlobalScope {
                fun launch(context: kotlinx.coroutines.CoroutineDispatcher, block: suspend () -> Unit) {
                    kotlinx.coroutines.CoroutineScope(context).launch { block() }
                }
            }
            
            object CoroutineScope {
                operator fun invoke(context: kotlinx.coroutines.CoroutineDispatcher) = object {
                    fun launch(block: suspend () -> Unit) {
                        kotlinx.coroutines.GlobalScope.launch(context, block)
                    }
                }
            }
        }
    }
}
