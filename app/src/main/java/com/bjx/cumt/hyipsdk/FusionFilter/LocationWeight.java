package com.bjx.cumt.hyipsdk.FusionFilter;

/**
 * Created by hc on 2018/7/1.
 */

public  class LocationWeight {
    public static double psweight(String str,int par){
        String[] rssi=str.split(",");
        double PSW=0;
        for(int i=0;i<rssi.length-1;i++){
            PSW=PSW+par/Math.abs(new Double(rssi[i]));
        }
        return PSW;
    }
}
