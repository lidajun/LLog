package com.github.lidajun.android.sample.llog;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.lidajun.android.common.utils.llog.LLog;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LLog.e();
    }
}
