package com.ryan.screenrecoder.ui.activity;

import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ryan.screenrecoder.R;
import com.ryan.screenrecoder.application.ScreenApplication;
import com.ryan.screenrecoder.application.SysValue;
import com.ryan.screenrecoder.bean.EventLogBean;
import com.ryan.screenrecoder.coder.MediaEncoder;
import com.ryan.screenrecoder.conn.TcpSendThread;
import com.ryan.screenrecoder.util.LogUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class TcpSendActivity extends AppCompatActivity implements TcpSendThread.onSendCallBack, MediaEncoder.onScreenCallBack {
    private final int BIT = 3000000;
    private final int FPS = 6;

    private MediaEncoder encoder;

    private TcpSendThread tcpSendThread;
    private TextView textview_info;
    private Button button_cutscreen;
    private ImageView imageview_show;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                FileOutputStream outputStream = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/111.png");
                Bitmap bitmap = (Bitmap) msg.obj;
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            imageview_show.setImageBitmap((Bitmap) msg.obj);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcp_send);
        button_cutscreen = ((Button) findViewById(R.id.button_cutscreen));
        imageview_show = ((ImageView) findViewById(R.id.imageview_show));
        textview_info = ((TextView) findViewById(R.id.textview_info));
        EventBus.getDefault().register(this);
        tcpSendThread = new TcpSendThread(this);
        tcpSendThread.setIp(getIntent().getStringExtra("ip"));
        tcpSendThread.start();
        button_cutscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encoder.cutScreen();
            }
        });
    }

    private void startRecording() {
        if (SysValue.api >= 21) {
            MediaProjection mediaProjection = ScreenApplication.getInstance().getMediaProjection();
            if (mediaProjection != null) {
                encoder = new MediaEncoder(mediaProjection, 1080, 1920, SysValue.screen_dpi)
                        .setVideoBit(BIT)
                        .setVideoFPS(FPS);
            }
        } else {
            DisplayManager displayManager = ScreenApplication.getInstance().getDisplayManager();
            if (displayManager != null) {
                encoder = new MediaEncoder(displayManager, SysValue.screen_width, SysValue.screen_height, SysValue.screen_dpi)
                        .setVideoBit(BIT)
                        .setVideoFPS(FPS);
            }
        }
        if (encoder != null) {
            encoder.setOnScreenCallBack(this);
            encoder.start();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventLogBean logBean) {
        textview_info.setText(LogUtil.getInfo(logBean.getLog()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        encoder.stopScreen();//关闭截屏
        tcpSendThread.close();//关闭连接
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onConnSuccess() {
        startRecording();
    }

    @Override
    public void onScreenInfo(byte[] bytes) {
        tcpSendThread.sendMessage(bytes);
    }

    @Override
    public void onCutScreen(Bitmap bitmap) {
//        try {
//                    FileOutputStream outputStream=new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath()+"/cut.png");
//                    encoder.cutScreen().compress(Bitmap.CompressFormat.PNG,100,outputStream);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
        handler.obtainMessage(0, bitmap).sendToTarget();
    }


}
