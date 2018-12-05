package com.myhitchhikingspots;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DashboardFragment extends android.support.v4.app.Fragment {

    MainActivity activity;
    List<Spot> spotList = new ArrayList();

    TextView txtNumSpotsSaved, txtNumHWSpotsDownloaded, txtShortestWaitingTime, txtLongestWaitingTime;
    SharedPreferences prefs;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            Spot[] bundleSpotList = (Spot[]) getArguments().getSerializable(MyMapsFragment.ARG_SPOTLIST_KEY);
            spotList = Arrays.asList(bundleSpotList);
        }

        prefs = activity.getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Set CompatVectorFromResourcesEnabled to true in order to be able to use ContextCompat.getDrawable
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        txtNumSpotsSaved = view.findViewById(R.id.txt_number_spots_saved);
        txtNumHWSpotsDownloaded = view.findViewById(R.id.txt_number_hw_spots_downloaded);
        txtShortestWaitingTime = view.findViewById(R.id.txt_shortest_waiting_time);
        txtLongestWaitingTime = view.findViewById(R.id.txt_longest_waiting_time);

        //If 'Go to my map' button is clicked, select My Map menu option
        view.findViewById(R.id.go_to_my_map).setOnClickListener(view1 -> activity.selectDrawerItem(R.id.nav_my_map));

        updateUI();

    }

    void updateUI() {
        Integer longestWaitingTime = 0, shortestWaitingTime = 0;

        if (spotList.size() > 0) {
            longestWaitingTime = spotList.get(0).getWaitingTime();
            shortestWaitingTime = spotList.get(0).getWaitingTime();
        }

        for (Spot spot : spotList) {
            Integer waitingTime = spot.getWaitingTime();
            if (waitingTime > longestWaitingTime)
                longestWaitingTime = waitingTime;

            //Only consider spots where the user has gotten rides
            if (spot.getIsHitchhikingSpot() && spot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE) {
                if (waitingTime < shortestWaitingTime)
                    shortestWaitingTime = waitingTime;
            }
        }

        Integer numHWSpotsDownloaded = prefs.getInt(Constants.PREFS_NUM_OF_HW_SPOTS_DOWNLOADED, 0);

        txtNumSpotsSaved.setText(String.format("%d spots", spotList.size()));
        txtNumHWSpotsDownloaded.setText(String.format("%d spots", numHWSpotsDownloaded));
        txtShortestWaitingTime.setText(Utils.getWaitingTimeAsString(shortestWaitingTime, activity.getBaseContext()));
        txtLongestWaitingTime.setText(Utils.getWaitingTimeAsString(longestWaitingTime, activity.getBaseContext()));
    }
}