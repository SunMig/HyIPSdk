package com.bjx.cumt.hyipsdk.geomagneticLoc;

import android.util.Log;

import com.bjx.cumt.hyipsdk.KNNAlgo.KNN;
import com.bjx.cumt.hyipsdk.KNNAlgo.MPoint;
import com.bjx.cumt.hyipsdk.KNNAlgo.ReferPoint;
import com.bjx.cumt.hyipsdk.util.LocalPath;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by hc on 2018/7/21.
 */

public class geomLocation extends Thread {
    private String TAG="geomLocation";
    private List<ReferPoint> mgeomagneticRadioMapList = new ArrayList<ReferPoint>();//指纹库集合
    private List<Double> MgeomagnetivList = new ArrayList<Double>();  //实时采集的地磁数据集合
    private KNN mknn=new KNN(1); //传递K值是1
    private GeoMagneticLocListener magneticLocListener;
    private double[][] scanGeoMagMat;
    private MPoint lastgeoMPoint = new MPoint(0, 0);
    private MPoint pointPlane = new MPoint(0, 0);
    private ReferPoint geoMreferpoint = new ReferPoint();
    private int Second_acquisition_time=20;
    public geomLocation() {
        init();
    }
    @Override
    public void run() {
        super.run();
        while (true) {
            Log.i(TAG, "地磁定位坐标开始"+","+MgeomagnetivList.size());
            if(MgeomagnetivList.size()==140) {
                //Log.i(TAG, "指纹库尺寸："+mgeomagneticRadioMapList.size()+mgeomagneticRadioMapList.get(0).getPointSingalAttrMat());
                //Log.i(TAG, "测试指纹1："+MgeomagnetivList.size());
                Log.i(TAG, "测试指纹1："+MgeomagnetivList.size()+","+MgeomagnetivList);
                MPoint geoMmpoint = mknn.getInterestPoint(mgeomagneticRadioMapList, getSiganlfpMatrix(Grad_diff_List(MgeomagnetivList)),1);
                //Log.i(TAG, "测试指纹2："+Grad_diff_List(MgeomagnetivList));
                //Log.i(TAG, "地磁定位坐标:"+","+geoMmpoint.x+","+geoMmpoint.y);
                if (geoMmpoint != null) {
                    MPoint tempPoint = new MPoint(geoMmpoint.x, geoMmpoint.y);
                    double dist = calDist(tempPoint, lastgeoMPoint);
                    double delX = 0d;
                    double delY = 0d;
                    if (dist > 5) {   //此处点与点之间的比对，是当前点与前一个点的比较结果
                        Log.i(TAG, "地磁定位坐标两次距离超过5m");
                        delX = 0.1;
                        delY = 0.1;
                    }
                    pointPlane.set(geoMmpoint.x + delX, geoMmpoint.y + delY);
                    lastgeoMPoint = pointPlane;
                    Log.i(TAG, "输出地磁定位的坐标为 X:" + pointPlane.x + ", Y:" + pointPlane.y);
                    magneticLocListener.onLocation(pointPlane.x, pointPlane.y);
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void init() {
        mgeomagneticRadioMapList = Mreadfingerprint.readRMList(LocalPath.Mgfpdatabase);//在这里初始化了指纹库list集合
        //Log.i(TAG,"面板数据"+scan_wifi_Map);
    }

    public void Mreceiver(List<Double> mlist) {
        this.MgeomagnetivList = new CopyOnWriteArrayList<>(mlist); //此集合是实时测量的磁数据
        //Log.i(TAG, "测试指纹："+MgeomagnetivList.size()+","+MgeomagnetivList.get(0));
    }
    //将采集的list数据进行梯度化与归一化，组成测试指纹
    private List<Double> Grad_diff_List(List<Double> mlist){
        //理论上每秒含有20组数据,传递是100组数据
        List<Double> mean_value=new ArrayList<Double>();
        List<Double> grad_diff_value=new ArrayList<Double>();
        int mag_window_size=1;
        double mag_total_value=0;
        for (int i=0;i<mlist.size();i++){
            //Log.i(TAG,"序号"+i);
            if(i==(mag_window_size)*Second_acquisition_time-1){
                mean_value.add(mag_total_value/Second_acquisition_time);
                //Log.i(TAG, "均值list" + String.valueOf(mag_total_value/Second_acquisition_time));
                //Log.i(TAG, "均值list的尺寸" + String.valueOf(mag_window_size));
                mag_window_size++;
                mag_total_value=0;
            }else{
                mag_total_value=mag_total_value+mlist.get(i);//累加
                //Log.i(TAG, "均值list" + String.valueOf(mag_total_value));
            }
        }
        Log.i(TAG, "均值list" + String.valueOf(mean_value));
        //根据获得均值求解梯度
        for (int i=0;i<mean_value.size();i++){

            if(i==0||i==mean_value.size()-1){
                if(i==0){
                    grad_diff_value.add(mean_value.get(i+1)-mean_value.get(i));
                }else{
                    grad_diff_value.add(mean_value.get(i)-mean_value.get(i-1));
                    double m=mean_value.get(i)-mean_value.get(i-1);
                    //Log.i(TAG, "梯度"+","+ String.valueOf(m));
                        }
            }else{
                    grad_diff_value.add((mean_value.get(i + 1) - mean_value.get(i - 1)) / 2);
                    double m = mean_value.get(i+1) - mean_value.get(i - 1);
                    //Log.i(TAG, "梯度" + String.valueOf(m));
            }
        }
        //根据均值求解前后1秒的误差
        for (int i=0;i<mean_value.size()-1;i++){
                grad_diff_value.add(mean_value.get(i+1)-mean_value.get(i));
        }
        Log.i(TAG, String.valueOf(grad_diff_value));
        return grad_diff_value;
    }


    //将传递进来的list集合变成一个一维数组,集合转数组
    private ReferPoint getSiganlfpMatrix(List<Double> list) {
        int geomagSize = list.size();
        Log.i(TAG,"接收扫面线程返回的数据"+geomagSize);
        double[][] scanGeoMagMat = new double[geomagSize][1];
        for (int i = 0; i < geomagSize; i++) {
            scanGeoMagMat[i][0] = list.get(i);

        }
        geoMreferpoint.addSignalAttr(scanGeoMagMat);
        return geoMreferpoint;
    }

    /**
     * 计算两个点间的距离
     */
    private double calDist(MPoint p1, MPoint p2) {
        double dist = 0d;
        double deltaX = p1.x - p2.x;
        double deltaY = p1.y - p2.y;
        dist = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
        return dist;
    }

    //监听器绑定
    public void SetLocListener(GeoMagneticLocListener locListener) {
        this.magneticLocListener=locListener;
    }
}

