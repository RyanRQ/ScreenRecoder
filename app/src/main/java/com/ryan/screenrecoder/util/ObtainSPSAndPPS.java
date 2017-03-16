package com.ryan.screenrecoder.util;

/**
 * Created by zx315476228 on 17-3-15.
 */

public class ObtainSPSAndPPS {
    public static void getSPSAndPPS(byte[] fileData) {

        // 'a'=0x61, 'v'=0x76, 'c'=0x63, 'C'=0x43
        byte[] avcC = new byte[] { 0x61, 0x76, 0x63, 0x43 };

        // avcC的起始位置
        int avcRecord = 0;
        for (int ix = 0; ix < fileData.length; ++ix) {
            if (fileData[ix] == avcC[0] && fileData[ix + 1] == avcC[1]
                    && fileData[ix + 2] == avcC[2]
                    && fileData[ix + 3] == avcC[3]) {
                // 找到avcC，则记录avcRecord起始位置，然后退出循环。
                avcRecord = ix + 4;
                break;
            }
        }
        if (0 == avcRecord) {
            System.out.println("没有找到avcC，请检查文件格式是否正确");
            return;
        }

        // 加7的目的是为了跳过
        // (1)8字节的 configurationVersion
        // (2)8字节的 AVCProfileIndication
        // (3)8字节的 profile_compatibility
        // (4)8 字节的 AVCLevelIndication
        // (5)6 bit 的 reserved
        // (6)2 bit 的 lengthSizeMinusOne
        // (7)3 bit 的 reserved
        // (8)5 bit 的numOfSequenceParameterSets
        // 共6个字节，然后到达sequenceParameterSetLength的位置
        int spsStartPos = avcRecord + 6;
        byte[] spsbt = new byte[] { fileData[spsStartPos],
                fileData[spsStartPos + 1] };
        int spsLength = bytes2Int(spsbt);
        byte[] SPS = new byte[spsLength];
        // 跳过2个字节的 sequenceParameterSetLength
        spsStartPos += 2;
        System.arraycopy(fileData, spsStartPos, SPS, 0, spsLength);
        printResult("SPS", SPS, spsLength);

        // 底下部分为获取PPS
        // spsStartPos + spsLength 可以跳到pps位置
        // 再加1的目的是跳过1字节的 numOfPictureParameterSets
        int ppsStartPos = spsStartPos + spsLength + 1;
        byte[] ppsbt = new byte[] { fileData[ppsStartPos],
                fileData[ppsStartPos + 1] };
        int ppsLength = bytes2Int(ppsbt);
        byte[] PPS = new byte[ppsLength];
        ppsStartPos += 2;
        System.arraycopy(fileData, ppsStartPos, PPS, 0, ppsLength);
        printResult("PPS", PPS, ppsLength);
    }

    private static int bytes2Int(byte[] bt) {
        int ret = bt[0];
        ret <<= 8;
        ret |= bt[1];
        return ret;
    }

    private static void printResult(String type, byte[] bt, int len) {
        System.out.println(type + "长度为：" + len);
        String cont = type + "的内容为：";
        System.out.print(cont);
        for (int ix = 0; ix < len; ++ix) {
            System.out.printf("%02x ", bt[ix]);
        }
        System.out.println("\n----------");
    }
}
