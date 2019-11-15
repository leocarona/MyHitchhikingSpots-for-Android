package com.myhitchhikingspots.utilities;

import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.myhitchhikingspots.interfaces.FirstLocationUpdateListener;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class LocationUpdatesCallback
        implements LocationEngineCallback<LocationEngineResult> {

    private final WeakReference<FirstLocationUpdateListener> fragmentWeakReference;

    public LocationUpdatesCallback(FirstLocationUpdateListener frag) {
        this.fragmentWeakReference = new WeakReference<>(frag);
    }

    boolean isWaitingForNextLocation = false;
    int previousCameraMode = CameraMode.NONE;

    public void moveMapCameraToNextLocationReceived() {
        FirstLocationUpdateListener frag = fragmentWeakReference.get();

        if (frag != null) {
            // Pass the new location to the Maps SDK's LocationComponent
            if (frag.getMapboxMap() != null) {
                LocationComponent lc = frag.getMapboxMap().getLocationComponent();
                if (!lc.isLocationComponentActivated())
                    return;

                previousCameraMode = lc.getCameraMode();
                isWaitingForNextLocation = true;

                //Make map camera move to next location received
                lc.setCameraMode(CameraMode.TRACKING_GPS_NORTH);
            }
        }
    }

    /**
     * The LocationEngineCallback interface's method which fires when the device's location has changed.
     *
     * @param result the LocationEngineResult object which has the last known location within it.
     */
    @Override
    public void onSuccess(LocationEngineResult result) {
        FirstLocationUpdateListener frag = fragmentWeakReference.get();

        if (frag != null) {
            Location location = result.getLastLocation();

            if (location == null) {
                return;
            }

            /*// Create a Toast which displays the new location's coordinates
            Toast.makeText(frag.getContext(), "String.format(activity.getString(R.string.new_location)," +
                            "String.valueOf(result.getLastLocation().getLatitude()), String.valueOf(result.getLastLocation().getLongitude()))",
                    Toast.LENGTH_SHORT).show();*/

            // Pass the new location to the Maps SDK's LocationComponent
            if (frag.getMapboxMap() != null && result.getLastLocation() != null) {
                frag.getMapboxMap().getLocationComponent().forceLocationUpdate(result.getLastLocation());

                if (isWaitingForNextLocation) {
                    //Make map camera stop following location updates
                    frag.getMapboxMap().getLocationComponent().setCameraMode(previousCameraMode);
                    //Reset values
                    previousCameraMode = CameraMode.NONE;
                    isWaitingForNextLocation = false;
                }
            }
        }
    }

    /**
     * The LocationEngineCallback interface's method which fires when the device's location can not be captured
     *
     * @param exception the exception message
     */
    @Override
    public void onFailure(@NonNull Exception exception) {
        Timber.d(exception.getLocalizedMessage());
        FirstLocationUpdateListener frag = fragmentWeakReference.get();
        if (frag != null) {
            Toast.makeText(frag.getContext(), exception.getLocalizedMessage(),
                    Toast.LENGTH_SHORT).show();
            frag.moveCameraToLastKnownLocation();
        }
    }
}