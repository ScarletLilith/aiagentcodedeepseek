package com.example.agentmemory

class MemoryBridge {
    companion object {
        init {
            System.loadLibrary("agent_memory_client")
        }
    }

    external fun initializeMemory(serviceUrl: String, apiKey: String): String
    external fun searchMemory(query: String): String
}
