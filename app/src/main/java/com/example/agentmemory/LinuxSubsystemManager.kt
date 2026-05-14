package com.example.agentmemory

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

class LinuxSubsystemManager(private val context: Context) {

    companion object {
        private const val TAG = "LinuxSubsystem"
        
        // 镜像源列表（依次尝试）
        private val BOOTSTRAP_MIRRORS = listOf(
            // 国内镜像（优先）
            "https://ghproxy.com/https://github.com/AndronixApp/AndronixOrigin/raw/master/bootstrap-aarch64.zip",
            "https://ghproxy.net/https://github.com/AndronixApp/AndronixOrigin/raw/master/bootstrap-aarch64.zip",
            "https://mirror.ghproxy.com/https://github.com/AndronixApp/AndronixOrigin/raw/master/bootstrap-aarch64.zip",
            // GitHub 原始源
            "https://github.com/AndronixApp/AndronixOrigin/raw/master/bootstrap-aarch64.zip",
            "https://github.com/termux/termux-packages/releases/download/bootstrap-2024.01.22/bootstrap-aarch64.zip"
        )
    }

    sealed class InstallationState {
        object Idle : InstallationState()
        object Skipped : InstallationState()
        data class Downloading(val progress: Int, val message: String, val mirrorIndex: Int = 0) : InstallationState()
        data class Extracting(val progress: Int, val currentFile: String = "") : InstallationState()
        data class Installing(val progress: Int, val message: String, val details: List<String> = emptyList()) : InstallationState()
        object Complete : InstallationState()
        data class Error(val message: String, val details: String? = null) : InstallationState()
    }

    private val _installationState = MutableStateFlow<InstallationState>(InstallationState.Idle)
    val installationState: StateFlow<InstallationState> = _installationState

    private val bootstrapDir: File
        get() = File(context.getExternalFilesDir(null), "bootstrap")

    private val installedFlag: File
        get() = File(bootstrapDir, ".installed")

    private val bootstrapZipFile: File
        get() = File(context.filesDir, "bootstrap-aarch64.zip")

    suspend fun setupSubsystemIfNeeded() {
        if (installedFlag.exists()) {
            _installationState.value = InstallationState.Complete
            startDaemonIfNeeded()
            return
        }

        if (checkAssetsExist()) {
            Log.d(TAG, "Assets found, installing from assets")
            withContext(Dispatchers.IO) {
                try {
                    bootstrapDir.mkdirs()
                    
                    _installationState.value = InstallationState.Extracting(0, "准备解压...")
                    extractAssets()
                    _installationState.value = InstallationState.Extracting(100, "解压完成")

                    _installationState.value = InstallationState.Installing(0, "开始安装...", listOf("正在初始化 Linux 子系统..."))
                    runInstallationScript()

                    installedFlag.createNewFile()
                    _installationState.value = InstallationState.Complete

                    startDaemon()
                } catch (e: Exception) {
                    Log.e(TAG, "Installation from assets failed", e)
                    _installationState.value = InstallationState.Error(
                        message = "Assets 安装失败: ${e.message}",
                        details = e.stackTraceToString()
                    )
                }
            }
            return
        }

        // Assets 不存在，尝试下载
        Log.d(TAG, "Assets not found, attempting to download")
        downloadAndInstallSubsystem()
    }

    private suspend fun downloadAndInstallSubsystem() {
        withContext(Dispatchers.IO) {
            var lastError: Exception? = null
            
            for ((index, mirror) in BOOTSTRAP_MIRRORS.withIndex()) {
                try {
                    Log.d(TAG, "Trying mirror ${index + 1}/${BOOTSTRAP_MIRRORS.size}: $mirror")
                    downloadAndExtractBootstrap(mirror, index)
                    
                    _installationState.value = InstallationState.Installing(0, "开始安装...", listOf("正在初始化 Linux 子系统..."))
                    runInstallationScript()

                    installedFlag.createNewFile()
                    _installationState.value = InstallationState.Complete
                    startDaemon()
                    return@withContext
                } catch (e: Exception) {
                    Log.e(TAG, "Mirror $mirror failed", e)
                    lastError = e
                }
            }

            _installationState.value = InstallationState.Error(
                message = "所有镜像源都无法下载，请检查网络",
                details = lastError?.stackTraceToString()
            )
        }
    }

