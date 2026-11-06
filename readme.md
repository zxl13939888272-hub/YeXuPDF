<div align="center">

# 页序 PDF · YeXuPDF

### 面向 Android 的本地优先 PDF 与文档工具箱

阅读、批注、签名、页面整理、OCR、扫描、图片处理和常见 Office 文档预览，尽量在手机本地完成。

</div>

> 当前状态：`v0.1.0` 发布候选版。正式签名 APK 已完成本地构建、签名与对齐校验；GitHub Release 和签名版真机安装仍待发布前验收。

## 项目目标

- 在没有 Google Play 服务的国内 Android 手机上正常安装和使用。
- PDF、图片、OCR 文字和 Office 文档默认只在设备本地处理，不上传到服务器。
- 需要联网的功能必须由用户明确触发，并与本地文档处理代码隔离。
- 推荐版直接携带必要模型和 Office 组件，安装后不需要从 Google 或 GitHub 下载运行资源。
- 提供可复现构建、自动测试、权限审计和 GitHub Release APK。

## 当前能力

- 阅读 PDF，搜索、复制文字并对扫描页执行本地 OCR。
- 高亮、下划线、手写、文本、形状、签名和表单填写。
- 合并、拆分、旋转、提取、删除和调整 PDF 页面顺序。
- 压缩、加密、解密、水印、页码、图片与 PDF 互转。
- 扫描纸质文档，裁剪、透视校正并生成 PDF。
- 打开 DOC/DOCX、XLS/XLSX、PPT/PPTX、ODT/ODS/ODP、RTF、CSV 和 TXT。
- 编辑 XLSX 单元格以及进行常用图片编辑。

推荐的 `offline` 变体不依赖 Google Play 服务：自动去背景使用 APK 内置的 U²-NetP + ONNX Runtime，Office 高保真转换使用 APK 内置并经 SHA-256 校验的 LibreOfficeKit。首次使用 Office 引擎时只是把 APK 内的组件解压到应用沙盒，不会联网。

## 隐私与联网边界

页序 PDF 没有账号、广告、统计 SDK、崩溃上报或文档上传服务。当前源码中只有以下用户主动联网场景：

1. 把用户输入的网址转换为 PDF。
2. 仅作为上游对照的 `foss` 变体可按需下载经过大小和 SHA-256 校验的 Office 引擎。

`offline` 推荐版不会下载模型、Office 引擎或代码；本地 PDF、图片、OCR 和 Office 文件不会上传。

本地 HTML 和 DOCX 的 WebView 会阻止外部资源请求。详细边界和各变体差异见 [PRIVACY.md](PRIVACY.md)。

## 构建

需要 JDK 17、Android SDK 36 和 Android Build Tools 36.1.0。

无 Google 服务的调试版本：

```bash
./gradlew :app:assembleOfflineDebug :app:testOfflineDebugUnitTest
```

包含上游 Google ML Kit 功能的对照版本：

```bash
./gradlew :app:assembleFossDebug :app:testFossDebugUnitTest
```

APK 位于 `app/build/outputs/apk/<flavor>/debug/`。

正式版使用项目所有者自己的长期发布密钥签名。首次配置可在 Windows PowerShell 中运行：

```powershell
.\scripts\setup-release-signing.ps1
```

脚本生成的 `.signing/` 与 `key.properties` 已被 Git 忽略。发布密钥和密码必须分别安全备份；任何一项丢失都会导致以后生成的 APK 无法覆盖安装现有版本。

`offline` 构建面向 Android 7.0+ 的 arm64 手机。构建时 Gradle 会取得固定版本的 LibreOfficeKit 并校验哈希；国内或隔离网络构建可提前准备该文件：

```bash
./gradlew :app:assembleOfflineRelease \
  -PofflineOfficeEngineArchive=/path/to/LibreOffice-arm64.tar.xz
```

该参数只影响开发电脑，安装后的手机不需要下载组件。开源方案比较和许可证评估见 [docs/OPEN_SOURCE_REVIEW.md](docs/OPEN_SOURCE_REVIEW.md)。

Windows 工程路径包含中文时，部分 Gradle 测试子进程可能无法正确解析 classpath，建议把仓库放在纯英文路径下构建。

## 工程结构

| 路径 | 用途 |
|---|---|
| `app/` | Android 应用、界面、文档工具和图片编辑器 |
| `pdf-core/` | PDF 操作与渲染支持 |
| `ocr-core/` | 本地 OCR |
| `backdrop/` | 液态玻璃背景效果 |
| `office_engine/` | 可选 Office 引擎的 Play 动态模块 |

## 发布包核验

推荐从本项目的 GitHub Releases 下载名称形如 `YeXuPDF-v0.1.0-offline-arm64.apk` 的文件。每个正式版本同时提供 `.sha256` 校验文件。

`v0.1.0` 及后续版本应使用以下证书指纹签名：

```text
SHA-256: 12CE35615F0C47D40B5F450612D1377BD0717859053A647B73242979E6FF3873
```

## 来源与许可证

页序 PDF 基于 [Chethan616/ClearPDF](https://github.com/Chethan616/ClearPDF) 修改，分叉基线为 ClearPDF 2.2.2、提交 `d9a6adc5c634652638628634001835c6678cc606`。

项目继续使用 [MIT License](LICENSE)。原作者版权声明、第三方许可证和 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 必须保留。项目改名不代表上游作者为本分叉提供担保或背书。

## English summary

YeXuPDF is a local-first Android PDF and document toolbox derived from ClearPDF. Its recommended distribution works without Google Play Services and keeps document processing on-device. See [PRIVACY.md](PRIVACY.md) for the exact network boundary.
