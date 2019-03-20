package com.bjx.cumt.hyipsdk.FusionFilter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.bjx.cumt.hyipsdk.KNNAlgo.KNN;
import com.bjx.cumt.hyipsdk.KNNAlgo.MPoint;
import com.bjx.cumt.hyipsdk.KNNAlgo.ReferPoint;
import com.bjx.cumt.hyipsdk.Mglocation.MatchPoint;
import com.bjx.cumt.hyipsdk.Mglocation.MgLocation;
import com.bjx.cumt.hyipsdk.MotionRecg.StepDectFsm;
import com.bjx.cumt.hyipsdk.floordetemine.floorInterface;
import com.bjx.cumt.hyipsdk.floordetemine.floorLis;
import com.bjx.cumt.hyipsdk.geomagneticLoc.GeoMagneticLocListener;
import com.bjx.cumt.hyipsdk.geomagneticLoc.geomLocation;
import com.bjx.cumt.hyipsdk.jama.Matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by dell on 2018/3/29.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MHybLocHandler {
    private final static String TAG="MHybLocHandler";
    private static String mwifiRPpath,mbleRPpath,magRPpath,mwifimacpath,mblemacpath;
    private String Path;
    private MgLocation mLocation=new MgLocation();
    private WifiManager Mwifimanager;
    private HybLocationListener hybLocationListener; //位置接口
    private HybOrienListener hybOrienListener;    //方向接口
    private HybmacListener hybmacListener;
    private String floor="1";
    private int count=1;
    private WifilocationListener Mwifilistener; //wifi定位的接口
    private Context mContext;
    private SensorManager mSensorManager;
    private Sensor mAccSensor,mRotateSensor,Magnetometer;
    private KNN knn;
    private ArrayList<String> BLEMAC=new ArrayList<String>();
    private ArrayList<String> mwifimac=new ArrayList<String>();
    private List<Double> geoMList=new ArrayList<Double>();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private ScanSettings.Builder scanSettingsBuilder;
    private List<ReferPoint> mBLERadioMapList =new ArrayList<ReferPoint>();//对象并初始化
    private LinkedHashMap<String,Double> macBLETempMap =new LinkedHashMap<String,Double>();
    private LinkedHashMap<String,Double> scan_iBeacon_Map=new LinkedHashMap<String,Double>();
    ArrayList<String> ALLmac=new ArrayList<String>();
    private MBLELocThread mbleLocThread=new MBLELocThread();
    public boolean bleLocThreadIsRun=true;//用于控制蓝牙定位的线程是否继续运行
    private String bleRssiResult ="";
    private String bleScanRssiResult ="";
    private MPoint mBlePoint=new MPoint();
    private ReferPoint testPoint_BLE=new ReferPoint();//一定要初始化
    private float[] mRotationMatrixFromVector=new float[16];
    private float[] mRotationMatrix=new float[16];
    private float[] orientationVals=new float[3];
    float orienValue=0;
    private geomLocation mgeomLocation=new geomLocation();
    floorLis floordetermine=new floorLis();
    private float accVal[]=new float[3]; //存储当前的加速度
    MKalman mKalman;
    int dp=3; // 状态变量数
    int mp=4; //观测变量数
    //滤波过程的各种矩阵的定义
    Matrix state_transition_matrix=new Matrix(dp,dp);
    Matrix measurement_trans_matrix=new Matrix(mp,dp);
    Matrix state_post=new Matrix(dp,1);
    Matrix state_pre=new Matrix(dp,1);
    Matrix process_noise_cov=new Matrix(dp,dp);
    Matrix measurement_noise_cov=new Matrix(mp,mp);
    Matrix measurement_mat=new Matrix(mp,1);
    Matrix error_cov_post=new Matrix(dp,dp);
    //点位的定义
    List<MatchPoint> MP=new ArrayList<MatchPoint>();
    MPoint pointPlane=new MPoint();
    MPoint lastBlePoint=new MPoint(0,0);
    MPoint tempPoint=new MPoint();
    MPoint wifipoint=new MPoint();
    MPoint MgPoint=new MPoint();
    StepDectFsm mStepDectFsm;
    float stepLen=0f;
    float deltaTime=0f;
    double wifiweight=0;
    private int AcquisitionTime=50000;

    //构造方法
    public MHybLocHandler(Context context, SensorManager sensorManager, BluetoothAdapter mBluetoothAdapter,WifiManager wifiManager,String FPath) {
        this.mContext = context;
        mSensorManager = sensorManager;
        this.mBluetoothAdapter = mBluetoothAdapter;
        this.Mwifimanager=wifiManager;
        this.Path=FPath;
        init();
    }
    /** 初始化，包括knn，数据读入，蓝牙，传感器,卡尔曼滤波等等 */
    private void init() {
        knn = new KNN();
        //指纹库路径
        CreatePath();
        //读入BLE文件，两个函数readRMList()和readMacTempMap()函数
        mBLERadioMapList = readRMList(mbleRPpath);
        macBLETempMap = readMacTempMap(mblemacpath);
        scan_iBeacon_Map = (LinkedHashMap<String, Double>) macBLETempMap.clone();
        initBluetooth();
        initSensor();
        //创建卡尔曼滤波对象
        mKalman = new MKalman(dp, mp);
        double[][] process_noise_double = {{1.5, 0, 0}, {0, 1.5, 0}, {0, 0, 0.01}};
        process_noise_cov = new Matrix(process_noise_double);
        mKalman.setProcess_noise_cov(process_noise_cov);//过程噪声协方差阵
        double[][] state_post_double = {{0}, {0}, {0}};
        state_post = new Matrix(state_post_double);
        mKalman.setState_post(state_post);// 状态转移矩阵
        //由于状态和观测为线性的关系，所以将观测矩阵放在这里
        double[][] measurement_trans_double = {{1, 0, 0}, {0, 1, 0}, {0, 0, 0}, {0, 0, 1}};
        measurement_trans_matrix = new Matrix(measurement_trans_double);
        mKalman.setMeasure_trans_matrix(measurement_trans_matrix);//测量转移矩阵
        double[][] measurement_noise_double = {{3.24, 0, 0, 0}, {0, 1.44, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}};
        measurement_noise_cov = new Matrix(measurement_noise_double);
        mKalman.setMeasurement_noise_cov(measurement_noise_cov);//观测噪声协方差矩阵
        double[][] error_p_post_double = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};//P 误差矩阵
        error_cov_post = new Matrix(error_p_post_double);
        mKalman.setError_cov_post(error_cov_post);//预测(先验)的误差协方差矩阵
//        // 误差协方差
//        error_cov_pre=new Matrix(dp,dp); // P' 误差协方差的预测矩阵
        //创建步态探测监听的对象
        mStepDectFsm = new StepDectFsm();
        //wifi定位程序
        wifiLocThread wifi = new wifiLocThread(Mwifimanager,mwifiRPpath,mwifimacpath);
        wifi.start();
        wifi.setWifilocationListener(new mwifiLocListener());

        //地磁定位程序
//        MgLocation Mg=new MgLocation(mContext,mSensorManager);
//        Mg.start();
//        Mg.setListener(new LocationListener() {
//            @Override
//            public void Sprint(double x, double y) {
//                MgPoint.x=x;
//                MgPoint.y=y;
//                Log.i(TAG,"地磁定位坐标："+x+","+y);
//            }
//        });
//        Mfloordetermine mfloordetermine=new Mfloordetermine();
//        mfloordetermine.start();
//        Log.i(TAG,"楼层判定线程启动");
//        mgeomLocation.SetLocListener(new geoMLocListener());
//        mgeomLocation.start();
    }


    /** 读取指纹库点的坐标数据至List<ReferPoint> @param fileString @return referPointAttrList */
    private List<ReferPoint> readRMList(String fileString){
        File radioMapFile=new File(fileString);
        String readLineString="";
        List<ReferPoint> referPointAttrList=new ArrayList<ReferPoint>();
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
            Log.i(TAG, String.valueOf(referPointAttrList.size()));
        }catch (Exception e){
            e.printStackTrace();
            Log.i("Hybrid Location Handler","读取文件至参考点列表存在错误");
        }
        return referPointAttrList;
    }

    /** 读取蓝牙的模板（MAC值） @param pathStr @return */
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

    /** 蓝牙扫描的回调  */
    private ScanCallback mScanCallback=new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device=result.getDevice();
