package com.example.cppnguide;

import org.opencv.core.Mat;

import java.io.Serializable;

public class Location implements Serializable{
    private double x;
    private double y;
    private String name;
    private int Angle;
    private String direction;

    public Location(double x, double y,int angle, String direction,String name){
        this.x = x;
        this.y = y;
        this.Angle = angle;
        this.direction = direction;
        if(name == "0"){
            this.name = "Hallway";
        }
        else {
            this.name = name;
        }
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
    public int getAngle(){
        return this.Angle;
    }

    public String getDirection() {return this.direction; }
}
