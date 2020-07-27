# SeedBackupCard Sample Code

> SeedBackupCard Sample Code for CoolWalletS

# Setup
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

# Commands & ResultCodes
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

# Use NFC in Android
- [android.nfc.NfcAdapter](https://developer.android.com/reference/android/nfc/NfcAdapter)
- [android.nfc.Tag](https://developer.android.com/reference/android/nfc/Tag)
- [android.nfc.tech.IsoDep](https://developer.android.com/reference/android/nfc/tech/IsoDep) 


# Work with Android NFC
## 1. Request NFC access in the Android manifest
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

## 1. Initial NFC
1. Call initNFC function in ```onCreate()```.
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initNFC();
}

private void initNFC() {
    mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
}

```
2. OverWrite ```onPause```, ```OnNewIntent``` and ```onResume``` function
```JAVA
@Override
protected void onPause() {
    super.onPause();
    if (mNfcAdapter != null)
        mNfcAdapter.disableForegroundDispatch(this);
}

@Override
protected void onNewIntent(Intent intent) {
    tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
}
      
      @Override
      protected void onResume() {
          super.onResume();
          IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
          IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
          IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
          IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected, tagDetected, ndefDetected};

          PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
          if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);     
      }
```

## 2. BackUp Sample Code


```JAVA
      private void backup() {
          String apduHeader = Commands.BACKUP;  //max 255   05 tobyte
          String command = "";
          byte[] _data = "YouBackUpDataString".getBytes();
          String hexData = bytesToHex(_data);
          String HashPinCode = getSHA256StrJava("YouPingCodeString");
          command = HashPinCode + hexData;
          sendCmdWithSecureChannel(apduHeader, command);
      }
```

## 3. Restore Sample Code
```JAVA
      private void restore() {
        String apduHeader = Commands.RESTORE;
        String command = "";
        String HashPinCode =getSHA256StrJava("YouPingCodeString")
        command = command + HashPinCode;
        sendCmdWithSecureChannel(apduHeader, command);
      }
```

## 4. Reset Sample Code
```JAVA
  private void reset() {
      sendCmdWithSecureChannel(Commands.RESET, "");
  }
```
