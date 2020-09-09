package com.myhitchhikingspots.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.interfaces.ISpotsRepository;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;
import com.myhitchhikingspots.utilities.Utils;

import org.greenrobot.greendao.database.Database;
import org.joda.time.DateTime;

import java.util.List;

public class SQLiteSpotsRepository implements ISpotsRepository {

    private MutableLiveData<List<Spot>> spots;
    private static SQLiteSpotsRepository sInstance;

    public static SQLiteSpotsRepository getInstance() {
        if (sInstance == null) {
            synchronized (SQLiteSpotsRepository.class) {
                if (sInstance == null) {
                    sInstance = new SQLiteSpotsRepository();
                }
            }
        }
        return sInstance;
    }

    boolean isSpotsLisFirsttLoaded = false;

    SQLiteSpotsRepository() {
        spots = new MutableLiveData<>();
    }

    public LiveData<List<Spot>> getSpots(@Nullable Context context) {
        if (!isSpotsLisFirsttLoaded) {
            loadSpots(context);
            isSpotsLisFirsttLoaded = true;
        }
        return spots;
    }

    private DaoSession getDaoSession(Context context) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, Constants.INTERNAL_DB_FILE_NAME, null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        return daoMaster.newSession();
    }

    /* Loads the list of spots ordered from the most recent at the begining to the oldest. */
    public void loadSpots(@Nullable Context context) {
        DaoSession daoSession = getDaoSession(context);
        SpotDao spotDao = daoSession.getSpotDao();
        List<Spot> spotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.StartDateTime).list();

        setSpots(spotList);
    }

    public void reloadSpots(@Nullable Context context) {
        loadSpots(context);
    }

    private void setSpots(List<Spot> spotList) {
        spots.setValue(spotList);
    }

    public Spot getWaitingSpot(@Nullable Context context) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();
        List<Spot> areWaitingForARide = spotDao.queryBuilder().where(SpotDao.Properties.IsHitchhikingSpot.eq(true), SpotDao.Properties.IsWaitingForARide.eq(true))
                .orderDesc(SpotDao.Properties.StartDateTime).list();

        if (areWaitingForARide.size() > 1)
            Crashlytics.logException(new Exception("Error at: LoadCurrentWaitingSpot. More than 1 spot was found with IsWaitingForARide set to true! This should never happen - Please be aware of this."));
        if (areWaitingForARide.size() >= 1)
            return areWaitingForARide.get(0);

        return null;
    }

    public Spot getLastAddedRouteSpot(@Nullable Context context) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();

        return spotDao.queryBuilder()
                .where(SpotDao.Properties.IsPartOfARoute.eq(true))
                .orderDesc(SpotDao.Properties.StartDateTime).limit(1).unique();
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

        //TODO: Check whether it's good idea to sync spots list here
        loadSpots(context);
    }

    public void insertOrReplace(@Nullable Context context, @NonNull Spot spot) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();
        spotDao.insertOrReplace(spot);

        //TODO: Check whether it's good idea to sync spots list here
        loadSpots(context);
    }

    public void insertOrReplace(@Nullable Context context, @NonNull List<Spot> newSpots, boolean shouldGenerateNewIds, @NonNull final IInsertOrReplaceEventListener callback) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();
        spotDao.insertOrReplaceInTx(newSpots);

        //TODO: Check whether it's good idea to sync spots list here
        loadSpots(context);

        int numberOfSpotsOnTheListNow = newSpots.size();
        if (spots.getValue() != null)
            numberOfSpotsOnTheListNow += spots.getValue().size();

        callback.onSuccess(numberOfSpotsOnTheListNow);
    }

    public void deleteSpots(@NonNull List<String> spotsToBeDeleted_idList, @Nullable Context context) {
        if (spotsToBeDeleted_idList.size() == 0)
            return;

        //Get a DB session
        Database db = DaoMaster.newDevSession(context, Constants.INTERNAL_DB_FILE_NAME).getDatabase();

        String sql = SpotDao.Properties.Id.columnName + " = '" + TextUtils.join("' OR " + SpotDao.Properties.Id.columnName + " = '", spotsToBeDeleted_idList) + "'";

        //Delete selected spots from DB
        db.execSQL(String.format("DELETE FROM %1$s WHERE %2$s",
                SpotDao.TABLENAME,
                sql));

        //Sync spots list
        loadSpots(context);
    }

    public void deleteSpot(@NonNull Spot spot, @Nullable Context context) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();
        spotDao.delete(spot);

        //Sync spots list
        loadSpots(context);
    }

    public void deleteAllSpots(@Nullable Context context) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();
        spotDao.deleteAll();

        //Sync spots list
        loadSpots(context);
    }

    public boolean isAnySpotMissingAuthor(@Nullable Context context) {
        DaoSession daoSession = getDaoSession(context);
        SpotDao spotDao = daoSession.getSpotDao();
        return !spotDao.queryBuilder().whereOr(SpotDao.Properties.AuthorUserName.isNull(), SpotDao.Properties.AuthorUserName.eq(""))
                .limit(1).list().isEmpty();
    }

    /**
     * Assign the given username to all spots missing AuthorUserName.
     *
     * @param username The username that the person uses on Hitchwiki.
     */
    public void assignMissingAuthorTo(@Nullable Context context, @Nullable String userId, @NonNull String username) {
        DaoSession daoSession = getDaoSession(context);
        String sqlUpdateAuthorStatement = "UPDATE %1$s SET %2$s = '%3$s' WHERE %2$s IS NULL OR %2$s = ''";
        //Get a DB session
        Database db = daoSession.getDatabase();

        //Delete selected spots from DB
        db.execSQL(String.format(sqlUpdateAuthorStatement,
                SpotDao.TABLENAME,
                SpotDao.Properties.AuthorUserName.columnName,
                username));

        //Sync spots list
        loadSpots(context);
    }
}
