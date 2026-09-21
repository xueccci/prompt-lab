# 提示词工坊 · Android APK

将本地网页版提示词优化器打包为 Android 应用（WebView 壳）。

## 应用信息
| 项 | 值 |
|---|---|
| 包名 | `com.promptlab.workshop` |
| 应用名 | 提示词工坊 |
| 最低系统 | Android 8.0 (API 26) |
| 网页 | `app/src/main/assets/index.html` |

## 一键编译（推荐）

本机若无 JDK/Android SDK，可运行脚本自动下载便携工具链并出包：

```powershell
cd "G:\提示词优化器\android-app"
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

成功后 APK 路径：

`app\build\outputs\apk\debug\app-debug.apk`

### 路径含中文时

Android Gradle 不支持工程路径中的非 ASCII 字符。请复制到英文路径再编译：

```powershell
# 将工程复制到英文路径（脚本/工具链可在原目录 tools\）
robocopy "G:\提示词优化器\android-app" "C:\Temp\promptlab-android" /E /XD tools .gradle app\build
Copy-Item "G:\提示词优化器\提示词优化器.html" "C:\Temp\promptlab-android\app\src\main\assets\index.html" -Force

# 使用原目录的 JDK/SDK/Gradle 编译
$env:JAVA_HOME = "G:\提示词优化器\android-app\tools\jdk\jdk-17.0.20.1+1"
$env:ANDROID_HOME = "G:\提示词优化器\android-app\tools\android-sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Push-Location "C:\Temp\promptlab-android"
& "G:\提示词优化器\android-app\tools\gradle-8.2\bin\gradle.bat" --no-daemon assembleDebug
Pop-Location
Copy-Item "C:\Temp\promptlab-android\app\build\outputs\apk\debug\app-debug.apk" "G:\提示词优化器\提示词工坊-v1.0-debug.apk" -Force
```

本机已成功打包示例 APK：

`G:\提示词优化器\提示词工坊-v1.0-debug.apk`

## 手动编译（已有 Android Studio / SDK）

1. 用 Android Studio 打开 `G:\提示词优化器\android-app`
2. 等待 Gradle Sync
3. `Build → Build App Bundles / APKs → Build APK`
4. 或命令行：`gradlew assembleDebug`

## 功能说明
- **普通优化 / 精简 / 扩写**：本地规则引擎完整可用
- **云端 API（DeepSeek 等）**：需手机能访问互联网
- **小模型 WebLLM**：依赖 **WebGPU**，部分 Android WebView/机型不支持，失败会自动回退本地
- **Ollama**：需电脑与手机同一网络，且服务端允许跨域

## 更新网页
用新版 HTML 覆盖：

`app\src\main\assets\index.html`

（与网页端主文件保持一致：`G:\提示词优化器\提示词优化器.html`）

然后在**英文路径**下重新执行 `build-apk.ps1`。

当前资产版本：与网页端 UI 优化同步（二级浮层下拉、滚动条、输出面板头、诊断默认收起、设置小模型/通用开关等）。

## 签名
`app-debug.apk` 为 debug 签名，可直接安装（需允许「未知来源」）。上架需自行配置 release 签名。
