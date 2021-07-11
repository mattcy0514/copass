package com.matt.copass;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class PolicyFragment extends Fragment {
    private View view;
    private Button mAcceptButton;
    private SharedPreferences mSharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_policy, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull @org.jetbrains.annotations.NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAcceptButton = view.findViewById(R.id.btn_policy_accept);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        Log.d("onViewCreated: ", String.valueOf(mSharedPreferences.getBoolean("is_policy_accepted", false)));

        mAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSharedPreferences.edit().putBoolean("is_policy_accepted", true).commit();
                NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.action_policyFragment_to_loginTransFragment);
            }
        });
    }

}