package com.myhitchhikingspots;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.myhitchhikingspots.model.Spot;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Download, view, navigate to, and delete an offline region.
 */
public class OfflineManagerActivity extends BaseActivity implements OnMapReadyCallback {

    // JSON encoding/decoding
    public static final String JSON_CHARSET = "UTF-8";
    public static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    // UI elements
    private MapView mapView;
    private MapboxMap mapboxMap;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.offline_map_master_layout);

        //Set CompatVectorFromResourcesEnabled to true in order to be able to use ContextCompat.getDrawable
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        menu_bottom = (BottomNavigationView) findViewById(R.id.bottom_navigation);

        downloadButton = (Button) findViewById(R.id.download_button);
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
                            Toast.makeText(getBaseContext(), "Still in progress, please wait",
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
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        fabLocateUser = (FloatingActionButton) findViewById(R.id.fab_locate_user);
        fabLocateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInProgress) {
                    //Show a toast to indicate that the download is still in progress
                    showStillInProgressToast();
                } else {
                    if (mapboxMap != null) {
                        moveCameraToLastKnownLocation();

                        if (waiting_GPS_update == null)
                            waiting_GPS_update = Toast.makeText(getBaseContext(), getString(R.string.waiting_for_gps), Toast.LENGTH_SHORT);
                        waiting_GPS_update.show();

                        locateUser();
                    }
                }
            }
        });

        fabZoomIn = (FloatingActionButton) findViewById(R.id.fab_zoom_in);
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

        fabZoomOut = (FloatingActionButton) findViewById(R.id.fab_zoom_out);
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
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        // Set up the offlineManager
        offlineManager = OfflineManager.getInstance(this);

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onMapReady(final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        //Show GPS location on the map without making the map camera follow it
        if (PermissionsManager.areLocationPermissionsGranted(OfflineManagerActivity.this) && !mapboxMap.isMyLocationEnabled())
            mapboxMap.setMyLocationEnabled(true);

        moveCameraToLastKnownLocation();

        locateUser();
    }

    // Override Activity lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();

        // Ensure no memory leak occurs if we register the location listener but the call hasn't
        // been made yet.
        if (mapboxMap != null) {
            mapboxMap.setOnMyLocationChangeListener(null);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    Toast waiting_GPS_update;

    void locateUser() {
        // Check if user has granted location permission
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            new android.support.v7.app.AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    //.setTitle(getString(R.string.refresh_datetime_dialog_title))
                    .setMessage(getString(R.string.waiting_for_gps))
                    .setPositiveButton("enable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(OfflineManagerActivity.this, new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.general_cancel_option), null).show();
        } else {
            // Enable the location layer on the map
            if (!mapboxMap.isMyLocationEnabled())
                mapboxMap.setMyLocationEnabled(true);

            //Make map camera follow GPS updates
            mapboxMap.setOnMyLocationChangeListener(new MapboxMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {
                    if (location == null)
                        return;

                    int zoomLevel = Constants.KEEP_ZOOM_LEVEL;

                    //If current zoom level is default (world level)
                    if (mapboxMap.getCameraPosition().zoom == mapboxMap.getMinZoomLevel())
                        zoomLevel = Constants.ZOOM_TO_SEE_FARTHER_DISTANCE;

                    //Move the map camera to the received location
                    moveCamera(new LatLng(location), zoomLevel);

                    mapboxMap.setOnMyLocationChangeListener(null);

                    //Hide "Waiting to receive your current location" toast, if it's still shown
                    if (waiting_GPS_update != null)
                        waiting_GPS_update.cancel();

                    //Show "Location updated" toast
                    Toast.makeText(getBaseContext(), getString(R.string.location_updated_message), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    Boolean isFirstLocationReceived = true;
    private LatLng requestToPositionAt = null;

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
                requestToPositionAt = latLng;

                if (zoom == Constants.KEEP_ZOOM_LEVEL)
                    mapboxMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                else
                    mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
            }
        }
    }

    private void moveCameraToLastKnownLocation() {
        LatLng moveCameraPositionTo = null;

        //If we know the current position of the user, move the map camera to there
        if (mapboxMap.getMyLocation() != null) {
            moveCameraPositionTo = new LatLng(mapboxMap.getMyLocation());
        } else {
            //The user might still be close to the last spot saved, move the map camera there
            Spot lastAddedSpot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getLastAddedRouteSpot();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(OfflineManagerActivity.this);

        final EditText regionNameEdit = new EditText(OfflineManagerActivity.this);
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
                            Toast.makeText(OfflineManagerActivity.this, getString(R.string.dialog_toast), Toast.LENGTH_SHORT).show();
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

        // Start the progressBar
        startProgress();

        // Create offline definition using the current
        // style and boundaries of visible map area
        String styleUrl = mapboxMap.getStyleUrl();
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
                OfflineManagerActivity.this.offlineRegion = offlineRegion;
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
                        endProgress(getString(R.string.end_progress_success));
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
                }
            }

            @Override
            public void onError(OfflineRegionError error) {
                // endProgress(getString(R.string.general_error_dialog_title));
                Crashlytics.log("onError message: " + error.getMessage());
                Crashlytics.logException(new Exception("onError reason: " + error.getReason()));
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
                endProgress(getString(R.string.tile_count_limit_exceed_error_message));
                Crashlytics.logException(new Exception("Mapbox tile count limit exceeded: " + limit + ". And error message should have been shown to the user saying '" + getString(R.string.tile_count_limit_exceed_error_message) + "'"));
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
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_no_regions_yet), Toast.LENGTH_SHORT).show();

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
                AlertDialog dialog = new AlertDialog.Builder(OfflineManagerActivity.this)
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

                                Toast.makeText(OfflineManagerActivity.this, items[regionSelected], Toast.LENGTH_LONG).show();

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
                                        endProgress(getString(R.string.toast_region_deleted));
                                    }

                                    @Override
                                    public void onError(String error) {
                                        endProgress(getString(R.string.general_error_dialog_title));
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

    private void endProgress(final String message) {
        // Don't notify more than once
        if (!isInProgress) {
            return;
        }

        // Stop and hide the progress bar
        isInProgress = false;
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);

        // Show a toast
        Toast.makeText(OfflineManagerActivity.this, message, Toast.LENGTH_LONG).show();


        // Enable map until download finishes
        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
    }


    Toast stillInProgressToast;

    void showStillInProgressToast() {
        //Show a toast to indicate that the download is still in progress
        // so the user can guess that everything will be enabled again when the progress finishes
        if (stillInProgressToast == null)
            stillInProgressToast = Toast.makeText(getBaseContext(), R.string.offline_map_in_progress_toast_message, Toast.LENGTH_SHORT);
        stillInProgressToast.show();
    }
}