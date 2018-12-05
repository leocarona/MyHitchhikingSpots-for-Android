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
    private final onPostExecute callback;
    private String errMsg = "";

    public interface onPostExecute {
        void setupData(List<Spot> spotList, String errMsg);
    }

    LoadHitchwikiSpotsListTask(onPostExecute callback) {
        this.callback = callback;
    }

    @Override
    protected List<Spot> doInBackground(Void... voids) {
        try {
            PlaceInfoBasic[] placesContainerFromFile = Utils.loadHitchwikiSpotsFromLocalFile();

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
        callback.setupData(spotList, errMsg);
    }
}
