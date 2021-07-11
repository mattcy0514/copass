package com.matt.copass;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

public class LoginActivity extends AppCompatActivity {
    private SharedPreferences mSharedPreferences;
    private NavController navController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if you are the first time to this app
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (mSharedPreferences.getBoolean("is_policy_accepted", false)) {
            Intent intent = new Intent(this, UserActivity.class);
            startActivity(intent);
            this.finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().hide();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }
}