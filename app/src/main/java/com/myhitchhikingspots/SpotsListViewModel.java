package com.myhitchhikingspots;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.myhitchhikingspots.interfaces.ISpotsRepository;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.persistence.FirebaseSpotsRepository;
import com.myhitchhikingspots.persistence.SQLiteSpotsRepository;

import java.util.List;

public class SpotsListViewModel extends AndroidViewModel {
    private MediatorLiveData<List<Spot>> mSpotsList;
    private MutableLiveData<Spot> mCurrentWaitingSpot;
    private ISpotsRepository mRepository;

    public SpotsListViewModel(Application context) {
        super(context);
        mSpotsList = new MediatorLiveData<>();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getUid() == null || mAuth.getUid().isEmpty())
            useSQLiteRepository();
        else
            useFirebaseRepository();

        mAuth.addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getUid() != null && !firebaseAuth.getUid().isEmpty()) {
                useFirebaseRepository();
            } else {
                //User has logged off.

                useSQLiteRepository();
            }
        });
    }

    void useSQLiteRepository() {
        if (mRepository instanceof SQLiteSpotsRepository)
            return;

        //Stop observing other repository
        if (mSpotsList != null && mRepository != null)
            mSpotsList.removeSource(mRepository.getSpots(getApplication()));

        //Set repository
        mRepository = ((MyHitchhikingSpotsApplication) getApplication()).getSQLiteSpotsRepository();

        //Start observing SQLiteRepository
        mSpotsList.addSource(mRepository.getSpots(getApplication()), this::setSpotListAndWaitingSpot);

        //Load list
        mRepository.loadSpots(getApplication());
    }

    void useFirebaseRepository() {
        if (mRepository instanceof FirebaseSpotsRepository)
            return;

        //Stop observing other repository
        if (mSpotsList != null && mRepository != null)
            mSpotsList.removeSource(mRepository.getSpots(getApplication()));

        //Set repository
        mRepository = ((MyHitchhikingSpotsApplication) getApplication()).getFirebaseSpotsRepository();

        //Start observing FirebaseRepository
        mSpotsList.addSource(mRepository.getSpots(getApplication()), this::setSpotListAndWaitingSpot);

        //Load list
        mRepository.loadSpots(getApplication());
    }

    void setSpotListAndWaitingSpot(List<Spot> newList) {
        mSpotsList.setValue(newList);
        updateWaitingSpot(newList);
    }

    void updateWaitingSpot(List<Spot> spots) {
        Spot waitingSpot = getWaitingSpot(spots);
        setCurrentWaitingSpot(waitingSpot);
    }

    public LiveData<List<Spot>> getSpots() {
        return mSpotsList;
    }

    public void reloadSpots() {
        mRepository.reloadSpots(getApplication());
    }

    private Spot getWaitingSpot(@NonNull List<Spot> spotList) {
        Spot spot = null;
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

    public Spot getLastAddedRouteSpot() {
        return mRepository.getLastAddedRouteSpot(getApplication());
    }

    public void deleteSpots(@NonNull List<String> spotsToBeDeleted_idList) {
        mRepository.deleteSpots(spotsToBeDeleted_idList, getApplication());

        /*
        //Because SQLiteRepo won't update the list of spots until we navigate back,
        //let's update mCurrentWaitingSpot here if needed.
        if (mRepository instanceof SQLiteSpotsRepository) {
            Spot mCurrentWaitingSpot = getCurrentWaitingSpot().getValue();
            boolean isWaitingForARide = mCurrentWaitingSpot != null &&
                    mCurrentWaitingSpot.getIsWaitingForARide() != null && mCurrentWaitingSpot.getIsWaitingForARide();
            if (isWaitingForARide && spotsToBeDeleted_idList.contains(mCurrentWaitingSpot.getSpotId()))
                setCurrentWaitingSpot(null);
        }*/
    }

    public boolean isAnySpotMissingAuthor() {
        return mRepository.isAnySpotMissingAuthor(getApplication());
    }

    /**
     * Assign the given username to all spots missing AuthorUserName.
     *
     * @param username The username that the person uses on Hitchwiki.
     */
    public void assignMissingAuthorTo(@NonNull String username) {
        mRepository.assignMissingAuthorTo(getApplication(), username);
    }
}
