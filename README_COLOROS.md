# 小布 Switch（ColorOS 适配版）

本文记录当前分支的适配范围和交接状态。仓库根目录 `README.md` 下方保留了上游小爱版说明；涉及 ColorOS 小布的包名、功能和验证情况时，以本文为准。

## 分支与目标环境

- 仓库：[`hekinya/XiaoAiSwitch`](https://github.com/hekinya/XiaoAiSwitch)
- 工作分支：`coloros-xiaobu`，从上游 `dev` 分支派生
- 适配基线：上游 [`v0.10.12`](https://github.com/SeimoDev/XiaoAiSwitch/releases/tag/v0.10.12)；派生时 `dev` 与该版本提交一致
- 目标设备信息：用户提供的截图显示 ColorOS `16.0.9.400(CN01)`、Android 16；Breeno 包名 `com.heytap.speechassist`，版本 `13.4.3(130403)`

这些是目标设备和 APK 的版本信息，不代表适配已在真机验证通过。

## 当前实现

- 模块使用独立包名 `io.github.xiaobuiswitch`，LSPosed 作用域指向 `com.heytap.speechassist`。
- `BreenoHookEntry` 只在目标包和版本 `130403` 匹配时尝试接管；当前定位的文本提交方法为 `AIChatEngineHelper.y(String, int, Bundle, boolean, boolean)`。
- 接管后复用小布原生的用户消息气泡、回答卡片和多轮上下文，并通过已有的 OpenAI 兼容 Provider 或 Codex 客户端请求回答。
- 保留已识别的小布服务开关、上传状态、每日限制、全双工和旅行计划等保护条件；版本或方法校验不匹配时应保留原厂链路。
- 目前只做文本回答适配。图片输入、手机控制、独立语音/TTS 链路尚未接入；语音识别后是否会经过同一文本入口也尚未验证。

## 文件入口

- `app/src/main/java/io/github/xiaoaiswitch/BreenoHookEntry.java`：小布 Hook 入口和版本/方法校验。
- `app/src/main/java/io/github/xiaoaiswitch/SettingsActivity.kt`、`AppUi.kt`：适配设置界面及名称。
- `app/src/main/AndroidManifest.xml`、`app/src/main/assets/xposed_init`：模块标识、Provider URI 和 Xposed 入口声明。
- `app/build.gradle`、`settings.gradle`：独立应用 ID 与适配版构建配置。

## 构建与安装

需要 JDK 17、Android SDK Platform 36，以及可下载 Gradle 和 Android 依赖的网络环境：

```sh
./gradlew :app:assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。

安装后在 LSPosed 中启用“小布 Switch”，确认作用域包含 `com.heytap.speechassist`，重启小布进程，再打开模块配置 Provider 和文本模型并启用模块接管。模块状态页应显示识别到的 Breeno 版本或具体的不匹配原因。

## 验证状态与后续工作

- 已运行 `git diff --check`，无空白错误。
- 当前工作环境未完成 Android 编译或真机验证。先前构建尝试在下载 Gradle 8.13 时因网络受限失败，环境也未检测到 Android SDK；因此不能据此判断代码能否编译或 Hook 能否运行。
- 后续接手时先检查当前分支改动，再按 [`CONTRIBUTING.md`](CONTRIBUTING.md) 执行上游要求的测试、覆盖率、Lint 和构建任务；构建通过后再在目标 ColorOS/Breeno 版本上验证 LSPosed 注入、原厂链路回退和原生回答卡片。
- 若需重新核对私有实现签名，可使用用户提供的 Breeno 13.4.3 APK；该 APK 未提交到仓库。

本分支当前是待验证的适配进度，不应当作已发布或已完成的 ColorOS 版本。
