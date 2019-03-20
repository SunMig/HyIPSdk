package com.bjx.cumt.hyipsdk.FusionFilter;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.bjx.cumt.hyipsdk.KNNAlgo.KNN;
import com.bjx.cumt.hyipsdk.KNNAlgo.MPoint;
import com.bjx.cumt.hyipsdk.KNNAlgo.ReferPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hc on 2018/6/10.
 */

public class wifiLocThread extends Thread {
    WifiManager Mwifimanager;
    private ArrayList<String> wifiMAC=new ArrayList<String>();
    WifilocationListener wifilocationListener;
    private final static String TAG="wifiLocThread";
    private List<ReferPoint> mWifiRadioMapList =new ArrayList<ReferPoint>();//对象并初始化
    private LinkedHashMap<String,Double> macWifiTempMap =new LinkedHashMap<String,Double>();
    private LinkedHashMap<String,Double> scan_wifi_Map=new LinkedHashMap<String,Double>();
    private  String RFPath,MacPath;
    String wifiScanRssiResult="",wifiRssiResult="",MwifiScanRssiResult="";
    KNN wifiKnn=new KNN();
    MPoint tempPoint=new MPoint();
    MPoint pointPlane=new MPoint(0,0);
    MPoint lastBlePoint=new MPoint(0,0);
    private ReferPoint testPoint_wifi=new ReferPoint();//一定要初始化
    private boolean wifiLocThreadIsRun;
    public wifiLocThread(WifiManager wifiManager,String path1 ,String path2){
        this.Mwifimanager=wifiManager;
        wifiLocThreadIsRun=true;
        this.RFPath=path1;
        this.MacPath=path2;
        init();
    }
    //wifi指纹库读取
    private void init(){
        mWifiRadioMapList=readRMList(RFPath);
        macWifiTempMap=readMacTempMap(MacPath);
        scan_wifi_Map=(LinkedHashMap<String, Double>) macWifiTempMap.clone();
        //Log.i(TAG,"面板数据"+scan_wifi_Map);
    }

