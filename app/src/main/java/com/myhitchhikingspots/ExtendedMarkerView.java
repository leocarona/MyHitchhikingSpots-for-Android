package com.myhitchhikingspots;

import android.support.annotation.Nullable;

import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.annotations.BaseMarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;

/**
 * Created by leoboaventura on 25/02/2017.
 */

public class ExtendedMarkerView extends MarkerView {

    private String tag;

    public ExtendedMarkerView(BaseMarkerViewOptions baseMarkerOptions, String tag) {
        super(baseMarkerOptions);
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

}
