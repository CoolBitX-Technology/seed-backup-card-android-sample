
package com.coolbitx.seedbackupcard_sdk;

import java.util.Random;

class CommonUtil {
    static String hexRandom(int byteLength) {
        Random ran = new Random();
        byte[] temp = new byte[byteLength];
        ran.nextBytes(temp);

        return HexUtil.toHexString(temp, byteLength);
    }
}
