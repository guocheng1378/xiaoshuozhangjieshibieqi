# 📚 小说章节识别器

一个基于 Jetpack Compose 的 Android 小说章节识别器，支持 TXT 和 EPUB 格式。

## 📥 下载

👉 [前往 Releases 页面下载最新 APK](https://github.com/guocheng1378/xiaoshuozhangjieshibieqi/releases/latest)

## ✨ 功能特性

- 📖 支持 **TXT** 和 **EPUB** 格式小说
- 🔍 智能章节识别（支持多种标题格式：第X章、Chapter X、【标题】等）
- 💾 自动保存阅读进度（章节 + 滚动位置）
- 📋 一键复制标题/全文内容
- 🕐 最近阅读记录
- 🎨 Material3 主题（支持浅色/深色/跟随系统）
- 🔤 自动编码检测（解决中文乱码问题）
- ⚙️ 字体大小、行间距可调

## 🛠 技术栈

| 组件 | 技术 |
|------|------|
| UI | Jetpack Compose + Material3 |
| 导航 | Navigation Compose |
| 存储 | DataStore Preferences |
| 文件 | Storage Access Framework (SAF) |
| 构建 | Gradle (Kotlin DSL) |
| CI/CD | GitHub Actions |

## 📦 项目结构

```
app/src/main/java/com/novelreader/
├── data/
│   ├── model/          # 数据模型 (BookFile, Chapter)
│   ├── parser/         # 文件解析器 (TxtParser, EpubParser)
│   └── repository/     # 数据仓库 (BookRepository)
├── navigation/         # 导航图
├── ui/
│   ├── components/     # 通用组件 (FloatingToolbar, CopyToast)
│   ├── screens/        # 页面 (Home, Reader, Settings)
│   └── theme/          # 主题配置
└── util/               # 工具类 (EncodingDetector, ClipboardHelper)
```

## 🚀 构建

```bash
git clone https://github.com/guocheng1378/xiaoshuozhangjieshibieqi.git
cd bookwork
./gradlew assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

**环境要求：**
- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34
- minSdk 29 (Android 10)
