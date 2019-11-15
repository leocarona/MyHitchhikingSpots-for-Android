package com.myhitchhikingspots;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin;
import com.myhitchhikingspots.interfaces.FirstLocationUpdateListener;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.LocationUpdatesCallback;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.os.Looper.getMainLooper;

/**
 * Download, view, navigate to, and delete an offline region.
 */
public class OfflineMapManagerFragment extends Fragment implements
        OnMapReadyCallback, PermissionsListener, MainActivity.OnMainActivityUpdated, FirstLocationUpdateListener {

    // JSON encoding/decoding
    public static final String JSON_CHARSET = "UTF-8";
    public static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    // UI elements
    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style style;
    private ProgressBar progressBar;
    private BottomNavigationView menu_bottom;
    private FloatingActionButton fabLocateUser, fabZoomIn, fabZoomOut;
    private Button downloadButton;

    private boolean isInProgress = false;
    private int regionSelected;
    private static final int PERMISSIONS_LOCATION = 0;

    // Offline objects
    private OfflineManager offlineManager;
    private OfflineRegion offlineRegion;
    SharedPreferences prefs;

    // Variables needed to add the location engine
    private LocationEngine locationEngine;
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    // Variables needed to listen to location updates
    private LocationUpdatesCallback callback = new LocationUpdatesCallback(this);

    private PermissionsManager locationPermissionsManager;

    Toast waiting_GPS_update;

    Toast stillInProgressToast;

    private MainActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_offline_map_manager, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Set CompatVectorFromResourcesEnabled to true in order to be able to use ContextCompat.getDrawable
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        prefs = activity.getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        menu_bottom = (BottomNavigationView) view.findViewById(R.id.bottom_navigation);

        downloadButton = (Button) view.findViewById(R.id.download_button);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInProgress) {
                    //Show a toast to indicate that the download is still in progress
                    showStillInProgressToast();
                } else {
                    // Download offline button
                    downloadRegionDialog();
                }
            }
        });

        menu_bottom.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    /*case R.id.action_view_map:

                        break;*/
                    case R.id.action_view_list:
                        if (isInProgress) {
                            //Show message to let the user know that this tab will be enabled when the progress finish
                            Toast.makeText(activity, "Still in progress, please wait",
                                    Toast.LENGTH_SHORT).show();

                            return false;
                        }

                        // List offline regions
                        downloadedRegionList();
                        break;
                }
                return true;
            }
        });

        // Set up the MapView
        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        fabLocateUser = (FloatingActionButton) view.findViewById(R.id.fab_locate_user);
        fabLocateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInProgress) {
                    //Show a toast to indicate that the download is still in progress
                    showStillInProgressToast();
                } else {
                    if (mapboxMap != null && style != null && style.isFullyLoaded()) {
                        callback.moveMapCameraToNextLocationReceived();
                    }
                }
            }
        });

        fabZoomIn = (FloatingActionButton) view.findViewById(R.id.fab_zoom_in);
        fabZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInProgress) {
                    //Show a toast to indicate that the download is still in progress
                    showStillInProgressToast();
                } else {
                    if (mapboxMap != null)
                        mapboxMap.moveCamera(CameraUpdateFactory.zoomIn());
                }
            }
        });

        fabZoomOut = (FloatingActionButton) view.findViewById(R.id.fab_zoom_out);
        fabZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInProgress) {
                    //Show a toast to indicate that the download is still in progress
                    showStillInProgressToast();
                } else {
                    if (mapboxMap != null)
                        mapboxMap.moveCamera(CameraUpdateFactory.zoomOut());
                }
            }
        });

        // Assign progressBar for later use
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        // Set up the offlineManager
        offlineManager = OfflineManager.getInstance(activity);
    }

    @Override
    public void updateSpotList(List<Spot> spotList, Spot mCurrentWaitingSpot) {
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(activity, getString(R.string.spot_form_user_location_permission_not_granted), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        //As we request at least two different permissions (location and storage) for the users,
        //instead of handling the results for location permission here alone, we've opted to handle it within onRequestPermissionsResult.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationLayer(style);
                callback.moveMapCameraToNextLocationReceived();
            } else {
                Toast.makeText(activity, getString(R.string.spot_form_user_location_permission_not_granted), Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationLayer(@NonNull Style loadedMapStyle) {
        if (mapboxMap == null)
            return;

        //Setup location plugin to display the user location on a map.
        // NOTE: map camera won't follow location updates by default here.
        setupLocationComponent(loadedMapStyle);

        LocationComponent locationComponent = mapboxMap.getLocationComponent();

        // Enable the location layer on the map
        if (locationComponent.isLocationComponentActivated() && !locationComponent.isLocationComponentEnabled())
            locationComponent.setLocationComponentEnabled(true);
    }


    @SuppressWarnings({"MissingPermission"})
    private void setupLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(activity, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            //Map camera should stop following gps updates
            locationComponent.setCameraMode(CameraMode.NONE);

            //Stop showing an arrow considering the compass of the device.
            //locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();
        } else {
            if (locationPermissionsManager == null)
                locationPermissionsManager = new PermissionsManager(this);
            locationPermissionsManager.requestLocationPermissions(activity);
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(activity);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    @Override
    public MapboxMap getMapboxMap() {
        return mapboxMap;
    }

    @Override
    public void onMapReady(final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        prefs.edit().putBoolean(Constants.PREFS_MAPBOX_WAS_EVER_LOADED, true).apply();

        this.mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments.
            OfflineMapManagerFragment.this.style = style;

            if (style.isFullyLoaded()) {
                LocalizationPlugin localizationPlugin = new LocalizationPlugin(mapView, mapboxMap, style);

                try {
                    localizationPlugin.matchMapLanguageWithDeviceDefault();
                } catch (RuntimeException exception) {
                    Crashlytics.logException(exception);
                }

                callback.moveMapCameraToNextLocationReceived();
            }
        });
    }

    // Override Activity lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
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
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    /**
     * Move the map camera to the given position with zoom Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT
     *
     * @param latLng Target location to change to
     */

    private void moveCamera(LatLng latLng) {
        moveCamera(latLng, Constants.KEEP_ZOOM_LEVEL);
    }

    /**
     * Move the map camera to the given position
     *
     * @param latLng Target location to change to
     * @param zoom   Zoom level to change to
     */
    private void moveCamera(LatLng latLng, long zoom) {
        if (latLng != null) {
            if (mapboxMap == null)
                Crashlytics.log("For some reason map was not loaded, therefore mapboxMap.moveCamera() was skipped to avoid crash. Shouldn't the map be loaded at this point?");
            else {
                if (zoom == Constants.KEEP_ZOOM_LEVEL)
                    mapboxMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                else
                    mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
            }
        }
    }

    private Location tryGetLastKnownLocation() {
        if (mapboxMap == null)
            return null;

        LocationComponent locationComponent = mapboxMap.getLocationComponent();
        Location loc = null;
        try {
            //Make sure location component has been activated, otherwise using any of its methods will throw an exception.
            if (locationComponent.isLocationComponentActivated())
                loc = locationComponent.getLastKnownLocation();
        } catch (SecurityException ex) {
        }
        return loc;
    }

    @SuppressWarnings({"MissingPermission"})
    @Override
    public void moveCameraToLastKnownLocation() {
        //Request permission of access to GPS updates or
        // directly initialize and enable the location plugin if such permission was already granted.
        enableLocationLayer(style);

        LatLng moveCameraPositionTo = null;

        //If we know the current position of the user, move the map camera to there
        Location lastLoc = tryGetLastKnownLocation();
        if (lastLoc != null)
            moveCameraPositionTo = new LatLng(lastLoc);

        if (moveCameraPositionTo != null) {
            moveCameraPositionTo = new LatLng(moveCameraPositionTo);
        } else {
            //The user might still be close to the last spot saved, move the map camera there
            Spot lastAddedSpot = ((MyHitchhikingSpotsApplication) activity.getApplicationContext()).getLastAddedRouteSpot();
            if (lastAddedSpot != null && lastAddedSpot.getLatitude() != null && lastAddedSpot.getLongitude() != null
                    && lastAddedSpot.getLatitude() != 0.0 && lastAddedSpot.getLongitude() != 0.0) {
                moveCameraPositionTo = new LatLng(lastAddedSpot.getLatitude(), lastAddedSpot.getLongitude());
            }
        }

        int zoomLevel = Constants.KEEP_ZOOM_LEVEL;

        //If current zoom level is default (world level)
        if (mapboxMap.getCameraPosition().zoom == mapboxMap.getMinZoomLevel())
            zoomLevel = Constants.ZOOM_TO_SEE_FARTHER_DISTANCE;

        if (moveCameraPositionTo != null)
            moveCamera(moveCameraPositionTo, zoomLevel);
    }

    private void downloadRegionDialog() {
        // Set up download interaction. Display a dialog
        // when the user clicks download button and require
        // a user-provided region name
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final EditText regionNameEdit = new EditText(activity);
        regionNameEdit.setHint(getString(R.string.set_region_name_hint));

        // Build the dialog box
        builder.setTitle(getString(R.string.dialog_title))
                .setView(regionNameEdit)
                .setMessage(getString(R.string.dialog_message))
                .setPositiveButton(getString(R.string.dialog_positive_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String regionName = regionNameEdit.getText().toString();
                        // Require a region name to begin the download.
                        // If the user-provided string is empty, display
                        // a toast message and do not begin download.
                        if (regionName.length() == 0) {
                            Toast.makeText(activity, getString(R.string.dialog_toast), Toast.LENGTH_SHORT).show();
                        } else {
                            // Begin download process
                            downloadRegion(regionName);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.dialog_negative_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // Display the dialog
        builder.show();
    }

    private void downloadRegion(final String regionName) {
        // Define offline region parameters, including bounds,
        // min/max zoom, and metadata

        //Create a record to track usage of Download Map Region
        Answers.getInstance().logCustom(new CustomEvent("Offline Map download click"));

        // Start the progressBar
        startProgress();

        // Create offline definition using the current
        // style and boundaries of visible map area
        String styleUrl = mapboxMap.getStyle().getUrl();
        LatLngBounds bounds = mapboxMap.getProjection().getVisibleRegion().latLngBounds;
        double minZoom = mapboxMap.getCameraPosition().zoom;
        double maxZoom = mapboxMap.getMaxZoomLevel();
        float pixelRatio = this.getResources().getDisplayMetrics().density;
        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                styleUrl, bounds, minZoom, maxZoom, pixelRatio);

        // Build a JSONObject using the user-defined offline region title,
        // convert it into string, and use it to create a metadata variable.
        // The metadata variable will later be passed to createOfflineRegion()
        byte[] metadata;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_FIELD_REGION_NAME, regionName);
            String json = jsonObject.toString();
            metadata = json.getBytes(JSON_CHARSET);
        } catch (Exception exception) {
            Crashlytics.log("Failed to encode metadata.");
            Crashlytics.logException(exception);
            metadata = null;
        }

        // Create the offline region and launch the download
        offlineManager.createOfflineRegion(definition, metadata, new OfflineManager.CreateOfflineRegionCallback() {
            @Override
            public void onCreate(OfflineRegion offlineRegion) {
                Crashlytics.log("Offline region created: " + regionName);
                OfflineMapManagerFragment.this.offlineRegion = offlineRegion;
                launchDownload();
            }

            @Override
            public void onError(String error) {
                Crashlytics.logException(new Exception("Error: " + error));
            }
        });
    }

    private void launchDownload() {
        // Set up an observer to handle download progress and
        // notify the user when the region is finished downloading
        offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
            @Override
            public void onStatusChanged(OfflineRegionStatus status) {
                try {
                    // Compute a percentage
                    double percentage = status.getRequiredResourceCount() >= 0
                            ? (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                            0.0;

                    if (status.isComplete()) {
                        // Download complete
                        endProgress(R.string.end_progress_success);
                        return;
                    } else if (status.isRequiredResourceCountPrecise()) {
                        // Switch to determinate state
                        setPercentage((int) Math.round(percentage));
                    }

                    // Log what is being currently downloaded
                    Crashlytics.log(String.format("%s%% of download is complete; %s bytes downloaded.",
                            String.valueOf(status.getCompletedResourceCount() / status.getRequiredResourceCount()),
                            String.valueOf(status.getCompletedResourceSize())));
                } catch (Exception ex) {
                    Crashlytics.logException(ex);
                    Toast.makeText(activity, "An exception occurred, but we'll keep trying", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(OfflineRegionError error) {
                String errMsg = "";
                if (error.getMessage().equals("timeout"))
                    errMsg = "Slow connection, but we'll keep trying";
                else
                    errMsg = error.getMessage() + ", but we'll keep trying";

                Toast.makeText(activity, errMsg, Toast.LENGTH_LONG).show();

                Crashlytics.log("onError message: " + error.getMessage());
            }

            /*
             * Implement this method to be notified when the limit on the number of Mapbox
             * tiles stored for offline regions has been reached.
             *
             * Once the limit has been reached, the SDK will not download further offline
             * tiles from Mapbox APIs until existing tiles have been removed. Contact your
             * Mapbox sales representative to raise the limit.
             *
             * This limit does not apply to non-Mapbox tile sources.
             *
             * This method will be executed on the main thread.
             */
            @Override
            public void mapboxTileCountLimitExceeded(long limit) {
                Crashlytics.setLong("limit", limit);
                endProgress(R.string.tile_count_limit_exceed_error_message);
                Crashlytics.logException(new Exception("Mapbox tile count limit exceeded."));
            }
        });

        // Change the region state
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
    }

    private void downloadedRegionList() {
        // Build a region list when the user clicks the list button

        // Reset the region selected int to 0
        regionSelected = 0;

        // Query the DB asynchronously
        offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
            @Override
            public void onList(final OfflineRegion[] offlineRegions) {
                // Check result. If no regions have been
                // downloaded yet, notify user and return
                if (offlineRegions == null || offlineRegions.length == 0) {
                    Toast.makeText(activity, getString(R.string.toast_no_regions_yet), Toast.LENGTH_SHORT).show();

                    //Select map tab
                    menu_bottom.setSelectedItemId(R.id.action_view_map);
                    return;
                }

                // Add all of the region names to a list
                ArrayList<String> offlineRegionsNames = new ArrayList<>();
                for (OfflineRegion offlineRegion : offlineRegions) {
                    offlineRegionsNames.add(getRegionName(offlineRegion));
                }
                final CharSequence[] items = offlineRegionsNames.toArray(new CharSequence[offlineRegionsNames.size()]);

                // Build a dialog containing the list of regions
                AlertDialog dialog = new AlertDialog.Builder(activity)
                        .setTitle(getString(R.string.navigate_title))
                        .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Track which region the user selects
                                regionSelected = which;
                            }
                        })
                        .setPositiveButton(getString(R.string.navigate_positive_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {

                                Toast.makeText(activity, items[regionSelected], Toast.LENGTH_LONG).show();

                                // Get the region bounds and zoom
                                LatLngBounds bounds = ((OfflineTilePyramidRegionDefinition)
                                        offlineRegions[regionSelected].getDefinition()).getBounds();
                                double regionZoom = ((OfflineTilePyramidRegionDefinition)
                                        offlineRegions[regionSelected].getDefinition()).getMinZoom();

                                // Create new camera position
                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                        .target(bounds.getCenter())
                                        .zoom(regionZoom)
                                        .build();

                                // Move camera to new position
                                mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                //Select map tab
                                menu_bottom.setSelectedItemId(R.id.action_view_map);
                            }
                        })
                        .setNeutralButton(getString(R.string.navigate_neutral_button_title), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Start the progressBar
                                startProgress();

                                // Begin the deletion process
                                offlineRegions[regionSelected].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
                                    @Override
                                    public void onDelete() {
                                        // Once the region is deleted, remove the
                                        // progressBar and display a toast
                                        endProgress(R.string.toast_region_deleted);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        endProgress(R.string.general_error_dialog_title);
                                        Crashlytics.logException(new Exception("Error: " + error));
                                    }
                                });

                                //Select map tab
                                menu_bottom.setSelectedItemId(R.id.action_view_map);
                            }
                        })
                        .setNegativeButton(getString(R.string.navigate_negative_button_title), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // When the user cancels, don't do anything.
                                // The dialog will automatically close

                                //Select map tab
                                menu_bottom.setSelectedItemId(R.id.action_view_map);
                            }
                        }).create();
                dialog.show();

            }

            @Override
            public void onError(String error) {
                Crashlytics.logException(new Exception("Error: " + error));
            }
        });
    }

    private String getRegionName(OfflineRegion offlineRegion) {
        // Get the region name from the offline region metadata
        String regionName;

        try {
            byte[] metadata = offlineRegion.getMetadata();
            String json = new String(metadata, JSON_CHARSET);
            JSONObject jsonObject = new JSONObject(json);
            regionName = jsonObject.getString(JSON_FIELD_REGION_NAME);
        } catch (Exception exception) {
            Crashlytics.log("Failed to decode metadata.");
            Crashlytics.logException(exception);
            regionName = String.format(getString(R.string.region_name), offlineRegion.getID());
        }
        return regionName;
    }

    // Progress bar methods
    private void startProgress() {
        if (activity == null || activity.isFinishing())
            return;

        // Start and show the progress bar
        isInProgress = true;
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);

        // Disable map until download finishes
        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                showStillInProgressToast();
                return true;
            }
        });
    }

    private void setPercentage(final int percentage) {
        progressBar.setIndeterminate(false);
        progressBar.setProgress(percentage);
    }

    private void endProgress(int messageStringResourceId) {
        // Don't notify more than once
        if (!isInProgress || activity == null || activity.isFinishing()) {
            return;
        }

        String message = activity.getString(messageStringResourceId);

        // Stop and hide the progress bar
        isInProgress = false;
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);

        // Show a toast
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        Crashlytics.log("A toast was shown to the user containing the following message: " + message);


        // Enable map until download finishes
        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
    }

    void showStillInProgressToast() {
        //Show a toast to indicate that the download is still in progress
        // so the user can guess that everything will be enabled again when the progress finishes
        if (stillInProgressToast == null)
            stillInProgressToast = Toast.makeText(activity, R.string.offline_map_in_progress_toast_message, Toast.LENGTH_SHORT);
        stillInProgressToast.show();
    }
}