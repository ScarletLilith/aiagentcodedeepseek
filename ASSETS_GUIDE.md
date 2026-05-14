# AgentMemory - Linux 子系统 Assets 打包指南

## 概述

这个项目需要 Linux 子系统来运行 MemPalace 和 palace-daemon。以下是准备 assets 文件的完整指南。

## 需要的文件

1. **bootstrap-aarch64.zip** (~100-200MB)
   - PRoot 引导系统 for ARM64 (Android)
   - 来源：Termux PRoot 项目

2. **server-bundle.zip** (~50-100MB)
   - MemPalace 和 palace-daemon
   - 需要自定义构建

## 方法1：下载 Termux PRoot Bootstrap（推荐）

```bash
#!/bin/bash
# 下载 PRoot Bootstrap for ARM64

ASSETS_DIR="./app/src/main/assets"
mkdir -p "$ASSETS_DIR"

echo "下载 Termux PRoot Bootstrap..."
wget -O "$ASSETS_DIR/bootstrap-aarch64.zip" \
    "https://github.com/termux/proot/releases/download/v5.4.0/bootstrap-aarch64.zip"

echo "下载完成！"
ls -lh "$ASSETS_DIR"
```

## 方法2：从已安装的 Termux 导出

如果你有已安装 Termux 的设备：

```bash
# 在 Termux 中运行
cd ~
# 打包 bootstrap
tar -cvf bootstrap-aarch64.tar usr/
gzip bootstrap-aarch64.tar
mv bootstrap-aarch64.tar.gz ~/bootstrap-aarch64.zip

# 复制到电脑
adb push ~/bootstrap-aarch64.zip /path/to/project/app/src/main/assets/
```

## 方法3：使用 Andronix（最简单）

Andronix 提供了预构建的 PRoot 包：

1. 下载 Andronix app
2. 选择 Ubuntu 或 Debian
3. 选择 ARM64 架构
4. 导出为 "PRoot" 格式
5. 解压得到 bootstrap 文件

下载链接：
- https://github.com/AndronixApp/AndronixOrigin
- 找到 bootstrap-aarch64.zip

## 方法4：手动构建 MemPalace Bundle

```bash
#!/bin/bash
# 创建 MemPalace server bundle

BUNDLE_DIR="/tmp/mempalace-bundle"
rm -rf "$BUNDLE_DIR"
mkdir -p "$BUNDLE_DIR/root/palace"
mkdir -p "$BUNDLE_DIR/usr/bin"
mkdir -p "$BUNDLE_DIR/etc"

# Palace Daemon 占位符
cat > "$BUNDLE_DIR/root/palace-daemon.py" << 'DAEMON_EOF'
#!/usr/bin/env python3
"""
Palace Daemon - MCP Gateway for MemPalace
Placeholder version - 实际版本需要从 MemPalace 项目获取
"""
import sys
import json

def main():
    print("Palace Daemon v0.1.0")
    print("Initializing MCP Gateway...")
    print("Daemon ready on port 18989")
    
    while True:
        try:
            line = input()
            if line.strip() == "quit":
                break
            print(json.dumps({"status": "ok", "message": "received"}))
        except EOFError:
            break

if __name__ == "__main__":
    main()
DAEMON_EOF
chmod +x "$BUNDLE_DIR/root/palace-daemon.py"

# 创建启动脚本
cat > "$BUNDLE_DIR/start-palace.sh" << 'START_EOF'
#!/bin/bash
cd /root
python3 palace-daemon.py &
echo $! > /tmp/palace-daemon.pid
START_EOF
chmod +x "$BUNDLE_DIR/start-palace.sh"

# 安装脚本
cat > "$BUNDLE_DIR/install.sh" << 'INSTALL_EOF'
#!/bin/bash
echo "MemPalace Installation"
pip3 install mempalace chromadb
echo "Installation complete"
INSTALL_EOF
chmod +x "$BUNDLE_DIR/install.sh"

# 打包
cd /tmp
zip -r server-bundle.zip mempalace-bundle/

echo "Server bundle created at /tmp/server-bundle.zip"
```

## 快速开始脚本

创建一个完整的打包脚本 `prepare_assets.sh`：

```bash
#!/bin/bash
# AgentMemory Assets 准备脚本

set -e

ASSETS_DIR="./app/src/main/assets"
mkdir -p "$ASSETS_DIR"

echo "========================================="
echo "  AgentMemory - Assets 准备"
echo "========================================="

# 1. 下载 PRoot Bootstrap
echo ""
echo "[1/2] 下载 PRoot Bootstrap..."
if [ ! -f "$ASSETS_DIR/bootstrap-aarch64.zip" ]; then
    wget -O "$ASSETS_DIR/bootstrap-aarch64.zip" \
        "https://github.com/AndronixApp/AndronixOrigin/raw/master/UTERM/bootstrap-aarch64.zip" \
        || wget -O "$ASSETS_DIR/bootstrap-aarch64.zip" \
        "https://github.com/termux/proot/releases/download/v5.4.0/bootstrap-aarch64.zip"
    
    echo "✓ Bootstrap 下载完成"
else
    echo "✓ Bootstrap 已存在"
fi

# 2. 创建 Server Bundle
echo ""
echo "[2/2] 创建 MemPalace Server Bundle..."
cat > "$ASSETS_DIR/server-bundle.zip" << 'BUNDLE_EOF'
# 这是一个占位符
# 实际使用时需要包含 MemPalace 和 palace-daemon
# 下载真实的 MemPalace: https://github.com/milla-jovovich/mempalace
BUNDLE_EOF

echo "✓ Server Bundle 占位符创建完成"
echo ""
echo "========================================="
echo "  准备完成！"
echo "========================================="
echo "Assets 位置: $ASSETS_DIR"
ls -lh "$ASSETS_DIR"
```

## 验证 Assets

打包完成后，运行：

```bash
# 检查文件
ls -lh app/src/main/assets/

# 验证 ZIP 文件
unzip -t app/src/main/assets/bootstrap-aarch64.zip | head -20
```

## 下一步

1. 确保 `bootstrap-aarch64.zip` 存在
2. 重新编译项目
3. 在设备上测试 Linux 子系统安装

## 注意事项

- ARM64 架构：使用 `aarch64` 版本
- ARM32 架构：使用 `arm` 版本（需要修改代码）
- x86 架构：模拟器使用，使用 `x86_64` 版本

## 获取帮助

如果下载失败：
1. 使用代理/VPN
2. 使用国内镜像
3. 从其他设备复制
