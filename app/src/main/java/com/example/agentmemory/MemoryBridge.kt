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

    // MemPalace-specific methods
    fun listWings(serviceUrl: String, apiKey: String): String {
        return callMCPTool("mempalace_list_wings", "{}", serviceUrl, apiKey)
    }

    fun listRooms(wing: String, serviceUrl: String, apiKey: String): String {
        return callMCPTool(
            "mempalace_list_rooms",
            "{\"wing\":\"${wing}\"}",
            serviceUrl,
            apiKey
        )
    }

    fun getDrawer(drawerId: String, serviceUrl: String, apiKey: String): String {
        return callMCPTool(
            "mempalace_get_drawer",
            "{\"drawer_id\":\"${drawerId}\"}",
            serviceUrl,
            apiKey
        )
    }

    fun mineContent(
        path: String,
        wing: String,
        serviceUrl: String,
        apiKey: String
    ): String {
        return callMCPTool(
            "mempalace_mine",
            "{\"path\":\"${path}\",\"wing\":\"${wing}\"}",
            serviceUrl,
            apiKey
        )
    }

    fun addAgent(
        name: String,
        description: String,
        serviceUrl: String,
        apiKey: String
    ): String {
        return callMCPTool(
            "mempalace_add_agent",
            "{\"name\":\"${name}\",\"description\":\"${description}\"}",
            serviceUrl,
            apiKey
        )
    }

    fun writeDiary(
        agent: String,
        content: String,
        serviceUrl: String,
        apiKey: String
    ): String {
        return callMCPTool(
            "mempalace_write_diary",
            "{\"agent\":\"${agent}\",\"content\":\"${content}\"}",
            serviceUrl,
            apiKey
        )
    }

    fun listAgents(serviceUrl: String, apiKey: String): String {
        return callMCPTool("mempalace_list_agents", "{}", serviceUrl, apiKey)
    }
}
