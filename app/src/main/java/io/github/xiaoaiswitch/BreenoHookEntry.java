package io.github.xiaoaiswitch;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.xiaoaiswitch.core.ChatProtocol;
import io.github.xiaoaiswitch.core.CodexClient;
import io.github.xiaoaiswitch.core.CodexOAuth;
import io.github.xiaoaiswitch.core.ModeProfiles;
import io.github.xiaoaiswitch.core.ModelClient;
import io.github.xiaoaiswitch.core.Profile;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Hook for the ColorOS Breeno build supplied with this port. */
public final class BreenoHookEntry implements IXposedHookLoadPackage {
    private static final String TARGET_PACKAGE = "com.heytap.speechassist";
    private static final int TARGET_VERSION = 130403;
    private static final String RECORD_ID_KEY = "customized_record_id";

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) {
        if (!TARGET_PACKAGE.equals(load.packageName)) return;
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Context context = ((Context) param.args[0]).getApplicationContext();
                try {
                    PackageInfo info = context.getPackageManager().getPackageInfo(TARGET_PACKAGE, 0);
                    if (info.versionCode != TARGET_VERSION) {
                        report(context, "小布版本不匹配：检测到 " + info.versionName + "（" + info.versionCode
                                + "），当前适配 13.4.3（130403）；保留原厂链路", false);
                        return;
                    }
                    Class<?> helper = XposedHelpers.findClassIfExists(
                            "com.heytap.speechassist.aichat.floatwindow.AIChatEngineHelper", load.classLoader);
                    if (helper == null) {
                        report(context, "未找到小布对话组件；保留原厂链路", false);
                        return;
                    }
                    Method submit = helper.getDeclaredMethod("y", String.class, int.class, Bundle.class,
                            boolean.class, boolean.class);
                    if (submit.getReturnType() != Void.TYPE) {
                        report(context, "小布文本接口结构不匹配；保留原厂链路", false);
                        return;
                    }
                    new Runtime(context, load.classLoader).install(helper);
                    report(context, "已接入小布 13.4.3；请在小布 Switch 中配置文本模型", true);
                } catch (Throwable error) {
                    report(context, "小布 Hook 安装失败：" + error.getClass().getSimpleName(), false);
                    XposedBridge.log("XiaoBuSwitch: install " + error.getClass().getSimpleName());
                }
            }
        });
    }

    private static void report(Context context, String text, boolean ready) {
        try {
            Bundle extras = new Bundle();
            if (ready) extras.putInt("moduleVersion", BuildConfig.VERSION_CODE);
            context.getContentResolver().call(ConfigProvider.URI, "status", text, extras);
        } catch (Throwable error) {
            XposedBridge.log("XiaoBuSwitch: status " + error.getClass().getSimpleName());
        }
    }

    private static final class Runtime {
        private final Context context;
        private final ClassLoader loader;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ExecutorService worker = Executors.newFixedThreadPool(2);
        private final AtomicLong generation = new AtomicLong();
        private final AtomicReference<ModelClient.Cancellation> active = new AtomicReference<>();
        private final Map<String, JSONArray> histories = new LinkedHashMap<>();
        private long historyRevision = Long.MIN_VALUE;

        Runtime(Context context, ClassLoader loader) {
            this.context = context;
            this.loader = loader;
        }

        void install(Class<?> helper) {
            XposedHelpers.findAndHookMethod(helper, "y", String.class, int.class, Bundle.class,
                    boolean.class, boolean.class, new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            intercept(param);
                        }
                    });
        }

        private void intercept(XC_MethodHook.MethodHookParam param) {
            try {
                String text = (String) param.args[0];
                Bundle bundle = param.args[2] instanceof Bundle ? (Bundle) param.args[2] : null;
                boolean addQuery = Boolean.TRUE.equals(param.args[3]);
                if (text == null || text.trim().isEmpty() || !addQuery) return;

                JSONObject config = config();
                if (!config.optBoolean("enabled", false)) return;
                Profile profile = ModeProfiles.resolve(config, "chat", false);
                if (!profile.enabled()) return;
                if (!nativeGuardsAllow(param.thisObject)) return;

                String roomId = currentRoom();
                if (roomId.isEmpty()) return;
                String suppliedRecordId = bundle == null ? null : bundle.getString(RECORD_ID_KEY);
                final String recordId = suppliedRecordId == null || suppliedRecordId.isEmpty()
                        ? UUID.randomUUID().toString() : suppliedRecordId;

                // Reuse Breeno's query bubble and generating state before suppressing its server request.
                XposedHelpers.callMethod(param.thisObject, "b", text, recordId, bundle,
                        Boolean.TRUE.equals(param.args[4]));
                param.setResult(null);

                long revision = config.optLong("revision", 0L);
                JSONArray history = history(roomId, revision);
                JSONObject request = ChatProtocol.request(profile, history, text);
                long requestId = generation.incrementAndGet();
                ModelClient.Cancellation cancellation = new ModelClient.Cancellation();
                ModelClient.Cancellation previous = active.getAndSet(cancellation);
                if (previous != null) previous.cancel();
                try {
                    worker.execute(() -> ask(requestId, revision, roomId, recordId, profile, request, cancellation));
                } catch (RuntimeException rejected) {
                    active.compareAndSet(cancellation, null);
                    showAnswer(requestId, roomId, recordId, "模型请求无法启动，请稍后重试。", false, null, null);
                }
            } catch (Throwable error) {
                XposedBridge.log("XiaoBuSwitch: request " + error.getClass().getSimpleName());
            }
        }

        private boolean nativeGuardsAllow(Object engine) {
            try {
                Class<?> appContext = XposedHelpers.findClass("com.heytap.speechassist.utils.z2", loader);
                Object context = XposedHelpers.callStaticMethod(appContext, "b");
                Class<?> preferences = XposedHelpers.findClass("com.heytap.speechassist.utils.z5", loader);
                if (!Boolean.TRUE.equals(XposedHelpers.callStaticMethod(preferences, "q", context))) return false;

                Class<?> common = XposedHelpers.findClass(
                        "com.heytap.speechassist.aichat.floatwindow.utils.AiChatCommonHelperKt", loader);
                if (Boolean.TRUE.equals(XposedHelpers.callStaticMethod(common, "p", null, 1, null))) return false;

                Class<?> stateManagerClass = XposedHelpers.findClass(
                        "com.heytap.speechassist.aichat.state.AiChatStateManager", loader);
                Object companion = XposedHelpers.getStaticObjectField(stateManagerClass, "f26332g");
                Object stateManager = XposedHelpers.callMethod(companion, "a");
                Class<?> subStateClass = XposedHelpers.findClass(
                        "com.heytap.speechassist.aichat.state.AiChatSubState", loader);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object dailyLimit = Enum.valueOf((Class<? extends Enum>) subStateClass.asSubclass(Enum.class), "DIALOG_LIMIT_TODAY");
                if (Boolean.TRUE.equals(XposedHelpers.callMethod(stateManager, "E", dailyLimit))) return false;

                Class<?> duplexClass = XposedHelpers.findClass("com.heytap.speechassist.utils.FullDuplexState", loader);
                Object duplex = XposedHelpers.getStaticObjectField(duplexClass, "INSTANCE");
                if (Boolean.TRUE.equals(XposedHelpers.callMethod(duplex, "n"))) return false;
                return !Boolean.TRUE.equals(XposedHelpers.callMethod(engine, "d"));
            } catch (Throwable error) {
                XposedBridge.log("XiaoBuSwitch: native guard " + error.getClass().getSimpleName());
                return false;
            }
        }

        private void ask(long requestId, long revision, String roomId, String recordId, Profile profile,
                         JSONObject request, ModelClient.Cancellation cancellation) {
            try {
                JSONObject response = chat(profile, request, cancellation);
                String answer = ChatProtocol.answer(response);
                if (!isCurrent(requestId, revision)) {
                    resetIfLatest(requestId, roomId);
                    return;
                }
                showAnswer(requestId, roomId, recordId, answer, true, request, profile);
            } catch (java.util.concurrent.CancellationException ignored) {
                // A newer question or a settings change superseded this request.
            } catch (Throwable error) {
                if (!isCurrent(requestId, revision)) {
                    resetIfLatest(requestId, roomId);
                    return;
                }
                String reason = error.getMessage();
                if (reason == null || reason.isBlank()) reason = "网络或模型服务异常";
                reason = reason.replace('\n', ' ').replace('\r', ' ');
                if (reason.length() > 100) reason = reason.substring(0, 100);
                showAnswer(requestId, roomId, recordId, "模型请求失败：" + reason, false, null, null);
            } finally {
                active.compareAndSet(cancellation, null);
            }
        }

        private JSONObject chat(Profile profile, JSONObject request, ModelClient.Cancellation cancellation) throws Exception {
            if (!"codex_subscription".equals(profile.provider())) {
                return ModelClient.chat(profile, request, cancellation);
            }
            CodexOAuth.Tokens tokens = credentials(false, null);
            try {
                return CodexClient.chat(profile, request, tokens, cancellation);
            } catch (ModelClient.HttpError error) {
                if (error.status() != 401) throw error;
                return CodexClient.chat(profile, request, credentials(true, tokens.accessToken()), cancellation);
            }
        }

        private CodexOAuth.Tokens credentials(boolean force, String previousAccess) throws Exception {
            Bundle extras = new Bundle();
            extras.putBoolean("force", force);
            if (previousAccess != null) extras.putString("previousAccess", previousAccess);
            Bundle result = context.getContentResolver().call(ConfigProvider.URI, "codexCredentials", null, extras);
            if (result == null || result.containsKey("error")) throw new CodexClient.LoginRequired();
            return new CodexOAuth.Tokens(result.getString("accessToken", ""), "", "",
                    result.getString("accountId", ""), "", "", result.getLong("expiresAt"));
        }

        private JSONObject config() throws Exception {
            Bundle result = context.getContentResolver().call(ConfigProvider.URI, "config", null, null);
            return new JSONObject(result == null ? "{}" : result.getString("json", "{}"));
        }

        private JSONArray history(String roomId, long revision) {
            synchronized (histories) {
                if (historyRevision != revision) {
                    histories.clear();
                    historyRevision = revision;
                }
                JSONArray value = histories.get(roomId);
                return value == null ? new JSONArray() : new JSONArray(value.toString());
            }
        }

        private boolean isCurrent(long requestId, long revision) {
            if (generation.get() != requestId) return false;
            try {
                JSONObject latest = config();
                return latest.optBoolean("enabled", false) && latest.optLong("revision", 0L) == revision;
            } catch (Exception error) {
                return false;
            }
        }

        private void resetIfLatest(long requestId, String roomId) {
            main.post(() -> {
                if (generation.get() == requestId) setIdle(roomId);
            });
        }

        private String currentRoom() {
            Class<?> type = XposedHelpers.findClassIfExists(
                    "com.heytap.speechassist.aichat.AIChatRoomIdManager", loader);
            if (type == null) return "";
            Object manager = XposedHelpers.getStaticObjectField(type, "INSTANCE");
            Object value = XposedHelpers.callMethod(manager, "H");
            return value instanceof String ? (String) value : "";
        }

        private void showAnswer(long requestId, String roomId, String recordId, String answer,
                                boolean addHistory, JSONObject request, Profile profile) {
            main.post(() -> {
                if (generation.get() != requestId) return;
                try {
                    if (!roomId.equals(currentRoom())) return;
                    if (addHistory && request != null && profile != null) {
                        synchronized (histories) {
                            histories.put(roomId, ChatProtocol.history(request, answer));
                            while (histories.size() > 8) histories.remove(histories.keySet().iterator().next());
                        }
                    }
                    Class<?> providerClass = XposedHelpers.findClass(
                            "com.heytap.speechassist.aichat.AIChatViewBeanProvider", loader);
                    Object provider = XposedHelpers.getStaticObjectField(providerClass, "INSTANCE");
                    Object bean = XposedHelpers.callMethod(provider, "f", recordId, roomId, answer);
                    Class<?> centerClass = XposedHelpers.findClass(
                            "com.heytap.speechassist.aichat.AIChatDataCenter", loader);
                    Object center = XposedHelpers.getStaticObjectField(centerClass, "INSTANCE");
                    XposedHelpers.callMethod(center, "s", bean);
                } catch (Throwable error) {
                    XposedBridge.log("XiaoBuSwitch: render " + error.getClass().getSimpleName());
                } finally {
                    setIdle(roomId);
                }
            });
        }

        private void setIdle(String roomId) {
            try {
                if (!roomId.equals(currentRoom())) return;
                Class<?> stateManagerClass = XposedHelpers.findClass(
                        "com.heytap.speechassist.aichat.state.AiChatStateManager", loader);
                Object companion = XposedHelpers.getStaticObjectField(stateManagerClass, "f26332g");
                Object manager = XposedHelpers.callMethod(companion, "a");
                Class<?> stateClass = XposedHelpers.findClass(
                        "com.heytap.speechassist.aichat.state.AiChatState", loader);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object idle = Enum.valueOf((Class<? extends Enum>) stateClass.asSubclass(Enum.class), "IDLE");
                XposedHelpers.callMethod(manager, "h", idle, false);
            } catch (Throwable error) {
                XposedBridge.log("XiaoBuSwitch: state " + error.getClass().getSimpleName());
            }
        }
    }
}
