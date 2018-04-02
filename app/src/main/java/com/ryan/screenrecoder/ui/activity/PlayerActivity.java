package com.ryan.screenrecoder.ui.activity;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ryan.screenrecoder.R;
import com.ryan.screenrecoder.conn.TcpServerThread;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlayerActivity extends AppCompatActivity implements SurfaceHolder.Callback, TcpServerThread.onFrameCallBack {

    private SurfaceView surfaceview_play;
    private MediaCodec decoder;
    private int TIME_INTERNAL = 1000;
    private int mCount = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        surfaceview_play = ((SurfaceView) findViewById(R.id.surfaceview_play));
        surfaceview_play.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initMediaDecoder(holder.getSurface());
        new TcpServerThread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onFrame(byte[] buf) {
        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
        int inputBufferIndex = decoder.dequeueInputBuffer(0);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, 0, buf.length);
            decoder.queueInputBuffer(inputBufferIndex, 0, buf.length, mCount * 1000000 / TIME_INTERNAL, 0);
            mCount++;
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo,0);
        while (outputBufferIndex >= 0) {
            decoder.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
    private void initMediaDecoder(Surface surface) {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1080, 1920);
        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            decoder.configure(mediaFormat, surface, null, 0);
            decoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
