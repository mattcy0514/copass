package com.matt.copass;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class VendorActivity extends AppCompatActivity {
    boolean mWriteMode = false;
    boolean isFinished = false;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private EditText mVendorNameEditText;
    private EditText mVendorCodeEditText;
    private Button mWriteButton;
    private DialogHelper dialogHelper;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor);

        mVendorNameEditText = findViewById(R.id.et_vendor_name);
        mVendorCodeEditText = findViewById(R.id.et_vendor_code);
        mWriteButton = findViewById(R.id.btn_vendor_write);
        dialogHelper = new DialogHelper(VendorActivity.this);

        mWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFinished = false;
                String name = mVendorNameEditText.getText().toString();
                String code = mVendorCodeEditText.getText().toString();
                Log.d("onNewIntent: ", code + name);
                if (name.equals("") || code.equals("")) {
                    Toast.makeText(VendorActivity.this, "Please Enter Your Info", Toast.LENGTH_SHORT).show();
                    return;
                }
                mNfcAdapter = NfcAdapter.getDefaultAdapter(VendorActivity.this);
                mNfcPendingIntent = PendingIntent.getActivity(VendorActivity.this, 0,
                        new Intent(VendorActivity.this, VendorActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

                enableTagWriteMode();

                dialog = dialogHelper.getVendorScanDialog();
                dialog.show();
            }
        });
    }

    private void enableTagWriteMode() {
        if (mNfcAdapter != null) {
            mWriteMode = true;
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            IntentFilter[] mWriteTagFilters = new IntentFilter[]{tagDetected};
            mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dialog != null && dialog.isShowing()) {
            enableTagWriteMode();
        }
        getSupportActionBar().hide();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!isFinished && mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.d("onNewIntent: ", "DEBUG");
            JSONObject jsonObject = new JSONObject();
            try {
                String vendorName = mVendorNameEditText.getText().toString();
                String vendorCode = mVendorCodeEditText.getText().toString();
                Log.d("onNewIntent: ", vendorCode + vendorName);
                if (vendorName.equals("") || vendorCode.equals("")) {
                    Toast.makeText(this, "Please Enter Your Info", Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    jsonObject.put("type", "sms");
                    jsonObject.put("name", vendorName);
                    jsonObject.put("code", vendorCode);
                    jsonObject.put("verification", vendorName.hashCode() + vendorCode.hashCode());
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            finally {
                NdefRecord record = createTextRecord(jsonObject.toString());
                NdefMessage message = new NdefMessage(new NdefRecord[]{record});
                if (writeTag(message, detectedTag)) {
                    Toast.makeText(this, "Success to write nfc tag", Toast.LENGTH_LONG)
                            .show();
                    try {
                        Dialog successDialog = dialogHelper.getSuccessDialog(jsonObject.getString("name"),
                                jsonObject.getString("code"), "VENDOR");
                        successDialog.show();
                        isFinished = true;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                cancelDialog();
                            }
                        }, 500);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Vibrator vibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                    vibrator.vibrate(500);
                }
            }
        }
    }

    // Source code from Internet
    public boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(getApplicationContext(),
                            "Error: tag not writable",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    Toast.makeText(getApplicationContext(),
                            "Error: tag too small",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    private NdefRecord createTextRecord (String message) {
        try {
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = message.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;

            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageSize + textLength);

            payload.write((byte) (languageSize & 0x1F));
            payload.write(language, 0, languageSize);
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());
        }
        catch (UnsupportedEncodingException e) {
            Log.e("createTextRecord", e.getMessage());
        }
        return null;
    }

    private void cancelDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.cancel();
        }
    }

}