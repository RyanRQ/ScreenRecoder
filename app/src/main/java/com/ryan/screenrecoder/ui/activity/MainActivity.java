package com.ryan.screenrecoder.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.ryan.screenrecoder.R;
import com.ryan.screenrecoder.coder.MediaEncoder;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback, Camera.PreviewCallback {
    private MediaProjectionManager mediaProjectionManager;
    private Button button_start;
    private Button button_end;
    private SurfaceView surfaceview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_start = ((Button) findViewById(R.id.button_start));
        button_end = ((Button) findViewById(R.id.button_end));
        surfaceview = ((SurfaceView) findViewById(R.id.surfaceview));
        button_start.setOnClickListener(this);
        button_end.setOnClickListener(this);
//        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
//        }else {
//            mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
//            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 0);
//        }
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.CAMERA},
//                    0);
//        } else {
//            initCamera();
//        }

    }


    /**
     * 初始化摄像头
     */
    private void initCamera() {
        SurfaceHolder surfaceHolder = surfaceview.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_end:
                break;
            case R.id.button_start:
                break;
        }
    }
    private MediaEncoder mediaEncoder;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        mediaEncoder=new MediaEncoder(this,mediaProjection,1080,1920,1);
//        mediaEncoder.setEncoderMode(MediaFormat.MIMETYPE_VIDEO_MPEG4);
        mediaEncoder.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 0);
//        initCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        openCamera(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private void openCamera(SurfaceHolder holder) {
        Camera camera = getCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        camera.setPreviewCallback(this);
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        int PreviewWidth = 0;
        int PreviewHeight = 0;
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);//获取窗口的管理器
        Display display = wm.getDefaultDisplay();//获得窗口里面的屏幕
        // 选择合适的预览尺寸
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        // 如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择
        if (sizeList.size() > 1) {
            Iterator<Camera.Size> itor = sizeList.iterator();
            while (itor.hasNext()) {
                Camera.Size cur = itor.next();
                if (cur.width >= PreviewWidth
                        && cur.height >= PreviewHeight) {
                    PreviewWidth = cur.width;
                    PreviewHeight = cur.height;
                    break;
                }
            }
        }
//        parameters.setPreviewFrameRate(5);
        parameters.setPictureFormat(ImageFormat.NV21);
        parameters.setPictureSize(PreviewWidth, PreviewHeight);
        Log.e("---",PreviewWidth+","+PreviewHeight);
        camera.setParameters(parameters);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
    }

    private Camera getCamera(int cameraType) {
        Camera camera = null;
        camera = Camera.open(cameraType);
        return camera;
    }
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.e("---","呵呵呵");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaEncoder.stopScreen();
    }
}
