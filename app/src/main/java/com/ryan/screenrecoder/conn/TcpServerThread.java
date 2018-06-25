package com.ryan.screenrecoder.conn;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zx315476228 on 17-3-14.
 */

public class TcpServerThread extends Thread {
    private final int PORT=6111;
    private BufferedInputStream dataInputStream;
    private onFrameCallBack callBack;
    private long timer_size;
    private boolean isStart=true;
    private Timer timer=new Timer();
    private TimerTask timerTask=new TimerTask() {
        @Override
        public void run() {
            Log.e("---","接收速度:"+(timer_size/1024)+"kb/s");
            timer_size=0;
        }
    };
    public TcpServerThread(onFrameCallBack callBack) {
        this.callBack = callBack;
    }

    public interface onFrameCallBack{
        void onFrame(byte[] data);
    }
    public void close(){
        isStart=false;
    }
    @Override
    public void run() {
        super.run();
        try {
            ServerSocket serverSocket=new ServerSocket(PORT);
            Socket clientSocket=serverSocket.accept();
            timer.schedule(timerTask,0,1000);
            dataInputStream=new BufferedInputStream(clientSocket.getInputStream());
            while (isStart){
                int readsize = dataInputStream.available();
                int ret = 0;
                if (readsize < 4) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                byte[] tmpArray = new byte[4];
                do {
                    ret += dataInputStream.read(tmpArray, ret, 4 - ret);
                } while (ret < 4);
                paseTeacherMessage(tmpArray);
            }
            if(dataInputStream!=null)
                dataInputStream.close();
            if(clientSocket!=null)
                clientSocket.close();
            if(serverSocket!=null)
                serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 数据解析
     *
     * @param data
     */
    private void paseTeacherMessage(byte[] data) {
        int size = bytesToInt(data, 0);//帧大小
        timer_size+=size;
        byte[] tmpArray = new byte[size];
        int ret=0;
        try {
            do {
                ret += dataInputStream.read(tmpArray, ret, size - ret);
            } while (ret < size);
            callBack.onFrame(tmpArray);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
    }
    public static int bytesToInt(byte[] ary, int offset) {
        int value;
        value = (int) ((ary[offset] & 0xFF)
                | ((ary[offset + 1] << 8) & 0xFF00)
                | ((ary[offset + 2] << 16) & 0xFF0000)
                | ((ary[offset + 3] << 24) & 0xFF000000));
        return value;
    }
    public static long bytes2Long(byte[] byteNum,int offset) {
        long num = 0;
        for (int ix = 0; ix < 8; ++ix) {
            num <<= 8;
            num |= (byteNum[ix+offset] & 0xff);
        }
        return num;
    }

}
