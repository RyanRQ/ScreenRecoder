package com.ryan.screenrecoder.coder;

import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.ryan.screenrecoder.application.SysValue;
import com.ryan.screenrecoder.bean.EventLogBean;
import com.ryan.screenrecoder.glec.EGLRender;
import com.ryan.screenrecoder.util.ObtainSPSAndPPS;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ryan on 2017/2/23 0023.
 */

public class MediaEncoder extends Thread {
    private final String TAG = "MediaEncoder";

    private final String mime_type = MediaFormat.MIMETYPE_VIDEO_AVC;


    private DisplayManager displayManager;
    private MediaProjection projection;
    private MediaCodec mEncoder;
    private VirtualDisplay virtualDisplay;
    private MediaMuxer muxer;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private EGLRender eglRender;
    private Surface surface;


    //屏幕相关
    private int screen_width;
    private int screen_height;
    private int screen_dpi;


    //编码参数相关
    private int frame_bit = 2000000;//2MB
    private int frame_rate = 20;//这里指的是Mediacodec30张图为1组 ，并不是视屏本身的FPS
    private int frame_internal = 1;//关键帧间隔 一组加一个关键帧
    private final int TIMEOUT_US = 10000;
    private int video_fps = 30;
    private byte[] sps=null;
    private byte[] pps=null;


    private onScreenCallBack onScreenCallBack;

    public void setOnScreenCallBack(MediaEncoder.onScreenCallBack onScreenCallBack) {
        this.onScreenCallBack = onScreenCallBack;
    }

    public interface onScreenCallBack {
        void onScreenInfo(byte[] bytes);
        void onCutScreen(Bitmap bitmap);
    }

    public MediaEncoder(MediaProjection projection, int screen_width, int screen_height, int screen_dpi) {
        this.projection = projection;
        initScreenInfo(screen_width, screen_height, screen_dpi);
    }

    public MediaEncoder(DisplayManager displayManager, int screen_width, int screen_height, int screen_dpi) {
        this.displayManager = displayManager;
        initScreenInfo(screen_width, screen_height, screen_dpi);
    }

    private void initScreenInfo(int screen_width, int screen_height, int screen_dpi) {
        this.screen_width = screen_width;
        this.screen_height = screen_height;
        this.screen_dpi = screen_dpi;
    }

    /**
     * 设置视频FPS
     *
     * @param fps
     */
    public MediaEncoder setVideoFPS(int fps) {
        video_fps = fps;
        return this;
    }

    /**
     * 设置视屏编码采样率
     *
     * @param bit
     */
    public MediaEncoder setVideoBit(int bit) {
        frame_bit = bit;
        return this;
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
        if (projection != null) {
            virtualDisplay = projection.createVirtualDisplay("screen", screen_width, screen_height, screen_dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, eglRender.getDecodeSurface(), null, null);
        } else {
            virtualDisplay = displayManager.createVirtualDisplay("screen", screen_width, screen_height, screen_dpi,
                    eglRender.getDecodeSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC);
        }
        startRecordScreen();
        release();
    }


    /**
     * 初始化编码器
     */
    private void prepareEncoder() throws IOException {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime_type, screen_width, screen_height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, frame_bit);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frame_rate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, frame_internal);
        mEncoder = MediaCodec.createEncoderByType(mime_type);
        mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface = mEncoder.createInputSurface();
        eglRender = new EGLRender(surface, screen_width, screen_height, video_fps);
        eglRender.setCallBack(new EGLRender.onFrameCallBack() {
            @Override
            public void onUpdate() {
                startEncode();
            }

            @Override
            public void onCutScreen(Bitmap bitmap) {
                onScreenCallBack.onCutScreen(bitmap);
            }
        });
        mEncoder.start();
    }
    /**
     * 开始录屏
     */
    private void startRecordScreen() {
        eglRender.start();
        release();
    }

    private void startEncode() {
        ByteBuffer[] byteBuffers = null;
        if (SysValue.api < 21) {
            byteBuffers = mEncoder.getOutputBuffers();
        }
        int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
        if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            resetOutputFormat();
        } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
//            Log.d("---", "retrieving buffers time out!");
//            try {
//                // wait 10ms
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//            }
        } else if (index >= 0) {
            if (SysValue.api < 21) {
                encodeToVideoTrack(byteBuffers[index]);
            } else {
                encodeToVideoTrack(mEncoder.getOutputBuffer(index));
            }
            mEncoder.releaseOutputBuffer(index, false);
        }
    }

    private void encodeToVideoTrack(ByteBuffer encodeData) {

//        ByteBuffer encodeData = mEncoder.getOutputBuffer(index);
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
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
//            muxer.writeSampleData(mVideoTrackIndex, encodeData, mBufferInfo);//写入文件
            byte[] bytes;
            if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                //todo 关键帧上添加sps,和pps信息
                bytes = new byte[mBufferInfo.size + sps.length + pps.length];
                System.arraycopy(sps, 0, bytes, 0, sps.length);
                System.arraycopy(pps, 0, bytes, sps.length, pps.length);
                encodeData.get(bytes, sps.length + pps.length, mBufferInfo.size);
            } else {
                bytes = new byte[mBufferInfo.size];
                encodeData.get(bytes, 0, mBufferInfo.size);
            }
            onScreenCallBack.onScreenInfo(bytes);
            EventBus.getDefault().post(new EventLogBean("send:" + mBufferInfo.size +"\tflag:" + mBufferInfo.flags));
            Log.e("---", "send:" + mBufferInfo.size +"\tflag:" + mBufferInfo.flags);
        }
    }

    private int mVideoTrackIndex;

    private void resetOutputFormat() {
        MediaFormat newFormat = mEncoder.getOutputFormat();
        Log.i(TAG, "output format changed.\n new format: " + newFormat.toString());
        mVideoTrackIndex = muxer.addTrack(newFormat);
        getSpsPpsByteBuffer(newFormat);
        muxer.start();
        Log.i(TAG, "started media muxer, videoIndex=" + mVideoTrackIndex);
    }


    /**
     * 获取编码SPS和PPS信息
     * @param newFormat
     */
    private void getSpsPpsByteBuffer(MediaFormat newFormat) {
        sps = newFormat.getByteBuffer("csd-0").array();
        pps = newFormat.getByteBuffer("csd-1").array();
        EventBus.getDefault().post(new EventLogBean("编码器初始化完成"));
    }

    public void stopScreen() {
        if (eglRender != null) {
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
        if (muxer != null) {
            muxer.stop();
            muxer.release();
            muxer = null;
        }
    }
    public void cutScreen(){
        eglRender.cutScreen();
    }
}
