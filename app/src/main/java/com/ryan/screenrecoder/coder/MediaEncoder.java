package com.ryan.screenrecoder.coder;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.ryan.screenrecoder.glec.EGLRender;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ryan on 2017/2/23 0023.
 */

public class MediaEncoder extends Thread {
    private final String TAG = "MediaEncoder";

    private String mime_type = "video/avc";
    private final int FRAME_RATE = 30;//30fps
    private final int FRAME_INTERVAL = 2;//关键帧间隔，单位:s
    private final int TIMEOUT_US = 10000;

    private MediaProjection projection;
    private MediaCodec mEncoder;
    private VirtualDisplay virtualDisplay;
    private MediaMuxer muxer;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private int screen_width;
    private int screen_height;
    private int screen_dpi;
    private int screen_bit = 600000;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private EGLRender eglRender;
    private Context context;

    public MediaEncoder(Context context, MediaProjection projection, int screen_width, int screen_height, int screen_dpi) {
        this.context = context;
        this.projection = projection;
        this.screen_width = screen_width;
        this.screen_height = screen_height;
        this.screen_dpi = screen_dpi;
    }


    @Override
    public void run() {
        super.run();
        try {
            prepareEncoder();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            muxer = new MediaMuxer(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.e("sam", e.getMessage(), e);

        }
        virtualDisplay = projection.createVirtualDisplay("screen", screen_width, screen_height, screen_dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,eglRender==null?surface: eglRender.getDecodeSurface(), null, null);
        startRecordScreen();
        release();
    }
    private Surface surface;
    /**
     * 初始化编码器
     */
    private void prepareEncoder() throws IOException {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime_type, screen_width, screen_height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, screen_bit);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FRAME_INTERVAL);
        mEncoder = MediaCodec.createEncoderByType(mime_type);
        mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface=mEncoder.createInputSurface();
        eglRender = new EGLRender(surface,context);
        eglRender.setCallBack(new EGLRender.onFrameCallBack() {
            @Override
            public void onUpdate() {
                startEncode();
            }
        });
        mEncoder.start();

    }

    /**
     * 开始录屏
     */
    private void startRecordScreen() {
        if(eglRender!=null){
            eglRender.start();
        }else {
            while (!mQuit.get()){
                startEncode();
            }
        }
        release();
    }
    private void startEncode(){
        int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
        if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            resetOutputFormat();
        } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.d("---", "retrieving buffers time out!");
            try {
                // wait 10ms
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        } else if (index >= 0) {
            encodeToVideoTrack(index);
            mEncoder.releaseOutputBuffer(index, false);
        }
    }
    private void encodeToVideoTrack(int index) {
        Log.e("---", "有了输出数据");
        ByteBuffer encodeData = mEncoder.getOutputBuffer(index);
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // Ignore it.
            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            mBufferInfo.size = 0;
        }
        if (mBufferInfo.size == 0) {
            Log.d(TAG, "info.size == 0, drop it.");
            encodeData = null;
        } else {
            Log.d(TAG, "got buffer, info: size=" + mBufferInfo.size
                    + ", presentationTimeUs=" + mBufferInfo.presentationTimeUs
                    + ", offset=" + mBufferInfo.offset);
        }
        if (encodeData != null) {

            encodeData.position(mBufferInfo.offset);
            encodeData.limit(mBufferInfo.offset + mBufferInfo.size);
            muxer.writeSampleData(mVideoTrackIndex, encodeData, mBufferInfo);//写入文件
            Log.i(TAG, "位置:" + mVideoTrackIndex + "\tsend:" + mBufferInfo.size + " bytes to muxer...");
        }
    }

    private int mVideoTrackIndex;

    private void resetOutputFormat() {
        // should happen before receiving buffers, and should only happen once
//        if (mMuxerStarted) {
//            throw new IllegalStateException("output format already changed!");
//        }
        MediaFormat newFormat = mEncoder.getOutputFormat();
        Log.i(TAG, "output format changed.\n new format: " + newFormat.toString());
        mVideoTrackIndex = muxer.addTrack(newFormat);
        muxer.start();
//        mMuxerStarted = true;
        Log.i(TAG, "started media muxer, videoIndex=" + mVideoTrackIndex);
    }

    public void stopScreen() {
        mQuit.set(true);
        if(eglRender!=null){
        eglRender.stop();
        }
    }
    public void release() {

        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
//        if ( != null) {
//            mMediaProjection.stop();
//        }
        if (muxer != null) {
            muxer.stop();
            muxer.release();
            muxer = null;
        }
    }
}
