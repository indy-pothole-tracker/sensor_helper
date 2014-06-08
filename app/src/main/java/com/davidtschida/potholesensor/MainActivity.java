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

    private double currentX, currentY, currentZ;
    private int accuracy;

    TextView x,y,z, acc, time, file_size, file_name;
    Switch _switch;

    File file;
    BufferedWriter out;

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

                    dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            uploadFile(file.getAbsolutePath());
                            return null;
                        }
                    }.execute();
                }
            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();
        File root = Environment.getExternalStorageDirectory();
        file = new File(root, "Sensor_log" + System.currentTimeMillis() + ".csv");

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

    public int uploadFile(String sourceFileUri) {
        String upLoadServerUri = getString(R.string.url);
        int serverResponseCode = 0;

        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        if (!file.isFile()) {

            dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :"
                    +sourceFileUri);

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
                        Toast.makeText(MainActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
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
}
