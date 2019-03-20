package com.bjx.cumt.hyipsdk.KNNAlgo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by cumt_bjx on 2018/3/15.
 * 用于存放x和y坐标，需要进行粒子化
 */

public class MPoint implements Parcelable {
    public double x;
    public double y;

    public MPoint() {
    }

    public MPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public MPoint(int x, int y){
        this.x=(double)x;
        this.y=(double)y;
    }
    /**
     * Set the point's x and y coordinates
     */
    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }
    /**
     * Negate the point's coordinates
     */
    public final void negate() {
        x = -x;
        y = -y;
    }
    /**
     * Offset the point's coordinates by dx, dy
     */
    public final void offset(double dx, double dy) {
        x += dx;
        y += dy;
    }
    /**
     * Returns true if the point's coordinates equal (x,y)
     */
    public final boolean equals(double x, double y) {
        return this.x == x && this.y == y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MPoint point = (MPoint) obj;
        if (x != point.x) return false;
        if (y != point.y) return false;
        return true;
    }

    @Override
    public String toString() {
        return "MPoint{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(x);
        parcel.writeDouble(y);
    }
    /**
     * 这部分一定要写，不然会报错
     * android.os.BadParcelableException:
     * Parcelable protocol requires a Parcelable.Creator object called  CREATOR on class com.casm.ips.radiomap.MPoint
     */
    public static final Creator<MPoint> CREATOR =new Creator<MPoint>() {
        /**
         * return a new mpoint from the data in the specified parcel
         */
        @Override
        public MPoint createFromParcel(Parcel parcel) {
            MPoint mPoint=new MPoint();
            mPoint.readFromParcel(parcel);
            return mPoint;
        }

        @Override
        public MPoint[] newArray(int i) {
            return new MPoint[i];
        }
    };

    public void readFromParcel(Parcel parcel){
        x=parcel.readDouble();
        y=parcel.readDouble();
    }
}
