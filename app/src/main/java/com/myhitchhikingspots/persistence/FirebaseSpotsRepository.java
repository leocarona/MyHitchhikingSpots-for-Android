package com.myhitchhikingspots.persistence;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.interfaces.IInsertOrReplaceEventListener;
import com.myhitchhikingspots.interfaces.ISpotsRepository;
import com.myhitchhikingspots.model.Spot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class FirebaseSpotsRepository implements ISpotsRepository {

    private MutableLiveData<List<Spot>> spots;
    boolean isSpotsListLoaded = false;
    private static FirebaseSpotsRepository sInstance;

    public static FirebaseSpotsRepository getInstance() {
        if (sInstance == null) {
            synchronized (FirebaseSpotsRepository.class) {
                if (sInstance == null) {
                    sInstance = new FirebaseSpotsRepository();
                }
            }
        }
        return sInstance;
    }

    String getUserId() {
        if (FirebaseAuth.getInstance().getUid() == null)
            throw new IllegalArgumentException("User not logged in");
        return FirebaseAuth.getInstance().getUid();
    }

    FirebaseSpotsRepository() {
        spots = new MutableLiveData<>();
        loadSpots(null);
    }

    public LiveData<List<Spot>> getSpots(@Nullable Context context) {
        if (!isSpotsListLoaded)
            loadSpots(context);
        return spots;
    }

    /* Loads the list of spots ordered from the most recent at the begining to the oldest. */
    public void loadSpots(@Nullable Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_SPOTS_PATH).child(getUserId());

        myRef.orderByChild("startDateTimeMillis").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Spot> lst = new ArrayList<>();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    try {
                        Spot spot = ds.getValue(Spot.class);
                        lst.add(spot);
                    } catch (Exception ex) {
                        Crashlytics.logException(ex);
                    }
                }
                //Let's reverse the list so that it is ordered descending
                Collections.reverse(lst);
                setSpots(lst);
                isSpotsListLoaded = true;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                setSpots(new ArrayList<>());
                isSpotsListLoaded = false;
            }
        });
    }

    public void reloadSpots(@Nullable Context context) {
        //We're already subscribed to changes to the spots list on Firebase Database,
        // so no need to reload it.
    }

    private void setSpots(List<Spot> spotList) {
        spots.setValue(spotList);
    }

    public Spot getWaitingSpot(@Nullable Context context) {
        Spot spot = null;
        //There should be only one waiting spot, and it should always be at the first position of the list
        // (the list is ordered descending by datetime). But in case some bug has happened and the user
        // has a waiting spot at a different position, let's go through the list.
        for (Spot s : spots.getValue()) {
            if (s.getIsWaitingForARide() != null && s.getIsWaitingForARide()) {
                spot = s;
                break;
            }
        }
        return spot;
    }

    public Spot getLastAddedRouteSpot(@Nullable Context context) {
        Spot res = null;
        for (Spot s : spots.getValue()) {
            if (!s.getIsPartOfARoute())
                continue;
            if (res == null) {
                res = s;
                continue;
            }
            if (firstIsMoreRecentThanSecond(s.getStartDateTimeMillis(), res.getStartDateTimeMillis()))
                res = s;
        }
        return res;
    }

    boolean firstIsMoreRecentThanSecond(Long first, Long second) {
        return (first != null && second != null && first > second);
    }

    public void insertOrReplace(@Nullable Context context, @NonNull Spot spot) throws Exception {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_SPOTS_PATH).child(getUserId());

        insertOrReplace(myRef, spot);
    }

    public void insertOrReplace(@Nullable Context context, @NonNull List<Spot> newSpots, boolean shouldGenerateNewIds, @NonNull final IInsertOrReplaceEventListener callback) throws Exception {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_SPOTS_PATH).child(getUserId());

        /*
        NOTE:
        If we just call spots.addAll(newSpots) here, it is possible that spots hasn't been loaded yet.
        To prevent that, we're retrieving the spots list from the database first.
        Also, note that we're assuming that the user has not edited their spots list on another device.
        Should we consider the possibility of the spots list be edited somewhere else and that
         we receive cached data here (instead of receiving the updated list), then this code here should be bigger and
         more complex.
        As of now, we opted to not prevent that possible scenario as the chances seem very small.
        */

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    HashMap<String, Spot> lst = new HashMap<>();
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        Spot spot = ds.getValue(Spot.class);
                        if (spot == null)
                            continue;
                        String spotId = getSpotIdOrNewRandomUUID(spot);
                        lst.put(spotId, spot);
                    }

                    for (Spot spot : newSpots) {
                        //Generate a new id for this spot
                        String spotId = "";
                        if (!shouldGenerateNewIds)
                            spotId = spot.getSpotId();
                        else {
                            spotId = myRef.push().getKey();
                            spot.setSpotId(spotId);
                            spot.setId(null);
                        }
                        lst.put(spotId, spot);
                    }

                    myRef.setValue(lst);

                    callback.onSuccess(lst.size());

                } catch (Exception ex) {
                    Crashlytics.logException(ex);
                    callback.onFailed(ex);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                setSpots(new ArrayList<>());
                isSpotsListLoaded = false;
                callback.onFailed(databaseError.toException());
            }
        });
    }

    String getSpotIdOrNewRandomUUID(Spot spot) {
        String spotId = spot.getSpotId();
        if (spotId == null && spot.getId() != null)
            spotId = spot.getId().toString();
        if (spotId == null || spotId.isEmpty())
            spotId = UUID.randomUUID().toString();
        return spotId;
    }


    private void insertOrReplace(DatabaseReference myRef, @NonNull Spot spot) throws Exception {
        String spotId = null;

        if (spot.getSpotId() == null || spot.getSpotId().isEmpty())
            spotId = myRef.push().getKey();
        else
            spotId = spot.getSpotId();

        if (spotId == null)
            throw new Exception("Something went wrong and a id couldn't be generated for this spot. The spot hasn't been saved.");

        spot.setSpotId(spotId);

        myRef.child(spotId).setValue(spot);
    }

    public void deleteSpots(@NonNull List<String> spotsToBeDeleted_idList, @Nullable Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_SPOTS_PATH).child(getUserId());

        for (String spotId : spotsToBeDeleted_idList)
            myRef.child(spotId).removeValue();
    }

    public void deleteAllSpots(@Nullable Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_SPOTS_PATH).child(getUserId());

        myRef.removeValue();
    }

    public void deleteSpot(@NonNull Spot spot, @Nullable Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_SPOTS_PATH).child(getUserId());

        myRef.child(spot.getSpotId()).removeValue();
    }

    public boolean isAnySpotMissingAuthor(@Nullable Context context) {
        boolean res = false;
        for (Spot s : spots.getValue()) {
            if (s.getAuthorUserName() == null || s.getAuthorUserName().isEmpty()) {
                res = true;
                break;
            }
        }
        return res;
    }

    /**
     * Assign the given username to all spots missing AuthorUserName.
     *
     * @param username The username that the person uses on Hitchwiki.
     */
    public void assignMissingAuthorTo(@Nullable Context context, @Nullable String userId, @NonNull String username) {
        throw new java.lang.UnsupportedOperationException("Not supported yet.");

        //Query all spots that belong to the current user which don't have a username set. Then update them on the database.

        /*FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Constants.FIREBASE_DATABASE_SPOTS_PATH).child(userId);

        myRef.setValue(spots);*/
    }
}
