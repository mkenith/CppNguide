package com.example.cppnguide;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
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

public class NavigationCamera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "Navigation";
    private CameraBridgeViewBase cameraBridgeViewBase;
    private Mat mRGBA = new Mat();
    private ObjectDetector objectDetector;
    private List<Location> Locations;
    private TextView score;
    private TextView index;
    private TextView location;
    private int count = 0;
    private int lastIndex = 0;
    private String []result = new String[2];
    static {
        System.loadLibrary("cppnguide");
    }


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
        Locations = new ArrayList<>();
        LoadFile();
        Toast.makeText(getBaseContext(),""+getBaseContext().getExternalFilesDir(null).getAbsolutePath(),Toast.LENGTH_LONG).show();


        try{
            objectDetector = new ObjectDetector(getAssets(),"ssd_mobilenet.tflite","labelmap.txt",300);

            Toast.makeText(getBaseContext(), "Loaded object detection Model", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            Toast.makeText(getBaseContext(), "Not loaded object detection Model", Toast.LENGTH_SHORT).show();
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
        if(count%10==0) {
            Mat resizeimage = new Mat();
            Size scaleSize = new Size(640, 480);
            Imgproc.resize(mRGBA, resizeimage, scaleSize);
            Imgproc.cvtColor(resizeimage, resizeimage, Imgproc.COLOR_RGBA2GRAY);
            String imageName = getBaseContext().getExternalFilesDir(null).getAbsolutePath() + "/query.jpg";
            Imgcodecs.imwrite(imageName, resizeimage);
            String res = navigation(""+getBaseContext().getExternalFilesDir(null).getAbsolutePath());
            result = res.split(",");
            if(Math.abs(lastIndex - Integer.parseInt(result[0]))<=5){
                lastIndex = Integer.parseInt(result[0]);
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        index.setText("Index: "+result[0]);
                        score.setText("Score: "+result[1]);
                        location.setText("Location: "+(int)Locations.get(lastIndex).getX()+","+(int)Locations.get(lastIndex).getY());

                    }
                });
            }

        }
        count++;
        mRGBA = objectDetector.recognizeImage(mRGBA);
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