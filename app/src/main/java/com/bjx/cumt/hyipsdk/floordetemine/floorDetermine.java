package com.bjx.cumt.hyipsdk.floordetemine;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class floorDetermine {
    private String Tag="floorDetermine";
    private  String floorResult;//楼层结果
    private LinkedList<floorFingerprint> pReadList = new LinkedList<floorFingerprint>();//楼层指纹库
    private List<String> testList;//测试Mac地址序列
    public floorDetermine(){//构造函数
    }
    public floorDetermine(LinkedList<floorFingerprint> pReadList,ArrayList<String> testList){//构造函数1
        this.pReadList = pReadList;
        this.testList =  new CopyOnWriteArrayList<String>(testList);//测试指纹序列
        deter();
    }
    public String getFloorResult(){//返回楼层结果
        return floorResult;
    }
    public void deter(){//判别楼层
        int t_id_cnt_dis = testList.size();
        String temp = testList.get(0);
        int temp_floor = 0;
        int result_floor = 0;
        int floor_1_temp = 0;
        int floor_2_temp = 0;
        int floor_3_temp = 0;
        int floor_4_temp = 0;
        int floor_5_temp = 0;
        Log.i(Tag,"楼层判定所用mac"+testList.toString());
        for (int i = 0; i < testList.size(); i++) {
            Log.i(Tag,"mac"+testList.get(i));
                for (int j = 0; j < pReadList.size(); j++) {
                    if(testList.size()>0) {
                    String mac=testList.get(i);
                    if (mac.equals(pReadList.get(j).getMac())) {
                        if (pReadList.get(j).getFloor().equals("1")) {
                            floor_1_temp = floor_1_temp + 1;
                        } else if (pReadList.get(j).getFloor().equals("2")) {
                            floor_2_temp = floor_2_temp + 1;
                        } else if (pReadList.get(j).getFloor().equals("3")) {
                            floor_3_temp = floor_3_temp + 1;
                        } else if (pReadList.get(j).getFloor().equals("4")) {
                            floor_4_temp = floor_4_temp + 1;
                        } else if (pReadList.get(j).getFloor().equals("5")) {
                            floor_5_temp = floor_5_temp + 1;
                        }
                    }
                }else{
                        Log.i(Tag,"测试所用AP列表为空");
                    }
            }
        }
        if (floor_1_temp > temp_floor) {
            temp_floor = floor_1_temp;
            result_floor = 1;
        }
        if (floor_2_temp > temp_floor) {
            temp_floor = floor_2_temp;
            result_floor = 2;
        }
        if (floor_3_temp > temp_floor) {
            temp_floor = floor_3_temp;
            result_floor = 3;
        }
        if (floor_4_temp > temp_floor) {
            temp_floor = floor_4_temp;
            result_floor = 4;
        }
        if (floor_5_temp > temp_floor) {
            temp_floor = floor_5_temp;
            result_floor = 5;
        }
        floorResult=Integer.toString(result_floor);
    }

}
