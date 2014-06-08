package com.davidtschida.potholesensor;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;

    private double currentX, currentY, currentZ;
    private int accuracy;

    TextView x,y,z, acc, time, file_size, file_name;
    Switch _switch;

    File file;
    BufferedWriter out;

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentX = event.values[0];
        currentY = event.values[1];
        currentZ = event.values[2];

        x.setText("X:" + currentX);
        y.setText("Y:" + currentY);
        z.setText("Z:" + currentZ);

        long _time = System.currentTimeMillis();
        time.setText("Time:"+ _time);

        try {
            out.write(_time + ", " + currentX+ ", " + currentY + ", " + currentZ+ ", " + accuracy + "\n");
        } catch (IOException e) {
            Log.e("MainActivity", "Unable to write to file", e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        this.accuracy = accuracy;
        acc.setText("A:" + this.accuracy);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        x = (TextView) findViewById(R.id.sensor_x);
        y = (TextView) findViewById(R.id.sensor_y);
        z = (TextView) findViewById(R.id.sensor_z);
        acc = (TextView) findViewById(R.id.sensor_acc);
        time = (TextView) findViewById(R.id.time);
        _switch = (Switch) findViewById(R.id._switch);
        file_size = (TextView) findViewById(R.id.file_size);
        file_name = (TextView) findViewById(R.id.file_name);

        _switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            out.flush();
                        } catch (IOException e) {
                            Log.e("MainActivity", "Unable to flush stream", e);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        file_size.setText("Size:" + file.length()/1024 + "KB");
                    }
                }.execute();

                if(isChecked) {
                    sensorManager.registerListener(MainActivity.this, sensor, SensorManager.SENSOR_DELAY_GAME);
                }
                else {
                    sensorManager.unregisterListener(MainActivity.this);
                }
            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();
        File root = Environment.getExternalStorageDirectory();
        file = new File(root, "Sensor_log" + System.currentTimeMillis() + ".txt");

        FileWriter filewriter = null;
        try {
            filewriter = new FileWriter(file, true);
        } catch (IOException e) {
            Log.e("MainActivity", "Unable to create writer", e);
            finish();
        }
        if(filewriter != null)
            out = new BufferedWriter(filewriter);

        file_name.setText(file.getName());
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(_switch.isChecked())
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        try {
            out.close();
        } catch (IOException e) {
            Log.e("MainActivity", "Unable to close writer", e);
            finish();
        }
    }

    private void writeLine(Object... elements)
    {
        String s = "";
        for(int i = 0; i < elements.length ; i++)
        {
            s = s + elements[i].toString();
            if(i < elements.length -1)
                s = s + ",";
        }


    }
}
