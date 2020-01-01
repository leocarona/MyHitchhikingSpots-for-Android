package com.myhitchhikingspots;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.persistence.SpotsRepository;

public class SpotFormViewModel extends ViewModel {
    private MutableLiveData<Spot> mCurrentSpot;
    private SpotsRepository mRepository;
    private MutableLiveData<SpotFormActivity.FormType> mFormType;

    public SpotFormViewModel() {
        mRepository = new SpotsRepository();
        mCurrentSpot = new MutableLiveData<>();
        mFormType = new MutableLiveData<>();
    }

    public LiveData<Spot> getCurrentSpot() {
        return mCurrentSpot;
    }

    public LiveData<SpotFormActivity.FormType> getFormType() {
        return mFormType;
    }

    public void setCurrentSpot(Spot spot, boolean shouldRetrieveDetailsFromHW) {
        mCurrentSpot.setValue(spot);
        mFormType.setValue(getFormType(spot, shouldRetrieveDetailsFromHW));
    }

    private SpotFormActivity.FormType getFormType(Spot spot, boolean shouldRetrieveDetailsFromHW) {
        SpotFormActivity.FormType mFormType2 = SpotFormActivity.FormType.Unknown;
        // If user is currently waiting for a ride at the current spot, show him the Evaluate form. If he is not,
        // that means he's saving a new spot so we need to show him the Create form instead.
        if (spot != null) {
            if (shouldRetrieveDetailsFromHW)
                mFormType2 = SpotFormActivity.FormType.HitchwikiSpot;
            else {
                // If Id greater than zero, this means the user is editing a spot that was already saved in the database. So show full form.
                if (spot.getId() != null && spot.getId() > 0) {
                    if (spot.getIsWaitingForARide() != null ? spot.getIsWaitingForARide() : false)
                        mFormType2 = SpotFormActivity.FormType.Evaluate;
                    else
                        mFormType2 = SpotFormActivity.FormType.Edit;
                } else
                    mFormType2 = SpotFormActivity.FormType.Create;
            }
        }
        return mFormType2;
    }

    public Spot getLastAddedRouteSpot(Context context) {
        return mRepository.getLastAddedRouteSpot(context);
    }

    public void insertOrReplace(Context context, Spot spot) {
        mRepository.insertOrReplace(context, spot);
    }

    public void deleteSpot(Context context, Spot spot) {
        mRepository.deleteSpot(context, spot);
    }

}
