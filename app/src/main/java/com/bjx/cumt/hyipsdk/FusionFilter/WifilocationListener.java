package com.bjx.cumt.hyipsdk.FusionFilter;

import java.util.ArrayList;

/**
 * Created by hc on 2018/6/10.
 */

public interface WifilocationListener  {
    public void onWifimac(ArrayList<String> mac);
    public void onLocation(double x, double y, double weight);

}
