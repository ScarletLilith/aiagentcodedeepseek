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

# Try to find libcurl (can be system-provided or prebuilt)
# If curl is not available, we'll compile without it and use stub implementation
find_package(CURL QUIET)

if(CURL_FOUND)
    message(STATUS "Found libcurl: ${CURL_INCLUDE_DIRS}")
    target_include_directories(
        agent_memory_client
        PRIVATE
        ${CURL_INCLUDE_DIRS}
    )
    target_link_libraries(
        agent_memory_client
        ${log-lib}
        ${CURL_LIBRARIES}
    )
    target_compile_definitions(
        agent_memory_client
        PRIVATE
        HAS_CURL=1
    )
else()
    message(STATUS "libcurl not found, compiling without HTTP support")
    target_link_libraries(
        agent_memory_client
        ${log-lib}
    )
endif()

# Include directories
target_include_directories(
    agent_memory_client
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Set C++ standard
set_target_properties(
    agent_memory_client
    PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED YES
)