    @Override
    public void run() {
        super.run();
        while (Mwifimanager.isWifiEnabled() & wifiLocThreadIsRun){
            myScanning();
            //遍历得到rssi字符串 scan_iBeacon_Map
            Iterator<Map.Entry<String,Double>> iterator_scan_ble =scan_wifi_Map.entrySet().iterator();
            while(iterator_scan_ble.hasNext()){
                Map.Entry entry_ble = iterator_scan_ble.next();
                String rssi_wifi=Double.toString((double)entry_ble.getValue());
                //放到字符串里
                wifiScanRssiResult=wifiScanRssiResult+","+rssi_wifi;
            }
            //删除第一个字符
            Log.i(TAG,"扫描数据"+wifiRssiResult);
            wifiRssiResult=wifiScanRssiResult.substring(1);
            //根据RSSI字符串确定wifi定位权重
            double wifiweight=LocationWeight.psweight(wifiRssiResult,1);
            //Log.i(TAG,"wifi定位权重"+wifiweight+"\n");
            //置空字符串
            wifiScanRssiResult="";
            //Log.i(TAG,"扫描数据"+wifiRssiResult.toString()+"\n");
           //Log.i(TAG,"wifilIST"+wifiMAC.toString()+"\n");
            //置空map与扫描的mac序列
            scan_wifi_Map= (LinkedHashMap<String, Double>) macWifiTempMap.clone();
            //Log.i(TAG,"清空LIST"+wifiMAC.toString()+"\n");
            //遍历macBLETempMap
            //Log.i(TAG,"遍历模版"+MgetWifiScanRssiResult(macWifiTempMap).substring(1)+"\n");
            String[] scanBleRssiResultArrays = wifiRssiResult.split(",");
            //Log.i(TAG,"得到的wifi rssi序列为"+wifiRssiResult);
            int blesize = scanBleRssiResultArrays.length;
            double[][] scanBleRssiMat=new double[blesize][1];

            for (int i = 0; i< blesize; i++){
                scanBleRssiMat[i][0]=Double.parseDouble(scanBleRssiResultArrays[i]);
//                    Log.i(TAG,"接收扫面线程返回的数据"+size+"的RSSI为"+scanRssiMat[i][0]+"\n");
            }
            testPoint_wifi.addSignalAttr(scanBleRssiMat);
            if(mWifiRadioMapList !=null) {
//                    Log.i(TAG, "wifi和蓝牙的指纹库列表不为空");
                MPoint interestPoint_BLE = wifiKnn.getInterestPoint(mWifiRadioMapList, testPoint_wifi);
                if (interestPoint_BLE != null ) {
                    // bjx 2018-03-20记录，应当是在本地坐标系下计算pdr融合，然后再进行坐标转换
//                        mTransCoord=new MTransCoord(interestPoint_BLE);
//                        mBlePoint=new MPoint(interestPoint_BLE.x,interestPoint_BLE.y);
                    tempPoint=new MPoint(interestPoint_BLE.x,interestPoint_BLE.y);
                    double dist=calDist(tempPoint,lastBlePoint);
                    double delX=0d;
                    double delY=0d;
                    if (dist>5){
                        Log.i(TAG,"wifi定位坐标两次距离超过5m");
                        delX=0.5;
                        delY=0.5;
                    }
                    pointPlane.set(interestPoint_BLE.x+delX,interestPoint_BLE.y+delY);
//                        pointPlane.setX(interestPoint_BLE.x+delX);
//                        pointPlane.setY(interestPoint_BLE.y+delY);
                    lastBlePoint=pointPlane;
                    //Log.i(TAG,"输出wifi定位的坐标为 X:"+pointPlane.x+", Y:"+pointPlane.y);
                    wifilocationListener.onLocation(pointPlane.x,pointPlane.y,wifiweight);
                    //wifilocationListener.onWifimac(wifiMAC);
                    //Log.i(TAG,"wifi的mac1"+wifiMAC);
                    wifiMAC.clear();
                    //置空rssiResult字符串
                    wifiRssiResult ="";
                }
            }
            try{
                Thread.sleep(1500);
            }catch (InterruptedException e){
                Log.i(TAG,"WIFi定位线程中断");
            }
        }
    }
    private List<ReferPoint> readRMList(String fileString){
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
                referPoint = new ReferPoint(Long.parseLong(arrayStrings[0]));
                //需要加z方向
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

    /** 读取蓝牙的模板 @param pathStr @return */
    private LinkedHashMap<String,Double> readMacTempMap(String pathStr){
        LinkedHashMap<String,Double> Mac_Temp_Map=new LinkedHashMap<>();//按插入顺序遍历
        String readLineString="";
        File mac_temp_file=new File(pathStr);
        try {
            FileReader frin = new FileReader(mac_temp_file);
            BufferedReader brin = new BufferedReader(frin);
            while((readLineString = brin.readLine()) != null){
                String mac = readLineString.trim();
                Mac_Temp_Map.put(mac,-95.0);
            }
            brin.close();
            frin.close();
            Log.i(TAG,"写入到LinkedHashMap中");
        }catch (FileNotFoundException e){
            e.printStackTrace();
//            Log.i(TAG, "指纹库中参考点为非数值格式");
        }catch (IOException e){
//            Log.i(TAG,"readline和文件流或bufferedReader或BufferedWriter关闭有IOException");
        }

        return Mac_Temp_Map;
    }
    //wifi扫描函数
    private void myScanning() {
        //开始扫描
        Mwifimanager.startScan();
        //获得扫描结果
        List<ScanResult> list = Mwifimanager.getScanResults();
        for (int j = 0; j < list.size(); j++) {
            String apname = list.get(j).SSID;
            String apmac = list.get(j).BSSID;
            wifiMAC.add(apmac);
            int aplevel = list.get(j).level;
            //Log.i(TAG,apmac+aplevel);
            if (scan_wifi_Map.containsKey(apmac)) {
                scan_wifi_Map.put(apmac, (double) aplevel);
            }
        }
    }
    /**  计算两个点间的距离 */
    private double calDist(MPoint p1,MPoint p2){
        double dist=0d;
        double deltaX=p1.x-p2.x;
        double deltaY=p1.y-p2.y;
        dist=Math.sqrt(Math.pow(deltaX,2)+Math.pow(deltaY,2));
        return dist;
    }
    //监听器
    public void setWifilocationListener(WifilocationListener locationListener){
        this.wifilocationListener=locationListener;
    }
    //map遍历函数
    private String MgetWifiScanRssiResult(LinkedHashMap<String,Double> map){
        MwifiScanRssiResult="";
        Iterator<Map.Entry<String,Double>> iterator_scan_ble =map.entrySet().iterator();
        while(iterator_scan_ble.hasNext()){
            Map.Entry entry_ble = iterator_scan_ble.next();
            String rssi_wifi=Double.toString((double)entry_ble.getValue());
            //放到字符串里
            MwifiScanRssiResult=MwifiScanRssiResult+","+rssi_wifi;
        }
        return MwifiScanRssiResult;
    }
}
