package com.myhitchhikingspots;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.SpotsListHelper;
import com.myhitchhikingspots.utilities.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment implements MainActivity.OnMainActivityUpdated {
    TextView txtNumSpotsSaved, txtNumHWSpotsDownloaded, txtShortestWaitingTime, txtLongestWaitingTime,
            txtShortestWaitingTimesAreBetween, txtBestHoursToHitchhike;
    SharedPreferences prefs;
    private boolean isHandlingRequestToOpenSpotForm = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null)
            return;

        prefs = activity.getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        txtNumSpotsSaved = view.findViewById(R.id.txt_number_spots_saved);
        txtNumHWSpotsDownloaded = view.findViewById(R.id.txt_number_hw_spots_downloaded);
        txtShortestWaitingTime = view.findViewById(R.id.txt_shortest_waiting_time);
        txtLongestWaitingTime = view.findViewById(R.id.txt_longest_waiting_time);
        txtShortestWaitingTimesAreBetween = view.findViewById(R.id.txt_1);
        txtBestHoursToHitchhike = view.findViewById(R.id.txt_2);

        //If 'Go to my map' button is clicked, select My Map menu option
        Button seeMyMapsBtn = view.findViewById(R.id.go_to_my_map);
        seeMyMapsBtn.setText(getString(R.string.action_button_label, getString(R.string.menu_my_maps)));
        seeMyMapsBtn.setOnClickListener(view1 -> activity.selectDrawerItem(R.id.nav_my_map));

        updateUI();
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

    private List<Spot> getSpotList() {
        Activity activity = getActivity();

        if (activity instanceof MainActivity)
            return ((MainActivity) activity).spotList;

        return new ArrayList<>();
    }

    @Nullable
    private Spot getCurrentWaitingSpot() {
        Activity activity = getActivity();

        if (activity instanceof MainActivity)
            return ((MainActivity) activity).mCurrentWaitingSpot;

        return null;
    }

    private boolean isWaitingForARide() {
        Spot mCurrentWaitingSpot = getCurrentWaitingSpot();
        return (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null) ?
                mCurrentWaitingSpot.getIsWaitingForARide() : false;
    }

    public void saveSpotButtonHandler(boolean isDestination) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null)
            return;
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
            spot = getCurrentWaitingSpot();
        }

        isHandlingRequestToOpenSpotForm = true;
        activity.startSpotFormActivityForResult(spot, cameraZoom, requestId, false, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        isHandlingRequestToOpenSpotForm = false;

        //NOTE: updateSpotList() will be called after DashboardFragment.onActivityResult() by MainActivity.onActivityResult()
    }

    @Override
    public void onSpotListChanged() {
        updateUI();
    }

    private void updateUI() {
        try {
            calculateStats();
        } catch (Exception ex) {
            Crashlytics.logException(ex);
            displayValues("", "", "", "", "", "");
        }
    }

    private void calculateStats() {
        Context context = getContext();
        if (context == null)
            return;

        Crashlytics.log("Calculating stats..");

        List<Spot> spotList = getSpotList();
        Crashlytics.setInt("spotList size", spotList.size());

        Integer longestWaitingTime = SpotsListHelper.getLongestWaitingTime(spotList),
                shortestWaitingTime = SpotsListHelper.getShortestWaitingTime(spotList),
                numOfRides = SpotsListHelper.getNumberOfRidesGotten(spotList),
                numHWSpotsDownloaded = prefs.getInt(Constants.PREFS_NUM_OF_HW_SPOTS_DOWNLOADED, 0);
        List<Pair<Integer, Integer>> numberOfOccurrences = SpotsListHelper.getNumberOfOccurrences(spotList);
        List<Pair<Pair<Integer, Integer>, Integer>> waitingTimeOccurrences = SpotsListHelper.getWaitingTimeOccurrences(spotList);

        String shortestWaitingTimeStr, longestWaitingTimeStr;
        shortestWaitingTimeStr = longestWaitingTimeStr = "- -";

        if (numOfRides > 2) {
            shortestWaitingTimeStr = Utils.getWaitingTimeAsString(shortestWaitingTime, context);
            longestWaitingTimeStr = Utils.getWaitingTimeAsString(longestWaitingTime, context);
        }

        String waitingTimeOccurrencesStr = getWaitingTimeOccurrencesStr(context, waitingTimeOccurrences, numOfRides);
        String txtBestHoursToHitchhikeStr = getBestHoursToHitchhikeStr(numberOfOccurrences, numOfRides);

        displayValues(numOfRides, numHWSpotsDownloaded, shortestWaitingTimeStr, longestWaitingTimeStr, waitingTimeOccurrencesStr, txtBestHoursToHitchhikeStr);
    }

    private void displayValues(Integer numOfRides, Integer numHWSpotsDownloaded, String shortestWaitingTimeStr, String longestWaitingTimeStr, String waitingTimeOccurrencesStr, String txtBestHoursToHitchhikeStr) {
        String numOfRidesStr = String.format(getString(R.string.dashboard_number_of_rides), numOfRides);
        String numHWSpotsDownloadedStr = String.format(getString(R.string.dashboard_number_of_hw_spots_downloaded), numHWSpotsDownloaded);

        displayValues(numOfRidesStr, shortestWaitingTimeStr, longestWaitingTimeStr, waitingTimeOccurrencesStr, txtBestHoursToHitchhikeStr, numHWSpotsDownloadedStr);
    }

    private void displayValues(String numOfRidesStr, String shortestWaitingTimeStr, String longestWaitingTimeStr, String waitingTimeOccurrencesStr, String txtBestHoursToHitchhikeStr, String numHWSpotsDownloadedStr) {
        txtNumSpotsSaved.setText(numOfRidesStr);
        txtNumHWSpotsDownloaded.setText(numHWSpotsDownloadedStr);
        txtShortestWaitingTime.setText(shortestWaitingTimeStr);
        txtLongestWaitingTime.setText(longestWaitingTimeStr);
        txtShortestWaitingTimesAreBetween.setText(waitingTimeOccurrencesStr);
        txtBestHoursToHitchhike.setText(txtBestHoursToHitchhikeStr);
    }

    private static String getWaitingTimeOccurrencesStr(Context context, List<Pair<Pair<Integer, Integer>, Integer>> waitingTimeOccurences, int numOfRides) {
        StringBuilder waitingTimeOccurrencesStr = new StringBuilder();
        for (int i = 0; i < waitingTimeOccurences.size(); i++) {
            Pair<Pair<Integer, Integer>, Integer> occurrence = waitingTimeOccurences.get(i);
            if (occurrence != null) {
                Pair<Integer, Integer> periodOfTime = occurrence.first;
                int numberOfOccurrences = occurrence.second == null ? 0 : occurrence.second;
                if (numberOfOccurrences > 0 && periodOfTime != null) {
                    String numberOfOccurrencesPercent = String.valueOf(numberOfOccurrences * 100 / numOfRides);
                    String periodBegins = Utils.getWaitingTimeAsString(periodOfTime.first == null ? 0 : periodOfTime.first, context),
                            periodEnds = Utils.getWaitingTimeAsString(periodOfTime.second == null ? 0 : periodOfTime.second, context),
                            formattedStr = "";

                    if (numberOfOccurrencesPercent.equals("0"))
                        numberOfOccurrencesPercent = "<1";

                    if (periodBegins.equals(context.getString(R.string.general_seconds_label)))
                        formattedStr = String.format(Locale.US,
                                "(%2$s%%) <%1$s", periodEnds.replace(" ", ""), numberOfOccurrencesPercent);
                    else
                        formattedStr = String.format(Locale.US,
                                "(%3$s%%) %1$s-%2$s", periodBegins.replace(" ", ""), periodEnds.replace(" ", ""), numberOfOccurrencesPercent);

                    waitingTimeOccurrencesStr.append(formattedStr);
                    waitingTimeOccurrencesStr.append("\n");
                }
            }
        }
        return waitingTimeOccurrencesStr.toString();
    }

    private static String getBestHoursToHitchhikeStr(List<Pair<Integer, Integer>> numberOfOccurrences, int numOfRides) {
        StringBuilder txtBestHoursToHitchhikeStr = new StringBuilder();
        for (int i = 0; i < numberOfOccurrences.size(); i++) {
            Pair<Integer, Integer> occurrence = numberOfOccurrences.get(i);
            if (occurrence != null) {
                int hour = occurrence.first == null ? 0 : occurrence.first;
                int numberOfRidesHitched = occurrence.second == null ? 0 : occurrence.second;
                if (numberOfRidesHitched > 0) {
                    String numberOfRidesHitchedPercent = String.valueOf(numberOfRidesHitched * 100 / numOfRides);

                    if (numberOfRidesHitchedPercent.equals("0"))
                        numberOfRidesHitchedPercent = "<1";

                    String hourSuffix = "pm";
                    if (hour < 12)
                        hourSuffix = "am";

                    txtBestHoursToHitchhikeStr.append(String.format(Locale.US, "(%4$s%%) %1$02d-%2$02d%3$s",
                            hour, hour + 1, hourSuffix, numberOfRidesHitchedPercent));
                    txtBestHoursToHitchhikeStr.append("\n");
                }
            }
        }
        return txtBestHoursToHitchhikeStr.toString();
    }
}