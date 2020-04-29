package acceltofile.flowpilots.com.acceltofile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Debug;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity{

    Button record_button, classify_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        record_button = (Button) findViewById(R.id.record_button);
        classify_button = (Button) findViewById(R.id.classify_button);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Here, this is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onRecordClick(View view) {
        if(record_button.getText().equals("Record")) {
            record_button.setText("Stop");
            Intent intent = new Intent(this, AcceleroRecordService.class);
            startForegroundService(intent);
        } else {
            record_button.setText("Record");
            Intent intent = new Intent(this, AcceleroRecordService.class);
            stopService(intent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onClassifyClick(View view) {
        if(classify_button.getText().equals("Classify")) {
            classify_button.setText("Stop");
            Intent intent = new Intent(this, ClassificationService.class);
            startForegroundService(intent);
        } else {
            classify_button.setText("Classify");
            Intent intent = new Intent(this, ClassificationService.class);
            stopService(intent);
        }
    }

}
