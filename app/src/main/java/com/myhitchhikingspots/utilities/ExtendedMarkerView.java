package com.myhitchhikingspots.utilities;

import androidx.annotation.Nullable;

import com.mapbox.mapboxsdk.annotations.BaseMarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.myhitchhikingspots.Constants;

/**
 * Created by leoboaventura on 25/02/2017.
 */

public class ExtendedMarkerView extends MarkerView {

    private String tag;
    private int spotType = Constants.SPOT_TYPE_UNKNOWN;

    public ExtendedMarkerView(BaseMarkerViewOptions baseMarkerOptions, String tag, int spotType) {
        super(baseMarkerOptions);
        this.tag = tag;
        this.spotType = spotType;
    }

    public String getTag() {
        return tag;
    }

    public int getSpotType() {
        return spotType;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setSpotType(int spotType) {
        this.spotType = spotType;
    }
}
