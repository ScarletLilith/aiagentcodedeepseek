cmake_minimum_required(VERSION 3.22.1)
project("agentmemory")

# Add native library
add_library(
    agent_memory_client
    SHARED
    mcp_client.cpp
)

# Find required packages
find_library(
    log-lib
    log
)

# Link libraries
target_link_libraries(
    agent_memory_client
    ${log-lib}
)

# Include curl and other headers if available
target_include_directories(
    agent_memory_client
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)