    private fun checkAssetsExist(): Boolean {
        return try {
            context.assets.open("bootstrap-aarch64.zip").use { true }
        } catch (e: Exception) {
            Log.d(TAG, "bootstrap-aarch64.zip not found in assets")
            false
        }
    }

    private fun extractAssets() {
        try {
            val bootstrapZip = context.assets.open("bootstrap-aarch64.zip")
            extractZip(bootstrapZip, bootstrapDir, 0, 100, "Bootstrap")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract bootstrap from assets", e)
            throw e
        }

        try {
            val serverBundleZip = context.assets.open("server-bundle.zip")
            extractZip(serverBundleZip, bootstrapDir, 50, 100, "Server Bundle")
        } catch (e: Exception) {
            Log.w(TAG, "server-bundle.zip not found in assets, skipping")
        }
    }

    private suspend fun downloadAndExtractBootstrap(mirrorUrl: String, mirrorIndex: Int) {
        withContext(Dispatchers.IO) {
            _installationState.value = InstallationState.Downloading(0, "正在下载 Linux 子系统 (镜像 ${mirrorIndex + 1})...", mirrorIndex)
            
            val url = URL(mirrorUrl)
            val connection = url.openConnection()
            connection.connect()
            
            val totalSize = connection.contentLength
            var downloadedSize = 0
            
            bootstrapZipFile.parentFile?.mkdirs()
            
            url.openStream().use { input ->
                FileOutputStream(bootstrapZipFile).use { output ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead
                        
                        val progress = if (totalSize > 0) {
                            (downloadedSize * 100 / totalSize)
                        } else {
                            -1
                        }
                        
                        _installationState.value = InstallationState.Downloading(
                            progress,
                            "下载中: ${formatSize(downloadedSize)} / ${formatSize(totalSize)} (镜像 ${mirrorIndex + 1})",
                            mirrorIndex
                        )
                    }
                }
            }
            
            _installationState.value = InstallationState.Downloading(100, "下载完成", mirrorIndex)
            
            bootstrapZipFile.inputStream().use { zipStream ->
                extractZip(zipStream, bootstrapDir, 0, 100, "Bootstrap")
            }
        }
    }

    private fun formatSize(bytes: Int): String {
        return if (bytes < 1024) {
            "$bytes B"
        } else if (bytes < 1024 * 1024) {
            "${bytes / 1024} KB"
        } else {
            "${bytes / (1024 * 1024)} MB"
        }
    }

    private fun extractZip(
        zipStream: java.io.InputStream,
        destDir: File,
        startProgress: Int,
        endProgress: Int,
        label: String
    ) {
        ZipInputStream(zipStream).use { zis ->
            val entries = mutableListOf<java.util.zip.ZipEntry>()
            var entry = zis.nextEntry
            
            while (entry != null) {
                entries.add(entry)
                entry = zis.nextEntry
            }
            
            val totalEntries = entries.size
            var processedEntries = 0
            
            zis.close()
            
            val zipSource = when {
                zipStream is ZipInputStream -> return@use
                else -> context.assets.open("bootstrap-aarch64.zip")
            }
            
            ZipInputStream(zipSource).use { zis2 ->
                var entry2 = zis2.nextEntry
                val buffer = ByteArray(4096)
                
                while (entry2 != null) {
                    val progress = startProgress + ((processedEntries * (endProgress - startProgress)) / totalEntries.coerceAtLeast(1))
                    _installationState.value = InstallationState.Extracting(
                        progress,
                        "解压 ${label}: ${entry2.name}"
                    )
                    
                    val file = File(destDir, entry2.name)
                    if (entry2.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            var len: Int
                            while (zis2.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    zis2.closeEntry()
                    entry2 = zis2.nextEntry
                    processedEntries++
                }
            }
        }
    }

    private fun runInstallationScript() {
        val scriptFile = File(bootstrapDir, "install.sh")
        if (!scriptFile.exists()) {
            scriptFile.writeText("""
                #!/bin/bash
                echo "UPDATE_SYSTEM"
                apt-get update -y 2>&1
                
                echo "INSTALL_DEPENDENCIES"
                apt-get install -y python3 python3-pip 2>&1
                
                echo "INSTALL_PYTHON_PACKAGES"
                pip3 install mempalace chromadb 2>&1
                
                echo "INIT_MEMPALACE"
                mempalace init ~/my_agent_memory 2>&1
                
                echo "INSTALLATION_COMPLETE"
            """.trimIndent())
            scriptFile.setExecutable(true)
        }

        val prootPath = File(bootstrapDir, "usr/bin/proot").absolutePath
        if (!File(prootPath).exists()) {
            Log.e(TAG, "PRoot not found at $prootPath")
            _installationState.value = InstallationState.Error(
                message = "PRoot 未找到，请确保 bootstrap 文件完整"
            )
            return
        }

        val processBuilder = ProcessBuilder(
            prootPath,
            "-0",
            "-r", bootstrapDir.absolutePath,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "${bootstrapDir.absolutePath}:/root",
            "/bin/bash", "/install.sh"
        )

        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()

        val outputLines = mutableListOf<String>()
        var currentStep = 0
        val steps = listOf(
            "UPDATE_SYSTEM" to 0,
            "INSTALL_DEPENDENCIES" to 25,
            "INSTALL_PYTHON_PACKAGES" to 50,
            "INIT_MEMPALACE" to 75,
            "INSTALLATION_COMPLETE" to 100
        )

        process.inputStream.bufferedReader().forEachLine { line ->
            Log.d(TAG, "Install: $line")
            outputLines.add(line)
            
            val matchedStep = steps.find { line.contains(it.first) }
            if (matchedStep != null) {
                currentStep = matchedStep.second
            }
            
            val displayMessage = when {
                line.contains("UPDATE_SYSTEM") -> "更新系统包..."
                line.contains("INSTALL_DEPENDENCIES") -> "安装系统依赖..."
                line.contains("INSTALL_PYTHON_PACKAGES") -> "安装 Python 包..."
                line.contains("INIT_MEMPALACE") -> "初始化 MemPalace..."
                line.contains("INSTALLATION_COMPLETE") -> "安装完成！"
                else -> line.take(100)
            }
            
            _installationState.value = InstallationState.Installing(
                currentStep.coerceIn(0, 99),
                displayMessage,
                outputLines.takeLast(5)
            )
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            _installationState.value = InstallationState.Error(
                message = "安装脚本执行失败 (退出码: $exitCode)",
                details = outputLines.joinToString("\n")
            )
        }
    }

    private fun startDaemon() {
        val prootPath = File(bootstrapDir, "usr/bin/proot").absolutePath
        val processBuilder = ProcessBuilder(
            prootPath,
            "-0",
            "-r", bootstrapDir.absolutePath,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "${bootstrapDir.absolutePath}:/root",
            "/bin/bash", "-c",
            "nohup palace-daemon --port 18989 > /root/daemon.log 2>&1 &"
        )

        try {
            processBuilder.start()
            Log.d(TAG, "Daemon started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start daemon", e)
        }
    }

    private suspend fun startDaemonIfNeeded() {
        withContext(Dispatchers.IO) {
            try {
                val prootPath = File(bootstrapDir, "usr/bin/proot").absolutePath
                val checkProcess = ProcessBuilder(
                    prootPath,
                    "-0",
                    "-r", bootstrapDir.absolutePath,
                    "/bin/bash", "-c",
                    "ps aux | grep -v grep | grep palace-daemon || echo 'NOT_RUNNING'"
                ).start()

                val result = checkProcess.inputStream.bufferedReader().readText()
                if (result.contains("NOT_RUNNING") || result.isBlank()) {
                    Log.d(TAG, "Daemon not running, starting...")
                    startDaemon()
                } else {
                    Log.d(TAG, "Daemon already running")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check daemon status", e)
                startDaemon()
            }
        }
    }

    fun isLinuxSubsystemAvailable(): Boolean {
        return checkAssetsExist() || bootstrapZipFile.exists()
    }
}
