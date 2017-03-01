package com.myhitchhikingspots;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.annotations.BaseMarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.exceptions.InvalidMarkerPositionException;
import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Created by leoboaventura on 25/02/2017.
 */

public class ExtendedMarkerViewOptions extends BaseMarkerViewOptions<ExtendedMarkerView, ExtendedMarkerViewOptions> {

    private String tag;

    private ExtendedMarkerView marker;

    public String getTag() {
        return this.tag;
    }

    public ExtendedMarkerViewOptions tag(String tag) {
        this.tag = tag;
        return getThis();
    }

    public ExtendedMarkerViewOptions() {
        marker = new ExtendedMarkerView(this,"");
    }

    private ExtendedMarkerViewOptions(Parcel in) {
        marker = new ExtendedMarkerView(this,"");
        position((LatLng) in.readParcelable(LatLng.class.getClassLoader()));
        snippet(in.readString());
        title(in.readString());
        flat(in.readByte() != 0);
        anchor(in.readFloat(), in.readFloat());
        infoWindowAnchor(in.readFloat(), in.readFloat());
        rotation(in.readFloat());
        visible(in.readByte() != 0);
        alpha(in.readFloat());
        if (in.readByte() != 0) {
            // this means we have an icon
            String iconId = in.readString();
            Bitmap iconBitmap = in.readParcelable(Bitmap.class.getClassLoader());
            Icon icon = IconFactory.recreate(iconId, iconBitmap);

            icon(icon);
        }

        tag(in.readString());
    }

    @Override
    public ExtendedMarkerViewOptions getThis() {
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(getPosition(), flags);
        out.writeString(getSnippet());
        out.writeString(getTitle());
        out.writeByte((byte) (isFlat() ? 1 : 0));
        out.writeFloat(getAnchorU());
        out.writeFloat(getAnchorV());
        out.writeFloat(getInfoWindowAnchorU());
        out.writeFloat(getInfoWindowAnchorV());
        out.writeFloat(getRotation());
        out.writeByte((byte) (isVisible() ? 1 : 0));
        out.writeFloat(alpha);
        Icon icon = getIcon();
        out.writeByte((byte) (icon != null ? 1 : 0));
        if (icon != null) {
            out.writeString(getIcon().getId());
            out.writeParcelable(getIcon().getBitmap(), flags);
        }
        out.writeString(tag);
    }

    @Override
    public ExtendedMarkerView getMarker() {
        if (position == null) {
            throw new InvalidMarkerPositionException();
        }

        marker.setPosition(position);
        marker.setSnippet(snippet);
        marker.setTitle(title);
        marker.setIcon(icon);
        marker.setFlat(flat);
        marker.setAnchor(anchorU, anchorV);
        marker.setInfoWindowAnchor(infoWindowAnchorU, infoWindowAnchorV);
        marker.setRotation(rotation);
        marker.setVisible(visible);
        marker.setAlpha(alpha);
        marker.setTag(tag);
        return marker; }



    public static final Parcelable.Creator<ExtendedMarkerViewOptions> CREATOR
            = new Parcelable.Creator<ExtendedMarkerViewOptions>() {
        public ExtendedMarkerViewOptions createFromParcel(Parcel in) {
            return new ExtendedMarkerViewOptions(in);
        }

        public ExtendedMarkerViewOptions[] newArray(int size) {
            return new ExtendedMarkerViewOptions[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtendedMarkerViewOptions that = (ExtendedMarkerViewOptions) o;
        return marker != null ? marker.equals(that.marker) : that.marker == null;
    }

    @Override
    public int hashCode() {
        return marker != null ? marker.hashCode() : 0;
    }
}
