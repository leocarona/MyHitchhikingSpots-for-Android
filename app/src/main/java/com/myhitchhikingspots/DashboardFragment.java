package com.myhitchhikingspots;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DashboardFragment extends Fragment implements MainActivity.OnMainActivityUpdated {

    MainActivity activity;
    List<Spot> spotList = new ArrayList();
    Spot mCurrentWaitingSpot = null;

    TextView txtNumSpotsSaved, txtNumHWSpotsDownloaded, txtShortestWaitingTime, txtLongestWaitingTime;
    SharedPreferences prefs;
    private boolean isHandlingRequestToOpenSpotForm = false;

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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = activity.getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        txtNumSpotsSaved = view.findViewById(R.id.txt_number_spots_saved);
        txtNumHWSpotsDownloaded = view.findViewById(R.id.txt_number_hw_spots_downloaded);
        txtShortestWaitingTime = view.findViewById(R.id.txt_shortest_waiting_time);
        txtLongestWaitingTime = view.findViewById(R.id.txt_longest_waiting_time);

        //If 'Go to my map' button is clicked, select My Map menu option
        view.findViewById(R.id.go_to_my_map).setOnClickListener(view1 -> activity.selectDrawerItem(R.id.nav_my_map));

        if (getArguments() != null) {
            if (getArguments().containsKey(MainActivity.ARG_SPOTLIST_KEY)) {
                Spot[] bundleSpotList = (Spot[]) getArguments().getSerializable(MainActivity.ARG_SPOTLIST_KEY);
                this.spotList = Arrays.asList(bundleSpotList);
            }

            if (getArguments().containsKey(MainActivity.ARG_CURRENTSPOT_KEY)) {
                this.mCurrentWaitingSpot = (Spot) getArguments().getSerializable(MainActivity.ARG_CURRENTSPOT_KEY);
            }

            updateUI();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Important: setHasOptionsMenu must be called so that onOptionsItemSelected works
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.dashboard_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_spot:
                if (!isHandlingRequestToOpenSpotForm)
                    saveSpotButtonHandler(false);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    boolean isWaitingForARide() {
        return (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null) ?
                mCurrentWaitingSpot.getIsWaitingForARide() : false;
    }

    public void saveSpotButtonHandler(boolean isDestination) {
        double cameraZoom = -1;
        Spot spot = null;
        int requestId = -1;
        if (!isWaitingForARide()) {
            requestId = Constants.SAVE_SPOT_REQUEST;
            spot = new Spot();
            spot.setIsHitchhikingSpot(!isDestination);
            spot.setIsDestination(isDestination);
            spot.setIsPartOfARoute(true);

        } else {
            requestId = Constants.EDIT_SPOT_REQUEST;
            spot = mCurrentWaitingSpot;
        }

        isHandlingRequestToOpenSpotForm = true;
        activity.startSpotFormActivityForResult(spot, cameraZoom, requestId, false, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        isHandlingRequestToOpenSpotForm = false;
    }

    @Override
    public void updateSpotList(List<Spot> spotList, Spot mCurrentWaitingSpot) {
        this.spotList = spotList;
        this.mCurrentWaitingSpot = mCurrentWaitingSpot;

        updateUI();
    }

    void updateUI() {
        Integer longestWaitingTime = 0, shortestWaitingTime = 0, numOfRides = 0;

        if (spotList.size() > 0) {
            Spot spot = spotList.get(0);
            longestWaitingTime = spot.getWaitingTime() == null ? 0 : spot.getWaitingTime();
            shortestWaitingTime = spot.getWaitingTime() == null ? 0 : spot.getWaitingTime();
        }

        for (Spot spot : spotList) {
            Boolean isDestination = spot.getIsDestination() == null ? false : spot.getIsDestination();
            Boolean isHitchhikingSpot = spot.getIsHitchhikingSpot() == null ? false : spot.getIsHitchhikingSpot();
            Boolean isGotARide = spot.getAttemptResult() != null && spot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE;

            if (isHitchhikingSpot) {
                if (isGotARide)
                    numOfRides++;

                if (!isDestination) {
                    Integer waitingTime = spot.getWaitingTime() == null ? 0 : spot.getWaitingTime();
                    if (waitingTime > longestWaitingTime)
                        longestWaitingTime = waitingTime;

                    //Only consider spots where the user has gotten rides
                    if (spot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE) {
                        if (waitingTime < shortestWaitingTime)
                            shortestWaitingTime = waitingTime;
                    }
                }
            }
        }

        String shortestWaitingTimeStr, longestWaitingTimeStr;
        shortestWaitingTimeStr = longestWaitingTimeStr = "- -";

        if (numOfRides > 2) {
            shortestWaitingTimeStr = Utils.getWaitingTimeAsString(shortestWaitingTime, activity.getBaseContext());
            longestWaitingTimeStr = Utils.getWaitingTimeAsString(longestWaitingTime, activity.getBaseContext());
        }

        Integer numHWSpotsDownloaded = prefs.getInt(Constants.PREFS_NUM_OF_HW_SPOTS_DOWNLOADED, 0);

        txtNumSpotsSaved.setText(String.format(getString(R.string.dashboard_number_of_rides), numOfRides));
        txtNumHWSpotsDownloaded.setText(String.format(getString(R.string.dashboard_number_of_hw_spots_downloaded), numHWSpotsDownloaded));
        txtShortestWaitingTime.setText(shortestWaitingTimeStr);
        txtLongestWaitingTime.setText(longestWaitingTimeStr);
    }
}