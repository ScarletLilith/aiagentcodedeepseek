# Termux 快速设置指南

## 一键安装依赖

```bash
# 更新包管理器
pkg update && pkg upgrade -y

# 安装必要工具
pkg install openjdk-21 gradle wget git -y

# 安装 Android SDK（可选）
pkg install android-sdk -y
```

## 快速编译

```bash
# 克隆项目
git clone https://github.com/ScarletLilith/aiagentcodedeepseek.git
cd aiagentcodedeepseek

# 运行编译脚本
bash build_termux.sh
```

## 手动编译步骤

### 1. 设置环境变量

```bash
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=/data/data/com.termux/files/usr/lib/android-sdk
export PATH=$JAVA_HOME/bin:$PATH
```

### 2. 生成 Gradle Wrapper

```bash
gradle wrapper --gradle-version 8.2
chmod +x gradlew
```

### 3. 编译

```bash
./gradlew assembleDebug --no-daemon
```

## 常见问题

### Q: 提示 JAVA_HOME 错误
```bash
# 查找 Java 路径
ls /data/data/com.termux/files/usr/lib/jvm/

# 设置正确的路径
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk
```

### Q: 提示 SDK 未找到
```bash
# 安装 Android SDK
pkg install android-sdk -y

# 或者创建 local.properties
echo "sdk.dir=/data/data/com.termux/files/usr/lib/android-sdk" > local.properties
```

### Q: 内存不足
```bash
# 限制 Gradle 内存使用
export GRADLE_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=256m"
./gradlew assembleDebug --no-daemon
```

### Q: 下载依赖慢
编辑 `settings.gradle.kts`，添加国内镜像：

```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    google()
    mavenCentral()
}
```

## APK 输出位置

编译成功后，APK 位于：
```
app/build/outputs/apk/debug/app-debug.apk
```

## 安装 APK

```bash
# 方法1：使用 adb（需要 root 或开发者模式）
adb install app/build/outputs/apk/debug/app-debug.apk

# 方法2：复制到下载目录，手动安装
cp app/build/outputs/apk/debug/app-debug.apk /storage/emulated/0/Download/
```

## 永久环境变量设置

将以下内容添加到 `~/.bashrc`：

```bash
echo 'export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk' >> ~/.bashrc
echo 'export ANDROID_HOME=/data/data/com.termux/files/usr/lib/android-sdk' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```
