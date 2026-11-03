<div align="center">

# 页序 PDF · YeXuPDF

### 面向 Android 的本地优先 PDF 与文档工具箱

阅读、批注、签名、页面整理、OCR、扫描、图片处理和常见 Office 文档预览，尽量在手机本地完成。

</div>

> 当前状态：开发中。仓库尚未发布正式签名版本，调试 APK 仅用于阶段验收。

## 项目目标

- 在没有 Google Play 服务的国内 Android 手机上正常安装和使用。
- PDF、图片、OCR 文字和 Office 文档默认只在设备本地处理，不上传到服务器。
- 需要联网的功能必须由用户明确触发，并与本地文档处理代码隔离。
- 模型或可选组件支持国内可访问的下载方式以及离线导入。
- 提供可复现构建、自动测试、权限审计和 GitHub Release APK。

## 当前能力

- 阅读 PDF，搜索、复制文字并对扫描页执行本地 OCR。
- 高亮、下划线、手写、文本、形状、签名和表单填写。
- 合并、拆分、旋转、提取、删除和调整 PDF 页面顺序。
- 压缩、加密、解密、水印、页码、图片与 PDF 互转。
- 扫描纸质文档，裁剪、透视校正并生成 PDF。
- 打开 DOC/DOCX、XLS/XLSX、PPT/PPTX、ODT/ODS/ODP、RTF、CSV 和 TXT。
- 编辑 XLSX 单元格以及进行常用图片编辑。

不同构建变体的能力仍在统一中：`fdroid` 不依赖 Google 服务，但上游暂时关闭了自动去背景和可选 Office 引擎；这些功能将在后续阶段替换为无 GMS 方案。

## 隐私与联网边界

页序 PDF 没有账号、广告、统计 SDK、崩溃上报或文档上传服务。当前源码中只有以下用户主动联网场景：

1. 把用户输入的网址转换为 PDF。
2. `foss` 变体按需下载经过大小和 SHA-256 校验的可选 Office 引擎。

本地 HTML 和 DOCX 的 WebView 会阻止外部资源请求。详细边界和各变体差异见 [PRIVACY.md](PRIVACY.md)。

## 构建

需要 JDK 17、Android SDK 36 和 Android Build Tools 36.1.0。

无 Google 服务的调试版本：

```bash
./gradlew :app:assembleFdroidDebug :app:testFdroidDebugUnitTest
```

包含上游 Google ML Kit 功能的对照版本：

```bash
./gradlew :app:assembleFossDebug :app:testFossDebugUnitTest
```

APK 位于 `app/build/outputs/apk/<flavor>/debug/`。

Windows 工程路径包含中文时，部分 Gradle 测试子进程可能无法正确解析 classpath，建议把仓库放在纯英文路径下构建。

## 工程结构

| 路径 | 用途 |
|---|---|
| `app/` | Android 应用、界面、文档工具和图片编辑器 |
| `pdf-core/` | PDF 操作与渲染支持 |
| `ocr-core/` | 本地 OCR |
| `backdrop/` | 液态玻璃背景效果 |
| `office_engine/` | 可选 Office 引擎的 Play 动态模块 |

## 来源与许可证

页序 PDF 基于 [Chethan616/ClearPDF](https://github.com/Chethan616/ClearPDF) 修改，分叉基线为 ClearPDF 2.2.2、提交 `d9a6adc5c634652638628634001835c6678cc606`。

项目继续使用 [MIT License](LICENSE)。原作者版权声明、第三方许可证和 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 必须保留。项目改名不代表上游作者为本分叉提供担保或背书。

## English summary

YeXuPDF is a work-in-progress, local-first Android PDF and document toolbox derived from ClearPDF. Its target distribution works without Google Play Services and keeps document processing on-device. See [PRIVACY.md](PRIVACY.md) for the exact network boundary.
