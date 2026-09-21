# 提示词工坊 · Prompt Lab

<p align="center">
  <img src="releases/提示词工坊-v1.4.0.apk" alt="v1.4.0" />
</p>

> 一款把口语化想法优化成 **Agent 可执行提示词** 的 Android 应用。  
> 支持普通优化 / 精简 / 扩写，可接云端 API、本机小模型，或纯本地规则。

---

## 功能特性

| 功能 | 说明 |
|------|------|
| **普通优化** | 按 Agent 结构改写：角色、任务、上下文、约束、输出格式 |
| **精简** | 从过长原文提取主干，去掉冗余 |
| **扩写** | 按需求逐项细化，补全可验收标准 |
| **三种引擎** | 云端 API（DeepSeek 等）· 浏览器小模型（WebLLM）· 本地多遍改写 |
| **Agent 优化层** | 优化过程可视化：角色 / 任务 / 上下文 / 约束 / 输出格式 / 过程自检 |
| **诊断面板** | 六维评分、模糊词检测、改写流水线说明（默认可收起） |
| **自循环** | 生成 → 批评 → 修订，结果更稳（可开关） |
| **主题** | 浅色 / 深色，与系统状态栏、底栏同步 |
| **设置** | API 密钥、小模型、通用偏好；「关于开发者」 |

---

## 成品下载

| 版本 | 文件 |
|------|------|
| **v1.4.0** | [`releases/提示词工坊-v1.4.0.apk`](releases/提示词工坊-v1.4.0.apk) |

**安装：** 手机允许「未知来源 / 安装未知应用」后，打开 APK 安装即可。

- 包名：`com.promptlab.workshop`
- 最低系统：Android 8.0（API 26）
- 签名：debug（本地调试用）

---

## 仓库结构

```
.
├── README.md
├── releases/
│   └── 提示词工坊-v1.4.0.apk      # 发布安装包
└── android/                        # 安卓源码
    ├── app/
    │   └── src/main/
    │       ├── java/.../MainActivity.java
    │       ├── res/                # 图标、布局、主题
    │       └── assets/             # （HTML 页面未包含在本仓库）
    ├── build-apk.ps1               # 一键编译脚本
    ├── build.gradle
    └── ...
```

> **说明：** 按发布要求，本仓库 **不包含** WebView 页面 `index.html`。  
> 如需自行打包，请将网页版 HTML 放入 `android/app/src/main/assets/index.html` 后重新编译。

---

## 引擎说明

| 引擎 | 网络 | 适用场景 |
|------|------|----------|
| **云端 API** | 需要 | DeepSeek / 任意 OpenAI 兼容接口，质量通常最好 |
| **小模型** | 首次需下载 | WebLLM（浏览器 WebGPU，零安装）或 Ollama / LM Studio 本机服务 |
| **本地规则** | 不需要 | 多遍本地改写 + 结构组装，离线可用，自动兜底 |

引擎可在主界面一键切换；配置写在应用内「设置」中。

---

## 自行编译（Android）

**环境：** JDK 17、Android SDK（API 34）、Gradle 8.2+

**一键脚本（Windows）：**

```powershell
cd android
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

**说明：**
- 工程路径若含中文，Gradle 可能失败；请先复制到英文路径再编译。
- 脚本可自动下载便携 JDK / SDK / Gradle（体积较大）。
- 成功产物：`android/app/build/outputs/apk/debug/app-debug.apk`

**手动编译：** 用 Android Studio 打开 `android/`，`Build → Build APK(s)`。

---

## 应用内设置速览

| 位置 | 可配置项 |
|------|----------|
| 设置 → 云端 API | 服务商、Base URL、API Key、模型名 |
| 设置 → 小模型 | WebLLM 预设 / Ollama / LM Studio；镜像加速 |
| 设置 → 通用 | 自循环、深色模式、关于开发者 |
| 主界面 | 模式（普通/精简/扩写）、引擎切换 |

API Key 仅保存在本机浏览器 localStorage，不会上传到第三方（除你配置的模型服务本身）。

---

## 技术形态

- **界面：** 单页 Web 应用（HTML/CSS/JS）打包进 Android WebView
- **壳层：** Java `MainActivity`：主题同步系统栏、安全区、返回提示、外链用系统浏览器打开
- **图标：** 二次元美少女风格启动图标（资源已内置）
- **开发者信息：** 头像等已打包为本地资源，离线可显示

---

## 开发者

| | |
|--|--|
| GitHub | [xueccci](https://github.com/xueccci) |
| 主页 | https://github.com/xueccci |

---

## 版本记录（节选）

| 版本 | 要点 |
|------|------|
| v1.4.0 | 开发者头像本地化；关于开发者；全面屏与深色系统栏同步 |
| v1.3.x | 应用图标、按钮圆角覆盖动效、面板布局与滚动条优化 |
| v1.2.x | 同步网页 UI 至 APK；设置页二级下拉、镜像开关等 |
| v1.1.x | 移动端调试包 |
| v1.0 | 首个 WebView 壳 APK |

---

## License

本仓库源码与 APK 仅供学习与个人使用。若需开源协议或商用授权，请通过 GitHub 联系开发者。
