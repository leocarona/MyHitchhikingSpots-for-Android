package com.myhitchhikingspots;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.persistence.SpotsRepository;

import java.util.ArrayList;
import java.util.List;

public class MyRoutesViewModel extends AndroidViewModel {
    private SpotsRepository mRepository;
    private MutableLiveData<List<Spot>> routeSpots;
    private MutableLiveData<List<Spot>> singleSpots;

    public MyRoutesViewModel(Application context) {
        super(context);
        mRepository = ((MyHitchhikingSpotsApplication) context).getSpotsRepository();

        routeSpots = new MutableLiveData<>();
        singleSpots = new MutableLiveData<>();
    }

    public void setRouteSpots(List<Spot> spots) {
        routeSpots.setValue(spots);
    }

    public void setSingleSpots(List<Spot> spots) {
        singleSpots.setValue(spots);
    }

    public void notifySpotListChanged(List<Spot> spotList) {
        List<Spot> newRouteSpots = new ArrayList<>();
        List<Spot> newSingleSpots = new ArrayList<>();
        for (Spot s : spotList) {
            if (s.getIsPartOfARoute() == null || !s.getIsPartOfARoute())
                newSingleSpots.add(s);
            else
                newRouteSpots.add(s);
        }

        routeSpots.setValue(newRouteSpots);
        singleSpots.setValue(newSingleSpots);
    }

    public LiveData<List<Spot>> getRouteSpots() {
        return routeSpots;
    }

    public LiveData<List<Spot>> getSingleSpots() {
        return singleSpots;
    }
}
