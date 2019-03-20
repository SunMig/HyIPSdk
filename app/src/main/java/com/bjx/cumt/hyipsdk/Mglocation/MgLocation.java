package com.bjx.cumt.hyipsdk.Mglocation;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.Tag;
import android.util.Log;
import com.bjx.cumt.hyipsdk.KNNAlgo.MPoint;
import com.bjx.cumt.hyipsdk.hyipsdk.MainActivity;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import static android.content.ContentValues.TAG;
import static com.bjx.cumt.hyipsdk.util.LocalPath.Mgfpdatabase;
import static com.bjx.cumt.hyipsdk.util.LocalPath.Mgfpdatabase;

/**
 * Created by Lenovo on 2018/6/30.
 */

public class MgLocation extends Thread implements SensorEventListener{
    private String TAg="MgLocation";
    private MatchPoint mp,mp1;
    double m,n,t,y;
    private PointFinger pf1,pf2,pf3;
    private Context mcontext;
    private SensorManager msensorManager;
    private LocationListener LL;
    private Sensor sensor;
    private PointFinger pfs1,pfs2,pfs3;
    public static final int w=20;
    List<PointFinger> PF1=new ArrayList<PointFinger>();
    List<PointFinger> PF2=new ArrayList<PointFinger>();
    List<PointFinger> PF3=new ArrayList<PointFinger>();
    private static List<MatchPoint> MP=new ArrayList<MatchPoint>();
    double[] arr1,arr2,arr3,arry;
    double[] arrmp,arrmps;
    double[] arr1_c,arr2_c,arr3_c;
    double[] newArr,juli,copyjuli;
    double[] arrycopy;
    double xx,yy;
    double min;
    float values[];
    int num1,count1=0;

    //构造方法
    public MgLocation(){

    }
    public  MgLocation(Context context,SensorManager sensorManager){
        this.mcontext=context;
        this.msensorManager=sensorManager;
        ReadFile(Mgfpdatabase);
        initList(PF1,PF2,PF3);
        initial();
        initSensor();
    }

    @Override
    public void run() {
        super.run();
        while (true){
            if(this.MP.size()==0){
                Log.i(TAg,"MP为空,无法定位");
            }else {
                Log.i(TAg,"MP不为空,开启定位");
                MatchingMethod(this.MP);
            }
            Log.i(TAg,"MP:"+this.MP.size());
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
   }
    }

    private  void initSensor(){
        sensor=msensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        msensorManager.registerListener(this,sensor,CollectTime.COLLECT_NORMAL);
    }

    //解算方法
    public  void MatchingMethod(List<MatchPoint> list){
            slideWindow(list);
            Log.i(TAg,"匹配定位函数执行");
            double[] dist = new double[3];
            double[] distcopy = new double[3];
            //截取与MP长度相同的数据匹配路径
            arr1_c = Arrays.copyOfRange(arr1, 0, num1);
            dist[0] = dtw(arr1_c, arrmp); //arr1_c是标本数组，arrmp是匹配的数组
            arr2_c = Arrays.copyOfRange(arr2, 0, num1);
            dist[1] = dtw(arr2_c, arrmp);
            arr3_c = Arrays.copyOfRange(arr3, 0, num1);
            dist[2] = dtw(arr3_c, arrmp);
            //找到dist矩阵的最小值对应的路线就是目前所处的路线
            distcopy = dist.clone();
            Arrays.sort(distcopy);
            double s = distcopy[0];
            if (s == dist[0]) {
                newArr = Arrays.copyOfRange(arr1, 0, arr1.length);
                xx = 0.3;
            } else if (s == dist[1]) {
                newArr = Arrays.copyOfRange(arr2, 0, arr2.length);
                xx = 1.2;
            } else {
                newArr = Arrays.copyOfRange(arr3, 0, arr3.length);
                xx = 2.1;
            }
            //滑动窗口得到各个窗口的数组与匹配的数组的DTW距离并放入juli数组，仍然用DTW匹配
            juli = new double[newArr.length - w];
            for (int i = 0; i + w < newArr.length; i++) {
                double[] ArrWindow = Arrays.copyOfRange(newArr, i, i + w); //截取滑动窗口的数组
                double d = dtw(ArrWindow, arrmps);
                juli[i] = d;
            }
            //需要找到最小距离的窗口的下标开始的地方
            min = juli[0];
            for (int i = 0; i < juli.length; i++) {
                if (min > juli[i]) {
                    min = juli[i];
                    count1 = i;
                }
            }
            Log.d(" count1= ", " " + count1);
            //找到i之后，复制arry数组从i到i+2w-1的数组值
            arrycopy = Arrays.copyOfRange(arry, count1, count1 + 2 * w - 1);
            //把复制的数组元素拿出求均值
            double sum = 0;  //不能声明为成员变量，每次调用匹配方法时sum都是一个独立的量
            for (int i = 0; i < arrycopy.length; i++) {
                sum = sum + arrycopy[i];
            }
            //得到YY的值
            yy = sum / arrycopy.length;
            Log.i(TAg,"匹配定位函数执行");
            LL.Sprint(xx,yy);
    }


