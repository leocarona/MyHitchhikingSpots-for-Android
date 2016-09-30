package com.myhitchhikingspots;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class SpotListFragment extends Fragment {
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.spot_list_fragment_layout, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.main_activity_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        DaoSession daoSession = ((MyHitchhikingSpotsApplication) getActivity().getApplicationContext()).getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();
        List spotList = spotDao.queryBuilder().orderDesc().orderDesc(SpotDao.Properties.StartDateTime).list();
        recyclerView.setAdapter(new SpotListAdapter(spotList, this));
    }


}
