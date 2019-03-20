package com.bjx.cumt.hyipsdk.geomagneticLoc;

import android.util.Log;

import com.bjx.cumt.hyipsdk.KNNAlgo.MPoint;
import com.bjx.cumt.hyipsdk.KNNAlgo.ReferPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hc on 2018/7/21.
 */

public class Mreadfingerprint {
    private static String TAG="Mreadfingerprint";
    public static List<ReferPoint> readRMList(String fileString){
        File radioMapFile=new File(fileString);
        String readLineString="";
        List<ReferPoint> referPointAttrList=new ArrayList<ReferPoint>();
        Log.i("路径：",fileString);
        try{
            FileReader frin = new FileReader(radioMapFile);
            BufferedReader brin = new BufferedReader(frin);
            while ((readLineString = brin.readLine()) != null) {
                ReferPoint referPoint = null;
                MPoint geoPoint = new MPoint();
                readLineString = readLineString.trim();
                String[] arrayStrings = readLineString.split(",");
                int mSize = arrayStrings.length;
                Log.i(TAG,arrayStrings.toString());
                //需加z方向的数据信息
                referPoint = new ReferPoint(Long.parseLong(arrayStrings[0]));
                geoPoint.x = Double.parseDouble(arrayStrings[1].trim());
                geoPoint.y = Double.parseDouble(arrayStrings[2].trim());
                referPoint.setPointCoor(geoPoint.x, geoPoint.y);
                int matrix_Size=mSize-3;
                double[][] signal_mat=new double[matrix_Size][1];
                for (int i=3;i<mSize;i++){
                    if (arrayStrings[i].equals("0")){
                        signal_mat[i-3][0]=-105d;
                    }else{
                        signal_mat[i-3][0]=Double.parseDouble(arrayStrings[i]);
                    }
                }
                referPoint.addSignalAttr(signal_mat);
                referPointAttrList.add(referPoint);
            }
            brin.close();
            frin.close();
            Log.i(TAG,"读取数据至List中");
        }catch (Exception e){
            e.printStackTrace();
            Log.i("Hybrid Location Handler","读取文件至参考点列表存在错误");
        }
        return referPointAttrList;
    }
}
