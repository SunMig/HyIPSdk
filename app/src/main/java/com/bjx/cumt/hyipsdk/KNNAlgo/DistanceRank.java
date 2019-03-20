package com.bjx.cumt.hyipsdk.KNNAlgo;

/**
 * Created by cumt_bjx on 2018/3/16.
 */

public class DistanceRank implements Comparable<DistanceRank>{
    private int Distance_ID=0;
    private double Distance=0d;
    //    private MPoint mPoint=null;
    //依据距离进行排序
    @Override
    public int compareTo(DistanceRank another){
        return getDistance().compareTo(another.getDistance());
    }

    @Override
    public Object clone() throws CloneNotSupportedException{
        return super.clone();
    }

    public int getID(){
        return Distance_ID;
    }
    public void setID(int id){
        Distance_ID=id;
    }

    public Double getDistance(){
        return Distance;
    }

    public void setDistance(double distance){
        Distance=distance;
    }
}
