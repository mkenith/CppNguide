package com.example.cppnguide;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class NavigationCamera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "Navigation";
    private static final int REQUEST_CODE_SPEECH_INPUT = 1090;
    private CameraBridgeViewBase cameraBridgeViewBase;
    private Mat mRGBA = new Mat();
    private ObjectDetector objectDetector;
    private List<Location> Locations;
    private TextView score;
    private TextView index;
    private TextView location;
    private int count = 0;
    private String destination;
    private List<Location> path;
    private String []result = new String[2];
    private int destination_index = 0;
    private int currentIndex = 0;
    private boolean navigating = false;
    private TextView navigation;
    private TextToSpeech textToSpeech;
    private TextView steps_estimation;

    private Button speak;
    static {
        System.loadLibrary("cppnguide");
    }

    private int lastIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_navigation);
        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.camera_view_2);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(NavigationCamera.this);
        score = findViewById(R.id.score);
        index = findViewById(R.id.index);
        location = findViewById(R.id.location);
        speak = findViewById(R.id.speak);
        navigation = findViewById(R.id.destination);
        Locations = new ArrayList<>();
        path = new ArrayList<>();
        steps_estimation = findViewById(R.id.step_estimation);

        LoadFile();
        Toast.makeText(getBaseContext(),""+getBaseContext().getExternalFilesDir(null).getAbsolutePath(),Toast.LENGTH_LONG).show();

        try{
            objectDetector = new ObjectDetector(getAssets(),"ssd_mobilenet.tflite","labelmap.txt",300);
            Toast.makeText(getBaseContext(), "Loaded object detection Model", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            Toast.makeText(getBaseContext(), "Not loaded object detection Model", Toast.LENGTH_SHORT).show();
        }

        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            onPause();
                Intent intent
                        = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                        Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");
                try {
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
                }
                catch (Exception e) {
                    Toast.makeText(getBaseContext(), " " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data)
    {
        onResume();
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                destination = result.get(0);
                boolean hasRoom = false;
                destination_index = 0;
                for(int i = 0 ;i < Locations.size(); i++ ){
                    if((Locations.get(i).getName()).toLowerCase().equals(destination.toLowerCase())){
                        destination_index = i;
                        destination = Locations.get(i).getName();
                        hasRoom = true;
                        break;
                        }
                    }
                if(hasRoom){
                    Toast.makeText(getBaseContext(),"Room is found! Navigating to room - "+ destination,Toast.LENGTH_LONG).show();
                    navigating = true;
                }
                else{
                    Toast.makeText(getBaseContext(),"Room is not found!" ,Toast.LENGTH_LONG).show();
                    destination = "";
                }
            }
        }
    }
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    cameraBridgeViewBase.setCameraPermissionGranted();
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
    @Override
    public void onResume(){
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        if(navigating==true) {
            if (count % 10 == 0) {
                Mat resizeimage = new Mat();
                Size scaleSize = new Size(640, 480);
                Imgproc.resize(mRGBA, resizeimage, scaleSize);
                Imgproc.cvtColor(resizeimage, resizeimage, Imgproc.COLOR_RGBA2GRAY);
                String imageName = getBaseContext().getExternalFilesDir(null).getAbsolutePath() + "/query.jpg";
                Imgcodecs.imwrite(imageName, resizeimage);
                String res = navigation("" + getBaseContext().getExternalFilesDir(null).getAbsolutePath());
                result = res.split(",");
                if (Math.abs(lastIndex - Integer.parseInt(result[0])) <= 10 && Integer.parseInt(result[0])>=currentIndex && Integer.parseInt(result[0])<=destination_index ) {
                    lastIndex = Integer.parseInt(result[0]);
                    currentIndex = Integer.parseInt(result[0]);
                    if(destination_index == currentIndex){
                        navigating = false;
                        runOnUiThread(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run() {
                                 textToSpeech = new TextToSpeech(getBaseContext(), new TextToSpeech.OnInitListener() {
                                    @Override
                                    public void onInit(int i) {
                                        textToSpeech.setLanguage(Locale.US);
                                        textToSpeech.speak("Arrived at " +destination,TextToSpeech.QUEUE_FLUSH,null,null);
                                    }
                                });


                            }
                        });
                    }
                    runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            navigation.setText("Navigating: "+destination +" ("+ destination_index+")" );
                            index.setText("Index: " + result[0]);
                            score.setText("Score: " + result[1]);
                            location.setText("Location: " + (int) Locations.get(lastIndex).getX() + "," + (int) Locations.get(lastIndex).getY());
                            steps_estimation.setText("Remaining Steps: "+(destination_index - currentIndex));
                        }
                    });
                }
            }
            count++;
        }
       // mRGBA = objectDetector.recognizeImage(mRGBA);
        return mRGBA;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase!= null){
            cameraBridgeViewBase.disableView();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!= null){
            cameraBridgeViewBase.disableView();
        }
    }
    private void LoadFile(){
        try{
            FileInputStream readData = new FileInputStream(getBaseContext().getExternalFilesDir(null).getAbsolutePath()+"/Locations.ser");
            ObjectInputStream readStream = new ObjectInputStream(readData);
            Locations = (ArrayList<Location>) readStream.readObject();
            readStream.close();
            Toast.makeText(getBaseContext(),"Loaded Serialized File",Toast.LENGTH_LONG).show();

        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    private native String navigation(String path);
}