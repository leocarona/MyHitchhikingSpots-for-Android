package com.myhitchhikingspots.interfaces;

import android.content.Context;
import android.location.Location;

import com.mapbox.mapboxsdk.maps.MapboxMap;

public interface FirstLocationUpdateListener {
    Context getContext();
    MapboxMap getMapboxMap();
    void moveCameraToLastKnownLocation();
    void updateLastKnownLocation(Location loc);
}
