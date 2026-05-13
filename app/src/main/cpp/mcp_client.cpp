#include <jni.h>
#include <string>
#include <android/log.h>
#include <sstream>
#include <vector>

#define LOG_TAG "AgentMemory"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef HAS_CURL
#include <curl/curl.h>
#endif

// Try to include nlohmann/json
// If not available, use simple JSON construction
#ifdef NLOHMANN_JSON_HPP
#include "json.hpp"
using json = nlohmann::json;
#else
// Simple JSON helper functions
namespace simple_json {
    std::string escape_string(const std::string& s) {
        std::string result;
        for (char c : s) {
            switch (c) {
                case '"': result += "\\\""; break;
                case '\\': result += "\\\\"; break;
                case '\n': result += "\\n"; break;
                case '\r': result += "\\r"; break;
                case '\t': result += "\\t"; break;
                default: result += c;
            }
        }
        return result;
    }
}
#endif

// Helper to build JSON-RPC request
std::string build_jsonrpc_request(const std::string& method, const std::string& tool_name, const std::string& args = "{}") {
    std::ostringstream oss;
    oss << "{"
        << "\"jsonrpc\":\"2.0\","
        << "\"id\":1,"
        << "\"method\":\"" << simple_json::escape_string(method) << "\","
        << "\"params\":{"
        << "\"name\":\"" << simple_json::escape_string(tool_name) << "\","
        << "\"arguments\":" << args
        << "}"
        << "}";
    return oss.str();
}

// Callback for curl write
size_t write_callback(void* contents, size_t size, size_t nmemb, void* userp) {
    ((std::string*)userp)->append((char*)contents, size * nmemb);
    return size * nmemb;
}

// Perform HTTP request
std::string perform_http_request(const std::string& url, const std::string& api_key, const std::string& json_data) {
    std::string response;
    
#ifdef HAS_CURL
    CURL* curl = curl_easy_init();
    if (curl) {
        struct curl_slist* headers = nullptr;
        
        // Build headers
        headers = curl_slist_append(headers, "Content-Type: application/json");
        std::string auth_header = "Authorization: Bearer " + api_key;
        headers = curl_slist_append(headers, auth_header.c_str());
        
        // Set curl options
        curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, json_data.c_str());
        curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, json_data.length());
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_callback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, 30L);
        
        // Perform request
        CURLcode res = curl_easy_perform(curl);
        
        if (res != CURLE_OK) {
            response = "{\"error\":\"curl error: " + std::string(curl_easy_strerror(res)) + "\"}";
            LOGE("curl error: %s", curl_easy_strerror(res));
        }
        
        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
    } else {
        response = "{\"error\":\"Failed to initialize curl\"}";
    }
#else
    response = "{\"error\":\"libcurl not available. Please configure NDK with libcurl support.\"}";
    LOGD("libcurl not available, returning stub response");
#endif
    
    return response;
}

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_example_agentmemory_MemoryBridge_initializeMemory(
        JNIEnv *env,
        jobject /* this */,
        jstring serviceUrl,
        jstring apiKey) {
    
    const char *urlChars = env->GetStringUTFChars(serviceUrl, nullptr);
    const char *keyChars = env->GetStringUTFChars(apiKey, nullptr);
    
    std::string url_str(urlChars);
    std::string key_str(keyChars);
    
    LOGD("Initializing memory system at: %s", url_str.c_str());
    
    // Build JSON-RPC request for mempalace_wake_up
    std::string json_request = build_jsonrpc_request("tools/call", "mempalace_wake_up", "{}");
    LOGD("Request: %s", json_request.c_str());
    
    // Perform request
    std::string response = perform_http_request(url_str, key_str, json_request);
    
    env->ReleaseStringUTFChars(serviceUrl, urlChars);
    env->ReleaseStringUTFChars(apiKey, keyChars);
    
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_agentmemory_MemoryBridge_searchMemory(
        JNIEnv *env,
        jobject /* this */,
        jstring query,
        jstring serviceUrl,
        jstring apiKey) {
    
    const char *queryChars = env->GetStringUTFChars(query, nullptr);
    const char *urlChars = env->GetStringUTFChars(serviceUrl, nullptr);
    const char *keyChars = env->GetStringUTFChars(apiKey, nullptr);
    
    std::string query_str(queryChars);
    std::string url_str(urlChars);
    std::string key_str(keyChars);
    
    LOGD("Searching memory for: %s", query_str.c_str());
    
    // Build arguments
    std::ostringstream args_oss;
    args_oss << "{\"query\":\"" << simple_json::escape_string(query_str) << "\"}";
    
    // Build JSON-RPC request for mempalace_search
    std::string json_request = build_jsonrpc_request("tools/call", "mempalace_search", args_oss.str());
    LOGD("Request: %s", json_request.c_str());
    
    // Perform request
    std::string response = perform_http_request(url_str, key_str, json_request);
    
    env->ReleaseStringUTFChars(query, queryChars);
    env->ReleaseStringUTFChars(serviceUrl, urlChars);
    env->ReleaseStringUTFChars(apiKey, keyChars);
    
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_example_agentmemory_MemoryBridge_callMCPTool(
        JNIEnv *env,
        jobject /* this */,
        jstring toolName,
        jstring argumentsJson,
        jstring serviceUrl,
        jstring apiKey) {
    
    const char *toolChars = env->GetStringUTFChars(toolName, nullptr);
    const char *argsChars = env->GetStringUTFChars(argumentsJson, nullptr);
    const char *urlChars = env->GetStringUTFChars(serviceUrl, nullptr);
    const char *keyChars = env->GetStringUTFChars(apiKey, nullptr);
    
    std::string tool_str(toolChars);
    std::string args_str(argsChars);
    std::string url_str(urlChars);
    std::string key_str(keyChars);
    
    LOGD("Calling MCP tool: %s", tool_str.c_str());
    
    // Build JSON-RPC request
    std::string json_request = build_jsonrpc_request("tools/call", tool_str, args_str);
    LOGD("Request: %s", json_request.c_str());
    
    // Perform request
    std::string response = perform_http_request(url_str, key_str, json_request);
    
    env->ReleaseStringUTFChars(toolName, toolChars);
    env->ReleaseStringUTFChars(argumentsJson, argsChars);
    env->ReleaseStringUTFChars(serviceUrl, urlChars);
    env->ReleaseStringUTFChars(apiKey, keyChars);
    
    return env->NewStringUTF(response.c_str());
}

}
