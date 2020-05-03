package com.myhitchhikingspots;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.persistence.SpotsRepository;

import org.greenrobot.greendao.database.Database;

import java.util.List;

public class SpotsListViewModel extends AndroidViewModel {
    private MutableLiveData<Spot> mCurrentWaitingSpot;
    private SpotsRepository mRepository;

    public SpotsListViewModel(Application context) {
        super(context);
        mRepository = ((MyHitchhikingSpotsApplication)context).getSpotsRepository();
    }

    public LiveData<List<Spot>> getSpots(Context context) {
        LiveData<List<Spot>> spotList = mRepository.getSpots(context);

        updateWaitingSpot(context);

        return spotList;
    }

    void updateWaitingSpot(Context context) {
        LiveData<List<Spot>> spots = mRepository.getSpots(context);
        if (mCurrentWaitingSpot == null && spots != null && spots.getValue() != null) {
            mCurrentWaitingSpot = new MutableLiveData<>();

            Spot waitingSpot = getWaitingSpot(spots.getValue());
            setCurrentWaitingSpot(waitingSpot);
        }
    }

    public void reloadSpots(Context context) {
        mRepository.loadSpots(context);
        updateWaitingSpot(context);
    }

    private Spot getWaitingSpot(List<Spot> spotList) {
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

    public void setCurrentWaitingSpot(Spot spot) {
        if (mCurrentWaitingSpot == null)
            mCurrentWaitingSpot = new MutableLiveData<>();
        mCurrentWaitingSpot.setValue(spot);
    }

    public LiveData<Spot> getCurrentWaitingSpot() {
        if (mCurrentWaitingSpot == null)
            mCurrentWaitingSpot = new MutableLiveData<>();
        return mCurrentWaitingSpot;
    }

    /**
     * TODO: We're keeping the old logic here, but in the future we can avaluate the possibility of
     * replacing this method for {@link #getSpots(Context)}
     * because they seem to do the same thing + we'd keep spotlist updated.
     **/
    public void loadWaitingSpot(Context context) {
        Spot waitingSpot = mRepository.getWaitingSpot(context);
        if (waitingSpot != null)
            setCurrentWaitingSpot(waitingSpot);
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

    public boolean isAnySpotMissingAuthor(Context context) {
        return mRepository.isAnySpotMissingAuthor(context);
    }

    /**
     * Assign the given username to all spots missing AuthorUserName.
     *
     * @param username The username that the person uses on Hitchwiki.
     */
    public void assignMissingAuthorTo(Context context, String username) {
        mRepository.assignMissingAuthorTo(context, username);
    }
}
