package com.ryan.screenrecoder.util;

/**
 * Created by zx315476228 on 17-3-15.
 */

public class LogUtil {
    private static final int MAX_BUFFER=50;
    private static String[] buffer=new String[MAX_BUFFER];
    private static int len=0;
    public static String getInfo(String txt){
        if(len==MAX_BUFFER){
            for (int i = 1; i < MAX_BUFFER; i++) {
                buffer[i-1]=buffer[i];
            }
            len--;
        }
        buffer[len]=txt+"\n";
        len++;
        StringBuffer info=new StringBuffer();
        for (int i = 0; i < len; i++) {
            info.append(buffer[i]);
        }
        return info.toString();
    }
}
