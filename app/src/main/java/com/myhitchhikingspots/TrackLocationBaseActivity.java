package com.myhitchhikingspots;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Date;


/**
 * Tracks user location using the help of Google Play services (GoogleApiClient)
 */
public class TrackLocationBaseActivity extends BaseActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected static final String TRACK_LOCATION_BASEACTIVITY_TAG = "track-location-base";


    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;


    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a Date.
     */
    protected Date mLastUpdateTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mLastUpdateTime = null;

        mRequestingLocationUpdates = true;

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices
        // API.
        buildGoogleApiClient();

        super.onCreate(savedInstanceState);
    }


    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Crashlytics.log(Log.INFO, TRACK_LOCATION_BASEACTIVITY_TAG, "Updating values from bundle");

        if (savedInstanceState != null) {
            try {
                // Update the value of mRequestingLocationUpdates from the Bundle.
                if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                    mRequestingLocationUpdates = savedInstanceState.getBoolean(
                            REQUESTING_LOCATION_UPDATES_KEY);
                }

                // Update the value of mCurrentLocation from the Bundle and update the UI to show the
                // correct latitude and longitude.
                if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                    // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                    // is not null.
                    mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
                }

                // Update the value of mLastUpdateTime from the Bundle and update the UI.
                if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                    mLastUpdateTime = (Date) savedInstanceState.getSerializable(LAST_UPDATED_TIME_STRING_KEY);
                }
            } catch (Exception ex) {
                Crashlytics.logException(ex);
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG);
            }
        }
    }


    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Crashlytics.log(Log.INFO, TRACK_LOCATION_BASEACTIVITY_TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */

    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        Crashlytics.log(Log.INFO, TRACK_LOCATION_BASEACTIVITY_TAG, mGoogleApiClient != null ? "mGoogleApiClient is NOT null" : "mGoogleApiClient is null");
        Crashlytics.log(Log.INFO, TRACK_LOCATION_BASEACTIVITY_TAG, mGoogleApiClient.isConnected() ? "mGoogleApiClient is connected" : "mGoogleApiClient is NOT connected");

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    boolean wasFirstLocationReceived = false;

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {

        mCurrentLocation = location;
        mLastUpdateTime = new Date();
        updateUILabels();

        if (!wasFirstLocationReceived) {
            wasFirstLocationReceived = true;
            updateUILocationSwitch();
            updateUISaveButtons();
        }

        Toast.makeText(this, getResources().getString(R.string.location_updated_message),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Crashlytics.log(Log.INFO, TRACK_LOCATION_BASEACTIVITY_TAG, "Connected to GoogleApiClient");

        updateUILocationSwitch();
        updateUISaveButtons();

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = new Date();
            updateUILabels();
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Crashlytics.log(Log.WARN, TRACK_LOCATION_BASEACTIVITY_TAG, "Connection suspended");
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.trying_to_reconnect_to_provider_of_location), Toast.LENGTH_LONG).show();
        updateUILocationSwitch();
        updateUISaveButtons();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Crashlytics.log(Log.ERROR, TRACK_LOCATION_BASEACTIVITY_TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.unable_to_find_location), Toast.LENGTH_LONG).show();
        updateUILocationSwitch();
        updateUISaveButtons();
    }

    protected void updateUILabels() {
    }

    protected void updateUILocationSwitch() {
    }

    protected void updateUISaveButtons() {
    }

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        try {
            savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
            savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
            savedInstanceState.putSerializable(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        } catch (Exception ex) {
            Crashlytics.logException(ex);
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG);
        }
        super.onSaveInstanceState(savedInstanceState);
    }

}
