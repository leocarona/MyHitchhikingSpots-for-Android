package com.myhitchhikingspots;

import android.os.AsyncTask;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import hitchwikiMapsSDK.entities.PlaceInfoBasic;

public class LoadHitchwikiSpotsListTask extends AsyncTask<Void, Void, List<Spot>> {
    private String errMsg = "";
    private WeakReference<HitchwikiMapViewFragment> contextRef;

    LoadHitchwikiSpotsListTask(HitchwikiMapViewFragment context) {
        this.contextRef = new WeakReference<>(context);
    }

    @Override
    protected List<Spot> doInBackground(Void... voids) {
        if (this.isCancelled())
            return null;

        HitchwikiMapViewFragment context = contextRef.get();
        if (context == null)
            return new ArrayList<>();

        try {
            PlaceInfoBasic[] placesContainerFromFile = Utils.loadHitchwikiSpotsFromLocalFile(context.getContext());

            List<Spot> spotList = new ArrayList<>();

            if (placesContainerFromFile != null) {
                for (int i = 0; i < placesContainerFromFile.length; i++) {
                    try {
                        Spot s = Utils.convertToSpot(placesContainerFromFile[i]);
                        if (s.getId() == null || s.getId() == 0)
                            s.setId((long) i);
                        spotList.add(s);
                    } catch (Exception ex) {
                        Crashlytics.logException(ex);
                    }
                }
            }

            return spotList;
        } catch (final Exception ex) {
            Crashlytics.logException(ex);
            errMsg = "Loading spots failed.\n" + ex.getMessage();
            return new ArrayList<>();
        }
    }

    @Override
    protected void onPostExecute(List<Spot> spotList) {
        super.onPostExecute(spotList);
        if (this.isCancelled())
            return;

        HitchwikiMapViewFragment context = contextRef.get();
        if (context != null)
            context.onHWSpotListChanged(spotList, errMsg);
    }
}
