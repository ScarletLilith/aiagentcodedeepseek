package com.example.agentmemory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "agent_memory_settings")

@Serializable
data class ModelConfig(
    val apiUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val modelName: String = "gpt-4",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val topP: Float = 1.0f
)

@Serializable
data class McpConfig(
    val enabled: Boolean = true,
    val serviceUrl: String = "http://localhost:18989/mcp",
    val apiKey: String = ""
)

@Serializable
data class ToolParameter(
    val name: String,
    val type: String = "string",
    val description: String,
    val required: Boolean = false
)

@Serializable
data class ToolConfig(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter> = emptyList(),
    val code: String,
    val enabled: Boolean = true
)

@Serializable
data class SkillConfig(
    val name: String,
    val description: String,
    val enabled: Boolean = true
)

@Serializable
data class KnowledgeBaseConfig(
    val name: String,
    val path: String,
    val enabled: Boolean = true
)

@Serializable
data class RuleConfig(
    val id: String,
    val name: String,
    val description: String,
    val content: String,
    val type: String = "global",
    val enabled: Boolean = true
)

@Serializable
data class AppConfig(
    val model: ModelConfig = ModelConfig(),
    val mcp: McpConfig = McpConfig(),
    val tools: List<ToolConfig> = emptyList(),
    val skills: List<SkillConfig> = emptyList(),
    val knowledgeBases: List<KnowledgeBaseConfig> = emptyList(),
    val rules: List<RuleConfig> = emptyList()
)

class SettingsViewModel(private val context: Context) {

    private val json = Json { prettyPrint = true }

    private object PreferencesKeys {
        val MODEL_CONFIG = stringPreferencesKey("model_config")
        val MCP_CONFIG = stringPreferencesKey("mcp_config")
        val TOOLS_CONFIG = stringPreferencesKey("tools_config")
        val SKILLS_CONFIG = stringPreferencesKey("skills_config")
        val KNOWLEDGE_BASES_CONFIG = stringPreferencesKey("knowledge_bases_config")
        val RULES_CONFIG = stringPreferencesKey("rules_config")
    }

    val modelConfig: Flow<ModelConfig> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.MODEL_CONFIG]?.let {
                try {
                    json.decodeFromString<ModelConfig>(it)
                } catch (e: Exception) {
                    ModelConfig()
                }
            } ?: ModelConfig()
        }

    val mcpConfig: Flow<McpConfig> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.MCP_CONFIG]?.let {
                try {
                    json.decodeFromString<McpConfig>(it)
                } catch (e: Exception) {
                    McpConfig()
                }
            } ?: McpConfig()
        }

    val toolsConfig: Flow<List<ToolConfig>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TOOLS_CONFIG]?.let {
                try {
                    json.decodeFromString<List<ToolConfig>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    val skillsConfig: Flow<List<SkillConfig>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SKILLS_CONFIG]?.let {
                try {
                    json.decodeFromString<List<SkillConfig>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    val knowledgeBasesConfig: Flow<List<KnowledgeBaseConfig>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.KNOWLEDGE_BASES_CONFIG]?.let {
                try {
                    json.decodeFromString<List<KnowledgeBaseConfig>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    val rulesConfig: Flow<List<RuleConfig>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.RULES_CONFIG]?.let {
                try {
                    json.decodeFromString<List<RuleConfig>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun saveModelConfig(config: ModelConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MODEL_CONFIG] = json.encodeToString(config)
        }
    }

    suspend fun saveMcpConfig(config: McpConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MCP_CONFIG] = json.encodeToString(config)
        }
    }

    suspend fun saveToolsConfig(tools: List<ToolConfig>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOOLS_CONFIG] = json.encodeToString(tools)
        }
    }

    suspend fun saveSkillsConfig(skills: List<SkillConfig>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SKILLS_CONFIG] = json.encodeToString(skills)
        }
    }

    suspend fun saveKnowledgeBasesConfig(bases: List<KnowledgeBaseConfig>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KNOWLEDGE_BASES_CONFIG] = json.encodeToString(bases)
        }
    }

    suspend fun saveRulesConfig(rules: List<RuleConfig>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RULES_CONFIG] = json.encodeToString(rules)
        }
    }

    suspend fun exportConfig(outputStream: OutputStream): Boolean {
        return try {
            val model = modelConfig.map { it }.getOrNull() ?: ModelConfig()
            val mcp = mcpConfig.map { it }.getOrNull() ?: McpConfig()
            val tools = toolsConfig.map { it }.getOrNull() ?: emptyList()
            val skills = skillsConfig.map { it }.getOrNull() ?: emptyList()
            val knowledgeBases = knowledgeBasesConfig.map { it }.getOrNull() ?: emptyList()
            val rules = rulesConfig.map { it }.getOrNull() ?: emptyList()
            
            val config = AppConfig(model, mcp, tools, skills, knowledgeBases, rules)
            val jsonStr = json.encodeToString(config)
            outputStream.write(jsonStr.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importConfig(inputStream: InputStream): Boolean {
        return try {
            val jsonStr = inputStream.readBytes().toString(Charsets.UTF_8)
            val config = json.decodeFromString<AppConfig>(jsonStr)
            
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.MODEL_CONFIG] = json.encodeToString(config.model)
                preferences[PreferencesKeys.MCP_CONFIG] = json.encodeToString(config.mcp)
                preferences[PreferencesKeys.TOOLS_CONFIG] = json.encodeToString(config.tools)
                preferences[PreferencesKeys.SKILLS_CONFIG] = json.encodeToString(config.skills)
                preferences[PreferencesKeys.KNOWLEDGE_BASES_CONFIG] = json.encodeToString(config.knowledgeBases)
                preferences[PreferencesKeys.RULES_CONFIG] = json.encodeToString(config.rules)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Register a tool with palace-daemon
     */
    suspend fun registerTool(tool: ToolConfig): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val mcpConfig = mcpConfig.firstOrNull() ?: return@withContext Result.failure(Exception("MCP config not found"))
                
                // Calculate register tool URL (replace /mcp with /register_tool)
                val baseUrl = mcpConfig.serviceUrl.replace("/mcp", "")
                val registerUrl = URL("$baseUrl/register_tool")
                
                // Build request body
                val requestBody = json.encodeToString(
                    mapOf(
                        "name" to tool.name,
                        "description" to tool.description,
                        "parameters" to tool.parameters,
                        "code" to tool.code
                    )
                )
                
                // Make HTTP request
                val connection = registerUrl.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer ${mcpConfig.apiKey}")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 30000
                }
                
                // Send request
                connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }
                
                // Read response
                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                }
                
                connection.disconnect()
                
                if (responseCode in 200..299) {
                    Result.success(response)
                } else {
                    Result.failure(Exception("HTTP $responseCode: $response"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private suspend fun <T> Flow<T>.getOrNull(): T? {
        return try {
            this.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
