package com.ryan.screenrecoder.ui.activity;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ryan.screenrecoder.R;
import com.ryan.screenrecoder.coder.MediaEncoder;


public class MainActivity extends AppCompatActivity {
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private MediaEncoder encoder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 0);

    }


    /**
     * 获取屏幕信息
     */
    private void getScreenInfo(){

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        encoder = new MediaEncoder(mediaProjection, 1080, 1920, 32);
        encoder.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        encoder.stopScreen();
    }
}
