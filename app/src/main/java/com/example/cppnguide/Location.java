package com.example.cppnguide;

import org.opencv.core.Mat;

public class Location {
    private double x;
    private double y;
    private String name;
    private int Angle;
    private String direction;
    private Mat descriptor;

    public Location(double x, double y,int angle, String direction){
        this.x = 0;
        this.y = 0;
        this.Angle = angle;
        this.direction = direction;
    }
    public double getX(){
        return this.x;
    }
    public double getY(){
        return this.y;
    }
    public String getName(){
        return this.name;
    }
}
