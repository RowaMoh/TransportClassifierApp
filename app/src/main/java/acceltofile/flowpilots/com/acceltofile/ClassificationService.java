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
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class ClassificationService extends Service implements SensorEventListener {

    String channelId = "acceltofile.NotificationChannelID";
    Sensor accel;
    SensorManager sensorManager;
    NotificationManager nManager;
    Notification notification;
    ArrayList<Float> x_sensorData = new ArrayList<>();
    ArrayList<Float> y_sensorData = new ArrayList<>();
    ArrayList<Float> z_sensorData = new ArrayList<>();
    //Classes are the rows, features the columns -> [C,F]
    float[][] variance = new float[4][3];
    float[][] mean = new float[4][3];
    String[] labels = {"Still", "Walking", "Biking", "Driving"};
    HashMap <String, Integer> labelmap = new HashMap<>();
    final float add_variance = 3.444191593582945e-06f;
    //Probability per class (of trained model in python)
    final float[] classPriors = new float[4];
    final int windowSize = 512;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onCreate() {
        super.onCreate();
        //Wait 3 seconds before starting classification
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
        //Enter the labels as unique keys
        for (String label : labels) {
            labelmap.put(label, 0);
        }

        classPriors[0] = 0.28318584f;
        classPriors[1] = 0.24778761f;
        classPriors[2] = 0.30973451f;
        classPriors[3] = 0.15929204f;

        //shape (n_classes, n_features)
        variance[0][0] = 1.39508513e+01f;
        variance[0][1] = 6.28767587e+01f;
        variance[0][2] = 4.04245870e-02f;
        variance[1][0] = 7.20833085f;
        variance[1][1] = 2.41560781e+02f;
        variance[1][2] = 2.70123613e-01f;
        variance[2][0] = 2.62165295e+01f;
        variance[2][1] = 1.93599034e+02f;
        variance[2][2] = 3.16687473e-01f;
        variance[3][0] = 1.80547364e+01f;
        variance[3][1] = 1.60263620e+02f;
        variance[3][2] = 1.74713993e-01f;

        mean[0][0] = 2.33901018f;
        mean[0][1] = 12.57467421f;
        mean[0][2] = 0.45954404f;
        mean[1][0] = 34.87203426f;
        mean[1][1] = 209.22345829f;
        mean[1][2] = 7.27387433f;
        mean[2][0] = 15.3230104f;
        mean[2][1] = 88.12663292f;
        mean[2][2] = 3.27875212f;
        mean[3][0] = 6.31621976f;
        mean[3][1] = 32.09242847f;
        mean[3][2] = 1.1854022f;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Classification Started", Toast.LENGTH_LONG).show();
        //SENSOR_DELAY_NORMAL = 0.2 seconds sampling rate
        //sensorManager.registerListener((SensorEventListener) this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        //10.000 us = 0.01s = 100Hz
        sensorManager.registerListener(this, accel, 10000);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        x_sensorData.add(event.values[0]);
        y_sensorData.add(event.values[1]);
        z_sensorData.add(event.values[2]);
        //Log.i("sensorData", "sensordata size, before: "+x_sensorData.size());
        //Samples sizes equal window sizes in analysis phase
        if(x_sensorData.size() >= windowSize){
            //Log.i("sensorData", "sensordata size, after: "+x_sensorData.size());
            float[] magnitudes = getMagnitude(x_sensorData, y_sensorData, z_sensorData);
            float[] features = getFeatures(magnitudes);
            float[] pred = classifyNB(features);
            //Log.i("CLASSIFICATION LABEL", "CLASS------> "+Integer.toString((int)pred[0]));

            //Add to probability score of the label
            int labelNumber = (int)pred[0];
            int currentScore = labelmap.get(labels[labelNumber]);
            labelmap.put(labels[labelNumber], currentScore+1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //!!!Do NOT use this method while profiling power consumption!!!
                notifyUser((int)pred[0]);
            }
            //Muy importante
            x_sensorData.clear();
            y_sensorData.clear();
            z_sensorData.clear();
            /*
            //Wait 3 seconds before re-classifying
            try {
                sensorManager.unregisterListener(this);
                sleep(1000);
                sensorManager.registerListener(this, accel, 10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } */
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void notifyUser(int label) {
        //Log.i("classification", "CLASS------> "+labels[label]);
        notification = new Notification.Builder(this, channelId)
                .setContentTitle("You are: "+labels[label])
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        // Send the notification.
        //The notify method will show an extra notification bar (will cause dual notification bars)
        //nManager.getNotificationChannel(channelId).setImportance(nManager.IMPORTANCE_MIN);
        nManager.notify(R.string.foreground_service_started, notification);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        // Tell the user we stopped.
        Toast.makeText(this, "Classification stopped", Toast.LENGTH_SHORT).show();

        int max = 0;
        String label = "";
        for (Map.Entry<String, Integer> pair: labelmap.entrySet()) {
            if(pair.getValue() > max){
                max = pair.getValue();
                label = pair.getKey();
            }
        }
        notification = new Notification.Builder(this, channelId)
                .setContentTitle("You were mostly: "+label)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        // Send the notification.
        //The notify method will show an extra notification bar (will cause dual notification bars)
        //nManager.getNotificationChannel(channelId).setImportance(nManager.IMPORTANCE_MIN);
        nManager.notify(R.string.end_classification, notification);

        nManager.cancel(R.string.foreground_service_started);
        super.onDestroy();
    }

    public float[] getMagnitude(ArrayList<Float> X, ArrayList<Float> Y, ArrayList<Float> Z){
        float[] magnitudes = new float[windowSize];
        for(int i=0;i<windowSize;i++){
            magnitudes[i] = (float) Math.sqrt(Math.pow(X.get(i),2)+Math.pow(Y.get(i),2)+Math.pow(Z.get(i),2));
        }
        return magnitudes;
    }

    public float[] getFeatures(float[] mag){
        //3 different features per window (one in this case)
        float sum = 0;
        float squaredSum = 0;
        float max = 0;
        float[] features = new float[3];
        for (float aMag : mag) {
            sum += aMag;
            squaredSum += Math.pow(aMag, 2);
            if (aMag > max) {
                max = aMag;
            }
        }
        //Maximum feature
        features[0] = max;
        //Norm feature
        features[1] = (float) Math.sqrt(squaredSum);
        //Mean feature
        features[2] = sum/mag.length;
        return features;
    }

    //Gaussian naive Bayes classification method using MAP rule (Max. a Posteriori)
    //param x are the different features of acquired data
    public float[] classifyNB(float[] x){
        //First index = label, second index = likelihood (not probability)
        float[] maxPosterior = new float[2];
        maxPosterior[0] = 0;
        maxPosterior[1] = 0;

        //Label (or class value) is the row axis, feature the column axis
        for(int label=0;label<labels.length;label++){
            float posteriorProduct = 1;
            for(int feat=0;feat<mean[0].length;feat++){
                //Likelihood calculated here using Gaussian/normal distribution
                float likelihood = (float)(Math.exp(-1 * (Math.pow((x[feat] - mean[label][feat]),2) / (2 * variance[label][feat])))) / (float)(Math.sqrt(2 * Math.PI * variance[label][feat]));
                //Product of posteriors per label placed in array multiplied by class priors
                posteriorProduct *= likelihood;
            }
            float tempPost = classPriors[label] * posteriorProduct;
            if(tempPost > maxPosterior[1]){
                maxPosterior[0] = label;
                maxPosterior[1] = tempPost;
            }
        }
        //Log.i("likelihood", "Likelihood------> "+likelihood[1]);
        return maxPosterior;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void showNotification() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.service_channel);
            String description = getString(R.string.channel_accel_service);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            nManager.createNotificationChannel(channel);
            // Set the info for the views that show in the notification panel.
            notification = new Notification.Builder(this, channelId)
                    .setContentTitle("Classification")  // the contents of the entry
                    .setContentText("Classifying transport")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setOnlyAlertOnce(true)
                    .build();

            // Send the notification.
            //The notify method will show an extra notification bar (will cause dual notification bars)
            //nManager.notify(R.string.foreground_service_started, notification);
            startForeground(R.string.foreground_ID, notification);
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("Classification")  // the contents of the entry
                    .setContentText("Classifying transport")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();

            // Send the notification.
            //The notify method will show an extra notification bar (will cause dual notification bars)
            //nManager.notify(R.string.foreground_service_started, notification);
            startForeground(R.string.foreground_ID, notification);
        }
    }

}
