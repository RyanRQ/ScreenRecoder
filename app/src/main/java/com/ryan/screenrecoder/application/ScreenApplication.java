package com.ryan.screenrecoder.application;

import android.app.Application;
import android.util.Log;

import com.ryan.screenrecoder.util.SysUtil;

/**
 * Created by zx315476228 on 17-3-10.
 */

public class ScreenApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SysValue.api=SysUtil.getVersionCode();
        SysValue.screen_width=SysUtil.getScreenWidth(this);
        SysValue.screen_height=SysUtil.getScreenHeight(this);
        SysValue.screen_dpi=SysUtil.getScreenDpi(this);
    }
}
