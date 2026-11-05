# 第 3 阶段开源方案评估

评估日期：2026-11-05。目标是让 YeXuPDF 的推荐 Android 版本不依赖 Google Play 服务，图片和文档始终在本机处理，并避免用户在国内首次使用时下载模型或引擎。

## 背景移除

| 方案 | 许可证与维护 | 代价或风险 | 结论 |
|---|---|---|---|
| Google ML Kit Subject Segmentation | Google APIs Terms；现有上游实现 | 依赖 Google Play 服务和运行时模型交付 | 仅保留在对照变体，不用于推荐版 |
| MediaPipe | Apache-2.0；Google 持续维护 | 官方隐私说明提到 Tasks 使用指标；现成模型更偏人物或交互分割 | 不采用 |
| AppcentMobile/removebg | MIT；约 108 stars、27 commits | JitPack 依赖增加国内构建失败点，封装维护较弱 | 只参考接口，不引入依赖 |
| U-2-Net / U2NetP | Apache-2.0；论文项目约 9.9k stars | 320×320 小模型边缘精度低于大型模型 | 采用，4.6 MB，直接打进 APK |
| ONNX Runtime Android | MIT；Microsoft 维护，约 22k stars | 增加原生库体积，最低 Android 7 | 采用，负责完全本地推理 |
| BiRefNet / RMBG-1.4 | 多种许可；近期 Android 样例 | 模型约 42–200 MB，内存峰值高，部分权重限制商用 | 当前不采用 |

实现参考了开源 Android 样例的 U2NetP 预处理方式，但推理适配代码按 YeXuPDF 的 `BackgroundRemover` 接口重新实现。模型固定为 4,574,861 字节，SHA-256 为 `309c8469258dda742793dce0ebea8e6dd393174f89934733ecc8b14c76f4ddd8`，自动测试会检查该值。

## Office 高保真转换

| 方案 | 许可证与维护 | 代价或风险 | 结论 |
|---|---|---|---|
| LibreOffice core / LibreOfficeKit | MPL-2.0 为主；大型成熟项目 | Android 自行编译复杂，运行时和字体体积较大 | 继续采用现有引擎架构 |
| OpenDocument.droid | MPL-2.0；约 485 stars、5,700+ commits | 更偏阅读器，核心 AAR 约 100 MB，接入方式与现有转换服务不同 | 不替换当前引擎 |
| Fadocx | GPL-3.0；内置 LibreOfficeKit | APK 约 346 MB，GPL 会改变整个项目的分发义务 | 不复用代码 |
| LibreOffice-Lite v2.0 | LibreOffice MPL-2.0；上游 ClearPDF 已接入 | 构建仓库很新、关注度低，二进制供应链风险较高 | 暂用，但固定大小和哈希、隔离进程、保留替换路线 |

推荐的 `offline` APK只支持 arm64，并携带 47,521,152 字节的 LibreOfficeKit 压缩包，SHA-256 为 `6665ad47db41d116248c40e6fc608a3425b9eb4a304f5ad5e5d5c80dbea61e30`。安装后的应用不会联网下载它；首次启用只是校验并解压到应用沙盒。开发电脑可以用 `-PofflineOfficeEngineArchive=<path>` 提供已下载文件，避免构建期间访问 GitHub。

## 保留风险

- U2NetP 是轻量模型，发丝、透明物体和复杂边缘可能需要手动擦除或恢复。
- Office 引擎解压需要约 300 MB Java 堆和额外存储，低内存设备可能无法安装。
- 当前 LibreOffice-Lite 二进制不是 YeXuPDF 自行编译；正式长期发布前应复现其构建或迁移到可审计的自建产物。
- 推荐 APK 目前只支持 Android 7.0+ 的 arm64 手机。
