package com.bjx.cumt.hyipsdk.util;

import android.os.Environment;

import java.io.File;

/**
 * Created by cumt_bjx on 2018/3/15.
 */

public class LocalPath {
    public static String SystemDirPath= Environment.getExternalStorageDirectory().toString();
    public static String IPSFolderPath=SystemDirPath+ File.separator+"CetcTest";
    public static String TiledLayerPath=SystemDirPath+File.separator+"fengmap";
    public static String RadioMapPath=IPSFolderPath+File.separator+"RadioMap";
    public static String logPath=IPSFolderPath+File.separator+"log_bjx";
    public static String fnmapFolderPath=TiledLayerPath+File.separator+"map";
    public static String fnThemeFolderPath=TiledLayerPath+File.separator+"theme";
    public static String cetcFnMapFilePath=fnmapFolderPath+File.separator+"fourfloor"+File.separator+"fourfloor.fmap";
    public static String cumtcFnMapFilePath=fnmapFolderPath+File.separator+"1563203010390360"+File.separator+"1563203010390360.fmap";
    public static String cetcFnMapThemeFilePath=fnThemeFolderPath+File.separator+"fourfloor"+File.separator+"fourfloor.theme";
    public static String cumtFnMapThemeFilePath=fnThemeFolderPath+File.separator+"1563203010390360"+File.separator+"1563203010390360"+File.separator+"1563203010390360.theme";
    public static String tpkfilePath=TiledLayerPath+File.separator+"cumt_hcxy_f4.tpk";
    public static String test=TiledLayerPath+File.separator+"test.tpk";
    public static String bleRMFilePath =RadioMapPath+File.separator+"BLE_RM.txt";
    public static String wifiRMFilePath=RadioMapPath+File.separator+"Wifi_RM.txt";
    public static String bleMacTempPath=RadioMapPath+File.separator+"ble_mac.txt";
    public static String wifiMacTempPath=RadioMapPath+File.separator+"wifi_mac.txt";
    public static String floordetermine=RadioMapPath+File.separator+"floordecide.txt";
    //public static String Mgfpdatabase=RadioMapPath+File.separator+"Mgrpdatabase.txt";
    //public static String Mgfpdatabase=RadioMapPath+File.separator+"geomagneticfp.txt";
    public static String Mgfpdatabase=RadioMapPath+File.separator+"magnetic.txt";
}
