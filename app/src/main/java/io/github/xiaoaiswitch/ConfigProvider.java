package io.github.xiaoaiswitch;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

public final class ConfigProvider extends ContentProvider {
    public static final Uri URI = Uri.parse("content://io.github.xiaobuiswitch.config");

    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        int uid = Binder.getCallingUid();
        if (uid != Process.myUid() && uid != targetUid()) throw new SecurityException("Caller not in module scope");
        Bundle result = new Bundle();
        if ("codexCredentials".equals(method)) {
            try {
                io.github.xiaoaiswitch.core.CodexOAuth.Tokens tokens = CodexAccounts.get(getContext()).credentials(
                        extras != null && extras.getBoolean("force", false), extras == null ? null : extras.getString("previousAccess"));
                result.putString("accessToken", tokens.accessToken());
                result.putString("accountId", tokens.accountId());
                result.putLong("expiresAt", tokens.expiresAt());
            } catch (Exception error) { result.putString("error", "请在模块设置中登录或重新登录 Codex 订阅"); }
            return result;
        }
        if ("asr".equals(method)) {
            getContext().getSharedPreferences("switch", 0).edit().putLong("asrStarted", System.currentTimeMillis())
                    .putString("status", "语音识别中").putLong("seen", System.currentTimeMillis()).apply();
            return result;
        }
        if ("config".equals(method)) {
            result.putString("json", getContext().getSharedPreferences("switch", 0).getString("config", "{}"));
            return result;
        }
        if ("status".equals(method) && arg != null) {
            android.content.SharedPreferences.Editor editor = getContext().getSharedPreferences("switch", 0).edit()
                    .putString("status", arg).putLong("seen", System.currentTimeMillis())
                    .putInt("activeVersion", extras == null ? 0 : extras.getInt("moduleVersion", 0));
            editor.apply();
            return result;
        }
        throw new IllegalArgumentException("Unknown provider method");
    }

    private int targetUid() {
        try {
            return getContext().getPackageManager().getApplicationInfo("com.heytap.speechassist", 0).uid;
        } catch (PackageManager.NameNotFoundException error) {
            return -1;
        }
    }

    @Override public String getType(Uri uri) { return "application/json"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
