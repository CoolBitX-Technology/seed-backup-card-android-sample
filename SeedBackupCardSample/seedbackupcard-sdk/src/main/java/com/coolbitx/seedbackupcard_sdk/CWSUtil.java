package com.coolbitx.seedbackupcard_sdk;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.coolbitx.seedbackupcard_sdk.HexUtil.byteArrToHexStr;
import static com.coolbitx.seedbackupcard_sdk.HexUtil.hexStrToByteArr;

public class CWSUtil {
    private static final String TAG = CWSUtil.class.getSimpleName();
    private static String secureKey = null;

    private static final String GenuineMasterChainCode_NonInstalled = "611c6956ca324d8656b50c39a0e5ef968ecd8997e22c28c11d56fb7d28313fa3";
    private static final String GenuineMasterPublicKey_NonInstalled = "04e720c727290f3cde711a82bba2f102322ab88029b0ff5be5171ad2d0a1a26efcd3502aa473cea30db7bc237021d00fd8929123246a993dc9e76ca7ef7f456ade";
    private static final String GenuineMasterChainCode_Test = "f5a0c5d9ffaee0230a98a1cc982117759c149a0c8af48635776135dae8f63ba4";
    private static final String GenuineMasterPublicKey_Test = "0401e3a7de779276ef24b9d5617ba86ba46dc5a010be0ce7aaf65876402f6a53a5cf1fecab85703df92e9c43e12a49f33370761153216df8291b7aa2f1a775b086";

    private static IsoDep techHandle = null;

    private interface Command {
        String BACKUP = "80320500";
        String GET_CARD_INFO = "80380000";
        String GET_NEXT_PARTIAL_ENCRYPTED = "80C20000";
        String RESET = "80360000";
        String RESTORE = "80340000";
        String SECURE_CHANNEL = "80CE000041";
    }

    public interface ResultCode {
        String SUCCESS = "9000";
        String RESET_FIRST = "6330";
        String NO_DATA = "6370";
        String PIN_CODE_NOT_MATCH = "6350";
        String CARD_IS_LOCKED = "6390";
    }

