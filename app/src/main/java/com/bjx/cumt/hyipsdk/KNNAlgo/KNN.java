package com.bjx.cumt.hyipsdk.KNNAlgo;

import android.util.Log;

import com.bjx.cumt.hyipsdk.jama.Matrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by cumt_bjx on 2018/3/16.
 */

public class KNN {
    private static String TAG = "KNN";
    //设置地磁定位NN序列
    private static int geomagneticLoc = 1;
    //这个可以设定最好
    private int nearPointNum;

    public KNN() {
        this.nearPointNum = 3;
        Log.i(TAG, "create KNN object");
    }

    //这个直接传递K值
    public KNN(int k) {
        this.nearPointNum = k;
        Log.i(TAG, "create KNN object");
    }

    public MPoint getInterestPoint(List<ReferPoint> referPointAllList, ReferPoint testPoint) {
        //Log.i(TAG,"getInterestPoint+wifi+ble");
        MPoint mPoint = new MPoint();
        double interestPoint_X = 0.0;
        double interestPoint_Y = 0.0;
        //计算距离
        List<DistanceRank> distanceRankList = distanceCalculate(referPointAllList, testPoint);
        //依据距离排序，提取临近点点号
        List<Integer> nearPointNumList = sortDistanceList(distanceRankList, nearPointNum);
        //计算权重
        List<Float> weightList = getWeight(distanceRankList, nearPointNum);
        //提取近邻坐标点---分类
        List<MPoint> nearPointCoorList = getNeighborPoint(referPointAllList, nearPointNumList);
        if (nearPointCoorList != null) {
            //加权质心算法
            for (int i = 0; i < nearPointCoorList.size(); i++) {
                interestPoint_X += nearPointCoorList.get(i).x * weightList.get(i);
                interestPoint_Y += nearPointCoorList.get(i).y * weightList.get(i);
            }
            mPoint.x = interestPoint_X;
            mPoint.y = interestPoint_Y;
        } else {
            mPoint = null;
        }
        return mPoint;
    }

    //地磁匹配调用此方法
    public MPoint getInterestPoint(List<ReferPoint> referPointAllList, ReferPoint testPoint, int mflag) {
        MPoint mPoint = new MPoint();
        if (mflag != 0) {
            Log.i(TAG, "getInterestPoint+geomagnetic");
            double interestPoint_X = 0.0;
            double interestPoint_Y = 0.0;
            //计算距离
            Log.i("运算测试指纹数据", String.valueOf(testPoint.getPointSingalAttrMat()));
            List<DistanceRank> distanceRankList = Mdtw.getdtwdistance(referPointAllList, testPoint);
            //依据距离排序，提取临近点点号
            List<Integer> nearPointNumList = sortDistanceList(distanceRankList, nearPointNum);
            for (int i = 0; i < nearPointNumList.size(); i++) {
                Log.i(TAG, "DTW距离序列排序结果" + "," + String.valueOf(nearPointNumList.get(i)) + "," + nearPointNumList);
            }
            //计算权重
            List<Float> weightList = getWeight(distanceRankList, nearPointNum);
            Log.i(TAG, "权重为：" + "," + weightList);
            //提取近邻坐标点---分类
            List<MPoint> nearPointCoorList = getNeighborPoint(referPointAllList, nearPointNumList);
            if (nearPointCoorList != null) {
                //加权质心算法
                for (int i = 0; i < nearPointCoorList.size(); i++) {
                    interestPoint_X += nearPointCoorList.get(i).x * weightList.get(i);
                    interestPoint_Y += nearPointCoorList.get(i).y * weightList.get(i);
                }
                mPoint.x = interestPoint_X;
                mPoint.y = interestPoint_Y;
            } else {
                mPoint = null;
            }
            Log.i(TAG, "getInterestPoint+geomagnetic" + "," + mPoint.x + "," + mPoint.y + "," + nearPointNumList + distanceRankList.get(nearPointNumList.get(0) - 1).getDistance());
        }

        return mPoint;
    }

    /**
     * 计算测试点与指纹库中点的距离，并放在对应List中，方便后续计算
     */
    public List<DistanceRank> distanceCalculate(List<ReferPoint> referPointList, ReferPoint testPoint) {
        //Log.i(TAG," 计算距离");
        if (referPointList == null) {
            return null;
        }

        if (testPoint == null) {
            return null;
        }

        if (referPointList.size() == 0) {
            Log.i(TAG, "参考点的表为空");
            return null;
        }

        Matrix testPointSignalAttr = testPoint.getPointSingalAttrMat();
        //新建一个列表用于存储距离
        List<DistanceRank> distanceList = new ArrayList<DistanceRank>();
        for (int i = 0; i < referPointList.size(); i++) {
            //获取参考点对象
            ReferPoint calReferPoint = referPointList.get(i);
            //Log.i(TAG,"取第 "+(i+1)+" 个参考点，点号是"+calReferPoint.getId());
            Matrix calReferPointSignalAttr = calReferPoint.getPointSingalAttrMat();
            // Log.i(TAG,"参考点中信号的行数为"+calReferPointSignalAttr.getRowDimension()+"列数为"+calReferPointSignalAttr.getColumnDimension());
            //Log.i(TAG,"参考点中信号"+calReferPointSignalAttr);
            double distance = 0.0d;
            Matrix differSignalAttr = calReferPointSignalAttr.minus(testPointSignalAttr);
//            String string="信号差值为";
            int row = differSignalAttr.getRowDimension();
            int column = differSignalAttr.getColumnDimension();
            for (int j = 0; j < row; j++) {
                for (int k = 0; k < column; k++) {
                    double diff = differSignalAttr.get(j, k);
//                    string+=","+diff;
                }
            }
//            Log.i(TAG,"计算得到的rssi差值序列为"+string);
//            Log.i(TAG,"直接转为字符串为"+differSignalAttr.toString());
            //arrayTimes treated as corresponding individual numbers
            Matrix distMat = (differSignalAttr.transpose()).arrayTimes(differSignalAttr.transpose());
            int distRow = distMat.getRowDimension();
            int distColumn = distMat.getColumnDimension();
            String distPFString = "信号差值的平方";
            double distSum = 0d;
            for (int m = 0; m < distRow; m++) {
                for (int n = 0; n < distColumn; n++) {
                    double pingfanghe = distMat.get(m, n);
                    distSum = distSum + pingfanghe;
                    distPFString += "," + pingfanghe;
                }
            }
//            Log.i(TAG,"计算平方和的矩阵行数为"+distMat.getRowDimension()+"列数为"+distMat.getColumnDimension());

            //计算平方和
            distance = Math.sqrt(distSum);
//            Log.i(TAG,distPFString+" ,平方和为"+distSum+"，平方根为"+distance);
//            distance=Math.sqrt(distMat.get(0,0));
            DistanceRank DR = new DistanceRank();
            DR.setDistance(distance);
            DR.setID((int) calReferPoint.getId());
//            DR.setmPoint(calReferPoint.getMPoint());
//            Log.i(TAG,"distanceCalculate写入到DR中的距离为"+distance+", ID为"+calReferPoint.getId()+", x坐标是："+calReferPoint.getMPoint().x+" y坐标是"+calReferPoint.getMPoint().y);//////
            //将其放入到list中就不用使dr为全局变量
            distanceList.add(DR);
        }
        return distanceList;
    }

