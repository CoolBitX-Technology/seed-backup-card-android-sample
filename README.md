# SeedBackupCard Sample Code

Setup
-----
Add seed-backup-card-sdk to ```build.gradle``` file
```groovy
dependencies {    
    implementation project(path: ':seedbackupcard-sdk')
}
```

Use NFC in Android
------------------
- [android.nfc.NfcAdapter](https://developer.android.com/reference/android/nfc/NfcAdapter)
- [android.nfc.Tag](https://developer.android.com/reference/android/nfc/Tag)
- [android.nfc.tech.IsoDep](https://developer.android.com/reference/android/nfc/tech/IsoDep) 


Work with Android NFC
---------------------
### Request NFC access in the Android manifest
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

### Use NFC adapter
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
First of all, you'll need to import the utilities in your UI controller.
```java
import com.coolbitx.seedbackupcard_sdk.CWSUtil;
```

### Backup
Store the given data and PIN in the card
```java
String resultCode = CWSUtil.backup(backupMessage, pin);
```

### Restore
Get the contents of the card by specifying the **valid** PIN
```java
String resultCode = CWSUtil.restore(pin);
```

### Reset
Erase the contents of the card. (PIN will be retained)
```java
String resultCode = CWSUtil.reset();
```

### Get the information of the card
Get the remaining tries and check if the card is being used
```java
String resultCode = getCardInfo();
```
