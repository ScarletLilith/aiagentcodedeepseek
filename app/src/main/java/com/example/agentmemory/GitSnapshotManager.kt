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
import java.text.SimpleDateFormat
import java.util.*

class GitSnapshotManager(private val context: Context) {

    companion object {
        private const val TAG = "GitSnapshotManager"
        private const val SNAPSHOT_DIR = "conversation_snapshots"
        private const val SNAPSHOT_INDEX = "snapshot_index.json"
        private const val MAX_SNAPSHOTS = 100
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val snapshotDir: File
        get() = File(context.getExternalFilesDir(null), SNAPSHOT_DIR).also { it.mkdirs() }

    private val indexFile: File
        get() = File(snapshotDir, SNAPSHOT_INDEX)

    private val _snapshots = MutableStateFlow<List<Snapshot>>(emptyList())
    val snapshots: StateFlow<List<Snapshot>> = _snapshots

    private val _currentSnapshot = MutableStateFlow<Snapshot?>(null)
    val currentSnapshot: StateFlow<Snapshot?> = _currentSnapshot

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    @Serializable
    data class Snapshot(
        val id: String,
        val timestamp: Long,
        val commitHash: String,
        val description: String,
        val messageCount: Int,
        val characterCount: Int,
        val tags: List<String> = emptyList(),
        val parentId: String? = null
    )

    @Serializable
    data class SnapshotIndex(
        val snapshots: List<Snapshot>,
        val currentSnapshotId: String?,
        val version: Int = 1
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        loadIndex()
    }

    private fun loadIndex() {
        try {
            if (indexFile.exists()) {
                val content = indexFile.readText()
                val index = json.decodeFromString<SnapshotIndex>(content)
                _snapshots.value = index.snapshots
                _currentSnapshot.value = index.snapshots.find { it.id == index.currentSnapshotId }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load index", e)
        }
    }

    private fun saveIndex() {
        try {
            val index = SnapshotIndex(
                snapshots = _snapshots.value,
                currentSnapshotId = _currentSnapshot.value?.id
            )
            indexFile.writeText(json.encodeToString(index))
            Log.d(TAG, "Index saved: ${_snapshots.value.size} snapshots")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save index", e)
        }
    }

    suspend fun createSnapshot(
        messages: List<ConversationMessage>,
        description: String = "Auto snapshot",
        tags: List<String> = emptyList()
    ): Result<Snapshot> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true

            val id = generateSnapshotId()
            val timestamp = System.currentTimeMillis()
            val messageCount = messages.size
            val characterCount = messages.sumOf { it.content.length }

            // Create snapshot file
            val snapshotFile = File(snapshotDir, "$id.json")
            val snapshotData = ConversationSnapshot(
                id = id,
                timestamp = timestamp,
                messages = messages,
                metadata = mapOf(
                    "description" to description,
                    "messageCount" to messageCount,
                    "characterCount" to characterCount,
                    "tags" to tags
                )
            )
            snapshotFile.writeText(json.encodeToString(snapshotData))

            // Generate commit hash
            val commitHash = generateCommitHash(id, timestamp, messages)

            // Create snapshot metadata
            val snapshot = Snapshot(
                id = id,
                timestamp = timestamp,
                commitHash = commitHash,
                description = description,
                messageCount = messageCount,
                characterCount = characterCount,
                tags = tags,
                parentId = _currentSnapshot.value?.id
            )

            // Update state
            val currentList = _snapshots.value.toMutableList()
            currentList.add(0, snapshot)
            
            // Limit snapshots
            if (currentList.size > MAX_SNAPSHOTS) {
                val removed = currentList.removeAt(currentList.size - 1)
                File(snapshotDir, "${removed.id}.json").delete()
            }

            _snapshots.value = currentList
            _currentSnapshot.value = snapshot
            saveIndex()

            Log.d(TAG, "Created snapshot: $id, hash: $commitHash")
            Result.success(snapshot)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create snapshot", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun restoreSnapshot(snapshotId: String): Result<ConversationSnapshot> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true

            val snapshot = _snapshots.value.find { it.id == snapshotId }
                ?: return@withContext Result.failure(Exception("Snapshot not found: $snapshotId"))

            val snapshotFile = File(snapshotDir, "$snapshotId.json")
            if (!snapshotFile.exists()) {
                return@withContext Result.failure(Exception("Snapshot file not found"))
            }

            val content = snapshotFile.readText()
            val snapshotData = json.decodeFromString<ConversationSnapshot>(content)

            // Update current snapshot
            _currentSnapshot.value = snapshot
            saveIndex()

            Log.d(TAG, "Restored snapshot: $snapshotId")
            Result.success(snapshotData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore snapshot", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun getSnapshotDiff(
        fromSnapshotId: String,
        toSnapshotId: String
    ): Result<SnapshotDiff> = withContext(Dispatchers.IO) {
        try {
            val fromSnapshot = _snapshots.value.find { it.id == fromSnapshotId }
                ?: return@withContext Result.failure(Exception("Source snapshot not found"))
            
            val toSnapshot = _snapshots.value.find { it.id == toSnapshotId }
                ?: return@withContext Result.failure(Exception("Target snapshot not found"))

            val fromFile = File(snapshotDir, "$fromSnapshotId.json")
            val toFile = File(snapshotDir, "$toSnapshotId.json")

            if (!fromFile.exists() || !toFile.exists()) {
                return@withContext Result.failure(Exception("Snapshot files not found"))
            }

            val fromData = json.decodeFromString<ConversationSnapshot>(fromFile.readText())
            val toData = json.decodeFromString<ConversationSnapshot>(toFile.readText())

            val diff = calculateDiff(fromData, toData)

            val snapshotDiff = SnapshotDiff(
                fromSnapshot = fromSnapshot,
                toSnapshot = toSnapshot,
                addedMessages = diff.addedMessages,
                removedMessages = diff.removedMessages,
                modifiedMessages = diff.modifiedMessages,
                stats = DiffStats(
                    additions = diff.addedMessages.sumOf { it.content.length },
                    deletions = diff.removedMessages.sumOf { it.content.length },
                    totalChanges = diff.addedMessages.size + diff.removedMessages.size + diff.modifiedMessages.size
                )
            )

            Result.success(snapshotDiff)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get diff", e)
            Result.failure(e)
        }
    }

    suspend fun getSnapshotHistory(limit: Int = 20): List<Snapshot> {
        return _snapshots.value.take(limit)
    }

    suspend fun deleteSnapshot(snapshotId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (snapshotId == _currentSnapshot.value?.id) {
                return@withContext Result.failure(Exception("Cannot delete current snapshot"))
            }

            val currentList = _snapshots.value.toMutableList()
            val removed = currentList.removeIf { it.id == snapshotId }
            
            if (removed) {
                File(snapshotDir, "$snapshotId.json").delete()
                _snapshots.value = currentList
                saveIndex()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Snapshot not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete snapshot", e)
            Result.failure(e)
        }
    }

    suspend fun addTag(snapshotId: String, tag: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentList = _snapshots.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == snapshotId }
            
            if (index >= 0) {
                val snapshot = currentList[index]
                val newTags = snapshot.tags.toMutableList()
                if (!newTags.contains(tag)) {
                    newTags.add(tag)
                }
                currentList[index] = snapshot.copy(tags = newTags)
                _snapshots.value = currentList
                saveIndex()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Snapshot not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add tag", e)
            Result.failure(e)
        }
    }

    suspend fun searchSnapshots(query: String): List<Snapshot> {
        val lowercaseQuery = query.lowercase()
        return _snapshots.value.filter { snapshot ->
            snapshot.description.lowercase().contains(lowercaseQuery) ||
            snapshot.tags.any { it.lowercase().contains(lowercaseQuery) }
        }
    }

    suspend fun exportSnapshot(snapshotId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val snapshot = _snapshots.value.find { it.id == snapshotId }
                ?: return@withContext Result.failure(Exception("Snapshot not found"))

            val snapshotFile = File(snapshotDir, "$snapshotId.json")
            if (!snapshotFile.exists()) {
                return@withContext Result.failure(Exception("Snapshot file not found"))
            }

            val exportContent = buildString {
                appendLine("# Conversation Snapshot")
                appendLine()
                appendLine("**ID**: $snapshotId")
                appendLine("**Commit Hash**: ${snapshot.commitHash}")
                appendLine("**Created**: ${formatTimestamp(snapshot.timestamp)}")
                appendLine("**Description**: ${snapshot.description}")
                appendLine("**Messages**: ${snapshot.messageCount}")
                appendLine("**Characters**: ${snapshot.characterCount}")
                appendLine("**Tags**: ${snapshot.tags.joinToString(", ")}")
                appendLine()
                appendLine("---")
                appendLine()
                
                val data = json.decodeFromString<ConversationSnapshot>(snapshotFile.readText())
                data.messages.forEachIndexed { index, message ->
                    appendLine("## Message ${index + 1}")
                    appendLine("**Role**: ${message.role}")
                    appendLine("**Timestamp**: ${formatTimestamp(message.timestamp)}")
                    appendLine()
                    appendLine(message.content)
                    appendLine()
                    appendLine("---")
                    appendLine()
                }
            }

            Result.success(exportContent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export snapshot", e)
            Result.failure(e)
        }
    }

    fun getStatistics(): SnapshotStatistics {
        val snapshotList = _snapshots.value
        return SnapshotStatistics(
            totalSnapshots = snapshotList.size,
            totalMessages = snapshotList.sumOf { it.messageCount },
            totalCharacters = snapshotList.sumOf { it.characterCount },
            avgMessagesPerSnapshot = if (snapshotList.isNotEmpty()) 
                snapshotList.sumOf { it.messageCount }.toFloat() / snapshotList.size 
                else 0f,
            oldestSnapshot = snapshotList.lastOrNull()?.timestamp,
            newestSnapshot = snapshotList.firstOrNull()?.timestamp
        )
    }

    private fun generateSnapshotId(): String {
        return "snap_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun generateCommitHash(id: String, timestamp: Long, messages: List<ConversationMessage>): String {
        val content = buildString {
            append(id)
            append(timestamp)
            messages.forEach { msg ->
                append(msg.role)
                append(msg.content)
                append(msg.timestamp)
            }
        }
        
        // Simple hash function
        var hash = 0
        for (char in content) {
            hash = (hash * 31 + char.code) and 0xFFFFFFFF
        }
        
        return String.format("%08x", hash)
    }

    private fun calculateDiff(
        from: ConversationSnapshot,
        to: ConversationSnapshot
    ): DiffResult {
        val fromMap = from.messages.mapIndexed { index, msg -> index to msg }.toMap()
        val toMap = to.messages.mapIndexed { index, msg -> index to msg }.toMap()

        val addedMessages = to.messages.filter { toMsg ->
            !from.messages.any { fromMsg -> 
                fromMsg.id == toMsg.id && fromMsg.content == toMsg.content 
            }
        }

        val removedMessages = from.messages.filter { fromMsg ->
            !to.messages.any { toMsg -> 
                toMsg.id == fromMsg.id && toMsg.content == fromMsg.content 
            }
        }

        val modifiedMessages = from.messages.filter { fromMsg ->
            to.messages.any { toMsg -> 
                toMsg.id == fromMsg.id && toMsg.content != fromMsg.content 
            }
        }

        return DiffResult(
            addedMessages = addedMessages,
            removedMessages = removedMessages,
            modifiedMessages = modifiedMessages
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    @Serializable
    data class ConversationSnapshot(
        val id: String,
        val timestamp: Long,
        val messages: List<ConversationMessage>,
        val metadata: Map<String, Any>
    )

    @Serializable
    data class ConversationMessage(
        val id: String,
        val role: String,
        val content: String,
        val timestamp: Long
    )

    data class SnapshotDiff(
        val fromSnapshot: Snapshot,
        val toSnapshot: Snapshot,
        val addedMessages: List<ConversationMessage>,
        val removedMessages: List<ConversationMessage>,
        val modifiedMessages: List<ConversationMessage>,
        val stats: DiffStats
    )

    data class DiffStats(
        val additions: Int,
        val deletions: Int,
        val totalChanges: Int
    )

    data class DiffResult(
        val addedMessages: List<ConversationMessage>,
        val removedMessages: List<ConversationMessage>,
        val modifiedMessages: List<ConversationMessage>
    )

    data class SnapshotStatistics(
        val totalSnapshots: Int,
        val totalMessages: Int,
        val totalCharacters: Int,
        val avgMessagesPerSnapshot: Float,
        val oldestSnapshot: Long?,
        val newestSnapshot: Long?
    )
}
