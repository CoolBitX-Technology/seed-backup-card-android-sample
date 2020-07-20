package com.coolbitx.seedbackup;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.coolbitx.seedbackup.databinding.ActivityMainBinding;
import com.coolbitx.seedbackup.utils.CWSUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding binding;

    private NfcAdapter mNfcAdapter;
    private IntentFilter[] intentFilters;
    private PendingIntent pendingIntent;
    private Tag mTag;
    private Mode currentMode;

    private enum Mode {
        BACKUP,
        RESTORE,
        RESET,
        CHECK,
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        initNFC();
        initViews();
    }


    private void initViews() {

        binding.btnCheck.setOnClickListener(view -> {
            currentMode = Mode.CHECK;


//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    synchronized (this) {
//                        // Check if the card is locked with default pin
//                        final String msg = CWSUtil.restore("0000", mTag);
//
//                        binding.tvResult.post(new Runnable() {
//                            @Override
//                            public void run() {
//
//                                String result = "";
//                                if (!TextUtils.isEmpty(msg)) {
//                                    switch (msg) {
//                                        case CWSUtil.ErrorCode.CARD_IS_LOCKED:
//                                            result = "The card is locked.";
//                                            break;
//                                        case CWSUtil.ErrorCode.NO_DATA:
//                                            result = "The card is empty.";
//                                            break;
//                                        default:
//                                            result = "The card is not empty.";
//                                    }
//                                }
//                                binding.tvResult.setText(result);
//                            }
//                        });
//                    }
//                }
//            }).start();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String msg = CWSUtil.check(mTag);

                    runOnUiThread(() -> showResult(msg));
                }
            }).start();
        });

        binding.btnBackup.setOnClickListener(view -> {
            currentMode = Mode.BACKUP;
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

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String msg = CWSUtil.backup(
                                binding.editBackup.getText().toString(),
                                binding.editPin.getText().toString(),
                                mTag);

                        runOnUiThread(() -> showResult(msg));
                    }
                }).start();
            }
        });

        binding.btnRestore.setOnClickListener(view -> {
            currentMode = Mode.RESTORE;
            if (mTag != null) {
                if (TextUtils.isEmpty(binding.editPin.getText())) {
                    binding.textInputPin.setError(getString(R.string.cant_be_blank));
                    binding.textInputPin.setErrorEnabled(true);
                    return;
                }
                binding.textInputPin.setErrorEnabled(false);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String msg = CWSUtil.restore(binding.editPin.getText().toString(), mTag);

                        runOnUiThread(() -> showResult(msg));
                    }
                }).start();
            }
        });

        binding.btnReset.setOnClickListener(view -> {
            currentMode = Mode.RESET;
            binding.textInputBackup.setErrorEnabled(false);
            binding.textInputPin.setErrorEnabled(false);
            if (mTag != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String msg = CWSUtil.reset(mTag);

                        runOnUiThread(() -> showResult(msg));
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
        }
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
        Log.d(TAG, "onNewIntent: " + intent.getAction() + ", tag: " + mTag.toString());
    }

    private void initNFC() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
//        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
//        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        intentFilters = new IntentFilter[]{tag};
    }

    private void showResult(String msg) {
        StringBuilder result = new StringBuilder();
        if (msg == null) return;
        switch (msg) {
            case CWSUtil.ErrorCode.SUCCESS:
                if (currentMode == Mode.BACKUP) {
                    result.append("Backup");
                } else if (currentMode == Mode.RESTORE) {
                    result.append("Restore");
                } else if (currentMode == Mode.RESET) {
                    result.append("Reset");
                }
                result.append(" succeeded!");
                break;
            case CWSUtil.ErrorCode.RESET_FIRST:
                result.append("You need to reset first.");
                break;
            case CWSUtil.ErrorCode.NO_DATA:
                result.append("The card is empty.");
                break;
            case CWSUtil.ErrorCode.PIN_CODE_NOT_MATCH:
                result.append("Your PIN doesn't match.");
                break;
            case CWSUtil.ErrorCode.CARD_IS_LOCKED:
                result.append("The card is locked.");
                break;
            default:
                result.append(msg);
        }
        binding.tvResult.setText(result.toString());
    }
}
