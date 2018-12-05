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

public class LoadSpotsAndRoutesTask extends AsyncTask<Void, Void, List<Spot>> {
    private final WeakReference<MyMapsFragment> activityRef;
    private Spot mCurrentWaitingSpot;
    private String errMsg = "";

    LoadSpotsAndRoutesTask(MyMapsFragment activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    @Override
    protected List<Spot> doInBackground(Void... voids) {
        MyMapsFragment activity = activityRef.get();
        if (activity == null)
            return null;

        try {
            MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) activity.getActivity().getApplicationContext());
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
        super.onPostExecute(spotList);
        MyMapsFragment activity = activityRef.get();
        if (activity == null)
            return;

        activity.setupData(spotList, mCurrentWaitingSpot, errMsg);
    }
}