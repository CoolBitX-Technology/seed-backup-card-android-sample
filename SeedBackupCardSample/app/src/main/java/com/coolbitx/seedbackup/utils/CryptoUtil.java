
package com.coolbitx.seedbackup.utils;

import org.spongycastle.util.encoders.Hex;

import java.nio.ByteBuffer;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class CryptoUtil {
    private static final String TAG = CryptoUtil.class.getSimpleName();

    static String encryptAES(String key, String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = ByteBuffer.allocate(16).putInt(0).array();
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec encryptSpec = new SecretKeySpec(Hex.decode(key), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, encryptSpec, ivSpec);
            String ciphertext = Hex.toHexString(cipher.doFinal(Hex.decode(plain)));
            return ciphertext;
        } catch (Exception e) {
//             assertTrue("encryptAES",false);
            return "Error!" + e.toString();
        }
    }

    static String decryptAES(String key, String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = ByteBuffer.allocate(16).putInt(0).array();
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec decryptSpec = new SecretKeySpec(Hex.decode(key), "AES");
            cipher.init(Cipher.DECRYPT_MODE, decryptSpec, ivSpec);
            String ciphertext = Hex.toHexString(cipher.doFinal(Hex.decode(plain)));
            return ciphertext;
        } catch (Exception e) {
//             assertTrue("decryptAES",false);
            return "Error! " + e.toString();
        }
    }

}
