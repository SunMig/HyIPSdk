package com.bjx.cumt.hyipsdk.FusionFilter;

/**
 * Created by dell on 2018/3/29.
 */

public interface HybLocationListener {
    /**
     * 获得定位位置
     * @param x
     * @param y
     * @param theta 角度
     */
//    public void onLocation(double x, double y,float theta);

    public void onLocation(double x, double y, String floor);

}
