package com.bjx.cumt.hyipsdk.floordetemine;
/*
指纹库数据类
 */
public class floorFingerprint {
    private  String floor;//楼层
    private  String Mac;//mac地址
    public floorFingerprint(){//构造函数1

    }
    public floorFingerprint(String floor,String Mac){//构造函数2
        this.floor=floor;
        this.Mac=Mac;
    }
    public String getFloor(){
        return floor;
    }
    public String getMac(){
        return Mac;
    }
}
