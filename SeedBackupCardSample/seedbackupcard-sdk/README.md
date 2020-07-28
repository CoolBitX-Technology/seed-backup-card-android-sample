# Seed-Backup-Card-SDK

Follow the instructions to build **seed-backup-card-sdk**. 

Setup
-----
Add the required dependencies in the module's ```build.gradle``` file
```groovy
dependencies {    
    implementation 'net.consensys.cava:cava:0.6.0'
    implementation 'com.madgag.spongycastle:core:1.58.0.0'
    implementation 'com.madgag.spongycastle:prov:1.58.0.0'
    implementation files('lib/bitcoinj-core-0.14.5.jar')
    implementation files('lib/slf4j-api-1.7.12.jar')
}
```

Commands & ResultCodes
----------------------
```java
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
```

Usage
-----

For more details on **SecureChannel**, please check the [link](https://github.com/CoolBitX-Technology/seed-backup-card-android-sample/blob/master/SecureChannel.txt).

### Backup
Store the given data and PIN in the card
```java
public static String backup(String backupMessage, String pin) {
    byte[] _data = backupMessage.getBytes();
    String hexData = bytesToHex(_data);
    String HashPinCode = getSHA256StrJava(pin);
    String command = HashPinCode + hexData;
    return sendCmdWithSecureChannel(Commands.BACKUP, command);
}
```

### Restore
Get the contents of the card by specifying the **valid** PIN
```java
public static String restore(String pin) {
    String hashedPinCode = getSHA256StrJava(pin);
    return sendCmdWithSecureChannel(Commands.RESTORE, hashedPinCode);
}
```

### Reset
Erase the contents of the card. (PIN will be retained)
```java
public static String reset() {
    return sendCmdWithSecureChannel(Commands.RESET, "");
}
```

### Get the information of the card
Get the remaining tries and check if the card is being used
```java
public static String getCardInfo() {
    return sendCmdWithSecureChannel(Command.GET_CARD_INFO, "");
}
```
