package com.myhitchhikingspots;

import android.os.AsyncTask;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;
import com.myhitchhikingspots.utilities.Utils;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Updates all StartDateTime to UTC.
 * WARN: Make sure this task is called only once
 * otherwise all StartDateTimes will get permanently messed up.
 **/
public class FixSpotsStartDateTimeAsyncTask extends AsyncTask<MyHitchhikingSpotsApplication, Void, List<Spot>> {
    private final LoadSpotsAndRoutesTask.onPostExecute callback;
    private List<Spot> spotList;
    private Spot mCurrentWaitingSpot;
    private String errMsg = "";

    FixSpotsStartDateTimeAsyncTask(LoadSpotsAndRoutesTask.onPostExecute callback, List<Spot> spotList, Spot mCurrentWaitingSpot) {
        this.callback = callback;
        this.spotList = spotList;
        this.mCurrentWaitingSpot = mCurrentWaitingSpot;
    }

    @Override
    protected List<Spot> doInBackground(MyHitchhikingSpotsApplication... appContexts) {
        if (this.isCancelled())
            return null;

        try {
            for (Spot s : spotList) {
                DateTime fixedDateTime = Utils.fixDateTime(s.getStartDateTimeMillis());
                s.setStartDateTime(fixedDateTime);
            }

            if (mCurrentWaitingSpot != null) {
                DateTime fixedDateTime = Utils.fixDateTime(mCurrentWaitingSpot.getStartDateTimeMillis());
                mCurrentWaitingSpot.setStartDateTime(fixedDateTime);
            }

            persistSpotListChangesOnDB(appContexts[0]);
        } catch (final Exception ex) {
            Crashlytics.logException(ex);
            errMsg = "Fixing spots date time failed.\n" + ex.getMessage();
            return new ArrayList<>();
        }

        return spotList;
    }

    void persistSpotListChangesOnDB(MyHitchhikingSpotsApplication appContext) {
        DaoSession daoSession = appContext.getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();

        spotDao.updateInTx(spotList);
    }

    @Override
    protected void onPostExecute(List<Spot> spotList) {
        super.onPostExecute(spotList);
        if (this.isCancelled())
            return;

        //Create a record to track when all StartDateTime have been automatically fixed (if there was any spot on the list)
        if (!spotList.isEmpty())
            Answers.getInstance().logCustom(new CustomEvent("StartDateTime automatically fixed"));

        callback.setupData(spotList, mCurrentWaitingSpot, errMsg);
    }
}
