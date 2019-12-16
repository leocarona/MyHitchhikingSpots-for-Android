package com.myhitchhikingspots.utilities;

import android.location.Location;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.myhitchhikingspots.interfaces.FirstLocationUpdateListener;

import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * Listens to LocationEngine's updates.
 * If moveMapCameraToNextLocationReceived() is requested then
 * frag.moveCameraToLastKnownLocation() will be called regardless if the LocationEngine succeeds to fetch the current GPS location or not.
 * Class inspired by https://docs.mapbox.com/help/tutorials/android-location-listening/#enable-the-locationcomponent
 **/
public class LocationUpdatesCallback
        implements LocationEngineCallback<LocationEngineResult> {
    boolean isWaitingForNextLocation = false;
    int previousCameraMode = CameraMode.NONE;

    private final WeakReference<FirstLocationUpdateListener> fragmentWeakReference;

    public LocationUpdatesCallback(FirstLocationUpdateListener frag) {
        this.fragmentWeakReference = new WeakReference<>(frag);
    }

    /**
     * Makes map camera move to the next GPS location received,
     * and calls frag.moveCameraToLastKnownLocation() regardless if LocationEngine succeeds or not.
     **/
    public void moveMapCameraToNextLocationReceived() {
        FirstLocationUpdateListener frag = fragmentWeakReference.get();
        if (frag == null)
            return;

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

    /**
     * The LocationEngineCallback interface's method which fires when the device's location has changed.
     *
     * @param result the LocationEngineResult object which has the last known location within it.
     */
    @Override
    public void onSuccess(LocationEngineResult result) {
        FirstLocationUpdateListener frag = fragmentWeakReference.get();
        if (frag == null)
            return;

        Location location = result.getLastLocation();

        if (location == null) {
            return;
        }

        // Pass the new location to the Maps SDK's LocationComponent
        if (frag.getMapboxMap() != null && result.getLastLocation() != null) {
            LocationComponent locationComponent = frag.getMapboxMap().getLocationComponent();
            if (!locationComponent.isLocationComponentActivated())
                return;
            locationComponent.forceLocationUpdate(result.getLastLocation());

            if (isWaitingForNextLocation) {
                //Make map camera stop following location updates
                locationComponent.setCameraMode(previousCameraMode);
                //Reset values
                previousCameraMode = CameraMode.NONE;
                isWaitingForNextLocation = false;
                frag.moveCameraToLastKnownLocation();
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