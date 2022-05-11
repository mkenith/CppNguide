package com.example.cppnguide;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cppnguide.OpenCVCamera;
import com.example.cppnguide.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'nguidecpp' library on application startup.
    private Button opencvcam;
    private Button navigation;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_CODE = 110;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_CODE = 120;
    private static final int ACTIVITY_RECOGNITION_PERMISSION_CODE = 130;
    static {
        System.loadLibrary("cppnguide");
    }

    private ActivityMainBinding binding;
    private ObjectDetector objectDetector;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        opencvcam = (Button)findViewById(R.id.opencvcam);
        opencvcam.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
                    {
                        requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_PERMISSION_CODE);
                    }
                    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_CODE);
                    }
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
                    }
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                    {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                    }
                    else{
                        Toast.makeText(getBaseContext(), "camera permission granted", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this,OpenCVCamera.class);
                        startActivity(intent);
                    }
                }
            }
        });
        navigation = findViewById(R.id.navigation);
        navigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,Navigation.class);
                startActivity(intent);
            }
        });

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode== ACTIVITY_RECOGNITION_PERMISSION_CODE){
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, " activity recognition denied", Toast.LENGTH_LONG).show();
            }
        }
        if(requestCode== WRITE_EXTERNAL_STORAGE_PERMISSION_CODE){
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, " write external storage permission denied", Toast.LENGTH_LONG).show();
            }
        }
        if(requestCode== READ_EXTERNAL_STORAGE_PERMISSION_CODE){
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, " read external storage permission denied", Toast.LENGTH_LONG).show();
            }

        }

        if (requestCode == CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Intent intent = new Intent(MainActivity.this, OpenCVCamera.class);
                startActivity(intent);
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }

    }

    /**
     * A native method that is implemented by the 'nguidecpp' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}