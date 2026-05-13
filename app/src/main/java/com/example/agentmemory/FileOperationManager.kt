package com.example.agentmemory

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class FileOperationManager(private val context: Context) {

    sealed class FileItem {
        data class Directory(val path: String, val name: String, val childCount: Int) : FileItem()
        data class FileInfo(
            val path: String,
            val name: String,
            val size: Long,
            val lastModified: Long,
            val extension: String
        ) : FileItem()
    }

    sealed class OperationResult {
        data class Success(val message: String, val data: Any? = null) : OperationResult()
        data class Error(val message: String, val exception: Throwable? = null) : OperationResult()
    }

    private val _currentPath = MutableStateFlow(getRootPath())
    val currentPath: StateFlow<String> = _currentPath

    private val _fileList = MutableStateFlow<List<FileItem>>(emptyList())
    val fileList: StateFlow<List<FileItem>> = _fileList

    private fun getRootPath(): String {
        return context.getExternalFilesDir(null)?.absolutePath ?: "/"
    }

    fun getParentPath(path: String): String? {
        val file = File(path)
        return if (file.parentFile?.exists() == true) file.parent else null
    }

    suspend fun listFiles(path: String): OperationResult = withContext(Dispatchers.IO) {
        try {
            val directory = File(path)
            if (!directory.exists() || !directory.isDirectory) {
                return@withContext OperationResult.Error("Directory not found: $path")
            }

            _currentPath.value = path

            val items = directory.listFiles()?.mapNotNull { file ->
                when {
                    file.isDirectory -> {
                        val childCount = file.listFiles()?.size ?: 0
                        FileItem.Directory(file.absolutePath, file.name, childCount)
                    }
                    file.isFile -> {
                        val extension = file.extension.lowercase()
                        FileItem.FileInfo(
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            extension = extension
                        )
                    }
                    else -> null
                }
            }?.sortedWith(compareBy(
                { it !is FileItem.Directory },
                { it.name.lowercase() }
            )) ?: emptyList()

            _fileList.value = items
            OperationResult.Success("Files listed successfully", items)
        } catch (e: Exception) {
            OperationResult.Error("Failed to list files: ${e.message}", e)
        }
    }

    suspend fun readFile(path: String): OperationResult = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                return@withContext OperationResult.Error("File not found: $path")
            }

            val content = file.readText(Charsets.UTF_8)
            OperationResult.Success("File read successfully", content)
        } catch (e: Exception) {
            OperationResult.Error("Failed to read file: ${e.message}", e)
        }
    }

    suspend fun writeFile(path: String, content: String, append: Boolean = false): OperationResult = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()

            if (append) {
                file.appendText(content, Charsets.UTF_8)
            } else {
                file.writeText(content, Charsets.UTF_8)
            }

            val directory = file.parentFile?.absolutePath ?: _currentPath.value
            listFiles(directory)

            OperationResult.Success("File written successfully")
        } catch (e: Exception) {
            OperationResult.Error("Failed to write file: ${e.message}", e)
        }
    }

    suspend fun createDirectory(path: String): OperationResult = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                return@withContext OperationResult.Error("Directory already exists: $path")
            }

            file.mkdirs()
            val parentPath = file.parentFile?.absolutePath ?: _currentPath.value
            listFiles(parentPath)

            OperationResult.Success("Directory created successfully")
        } catch (e: Exception) {
            OperationResult.Error("Failed to create directory: ${e.message}", e)
        }
    }

    suspend fun delete(path: String): OperationResult = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext OperationResult.Error("File or directory not found: $path")
            }

            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }

            val parentPath = file.parentFile?.absolutePath ?: _currentPath.value
            listFiles(parentPath)

            OperationResult.Success("Deleted successfully")
        } catch (e: Exception) {
            OperationResult.Error("Failed to delete: ${e.message}", e)
        }
    }

    suspend fun copy(sourcePath: String, destPath: String): OperationResult = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val dest = File(destPath)

            if (!source.exists()) {
                return@withContext OperationResult.Error("Source not found: $sourcePath")
            }

            dest.parentFile?.mkdirs()

            if (source.isDirectory) {
                copyDirectory(source, dest)
            } else {
                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            val parentPath = dest.parentFile?.absolutePath ?: _currentPath.value
            listFiles(parentPath)

            OperationResult.Success("Copied successfully")
        } catch (e: Exception) {
            OperationResult.Error("Failed to copy: ${e.message}", e)
        }
    }

    private fun copyDirectory(source: File, dest: File) {
        dest.mkdirs()
        source.listFiles()?.forEach { file ->
            val destFile = File(dest, file.name)
            if (file.isDirectory) {
                copyDirectory(file, destFile)
            } else {
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    suspend fun move(sourcePath: String, destPath: String): OperationResult = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val dest = File(destPath)

            if (!source.exists()) {
                return@withContext OperationResult.Error("Source not found: $sourcePath")
            }

            dest.parentFile?.mkdirs()
            Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)

            val sourceParent = source.parentFile?.absolutePath ?: _currentPath.value
            val destParent = dest.parentFile?.absolutePath ?: _currentPath.value

            if (sourceParent != destParent) {
                listFiles(sourceParent)
            }
            listFiles(destParent)

            OperationResult.Success("Moved successfully")
        } catch (e: Exception) {
            OperationResult.Error("Failed to move: ${e.message}", e)
        }
    }

    suspend fun rename(sourcePath: String, newName: String): OperationResult = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            if (!source.exists()) {
                return@withContext OperationResult.Error("Source not found: $sourcePath")
            }

            val dest = File(source.parentFile, newName)
            if (dest.exists()) {
                return@withContext OperationResult.Error("Destination already exists")
            }

            source.renameTo(dest)

            val parentPath = source.parentFile?.absolutePath ?: _currentPath.value
            listFiles(parentPath)

            OperationResult.Success("Renamed successfully")
        } catch (e: Exception) {
            OperationResult.Error("Failed to rename: ${e.message}", e)
        }
    }

    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    fun isTextFile(path: String): Boolean {
        val textExtensions = setOf(
            "txt", "md", "json", "xml", "html", "css", "js", "ts", "py", "kt", "java",
            "c", "cpp", "h", "hpp", "sh", "bat", "yaml", "yml", "toml", "ini", "cfg",
            "log", "csv", "sql", "gradle", "properties", "gitignore", "dockerfile"
        )
        val extension = File(path).extension.lowercase()
        return extension in textExtensions
    }

    fun getFileIcon(fileItem: FileItem): String {
        return when (fileItem) {
            is FileItem.Directory -> "📁"
            is FileItem.FileInfo -> when (fileItem.extension) {
                "txt", "md" -> "📄"
                "json", "xml", "yaml", "yml", "toml" -> "📋"
                "kt", "java", "py", "js", "ts", "c", "cpp", "h", "hpp" -> "💻"
                "html", "css" -> "🌐"
                "sh", "bat" -> "⚙️"
                "png", "jpg", "jpeg", "gif", "webp", "svg" -> "🖼️"
                "mp3", "wav", "flac" -> "🎵"
                "mp4", "avi", "mkv" -> "🎬"
                "zip", "tar", "gz", "rar" -> "📦"
                "pdf" -> "📕"
                else -> "📄"
            }
        }
    }
}
