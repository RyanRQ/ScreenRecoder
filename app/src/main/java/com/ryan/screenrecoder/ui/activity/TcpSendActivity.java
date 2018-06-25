package com.ryan.screenrecoder.ui.activity;

import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import com.ryan.screenrecoder.R;
import com.ryan.screenrecoder.application.ScreenApplication;
import com.ryan.screenrecoder.application.SysValue;
import com.ryan.screenrecoder.bean.EventLogBean;
import com.ryan.screenrecoder.coder.MediaEncoder;
import com.ryan.screenrecoder.conn.TcpSendThread;
import com.ryan.screenrecoder.util.SharedUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;


public class TcpSendActivity extends AppCompatActivity implements TcpSendThread.OnConnCallBack, MediaEncoder.OnScreenCallBack {
    private static final int HANDLER_CONN_SUCCESS = 0;
    private static final int HANDLER_CUT_SCRENN_SUCCESS = 1;

    private static final int BIT = 3000000;
    private static final int FPS = 8;//FPS

    private MediaEncoder encoder;

    private TcpSendThread tcpSendThread;
    private MHandler handler = new MHandler(this);

    private static class MHandler extends Handler {
        private final WeakReference<TcpSendActivity> mActivity;

        MHandler(TcpSendActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_CUT_SCRENN_SUCCESS:
                    try {
                        FileOutputStream outputStream = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/111.png");
                        Bitmap bitmap = (Bitmap) msg.obj;
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                        Toast.makeText(mActivity.get(), "截图已保存至:sdCard/111.png", Toast.LENGTH_LONG).show();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;
                case HANDLER_CONN_SUCCESS:
                    Toast.makeText(mActivity.get(), "连接成功，开始录屏", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private VideoView videoView_test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_tcp_send);
        videoView_test = ((VideoView) findViewById(R.id.videoView_test));


        String uri = "android.resource://" + getPackageName() + "/" + R.raw.test;
        videoView_test.setVideoURI(Uri.parse(uri));
        videoView_test.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                mediaPlayer.setLooping(true);
            }
        });

         findViewById(R.id.button_cutscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (encoder != null)
                    encoder.cutScreen();
            }

        });

        tcpSendThread = new TcpSendThread(this);
        tcpSendThread.setIp(getIntent().getStringExtra("ip"));
        tcpSendThread.start();
    }

    /**
     * 连接成功后开始录屏
     */
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
        videoView_test.start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventLogBean logBean) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tcpSendThread.close();//关闭连接
        if (encoder != null)
            encoder.stopScreen();//关闭截屏
        if (videoView_test.isPlaying())
            videoView_test.stopPlayback();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onConnSuccess(String ip) {
        startRecording();
        SharedUtil.init(this).setIp(ip);
        handler.sendEmptyMessage(HANDLER_CONN_SUCCESS);

    }

    @Override
    public void onScreenInfo(byte[] bytes) {
        tcpSendThread.sendMessage(bytes);
    }

    @Override
    public void onCutScreen(Bitmap bitmap) {
        handler.obtainMessage(HANDLER_CUT_SCRENN_SUCCESS, bitmap).sendToTarget();
    }
}
