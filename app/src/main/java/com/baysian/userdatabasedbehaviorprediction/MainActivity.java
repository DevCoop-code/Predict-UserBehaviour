package com.baysian.userdatabasedbehaviorprediction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
{

    private static final String TAG = "MainActivity";

    boolean GPS_FLAG = false;

    private final int MY_PERMISSION_REQUEST_STORAGE = 100;

    SharedData sharedData;

    Button finish_btn;
    Button start_btn;
    //Timer timer;

    TextView service_status;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedData = new SharedData();

        GPSCheckPermission();
        /*
        timer = new Timer();
        timer.schedule(adTask, 0, 1000);
        */
        finish_btn = (Button)findViewById(R.id.fin_btn);
        start_btn = (Button)findViewById(R.id.start_btn);

        service_status = (TextView)findViewById(R.id.service_status);
    }

    /*
    TimerTask adTask = new TimerTask()
    {
        SharedData sharedData = new SharedData();

        @Override
        public void run()
        {

        }
    };
    */
    private void GPSCheckPermission()
    {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION))
            {
                Log.v("[Activity Permission]","ACCESS_FINE_LOCATION");
            }
            sharedData.GPS_FLAG = false;

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_STORAGE);
        }
        else
        {
            sharedData.GPS_FLAG = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch(requestCode)
        {
            case MY_PERMISSION_REQUEST_STORAGE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    sharedData.GPS_FLAG = true;
                }
                else
                {
                    sharedData.GPS_FLAG = false;
                }
                break;
        }
    }

    public void finishService(View v)
    {
        Log.v(TAG,"finishbutton");
        service_status.setText("Service Finished");
        stopService(new Intent(this, GatherData.class));
    }
    public void startService(View v)
    {
        Log.v(TAG,"startbutton");
        service_status.setText("Service Start");
        startService(new Intent(this, GatherData.class));
    }
}