package com.bjx.cumt.hyipsdk.floordetemine;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.List;

public class testGet {
    private List<ScanResult> list;
    public List<String> testlist = new ArrayList<String>();
    public testGet(List<ScanResult> list){
        this.list=list;

    }
    public testGet(){
    }
    public List<String> testMap(){
        String pMac="";

        List<String> ptestlist = new ArrayList<String>();
        for(int i = 0;i<list.size();i++){
                pMac=list.get(i).BSSID;
                ptestlist.add(pMac);
        }
        return ptestlist;
    }
}
