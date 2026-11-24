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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/** Hook for the ColorOS Breeno build supplied with this port. */
public final class BreenoHookEntry implements IXposedHookLoadPackage {
    private static final String TARGET_PACKAGE = "com.heytap.speechassist";
    private static final int TARGET_VERSION = 130403;
    private static final String RECORD_ID_KEY = "customized_record_id";
    private static final String STATE_MANAGER_COMPANION_FIELD = "g";
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) {
        if (!TARGET_PACKAGE.equals(load.packageName) || !TARGET_PACKAGE.equals(load.processName)) return;
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!INSTALLED.compareAndSet(false, true)) return;
                Context base = (Context) param.args[0];
                Context context = base == null ? null : (base.getApplicationContext() != null ? base.getApplicationContext() : base);
                if (context == null && param.thisObject instanceof Context) {
                    context = (Context) param.thisObject;
                }
                if (context == null) {
                    XposedBridge.log("XiaoBuSwitch: install null context");
                    return;
                }
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
                    validateRuntimeShape(helper, load.classLoader);
                    new Runtime(context, load.classLoader).install(helper);
                    report(context, "已加载小布 13.4.3 文本 Hook；请在小布 Switch 中配置文本模型", true);
                } catch (Throwable error) {
                    report(context, "小布 Hook 安装失败：" + error.getClass().getSimpleName(), false);
                    XposedBridge.log("XiaoBuSwitch: install " + error);
                }
            }
        });
    }

    private static void validateRuntimeShape(Class<?> helper, ClassLoader loader) throws Exception {
        Class<?> answer = XposedHelpers.findClass(
                "com.heytap.speechassist.aichat.bean.AIChatViewBean", loader);
        helper.getDeclaredMethod("b", String.class, String.class, Bundle.class, boolean.class);
        helper.getDeclaredMethod("d");
        helper.getDeclaredMethod("t", boolean.class, String.class, answer, Boolean.class, boolean.class, String.class);

        Class<?> room = XposedHelpers.findClass("com.heytap.speechassist.aichat.AIChatRoomIdManager", loader);
        room.getDeclaredField("INSTANCE");
        room.getDeclaredMethod("H");
        Class<?> provider = XposedHelpers.findClass("com.heytap.speechassist.aichat.AIChatViewBeanProvider", loader);
        provider.getDeclaredField("INSTANCE");
        provider.getDeclaredMethod("f", String.class, String.class, String.class);
        Class<?> dataCenter = XposedHelpers.findClass("com.heytap.speechassist.aichat.AIChatDataCenter", loader);
        dataCenter.getDeclaredField("INSTANCE");
        dataCenter.getDeclaredMethod("s", answer);

        Class<?> state = XposedHelpers.findClass("com.heytap.speechassist.aichat.state.AiChatState", loader);
        Class<?> subState = XposedHelpers.findClass("com.heytap.speechassist.aichat.state.AiChatSubState", loader);
        Class<?> stateManager = XposedHelpers.findClass(
                "com.heytap.speechassist.aichat.state.AiChatStateManager", loader);
        Class<?> stateManagerCompanion = stateManager.getDeclaredField(STATE_MANAGER_COMPANION_FIELD).getType();
        stateManagerCompanion.getDeclaredMethod("a");
        stateManager.getDeclaredMethod("h", state, boolean.class);
        stateManager.getDeclaredMethod("E", subState);

        Class<?> appContext = XposedHelpers.findClass("com.heytap.speechassist.utils.z2", loader);
        appContext.getDeclaredMethod("b");
        Class<?> preferences = XposedHelpers.findClass("com.heytap.speechassist.utils.z5", loader);
        preferences.getDeclaredMethod("q", Context.class);
        Class<?> common = XposedHelpers.findClass(
                "com.heytap.speechassist.aichat.floatwindow.utils.AiChatCommonHelperKt", loader);
        common.getDeclaredMethod("p", String.class, int.class, Object.class);
        Class<?> duplex = XposedHelpers.findClass("com.heytap.speechassist.utils.FullDuplexState", loader);
        duplex.getDeclaredField("INSTANCE");
        duplex.getDeclaredMethod("n");
        Class<?> modeManager = XposedHelpers.findClass("com.heytap.speechassist.core.w2", loader);
        Class<?> modeState = modeManager.getDeclaredMethod("a").getReturnType();
        modeState.getDeclaredMethod("S1");
        Class<?> tts = XposedHelpers.findClass(
                "com.heytap.speechassist.aichat.streamtts.AiChatTTSHighlightPlayManager", loader);
        tts.getDeclaredField("INSTANCE");
        tts.getDeclaredMethod("M");
    }

    private static void report(Context context, String text, boolean ready) {
        try {
            Bundle extras = new Bundle();
            extras.putInt("moduleVersion", ready ? BuildConfig.VERSION_CODE : 0);
            context.getContentResolver().call(ConfigProvider.URI, "status", text, extras);
        } catch (Throwable error) {
            XposedBridge.log("XiaoBuSwitch: status " + error);
        }
    }

    private static final class Runtime {
        private final Context context;
        private final ClassLoader loader;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ExecutorService worker = Executors.newFixedThreadPool(2);
        private final AtomicLong generation = new AtomicLong();
        private final AtomicReference<ActiveRequest> active = new AtomicReference<>();
        private final Map<String, JSONArray> histories = new LinkedHashMap<>();
        private long historyRevision = Long.MIN_VALUE;

        Runtime(Context context, ClassLoader loader) {
            this.context = context;
            this.loader = loader;
        }

        void install(Class<?> helper) {
            Class<?> answerBean = XposedHelpers.findClass(
                    "com.heytap.speechassist.aichat.bean.AIChatViewBean", loader);
            XposedHelpers.findAndHookMethod(helper, "t", boolean.class, String.class, answerBean,
                    Boolean.class, boolean.class, String.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    cancelForNativeStop(param);
                }
            });
            XposedHelpers.findAndHookMethod(helper, "y", String.class, int.class, Bundle.class,
                    boolean.class, boolean.class, new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            intercept(param);
                        }
                    });
        }

        private void intercept(XC_MethodHook.MethodHookParam param) {
            boolean intercepted = false;
            long requestId = -1L;
            String roomId = "";
            String recordId = "";
            ActiveRequest requestState = null;
            try {
                String text = (String) param.args[0];
                Bundle bundle = param.args[2] instanceof Bundle ? (Bundle) param.args[2] : null;
                Bundle bubbleBundle = bundle == null ? new Bundle() : new Bundle(bundle);
                boolean addQuery = Boolean.TRUE.equals(param.args[3]);
                String boxTool = bundle == null ? null : bundle.getString("extra_box_toolTag");
                XposedBridge.log("XiaoBuSwitch: y called len=" + (text == null ? -1 : text.length())
                        + " inputType=" + param.args[1] + " addQuery=" + param.args[3]
                        + " stateFlag=" + param.args[4] + " boxTool=" + boxTool);
                if (text == null || text.trim().isEmpty() || !addQuery) {
                    XposedBridge.log("XiaoBuSwitch: skip input flags");
                    return;
                }
                if (boxTool != null && !boxTool.isBlank()) {
                    XposedBridge.log("XiaoBuSwitch: skip boxTool=" + boxTool);
                    return;
                }

                JSONObject config = config();
                if (!config.optBoolean("enabled", false)) {
                    XposedBridge.log("XiaoBuSwitch: skip master disabled");
                    return;
                }
                Profile profile = ModeProfiles.resolve(config, "chat", false);
                if (!profile.enabled()) {
                    XposedBridge.log("XiaoBuSwitch: skip chat disabled");
                    return;
                }
                if (!nativeGuardsAllow(param.thisObject)) return;

                roomId = currentRoom();
                if (roomId.isEmpty()) {
                    XposedBridge.log("XiaoBuSwitch: skip empty roomId");
                    return;
                }
                XposedBridge.log("XiaoBuSwitch: intercepting provider=" + profile.provider() + " model=" + profile.model());
                String suppliedRecordId = bubbleBundle.getString(RECORD_ID_KEY);
                recordId = suppliedRecordId == null || suppliedRecordId.isEmpty()
                        ? UUID.randomUUID().toString() : suppliedRecordId;
                if (suppliedRecordId == null || suppliedRecordId.isEmpty()) {
                    bubbleBundle.putString(RECORD_ID_KEY, recordId);
                }
                bubbleBundle.putBoolean("speech_in_aichat_room_flag", true);
                if (param.args[1] instanceof Integer) {
                    bubbleBundle.putInt("input_type", (Integer) param.args[1]);
                }

                long revision = config.optLong("revision", 0L);
                JSONArray history = history(roomId, revision);
                JSONObject request = ChatProtocol.request(profile, history, text);
                requestId = generation.incrementAndGet();
                ModelClient.Cancellation cancellation = new ModelClient.Cancellation();
                requestState = new ActiveRequest(roomId, cancellation);
                ActiveRequest previous = active.getAndSet(requestState);
                if (previous != null) previous.cancellation.cancel();

                // Reuse Breeno's query bubble and generating state before suppressing its server request.
                XposedHelpers.callMethod(param.thisObject, "b", text, recordId, bubbleBundle,
                        Boolean.TRUE.equals(param.args[4]));
                param.setResult(null);
                intercepted = true;
                stopNativeTts();

                try {
                    final long currentRequestId = requestId;
                    final String currentRoomId = roomId;
                    final String currentRecordId = recordId;
                    final ActiveRequest currentRequest = requestState;
                    worker.execute(() -> ask(currentRequestId, revision, currentRoomId, currentRecordId,
                            profile, request, currentRequest));
                } catch (RuntimeException rejected) {
                    active.compareAndSet(requestState, null);
                    showAnswer(requestId, roomId, recordId, "模型请求无法启动，请稍后重试。", false, null, null);
                }
            } catch (Throwable error) {
                if (requestState != null) {
                    active.compareAndSet(requestState, null);
                    requestState.cancellation.cancel();
                }
                if (intercepted) {
                    showAnswer(requestId, roomId, recordId,
                            "模型请求失败：" + errorText(error), false, null, null);
                }
                XposedBridge.log("XiaoBuSwitch: request " + error.getClass().getSimpleName());
            }
        }

        private void cancelForNativeStop(XC_MethodHook.MethodHookParam param) {
            ActiveRequest running = active.get();
            if (running == null) return;
            try {
                Object bean = param.args[2];
                Object roomValue = bean == null ? null : XposedHelpers.callMethod(bean, "getRoomId");
                String room = roomValue instanceof String && !((String) roomValue).isEmpty()
                        ? (String) roomValue : currentRoom();
                // The stop path may not carry the request's customized ID, so scope it by active room.
                if (!running.roomId.equals(room)) return;
                if (active.compareAndSet(running, null)) {
                    generation.incrementAndGet();
                    running.cancellation.cancel();
                }
            } catch (Throwable error) {
                XposedBridge.log("XiaoBuSwitch: stop " + error.getClass().getSimpleName());
            }
        }

        private static String errorText(Throwable error) {
            String reason = error.getMessage();
            if (reason == null || reason.isBlank()) reason = "网络或模型服务异常";
            reason = reason.replace('\n', ' ').replace('\r', ' ');
            return reason.length() > 100 ? reason.substring(0, 100) : reason;
        }

        private void stopNativeTts() {
            try {
                Class<?> type = XposedHelpers.findClass(
                        "com.heytap.speechassist.aichat.streamtts.AiChatTTSHighlightPlayManager", loader);
                Object manager = XposedHelpers.getStaticObjectField(type, "INSTANCE");
                XposedHelpers.callMethod(manager, "M");
            } catch (Throwable error) {
                XposedBridge.log("XiaoBuSwitch: stop TTS " + error.getClass().getSimpleName());
            }
        }

        private boolean nativeGuardsAllow(Object engine) {
            try {
                Class<?> appContext = XposedHelpers.findClass("com.heytap.speechassist.utils.z2", loader);
                Object ctx = XposedHelpers.callStaticMethod(appContext, "b");
                if (ctx == null) ctx = this.context;
                Class<?> preferences = XposedHelpers.findClass("com.heytap.speechassist.utils.z5", loader);
                if (!Boolean.TRUE.equals(XposedHelpers.callStaticMethod(preferences, "q", new Class<?>[]{Context.class}, ctx))) {
                    XposedBridge.log("XiaoBuSwitch: guard blocked by z5.q");
                    return false;
                }

                Class<?> common = XposedHelpers.findClass(
                        "com.heytap.speechassist.aichat.floatwindow.utils.AiChatCommonHelperKt", loader);
                if (Boolean.TRUE.equals(XposedHelpers.callStaticMethod(
                        common, "p", new Class<?>[]{String.class, int.class, Object.class}, null, 1, null))) {
                    XposedBridge.log("XiaoBuSwitch: guard blocked by common.p");
                    return false;
                }

                Class<?> stateManagerClass = XposedHelpers.findClass(
                        "com.heytap.speechassist.aichat.state.AiChatStateManager", loader);
                Object companion = XposedHelpers.getStaticObjectField(stateManagerClass, STATE_MANAGER_COMPANION_FIELD);
                Object stateManager = XposedHelpers.callMethod(companion, "a");
                Class<?> subStateClass = XposedHelpers.findClass(
                        "com.heytap.speechassist.aichat.state.AiChatSubState", loader);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object dailyLimit = Enum.valueOf((Class<? extends Enum>) subStateClass.asSubclass(Enum.class), "DIALOG_LIMIT_TODAY");
                if (Boolean.TRUE.equals(XposedHelpers.callMethod(stateManager, "E", dailyLimit))) {
                    XposedBridge.log("XiaoBuSwitch: guard blocked by DIALOG_LIMIT_TODAY");
                    return false;
                }

                Class<?> duplexClass = XposedHelpers.findClass("com.heytap.speechassist.utils.FullDuplexState", loader);
                Object duplex = XposedHelpers.getStaticObjectField(duplexClass, "INSTANCE");
                if (Boolean.TRUE.equals(XposedHelpers.callMethod(duplex, "n"))) {
                    XposedBridge.log("XiaoBuSwitch: guard blocked by FullDuplexState.n");
                    return false;
                }

                Class<?> modeManagerClass = XposedHelpers.findClass("com.heytap.speechassist.core.w2", loader);
                Object modeManager = XposedHelpers.callStaticMethod(modeManagerClass, "a");
                int mode = ((Number) XposedHelpers.callMethod(modeManager, "S1")).intValue();
                if (mode == 15 || mode == 16 || mode == 21 || mode == 22) {
                    XposedBridge.log("XiaoBuSwitch: guard blocked by mode=" + mode);
                    return false;
                }
                boolean engineD = Boolean.TRUE.equals(XposedHelpers.callMethod(engine, "d"));
                if (engineD) XposedBridge.log("XiaoBuSwitch: guard blocked by engine.d");
                return !engineD;
            } catch (Throwable error) {
                XposedBridge.log("XiaoBuSwitch: native guard " + error.getClass().getSimpleName());
                XposedBridge.log(error);
                return false;
            }
        }

        private void ask(long requestId, long revision, String roomId, String recordId, Profile profile,
                         JSONObject request, ActiveRequest requestState) {
            try {
                JSONObject response = chat(profile, request, requestState.cancellation);
                String answer = ChatProtocol.answer(response);
                XposedBridge.log("XiaoBuSwitch: answer received len=" + (answer == null ? -1 : answer.length()));
                if (!isCurrent(requestId, revision)) {
                    resetIfLatest(requestId, roomId);
                    return;
                }
                showAnswer(requestId, roomId, recordId, answer, true, request, profile);
            } catch (java.util.concurrent.CancellationException ignored) {
                // A newer question or a settings change superseded this request.
            } catch (Throwable error) {
                XposedBridge.log("XiaoBuSwitch: ask failed " + error.getClass().getSimpleName() + ": " + errorText(error));
                if (!isCurrent(requestId, revision)) {
                    resetIfLatest(requestId, roomId);
                    return;
                }
                showAnswer(requestId, roomId, recordId, "模型请求失败：" + errorText(error), false, null, null);
            } finally {
                active.compareAndSet(requestState, null);
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
                if (value == null) return new JSONArray();
                try {
                    return new JSONArray(value.toString());
                } catch (org.json.JSONException error) {
                    XposedBridge.log("XiaoBuSwitch: history copy " + error.getClass().getSimpleName());
                    histories.remove(roomId);
                    return new JSONArray();
                }
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
                Object companion = XposedHelpers.getStaticObjectField(stateManagerClass, STATE_MANAGER_COMPANION_FIELD);
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

        private static final class ActiveRequest {
            final String roomId;
            final ModelClient.Cancellation cancellation;

            ActiveRequest(String roomId, ModelClient.Cancellation cancellation) {
                this.roomId = roomId;
                this.cancellation = cancellation;
            }
        }
    }
}