    //截取滑动窗口
    public void slideWindow(List<MatchPoint> mylist){
        num1 = mylist.size();
        Log.d(" mylist的大小：", " " + num1);
        arrmp = new double[num1];
        for (int i = 0; i < mylist.size() - 1; i++) {
                mp1 = mylist.get(i);
                arrmp[i] = mp1.B;
            }
        //此数组用于滑动窗口匹配
        if(arrmp.length>20) {
            arrmps = Arrays.copyOfRange(arrmp, num1 - 20, num1);
        }
    }


    //集合元素的初始化
    public void initList(List PF1,List PF2,List PF3){
        this.PF1=PF1;
        this.PF2=PF2;
        this.PF3=PF3;
    }

    //初始化
    public void initial(){
        int num=PF1.size();
        arr1=new double[num];
        arr2=new double[num];
        arr3=new double[num];
        arry=new double[num];
        //集合元素取出放进数组
        for(int i=0;i<PF1.size()-1;i++){
            pfs1=PF1.get(i);
            pfs2=PF2.get(i);
            pfs3=PF3.get(i);
            arr1[i]=pfs1.B;
            arr2[i]=pfs2.B;
            arr3[i]=pfs3.B;
            arry[i]=pfs1.y;
        }
        Log.d(" arr1的第一个数是："," "+arr1[0]);
    }


    //dtw算法过程
    public double dtw(double a[],double b[]){
        int num2=b.length;
        double[][] SumDistance=new double[num2][num2];
        double[][] dtw=new double[num2][num2];
        //先初始化累积矩阵SumDistance
        for(int i=0;i<num2;i++){
            for(int j=0;j<num2;j++){
                SumDistance[i][j]=Math.sqrt((b[j]-a[i])*(b[j]-a[i]));
            }
        }

        //初始化dtw矩阵，即损耗矩阵
        for(int i=0;i<num2;i++){
            for(int j=0;j<num2;j++){
                //临界条件判断
                if(i>0&&j>0){
                    dtw[i][j]=minDist(dtw[i][j-1]+SumDistance[i][j],dtw[i-1][j]+SumDistance[i][j],dtw[i-1][j-1]+2*SumDistance[i][j]);
                }
                else if(i==0&&j>0){
                    dtw[i][j] = dtw[i][j-1]+SumDistance[i][j];
                }
                else if(i>0&&j==0){
                    dtw[i][j]= dtw[i-1][j]+SumDistance[i][j];
                }else{
                    dtw[i][j]=0;
                }
            }
        }
        //返回dtw矩阵的最后一行最后一列的值
        return dtw[num2-1][num2-1];
    }

    private double minDist(double dist1,double dist2,double dist3){
        return(dist1<dist2?(dist2<dist3?dist3:(dist1>dist3?dist3:dist1)):(dist2>dist3?dist3:dist2));
    }


    //监听器绑定
    public void setListener(LocationListener locationListener){
        this.LL=locationListener;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
//            values = sensorEvent.values.clone();
//            double Bx = values[0];
//            double By = values[1];
//            double Bz = values[2];
//            double B = Math.sqrt(Bx * Bx + By * By + Bz * Bz);
//            mp = new MatchPoint(B);
//            MP.add(mp);
//            Log.d(TAg, "MP的尺寸 " + MP.size());
//            if(MP.size()>30){
//                Log.d(TAg, "是否执行代码");
//                CopyOnWriteArrayList mmylist=new CopyOnWriteArrayList<>(MP);
//                Log.d(TAg, "mylist的尺寸 " + mmylist.size());
//                MatchingMethod(mmylist);
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//           }
    }
    //传输文件
    public void setMatchpoint(List<MatchPoint> mp){
        this.MP=new CopyOnWriteArrayList<>(mp);
        //Log.i(TAg,"参数传递MP:"+MP.size());
    }
    //读取文件
    public void ReadFile(String string) {
        try {
            //InputStream is = getResources().openRawResource(R.raw.aa);
            //String string=Environment.getExternalStorageDirectory()+File.separator+"aa.txt";
            FileInputStream fis=new FileInputStream(string);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String str = "";
            //一行一行的读取
            while ((str = br.readLine()) != null) {
                //字符串空格分割
                String[] data = str.split(",");
                m = Double.valueOf(data[0]);
                n = Double.valueOf(data[1]);
                t = Double.valueOf(data[2]);
                y = Double.valueOf(data[3]);
                //初始化集合
                pf1 = new PointFinger(m, y);
                PF1.add(pf1);
                pf2 = new PointFinger(n, y);
                PF2.add(pf2);
                pf3 = new PointFinger(t, y);
                PF3.add(pf3);
                //Toast.makeText(this, "OK",Toast.LENGTH_SHORT).show();
                //Log.d("PF1的大小", " " + PF1.size());

            }
            fis.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}