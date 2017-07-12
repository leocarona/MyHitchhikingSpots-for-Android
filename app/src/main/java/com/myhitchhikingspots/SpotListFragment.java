package com.myhitchhikingspots;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

public class SpotListFragment extends Fragment {
    private RecyclerView recyclerView;
    List<Spot> spotList = new ArrayList<>();

    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     * The user requests an address by pressing the Fetch Address button. This may happen
     * before GoogleApiClient connects. This activity uses this boolean to keep track of the
     * user's intent. If the value is true, the activity tries to fetch the address as soon as
     * GoogleApiClient connects.
     */
    protected boolean mAddressRequested;

    /**
     * The formatted location address.
     */
    protected Address mAddressOutput;

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private SpotFormActivity.AddressResultReceiver mResultReceiver;

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
        Crashlytics.log(Log.INFO, TAG, "onResume was called");
        updateUI();
    }

    protected static final String TAG = "spot-list-fragment";

    void updateUI() {
        Crashlytics.log(Log.INFO, TAG, "updateUI was called");
        try {
            if (recyclerView != null) {
                SpotListAdapter adapter = new SpotListAdapter(spotList, this);
                recyclerView.setAdapter(adapter);
            }
        } catch (Exception ex) {
            Crashlytics.logException(ex);
            ((TrackLocationBaseActivity) getActivity()).showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                    "Updating UI on fragment 2 - " + ex.getMessage()));
        }
    }

    public void setValues(List list) {
        Crashlytics.log(Log.INFO, TAG, "setValues was called");
        spotList = list;

        if (this.isResumed())
            updateUI();
    }

    /**
     * Runs when user clicks the Fetch Address button. Starts the service to fetch the address if
     * GoogleApiClient is connected.
     *
    public void fetchMissingAddressButtonHandler(View view) {

        Spot lastAddedSpot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getLastAddedRouteSpot();
        if (lastAddedSpot != null) {
            LatLng pinPosition = new LatLng(lastAddedSpot.getLatitude(), lastAddedSpot.getLongitude());

            // We only start the service to fetch the address if GoogleApiClient is connected.
            if (pinPosition == null)
                return;

            fetchAddress(new MyLocation(pinPosition.getLatitude(), pinPosition.getLongitude()));
        }else
            Toast.makeText(getContext(), "No address to be fetched.", Toast.LENGTH_LONG).show();
    }

    public void fetchAddress(MyLocation loc) {
        startIntentService(loc);

        // If GoogleApiClient isn't connected, we process the user's request by setting
        // mAddressRequested to true. Later, when GoogleApiClient connects, we launch the service to
        // fetch the address. As far as the user is concerned, pressing the Fetch Address button
        // immediately kicks off the process of getting the address.
        mAddressRequested = true;
        updateUIWidgets();
    }

    Intent fetchAddressServiceIntent;

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     *
    protected void startIntentService(MyLocation location) {
        // Create an intent for passing to the intent service responsible for fetching the address.
        fetchAddressServiceIntent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        fetchAddressServiceIntent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        fetchAddressServiceIntent.putExtra(Constants.LOCATION_DATA_EXTRA, location);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(fetchAddressServiceIntent);
    }

    protected void stopIntentService() {
        if (!mAddressRequested || fetchAddressServiceIntent == null)
            return;

        stopService(fetchAddressServiceIntent);
        mAddressRequested = false;
        //updateUIWidgets();
    }


    protected void displayAddressOutput() {
        if (mAddressOutput != null) {
            mCurrentSpot.setCity(mAddressOutput.getLocality());
            mCurrentSpot.setState(mAddressOutput.getAdminArea());
            mCurrentSpot.setCountry(mAddressOutput.getCountryName());
            mCurrentSpot.setCountryCode(mAddressOutput.getCountryCode());
            mCurrentSpot.setGpsResolved(true);
        } else {
            mCurrentSpot.setCity("");
            mCurrentSpot.setState("");
            mCurrentSpot.setCountry("");
            mCurrentSpot.setCountryCode("");
            mCurrentSpot.setGpsResolved(false);
        }

        mLocationAddressTextView.setText(getString(mCurrentSpot));

    }

    @NonNull
    private String getString(Spot mCurrentSpot) {
        String spotLoc = "";
        try {
            spotLoc = spotLocationToString(mCurrentSpot).trim();
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }
        return spotLoc;
    }*/
}
