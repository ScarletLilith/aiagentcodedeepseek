#!/bin/bash
# AgentMemory Linux 子系统 Bootstrap 下载脚本
# 使用多个国内镜像源，自动切换

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 目标目录
DEST_DIR="app/src/main/assets"
DEST_FILE="$DEST_DIR/bootstrap-aarch64.zip"

# 镜像源列表（依次尝试）
MIRRORS=(
    # 国内镜像（优先）
    "https://ghproxy.com/https://github.com/AndronixApp/AndronixOrigin/raw/master/bootstrap-aarch64.zip"
    "https://ghproxy.net/https://github.com/AndronixApp/AndronixOrigin/raw/master/bootstrap-aarch64.zip"
    "https://mirror.ghproxy.com/https://github.com/AndronixApp/AndronixOrigin/raw/master/bootstrap-aarch64.zip"
    # GitHub 原始源
    "https://github.com/AndronixApp/AndronixOrigin/raw/master/bootstrap-aarch64.zip"
    "https://github.com/termux/termux-packages/releases/download/bootstrap-2024.01.22/bootstrap-aarch64.zip"
)

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_banner() {
    echo ""
    echo "======================================"
    echo "  AgentMemory Bootstrap 下载工具"
    echo "======================================"
    echo ""
}

# 创建目标目录
mkdir -p "$DEST_DIR"

print_banner

# 检查是否已存在
if [ -f "$DEST_FILE" ]; then
    print_warning "bootstrap 已存在: $DEST_FILE"
    echo -n "是否重新下载？(y/N): "
    read -r answer
    if [ "$answer" != "y" ] && [ "$answer" != "Y" ]; then
        print_success "跳过下载"
        ls -lh "$DEST_FILE"
        exit 0
    fi
    rm -f "$DEST_FILE"
fi

# 尝试下载
for ((i=0; i<${#MIRRORS[@]}; i++)); do
    mirror=${MIRRORS[$i]}
    mirror_num=$((i + 1))
    
    print_info "尝试镜像 $mirror_num/${#MIRRORS[@]}: $mirror"
    
    if command -v wget &> /dev/null; then
        # 使用 wget
        if wget --timeout=30 --tries=2 -O "$DEST_FILE" "$mirror"; then
            print_success "下载成功！"
            break
        else
            print_warning "镜像 $mirror_num 下载失败"
            rm -f "$DEST_FILE" 2>/dev/null || true
            continue
        fi
    elif command -v curl &> /dev/null; then
        # 使用 curl
        if curl -L --max-time 30 --retry 2 -o "$DEST_FILE" "$mirror"; then
            print_success "下载成功！"
            break
        else
            print_warning "镜像 $mirror_num 下载失败"
            rm -f "$DEST_FILE" 2>/dev/null || true
            continue
        fi
    else
        print_error "未找到 wget 或 curl，请先安装其中一个"
        exit 1
    fi
done

# 检查是否下载成功
if [ ! -f "$DEST_FILE" ]; then
    print_error "所有镜像源都下载失败，请检查网络连接"
    echo ""
    echo "你也可以手动下载文件到：$DEST_DIR/"
    echo "下载地址：https://github.com/AndronixApp/AndronixOrigin/raw/master/bootstrap-aarch64.zip"
    exit 1
fi

# 验证文件
print_info "验证文件..."
ls -lh "$DEST_FILE"

# 尝试解压预览
if command -v unzip &> /dev/null; then
    echo ""
    print_info "解压预览（前20个文件）："
    unzip -l "$DEST_FILE" 2>/dev/null | head -30 || true
fi

print_success ""
print_success "======================================"
print_success "  Bootstrap 下载完成！"
print_success "======================================"
print_success "文件位置: $DEST_FILE"
print_success ""
print_success "现在可以运行 'bash build_termux.sh' 编译项目了"
