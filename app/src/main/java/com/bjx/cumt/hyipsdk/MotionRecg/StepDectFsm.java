package com.bjx.cumt.hyipsdk.MotionRecg;

/**
 * 步态识别，只需传入传感器的数据数组即可
 * Created by bjx on 2018/3/29.
 */

public class StepDectFsm {
    float orienValue=0;
    int winSize=7;
    private float accVal[]=new float[3]; //存储当前的加速度
    float accMag; //加速度模值
    float avgAcc=0; //均值滤波后的当前加速度
    float[] accVector=new float[7]; //窗口存储的数据，winSize=7
    float[] avgAccArr=new float[3]; //向FSM中传递的三个历史数据
    int avgCount=0;
    int accCount=0;
    // fsm算法需要的参数
    float accDiffThreshold=0.047f; //差分阈值
    float accStartThreshold=9.8f; //加速度起始阈值
    float InPeekThreshold=5; // 一个采样周期内，正向加速度变化量能达到的最大值
    float DePeekThreshold=5; // 在一个采样周期内，负向加速度变化量能达到的最大值
    float DeThreshold=4; //干扰阈值
    float PeekThread=11; //波峰阈值
    int win=7;  //均值滤波的滑动窗口为7
    float acc;      //加速度模值
    float aveAcc=0; // 均值滤波后的当前加速度
    //    float accVector[];//滑动窗口存储的数据，7个
    float[] last3AveAcc=new float[3];;//为向FSM中传递的三个历史数据
    int aveCount=0;     //更新三个历史数据
    //    int accCount=0;     //更新滑动窗口中的数据
    float A_diff=0.03f; //差分阈值
    float A_Thr=9.8f;   //起始加速度
    float InThr=5;      //确定上升阈值
    float DeThr=5;      //确定下降阈值
    float D_Thr=4;      //干扰阈值
    float PeakThr=11;   //波峰阈值
    int state=0;        //状态值
    boolean init=false;
    int Pin=0;
    int Pde=0;
    long stepCount=0;
    boolean isNewStep=false;
    long curStepTime=0;
    long lastStepTime=0;
    float deltaTime=0f;
    float stepACoff=0.23f;
    float stepBCoff=0.34f;
    float stepLen;
    /** 结构体*/
    public StepDectFsm(){
        //记录下开始运行的时间
        lastStepTime=System.currentTimeMillis();//毫秒
    }
    public boolean StepDect(float[] accVal){
        boolean isStep=false;
        accMag=(float)Math.sqrt(Math.pow(accVal[0],2)+ Math.pow(accVal[1],2)+Math.pow(accVal[2],2));
        //当滑动窗口技术不满7时,往里存储数据
        //当滑动窗口技术不满7时,往里存储数据
        if (accCount<winSize-1){
            accVector[accCount]=accMag;
            isStep=false;
        }else {
            // 前面存储了6个数，这里加上第7个数
            accVector[winSize - 1] = accMag;
            //对着七个数进行滤波（指数加权移动平均）
            avgAcc = ewmaFilter(accVector);
            if(avgCount<2){
                avgAccArr[avgCount]=avgAcc;
            }else {
                avgAccArr[2]=avgAcc;
                //将avgAccArr放到fsm中去判断是否为1步
                isStep=stepDetecFsm(avgAccArr);
                //删除第一位，并将后面的所有数据前移
                System.arraycopy(avgAccArr,1,avgAccArr,0,2);
            }
            avgCount++;
            // 删除第一位，并将后面所有数据前移
            System.arraycopy(accVector,1,accVector,0,winSize-1);
        }
        accCount++;
        return isStep;
    }

    private boolean stepDetecFsm(float[] avgAccVect){
        isNewStep=false;
        float avgAccPre=avgAccVect[0];  //第一个均值
        float avgAccMid=avgAccVect[1];  //第二个均值，中间的
        float avgAccCur=avgAccVect[2];  //第三个均值，当前的
        int step=0;
        float ajcAccDif=avgAccCur-avgAccMid;

        if (ajcAccDif>accDiffThreshold){
            Pin++;
        }
        if (ajcAccDif<accDiffThreshold){
            Pde++;
        }

        if(state==0){
            if (avgAccCur<accStartThreshold){
                state=1;
                init=true;
            }
        }

        if (state==1){
            if (init){
                Pin=0;
                Pde=0;
                init=false;
            }
            if (Pde>DeThreshold){
                state=0;
            }else if(Pin>InPeekThreshold){
                state=2;
            }
        }

        if (state==2){
            if (avgAccCur<avgAccMid & avgAccMid > avgAccPre ){
                state=3;
            }
        }

        if (state==3){
            if (avgAccMid>PeekThread){
                state=4;
            }else {
                state=2;
            }
        }

        if (state==4) {
            if (Pde > DePeekThreshold) {
                state = 0;
//                step_flag=true;
                isNewStep = true;
                curStepTime=System.currentTimeMillis();//毫秒
                deltaTime=(curStepTime-lastStepTime)/1000;
                lastStepTime=curStepTime;
                stepLen=computeStepLength(stepACoff,stepBCoff,deltaTime);
            }
        }
        return isNewStep;
    }

    /**
     * 返回一步的时间
     * @return
     */
    public float getDeltaTime(){
        return deltaTime;
    }

    /**
     * 指数移动平均  （Exponential Moving Average, EMA或EWMA）是以指数式递减加权的移动平均。
     * 各数值的加权而随时间而指数式递减，越近期的数据加权越重，但较旧的数据也给予一定的加权。
     * @return
     */
    private float ewmaFilter(float[] accVector){
        float avg=0f;

        int len=accVector.length;
        float a=1-2/(len+1);
        for (int j=0;j<len;j++){
            avg=avg+(float)Math.pow(1-a,j)* accVector[len-1-j];
        }
        avg=avg*a;
        return avg;
    }
    /**
     * 计算步长
     * @return
     */
    private float computeStepLength(float stepACoff,float stepBCoff, float deltaTime){
        float stepLen=0f;
        if (isNewStep){
            stepLen=stepACoff+stepBCoff*(1/deltaTime);
        }
        if (stepLen>1.2){
            stepLen=0.65f;
        }
        return stepLen;
    }

    /**
     * 获得步长
     * @return
     */
    public float getStepLength(){
        return stepLen;
    }


}
