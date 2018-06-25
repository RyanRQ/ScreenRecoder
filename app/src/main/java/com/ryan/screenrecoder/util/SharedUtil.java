package com.ryan.screenrecoder.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedUtil {
    private static SharedUtil preferenceUtil;
    private SharedPreferences shareditorPreferences;

    private SharedUtil(Context context) {
        if (shareditorPreferences == null) {
            shareditorPreferences = context.getSharedPreferences(
                    "screenRecoder", Context.MODE_PRIVATE);
        }
    }

    public static SharedUtil init(Context context) {
        if (preferenceUtil == null)
            preferenceUtil = new SharedUtil(context);
        return preferenceUtil;
    }

    public String getIp() {
        return shareditorPreferences.getString("ip", "");
    }

    public void setIp(String ip) {
        shareditorPreferences.edit().putString("ip", ip).apply();
    }
}
