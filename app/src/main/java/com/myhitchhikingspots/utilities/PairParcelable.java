package com.myhitchhikingspots.utilities;

import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.mapboxsdk.annotations.Icon;

/**
 * Created by leoboaventura on 14/06/2017.
 */

public class PairParcelable implements Parcelable {
    String key, value;

    public void setKey(String k) {
        key = k;
    }

    public void setValue(String v) {
        value = v;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public PairParcelable(String k, String v) {
        key=k;
        value=v;
    }

    public PairParcelable(Parcel in) {
        setKey(in.readString());
        setValue(in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(getKey());
        out.writeString(getValue());
    }

    public static final Parcelable.Creator<PairParcelable> CREATOR
            = new Parcelable.Creator<PairParcelable>() {
        public PairParcelable createFromParcel(Parcel in) {
            return new PairParcelable(in);
        }

        public PairParcelable[] newArray(int size) {
            return new PairParcelable[size];
        }
    };

}
