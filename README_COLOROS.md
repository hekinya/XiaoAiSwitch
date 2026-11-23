# 小布 Switch（ColorOS 适配版）

这是基于 [SeimoDev/XiaoAiSwitch v0.10.12](https://github.com/SeimoDev/XiaoAiSwitch/releases/tag/v0.10.12) 做的 ColorOS 小布适配分支。

## 当前适配范围

- 目标包名：`com.heytap.speechassist`
- 已核对版本：Breeno 13.4.3，`versionCode=130403`
- 已核对系统：ColorOS 16.0.9.400（CN01），Android 16
- 接管小布文本提交，复用小布原生用户消息气泡、回答卡片和多轮上下文
- 可使用 OpenAI 兼容 API 或 Codex 订阅
- 图片、手机控制和语音播报尚未接入；语音识别出的文本如果经过同一文本提交方法，会显示文字回答
- 保留小布的服务开关、上传状态、每日限制和全双工保护检查
- 版本号或内部方法签名不匹配时不接管，保留小布原厂请求

## 构建

需要 JDK 17、Android SDK Platform 36，以及可下载 Gradle 和 Android 依赖的网络环境：

```sh
./gradlew :app:assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。

## 安装与启用

1. 安装 APK，并在 LSPosed 中启用“小布 Switch”。
2. 确认模块作用域包含 `com.heytap.speechassist`，然后重启小布进程。
3. 打开“小布 Switch”，添加 API Provider 或登录 Codex 订阅。
4. 进入“文本对话”，选择 Provider、填写模型名和 System Prompt，打开“使用自定义服务”并保存。
5. 在首页打开“模块接管”。

模块状态页会显示已识别的 Breeno 版本或具体不匹配原因。关闭“模块接管”或关闭文本模型后，小布恢复原厂请求。

## 实现入口

`BreenoHookEntry` 在小布 13.4.3 中核对 `AIChatEngineHelper.y(String, int, Bundle, boolean, boolean)`；符合版本和配置条件后，阻止该次原厂服务请求，在原生卡片中显示模型回复。