    /**
     * 对点按照距离进行排序
     * 并选取邻近的m个点放到list
     */
    public List<Integer> sortDistanceList(List<DistanceRank> distanceList, int sortSize) {
        //Log.i(TAG," sortDistanceList");
        //先定义一个放置距离的List
        List<Integer> resultList = new ArrayList<Integer>();
        if (distanceList != null && sortSize > distanceList.size()) {
            sortSize = distanceList.size();
        }
//        Log.i(TAG,"计算出的距离个数"+sortSize);
        //生成一个距离级别的数组
        DistanceRank[] distanceRanks = new DistanceRank[distanceList.size()];
        //将列表生成数组
        distanceList.toArray(distanceRanks);
        //对数组进行排序
        Arrays.sort(distanceRanks);
        for (int i = 0; i < sortSize; i++) {
            resultList.add(distanceRanks[i].getID());
        }
        return resultList;
    }

    /**
     * 从距离列表中选取m个计算各自的权重，并放在列表中
     */
    public List<Float> getWeight(List<DistanceRank> distanceList, int sortSize) {
        // Log.i(TAG,"getWeight");
        List<Float> resultList = new ArrayList<Float>();
        if (sortSize > distanceList.size()) {
            sortSize = distanceList.size();
        }
        //生成一个距离级别的数组
        DistanceRank[] distanceRanks = new DistanceRank[distanceList.size()];
        //将列表生成数组
        distanceList.toArray(distanceRanks);
        //对数组进行排序
        Arrays.sort(distanceRanks);
        double distanceInverseSum = 0d;
        for (int i = 0; i < sortSize; i++) {
            double distance = distanceRanks[i].getDistance();
            double weight;
            if (Math.abs(distance - 0) <= 0.001) {
                weight = 1000d;
            } else {
                weight = 1 / distance;
            }
            distanceInverseSum += weight;
            //重写distanceRank
            distanceRanks[i].setDistance(weight);
        }
        //计算权重，并放在列表中
        for (int i = 0; i < sortSize; i++) {
            float weightfloat = (float) (distanceRanks[i].getDistance() / distanceInverseSum);
            resultList.add(weightfloat);
            //Log.i(TAG, "点号："+distanceRanks[i].getID()+"距离："+(1/distanceRanks[i].getDistance())+"权重:"+weightfloat);
//          resultList.add((float)(distanceRanks[i].getDistance()/distanceInverseSum));
        }
        return resultList;
    }

    //提取对应点的坐标位置
    List<MPoint> getNeighborPoint(List<ReferPoint> referPointList, List<Integer> nearestList) {
        List<MPoint> resultlist = new ArrayList<MPoint>();
        ReferPoint neighborReferPoint;
        if (nearestList == null || nearestList.size() == 0) {
            Log.i(TAG, "getNeighborPoint（坐标点近邻提取）列表为空");
            resultlist = null;
        } else {
            for (int i = 0; i < nearestList.size(); i++) {
                //获取近邻点点号
                int neighborPointID = nearestList.get(i);
                //获取近邻参考点,这里有个弊端，必须是点号和参考点list中的一致
//                ReferPoint neighborReferPoint=referPointList.get(neighborPointID);——点号减1才是index
                //根据点号（即ID）获取参考点
                Log.i(TAG, "点号" + String.valueOf(neighborPointID));
                for (int j = 0; j < referPointList.size(); j++) {
                    if (referPointList.get(j).getId() == neighborPointID) {
                        Log.i(TAG, String.valueOf("索引所在位置" +j));
                        neighborReferPoint = referPointList.get(j);
                        //获取几何点
                        MPoint neighborPoint = new MPoint();
                        neighborPoint = neighborReferPoint.getMPoint();
//				        Log.i(TAG,"getNeighborPoint 近邻点坐标"+neighborPoint.x+","+neighborPoint.y);
                        resultlist.add(neighborPoint);
//					    resultlist.add(referPointList.get(i).getPoint());
                    }
                }
            }
        }

        return resultlist;
    }
}
