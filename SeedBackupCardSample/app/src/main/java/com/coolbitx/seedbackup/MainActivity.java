package com.coolbitx.seedbackup;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.coolbitx.seedbackup.utils.CWSUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView txtResult;
    private EditText mEtBackupData;
    private EditText mEtPinCode;

    private NfcAdapter mNfcAdapter;
    private Tag mTag;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initNFC();
    }

    private void initViews() {
        mEtBackupData = findViewById(R.id.edit_backup_msg);
        mEtPinCode = findViewById(R.id.edit_pin);
        Button mBtnBackup = findViewById(R.id.btn_backup);
        Button mBtnRestore = findViewById(R.id.btn_restore);
        Button mBtReset = findViewById(R.id.btn_reset);
        txtResult = findViewById(R.id.txtResult);

        mBtnBackup.setOnClickListener(view -> {
            if (mTag != null) {
                String msg = CWSUtil.backup(mEtBackupData.getText().toString(), mEtPinCode.getText().toString(), mTag);
                showResult(msg);
            }
        });
        mBtnRestore.setOnClickListener(view -> {
            if (mTag != null) {
                String msg = CWSUtil.restore(mEtPinCode.getText().toString(), mTag);
                showResult(msg);
            }
        });
        mBtReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTag != null) {
                    String msg = CWSUtil.reset(mTag);
                    showResult(msg);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
//        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
//        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{tagDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.d(TAG, "onNewIntent: " + intent.getAction());

    }

    private void initNFC() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    private void showResult(String msg) {
        txtResult.setText(msg);
    }
}
