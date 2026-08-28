package com.example.funkyeventapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.funkyeventapp.services.AuthService;

public class MainActivity extends AppCompatActivity {
    private static boolean sessionClearedForCurrentProcess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sessionClearedForCurrentProcess) {
            AuthService.getInstance().logout();
            sessionClearedForCurrentProcess = true;
        }
        setContentView(R.layout.activity_main);
    }
}
