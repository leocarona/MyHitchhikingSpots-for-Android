package com.myhitchhikingspots.interfaces;

import android.content.Context;

import com.mapbox.mapboxsdk.maps.MapboxMap;

public interface FirstLocationUpdateListener {
    Context getContext();
    MapboxMap getMapboxMap();
    void moveCameraToLastKnownLocation();
}
