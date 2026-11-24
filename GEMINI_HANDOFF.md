# Gemini 接手说明：ColorOS 小布适配

> 这是工程交接文档，不是面向用户的最终 README。用户已明确把最终的小布版 README 交给 Gemini；请在完成代码检查、构建和真机验证后，由 Gemini 编写最终版本。

## 项目目标

把上游 [SeimoDev/XiaoAiSwitch](https://github.com/SeimoDev/XiaoAiSwitch) 适配到 ColorOS 的小布助手（Breeno），让用户能在小布原生对话界面使用自定义文本模型。适配以安全回退为优先：目标版本或方法签名不匹配时应保留小布原厂链路。

## 仓库与分支

- 用户 fork：[hekinya/XiaoAiSwitch](https://github.com/hekinya/XiaoAiSwitch)
- 工作分支：[coloros-xiaobu](https://github.com/hekinya/XiaoAiSwitch/tree/coloros-xiaobu)
- 上游基线：SeimoDev/XiaoAiSwitch 的 v0.10.12，提交 8fc34e5a7b85a9e217bb3c7086a243a67ed7adaa
- 本文生成时观察到的分支最新提交：bd6de316296efc580a76606c3f9fa2784a909f9c（修复历史记录复制时的 checked JSON exception）。分支之后可能继续变化，请以远端分支最新状态为准。
- 不要重新 fork 或另开分支；先检查并继续现有 coloros-xiaobu。
- 注意：生成此文档的沙盒工作树仍停在基线提交并带有未提交修改，落后于远端分支。请先从 GitHub 获取最新分支并检查差异，**不要把这个旧工作树直接推送覆盖远端进度**。

## 目标设备与应用

以下为用户提供的信息，仍需在目标设备上复核：

- 系统：ColorOS 16.0.9.400(CN01)，Android 16
- 小布包名：com.heytap.speechassist
- Breeno 版本：13.4.3，versionCode=130403
- 用户曾提供 Breeno 13.4.3 APK；APK 没有提交进仓库。若需要重新静态分析，请让用户把 APK 提供给 Gemini。

## 当前实现概况

远端分支在 app/src/main/java/io/github/xiaoaiswitch/BreenoHookEntry.java 中加入适配入口，面向包名及版本 130403，定位的小布文本提交方法为：

AIChatEngineHelper.y(String, int, Bundle, boolean, boolean)

当前适配集中于文本对话：复用小布的用户消息气泡、回答卡片及多轮上下文，并调用项目已有的 OpenAI 兼容 Provider / Codex 客户端。入口会校验目标版本和运行时结构；需继续确认所有失败路径都不会吞掉小布原厂处理。

已改动的主要位置：

- app/src/main/java/io/github/xiaoaiswitch/BreenoHookEntry.java：小布 Hook、版本/方法校验、文本请求接管。
- app/src/main/java/io/github/xiaoaiswitch/SettingsActivity.kt、AppUi.kt：设置界面名称及文本适配说明。
- app/src/main/java/io/github/xiaoaiswitch/ConfigProvider.java：状态回传与目标应用 UID 校验。
- app/src/main/AndroidManifest.xml、app/src/main/assets/xposed_init、app/src/main/res/values/arrays.xml：LSPosed 入口、作用域及 Provider 标识。
- app/build.gradle、settings.gradle、app/src/main/res/values/strings.xml：独立应用标识与小布显示名称。

目标模块应用 ID 为 io.github.xiaobuiswitch，代码包/namespace 仍是 io.github.xiaoaiswitch，LSPosed 作用域为 com.heytap.speechassist。

## 功能边界与待验证事项

目前目标是文本问答。图片输入、手机控制、独立语音/TTS 链路尚未完成或验证；不要在最终 README 中把上游小爱版已有的这些功能写成小布已支持。

请重点审查并验证：

1. 仅接管预期的普通文本提交；小布原生工具、图片/特殊输入和其他模式不会误走文本模型。
2. 模块关闭、Provider 未配置、版本或 Hook 结构不匹配、网络/API 报错时，状态清楚且原厂行为能安全回退。
3. 原有服务开关、上传状态、每日限制、全双工、特殊 UI 模式、旅行计划等保护条件没有被错误绕过。
4. 多轮上下文、连续提问、停止按钮与请求取消行为正确；原生回答卡片渲染没有生命周期或重复记录问题。
5. 在目标手机上验证 LSPosed 注入、状态页诊断和小布原厂链路回退。

目前没有可确认的成功 Android 编译或真机验证结果。先前 ChatGPT 的 Linux 构建容器无法访问 Gradle 下载站；这只说明那个容器的网络受限，不代表用户 Windows 的网络不可用。此前执行过 git diff --check，但它不等于构建或运行验证。

## 用户本机的构建环境

用户已在 Windows 的 D:\Tools\Android 安装 Android Studio 和 Android SDK。该目录在 ChatGPT 的 Linux 沙盒中不可见；除非 Gemini 的执行环境能实际访问用户电脑，否则不要声称已使用这套环境完成构建。

项目配置使用 compileSdk 36、Gradle Wrapper 8.13、Android Gradle Plugin 8.13.2、Kotlin 2.3.20，需要 JDK 17 或兼容版本。Windows PowerShell 可从 fork 分支克隆并构建：

~~~powershell
git clone -b coloros-xiaobu https://github.com/hekinya/XiaoAiSwitch.git
cd XiaoAiSwitch
.\gradlew.bat :app:assembleDebug
~~~

若用户已有克隆，先 git fetch 并切换到远端 coloros-xiaobu 的最新提交。Android Studio 的 Gradle Sync 使用项目 Wrapper；第一次构建仍需下载 Gradle 8.13 和 Maven 依赖。确认 SDK Manager 安装了 Android SDK Platform 36。Debug APK 预期位于：

app/build/outputs/apk/debug/app-debug.apk

## Gemini 接手顺序

1. 获取并检查 fork 的最新 coloros-xiaobu，梳理自上次提交以来的改动，避免用旧工作树覆盖新提交。
2. 在可用的 Windows/Android Studio 环境编译；遇到错误先修复，再汇报真实构建结果。若无法访问用户电脑，给用户明确的命令并请其回传完整构建日志。
3. 按上面的功能边界完成代码审查和真机验证；未验证的行为在结论和文档中继续标注为未验证。
4. 由 Gemini 编写最终的小布版 README。检查仓库现有 README.md 与 README_COLOROS.md 的关系，决定清晰的最终入口；面向用户的内容应准确说明目标版本、安装/启用、配置、功能边界、构建方式和已验证情况，不要沿用上游小爱版本的兼容性承诺。
5. 把必要的代码与文档提交到现有 fork 分支。除非用户另行要求，不要合并回上游仓库。

## 可直接交给 Gemini 的任务提示

请先阅读仓库根目录的 GEMINI_HANDOFF.md，再检查 hekinya/XiaoAiSwitch 的 coloros-xiaobu 最新分支和全部相关代码。继续完成 ColorOS 小布（Breeno 13.4.3）的文本接入适配，优先在 Windows Android Studio 环境构建并修复问题，然后设计并执行可行的真机验证。确认所有未匹配/失败场景安全回退到小布原生流程。最后由你撰写面向用户的最终小布版 README，只写已经确认的事实，并把代码与 README 提交回现有 coloros-xiaobu 分支。不要重新 fork、不要覆盖远端新提交，也不要把未验证功能描述成已完成。如果无法直接访问用户的 Windows 环境或 Breeno APK，请说明并向用户索取构建日志或 APK，不要假称已验证。