//            Log.i(TAG,"采用onScanResult方法得到的数据为");
            if (device!=null){
                int rssi=result.getRssi();
                String Mac=device.getAddress();
                BLEMAC.add(Mac);
                if (scan_iBeacon_Map.containsKey(Mac)){
                    scan_iBeacon_Map.put(Mac,(double)rssi);
//                    Log.i(TAG,Mac+","+Double.toString(rssi));
                }
            }
        }
        //这种回调方法在使用SCAN_MODE_LOW_LATENCY模式时不会回传数据，但是在默认情况下SCAN_MODE_LOW_POWER，是可以回传数据的
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            //遍历整个列表
            for (int j=0;j<results.size();j++){
                String iBeaconMacAddress=results.get(j).getDevice().getAddress();
                int iBeaconRssiValue=results.get(j).getRssi();
//                Log.i(TAG,iBeaconMacAddress+",   "+iBeaconRssiValue);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    /** 蓝牙定位的线程 */
    public class MBLELocThread extends Thread{
        @Override
        public void run() {
            super.run();
            while (mBluetoothAdapter.isEnabled() & bleLocThreadIsRun){
                //遍历得到rssi字符串 scan_iBeacon_Map
                Iterator<Map.Entry<String,Double>> iterator_scan_ble =scan_iBeacon_Map.entrySet().iterator();
                while(iterator_scan_ble.hasNext()){
                    Map.Entry entry_ble = iterator_scan_ble.next();
                    String rssi_ble=Double.toString((double)entry_ble.getValue());
                    //放到字符串里
                    bleScanRssiResult=bleScanRssiResult+","+rssi_ble;
                }
                //Log.i(TAG,"得到的ble rssi序列为"+bleScanRssiResult);
                //删除第一个字符
                bleRssiResult=bleScanRssiResult.substring(1);
                //得到蓝牙定位权重
                double bleweight=LocationWeight.psweight(bleRssiResult,7);
                //置空字符串
                bleScanRssiResult="";
                //置空map与mac列表
                scan_iBeacon_Map= (LinkedHashMap<String, Double>) macBLETempMap.clone();
                //Log.i(TAG,"扫描数据"+scan_iBeacon_Map.toString()+"\n");
                //遍历macBLETempMap
//                String mapString=MLinkedMapToString(macBLETempMap);
//                Log.i(TAG,"遍历Mac模版数据"+mapString);
                String[] scanBleRssiResultArrays = bleRssiResult.split(",");
                int blesize = scanBleRssiResultArrays.length;
                double[][] scanBleRssiMat=new double[blesize][1];
                for (int i = 0; i< blesize; i++){
                    scanBleRssiMat[i][0]=Double.parseDouble(scanBleRssiResultArrays[i]);
//                    Log.i(TAG,"接收扫面线程返回的数据"+size+"的RSSI为"+scanRssiMat[i][0]+"\n");
                }
                testPoint_BLE.addSignalAttr(scanBleRssiMat);
                if(mBLERadioMapList !=null) {
                    Log.i(TAG, "指纹库的尺寸"+mBLERadioMapList.size());
                    MPoint interestPoint_BLE = knn.getInterestPoint(mBLERadioMapList, testPoint_BLE);
                    if (interestPoint_BLE != null ) {
                        // bjx 2018-03-20记录，应当是在本地坐标系下计算pdr融合，然后再进行坐标转换
//                        mTransCoord=new MTransCoord(interestPoint_BLE);
//                        mBlePoint=new MPoint(interestPoint_BLE.x,interestPoint_BLE.y);
                        tempPoint=new MPoint(interestPoint_BLE.x,interestPoint_BLE.y);
                        double dist=calDist(tempPoint,lastBlePoint);
                        double delX=0d;
                        double delY=0d;
                        if (dist>5){
                            Log.i(TAG,"蓝牙定位坐标两次距离超过5m");
                            delX=0.5;
                            delY=0.5;
                        }
                        //Log.i(TAG,"输出ble定位的坐标为 X:"+interestPoint_BLE.x+", Y:"+interestPoint_BLE.y+"\n");
                        //Log.i(TAG,"输出wifi定位的坐标为 X:"+wifipoint.x+", Y:"+wifipoint.y+"\n");

                        //计算蓝牙，wifi定位的各自的权重（需加地磁定位权重）
                        double wifi_weight=wifiweight/(bleweight+wifiweight);
                        double ble_weight=bleweight/(bleweight+wifiweight);
                        Log.i(TAG,"权重"+wifi_weight+","+ble_weight);
                        //暂时不进行ble wifi的融合
                        //pointPlane.set(ble_weight*interestPoint_BLE.x+delX,ble_weight*interestPoint_BLE.y+delY);
                        pointPlane.set(wifi_weight*wifipoint.x+ble_weight*interestPoint_BLE.x+delX,wifi_weight*wifipoint.y+ble_weight*interestPoint_BLE.y+delY);
//                        pointPlane.setX(interestPoint_BLE.x+delX);
//                        pointPlane.setY(interestPoint_BLE.y+delY);
                        lastBlePoint=pointPlane;
                        Log.i(TAG,"输出wifi+ble定位的坐标为 X:"+pointPlane.x+", Y:"+pointPlane.y);
                        //置空rssiResult字符串
                        bleRssiResult ="";
                    }
                }
                try{
                    Thread.sleep(1500);
                }catch (InterruptedException e){
                    Log.i(TAG,"蓝牙定位线程中断");
                }
            }
        }
    }
   //楼层判断线程
    private class Mfloordetermine extends Thread{
       @Override
       public void run() {
           super.run();
           while (true) {
               //楼层判定
               Set set=new LinkedHashSet<String>();
               set.addAll(new CopyOnWriteArrayList<>(BLEMAC));
               ArrayList<String> blemac = new ArrayList<String>();
               blemac.addAll(set);
               set.clear();
               set.addAll(mwifimac);
               ArrayList<String> wifimac =new  ArrayList<String>();
               wifimac.addAll(set);
               Log.i(TAG, "bLE基站mac：" + blemac);
               if (blemac.size() == 0 && wifimac.size() == 0) {
                   Log.i(TAG, "mac列表为空");
               } else {
                   floordetermine.macListSet2(wifimac, blemac);
                   floordetermine.setListner(new interListence());
                   floordetermine.floorResultcal();
               }
               BLEMAC.clear();
               mwifimac.clear();
               try {
                   Thread.sleep(1500);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
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

    /** 初始化传感器*/
    private void initSensor(){
        mAccSensor=this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//步态步长
        mRotateSensor=this.mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);//方向识别
        Magnetometer=this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);//磁力计
    }

    /** 初始化蓝牙 */
    private void initBluetooth(){

        //开启蓝牙的操作
//        mBluetoothManager=(BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);//传入mContext的好处
//        mBluetoothAdapter = mBluetoothManager.getAdapter();
        bleScanner=mBluetoothAdapter.getBluetoothLeScanner();
        scanSettingsBuilder=new ScanSettings.Builder();
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
    }

    /** 设定混合定位的位置监听@param hybLocListener */
    public void setHybLocListener(HybLocationListener hybLocListener){
        this.hybLocationListener=hybLocListener;
    }
    /** 设定朝向的监听 */
    public void setHybOrienListener(HybOrienListener hybOrienListener){
        this.hybOrienListener=hybOrienListener;
    }
    /** 设定定位mac列表的监听 */
    public void setHymacListener(HybmacListener hybmacListener){
        this.hybmacListener=hybmacListener;
    }

    public SensorEventListener mSensorEventListener=new SensorEventListener(){
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accVal[0]=event.values[0];
                    accVal[1]=event.values[1];
                    accVal[2]=event.values[2];
                    if (mStepDectFsm.StepDect(accVal)){
                        stepLen=mStepDectFsm.getStepLength();
                        Log.i(TAG,"得到的步长是"+stepLen);
                        deltaTime=mStepDectFsm.getDeltaTime();
                        //估算位置
                        estimateCoord();
                    }
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    mSensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector, event.values);
                    mSensorManager.remapCoordinateSystem(mRotationMatrixFromVector,
                            SensorManager.AXIS_X, SensorManager.AXIS_Z,
                            mRotationMatrix);
                    mSensorManager.getOrientation(mRotationMatrix, orientationVals);

                    orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
                    orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
                    orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);
                    orienValue=orientationVals[0]-5.56f;//消除徐州(5.81)磁偏角的影响,石家庄（5.56）
                    //Log.i(TAG,"输出的角度为"+orienValue);
                    //给接口提供角度
                    hybOrienListener.onOrien(orienValue);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    double Bx = event.values[0];
                    double By = event.values[1];
                    double Bz = event.values[2];
                    double B = Math.sqrt(Bx * Bx + By * By + Bz * Bz);
//                    MatchPoint mp = new MatchPoint(B);
//                    int MPsizw=30;
//                    if(MP.size()<MPsizw){
//                        MP.add(mp);
//                    }else{
//                        MP.remove(0);
//                        MP.add(29,mp);
//                    }
//                    List<MatchPoint> M=new CopyOnWriteArrayList<>(MP);
//                    mLocation.setMatchpoint(M);
                    if(geoMList.size()<140){
                        geoMList.add(B);
                    }else{
                        geoMList.remove(0);
                        geoMList.add(139,B);
                        mgeomLocation.Mreceiver(new CopyOnWriteArrayList<>(geoMList));
                    }
                   //Log.i(TAG, String.valueOf(geoMList));
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    /** 估算位置，把估算的结果传送到接口监听处
     * 需在此处继续完善.....（能输出x,y,z三轴的坐标信息）
     * */
    private void estimateCoord(){
        double [][] transition_double={{1,0,stepLen*Math.cos(Math.toRadians(orienValue))},{0,1,-stepLen*Math.sin(Math.toRadians(orienValue))},{0,0,1}}; // 按行进行存储的
        state_transition_matrix=new Matrix(transition_double);
        mKalman.setState_trans_matrix(state_transition_matrix);
        double [][] deltaX={{stepLen*Math.sin(Math.toRadians(orienValue))},{stepLen*Math.cos(Math.toRadians(orienValue))},{0.01}};
        //预测状态
        state_pre=state_post.plus(new Matrix(deltaX));
        mKalman.setState_pre(state_pre);
        //若观测矩阵为动态变化的，需要放在这里，2018-03-29
//        double [][] measurement_trans_double={{1,0,0},{0,1,0},{0,0,0},{0,0,1}};
//        measurement_trans_matrix=new Matrix(measurement_trans_double);
//        mKalman.setMeasure_trans_matrix(measurement_trans_matrix);

        //观测量
        double [][] measurement_double={{pointPlane.x},{pointPlane.y},{stepLen},{orienValue}};
        measurement_mat=new Matrix(measurement_double);
        state_post=mKalman.Fusion(measurement_mat);
        //输出结果至接口
        hybLocationListener.onLocation(state_post.get(0,0),state_post.get(1,0),floor);
        Log.i(TAG,"得到的卡尔曼坐标"+state_post.get(0,0)+","+state_post.get(1,0));
    }
    /** 融合开始 */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void start(){
        if (mBluetoothAdapter.isEnabled()) {
            //启动蓝牙扫描，并设定回调函数
            bleScanner.startScan(null, scanSettingsBuilder.build(), mScanCallback);
            bleLocThreadIsRun = true;
            //然后在这里开启定位计算的线程
            mbleLocThread.start();
            mSensorManager.registerListener(mSensorEventListener,mAccSensor,SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(mSensorEventListener,mRotateSensor,SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(mSensorEventListener,Magnetometer,AcquisitionTime);

//            stepCount=0;
        }else{
            Log.i(TAG,"start里的Bluetoothadapter不可用");
        }
    }
    /**结束 */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stop(){
        mSensorManager.unregisterListener(mSensorEventListener);
        bleLocThreadIsRun=false;
        bleScanner.stopScan(mScanCallback);//取消回调函数
    }
    private class interListence implements floorInterface {
        public void floorPirint(String floorResults){
            floor=floorResults;
            Log.i(TAG,"楼层是："+floorResults);
        }
    }
    //地磁定位结果监听（需再定义x,y,z数据）
    private class geoMLocListener implements GeoMagneticLocListener{
        @Override
        public void onLocation(double x, double y) {
            Log.i(TAG,"地磁定位结果："+x+","+y);
        }
    }
    //wifi定位结果监听(需要加z数据)
    private class mwifiLocListener implements WifilocationListener{
        @Override
        public void onWifimac(ArrayList<String> mac) {

        }
        @Override
        public void onLocation(double x, double y, double weight) {
            wifipoint.x = x;
            wifipoint.y = y;
            wifiweight = weight;
        }
    }
    //指纹库路径生成
    private void CreatePath(){
        mwifiRPpath=Path+File.separator+"Wifi_RM.txt";
        mbleRPpath=Path+File.separator+"BLE_RM.txt";
        magRPpath=Path+File.separator+"magnetic.txt";
        mwifimacpath=Path+File.separator+"wifi_mac.txt";
        mblemacpath=Path+File.separator+"ble_mac.txt";
    }
}
