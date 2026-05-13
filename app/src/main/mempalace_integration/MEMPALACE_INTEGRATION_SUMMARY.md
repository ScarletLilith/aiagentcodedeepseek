# MemPalace 集成总结

## 概述

AgentMemory 项目已成功集成 MemPalace，实现了跨会话、跨天的上下文连续性管理。

## 核心组件

### 1. MemPalaceManager.kt
管理 MemPalace 的核心操作：
- `initialize()` - 初始化记忆宫殿
- `mineProject()` - 挖掘项目内容
- `wakeUp()` - 唤醒记忆获取上下文
- `search()` - 语义搜索
- `listWings()` / `listRooms()` - 管理层级结构
- `addAgent()` / `writeDiary()` - 智能体管理
- `getContextForLLM()` - 构建 LLM 上下文

### 2. ContextManager.kt
管理对话历史和上下文：
- `buildContext()` - 构建上下文
- `saveToHistory()` - 保存到历史
- `getContextForQuery()` - 获取查询相关上下文
- `getTimeline()` - 获取时间线
- `searchHistory()` - 搜索历史
- `exportContext()` - 导出上下文

### 3. MemoryBridge.kt
JNI 桥接到 NDK MCP 客户端：
- `listWings()` - 列出所有 wings
- `listRooms()` - 列出房间
- `getDrawer()` - 获取抽屉内容
- `mineContent()` - 挖掘内容
- `addAgent()` - 添加智能体
- `writeDiary()` - 写日记
- `listAgents()` - 列出智能体

### 4. MemPalaceScreen.kt
记忆宫殿管理 UI：
- 搜索记忆 Tab
- 记忆宫殿结构 Tab
- 智能体管理 Tab

## 工作流程

### 1. 首次使用
1. App 启动 → LinuxSubsystemManager 初始化 PRoot 环境
2. 安装 MemPalace 和 palace-daemon
3. MemPalaceManager 初始化记忆宫殿

### 2. 挖掘内容
1. 调用 `mineProject(projectPath, wing)`
2. palace-daemon 执行 `mempalace mine` 命令
3. 内容被存储到 ChromaDB

### 3. 上下文获取
1. 用户提问
2. `ContextManager.getContextForQuery()` 被调用
3. `MemPalaceManager.search()` 执行向量搜索
4. 返回相关记忆构建上下文
5. 上下文被喂给 LLM
6. 对话和响应被保存到历史

### 4. 跨会话连续性
1. 每次对话结束，`saveToHistory()` 保存到历史
2. 历史按时间线组织
3. 新会话可通过 `getTimeline()` 获取历史
4. 搜索历史找到相关上下文

## 层级结构

```
记忆宫殿 (Palace)
├── Wing 1 (项目 1)
│   ├── Room 1 (数据库)
│   │   ├── Drawer 1 (对话片段)
│   │   └── Drawer 2 (代码片段)
│   ├── Room 2 (API)
│   └── Room 3 (认证)
├── Wing 2 (项目 2)
└── Wing 3 (通用)
```

## 数据流

```
用户提问
    ↓
ContextManager.getContextForQuery()
    ↓
MemPalaceManager.search()
    ↓
MemoryBridge.callMCPTool("mempalace_search")
    ↓
NDK MCP Client
    ↓
palace-daemon (HTTP)
    ↓
MemPalace CLI
    ↓
ChromaDB (向量检索)
    ↓
返回相关记忆
    ↓
构建上下文文本
    ↓
喂给 LLM
    ↓
保存到历史
```

## 配置要求

### Android assets
确保以下文件在 `app/src/main/assets/`：
- `bootstrap-aarch64.zip` - ARM64 Linux 根文件系统
- `server-bundle.zip` - MemPalace + palace-daemon

### Linux 子系统安装
安装脚本自动执行：
1. 更新系统包
2. 安装 Python 和 pip
3. 安装 mempalace 和 chromadb
4. 初始化记忆宫殿
5. 启动 palace-daemon

## MCP 协议

MemPalace 提供 29 个 MCP 工具，AgentMemory 使用以下核心工具：

- `mempalace_wake_up` - 唤醒记忆
- `mempalace_search` - 语义搜索
- `mempalace_list_wings` - 列出项目
- `mempalace_list_rooms` - 列出房间
- `mempalace_get_drawer` - 获取内容
- `mempalace_mine` - 挖掘内容
- `mempalace_add_agent` - 添加智能体
- `mempalace_write_diary` - 写日记

## 优势

1. **零 API 依赖** - 核心功能无需网络调用
2. **层级结构** - Wings/Rooms/Drawers 组织记忆
3. **向量检索** - 96.6% R@5 准确率
4. **跨会话连续** - 时间线检索保证连续性
5. **无损存储** - 原文存储，不做摘要
6. **本地优先** - 数据留在设备上

## 使用示例

```kotlin
// 获取上下文
val context = contextManager.getContextForQuery(
    query = "之前的数据库设计是怎样的？",
    memPalaceManager = memPalaceManager,
    limit = 10
)

// 构建 LLM 请求
val request = LLMRequest(
    systemPrompt = context,
    userMessage = userMessage
)

// 发送给 LLM
val response = llmClient.send(request)

// 保存到历史
contextManager.saveToHistory(
    query = userMessage,
    memories = searchResults,
    response = response.content
)
```

## 更多信息

- MemPalace 官网: https://mempalaceofficial.com
- MemPalace GitHub: https://github.com/MemPalace/mempalace
- 文档: https://mempalaceofficial.com/guide/getting-started
