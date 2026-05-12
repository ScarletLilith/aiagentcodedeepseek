# AgentMemory

Android app with AI memory system using MemPalace.

## Project Structure

- `/app` - Main Android application
  - `/src/main/java/com/example/agentmemory` - Kotlin source code
    - `MainActivity.kt` - Main activity with navigation
    - `MemoryBridge.kt` - JNI bridge to native code
    - `SettingsViewModel.kt` - Settings and data management
    - `SettingsScreen.kt` - UI for settings
    - `VerifyScreen.kt` - UI for verification
    - `EditorScreen.kt` - Code editor
    - `RulesScreen.kt` - Rules management
    - `LinuxSubsystemManager.kt` - Linux subsystem management
  - `/src/main/cpp` - Native C++ code
    - `mcp_client.cpp` - MCP client implementation
  - `/src/main/assets` - Assets (bootstrap-aarch64.zip, server-bundle.zip)
  - `/src/main/res` - Resources

## Setup Instructions

### 1. Prerequisites

- Android Studio Hedgehog or later
- Android NDK (for C++ build)
- Android SDK API 26 or later
- Gradle 8.2.1 or later

### 2. Required Assets

Add the following to `app/src/main/assets/`:
- `bootstrap-aarch64.zip` - ARM64 bootstrap Linux filesystem
- `server-bundle.zip` - Server bundle with MemPalace and palace-daemon

### 3. Dependencies

- Jetpack Compose
- DataStore Preferences
- Kotlin Coroutines
- kotlinx.serialization
- sora-editor
- libcurl (for C++)
- nlohmann/json (for C++)

### 4. Build

1. Open project in Android Studio
2. Sync Gradle
3. Build project

### 5. Run

- Connect ARM64 device or use ARM64 emulator
- Run the app

## Features

- Model configuration (API URL, key, temperature, etc.)
- MCP connection settings
- Custom Python tools management
- Skills management
- Knowledge bases
- Rules engine (global, project, dynamic)
- Config export/import
- Verification interface
- Linux subsystem management
- Code editor (sora-editor) with Python support

## Architecture

```
┌─────────────────────────────────────────┐
│     UI (Jetpack Compose)                │
├─────────────────────────────────────────┤
│  Settings | Verify | Editor | Rules     │
└──────────┬──────────────────────────────┘
           │
┌──────────▼──────────────────────────────┐
│  Kotlin Layer                          │
│  - SettingsViewModel                  │
│  - MemoryBridge                       │
│  - LinuxSubsystemManager              │
└──────────┬──────────────────────────────┘
           │
┌──────────▼──────────────────────────────┐
│  NDK Layer (C++)                        │
│  - MCP Client                          │
│  - libcurl + JSON-RPC                 │
└──────────┬──────────────────────────────┘
           │
┌──────────▼──────────────────────────────┐
│  Linux Subsystem (PRoot)                │
│  - MemPalace                           │
│  - palace-daemon (port 18989)          │
└─────────────────────────────────────────┘
```

## License

MIT License
