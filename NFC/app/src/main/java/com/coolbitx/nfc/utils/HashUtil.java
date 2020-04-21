package com.coolbitx.nfc.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.spongycastle.util.encoders.Hex;


/**
 * @author liu
 */
public class HashUtil {

    public static String SHA256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Hex.decode(data));
            byte[] messageDigest = digest.digest();

            return HexUtil.toHexString(messageDigest, 32);
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getClass().getSimpleName() + e.toString());
        }
        // assertTrue("SHA Error",false);
        return null;
    }

    public static String HMAC2512(String key, String data) {
        try {
            byte[] ret = new byte[64];
            HMAC(Hex.decode(key), 0, key.length() / 2, Hex.decode(data), 0, data.length() / 2, ret, 0, MessageDigest.getInstance("SHA-512"));
            return HexUtil.toHexString(ret, 64);
        } catch (Exception e) {
            // assertTrue("HMAC2512",false);
            return "error";
        }
    }

    private static int HMAC(byte[] key, int keyOff, int keyLength,
                            byte[] buf, int offset, int length, byte[] destbuf,
                            int destOffset, MessageDigest hash) {
        try {
            final byte IPAD = (byte) 0x36;
            final byte OPAD = (byte) 0x5c;

            short blockSize = 128;
            //if (hash == ShaUtil.sha_1) {
            //        blockSize = 64;
            //}
            byte[] workspace = new byte[blockSize + hash.getDigestLength()];

            if (keyLength > blockSize) {
                hash.update(key, keyOff, keyLength);
                hash.digest(workspace, 0, blockSize);
            } else {
                System.arraycopy(key, keyOff, workspace, 0,
                        keyLength);
            }

            // Setup IPAD secrets
            for (short ctr = (short) 0; ctr < blockSize; ctr++) {
                workspace[ctr] ^= IPAD;
            }

            // hash(i_key_pad | message)
            hash.update(workspace, 0, blockSize);
            hash.update(buf, offset, length);
            hash.digest(workspace,
                    blockSize, hash.getDigestLength());

            // transform workspace[(short)(0~blockSize)] from IPAD to OPAD
            for (short ctr = (short) 0; ctr < blockSize; ctr++) {
                workspace[ctr] ^= IPAD;
                workspace[ctr] ^= OPAD;
            }

            hash.update(workspace, 0, blockSize + hash.getDigestLength());
            hash.digest(destbuf, destOffset, hash.getDigestLength());

            return hash.getDigestLength();
        } catch (Exception e) {
            // assertTrue("HMAC",false);
            return 0;
        }
    }
}
