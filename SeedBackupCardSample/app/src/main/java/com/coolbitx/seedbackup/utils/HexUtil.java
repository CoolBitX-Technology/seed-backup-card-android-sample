
package com.coolbitx.seedbackup.utils;

import java.math.BigInteger;

import org.spongycastle.util.encoders.Hex;

public class HexUtil {

    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    static String toHexString(byte[] data, int byteLength) {
        return addZeroForNum(Hex.toHexString(data), byteLength * 2);
    }

    static String toHexString(int i, int byteLength) {
        return String.format("%0" + byteLength * 2 + "X", i);
    }

    static String toHexString(BigInteger i, int byteLength) {
        return addZeroForNum(i.toString(16), byteLength * 2);
    }

    static byte[] toByteArray(String data) {
        return Hex.decode(data);
    }

    static int toInt(String hexData) {
        return new BigInteger(hexData, 16).intValue();
    }

    static int toInt(BigInteger i) {
        return i.intValue();
    }

    static String addZeroForNum(String str, int strLength) {
        if (str.length() > strLength) {
            // assertTrue("addZeroForNum",false);
        }
        while (str.length() < strLength) {
            str = "0" + str;
        }
        return str;
    }

    static String byteArrToHexStr(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    static byte[] hexStrToByteArr(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
