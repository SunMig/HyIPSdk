package com.bjx.cumt.hyipsdk.hyipsdk;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bjx.cumt.hyipsdk.FusionFilter.HybLocationListener;
import com.bjx.cumt.hyipsdk.FusionFilter.HybOrienListener;
import com.bjx.cumt.hyipsdk.FusionFilter.MHybLocHandler;
import com.bjx.cumt.hyipsdk.R;
import com.bjx.cumt.hyipsdk.geomagneticLoc.GeoMagneticLocListener;
import com.bjx.cumt.hyipsdk.util.LocalPath;
import com.bjx.cumt.hyipsdk.util.LogcatHelper;
import com.bjx.cumt.hyipsdk.util.MyConstants;

public class MainActivity extends Activity {

    private static String TAG="MainActivity";
    private ImageButton btnLocBtn,zoomInBtn,zoomOutBtn;
    private TextView showTv;
    private SensorManager mSensorManager;
    private BluetoothManager bluetoothManager;
    private WifiManager wifiManager;
    private BluetoothAdapter mBluetoothAdapter;
    private Drawable markerLoc;
    private MHybLocHandler mHybLocHandler;
    private Bitmap zoomLocBitmap;//定位的放缩Bitmap
    private float orien;
    private long updateLocId=0;
    private String[] needed_permission;
    //geomLocation mgeomLocation=new geomLocation();
    private double mx,my; //定义坐标
    private String RPpath= LocalPath.RadioMapPath;
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化view
        initView();
        //动态授权
        requestApplicationPermission();
//        //开启log存储
        LogcatHelper.getInstance(this).start();
        wifiManager=(WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mSensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //获取BluetoothManager
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //开启wifi
        if(!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
            Log.i(TAG,"wifi打开");
        }
        //获取BluetoothAdapter
        if (bluetoothManager != null) {
            mBluetoothAdapter=bluetoothManager.getAdapter(); //获取适配器
            if (mBluetoothAdapter!=null){
                if (!mBluetoothAdapter.isEnabled()){
                    //调用enable()方法直接打开蓝牙
                    //该方法也可以打开蓝牙，但是会有一个很丑的弹窗，可以自行尝试一下
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, MyConstants.REQUEST_ENABLE_BT);
//                    mBluetoothAdapter.enable();
                }else {
                    //在这里调用混合定位的对象
                    mHybLocHandler=new MHybLocHandler(MainActivity.this,mSensorManager,mBluetoothAdapter,wifiManager,RPpath);
                    //添加位置监听的接口,该接口在后面的代码中有定义
                    mHybLocHandler.setHybLocListener(new MyHybLocListener());
                    //添加角度监听接口
                    mHybLocHandler.setHybOrienListener(new MyHybOrienListener());
                    mHybLocHandler.start();
                    Log.i(TAG,"定位请求开始111111");
                }
            }
        }
//        mgeomLocation.SetLocListener(new magLocListener());
//        mgeomLocation.start();
    }
    //权限的申请
    private void requestApplicationPermission() {
         needed_permission = new String[]{
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.READ_LOGS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        boolean permission_ok = true;
        for (String permission : needed_permission) {
            if (ContextCompat.checkSelfPermission(this,
                    permission) != PackageManager.PERMISSION_GRANTED) {
                permission_ok = false;
//                mTextView.append(String.valueOf(permission_ok)+"\n");
            }
        }
            if (!permission_ok) {
            ActivityCompat.requestPermissions(this, needed_permission, 1);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    //生成混合定位对象
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==MyConstants.REQUEST_ENABLE_BT){
            if (resultCode==Activity.RESULT_OK){
                Log.i(TAG,"混合定位在蓝牙活动回调中开启");
                //混合定位开始
                mHybLocHandler=new MHybLocHandler(MainActivity.this,mSensorManager,mBluetoothAdapter,wifiManager,RPpath);
                //添加位置监听的接口
                mHybLocHandler.setHybLocListener(new MyHybLocListener());
                //添加角度监听接口
                mHybLocHandler.setHybOrienListener(new MyHybOrienListener());
                mHybLocHandler.start();
            }
        }
    }
    /** 实现混合定位的位置接口 */
    public class MyHybLocListener implements HybLocationListener {
        @Override
        public void onLocation(double x, double y, String floor) {
            Log.i(TAG,"输出的坐标为："+x+" ,"+y);
            //坐标转换
            double[] TransCoord=CoordTransform(x,y);
            //在这里更新位置
            //存储的是平面坐标系下的米,现在还原，主要是用CAD——》高斯投影的转换参数
            //showTv.setText("楼层为："+floor+"\n"+"坐标为"+TransCoord[0]+" ,"+TransCoord[1]);
            showTv.setText("平面坐标为："+mx+" ,"+my);
            updateLocId++;
        }
    }

    public class MyHybOrienListener implements HybOrienListener{
        @Override
        public void onOrien(float theta) {
            //在这里更新角度
            if(theta<0){
                orien=theta+180;
            }else{
                orien=theta;
            }
        }
    }

    //初始化android系统的控件TextView
    private void initView(){
        showTv=(TextView)findViewById(R.id.showText);
    }
    //绘图，绘制的是背景图
    private Bitmap zoomBitmap(Drawable drawable, int w, int h){
        //首先将drawable转为bitmap,Bitmap是位图文件
        int width=drawable.getIntrinsicWidth();
        int height=drawable.getIntrinsicHeight();
        /*
           Bitmap.Config是Bitmap中的一个内部类,在Bitmap类里createBitmap(intwidth, int height, Bitmap.Config config)方法里会用到
           ALPHA_8 代表8位Alpha位图,ARGB_4444 代表16位ARGB位图,ARGB_8888 代表32位ARGB位图,RGB_565 代表8位RGB位图,位图数越高存储的影像越好
        */
        Bitmap.Config config=drawable.getOpacity()!= PixelFormat.OPAQUE?Bitmap.Config.ARGB_8888:Bitmap.Config.RGB_565;
        Bitmap createNewBitmap=Bitmap.createBitmap(width,height,config);
        /*
        * 使用Canvas类来显示和旋转位图
        * */
        Canvas canvas=new Canvas(createNewBitmap);
        drawable.setBounds(0,0,width,height);
        drawable.draw(canvas);
        Matrix matrix=new Matrix();
        float scaleWidth=((float)w/width);
        float scaleHeight=((float)h/height);
        matrix.postScale(scaleWidth,scaleHeight);
        Bitmap zoomNewBitmap=Bitmap.createBitmap(createNewBitmap,0,0,width,height,matrix,true);
        return zoomNewBitmap;
    }

    //坐标转换,指纹库坐标不是空间直角坐标的情况下使用该函数（331暂时不用）
    private double[] CoordTransform(double x,double y){
    /*  double dx=13039600.193882;
        double dy=4057283.780394;
        double RotateAngle=0.0007317719;
        double K=0.997230475269; */
    double dx=13039612.224673,dy=4057296.971899;
    double RotateAngle=0.0004245568,K=0.982940816107;
    double[] LocationCoord=new double[2];
    //x=DX+875.5732*cos(T)*K- 888.8734*sin(T)*K;
    LocationCoord[0]=dx+K*Math.cos(RotateAngle)*x-K*Math.sin(RotateAngle)*y;
    LocationCoord[1]=dy+K*Math.sin(RotateAngle)*x+K*Math.cos(RotateAngle)*y;
    //Log.i(TAG,Double.toString(LocationCoord[0])+","+Double.toString(LocationCoord[1]));
    return LocationCoord;
}
    @Override
    protected void onResume() {
        super.onResume();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHybLocHandler.stop();
    }

    //地磁定位
    private class magLocListener implements GeoMagneticLocListener{

        @Override
        public void onLocation(double x, double y) {
           // showTv.setText("坐标为"+x+" ,"+y);
            Log.i(TAG,"我的地磁定位坐标为"+x+" ,"+y);
            mx=x;my=y;
        }
    }
}
