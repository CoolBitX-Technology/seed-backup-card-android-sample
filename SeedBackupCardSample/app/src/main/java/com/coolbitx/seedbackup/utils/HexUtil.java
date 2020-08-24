
package com.coolbitx.seedbackup.utils;

import java.math.BigInteger;

import org.spongycastle.util.encoders.Hex;
/**
 *
 * @author derek
 */
public class HexUtil {


    public static String toHexString(byte[] data,int byteLength){
        return addZeroForNum(Hex.toHexString(data),byteLength*2);
    }
    public static String toHexString(int i,int byteLength){
        return String.format("%0"+byteLength*2+"X",i);
    }
    public static String toHexString(BigInteger i,int byteLength){
        return addZeroForNum(i.toString(16),byteLength*2);
    }
    
    public static byte[] toByteArray(String data){
        return Hex.decode(data);
    }

    public static int toInt(String hexData){
        return new BigInteger(hexData,16).intValue();
    }
    public static int toInt(BigInteger i){
        return i.intValue();
    }

    public static String addZeroForNum(String str, int strLength) {
        if (str.length() > strLength) {
           // assertTrue("addZeroForNum",false);
        }
        while (str.length() < strLength) {
            str = "0" + str;
        }
        return str;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String byteArrayToHexStr(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
