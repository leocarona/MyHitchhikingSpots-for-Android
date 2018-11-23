package com.myhitchhikingspots.utilities;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.utilities.ExtendedMarkerView;

/**
 * Default MarkerViewAdapter used for base class of MarkerView to adapt a MarkerView to an ImageView
 */
public class ExtendedMarkerViewAdapter extends MapboxMap.MarkerViewAdapter<ExtendedMarkerView> {

    private LayoutInflater inflater;

    public ExtendedMarkerViewAdapter(Context context) {
        super(context, null);
        inflater = LayoutInflater.from(context);
    }

    @Nullable
    @Override
    public View getView(@NonNull ExtendedMarkerView marker, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.mapbox_view_image_marker, parent, false);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.imageView.setImageBitmap(marker.getIcon().getBitmap());
        return convertView;
    }

    private static class ViewHolder {
        ImageView imageView;
    }
}