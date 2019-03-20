package com.bjx.cumt.hyipsdk.floordetemine;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import static com.bjx.cumt.hyipsdk.util.LocalPath.floordetermine;

public class floorLis {
    private String Tag="floorLis";
    private floorInterface floorInter;
    private WifiManager wifiManager;
    private List<ScanResult> list;
    private LinkedList<floorFingerprint> pReadList = new LinkedList<floorFingerprint>();//楼层指纹库
    private List<String> testList = new ArrayList<>();//测试Mac地址序列
    private String floorResult;//楼层结果

    public floorLis(){
        initReadFingerprint();//读取指纹库；
    }
    /**
     * 函数 macListSet1 传入mac地址列表
     * @param testList W，B合并的mac地址列表
     */
    public void macListSet1(List<String> testList){
        this.testList=testList;
    }

    /**
     * 函数 macListSet2 传入mac地址列表
     * @param testListW
     * @param testListB
     * 分别为两种列表
     */
    public void  macListSet2(List<String> testListW,List<String> testListB){
        testListW.addAll(testListB);
        this.testList=testListW;
    }
    public void floorResultcal(){
       // while(true){
            floorDetermine t1 = new floorDetermine(pReadList, (ArrayList<String>) testList);
            floorResult = t1.getFloorResult();
            floorInter.floorPirint(floorResult);
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        //}
    }
    public void setListner(floorInterface pFloorInter){
        this.floorInter=pFloorInter;
    }

    public void initReadFingerprint(){//读取楼层指纹库
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File exSToDir = Environment.getExternalStorageDirectory();
            File folderDir = new File(exSToDir, "Fingerprints");
            if (!folderDir.exists()) {
                folderDir.mkdir();
            }
            floorFingerprintRead fread =new floorFingerprintRead();
            File file =new File(floordetermine);
            if(!file.exists()){
                Log.i(Tag,"楼层判定指纹库不存在");
            }else  {
                pReadList=fread.Read(floordetermine);
            }

        }
    }
}
