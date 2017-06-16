package org.puder.activitymonitor;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.puder.activitymonitor.ann.Config;

import org.puder.activitymonitor.ann.ANN;
import org.puder.activitymonitor.ann.ANNConsumer;

public class MainActivity extends AppCompatActivity implements SensorEventListener, ANNConsumer {

    private ANN ann;
    private TextView textViewStanding;
    private TextView textViewHopping;
    private TextView textViewWalking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ann = new ANN(this);
        textViewStanding = (TextView) findViewById(R.id.standing);
        textViewHopping = (TextView) findViewById(R.id.hopping);
        textViewWalking = (TextView) findViewById(R.id.walking);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_test) {
            startActivity(new Intent(this, RecorderActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        startAccelerometer();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAccelerometer();
    }

    private void startAccelerometer() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensorAccelerometer,
                1000 * 1000 / Config.SAMPLING_FREQUENCY);
    }

    private void stopAccelerometer() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.unregisterListener(this, sensorAccelerometer);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long ts = event.timestamp;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        ann.addAccelerometerReading(ts, x, y, z);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void consume(double standing, double hopping, double walking) {
        textViewStanding.setAlpha(Math.max(0.1f, (float) standing));
        textViewHopping.setAlpha(Math.max(0.1f, (float) hopping));
        textViewWalking.setAlpha(Math.max(0.1f, (float) walking));
        /*
        NumberFormat formatter = new DecimalFormat("#0.0000");
        textViewStanding.setText("Standing (" + formatter.format(standing) + ")");
        textViewHopping.setText("Hopping (" + formatter.format(hopping) + ")");
        textViewWalking.setText("Walking (" + formatter.format(walking) + ")");
        */
    }
}