package com.ryan.screenrecoder.application;

import android.app.Application;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.util.Log;

import com.ryan.screenrecoder.util.SysUtil;

import org.greenrobot.eventbus.EventBus;

import java.nio.IntBuffer;

/**
 * Created by zx315476228 on 17-3-10.
 */

public class ScreenApplication extends Application {
    private MediaProjection mediaProjection;
    private DisplayManager displayManager;
    private  static ScreenApplication screenApplication;
    public MediaProjection getMediaProjection() {
        return mediaProjection;
    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public void setDisplayManager(DisplayManager displayManager) {
        this.displayManager = displayManager;
    }

    public static ScreenApplication getInstance() {
        return screenApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        screenApplication=this;
        SysValue.api = SysUtil.getVersionCode();
        SysValue.screen_width = SysUtil.getScreenWidth(this);
        SysValue.screen_height = SysUtil.getScreenHeight(this);
        SysValue.screen_dpi = SysUtil.getScreenDpi(this);
    }
}
