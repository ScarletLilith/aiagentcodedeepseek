# MemPalace 集成说明

## 概述

MemPalace 是一个本地优先的 AI 记忆系统，提供：
- **层级记忆结构**：Wings（项目）→ Rooms（主题）→ Drawers（原始内容）
- **向量检索**：基于 ChromaDB，支持 96.6% R@5 准确率
- **时间线**：按时间顺序组织和检索记忆
- **MCP 协议**：29 个 MCP 工具用于记忆操作
- **零 API 依赖**：核心功能无需网络调用

## 集成架构

```
AgentMemory App
├── LinuxSubsystemManager
│   └── PRoot 环境
│       ├── MemPalace CLI
│       ├── palace-daemon (MCP Server)
│       └── ChromaDB
├── MemPalaceManager (Kotlin)
│   ├── mine() - 挖掘内容到记忆宫殿
│   ├── wakeUp() - 加载相关记忆
│   ├── search() - 语义搜索
│   └── manage() - 管理 wings/rooms/drawers
└── MemoryBridge (JNI)
    └── MCP 协议通信
```

## 使用方法

### 1. 初始化 MemPalace

```kotlin
val memPalaceManager = MemPalaceManager(context)
memPalaceManager.initialize()
```

### 2. 挖掘项目内容

```kotlin
val result = memPalaceManager.mineProject(
    projectPath = "/path/to/project",
    wing = "my-agent"  // 项目 wing
)
```

### 3. 唤醒记忆（获取上下文）

```kotlin
val context = memPalaceManager.wakeUp(
    query = "之前的数据库设计是怎样的？"
)
// 返回相关历史上下文用于喂给 LLM
```

### 4. 搜索记忆

```kotlin
val results = memPalaceManager.search(
    query = "用户认证实现",
    limit = 5
)
```

## CLI 命令

在 PRoot 环境中可以直接使用：

```bash
# 初始化
mempalace init ~/my_agent_memory

# 挖掘项目
mempalace mine ~/project --wing my-project

# 挖掘对话
mempalace mine ~/.claude/projects/ --mode convos --wing my-project

# 搜索
mempalace search "之前的实现方案"

# 唤醒
mempalace wake-up
```

## MCP 工具

palace-daemon 提供 29 个 MCP 工具：

- `mempalace_search` - 语义搜索
- `mempalace_wake_up` - 加载上下文
- `mempalace_list_wings` - 列出所有 wings
- `mempalace_list_rooms` - 列出房间
- `mempalace_get_drawer` - 获取抽屉内容
- `mempalace_mine` - 挖掘内容
- `mempalace_add_agent` - 添加智能体
- `mempalace_write_diary` - 写日记
- 等等...

## 数据存储

- 默认位置：`~/my_agent_memory`
- ChromaDB 数据：`~/.cache/mempalace/`
- 日志文件：`~/.cache/mempalace/mempalace.log`

## 依赖

- Python 3.9+
- ChromaDB >= 1.5.4
- ~300 MB 磁盘空间

## 更多信息

- 官网：https://mempalaceofficial.com
- GitHub：https://github.com/MemPalace/mempalace
- 文档：https://mempalaceofficial.com/guide/getting-started
