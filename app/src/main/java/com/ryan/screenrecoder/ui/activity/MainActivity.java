package com.ryan.screenrecoder.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.ryan.screenrecoder.R;
import com.ryan.screenrecoder.application.SysValue;
import com.ryan.screenrecoder.coder.MediaEncoder;
import com.ryan.screenrecoder.util.SysUtil;


public class MainActivity extends AppCompatActivity {
    private final int REQUEST_CODE = 0x11;
    private final int PERMISSION_CODE = 0x12;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private MediaEncoder encoder;
    private DisplayManager displaymanager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (SysValue.api >= Build.VERSION_CODES.M) {
            getAppPermission();
        } else if (SysValue.api >= 21) {
            getMeidProjection();
        } else {
            //todo 需要root权限或系统签名
            displaymanager = ((DisplayManager) getSystemService(Context.DISPLAY_SERVICE));
            startRecording();
        }
    }

    private void getAppPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_CODE);
    }

    private void getMeidProjection() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            startRecording();
        } else {
            Toast.makeText(this, "无法录制屏幕", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecording() {
        if (SysValue.api >= 21) {
            encoder = new MediaEncoder(mediaProjection, SysValue.screen_width, SysValue.screen_height, SysValue.screen_dpi)
                    .setVideoBit(3000000)
                    .setVideoFPS(8);
        } else {
            encoder = new MediaEncoder(displaymanager, SysValue.screen_width, SysValue.screen_height, SysValue.screen_dpi)
                    .setVideoBit(3000000)
                    .setVideoFPS(5);
        }
        encoder.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getMeidProjection();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        encoder.stopScreen();
    }
}
