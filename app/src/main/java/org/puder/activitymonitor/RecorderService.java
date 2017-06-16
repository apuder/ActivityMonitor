package org.puder.activitymonitor;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.puder.activitymonitor.ann.Config;


public class RecorderService extends IntentService implements SensorEventListener {

    final public static String     EXTRA_ACTIVITY_TYPE = "NAME";

    public static volatile boolean isRunning           = false;

    private String                 name;
    private StorageUtil            storage;
    private Handler                sensorHandler;
    private HandlerThread          sensorHandlerThread;
    private PowerManager.WakeLock  wakeLock;


    public RecorderService() {
        super("RecorderService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        isRunning = true;
        showForegroundNotification();
        name = intent.getStringExtra(EXTRA_ACTIVITY_TYPE);
        storage = new StorageUtil(name);
        startAccelerometer();
        try {
            Thread.sleep(Config.LENGTH_RECORDING_IN_MINUTES * 60 * 1000);
        } catch (InterruptedException e) {
        }
        stopAccelerometer();
        storage.close();
        RecorderFinishedEvent event = new RecorderFinishedEvent();
        event.activityType = name;
        EventBus.getDefault().post(event);
        isRunning = false;
        hideForegroundNotification();
        ToneGenerator beep = new ToneGenerator(AudioManager.STREAM_MUSIC, 500);
        beep.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 1000);
    }

    private void getWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivityMonitor");
        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        wakeLock.release();
        wakeLock = null;
    }

    private void showForegroundNotification() {
        Intent notificationIntent = new Intent(this, RecorderActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.app_name))
                .setContentText("Activity recording in progress")
                .setSmallIcon(R.drawable.ic_directions_walk_black_24dp)
                .setContentIntent(pendingIntent).build();

        startForeground(1, notification);
    }

    private void hideForegroundNotification() {
        stopForeground(true);
    }

    private void startAccelerometer() {
        getWakeLock();
        sensorHandlerThread = new HandlerThread("ActivityMonitorSensorThread");
        sensorHandlerThread.start();
        sensorHandler = new Handler(sensorHandlerThread.getLooper());
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensorAccelerometer,
                1000 * 1000 / Config.SAMPLING_FREQUENCY, sensorHandler);
    }

    private void stopAccelerometer() {
        releaseWakeLock();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.unregisterListener(this, sensorAccelerometer);
        sensorHandlerThread.quit();
        sensorHandlerThread = null;
        sensorHandler = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long ts = event.timestamp;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        String row = String.format("%d,%f,%f,%f\n", ts, x, y, z);
        storage.append(row);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
