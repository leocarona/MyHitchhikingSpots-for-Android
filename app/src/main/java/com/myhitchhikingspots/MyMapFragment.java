package com.myhitchhikingspots;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class MyMapFragment extends Fragment {
    private MapView mapView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.my_map_fragment_layout, container, false);

        MapboxAccountManager.start(getContext(), "pk.eyJ1IjoibGVvY2Fyb25hIiwiYSI6ImNpd2V6Nm9sdjAwYmMyeW54OG5xeXp2MGoifQ.FO_pA1J1aWzH4kv2rcIDZw");
        mapView = (MapView) rootView.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {

                // Customize map with markers, polylines, etc.
            }
        });

/*
        recyclerView = (RecyclerView) rootView.findViewById(R.id.main_activity_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
*/
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
     /*   DaoSession daoSession = ((MyHitchhikingSpotsApplication) getActivity().getApplicationContext()).getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();
        List spotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();
        recyclerView.setAdapter(new SpotListAdapter(spotList, this));*/
    }


    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

}
