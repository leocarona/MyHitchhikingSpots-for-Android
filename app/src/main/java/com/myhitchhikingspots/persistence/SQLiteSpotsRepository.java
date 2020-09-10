package com.myhitchhikingspots.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.interfaces.IInsertOrReplaceEventListener;
import com.myhitchhikingspots.interfaces.ISpotsRepository;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import org.greenrobot.greendao.database.Database;

import java.util.List;

public class SQLiteSpotsRepository implements ISpotsRepository {

    private MutableLiveData<List<Spot>> spots;
    private static SQLiteSpotsRepository sInstance;
    boolean isSpotsLisFirsttLoaded = false;

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

    public Spot getLastAddedRouteSpot(@Nullable Context context) {
        DaoSession daoSession = getDaoSession(context);

        SpotDao spotDao = daoSession.getSpotDao();

        return spotDao.queryBuilder()
                .where(SpotDao.Properties.IsPartOfARoute.eq(true))
                .orderDesc(SpotDao.Properties.StartDateTime).limit(1).unique();
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
    public void assignMissingAuthorTo(@Nullable Context context, @NonNull String username) {
        DaoSession daoSession = getDaoSession(context);
        String sqlUpdateAuthorStatement = "UPDATE %1$s SET %2$s = '%3$s' WHERE %2$s IS NULL OR %2$s = ''";
        //Get a DB session
        Database db = daoSession.getDatabase();

        //Update spots on DB
        db.execSQL(String.format(sqlUpdateAuthorStatement,
                SpotDao.TABLENAME,
                SpotDao.Properties.AuthorUserName.columnName,
                username));

        //Sync spots list
        loadSpots(context);
    }
}
