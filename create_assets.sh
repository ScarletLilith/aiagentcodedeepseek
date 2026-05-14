#!/bin/bash
# 创建 Linux 子系统 assets 的脚本
# 在有网络的电脑上运行此脚本

set -e

echo "========================================="
echo "  AgentMemory - Assets 打包脚本"
echo "========================================="

# 创建临时目录
TEMP_DIR=$(mktemp -d)
ASSETS_DIR="$TEMP_DIR/assets"

echo "创建目录结构..."
mkdir -p "$ASSETS_DIR"

cd "$TEMP_DIR"

echo ""
echo "下载 PRoot Bootstrap (aarch64)..."
# 下载 PRoot for Android ARM64
wget -O bootstrap-aarch64.zip \
    "https://github.com/termux/proot/releases/download/v5.4.0/bootstrap-aarch64.zip" \
    || wget -O bootstrap-aarch64.zip \
    "https://github.com/AndronixApp/AndronixOrigin/raw/master/UTERM/bootstrap-aarch64.zip"

echo "下载 Server Bundle (MemPalace)..."
# 创建一个包含 MemPalace 的基础包
# 这里你需要根据实际情况下载 MemPalace 或创建自己的包
mkdir -p server_bundle
cd server_bundle

# MemPalace 基础结构
mkdir -p root/.mempalace
mkdir -p root/palace
mkdir -p usr/bin
mkdir -p usr/lib
mkdir -p etc

# 创建 palace-daemon 占位符（实际需要在运行时生成）
cat > root/palace-daemon.sh << 'DAEMON_EOF'
#!/bin/bash
# Palace Daemon - 占位脚本
# 实际 daemon 会在首次运行时下载
echo "Initializing MemPalace..."
sleep 2
echo "Palace Daemon ready"
DAEMON_EOF
chmod +x root/palace-daemon.sh

# 创建安装脚本
cat > install.sh << 'INSTALL_EOF'
#!/bin/bash
echo "MemPalace Installation Script"
echo "This will be customized based on actual MemPalace requirements"
INSTALL_EOF
chmod +x install.sh

# 打包
cd "$TEMP_DIR"
zip -r server-bundle.zip server_bundle/

echo ""
echo "========================================="
echo "创建 assets 文件"
echo "========================================="

# 复制到正确位置
mkdir -p "$ASSETS_DIR"
cp bootstrap-aarch64.zip "$ASSETS_DIR/"
cp server-bundle.zip "$ASSETS_DIR/"

# 创建输出目录
OUTPUT_DIR="$HOME/AgentMemory-assets"
mkdir -p "$OUTPUT_DIR"
cp "$ASSETS_DIR"/*.zip "$OUTPUT_DIR/"

# 清理
rm -rf "$TEMP_DIR"

echo ""
echo "========================================="
echo "✅ Assets 创建完成！"
echo "========================================="
echo "文件位置: $OUTPUT_DIR"
echo ""
echo "请将以下文件复制到项目的 app/src/main/assets/ 目录："
echo "  - bootstrap-aarch64.zip"
echo "  - server-bundle.zip"
echo ""
echo "或者运行："
echo "  cp $OUTPUT_DIR/*.zip /path/to/AgentMemory/app/src/main/assets/"
