package com.davidtschida.potholesensor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;

    private long initialLength;

    private double currentX, currentY, currentZ;
    private int accuracy;

    TextView x,y,z, acc, time, file_size, file_name;
    Switch _switch;
    Button btn_pothole;

    File file;
    BufferedWriter out;

    boolean pothole = false;

    ProgressDialog dialog = null;

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
            out.write(_time + ", " + currentX+ ", " + currentY + ", " + currentZ+ ", " + accuracy);
            if(pothole) out.write(", true");
            pothole=false;
            out.newLine();
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

        initViews();

        _switch.setOnCheckedChangeListener(new MyOnCheckedChangeListener());

        btn_pothole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pothole = true;
            }
        });
    }

    private void initViews() {
        x = (TextView) findViewById(R.id.sensor_x);
        y = (TextView) findViewById(R.id.sensor_y);
        z = (TextView) findViewById(R.id.sensor_z);
        acc = (TextView) findViewById(R.id.sensor_acc);
        time = (TextView) findViewById(R.id.time);
        _switch = (Switch) findViewById(R.id._switch);
        file_size = (TextView) findViewById(R.id.file_size);
        file_name = (TextView) findViewById(R.id.file_name);
        btn_pothole = (Button) findViewById(R.id.pothole);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        createFile();
        file_name.setText(file.getName());
    }

    private void createFile() {
        File root = Environment.getExternalStorageDirectory();
        file = new File(root, "Sensor_log_" + System.currentTimeMillis() + ".csv");

        FileWriter filewriter;
        try {
            filewriter = new FileWriter(file, true);

            if(out != null)
                out.close();

            out = new BufferedWriter(filewriter);

            out.write("time(unix), x, y, z, accuracy, pothole\n");

            out.flush();

            initialLength = file.length();
        } catch (IOException e) {
            Log.e("MainActivity", "Unable to create writer", e);
            finish();
        }
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
        if(file.length() == initialLength)
        {
            if(!file.delete())
            {
                Toast.makeText(this, "File could not be deleted.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int uploadFile(String fileName) {
        String upLoadServerUri = getString(R.string.url);
        int serverResponseCode = 0;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 3 * 1024 * 1024;

        if (!file.isFile()) {

            dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :"
                    +fileName);

            return 0;
        }
        else
        {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(file);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                //conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                                + fileName + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "File Upload Complete.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "MalformedURLException", Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "Got Exception : see logcat ",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server Exception", "Exception : "
                        + e.getMessage(), e);
            }
            dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }

    private class MyOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
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

                dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected void onPostExecute(Void aVoid) {
                        file_name.setText(file.getName());
                    }

                    @Override
                    protected Void doInBackground(Void... params) {

                        if(uploadFile(file.getAbsolutePath()) == 200)
                        {
                            if(!file.delete())
                                Toast.makeText(MainActivity.this, "File could not be deleted", Toast.LENGTH_LONG).show();
                            createFile();
                        }
                        return null;
                    }
                }.execute();
            }
        }
    }
}
