package com.example.agentmemory

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class CommandExecutor(private val context: Context) {

    companion object {
        private const val TAG = "CommandExecutor"
        private const val MAX_HISTORY = 100
    }

    sealed class CommandState {
        object Idle : CommandState()
        data class Running(val command: String, val pid: Long? = null) : CommandState()
        data class Completed(
            val command: String,
            val exitCode: Int,
            val output: String,
            val duration: Long
        ) : CommandState()
        data class Error(val message: String, val exception: Throwable? = null) : CommandState()
    }

    data class CommandHistory(
        val id: Long,
        val command: String,
        val timestamp: Long,
        val exitCode: Int?,
        val output: String?
    )

    private val _commandState = MutableStateFlow<CommandState>(CommandState.Idle)
    val commandState: StateFlow<CommandState> = _commandState

    private val _commandOutput = MutableStateFlow<String>("")
    val commandOutput: StateFlow<String> = _commandOutput

    private val _commandHistory = MutableStateFlow<List<CommandHistory>>(emptyList())
    val commandHistory: StateFlow<List<CommandHistory>> = _commandHistory

    private var currentProcess: Process? = null
    private var historyCounter = 0L

    suspend fun executeCommand(
        command: String,
        workingDir: String? = null,
        timeoutMs: Long = 30000L,
        env: Map<String, String> = emptyMap()
    ): CommandState = withContext(Dispatchers.IO) {
        try {
            _commandState.value = CommandState.Running(command)
            _commandOutput.value = ""

            val startTime = System.currentTimeMillis()
            val outputBuilder = StringBuilder()

            val processBuilder = ProcessBuilder()
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            
            if (isWindows) {
                processBuilder.command("cmd.exe", "/c", command)
            } else {
                processBuilder.command("sh", "-c", command)
            }

            workingDir?.let { dir ->
                val workDirFile = File(dir)
                if (workDirFile.exists() && workDirFile.isDirectory) {
                    processBuilder.directory(workDirFile)
                }
            }

            if (env.isNotEmpty()) {
                val environment = processBuilder.environment()
                env.forEach { (key, value) ->
                    environment[key] = value
                }
            }

            processBuilder.redirectErrorStream(true)

            currentProcess = processBuilder.start()

            val reader = currentProcess!!.inputStream.bufferedReader()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    outputBuilder.append(it).append("\n")
                    _commandOutput.value = outputBuilder.toString()
                }
            }

            val completed = currentProcess!!.waitFor()
            val duration = System.currentTimeMillis() - startTime

            val output = outputBuilder.toString()
            val historyItem = CommandHistory(
                id = ++historyCounter,
                command = command,
                timestamp = System.currentTimeMillis(),
                exitCode = completed,
                output = output
            )

            addToHistory(historyItem)

            val resultState = CommandState.Completed(
                command = command,
                exitCode = completed,
                output = output,
                duration = duration
            )

            _commandState.value = resultState
            resultState
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed", e)
            val errorState = CommandState.Error("Command failed: ${e.message}", e)
            _commandState.value = errorState
            errorState
        } finally {
            currentProcess = null
        }
    }

    suspend fun executeInLinuxSubsystem(
        command: String,
        timeoutMs: Long = 30000L
    ): CommandState = withContext(Dispatchers.IO) {
        try {
            _commandState.value = CommandState.Running(command)
            _commandOutput.value = ""

            val bootstrapDir = File(context.getExternalFilesDir(null), "bootstrap")
            val prootPath = File(bootstrapDir, "usr/bin/proot")

            if (!prootPath.exists()) {
                val errorState = CommandState.Error("Linux subsystem not installed")
                _commandState.value = errorState
                return@withContext errorState
            }

            val startTime = System.currentTimeMillis()
            val outputBuilder = StringBuilder()

            val processBuilder = ProcessBuilder(
                prootPath.absolutePath,
                "-0",
                "-r", bootstrapDir.absolutePath,
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sys",
                "-b", "${bootstrapDir.absolutePath}:/root",
                "/bin/bash", "-c", command
            )

            processBuilder.redirectErrorStream(true)

            currentProcess = processBuilder.start()

            val reader = currentProcess!!.inputStream.bufferedReader()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    outputBuilder.append(it).append("\n")
                    _commandOutput.value = outputBuilder.toString()
                }
            }

            val completed = currentProcess!!.waitFor()
            val duration = System.currentTimeMillis() - startTime

            val output = outputBuilder.toString()
            val historyItem = CommandHistory(
                id = ++historyCounter,
                command = command,
                timestamp = System.currentTimeMillis(),
                exitCode = completed,
                output = output
            )

            addToHistory(historyItem)

            val resultState = CommandState.Completed(
                command = command,
                exitCode = completed,
                output = output,
                duration = duration
            )

            _commandState.value = resultState
            resultState
        } catch (e: Exception) {
            Log.e(TAG, "Linux command execution failed", e)
            val errorState = CommandState.Error("Linux command failed: ${e.message}", e)
            _commandState.value = errorState
            errorState
        } finally {
            currentProcess = null
        }
    }

    fun stopCurrentCommand(): Boolean {
        return try {
            currentProcess?.destroy()
            currentProcess = null
            _commandState.value = CommandState.Idle
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop command", e)
            false
        }
    }

    private fun addToHistory(item: CommandHistory) {
        val history = _commandHistory.value.toMutableList()
        history.add(0, item)
        if (history.size > MAX_HISTORY) {
            history.removeAt(history.size - 1)
        }
        _commandHistory.value = history
    }

    fun clearHistory() {
        _commandHistory.value = emptyList()
    }

    fun clearOutput() {
        _commandOutput.value = ""
    }

    fun formatDuration(ms: Long): String {
        return when {
            ms < 1000 -> "${ms}ms"
            ms < 60000 -> "${ms / 1000}.${(ms % 1000) / 100}s"
            else -> "${ms / 60000}m ${(ms % 60000) / 1000}s"
        }
    }

    fun isRunning(): Boolean {
        return _commandState.value is CommandState.Running
    }

    suspend fun ping(host: String, count: Int = 4): CommandState {
        val command = if (System.getProperty("os.name").lowercase().contains("win")) {
            "ping -n $count $host"
        } else {
            "ping -c $count $host"
        }
        return executeCommand(command)
    }

    suspend fun listProcesses(): CommandState {
        val command = if (System.getProperty("os.name").lowercase().contains("win")) {
            "tasklist"
        } else {
            "ps aux"
        }
        return executeCommand(command)
    }

    suspend fun diskUsage(path: String? = null): CommandState {
        val command = if (System.getProperty("os.name").lowercase().contains("win")) {
            "dir ${path ?: ""}"
        } else {
            "df -h ${path ?: ""}"
        }
        return executeCommand(command)
    }

    suspend fun getSystemInfo(): CommandState {
        val command = if (System.getProperty("os.name").lowercase().contains("win")) {
            "systeminfo"
        } else {
            "uname -a"
        }
        return executeCommand(command)
    }
}
