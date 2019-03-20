package com.bjx.cumt.hyipsdk.FusionFilter;

import android.util.Log;

import com.bjx.cumt.hyipsdk.jama.Matrix;

/**
 * 本算法为扩展卡尔曼滤波算法  java版本
 * 参考了JKalman -KALMAN FILTER(JAVA)
 * 对JKalman进行了修改，依据状态方程进行一步预测，
 * 然后基于状态转移矩阵、观测方程矩阵和控制矩阵完成最佳状态估计和误差估计
 * 版权归毕京学博士所有
 * Copyright (C) 2018  Jingxue Bi
 * Created by bjx on 2018/3/28.
 * <pre>
 * x<sub>k</sub>=A*x<sub>k-1</sub>+B*u<sub>k</sub>+w<sub>k</sub>
 * z<sub>k</sub>=Hx<sub>k</sub>+v<sub>k</sub>,
 * </pre>
 * <p>where:
 * <pre>
 * x<sub>k</sub> (x<sub>k-1</sub>) - state of the system at the moment k (k-1)
 * z<sub>k</sub> - measurement of the system state at the moment k
 * u<sub>k</sub> - external control applied at the moment k
 * w<sub>k</sub> and v<sub>k</sub> are normally-distributed process and measurement noise, respectively:
 * p(w) ~ N(0,Q)
 * p(v) ~ N(0,R),
 * that is,
 * Q - process noise covariance matrix, constant or variable,
 * R - measurement noise covariance matrix, constant or variable
 * Jama matrix的使用
 * double[][] vals = {{1.,2.,3},{4.,5.,6.},{7.,8.,10.}};
 * Matrix A = new Matrix(vals);
 * Matrix b = Matrix.random(3,1);
 * Matrix x = A.solve(b);
 * Matrix r = A.times(x).minus(b);
 * double rnorm = r.normInf();
 */

public class MKalman {
    /** number of measurement vector dimensions */
    int mp;
    /** number of state vector dimensions */
    int dp;
    /** number of control vector dimensions */
    int cp;
    /**  predicted state 这里做了修改，由于状态方程常为非线性的 (x'(k)): x(k)=f(x(k-1))+  */
    Matrix state_pre;
    /**  posteriori state 后验状态，或者是状态修正 x(k)=x'(k)+K(k)*(z(k)-H*x'(k))*/
    Matrix state_post;
    /**  状态转移矩阵 state transition matrix (A) */
    Matrix state_trans_matrix;
    /** 状态控制矩阵 state control matrix (B) */
    Matrix state_control_matrix;
    /**  测量转移矩阵  measurement transition matrix (H) */
    Matrix measurement_trans_matrix;
    /** 过程噪声协方差矩阵 process noise covariance matrix（Q)*/
    Matrix process_noise_cov;
    /** 观测噪声协方差矩阵 measurement noise covariance matrix (R) */
    Matrix measurement_noise_cov;
    /** 预测(先验)的误差协方差矩阵 predicted error covariance matrix (P'(k))  : P'(k)=A*P(k-1)*At + Q) */
    Matrix error_cov_pre;
    /** 卡尔曼增益 Kalman gain matrix (K(k)): K(k)=P'(k)*Ht*inv(H*P'(k)*Ht+R) */
    Matrix gain;
    /** 后验的误差协方差矩阵 posteriori error covariance matrix (P(k)): P(k)=(I-K(k)*H)*P'(k) */
    Matrix error_cov_post;
    /**  tempeary matrix */
    Matrix temp1;
    Matrix temp2;
    Matrix temp3;
    Matrix temp4;
    Matrix temp5;

    /**
     * 卡尔曼滤波/线性卡尔曼滤波
     * @param dynam_params
     * @param measure_params
     * @param control_params
     */
    public MKalman (int dynam_params, int measure_params, int control_params) {
        dp = dynam_params;
        mp = measure_params;
        cp = control_params;
        // 状态预测
        state_pre=new Matrix(dp,1); // 4X1  先验状态
        //状态更新
        state_post=new Matrix(dp,1); //4X1,后验状态
        // init
        state_trans_matrix=new Matrix(dp,dp); // 状态转移矩阵
        state_control_matrix=new Matrix(dp,cp); // 状态控制矩阵
        measurement_trans_matrix=new Matrix(mp,dp);// 观测转移矩阵
        //协方差
        process_noise_cov=new Matrix(dp,dp); //Q 状态噪声矩阵，很多设为单位阵
        measurement_noise_cov=new Matrix(mp,mp); // R 观测噪声矩阵
        //误差协方差
        error_cov_pre=new Matrix(dp,dp);// P' 误差协方差的预测矩阵
        error_cov_post=new Matrix(dp,dp); // P 误差协方差的后验矩阵
        // 卡尔曼增益
        gain=new Matrix(dp,mp);

        temp1=new Matrix(dp,dp);
        temp2=new Matrix(mp,dp);
        temp3=new Matrix(mp,mp);
        temp4=new Matrix(mp,dp);
        temp5=new Matrix(mp,1);
    }

