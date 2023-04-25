package com.example.myapplication;

import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.StrictMath.asin;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {
     
    private Button sensorButton;
    private EditText editTextPitch, editTextRoll, editTextYaw;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    double pitch_ = 0.0, roll_ = 0.0, yaw_ = 0.0;
    double pitch = 0.0, roll = 0.0, yaw = 0.0;
    float acclX = 0.0f, acclY = 0.0f, acclZ = 0.0f;
    float gyroX = 0.0f, gyroY = 0.0f, gyroZ = 0.0f;
    float magX = 0.0f, magY = 0.0f, magZ = 0.0f;

    private float[] mag = new float[3];
    private float init_time = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        editTextPitch = (EditText) findViewById(R.id.editTextPitch);
        editTextRoll = (EditText) findViewById(R.id.editTextRoll);
        editTextYaw = (EditText) findViewById(R.id.editTextYaw);

        sensorButton = (Button) findViewById(R.id.sensorBtn);

        sensorButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.sensorBtn:
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //  Acquire the duration/time difference
        float dt = (event.timestamp - init_time) / 1e9f;
        //  Calculate alpha for the complimentary filter
        float alpha = 0.75f/(0.75f+dt);
        if (init_time == 0.0f)
        {
            dt = 0.0f;
        }
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            acclX = event.values[0];
            acclY = event.values[1];
            acclZ = event.values[2];

            // Calculate the pitch and roll angles (in radians) through accelerometer data
            pitch_ = atan2(acclY, acclZ);
            roll_ = atan2(acclX, acclZ);
        }
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];

            // Combine the values found in accelerometer and gyroscope through a complimentary filter
            pitch = (1-alpha)*(pitch + gyroY*dt) + alpha*pitch_;
            roll = (1-alpha)*(roll + gyroX*dt) + alpha*roll_;
        }
        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            magX = event.values[0];
            magY = event.values[1];
            magZ = event.values[2];

            // Approximate the magnetic fields in x and y axis in based on the calculated pitch and roll
            double _Bx = magX*cos(pitch) + magY*sin(pitch)*sin(roll) + magZ*sin(pitch)*cos(roll);
            double _By = magZ*sin(roll) - magY*cos(roll);

            // Calculate yaw using the magnetic field scalars
            yaw_ = atan2(-_By, _Bx);

            // Recalculate yaw with offset so 0 rad points to north
            if (yaw_ >= -PI/2 && yaw_ <= PI) {
                yaw_ -= (PI / 2);
            } else {
                yaw_ += 3*PI/2;
            }
            // Combine the values found in magnetometer and gyroscope through a complimentary filter
            yaw = (1-alpha)*(yaw + gyroZ*dt) + alpha*yaw_;
        }
        //  Update initial time to current time
        init_time = event.timestamp;
        //  Use handler to update the UI in a thread-safe manner
        QuaterTask myTask = new QuaterTask(pitch, roll, yaw);
        myHandler.post(myTask);
        Log.d("ORIENTATION", "pitch: " + pitch + ", roll: " + roll + ", yaw: " + yaw);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    private class QuaterTask implements Runnable{
        //  This puts the processes of updating the pitch, roll, and yaw in the UI in a queue
        double _pitch, _roll, _yaw;
        //  Formats the values to two precision decimal points
        DecimalFormat df = new DecimalFormat("#.##");
        public QuaterTask(double pi, double ro, double ya){
            // Convert the values to degrees
            this._pitch = toDegrees(pi);
            this._roll = toDegrees(ro);
            this._yaw = toDegrees(ya);
        }
        @Override
        public void run() {
            editTextPitch.setText(df.format(_pitch));
            editTextRoll.setText(df.format(_roll));
            editTextYaw.setText(df.format(_yaw));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this,gyroscope);
        sensorManager.unregisterListener(this,magnetometer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope,SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetometer,SensorManager.SENSOR_DELAY_FASTEST);
    }
}