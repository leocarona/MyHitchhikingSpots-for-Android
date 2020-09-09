package com.myhitchhikingspots;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.myhitchhikingspots.interfaces.ISpotsRepository;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.persistence.SQLiteSpotsRepository;

public class SpotFormViewModel extends AndroidViewModel {
    private MutableLiveData<Spot> mCurrentSpot;
    private ISpotsRepository mRepository;

    public SpotFormViewModel(Application context) {
        super(context);
        mCurrentSpot = new MutableLiveData<>();

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
        mRepository = ((MyHitchhikingSpotsApplication) getApplication()).getSQLiteSpotsRepository();
    }

    void useFirebaseRepository() {
        mRepository = ((MyHitchhikingSpotsApplication) getApplication()).getFirebaseSpotsRepository();
    }

    public LiveData<Spot> getCurrentSpot() {
        return mCurrentSpot;
    }

    public void setCurrentSpot(Spot spot) {
        if (spot == null)
            mCurrentSpot = new MutableLiveData<>();
        else
            mCurrentSpot.setValue(spot);
    }

    public Spot getLastAddedRouteSpot(Context context) {
        return mRepository.getLastAddedRouteSpot(context);
    }

    public void insertOrReplace(@Nullable Context context, @NonNull Spot spot) throws Exception {
        mRepository.insertOrReplace(context, spot);
    }

    public void deleteSpot(@Nullable Context context, @Nullable String userId, @NonNull Spot spot) {
        mRepository.deleteSpot(spot, context);
    }
}
