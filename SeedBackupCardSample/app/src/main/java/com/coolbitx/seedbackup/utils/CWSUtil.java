package com.coolbitx.seedbackup.utils;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.coolbitx.seedbackup.utils.HexUtil.bytesToHex;
import static com.coolbitx.seedbackup.utils.HexUtil.hexStringToByteArray;

public class CWSUtil {

    private static String secureKey = null;

    private static final String GenuineMasterChainCode_NonInstalled = "611c6956ca324d8656b50c39a0e5ef968ecd8997e22c28c11d56fb7d28313fa3";
    private static final String GenuineMasterPublicKey_NonInstalled = "04e720c727290f3cde711a82bba2f102322ab88029b0ff5be5171ad2d0a1a26efcd3502aa473cea30db7bc237021d00fd8929123246a993dc9e76ca7ef7f456ade";
    private static final String GenuineMasterChainCode_Test = "f5a0c5d9ffaee0230a98a1cc982117759c149a0c8af48635776135dae8f63ba4";
    private static final String GenuineMasterPublicKey_Test = "0401e3a7de779276ef24b9d5617ba86ba46dc5a010be0ce7aaf65876402f6a53a5cf1fecab85703df92e9c43e12a49f33370761153216df8291b7aa2f1a775b086";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static IsoDep techHandle = null;

    private interface Commands {
        String BACKUP = "80320500";
        String RESTORE = "80340000";
        String RESET = "80360000";
        String SECURE_CHANNEL = "80CE000041";
    }

    public interface ErrorCode {
        String SUCCESS = "9000";
        String RESET_FIRST = "6330";
        String NO_DATA = "6370";
        String PIN_CODE_NOT_MATCH = "6350";
        String CARD_IS_LOCKED = "6390";
    }

