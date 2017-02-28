package com.myhitchhikingspots;

import android.support.annotation.Nullable;

import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.annotations.BaseMarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerView;

/**
 * Created by leoboaventura on 25/02/2017.
 */

public class ExtendedMarkerView extends Marker {

    private String tag;

    public ExtendedMarkerView(BaseMarkerOptions baseMarkerOptions, String tag) {
        super(baseMarkerOptions);
        this.tag = tag;
    }

   /* public ExtendedMarkerView() {
        super(new MarkerViewOptions());
        /this.alpha = baseMarkerViewOptions.getAlpha();
        this.anchorU = baseMarkerViewOptions.getAnchorU();
        this.anchorV = baseMarkerViewOptions.getAnchorV();
        this.infoWindowAnchorU = baseMarkerViewOptions.getInfoWindowAnchorU();
        this.infoWindowAnchorV = baseMarkerViewOptions.getInfoWindowAnchorV();
        this.flat = baseMarkerViewOptions.isFlat();
        this.rotation = baseMarkerViewOptions.getRotation();
        this.selected = baseMarkerViewOptions.selected;
        this.tag =;/
    }*/

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

}
