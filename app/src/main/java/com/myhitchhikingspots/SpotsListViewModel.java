package com.myhitchhikingspots;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.persistence.SpotsRepository;

import org.greenrobot.greendao.database.Database;

import java.util.List;

public class SpotsListViewModel extends AndroidViewModel {
    private MediatorLiveData<List<Spot>> spotList;
    private MutableLiveData<Spot> mWaitingSpot;
    private SpotsRepository mRepository;

    public SpotsListViewModel(Application context) {
        super(context);
        mRepository = ((MyHitchhikingSpotsApplication) context).getSpotsRepository();
        spotList = new MediatorLiveData<>();
        mWaitingSpot = new MutableLiveData<>();
        spotList.addSource(mRepository.getSpots(context), s -> {
            spotList.setValue(s);
            Spot waitingSpot = null;
            if (s != null)
                waitingSpot = findWaitingSpot(s);
            setWaitingSpot(waitingSpot);
        });
    }

    public LiveData<List<Spot>> getSpots() {
        return spotList;
    }

    public void reloadSpots(Context context) {
        mRepository.loadSpots(context);
    }

    private Spot findWaitingSpot(List<Spot> spotList) {
        Spot spot = null;
        //There should be only one waiting spot, and it should always be at the first position of the list
        // (the list is ordered descending by datetime). But in case some bug has happened and the user
        // has a waiting spot at a different position, let's go through the list.
        for (Spot s : spotList) {
            if (s.getIsWaitingForARide() != null && s.getIsWaitingForARide()) {
                spot = s;
                break;
            }
        }
        return spot;
    }

    public void setWaitingSpot(Spot spot) {
        mWaitingSpot.setValue(spot);
    }

    public LiveData<Spot> getWaitingSpot() {
        return mWaitingSpot;
    }

    public Cursor rawQuery(Context context, String sql, String[] selectionArgs) {
        return mRepository.rawQuery(context, sql, selectionArgs);
    }

    public Spot getLastAddedRouteSpot(Context context) {
        return mRepository.getLastAddedRouteSpot(context);
    }

    public void execSQL(Context context, String sqlStatement) {
        //Get a DB session
        Database db = DaoMaster.newDevSession(context, Constants.INTERNAL_DB_FILE_NAME).getDatabase();

        //Delete selected spots from DB
        db.execSQL(sqlStatement);
    }
}