    private static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) return null;

        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String byteArrayToStr(byte[] byteArray) {

        if (byteArray == null) return null;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int i = 0; i < byteArray.length; i++) {
            int tmp = byteArray[i];
            if (tmp > 0) output.write(byteArray[i]);
        }
        byte[] newArray = output.toByteArray();


        String str;
        try {
            str = new String(newArray, "UTF-8");
        } catch (IOException ex) {
            str = ex.toString();
        }

        return str;
    }

    public static String getSHA256StrJava(String str) {

        String encodeStr = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
            encodeStr = HexUtil.bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encodeStr;
    }

    public static String backup(String data, String pinCode, Tag tag) {
        String apduHeader = CWSUtil.Commands.BACKUP;  //max 255   05 tobyte
        String command = "";
        byte[] dataBytes = data.getBytes();
        String hexData = bytesToHex(dataBytes);
        String HashPinCode = getSHA256StrJava(pinCode);
        command = HashPinCode + hexData;
        return sendCmdWithSecureChannel(apduHeader, command, tag);
    }

    public static String restore(String pinCode, Tag tag) {
        String apduHeader = Commands.RESTORE;
        String command = "";
        String HashPinCode = getSHA256StrJava(pinCode);
        command = command + HashPinCode;
        return sendCmdWithSecureChannel(apduHeader, command, tag);
    }

    public static String reset(Tag tag) {
        return sendCmdWithSecureChannel(Commands.RESET, "", tag);
    }

    public static void getTriesRemaining(Tag tag) {

    }

    private static String sendCmdWithSecureChannel(String apduHeader, String cmd, Tag tag) {

        try {
            String sessionAppPrivateKey = CommonUtil.hexRandom(32);
            String sessionAppPublicKey = KeyUtil.getPublicKey(sessionAppPrivateKey);
            String command = Commands.SECURE_CHANNEL;
            command = command + sessionAppPublicKey;
            System.out.println("command:" + command);
            byte[] bytes = hexStringToByteArray(command);
            techHandle = IsoDep.get(tag);
            if (techHandle.isConnected()) techHandle.close();
            techHandle.connect();
            byte[] resultBytes = techHandle.transceive(bytes);

            String ret = byteArrayToHexStr(resultBytes);
            System.out.println("result" + ret);
            if (ret.length() == 4) {
                return "error" + ret;
            }
            String installType = ret.substring(0, 4);
            int cardNameLength = HexUtil.toInt(ret.substring(4, 8));
            String cardNameHex = ret.substring(8, 8 + cardNameLength * 2);
            ret = ret.substring(8 + cardNameLength * 2, ret.length());
            String nonceIndex = ret.substring(0, 64);
            String GenuineMasterPublicKey = null;
            String GenuineMasterChainCode = null;
            switch (installType) {
                case "0000":
                    GenuineMasterPublicKey = GenuineMasterPublicKey_NonInstalled;
                    GenuineMasterChainCode = GenuineMasterChainCode_NonInstalled;
                    break;
                case "0001":
                    GenuineMasterPublicKey = GenuineMasterPublicKey_Test;
                    GenuineMasterChainCode = GenuineMasterChainCode_Test;
                    break;
                // add case "0002" here for real HSM key.
                default:
                    throw new Exception("Error");
            }

            String GenuineChild1PublicKey = KeyUtil.getChildPublicKey(GenuineMasterPublicKey, GenuineMasterChainCode, cardNameHex);
            String GenuineChild1ChainCode = KeyUtil.getChildChainCode(GenuineMasterPublicKey, GenuineMasterChainCode, cardNameHex);
            String GenuineChild2PublicKey = KeyUtil.getChildPublicKey(GenuineChild1PublicKey, GenuineChild1ChainCode, nonceIndex);

            secureKey = KeyUtil.getEcdhKey(GenuineChild2PublicKey, sessionAppPrivateKey);
            String[] apduCommand = sendSecureInner(apduHeader, cmd);
            int blockNumber = apduCommand.length;
            String[] apduResult = new String[blockNumber];
            String tmp = "";
            for (int i = 0; i < blockNumber; i++) {
                System.out.println("apduCommand[" + (i + 1) + "/" + blockNumber + "]:" + apduCommand[i]);
                byte[] bcmd = HexUtil.toByteArray(apduCommand[i]);
                byte[] resultByte = techHandle.transceive(bcmd);
                apduResult[i] = HexUtil.toHexString(resultByte, resultByte.length);
                String result = byteArrayToHexStr(resultByte);
                System.out.println("rtn:" + byteArrayToHexStr(resultByte));

                if (result.length() == 4) {  //cmd
                    tmp = tmp + byteArrayToHexStr(resultByte);
                } else {
                    String resultSN = resultSecureInner(apduResult[i]);
                    if (resultSN.length() == 4) {
                        tmp = tmp + resultSecureInner(apduResult[i]);

                    } else {
                        tmp = tmp + byteArrayToStr(hexStringToByteArray(resultSecureInner(apduResult[i])));

                    }
                }
            }

            switch (tmp) {
                case ErrorCode.SUCCESS: {
                    tmp = ErrorCode.SUCCESS;
                    break;
                }
                case ErrorCode.RESET_FIRST: {
                    tmp = ErrorCode.RESET_FIRST;
                    break;
                }
                case ErrorCode.NO_DATA: {
                    tmp = ErrorCode.NO_DATA;
                    break;
                }
                case ErrorCode.PIN_CODE_NOT_MATCH: {
                    tmp = ErrorCode.PIN_CODE_NOT_MATCH;
                    break;
                }
                case ErrorCode.CARD_IS_LOCKED: {
                    tmp = ErrorCode.CARD_IS_LOCKED;
                    break;
                }
            }

            return tmp;


        } catch (Exception ex) {
            Log.e("ex", ex.toString());
            // showResult("error:" + ex.toString());
        } finally {
            try {
                techHandle.close();
            } catch (Exception ex) {
                // showResult("error:" + ex.toString());
            }
        }
        return null;
    }

    private static String[] sendSecureInner(String apduHeader, String apduData) {
        /*
        add salt and checksum, then encrypt origin header & data into a cipher
        real command to be send = 80CCXXXX + cipherData
        cipherData = cipherVersion + AES(apduHeader+hash+salt+apduData) with secureKey
        cipherVersion = 0x00 in this version
        apduHeader = 4Bytes of original APDU header, CLS+INS+P1+P2, e.g. 0x80520000
        apduData = original APDU payload with any length any content
        salt = 4Bytes of random data
        hash = SHA256(apduHeader+salt+apduData), 32Bytes
        '+' means concatenate
        */
        String salt = CommonUtil.hexRandom(4);
        String hash = HashUtil.SHA256(apduHeader + salt + apduData);
        String cipherData = "00" + CryptoUtil.encryptAES(secureKey, apduHeader + hash + salt + apduData);
        System.out.println("secureKey:" + secureKey + " (" + (secureKey.length() / 2) + "Bytes)");
        System.out.println("securePln:" + apduHeader + " " + apduData + " (" + (apduData.length() / 2) + "Bytes) +" + salt + "," + hash);
        System.out.println("secureCph:" + cipherData + " (" + (cipherData.length() / 2) + "Bytes)");

        /*
        Divide cipherData into parts if it's too long to be send in one command
        e.g.80CC0003 + cipherData(1/3)
            80CC0103 + cipherData(2/3)
            80CC0203 + cipherData(3/3)
        80CC is the CLA & INS of SecureChannel
        P1 = index of this part, start from 00
        P2 = total number of parts
        If no dividing needed, use 80CC0001
        Length of each parts could be 0 <= x <= 250, don't need to be equal
        */

        // casually chosen blockSize, this could be 1~250
        int blockSize = 240;
        // number of parts, ceil(length/size)
        int blockNumber = (cipherData.length() / 2 - 1) / blockSize + 1;

        String[] apduCommand = new String[blockNumber];

        for (int i = 0; i < blockNumber - 1; i++) {
            String commandHeader = "80CC" + HexUtil.toHexString(i, 1) + HexUtil.toHexString(blockNumber, 1);
            String commandData = cipherData.substring(i * blockSize * 2, (i + 1) * blockSize * 2);
            String commandLength = HexUtil.toHexString(commandData.length() / 2, 1);
            System.out.println("command:" + commandHeader + " " + commandData + " (" + (commandData.length() / 2) + "Bytes)");
            apduCommand[i] = commandHeader + commandLength + commandData;
        }
        String commandHeader = "80CC" + HexUtil.toHexString(blockNumber - 1, 1) + HexUtil.toHexString(blockNumber, 1);
        String commandData = cipherData.substring((blockNumber - 1) * blockSize * 2, cipherData.length());
        String commandLength = HexUtil.toHexString(commandData.length() / 2, 1);
        apduCommand[blockNumber - 1] = commandHeader + commandLength + commandData;
        System.out.println("command:" + commandHeader + " " + commandData + " (" + (commandData.length() / 2) + "Bytes)");
        return apduCommand;
    }

    private static String resultSecureInner(String apduResult) {
        //int blockNumber = apduResult.length;

        String rtn = apduResult;
        if (rtn.substring(rtn.length() - 4, rtn.length()).equalsIgnoreCase("9000")) {
            String decrypted = CryptoUtil.decryptAES(secureKey, rtn.substring(0, rtn.length() - 4));
            String decryptedHash = decrypted.substring(0, 64);
            String decryptedSalt = decrypted.substring(64, 72);
            String decryptedData = decrypted.substring(72, decrypted.length());

            // reassemble returnData to origin form
            rtn = decryptedData + "9000";
        } else if (rtn.substring(rtn.length() - 4, rtn.length()).equalsIgnoreCase("6350")) {
            String decrypted = CryptoUtil.decryptAES(secureKey, rtn.substring(0, rtn.length() - 4));
            String decryptedHash = decrypted.substring(0, 64);
            String decryptedSalt = decrypted.substring(64, 72);
            String decryptedData = decrypted.substring(72, decrypted.length());

            // reassemble returnData to origin form
            rtn = decryptedData + "6350";
        }
        System.out.println("result rtn" + rtn);

        return rtn;
    }
}