    private static String getSHA256StrJava(String str) {

        String encodeStr = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
            encodeStr = byteArrToHexStr(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encodeStr;
    }

    public static String backup(String data, String pinCode, Tag tag) {
        String apduHeader = Command.BACKUP;  //max 255   05 tobyte
        String command;
        byte[] dataBytes = data.getBytes();
        String hexData = byteArrToHexStr(dataBytes);
        String hashedPinCode = getSHA256StrJava(pinCode);
        command = hashedPinCode + hexData;
        return sendCmdWithSecureChannel(apduHeader, command, tag);
    }

    public static String restore(String pinCode, Tag tag) {
        String apduHeader = Command.RESTORE;
        String hashedPinCode = getSHA256StrJava(pinCode);
        return sendCmdWithSecureChannel(apduHeader, hashedPinCode, tag);
    }

    /**
     * erase the content of the card
     *
     * @param tag nfc tag
     * @return result code
     */
    public static String reset(Tag tag) {
        return sendCmdWithSecureChannel(Command.RESET, "", tag);
    }

    public static String check(Tag tag) {
        return sendCmdWithSecureChannel(Command.GET_CARD_INFO, "", tag);
    }

    private static String sendCmdWithSecureChannel(String apduHeader, String cmd, Tag tag) {

        try {
            String sessionAppPrivateKey = CommonUtil.hexRandom(32);
            String sessionAppPublicKey = KeyUtil.getPublicKey(sessionAppPrivateKey);
            String command = Command.SECURE_CHANNEL;
            command = command + sessionAppPublicKey;
            byte[] commandBytes = hexStrToByteArr(command);

            techHandle = IsoDep.get(tag);
            if (techHandle.isConnected()) techHandle.close();
            techHandle.connect();
            byte[] resultBytes = techHandle.transceive(commandBytes);

            String ret = byteArrToHexStr(resultBytes);
            if (ret.length() == 4) {
                return ret;
            }
            String installType = ret.substring(0, 4);
            int cardNameLength = HexUtil.toInt(ret.substring(4, 8));
            String cardNameHex = ret.substring(8, 8 + cardNameLength * 2);
            ret = ret.substring(8 + cardNameLength * 2);
            String nonceIndex = ret.substring(0, 8);

//            Log.i(TAG, "The rest of ret: " + ret.substring(8, 40));
//            String validationData = ret.substring(8, 40);
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
            //String cardName = new String(Hex.decode(cardNameHex));
            String cardNameHash = HashUtil.sha256(cardNameHex);
            String firstIndex = HexUtil.toHexString(HexUtil.toInt(cardNameHash.substring(0, 2)) & 0x7f, 1) + cardNameHash.substring(2, 8);

            String GenuineChild1PublicKey = KeyUtil.getChildPublicKey(GenuineMasterPublicKey, GenuineMasterChainCode, firstIndex);
            String GenuineChild1ChainCode = KeyUtil.getChildChainCode(GenuineMasterPublicKey, GenuineMasterChainCode, firstIndex);
            String GenuineChild2PublicKey = KeyUtil.getChildPublicKey(GenuineChild1PublicKey, GenuineChild1ChainCode, nonceIndex);

            secureKey = KeyUtil.getEcdhKey(GenuineChild2PublicKey, sessionAppPrivateKey);
//            Log.i(TAG,"CryptoUtil.decryptAES: " + CryptoUtil.decryptAES(secureKey, validationData));
            String[] apduCommand = sendSecureInner(apduHeader, cmd);

            int blockNumber = apduCommand.length;

            String[] apduResult = new String[blockNumber];

            StringBuilder result = new StringBuilder();

            for (int i = 0; i < blockNumber; i++) {
                Log.i(TAG, "apduCommand[" + (i + 1) + "/" + blockNumber + "]:" + apduCommand[i]);
                byte[] bcmd = HexUtil.toByteArray(apduCommand[i]);
                byte[] resultByte = techHandle.transceive(bcmd);
                apduResult[i] = HexUtil.toHexString(resultByte, resultByte.length);

                String resultHexStr = byteArrToHexStr(resultByte);
                Log.i(TAG, "byteArrayToHexStr(resultByte):" + resultHexStr);

                String postfix = apduResult[i].substring(apduResult[i].length() - 4);

                Log.i(TAG, "result code: " + postfix);
                if (postfix.equals(ResultCode.SUCCESS)) {
                    // success
                    String data;
                    switch (apduHeader) {
                        case Command.GET_CARD_INFO:
                            // the first 2 digits mean the remaining tries (i.e. 03 means 3 remaining tries)
                            // the last 2 digits mean the card is empty (00) or occupied (01)
                            data = getDecryptedData(getPartialEncryptedData(apduResult[i]));
                            result.append(data);
                            break;
                        case Command.RESTORE:
                            StringBuilder concatEncrypted = new StringBuilder(getPartialEncryptedData(apduResult[i]));
                            int indexOfBlock = Integer.parseInt(apduResult[i].substring(0, 2));
                            int numberOfBlocks = Integer.parseInt(apduResult[i].substring(2, 4));

                            while (indexOfBlock + 1 < numberOfBlocks) {
                                Log.d(TAG, "block " + (indexOfBlock + 1) + " out of " + numberOfBlocks);
                                byte[] nextEncrypted = techHandle.transceive(getCmdToGetNextPartialEncryptedData());
                                String apduNextResult = HexUtil.toHexString(nextEncrypted, nextEncrypted.length);
                                Log.d(TAG, "apduNextResult: " + apduNextResult);
                                concatEncrypted.append(getPartialEncryptedData(apduNextResult));
                                indexOfBlock++;
                            }
                            Log.d(TAG, "concatEncrypted: " + concatEncrypted.toString());
                            data = getDecryptedData(concatEncrypted.toString());
                            // convert hex to bytes to string
                            result.append(new String(hexStrToByteArr(data), StandardCharsets.UTF_8));
                            break;
                        case Command.BACKUP:
                            if (i != blockNumber - 1) continue;
                        case Command.RESET:
                        default:
                            result.append(postfix);
                    }
                } else {
                    // error
                    result.append(resultHexStr);
                }
            }

            Log.i(TAG, "result: " + result.toString());
            return result.toString();

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "error: " + ex.toString());
            // showResult("error:" + ex.toString());
        } finally {
            try {
                if (techHandle != null)
                    techHandle.close();
            } catch (IOException e) {
                e.printStackTrace();
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
        String hash = HashUtil.sha256(apduHeader + salt + apduData);
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
        String commandData = cipherData.substring((blockNumber - 1) * blockSize * 2);
        String commandLength = HexUtil.toHexString(commandData.length() / 2, 1);
        apduCommand[blockNumber - 1] = commandHeader + commandLength + commandData;
        System.out.println("command:" + commandHeader + " " + commandData + " (" + (commandData.length() / 2) + "Bytes)");
        return apduCommand;
    }

    private static String getPartialEncryptedData(String apduReturnResult) {
        Log.d(TAG, "getPartialEncryptedData - apduReturnResult: " + apduReturnResult + ", length: " + apduReturnResult.length());
        return apduReturnResult.substring(4, apduReturnResult.length() - 4);
    }

    private static String getDecryptedData(String fullLengthEncryptedData) {
        String decrypted = CryptoUtil.decryptAES(secureKey, fullLengthEncryptedData);
        Log.d(TAG, "getDecryptedData - decrypted: " + decrypted + ", length: " + decrypted.length());
        String decryptedHash = decrypted.substring(0, 64);
        String decryptedSalt = decrypted.substring(64, 72);
        String decryptedData = decrypted.substring(72);
        Log.d(TAG, "getDecryptedData - decryptedData: " + decryptedData);
        return decryptedData;
    }

    private static byte[] getCmdToGetNextPartialEncryptedData() {
        return HexUtil.toByteArray(Command.GET_NEXT_PARTIAL_ENCRYPTED);
    }
}
