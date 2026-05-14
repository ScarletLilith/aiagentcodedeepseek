# AgentMemory Linux 子系统准备指南

## 🚨 重要说明

这个应用的核心功能是 **Linux 子系统 + MemPalace**，没有这个子系统，应用功能会大大降低。

## 获取 Linux 子系统文件

### 方案1：在 Termux 中导出（推荐）

如果你有 Termux 账号或能访问 Termux：

```bash
# 在 Termux 中运行
pkg update
pkg install proot
termux-setup-storage

# 创建 bootstrap
cd ~
tar -cvf bootstrap-aarch64.tar usr/
gzip bootstrap-aarch64.tar

# 使用 ADB 复制到电脑
adb push ~/bootstrap-aarch64.tar.gz /path/to/save/

# 或者在 Termux 中启动 HTTP 服务器
python -m http.server 8080
# 然后在电脑浏览器访问 Termux IP 下载
```

### 方案2：使用预构建的 Bootstrap

从这些地方下载：

1. **GitHub - AndronixOrigin**
   ```
   https://github.com/AndronixApp/AndronixOrigin/raw/master/UTERM/bootstrap-aarch64.zip
   ```

2. **GitHub - PRoot**
   ```
   https://github.com/termux/proot/releases
   ```

3. **GitHub - Termux Bootstrap**
   ```
   https://github.com/termux/termux-packages/releases
   ```

### 方案3：使用国内镜像

如果 GitHub 访问困难：

```bash
# 使用 Gitee 镜像
git clone https://gitee.com/mirrors/termux-packages.git
cd termux-packages
# 查找 bootstrap 文件
```

### 方案4：在 Android 手机上提取

如果你有一台已 root 的 Android 手机：

```bash
# 在 Termux 中
cd ~
# 打包
tar -czvf /sdcard/bootstrap-aarch64.tar.gz usr/
# 通过文件管理器复制到电脑
```

## 放置文件

下载后，将 `bootstrap-aarch64.zip` 放到：

```
AgentMemory/
└── app/
    └── src/
        └── main/
            └── assets/
                └── bootstrap-aarch64.zip  ← 放这里
```

## 验证

```bash
# 检查文件
unzip -l app/src/main/assets/bootstrap-aarch64.zip | head -20
```

## 重新编译

```bash
cd ~/aiagentcodedeepseek
bash build_termux.sh
```

## 文件大小参考

- **bootstrap-aarch64.zip**: 100-200 MB
- **解压后**: 300-500 MB

## 获取帮助

如果无法获取 bootstrap 文件：

1. 加入 Termux Telegram 群组
2. 使用科学上网工具
3. 从已安装 Termux 的朋友那里复制
4. 使用模拟器（如 BlueStacks）中的 Termux

## 技术支持

如果以上方法都无法获取文件，请：
1. 在 GitHub 提 Issue
2. 提供你的网络环境信息
3. 我会尝试提供其他解决方案
