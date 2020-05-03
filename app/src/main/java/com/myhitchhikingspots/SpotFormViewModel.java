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

    public SpotFormViewModel() {
        mRepository = new SpotsRepository();
        mCurrentSpot = new MutableLiveData<>();
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

    public void insertOrReplace(Context context, Spot spot) {
        mRepository.insertOrReplace(context, spot);
    }

    public void deleteSpot(Context context, Spot spot) {
        mRepository.deleteSpot(context, spot);
    }
}
