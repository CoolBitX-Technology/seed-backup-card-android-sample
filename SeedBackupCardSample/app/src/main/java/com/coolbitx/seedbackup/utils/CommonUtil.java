
package com.coolbitx.seedbackup.utils;

import java.util.Random;

/**
 *
 * @author liu
 */
public class CommonUtil {



    public static String hexRandom(int byteLength) {
        Random ran = new Random();
        byte[] temp = new byte[byteLength];
        ran.nextBytes(temp);

        return HexUtil.toHexString(temp,byteLength);
    }


}
