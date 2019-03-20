package com.bjx.cumt.hyipsdk.KNNAlgo;

import android.util.Log;

import com.bjx.cumt.hyipsdk.jama.Matrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by hc on 2018/7/21.
 */

public class Mdtw {
    private static String  TAG = "Mdtw";

    public Mdtw() {

    }
    public static List<DistanceRank> getdtwdistance(List<ReferPoint> referPointList, ReferPoint testPoint) {
        Log.i(TAG," 计算距离");
        if (referPointList==null){
            return null;
        }
        if (testPoint==null){
            return null;
        }
        if (referPointList.size()==0){
            Log.i(TAG,"参考点的表为空");
            return null;
        }
        Matrix testPointSignalAttr=testPoint.getPointSingalAttrMat();
        //新建一个列表用于存储距离
        List<DistanceRank> distanceList=new ArrayList<DistanceRank>();
        //计算dtw距离
        for (int i=0;i<referPointList.size();i++){
            //Log.i(TAG,"匹配指纹个数"+","+i);
            //Log.i(TAG, String.valueOf(referPointList.size()));
            double mdtwdistance=mdtw(referPointList.get(i),testPointSignalAttr);
            Log.i(TAG,"与每个测试指纹之间的dtw距离"+mdtwdistance);
            DistanceRank DR = new DistanceRank();
            DR.setDistance(mdtwdistance);
            DR.setID((int) referPointList.get(i).getId());
            //将其放入到list中就不用使dr为全局变量
            distanceList.add(DR);
        }
        return distanceList;
    }
    private static double mdtw(ReferPoint referPoint, Matrix testPointSignalAttr){
        //计算dtw距离
        int M=referPoint.getPointSingalAttrMat().getRowDimension()-2;
        int N=testPointSignalAttr.getRowDimension();
        Matrix Eucdistance=new Matrix(N,M);
//        Matrix tp=new Matrix(M,N);
//        Matrix fp=new Matrix(M,N);
//        //将地磁指纹向量扩大
//        for (int i=0;i<N;i++){
//            //Log.i(TAG, String.valueOf(i));
//            double geoMcompont=testPointSignalAttr.get(i,0);
//            for (int j=0;j<M;j++){
//                //Log.i(TAG, String.valueOf(j));
//                tp.set(j,i,geoMcompont);
//            }
//        }
//        //参考点指纹扩大
//        for (int i=0;i<M;i++){
//            double geoMcompont=referPoint.getPointSingalAttrMat().get(i,0);
//            for (int j=0;j<N;j++){
//                fp.set(i,j,geoMcompont);
//            }
//        }
        //计算与指纹库中指纹的欧式距离
        //Log.i(TAG, fp.toString());
        //欧式距离矩阵， //记得开根号
//        Matrix dist = tp.minus(fp);
//        Eucdistance = (dist.arrayTimes(dist));
        Log.i(TAG,"维度"+","+M+","+N);
        for(int i=0;i<N;i++){
            double geoCompent=testPointSignalAttr.get(i,0);
            for (int j=0;j<M;j++){
                double geofp=referPoint.getPointSingalAttrMat().get(j,0);
                double eucdist=Math.sqrt((geoCompent-geofp)*(geoCompent-geofp));
                Eucdistance.set(i,j,eucdist);
                Log.i(TAG, String.valueOf(eucdist)+","+geoCompent+","+geofp);
            }
        }
        Matrix Dtwdistance=new Matrix(N,M);
        //根据欧式距离计算dtw距离
        Dtwdistance.set(0,0,Eucdistance.get(0,0));
        for(int i=1;i<M;i++){
            //Log.i(TAG,"矩阵维度"+","+i);
            Dtwdistance.set(0,i,Dtwdistance.get(0,i-1)+Eucdistance.get(0,i));
            //Log.i(TAG,"第一行距离"+","+(Dtwdistance.get(0,i-1)+","+Eucdistance.get(0,i)));
        }
        for (int i=1;i<M;i++){
            for (int j=0;j<N;j++){
                if(j==0){
                    Dtwdistance.set(i,j,Dtwdistance.get(i-1,j)+Eucdistance.get(i,j));
                }else{
                    List<Double> md=new ArrayList<Double>();
                    md.add(Dtwdistance.get(i-1,j)+Eucdistance.get(i,j));
                    md.add(Dtwdistance.get(i-1,j-1)+Eucdistance.get(i,j));
                    md.add(Dtwdistance.get(i,j-1)+Eucdistance.get(i,j));
                    double mdmin= Collections.min(md);
                    Log.i(TAG,"对应位置dtw距离"+","+mdmin+","+md);
                    Dtwdistance.set(i,j,mdmin);
                    //将md清空
                    md.clear();
                }
            }
        }
        //dtw 距离为
        double mdtwdisstance = Dtwdistance.get(M-1, N-1);
        //Log.i(TAG, "dtw距离:"+","+String.valueOf(mdtwdisstance));
        return mdtwdisstance;
    }

}
