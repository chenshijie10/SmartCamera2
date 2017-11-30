package com.lenovo.chensj.smartcamera2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainCameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_camera);
        requestFullScreen();
    }

    private void requestFullScreen() {
    }
}
