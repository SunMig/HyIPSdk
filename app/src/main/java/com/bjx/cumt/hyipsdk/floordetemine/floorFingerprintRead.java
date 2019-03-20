package com.bjx.cumt.hyipsdk.floordetemine;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

/*
指纹库读取类型
 */
public class floorFingerprintRead {
    /**
     * 函数：Read读取楼层指纹库
     * @param fileNames 路径
     * @return LinkedList<floorFingerprint> pReadList 楼层指纹库list
     */
     public LinkedList<floorFingerprint> Read(String fileNames){
         LinkedList<floorFingerprint> pReadList = new LinkedList<floorFingerprint>();
         String readline = "";
         File file = new File(fileNames);
         try {
             InputStream fName=new FileInputStream(file);
             InputStreamReader inputStreamReader1=new InputStreamReader(fName);
             BufferedReader bName=new BufferedReader(inputStreamReader1);
             while((readline =bName.readLine())!=null){
                 String [] pLine = readline.split(",");
                 pReadList.add(new floorFingerprint(pLine[0],pLine[1]));
             }
             fName.close();
             inputStreamReader1.close();
             bName.close();
         }
         catch (Exception e) {
             e.printStackTrace();
         }
         return pReadList;
     }
}
