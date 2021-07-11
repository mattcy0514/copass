package com.matt.copass;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DialogHelper {
    private Activity activity;

    public DialogHelper(Activity activity) {
        this.activity = activity;
    }

    public Dialog getSuccessDialog(String name, String code, String identification) {
        View view = activity.getLayoutInflater().inflate(R.layout.layout_success, null);
        Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        ((TextView) view.findViewById(R.id.tv_success_title)).setText(name);
        ((TextView) view.findViewById(R.id.tv_success_code)).append(code);
        if(identification.equals("USER")) {
            ((TextView) view.findViewById(R.id.tv_success_hint)).setText("成功簽到");
            ((TextView) view.findViewById(R.id.tv_success_hint_to_user)).setText("請服務人員檢閱此畫面");
        }
        else if (identification.equals("VENDOR")) {
            ((TextView) view.findViewById(R.id.tv_success_hint)).setText("成功");
            ((TextView) view.findViewById(R.id.tv_success_hint_to_user)).setText("現在可以以此卡片供顧客感應");
        }
        ((TextView) view.findViewById(R.id.tv_success_time)).setText(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
        dialog.setContentView(view);
        ((Button) view.findViewById(R.id.btn_success_to_home)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                if (identification.equals("VENDOR")) {
                    activity.finish();
                }
            }
        });
        return dialog;
    }

    public Dialog getVendorScanDialog() {
        View view = activity.getLayoutInflater().inflate(R.layout.layout_vender_nfc, null);
        Dialog dialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        dialog.setContentView(view);
        return dialog;
    }
}
