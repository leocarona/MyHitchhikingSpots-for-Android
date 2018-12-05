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
        void setupData(List<Spot> spotList, Spot mCurrentWaitingSpot, String errMsg);
    }

    LoadSpotsAndRoutesTask(onPostExecute callback) {
        this.callback = callback;
    }

    @Override
    protected List<Spot> doInBackground(MyHitchhikingSpotsApplication... appContexts) {
        try {
            MyHitchhikingSpotsApplication appContext = appContexts[0];
            DaoSession daoSession = appContext.getDaoSession();
            SpotDao spotDao = daoSession.getSpotDao();

            mCurrentWaitingSpot = appContext.getCurrentSpot();

            return spotDao.queryBuilder().orderDesc(SpotDao.Properties.IsPartOfARoute, SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();

        } catch (final Exception ex) {
            Crashlytics.logException(ex);
            errMsg = "Loading spots failed.\n" + ex.getMessage();
            return new ArrayList<>();
        }
    }


    @Override
    protected void onPostExecute(List<Spot> spotList) {
        callback.setupData(spotList, mCurrentWaitingSpot, errMsg);
    }
}