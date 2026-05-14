#!/bin/bash
# Termux 编译脚本 - AgentMemory 项目
# 使用方法: bash build_termux.sh

set -e

echo "========================================="
echo "  AgentMemory - Termux 编译脚本"
echo "========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Termux 环境
if [ -z "$PREFIX" ]; then
    echo -e "${RED}错误: 此脚本必须在 Termux 中运行${NC}"
    exit 1
fi

# 设置环境变量
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=/data/data/com.termux/files/usr/lib/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

echo -e "${YELLOW}环境变量设置:${NC}"
echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_HOME=$ANDROID_HOME"

# 检查 Java
echo -e "\n${YELLOW}检查 Java...${NC}"
if ! java -version 2>&1 | grep -q "21"; then
    echo -e "${RED}Java 21 未安装，正在安装...${NC}"
    pkg install openjdk-21 -y
fi
java -version

# 检查 Android SDK
echo -e "\n${YELLOW}检查 Android SDK...${NC}"
if [ ! -d "$ANDROID_HOME" ]; then
    echo -e "${RED}Android SDK 未安装，正在安装...${NC}"
    pkg install android-sdk -y
fi

# 检查必要的 SDK 组件
check_sdk_components() {
    echo -e "\n${YELLOW}检查 SDK 组件...${NC}"
    
    if [ ! -d "$ANDROID_HOME/platforms/android-34" ]; then
        echo "安装 Android 34 Platform..."
        sdkmanager "platforms;android-34" || true
    fi
    
    if [ ! -d "$ANDROID_HOME/build-tools/34.0.0" ]; then
        echo "安装 Build Tools 34.0.0..."
        sdkmanager "build-tools;34.0.0" || true
    fi
    
    if [ ! -d "$ANDROID_HOME/platform-tools" ]; then
        echo "安装 Platform Tools..."
        sdkmanager "platform-tools" || true
    fi
}

# 尝试检查 SDK 组件（可能需要先接受许可）
if command -v sdkmanager &> /dev/null; then
    check_sdk_components || echo -e "${YELLOW}SDK 组件检查跳过，将使用已有配置${NC}"
fi

# 检查 Gradle Wrapper
echo -e "\n${YELLOW}检查 Gradle Wrapper...${NC}"
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo -e "${YELLOW}Gradle Wrapper 不存在，正在生成...${NC}"
    mkdir -p gradle/wrapper
    
    # 使用系统 gradle 生成 wrapper
    if command -v gradle &> /dev/null; then
        gradle wrapper --gradle-version 8.2
    else
        echo -e "${RED}Gradle 未安装，正在安装...${NC}"
        pkg install gradle -y
        gradle wrapper --gradle-version 8.2
    fi
fi

# 赋予执行权限
chmod +x gradlew

# 创建 local.properties（如果不存在）
if [ ! -f "local.properties" ]; then
    echo -e "${YELLOW}创建 local.properties...${NC}"
    cat > local.properties << EOF
sdk.dir=$ANDROID_HOME
java.home=$JAVA_HOME
EOF
fi

# 开始编译
echo -e "\n${GREEN}开始编译 Debug 版本...${NC}"
echo "========================================="

# 编译命令
./gradlew assembleDebug --no-daemon --stacktrace

# 检查编译结果
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    APK_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    echo -e "\n${GREEN}=========================================${NC}"
    echo -e "${GREEN}编译成功！${NC}"
    echo -e "${GREEN}=========================================${NC}"
    echo -e "APK 位置: ${YELLOW}app/build/outputs/apk/debug/app-debug.apk${NC}"
    echo -e "APK 大小: ${YELLOW}$APK_SIZE${NC}"
    
    # 复制到共享存储（如果有权限）
    if [ -d "/storage/emulated/0/Download" ]; then
        cp app/build/outputs/apk/debug/app-debug.apk /storage/emulated/0/Download/AgentMemory-debug.apk
        echo -e "\n${GREEN}已复制到: /storage/emulated/0/Download/AgentMemory-debug.apk${NC}"
    fi
else
    echo -e "\n${RED}编译失败，请检查错误信息${NC}"
    exit 1
fi
