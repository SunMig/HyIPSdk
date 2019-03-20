package com.bjx.cumt.hyipsdk.KNNAlgo;


import android.os.Handler;

import com.bjx.cumt.hyipsdk.jama.Matrix;


/**
 * 将指纹库的数据存储到这里
 * 点号，x,y,h,rssi1,rssi2,rssi3,……
 * Created by cumt_bjx on 2018/3/16.
 */

public class ReferPoint {
    private static String TAG="ReferPoint";
    private long pointId=0l;
    private MPoint mPoint4Coordinate=null;
    private Matrix pointSingalAttrMat=null;
    private static Handler referPointHandler;
    private String scanTimeStr;
    public ReferPoint(long id){
        //赋值点号
        this.pointId=id;
        //生成坐标存储对象
        this.mPoint4Coordinate=new MPoint();
        //生成信号强度存储对象
//        this.pointSingalAttrMat=new Hashtable<String, Integer>();


    }
    public ReferPoint(){
        this.mPoint4Coordinate=new MPoint();

    }

    public long getId(){
        return pointId;
    }

    public void setId(long id){
        pointId=id;
    }

    public void setPointCoor(double x,double y){
        mPoint4Coordinate.x=x;
        mPoint4Coordinate.y=y;
    }

    public void setPointCoor(int x,int y){
        mPoint4Coordinate.x=x;
        mPoint4Coordinate.y=y;
    }
    public MPoint getMPoint(){
        return mPoint4Coordinate;
    }
    public void setScanTime(String scanTimeStr){
        this.scanTimeStr=scanTimeStr;
    }
    public String getScanTime(){
        return this.scanTimeStr;
    }
    //    % 按照列来存储 80-by-1
    public void addSignalAttr(double[][] vals){

        pointSingalAttrMat=new Matrix(vals);
//        Log.i(TAG,"KNN里组合的信号指纹是："+pointSingalAttrMat.toString());
    }

    public Matrix getPointSingalAttrMat(){
        return pointSingalAttrMat;
    }

    /**
     * 这部分先留着，等蓝牙指纹采集定下来后再描述？
     * 给定的是逗号分割的字符串
     * @return
     */
    public static ReferPoint buildReferPointList(double[][] vals){
        //对信号指纹进行分割
        ReferPoint testPoint=new ReferPoint(0);
        testPoint.addSignalAttr(vals);
        return testPoint;
    }
}
