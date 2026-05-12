package com.example.agentmemory

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class LinuxSubsystemManager(private val context: Context) {

    sealed class InstallationState {
        object Idle : InstallationState()
        data class Extracting(val progress: Int) : InstallationState()
        data class Installing(val message: String) : InstallationState()
        object Complete : InstallationState()
        data class Error(val message: String) : InstallationState()
    }

    private val _installationState = MutableStateFlow<InstallationState>(InstallationState.Idle)
    val installationState: StateFlow<InstallationState> = _installationState

    private val bootstrapDir: File
        get() = File(context.getExternalFilesDir(null), "bootstrap")

    private val installedFlag: File
        get() = File(bootstrapDir, ".installed")

    suspend fun setupSubsystemIfNeeded() {
        if (installedFlag.exists()) {
            _installationState.value = InstallationState.Complete
            startDaemonIfNeeded()
            return
        }

        withContext(Dispatchers.IO) {
            try {
                _installationState.value = InstallationState.Extracting(0)
                extractAssets()
                _installationState.value = InstallationState.Extracting(100)

                _installationState.value = InstallationState.Installing("Running installation script...")
                runInstallationScript()

                installedFlag.createNewFile()
                _installationState.value = InstallationState.Complete

                startDaemon()
            } catch (e: Exception) {
                _installationState.value = InstallationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun extractAssets() {
        bootstrapDir.mkdirs()

        val bootstrapZip = context.assets.open("bootstrap-aarch64.zip")
        val serverBundleZip = context.assets.open("server-bundle.zip")

        extractZip(bootstrapZip, bootstrapDir, 0, 50)
        extractZip(serverBundleZip, bootstrapDir, 50, 100)
    }

    private fun extractZip(zipStream: java.io.InputStream, destDir: File, startProgress: Int, endProgress: Int) {
        ZipInputStream(zipStream).use { zis ->
            var entry = zis.nextEntry
            var count = 0
            val buffer = ByteArray(4096)

            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
                count++
            }
        }
    }

    private fun runInstallationScript() {
        val scriptFile = File(bootstrapDir, "install.sh")
        if (!scriptFile.exists()) {
            scriptFile.writeText("""
                #!/bin/bash
                apt-get update -y
                apt-get install -y python3 python3-pip
                pip3 install mempalace chromadb
                mempalace init ~/my_agent_memory
                echo "INSTALLATION_COMPLETE"
            """.trimIndent())
            scriptFile.setExecutable(true)
        }

        val prootPath = File(bootstrapDir, "usr/bin/proot").absolutePath
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

        process.inputStream.bufferedReader().forEachLine { line ->
            _installationState.value = InstallationState.Installing(line)
        }

        process.waitFor()
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
            "nohup palace-daemon --port 18989 > /dev/null 2>&1 &"
        )

        processBuilder.start()
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
                    "ps aux | grep -v grep | grep palace-daemon"
                ).start()

                val result = checkProcess.inputStream.bufferedReader().readText()
                if (result.isBlank()) {
                    startDaemon()
                }
            } catch (e: Exception) {
                startDaemon()
            }
        }
    }
}
