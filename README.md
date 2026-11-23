# 小爱 Switch

> **此工作副本已改为 ColorOS 小布适配分支。** 请先看 [README_COLOROS.md](README_COLOROS.md) 获取适配范围、安装与构建说明。下方原说明描述的是上游小爱版，不能作为本分支的兼容性说明。

<img src="assets/icon/xiaoai-switch-anime-original.png" width="112" height="112" alt="小爱 Switch 图标">

[![开源协议：GPL-3.0-only](https://img.shields.io/badge/license-GPL--3.0--only-blue?logo=gnu)](LICENSE)
[![Android 构建状态](https://github.com/SeimoDev/XiaoAiSwitch/actions/workflows/build.yml/badge.svg?branch=dev)](https://github.com/SeimoDev/XiaoAiSwitch/actions/workflows/build.yml)
[![正式版本](https://img.shields.io/github/v/release/SeimoDev/XiaoAiSwitch)](https://github.com/SeimoDev/XiaoAiSwitch/releases/latest)
[![Android API 27+](https://img.shields.io/badge/Android-API%2027%2B-3DDC84?logo=android&logoColor=white)](#兼容性)
[![LSPosed 模块](https://img.shields.io/badge/LSPosed-Xposed%20API%2082-orange)](#兼容性)
[![GitHub Stars](https://img.shields.io/github/stars/SeimoDev/XiaoAiSwitch?logo=github)](https://github.com/SeimoDev/XiaoAiSwitch/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/SeimoDev/XiaoAiSwitch?logo=github)](https://github.com/SeimoDev/XiaoAiSwitch/forks)
[![GitHub Issues](https://img.shields.io/github/issues/SeimoDev/XiaoAiSwitch)](https://github.com/SeimoDev/XiaoAiSwitch/issues)

自定义超级小爱的文本、语音、识图、绘图和手机控制模型。用一个 LSPosed 模块管理 AI Provider、模型与 System Prompt，结果仍在小爱的原生界面显示。

[使用方法](#使用方法) · [从源码构建](#从源码构建) · [贡献指南](CONTRIBUTING.md) · [反馈问题](https://github.com/SeimoDev/XiaoAiSwitch/issues/new) · [Star 趋势](#star-趋势)

## 功能

- **模型管理**：兼容 OpenAI API，也支持在手机独立登录并直连 Codex 订阅。文本和语音的默认／思考模式可分别选模型和提示词。
- **文本与语音**：流式服务的回答逐段出现在原生 Markdown 卡片上；语音保留小爱的语音识别和 TTS，包括 8.2 双工语音入口。
- **识图**：可为小爱的图片问答单独选择支持图片输入的模型。绘图和图生图仍使用「绘图」配置。
- **绘图**：支持文生图、原图编辑和放大预览。可用独立图片 API，或直接使用 Codex 订阅绘图。
- **手机控制**：支持截图、点击、滑动和输入；沿用原厂 AI 智控权限和全局光效，可随时停止。
- **超级岛停止**：支持超级岛的 8.2 新设备在控制时点击顶部「控制中」，展开后可选择「停止控制」；不再用悬浮按钮覆盖应用内容。
- **系统工具**：文本与语音可打开已安装的应用、提交单次闹钟和倒计时，不依赖屏幕控制。
- **原厂全局记忆**：8.2 的明确记忆命令（如“请记住……”或“查一下我的记忆……”）由原厂小爱处理，普通对话仍走自选模型。

## 兼容性

| 项目 | 参数 |
| --- | --- |
| 应用包名 | `io.github.xiaoaiswitch` |
| 小爱作用域 | `com.miui.voiceassist` |
| 已适配小爱版本 | `7.11.20.3514`（`507011020`）；`8.2.70.8421`（`508002070`，含双工语音和手机控制） |
| Android SDK | `minSdk 27` · `targetSdk 35` · `compileSdk 36` |
| 模块加载 | LSPosed，声明最低 Xposed API 82 |
| 真机验证 | 小米平板 5（Android 16、LSPosed 2.2.1）；`2304FPN6DC`（[8.2 验证记录](analysis/voiceassist-8.2.md)） |
| 构建工具链 | JDK 17 · Gradle 8.13 · AGP 8.13.2 · Kotlin 2.3.20 · Miuix 0.8.7 |
| 默认分支 | `dev` |

模块启动时会校验已知 Hook 的参数、返回类型和 Handler。遇到新版本时，运行状态可提示是否匹配已知结构，仍保留原厂链路，防止接管到错误的方法。只有当新版入口或行为有实质变化时，才需重新定位和验证；详见 [8.2 适配记录](analysis/voiceassist-8.2.md)。

设置页支持 Android 14 及以上的预测性返回：在手势导航模式下侧滑可预览上一页，中途取消不改变页面，也不提交配置草稿。

## 使用方法

1. 从 [GitHub Releases](https://github.com/SeimoDev/XiaoAiSwitch/releases/latest) 下载并安装正式版 APK，在 LSPosed 中为模块勾选 **超级小爱**，然后重启小爱进程。
2. 打开 **小爱 Switch → AI Provider**，添加 OpenAI 兼容源或登录 Codex 订阅。
3. 在各通道的模型配置页选择 Provider、模型与 System Prompt，保存后打开首页的 **模块接管**。

| 配置 | 说明 |
| --- | --- |
| API Endpoint / API Token | 在 Provider 中统一配置。例如 `https://HOST/v1`；对话也接受完整的 `/chat/completions` 地址。 |
| Model / System Prompt | 每个通道和模式独立选择。兼容对话源发送 `role: system`；绘图使用提示词前缀。 |
| 识图 | 在首页进入「识图」，启用并选择支持图片输入的模型。之后在小爱的图片问答入口添加照片。未启用时保留原厂识图。 |
| 默认通道 / 自动分流 | 在通用设置配置。自动分流没有固定调用上限；模型给出最终答复或任务取消时结束。也可用 `/chat 文本`、`/voice 文本`、`/draw 描述`、`/control 任务` 显式切换。 |
| 系统工具 | 在「通用设置」单独开启。启用时自动在你的 System Prompt 后加入工具说明；时钟动作的实际状态以系统时钟为准。 |
| 原厂全局记忆 | 在「通用设置」查看开关和原厂记忆设置。可用 `/memory 请求` 明确指定原厂处理；模块不把原厂记忆条目交给自选 Provider。 |
| 手机控制 | 模型需支持图像和工具调用；手机需解锁，并通过原厂屏幕授权。8.2 的点击和滑动以最近一张截图的可操作范围为准，不包含状态栏等系统边缘。打开应用后等待目标进入前台，再以新画面操作。暂时异常隔 5 秒重试，最多额外重试 3 次。可在通用设置填写步骤上限（默认 12 步），也可手动停止。 |

## 从源码构建

安装 JDK 17 与 Android SDK 36，通过 `ANDROID_HOME` 或本地 `local.properties` 的 `sdk.dir` 指定 SDK 位置。

```bash
git clone --branch dev https://github.com/SeimoDev/XiaoAiSwitch.git
cd XiaoAiSwitch/core
../gradlew test jacocoTestCoverageVerification :app:lintDebug :app:assembleDebug :app:assembleRelease
```

Windows 可将 `../gradlew` 替换为 `..\gradlew.bat`。CI 执行相同的测试、JaCoCo 覆盖率门槛、lint 和构建任务；不连接真实设备。

回到仓库根目录安装调试 APK：

```powershell
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

## 贡献与文档

- [贡献指南](CONTRIBUTING.md) · [问题反馈](https://github.com/SeimoDev/XiaoAiSwitch/issues)
- [AI Provider 注册表](analysis/provider-registry.md) · [默认与思考模式](analysis/thinking-modes.md) · [Codex 订阅绘图](analysis/codex-images.md)
- [新旧版识图适配](analysis/vision-native-pipeline.md)
- [原厂系统工具链路](analysis/native-system-tools.md)
- [原生流式卡片](analysis/native-streaming.md) · [原厂全局记忆](analysis/xiaoai-global-memory.md)
- [小爱 AIVS 协议](https://developers.xiaoai.mi.com/documents/Home?type=%2Fapi%2Fdoc%2Frender_markdown%2FVoiceserviceAccess%2FDevice%2Fdevelop%2FProtocolDocument%2FEndpointOverview) · [LSPosed 模块作用域](https://github.com/LSPosed/LSPosed/wiki/Module-Scope)

## 开源协议

本项目源代码按 [GNU General Public License v3.0 only（GPL-3.0-only）](LICENSE) 发布。第三方组件的授权见 [NOTICE](NOTICE.md)。

## Star 趋势

[![小爱 Switch 的 GitHub Star 趋势](assets/star-history.svg)](https://github.com/SeimoDev/XiaoAiSwitch/stargazers)

GitHub Actions 每日读取仓库公开的 Star 总数，记录采样到的变化；不收集点 Star 的用户信息。
