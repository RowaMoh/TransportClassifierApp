package acceltofile.flowpilots.com.acceltofile;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static java.lang.Thread.sleep;

public class AcceleroRecordService extends Service implements SensorEventListener{

    SensorManager sensorManager;
    NotificationManager nManager;
    Sensor accel;
    //Temporary arraylists for optimisation of data retrieval (File IO will be slow)
    ArrayList<Float> x_sensorData = new ArrayList<>();
    ArrayList<Float> y_sensorData = new ArrayList<>();
    ArrayList<Float> z_sensorData = new ArrayList<>();
    ArrayList<Long> sensorTimestamps = new ArrayList<>();
    File file;
    FileWriter fileWriter;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onCreate() {
        super.onCreate();
        //Wait 3 seconds before recording
        try {
            sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.
        showNotification();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //TYPE_LINEAR_ACCELERATION excludes gravity -> acceleration - gravity
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        //SENSOR_DELAY_NORMAL = 0.2 seconds sampling rate
        //sensorManager.registerListener((SensorEventListener) this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        //10.000 us = 0.01s = 100Hz
        sensorManager.registerListener(this, accel, 10000);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //event.values contains the xyz coordinate raw accelerometer data
        //Log.i("REALTIME_ACCELX", Float.toString(xyz[0]));
        //sensorData.add(event.values);
        x_sensorData.add(event.values[0]);
        y_sensorData.add(event.values[1]);
        z_sensorData.add(event.values[2]);
        //Log.i("REALTIME_ACCELX1", Float.toString(event.values[0]));
        //Log.i("REALTIME_ACCELX2", Float.toString(sensorData.get(sensorData.size()-1)[0]));
        //Save the corresponding timestamp to the obtained data
        sensorTimestamps.add(event.timestamp);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onDestroy() {
        //bw = new BufferedWriter(fileWriter);
        sensorManager.unregisterListener(this);
        //file = new File(getApplicationContext().getFilesDir(),"accel_data"+filenr+".csv");
        //file = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "accel_data"+filenr+".csv");
        String filename = "accel_data";
        file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS),"accel_data"+".csv");
        int filenr = 1;
        while(file.exists()){
            file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS),"accel_data"+filenr+".csv");
            filenr++;
        }
        try {
            fileWriter = new FileWriter(file);
            //fileWriter = new FileWriter(getFilesDir()+"accel_data"+filenr+".csv");
            //fOut = openFileOutput("accel_data"+filenr, Context.MODE_PRIVATE);
            fileWriter.write("Timestamp"+","
                    +"X"+","
                    +"Y"+","
                    +"Z"+"\n");
            for(int i=0;i<x_sensorData.size();i++) {
          /*      Log.i("SENSOR_INFO", Long.toString(sensorTimestamps.get(i)/1000)+","
                        +Float.toString(x_sensorData.get(i))+","
                        +Float.toString(y_sensorData.get(i))+","
                        +Float.toString(z_sensorData.get(i))); */
                fileWriter.write(Long.toString(sensorTimestamps.get(i))+","
                        +Float.toString(x_sensorData.get(i))+","
                        +Float.toString(y_sensorData.get(i))+","
                        +Float.toString(z_sensorData.get(i))+"\n");
                //bw.write(sensorTimestamps.get(i));
                //fOut.write((byte) sensorTimestamps.get(i));
                //fOut.write((byte) sensorData.get(i)[0]);
            }
            //fOut.close();
            fileWriter.close();
            x_sensorData.clear();
            y_sensorData.clear();
            z_sensorData.clear();
            sensorTimestamps.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Tell the user we stopped.
        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
        nManager.cancel(R.string.foreground_service_started);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void showNotification() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.service_channel);
            String description = getString(R.string.channel_accel_service);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            String channelId = "acceltofile.NotificationChannelID";
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            nManager.createNotificationChannel(channel);
            // Set the info for the views that show in the notification panel.
            Notification notification = new Notification.Builder(this, channelId)
                    .setContentTitle("Accelerometer")  // the contents of the entry
                    .setContentText("Recording data")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();

            // Send the notification.
            //The notify method will show an extra notification bar (will cause dual notification bars)
            //nManager.notify(R.string.foreground_service_started, notification);
            startForeground(R.string.foreground_ID, notification);
        } else {
            Notification notification = new Notification.Builder(this)
                    .setContentTitle("Accelerometer")  // the contents of the entry
                    .setContentText("Accelerometer data")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();

            // Send the notification.
            //The notify method will show an extra notification bar (will cause dual notification bars)
            //nManager.notify(R.string.foreground_service_started, notification);
            startForeground(R.string.foreground_ID, notification);
        }
    }
}
