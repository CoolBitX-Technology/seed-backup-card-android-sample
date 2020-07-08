package com.coolbitx.seedbackup;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.coolbitx.seedbackup.databinding.ActivityMainBinding;
import com.coolbitx.seedbackup.utils.CWSUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding binding;

    private NfcAdapter mNfcAdapter;
    private Tag mTag;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initNFC();
    }


    private void initViews() {

        binding.btnBackup.setOnClickListener(view -> {
            if (mTag != null) {
                if (TextUtils.isEmpty(binding.editBackup.getText())) {
                    binding.textInputBackup.setError(getString(R.string.cant_be_blank));
                    binding.textInputBackup.setErrorEnabled(true);
                    return;
                }
                if (TextUtils.isEmpty(binding.editPin.getText())) {
                    binding.textInputPin.setError(getString(R.string.cant_be_blank));
                    binding.textInputPin.setErrorEnabled(true);
                    return;
                }

                binding.textInputBackup.setErrorEnabled(false);
                binding.textInputPin.setErrorEnabled(false);
                String msg = CWSUtil.backup(
                        binding.editBackup.getText().toString(),
                        binding.editPin.getText().toString(),
                        mTag);
                showResult(msg);
            }
        });
        binding.btnRestore.setOnClickListener(view -> {
            if (mTag != null) {
                if (TextUtils.isEmpty(binding.editPin.getText())) {
                    binding.textInputPin.setError(getString(R.string.cant_be_blank));
                    binding.textInputPin.setErrorEnabled(true);
                    return;
                }
                binding.textInputPin.setErrorEnabled(false);
                String msg = CWSUtil.restore(binding.editPin.getText().toString(), mTag);
                showResult(msg);
            }
        });
        binding.btnReset.setOnClickListener(view -> {
            binding.textInputBackup.setErrorEnabled(false);
            binding.textInputPin.setErrorEnabled(false);
            if (mTag != null) {
                String msg = CWSUtil.reset(mTag);
                showResult(msg);
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
        String result;
        if (msg == null) return;
        switch (msg) {
            case CWSUtil.ErrorCode.SUCCESS:
                result = "Success";
                break;
            case CWSUtil.ErrorCode.RESET_FIRST:
                result = "You need to reset first";
                break;
            case CWSUtil.ErrorCode.NO_DATA:
                result = "There's no data";
                break;
            case CWSUtil.ErrorCode.PIN_CODE_NOT_MATCH:
                result = "Your PIN doesn't match";
                break;
            case CWSUtil.ErrorCode.CARD_IS_LOCKED:
                result = "The card is locked.";
                break;
            default:
                result = msg;
        }
        binding.tvResult.setText(result);
    }
}
