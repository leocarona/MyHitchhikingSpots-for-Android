package com.myhitchhikingspots;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class SpotListFragment extends Fragment {
    private RecyclerView recyclerView;
    List<Spot> spotList = new ArrayList<>();

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
        Crashlytics.log(Log.INFO, "tracking-SpotListFrag", "onResume was called");
        updateUI();
    }

    protected static final String TAG = "spot-list-fragment";

    void updateUI() {
        Crashlytics.log(Log.INFO, "tracking-SpotListFrag", "updateUI was called");
        try {
            if (recyclerView != null) {
                SpotListAdapter adapter = new SpotListAdapter(spotList, this);
                recyclerView.setAdapter(adapter);
            }
        } catch (Exception ex) {
            Crashlytics.log(Log.ERROR, TAG, "Updating UI on fragment 2" + '\n' + Log.getStackTraceString(ex));
            ((TrackLocationBaseActivity) getActivity()).showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                    "Updating UI on fragment 2 - " + ex.getMessage()));
        }
    }

    public void setValues(List list) {
        Crashlytics.log(Log.INFO, "tracking-SpotListFrag", "setValues was called");
        spotList = list;

        if (this.isResumed())
            updateUI();
    }
}
