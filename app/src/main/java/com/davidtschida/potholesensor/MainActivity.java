package com.davidtschida.potholesensor;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;


public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;

    private double currentX, currentY, currentZ;
    private int accuracy;

    TextView x,y,z, acc, time;
    Switch _switch;

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentX = event.values[0];
        currentY = event.values[1];
        currentZ = event.values[2];

        x.setText("X:" + currentX);
        y.setText("Y:" + currentY);
        z.setText("Z:" + currentZ);
        time.setText("Time:"+ System.currentTimeMillis());
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

        _switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    sensorManager.registerListener(MainActivity.this, sensor, SensorManager.SENSOR_DELAY_GAME);
                else
                    sensorManager.unregisterListener(MainActivity.this);
            }
        });
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
