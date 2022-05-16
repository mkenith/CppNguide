package com.example.cppnguide;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapCreationCamera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat mRGBA, imageToSave;

    private static final String TAG = "OpenCVCamera";
    private CameraBridgeViewBase cameraBridgeViewBase;

    private TextView step_view;
    private TextView angle_view;

    private int angle = 0;
    private int stepCount = 0;
    private int prevStepCount =0;
    private boolean detected = false;
    private boolean consumed = false;


    private double currentX = 0;
    private double currentY = 0;
    private double prevX = 0;
    private double prevY = 0;
    private int prevAngle = 0;


    private Mat prev;
    private int count = 0;
    private int imageCount = 0;

    private List roomDetection;

    private Boolean isRecording = false;
    private Button record;
    private String baseStorage;

    private ProgressBar pb;
    private Button add_room;
    private List<Integer> rotationVectors;
    private List<String> rooms;
    private boolean added_room = false;
    private int roomCount = 0;
    private TextView roomsView;
    private int num_image=0;
    private StaticStepCounter ssc;
    private TextToSpeech textToSpeech;




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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_open_cvcamera);
        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(MapCreationCamera.this);


        step_view = findViewById(R.id.step);
        angle_view = findViewById(R.id.angle);
        record = findViewById(R.id.Record);
        add_room =findViewById(R.id.addRoom);
        roomsView = findViewById(R.id.rooms);

        rooms = new ArrayList<String>();
        rotationVectors = new ArrayList<Integer>();
        ssc = new StaticStepCounter(10.5,10);

        add_room.setBackgroundColor(Color.GREEN);
        add_room.setVisibility(View.INVISIBLE);

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                textToSpeech.setLanguage(Locale.US);
                textToSpeech.speak("Map Creation.",TextToSpeech.QUEUE_FLUSH,null,null);
            }
        });

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        CreateMapAlgorithm cm = new CreateMapAlgorithm(getBaseContext().getExternalFilesDir(null).getAbsolutePath(),baseStorage,rotationVectors,rooms,num_image,stepCount);
                        cm.create();
                        num_image = 0;
                        stepCount = 0;
                        imageCount = 0;
                        Toast.makeText(getBaseContext(),"Response: "+ cm.response,Toast.LENGTH_LONG).show();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        deleteRecursive(new File(baseStorage));
                        break;
                }
            }
        };

        add_room.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                roomCount++;
                roomsView.setText("Rooms Added: "+(roomCount));
                added_room = true;
                textToSpeech.speak("Room Added.",TextToSpeech.QUEUE_FLUSH,null,null);

            }
        });

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isRecording){
                    onPause();
                    record.setBackgroundColor(Color.BLUE);
                    record.setText("Record");
                    isRecording = false;
                    add_room.setVisibility(View.INVISIBLE);

                    AlertDialog.Builder builder = new AlertDialog.Builder(MapCreationCamera.this);
                    builder.setMessage("Do you want to create the map?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();

                }
                else{
                    onResume();
                    String timeStamp = new SimpleDateFormat("dd_MM_yyyy_HH_mm_SS").format(new Date());
                    File folder = new File( getBaseContext().getExternalFilesDir(null).getAbsolutePath()+
                            File.separator +"nGuideCPP"+  File.separator+ timeStamp);
                    boolean success = false;
                    if (!folder.exists()) {
                        success = folder.mkdirs();
                    }
                    baseStorage = getBaseContext().getExternalFilesDir(null).getAbsolutePath() + "/nGuideCPP/" + timeStamp  ;
                    File outFolder = new File(baseStorage + "/Images");
                    if (!outFolder.exists()) {
                        outFolder.mkdirs();
                    }
                    outFolder = new File(baseStorage + "/Map");
                    if (!outFolder.exists()) {
                        outFolder.mkdirs();
                    }

                    outFolder = new File(baseStorage + "/Files");
                    if (!outFolder.exists()) {
                        outFolder.mkdirs();
                    }

                    try{
                        rooms.clear();
                        rotationVectors.clear();
                    }catch(Exception e) {

                    }
                    count = 0;
                    roomCount = 0;
                    add_room.setVisibility(View.VISIBLE);
                    roomsView.setText("Rooms Added: 0");
                    stepCount = 0;
                    step_view.setText("Steps: 0");
                    record.setBackgroundColor(Color.RED);
                    record.setText("Recording...");
                    isRecording = true;
                }
            }
        });


        android.hardware.SensorManager sensorManager = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensorRotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Sensor sensorAccelometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        SensorEventListener sensorEventListenerRotationVector = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] rotationMatrix = new float[16];
                android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                float[] remappedRotationMatrix = new float[16];
                android.hardware.SensorManager.remapCoordinateSystem(rotationMatrix, android.hardware.SensorManager.AXIS_X, android.hardware.SensorManager.AXIS_Z,
                        remappedRotationMatrix);
                float[] orientations = new float[3];
                android.hardware.SensorManager.getOrientation(remappedRotationMatrix, orientations);
                angle = (int) ((Math.toDegrees(orientations[0]) + 360) % 360);
                angle_view.setText("Direction: "+angle);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        SensorEventListener sensorEventListenerAccelometer = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(isRecording){
                    float upperThreshold = 10.5f;
                    float lowerTreshold = 10.0f;

                    float x_acceleration = event.values[0];
                    float y_acceleration = event.values[1];
                    float z_acceleration = event.values[2];

                    double magnitude = (double) Math.sqrt(x_acceleration * x_acceleration + y_acceleration * y_acceleration + z_acceleration * z_acceleration);
                    /*
                    ssc.findStep(magnitude);
                    if(ssc.getStepCount() > stepCount){
                       stepCount = ssc.getStepCount();
                       rotationVectors.add(angle);
                       step_view.setText("Steps: "+stepCount);
                       stepCount++;
                    }

                    */

                    if (!detected) {
                        if (magnitude > upperThreshold) {
                            detected = true;
                        }
                    } else if (magnitude < lowerTreshold) {
                        detected = false;
                        consumed = false;
                    }
                    if (detected && !consumed) {
                        stepCount++;
                        consumed = true;
                        rotationVectors.add(angle);
                        step_view.setText("Steps: " + stepCount);

                    }
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        sensorManager.registerListener(sensorEventListenerRotationVector, sensorRotation, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListenerAccelometer, sensorAccelometer, SensorManager.SENSOR_DELAY_NORMAL);

    }
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
        if(isRecording) {
            /*
            if(prevStepCount < stepCount) {
                Mat resizeimage = new Mat();
                Size scaleSize = new Size(640, 480);
                Imgproc.resize(mRGBA, resizeimage, scaleSize);
                Imgproc.cvtColor(resizeimage, resizeimage, Imgproc.COLOR_RGBA2GRAY);
                if (added_room) {
                    rooms.add(roomCount);
                    added_room = false;
                    roomCount++;
                } else {
                    rooms.add(0);
                }
                rotationVectors.add(angle);
                String imageName = String.format("%010d", count) + ".jpg";
                Imgcodecs.imwrite(baseStorage + "/Images/" + imageName, mRGBA);
                num_image += 1;
                prevStepCount = stepCount;
                count++;
            }
            */
            if(count%10 == 0) {
                Mat resizeimage = new Mat();
                Size scaleSize = new Size(640, 480);
                Imgproc.resize(mRGBA, resizeimage, scaleSize);
                Imgproc.cvtColor(resizeimage, resizeimage, Imgproc.COLOR_RGBA2GRAY);
                if (added_room) {
                    rooms.add(""+roomCount);
                    added_room = false;
                } else {
                    rooms.add(""+0);
                }
                rotationVectors.add(this.angle);
                String imageName = String.format("%010d", imageCount) + ".jpg";
                Imgcodecs.imwrite(baseStorage + "/Images/" + imageName, resizeimage);
                num_image += 1;
                prevStepCount = stepCount;
                imageCount++;
            }
            count++;
        }

        mRGBA = featureDetector(mRGBA);
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
    public Mat featureDetector(Mat mRGB){
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        ORB orb = ORB.create(1000);
        orb.detect(mRGB, keypoints1);
        Mat descriptors1 = new Mat();
        orb.compute(mRGB, keypoints1, descriptors1);
        Features2d.drawKeypoints(mRGB,keypoints1,mRGB);
        return mRGB;
    }
    //FileHandler
    public void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }
    public float[] lowPassFilter( float[] input, float[] output ) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + 1.0f * (input[i] - output[i]);
        }
        return output;
    }

    // Feedback
    public void Speak(String message){
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null,null);
    }



}

