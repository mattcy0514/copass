package com.matt.copass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

public class PermissionFragment extends Fragment {
    private View view;
    private final String[] permissions =
            {Manifest.permission.NFC, Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE};
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_permission, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requestPermissions(permissions, 0);
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            // There are two dangerous permissions, hence the length of grantResults is 2
            if (grantResults.length > 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), "Permitted", Toast.LENGTH_SHORT).show();
                NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.action_permissionFragment_to_policyFragment);
            } else {
                Toast.makeText(getActivity(), "Cannot permitted", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
    }
}