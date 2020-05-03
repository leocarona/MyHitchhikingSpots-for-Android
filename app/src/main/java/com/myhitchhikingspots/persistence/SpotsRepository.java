package com.myhitchhikingspots.persistence;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;
import com.myhitchhikingspots.utilities.Utils;

import org.joda.time.DateTime;

import java.util.List;

public class SpotsRepository {

    private MutableLiveData<List<Spot>> spots;
    private static SpotsRepository sInstance;

    public static SpotsRepository getInstance() {
        if (sInstance == null) {
            synchronized (SpotsRepository.class) {
                if (sInstance == null) {
                    sInstance = new SpotsRepository();
                }
            }
        }
        return sInstance;
    }

    public LiveData<List<Spot>> getSpots(Context context) {
        if (spots == null) {
            spots = new MutableLiveData<>();
            loadSpots(context);
        }
        return spots;
    }

    private DaoSession getDaoSession(Context context) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, Constants.INTERNAL_DB_FILE_NAME, null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        return daoMaster.newSession();
    }

    public void loadSpots(Context context) {
        DaoSession daoSession = getDaoSession(context);
        SpotDao spotDao = daoSession.getSpotDao();
        List<Spot> spotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.IsPartOfARoute, SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();

        setSpots(spotList);
    }

    private void setSpots(List<Spot> spotList) {
        spots.setValue(spotList);
    }

    public Spot getWaitingSpot(Context context) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();
        List<Spot> areWaitingForARide = spotDao.queryBuilder().where(SpotDao.Properties.IsHitchhikingSpot.eq(true), SpotDao.Properties.IsWaitingForARide.eq(true))
                .orderDesc(SpotDao.Properties.IsPartOfARoute, SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();

        if (areWaitingForARide.size() > 1)
            Crashlytics.logException(new Exception("Error at: LoadCurrentWaitingSpot. More than 1 spot was found with IsWaitingForARide set to true! This should never happen - Please be aware of this."));
        if (areWaitingForARide.size() >= 1)
            return areWaitingForARide.get(0);

        return null;
    }

    public Spot getLastAddedRouteSpot(Context context) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();

        return spotDao.queryBuilder()
                .where(SpotDao.Properties.IsPartOfARoute.eq(true))
                .orderDesc(SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).limit(1).unique();
    }

    public void fixSpotsStartDateTime(Context context, List<Spot> spotList, Spot currentWaitingSpot) {
        for (Spot s : spotList) {
            DateTime fixedDateTime = Utils.fixDateTime(s.getStartDateTimeMillis());
            s.setStartDateTime(fixedDateTime);
        }

        if (currentWaitingSpot != null) {
            DateTime fixedDateTime = Utils.fixDateTime(currentWaitingSpot.getStartDateTimeMillis());
            currentWaitingSpot.setStartDateTime(fixedDateTime);
        }

        updateInTx(context, spotList);
    }

    public void updateInTx(Context context, List<Spot> spotList) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();
        spotDao.updateInTx(spotList);
    }

    public Cursor rawQuery(Context context, String sql, String[] selectionArgs) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, Constants.INTERNAL_DB_FILE_NAME, null);
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.rawQuery(sql, selectionArgs);
    }

    public void insertOrReplace(Context context, Spot spot) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();
        spotDao.insertOrReplace(spot);
    }

    public void deleteSpot(Context context, Spot spot) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();
        spotDao.delete(spot);
    }
}
