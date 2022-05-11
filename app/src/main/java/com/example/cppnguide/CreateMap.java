package com.example.cppnguide;

import android.app.Activity;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CreateMap extends Activity {
    private List rotationVectors;
    private String baseStorage;
    private List roomArea;
    private List<Location> Locations;
    private int stepCount;
    public String response;
    private int num_image;

    static {
        System.loadLibrary("cppnguide");
    }

    public CreateMap(String baseStorage, List rotationVectors,List roomArea,int num_image,int steps){
        this.rotationVectors = rotationVectors;
        this.baseStorage = baseStorage;
        this.roomArea = roomArea;
        this.Locations = new ArrayList<Location>();
        this.response = createVocabulary(num_image,baseStorage);
        this.num_image = num_image;
        this.stepCount = steps;
    }
    public void create(){
        double current_x = 0;
        double current_y = 0;
        double prev_x = 0;
        double prev_y = 0;
        double step_length = 1;
        int prevAngle = 0;
        for(int i = 0; i< stepCount;i++){
            String direction = getDirection(rotationVectors.indexOf(i),prevAngle);
            prevAngle = rotationVectors.indexOf((i));
            Locations.add(new Location(current_x,current_y,rotationVectors.indexOf(i),direction));
            current_x =  prev_x + (step_length * Math.sin(Math.toRadians(rotationVectors.indexOf(i))));
            current_y =  prev_y + (step_length * Math.cos(Math.toRadians(rotationVectors.indexOf(i))));
            prev_x = current_x;
            prev_y = current_y;
        }
    }
    public  String getDirection(int current,int target){
        int thresh = 20;
        double angleDiff,diff;
        angleDiff = current - target;
        angleDiff = ((angleDiff + 180) % 360) - 180;
        if(Math.abs(angleDiff)<thresh){
            return "forward";
        }
        diff = target - current;
        if(diff < 0) {
            diff += 360;
        }
        if(diff > 180) {
            return "right";
        }
        else {
            return "left";
        }
    }

    public int compareImages(Mat prev, Mat current) {

        ORB orb = ORB.create();
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        // first image
        Mat descriptors1 = new Mat();
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        orb.detect(prev, keypoints1);
        orb.compute(prev, keypoints1, descriptors1);
        // second image
        Mat descriptors2 = new Mat();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();

        orb.detect(current, keypoints2);
        orb.compute(current, keypoints2, descriptors2);
        // match these two keypoints sets
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptors1, descriptors2, matches);
        return matches.size(1);
    }

    public native String createVocabulary(int num_images,String path);


}
