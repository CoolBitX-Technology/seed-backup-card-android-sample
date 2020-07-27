# SeedBackupCard Sample Code

Setup
-----
Add the required dependencies in the module's ```build.gradle``` file
```groovy
dependencies{
    implementation files('lib/bitcoinj-core-0.14.5.jar')
    implementation files('lib/slf4j-api-1.7.12.jar')
    implementation 'net.consensys.cava:cava:0.6.0'
    implementation group: 'com.madgag.spongycastle', name: 'prov', version: '1.58.0.0'
    implementation group: 'com.madgag.spongycastle', name: 'core', version: '1.58.0.0'
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
For more details on **SecureChannel**, please check the [link](https://github.com/CoolBitX-Technology/seed-backup-card-android-sample/blob/master/SecureChannel.txt).

Use NFC in Android
------------------
- [android.nfc.NfcAdapter](https://developer.android.com/reference/android/nfc/NfcAdapter)
- [android.nfc.Tag](https://developer.android.com/reference/android/nfc/Tag)
- [android.nfc.tech.IsoDep](https://developer.android.com/reference/android/nfc/tech/IsoDep) 


Work with Android NFC
---------------------
### 1. Request NFC access in the Android manifest
Declare the following items in your app's ```AndroidManifest.xml``` file:

- The permission to access the NFC hardware
```xml
<uses-permission android:name="android.permission.NFC" />
``` 

- The minimum SDK version that your application can support
```xml
<uses-sdk android:minSdkVersion="10"/>
```

- Optional: The ```uses-feature``` element so that the application shows up in Google Play only for devices have NFC hardware
```xml
<uses-feature android:name="android.hardware.nfc" android:required="true" />
``` 

### 2. Use NFC adapter
- Initialize NFC adapter in ```onCreate()```.
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
}
```
- Use the foreground dispatch system

The **foreground dispatch system** allows an activity to intercept an NFC intent (i.e. tag detected) 
and claim priority over other activities that are registered to handle the same intent. 
This way, when our application is in the foreground, 
users won't be prompted to choose which application to open, and the intent is sent to our application.

For more details on foreground dispatch system, please check the [link](https://developer.android.com/guide/topics/connectivity/nfc/advanced-nfc#foreground-dispatch).

Usage
-----

### BackUp
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
Get the information of the card
```java
public static String getCardInfo() {
    return sendCmdWithSecureChannel(Command.GET_CARD_INFO, "");
}
```