    /**
     * 没有状态噪声的情况下
     * @param dynam_params
     * @param measure_params
     */
    public MKalman(int dynam_params, int measure_params){
        this(dynam_params,measure_params,0);
    }

//    public Matrix Predict(Matrix control){
//        // (1) predict the state adead
//        // update the state :在非线性状态方程中，应使用状态方程去预测状态方程，所以这里要注意
//        //这部分放在其他程序里，通过设置来搞定
//
//        //(2)preject the error covariance ahead
//        // update error covariance matrices: temp1 = A*P(k)
//        temp1=state_trans_matrix.times(error_cov_post);
//        // P'(k) = temp1*At + Q
////        error_cov_pre=temp1.times(transition_matrix.transpose()).plus(process_noise_cov);
//        error_cov_pre = temp1.gemm(state_trans_matrix.transpose(), process_noise_cov, 1, 1);
//
//    }
    /**
     * bjx: 预测方程改放在主程序里  2018-03-29
     * adjust model state.
     * The function kalmanCorrect adjusts stochastic model state
     * K<sub>k</sub>=P'<sub>k</sub>*H<sup>T</sup>*(H*P'<sub>k</sub>*H<sup>T</sup>+R)<sup>-1</sup>
     * x<sub>k</sub>=x'<sub>k</sub>+K<sub>k</sub>*(z<sub>k</sub>-H*x'<sub>k</sub>)
     * P<sub>k</sub>=(I-K<sub>k</sub>*H)*P'<sub>k</sub>
     * @param measurement
     * @return
     */
    public Matrix Fusion(final Matrix measurement){
        //(2)preject the error covariance ahead
        // update error covariance matrices: temp1 = A*P(k)
        temp1=state_trans_matrix.times(error_cov_post);
        // P'(k) = temp1*At + Q
//        error_cov_pre=temp1.times(transition_matrix.transpose()).plus(process_noise_cov);
        error_cov_pre = temp1.gemm(state_trans_matrix.transpose(), process_noise_cov, 1, 1);
        // 以上为预测部分
        // (1) Compute the Kalman gain
        // temp2 = H*P'(k)
        temp2 = measurement_trans_matrix.times(error_cov_pre);
        // temp3=temp2*Ht + R
        temp3 = temp2.gemm(measurement_trans_matrix.transpose(), measurement_noise_cov, 1, 1);
        // temp4= inv(temp3)*temp2 = Kt(k)
        temp4=temp3.solve(temp2);
        // K(k)
        gain = temp4.transpose();
        // (2) Update estimate with measurement z(k)
        // temp5 = z(k) - H*x'(k)
        Log.i("看矩阵","观测转移矩阵为：\n"+measurement_trans_matrix.toString());
        Log.i("看矩阵","观测量为：\n"+measurement.toString());
        Log.i("看矩阵","状态预测为：\n"+state_pre.toString());
        temp5 = measurement_trans_matrix.gemm(state_pre, measurement, -1, 1);
        // x(k) = x'(k) + K(k)*temp5
        state_post = gain.gemm(temp5, state_pre, 1, 1);
        Log.i("看矩阵","状态更新为：\n"+state_post.toString());
        // (3) Update the error covariance.
        // P(k) = P'(k) - K(k)*temp2
        error_cov_post = gain.gemm(temp2, error_cov_pre, -1, 1);
        return state_post;
    }
    /**
     * 设定状态预测,这部分需由外部输入
     * @param state_pre
     */
    public void setState_pre(Matrix state_pre){
        this.state_pre = state_pre;
    }
    /**
     * Getter
     * @return
     */
    public Matrix getState_pre() {
        return state_pre;
    }
    /**
     * Setter
     * @param state_post
     */
    public void setState_post(Matrix state_post) {
        this.state_post = state_post;
    }

    public Matrix getState_post() {
        return state_post;
    }

    /**
     * 设定状态转移矩阵
     * @param transition_matrix
     */
    public void setState_trans_matrix(Matrix transition_matrix){
        this.state_trans_matrix=transition_matrix;
    }
    public Matrix getState_trans_matrix(){
        return state_trans_matrix;
    }

    /**
     * 设定状态控制矩阵
     * @param control_matrix
     */
    public void setState_control_matrix(Matrix control_matrix){
        this.state_control_matrix=control_matrix;
    }
    public Matrix getState_control_matrix(){
        return state_control_matrix;
    }
    /**  设定的是观测转移矩阵  */
    public void setMeasure_trans_matrix(Matrix measure_trans_matrix){
        this.measurement_trans_matrix=measure_trans_matrix;
    }
    public Matrix getMeasure_trans_matrix(){
        return measurement_trans_matrix;
    }
    /** 设定过程噪声  */
    public void setProcess_noise_cov(Matrix process_noise_cov){
        this.process_noise_cov=process_noise_cov;
    }
    public Matrix getProcess_noise_cov(){
        return process_noise_cov;
    }
    /**  设定测量噪声  */
    public void setMeasurement_noise_cov(Matrix measurement_noise_cov){
        this.measurement_noise_cov=measurement_noise_cov;
    }
    public Matrix getMeasurement_noise_cov(){
        return measurement_noise_cov;
    }
    /** 设定误差协方差矩阵 */
    public void setError_cov_pre(Matrix error_cov_pre){
        this.error_cov_pre = error_cov_pre;
    }
    public Matrix getError_cov_pre(){
        return error_cov_pre;
    }
    /** 设定卡尔曼增益  */
    public void setGain(Matrix gain){
        this.gain=gain;
    }
    public Matrix getGain(){
        return gain;
    }
    /** 设定误差协方差更新矩阵 */
    public void setError_cov_post(Matrix error_cov_post){
        this.error_cov_post=error_cov_post;
    }
    public Matrix getError_cov_post(){
        return error_cov_post;
    }
    /** 自定义的矩阵转字符串的函数 */
    public String mat2string(Matrix matrix){
        String matStr="";
        int row=matrix.getRowDimension();
        int col=matrix.getColumnDimension();
        for (int i=0;i<row;i++){
            for(int j=0;j<col;j++){
                matStr+=String.valueOf(matrix.get(i,j))+"\t";
            }
        }

        return matStr;
    }
}
