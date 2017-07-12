package com.myhitchhikingspots.model;

/**
 * Created by leoboaventura on 02/10/2016.
 */

public class MyLocation implements java.io.Serializable  {
    private double mLatitude = 0.0;
    private double mLongitude = 0.0;

    public MyLocation(double lat, double lng) {
        mLatitude = lat;
        mLongitude = lng;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }
}
