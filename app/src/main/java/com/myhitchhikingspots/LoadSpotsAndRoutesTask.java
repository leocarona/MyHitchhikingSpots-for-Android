package com.myhitchhikingspots;

import android.os.AsyncTask;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.MyHitchhikingSpotsApplication;
import com.myhitchhikingspots.MyMapsFragment;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Route;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LoadSpotsAndRoutesTask extends AsyncTask<MyHitchhikingSpotsApplication, Void, List<Spot>> {
    private final onPostExecute callback;
    private Spot mCurrentWaitingSpot;
    private String errMsg = "";

    public interface onPostExecute {
        /**
         * A method to be called once all the spots have been loaded.
         *
         * @param spotList            The list of all saved spots, ordered from the most recently saved spots to the oldest. Spots that belong to routes are the first ones on this list.
         * @param mCurrentWaitingSpot The spot where the user is waiting for a ride, or null.
         * @param errMsg              Any error message that might have occurred while loading the list of spots, or any empty string if succeeded loading the list.
         **/
        void setupData(List<Spot> spotList, Spot mCurrentWaitingSpot, String errMsg);
    }

    LoadSpotsAndRoutesTask(onPostExecute callback) {
        this.callback = callback;
    }

    @Override
    protected List<Spot> doInBackground(MyHitchhikingSpotsApplication... appContexts) {
        if (this.isCancelled())
            return null;

        try {
            MyHitchhikingSpotsApplication appContext = appContexts[0];
            DaoSession daoSession = appContext.getDaoSession();
            SpotDao spotDao = daoSession.getSpotDao();

            return spotDao.queryBuilder().orderDesc(SpotDao.Properties.IsPartOfARoute, SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();

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

        //There should be only one waiting spot, and it should always be at the first position of the list
        // (the list is ordered descending by datetime). But in case some bug has happened and the user
        // has a waiting spot at a different position, let's go through the list.
        for (Spot s : spotList) {
            if (s.getIsWaitingForARide() != null && s.getIsWaitingForARide()) {
                mCurrentWaitingSpot = s;
                break;
            }
        }

        callback.setupData(spotList, mCurrentWaitingSpot, errMsg);
    }
}