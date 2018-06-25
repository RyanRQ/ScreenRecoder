package com.ryan.screenrecoder.conn;

import android.util.Log;

import com.ryan.screenrecoder.bean.EventLogBean;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by zx315476228 on 17-3-14.
 */

public class TcpSendThread extends Thread {
    private final int port = 6111;
    private String ip = "192.168.0.132";
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
    private OnConnCallBack onSendCallBack;
    private boolean isRuning;
    private Socket socket;

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setOnSendCallBack(OnConnCallBack onSendCallBack) {
        this.onSendCallBack = onSendCallBack;
    }

    public interface OnConnCallBack {
        void onConnSuccess(String ip);
    }

    public TcpSendThread(OnConnCallBack onSendCallBack) {
        this.onSendCallBack = onSendCallBack;
    }

    @Override
    public void run() {
        super.run();
        try {
            EventBus.getDefault().post(new EventLogBean("等待连接"));
            socket = new Socket(ip, port);
            Log.e("---", "连接成功");
            EventBus.getDefault().post(new EventLogBean("连接成功!!!"));
            inputStream = new BufferedInputStream(socket.getInputStream());
            outputStream = new BufferedOutputStream(socket.getOutputStream());
            onSendCallBack.onConnSuccess(ip);
            isRuning = true;
            while (isRuning) {
                int readSize = inputStream.available();
                if (readSize < 4) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        close();
    }

    private void closeConn() {

        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(byte[] data) {
        byte[] content = new byte[data.length + 4];
        System.arraycopy(intToBytes(data.length), 0, content, 0, 4);
        System.arraycopy(data, 0, content, 4, data.length);
        try {
            outputStream.write(content, 0, content.length);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] intToBytes(int value) {
        byte[] byte_src = new byte[4];
        byte_src[3] = (byte) ((value & 0xFF000000) >> 24);
        byte_src[2] = (byte) ((value & 0x00FF0000) >> 16);
        byte_src[1] = (byte) ((value & 0x0000FF00) >> 8);
        byte_src[0] = (byte) ((value & 0x000000FF));
        return byte_src;
    }

    public static byte[] long2Bytes(long num) {
        byte[] byteNum = new byte[8];
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((num >> offset) & 0xff);
        }
        return byteNum;
    }

    public void close() {
        isRuning = false;
    }
}
