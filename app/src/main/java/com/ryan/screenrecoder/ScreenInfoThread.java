package com.ryan.screenrecoder;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by win on 2017/2/23 0023.
 */

public class ScreenInfoThread extends Thread {
    private final String MIME_TYPE="video/avc";
    private final int FRAME_RATE=30;//30fps
    private final int FRAME_INTERVAL=10;//关键帧间隔，单位:s
    private final int TIMEOUT_US=10000;

    private MediaProjection projection;
    private MediaCodec mEncoder;
    private Surface surface;
    private VirtualDisplay virtualDisplay;
    private MediaMuxer mediaMuxer;
    private MediaCodec.BufferInfo mediaBufferInfo=new MediaCodec.BufferInfo();
    private int screen_width;
    private int screen_height;
    private int screen_dpi;
    private int screen_bit=8;

    public ScreenInfoThread(MediaProjection projection,int screen_width,int screen_height,int screen_dpi){
        this.projection= projection;
        this.screen_width=screen_width;
        this.screen_height=screen_height;
        this.screen_dpi=screen_dpi;
    }

    @Override
    public void run() {
        super.run();
        try {
            prepareEncoder();
        } catch (IOException e) {
            e.printStackTrace();
        }
        virtualDisplay=projection.createVirtualDisplay("screen",screen_width,screen_height,screen_dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,surface,null,null);
    }

    /**
     * 初始化编码器
     */
    private void prepareEncoder() throws IOException {
        MediaFormat mediaFormat=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_VIDEO_AVC,screen_width,screen_height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,screen_bit);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,FRAME_INTERVAL);
        mEncoder=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mEncoder.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface=mEncoder.createInputSurface();
        mEncoder.start();
    }
    /**
     * 开始录屏
     */
    private void startRecordScreen(){
        int index=mEncoder.dequeueOutputBuffer(mediaBufferInfo,TIMEOUT_US);
        if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){

        }else if(index==MediaCodec.INFO_TRY_AGAIN_LATER){

        }else if(index>=0){
            encodeToVideoTrack(index);
        }
    }
    private void encodeToVideoTrack(int index){
        ByteBuffer encodeData=mEncoder.getOutputBuffer(index);
    }
}
