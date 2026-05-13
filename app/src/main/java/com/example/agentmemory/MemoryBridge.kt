package com.example.agentmemory

class MemoryBridge {
    companion object {
        init {
            System.loadLibrary("agent_memory_client")
        }
    }

    /**
     * Initialize memory system by calling mempalace_wake_up
     */
    external fun initializeMemory(serviceUrl: String, apiKey: String): String

    /**
     * Search memory using mempalace_search tool
     */
    external fun searchMemory(query: String, serviceUrl: String, apiKey: String): String

    /**
     * Call any MCP tool
     */
    external fun callMCPTool(
        toolName: String,
        argumentsJson: String,
        serviceUrl: String,
        apiKey: String
    ): String
}
