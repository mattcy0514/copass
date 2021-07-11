package com.matt.copass;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class UserActivity extends AppCompatActivity {
    boolean isFirstTime = true;
    boolean mReadMode = false;
    private Toolbar tb_drawer;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private android.app.Dialog dialog;
    private DialogHelper dialogHelper;
    private final String[] permissions =
            {Manifest.permission.NFC, Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE};
    private TelephonyManager telephonyManager;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // Dynamically request permissions
        requestPermissions(permissions, 1);

        // Initialize the telephony status
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        ((TextView) findViewById(R.id.tv_card_info)).append("\n\n" + telephonyManager.getNetworkOperatorName());

        dialogHelper = new DialogHelper(UserActivity.this);

        // Initialize the NFC component onCreate
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            Intent enableNfcIntent = new Intent(Settings.ACTION_NFC_SETTINGS);
            Toast.makeText(this, "Open NFC", Toast.LENGTH_SHORT).show();
            startActivity(enableNfcIntent);
        }
        // onCreate() > onResume() > onNewIntent() > onResume()
        // If anywhere touches NFC tag, execute intent to determine what to do
        // isFirstTime is initialized onCreate(), after opening app, isFirstTime will become false
        if (isFirstTime && getIntent() != null) {
            executeIntent(getIntent());
            isFirstTime = false;
        }

        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, UserActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        tb_drawer = findViewById(R.id.tb_drawer);
        tb_drawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UserActivity.this, VendorActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableTagReadMode();
        getSupportActionBar().hide();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void enableTagReadMode() {
        if (mNfcAdapter != null) {
            mReadMode = true;
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            IntentFilter[] mWriteTagFilters = new IntentFilter[] { tagDetected };
            mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
        }
    }

    private void disableTagReadMode() {
        if (mNfcAdapter != null) {
            mReadMode = false;
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.e("onNewIntent: ", String.valueOf(getIntent().getAction()));
        // Close the previous dialog to prevent from multiple dialog showing
        cancelDialog();
        executeIntent(intent);
    }

    private void executeIntent(Intent intent) {
        Log.e("executeIntent: ", String.valueOf(getIntent().getAction()));
        String action = intent.getAction();
        if (mReadMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {
                NdefMessage[] messages = new NdefMessage[rawMessages.length];
                for (int i = 0; i < rawMessages.length; i++) {
                    messages[i] = (NdefMessage) rawMessages[i];
                }
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(getNdefText(messages));
                    // This application can implement with not only sms but web api
                    // Depends on type in nfc tag
//                    String type = jsonObject.getString("type");
                    String name = jsonObject.getString("name");
                    String code = jsonObject.getString("code");
                    int verification = jsonObject.getInt("verification");

                    // Check the format of NFC Tag
                    if (name.hashCode() + code.hashCode() == verification) {
                        SmsManager smsManager = SmsManager.getDefault();
                        // Check telephony state
                        if (!(telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY)) {
                            Toast.makeText(this, "Check for your SIM Card state", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            smsManager.sendTextMessage("1922", null, "場所代碼：" + code + "\n" +
                                    "本次實聯簡訊限防疫目的使用。", null, null);
                            dialog = dialogHelper.getSuccessDialog(name, code, "USER");
                            dialog.show();
                        }
                    }
                    else {
                        Toast.makeText(this, "This card is not legal", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "This card is not legal", Toast.LENGTH_SHORT).show();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "This card is not legal", Toast.LENGTH_SHORT).show();
                }
                // Vibration Interaction with users
                Vibrator vibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                vibrator.vibrate(500);
            }
        }
    }

    private String getNdefText(NdefMessage[] messages) throws UnsupportedEncodingException {
        byte[] payloadBytes = messages[0].getRecords()[0].getPayload();
        boolean isUTF8 = (payloadBytes[0] & 0x080) == 0;
        int languageLength = payloadBytes[0] & 0x03F;
        int textLength = payloadBytes.length - 1 - languageLength;
        final String payloadText = new String(payloadBytes, 1 + languageLength, textLength, isUTF8 ? "UTF-8" : "UTF-16");
        return payloadText;
    }

    private void cancelDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.cancel();
            dialog = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permitted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cannot permitted", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }
}
