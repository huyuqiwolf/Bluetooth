package com.ken.demo.bluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final int REQUEST_PERMISSION = 0x1000;

    private Button btnServer;
    private Button btnClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnClient = findViewById(R.id.btn_client);
        btnServer = findViewById(R.id.btn_server);
        if (!hasPermission()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSION);
        }

        btnServer.setOnClickListener((v) -> {
            if (!hasPermission()) {
                requestPermissions(PERMISSIONS, REQUEST_PERMISSION);
                return;
            }
            startActivity(new Intent(MainActivity.this, ServerActivity.class));
        });
        btnClient.setOnClickListener((v) -> {
            if (!hasPermission()) {
                requestPermissions(PERMISSIONS, REQUEST_PERMISSION);
                return;
            }
            startActivity(new Intent(MainActivity.this, FindDeviceActivity.class));
        });

    }


    private boolean hasPermission() {
        for (String permission : PERMISSIONS) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "request permission failed!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
