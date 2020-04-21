# CoolWalletS SeedBackupCard SampleCode

> SeedBackupCard SampleCode for CoolWalletS

```
version:    1.0.0
status:     release
copyright:  coolbitX
```

## Setting in Android Studio

1. Open Android Studio > Click Android Studio in header > preference > modify Android SDK Location( Your SDK path);

2. Inspection depencies in build.gradel(app)

    dependences{
        implementation 'com.android.support:appcompat-v7:28.0.0'
        implementation files('lib/bitcoinj-core-0.14.5.jar')
         implementation 'com.google.guava:guava:27.0.1-android'
         implementation files('lib/slf4j-api-1.7.12.jar')
         implementation 'net.consensys.cava:cava:0.6.0'
         implementation group: 'com.madgag.spongycastle', name: 'prov', version: '1.58.0.0'
         implementation group: 'com.madgag.spongycastle', name: 'core', version: '1.58.0.0'
    }

## Example Usage

### 0. APDU Command  & ErrorCode

You can see SecureChannel Commentary  in SecureChannel.txt.

```JAVA
  private interface Commands {
      String BACKUP = "80320500";
      String RESTORE = "80340000";
      String RESET = "80360000";
      String SECURE_CHANNEL = "80CE000041";
  }

  private interface ErrorCode {
      String SUCCESS = "9000";
      String RESET_FIRST = "6330";
      String NO_DATA = "6370";
      String PING_CODE_NOT_MATCH = "6350";
      String CARD_IS_LOCKED = "6390";
  }
```

### 1. NFC Librarys
[android.nfc.NfcAdapter](https://developer.android.com/reference/android/nfc/NfcAdapter)
[android.nfc.Tag](https://developer.android.com/reference/android/nfc/Tag)
[android.nfc.tech.IsoDep](https://developer.android.com/reference/android/nfc/tech/IsoDep) 


### 2. Initial NFC
1.Need call initNFC function in onCreat().
```JAVA

      @Override
      protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initNFC();
      }
      private void initNFC() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
      }

```
2.OverWrite onPause,OnNewIntent and onResume function
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
          Log.d(TAG, "onNewIntent: " + intent.getAction());

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

### 3. BackUp Sample Code


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

### 4. Restore Sample Code


```JAVA
      private void restore() {
        String apduHeader = Commands.RESTORE;
        String command = "";
        String HashPinCode =getSHA256StrJava("YouPingCodeString")
        command = command + HashPinCode;
        sendCmdWithSecureChannel(apduHeader, command);
      }
```

### 5. Reset Sample Code


```JAVA
  private void reset() {
      sendCmdWithSecureChannel(Commands.RESET, "");
  }
```
