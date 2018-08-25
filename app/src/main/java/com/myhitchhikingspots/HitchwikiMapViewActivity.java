package com.myhitchhikingspots;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import hitchwikiMapsSDK.entities.PlaceInfoBasic;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewManager;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.ExtendedMarkerView;
import com.myhitchhikingspots.utilities.ExtendedMarkerViewAdapter;
import com.myhitchhikingspots.utilities.ExtendedMarkerViewOptions;
import com.myhitchhikingspots.utilities.IconUtils;
import com.myhitchhikingspots.utilities.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HitchwikiMapViewActivity extends BaseActivity implements OnMapReadyCallback, PermissionsListener {
    private MapView mapView;
    private MapboxMap mapboxMap;
    //private LocationEngine locationEngine;
    //private LocationEngineListener locationEngineListener;
    private FloatingActionButton fabLocateUser, fabZoomIn, fabZoomOut;//, fabShowAll;
    //private TextView mWaitingToGetCurrentLocationTextView;
    private CoordinatorLayout coordinatorLayout;
    Boolean shouldDisplayIcons = true;

    boolean wasSnackbarShown;

    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationLayerPlugin;

    //WARNING: in order to use BaseActivity the method onCreate must be overridden
    // calling first setContentView to the view you want to use
    // and then calling super.onCreate AFTER setContentView.
    // Please always make sure this is been done!
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.hitchwiki_map_master_layout);

        //Set CompatVectorFromResourcesEnabled to true in order to be able to use ContextCompat.getDrawable
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        //mWaitingToGetCurrentLocationTextView = (TextView) findViewById(R.id.waiting_location_textview);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        prefs = getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        //savedInstanceState will be not null when a screen is rotated, for example. But will be null when activity is first created
        if (savedInstanceState == null) {
            if (!wasSnackbarShown) {
                if (getIntent().getBooleanExtra(Constants.SHOULD_SHOW_SPOT_SAVED_SNACKBAR_KEY, false))
                    showSpotSavedSnackbar();
                else if (getIntent().getBooleanExtra(Constants.SHOULD_SHOW_SPOT_DELETED_SNACKBAR_KEY, false))
                    showSpotDeletedSnackbar();
            }
            wasSnackbarShown = true;
        } else
            updateValuesFromBundle(savedInstanceState);

        fabLocateUser = (FloatingActionButton) findViewById(R.id.fab_locate_user);
        fabLocateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null) {
                    moveCameraToLastKnownLocation();

                    if (waiting_GPS_update == null)
                        waiting_GPS_update = Toast.makeText(getBaseContext(), getString(R.string.waiting_for_gps), Toast.LENGTH_SHORT);
                    waiting_GPS_update.show();

                    locateUser();
                }
            }
        });

        fabZoomIn = (FloatingActionButton) findViewById(R.id.fab_zoom_in);
        fabZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null)
                    mapboxMap.moveCamera(CameraUpdateFactory.zoomIn());
            }
        });

        fabZoomOut = (FloatingActionButton) findViewById(R.id.fab_zoom_out);
        fabZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null)
                    mapboxMap.moveCamera(CameraUpdateFactory.zoomOut());
            }
        });

        /*fabShowAll = (FloatingActionButton) findViewById(R.id.fab_show_all);
        fabShowAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null) {
                    zoomOutToFitAllMarkers();
                }
            }
        });*/


        // Get the location engine object for later use.
        //locationEngine = LocationSource.getLocationEngine(this);
        //locationEngine.activate();

        mapView = (MapView) findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        loadMarkerIcons();

        //Rename old Hitchwiki Maps directory to something more intuitive for the user
        if (prefs.getBoolean(Constants.PREFS_HITCHWIKI_STORAGE_RENAMED, false)) {
            File oldFolder = new File(Constants.HITCHWIKI_MAPS_STORAGE_OLDPATH);
            File newFolder = new File(Constants.HITCHWIKI_MAPS_STORAGE_PATH);
            oldFolder.renameTo(newFolder);
            prefs.edit().putBoolean(Constants.PREFS_HITCHWIKI_STORAGE_RENAMED, true).apply();
        }

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
    }

    // Permissions variables
    private static final int PERMISSIONS_LOCATION = 0;
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 1;

    //persmission method.
    public static boolean isReadStoragePermissionGranted(Activity activity) {
        // Check if we have read permission
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        // Check if user has granted location permission
        return (readPermission == PackageManager.PERMISSION_GRANTED);
    }

    public static void requestReadStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSIONS_EXTERNAL_STORAGE
        );
    }

    Toast waiting_GPS_update;

    void locateUser() {
        enableLocationPlugin();

        // Enable the location layer on the map
        if (PermissionsManager.areLocationPermissionsGranted(HitchwikiMapViewActivity.this) && !locationLayerPlugin.isLocationLayerEnabled())
            locationLayerPlugin.setLocationLayerEnabled(true);

        /*  // Check if user has granted location permission
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            Snackbar.make(coordinatorLayout, getResources().getString(R.string.waiting_for_gps), Snackbar.LENGTH_LONG)
                    .setAction(R.string.general_enable_button_label, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(HitchwikiMapViewActivity.this, new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
                        }
                    }).show();
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.waiting_for_gps), Toast.LENGTH_SHORT).show();;/
        }*/
    }

    void showSpotSavedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_saved_successfuly),
                null, null);
    }

    void showSpotDeletedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_deleted_successfuly),
                null, null);
    }

    Snackbar snackbar;

    void showSnackbar(@NonNull CharSequence text, CharSequence action, View.OnClickListener listener) {
        String t = "";
        if (text != null && text.length() > 0)
            t = text.toString();
        snackbar = Snackbar.make(coordinatorLayout, t.toUpperCase(), Snackbar.LENGTH_LONG)
                .setAction(action, listener);

        // get snackbar view
        View snackbarView = snackbar.getView();

        // set action button color
        snackbar.setActionTextColor(Color.BLACK);

        // change snackbar text color
        int snackbarTextId = android.support.design.R.id.snackbar_text;
        TextView textView = (TextView) snackbarView.findViewById(snackbarTextId);
        if (textView != null) textView.setTextColor(Color.WHITE);


        // change snackbar background
        snackbarView.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.ic_regular_spot_color));

        snackbar.show();
    }

    void dismissSnackbar() {
        try {
            if (snackbar != null && snackbar.isShown())
                snackbar.dismiss();
        } catch (Exception e) {
        }
    }

    private void loadMarkerIcons() {
        ic_single_spot = IconUtils.drawableToIcon(this, R.drawable.ic_marker_got_a_ride_24dp, -1);

        ic_took_a_break_spot = IconUtils.drawableToIcon(this, R.drawable.ic_break_spot_icon, -1);
        ic_waiting_spot = IconUtils.drawableToIcon(this, R.drawable.ic_marker_waiting_for_a_ride_24dp, -1);
        ic_arrival_spot = IconUtils.drawableToIcon(this, R.drawable.ic_arrival_icon, -1);

        ic_typeunknown_spot = IconUtils.drawableToIcon(this, R.drawable.ic_edit_location_black_24dp, getIdentifierColorStateList(-1));
        ic_got_a_ride_spot0 = IconUtils.drawableToIcon(this, R.drawable.ic_marker_got_a_ride_24dp, getIdentifierColorStateList(0));
        ic_got_a_ride_spot1 = IconUtils.drawableToIcon(this, R.drawable.ic_marker_got_a_ride_24dp, getIdentifierColorStateList(1));
        ic_got_a_ride_spot2 = IconUtils.drawableToIcon(this, R.drawable.ic_marker_got_a_ride_24dp, getIdentifierColorStateList(2));
        ic_got_a_ride_spot3 = IconUtils.drawableToIcon(this, R.drawable.ic_marker_got_a_ride_24dp, getIdentifierColorStateList(3));
        ic_got_a_ride_spot4 = IconUtils.drawableToIcon(this, R.drawable.ic_marker_got_a_ride_24dp, getIdentifierColorStateList(4));
    }

    Spot mCurrentWaitingSpot;
    boolean mIsWaitingForARide;
    boolean mWillItBeFirstSpotOfARoute;

    public void setValues(List<Spot> spotList, Spot currentWaitingSpot) {
        mCurrentWaitingSpot = currentWaitingSpot;

        if (mCurrentWaitingSpot == null || mCurrentWaitingSpot.getIsWaitingForARide() == null)
            mIsWaitingForARide = false;
        else
            mIsWaitingForARide = mCurrentWaitingSpot.getIsWaitingForARide();


        mWillItBeFirstSpotOfARoute = spotList.size() == 0 || (spotList.get(0).getIsDestination() != null && spotList.get(0).getIsDestination());
    }

    public enum pageType {
        NOT_FETCHING_LOCATION,
        WILL_BE_FIRST_SPOT_OF_A_ROUTE, //user sees "save spot" but doesn't see "arrival" button
        WILL_BE_REGULAR_SPOT, //user sees both "save spot" and "arrival" buttons
        WAITING_FOR_A_RIDE //user sees "got a ride" and "take a break" buttons
    }

    private pageType currentPage;

    protected void updateCurrentPage() {
        //If it's not waiting for a ride
        if (!mIsWaitingForARide) {
            /*if (!locationEngine.areLocationPermissionsGranted() || locationEngine.getLastLocation() == null
                    || !mRequestingLocationUpdates) {
                currentPage = pageType.NOT_FETCHING_LOCATION;
            } else {*/
            if (mWillItBeFirstSpotOfARoute)
                currentPage = pageType.WILL_BE_FIRST_SPOT_OF_A_ROUTE;
            else {
                currentPage = pageType.WILL_BE_REGULAR_SPOT;
            }
            //}

        } else {
            currentPage = pageType.WAITING_FOR_A_RIDE;
        }

        Crashlytics.setString("currentPage", currentPage.toString());
    }

    /**
     * Represents a geographical location.
     */
    boolean wasFirstLocationReceived = false;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locateUser();
            }
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "R.string.user_location_permission_not_granted", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            Toast.makeText(this, "R.string.user_location_permission_not_granted", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationPlugin() {
        if (locationLayerPlugin == null) {
            // Check if permissions are enabled and if not request
            if (PermissionsManager.areLocationPermissionsGranted(this)) {

                // Create an instance of the plugin. Adding in LocationLayerOptions is also an optional parameter
                locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap);

                locationLayerPlugin.addOnCameraTrackingChangedListener(new OnCameraTrackingChangedListener() {
                    @Override
                    public void onCameraTrackingDismissed() {
                        // Tracking has been dismissed
                    }

                    @Override
                    public void onCameraTrackingChanged(int currentMode) {
                        // CameraMode has been updated

                        if (!wasFirstLocationReceived) {
                            updateCurrentPage();
                            wasFirstLocationReceived = true;
                        }
                    }
                });

                // Set the plugin's camera mode
                locationLayerPlugin.setCameraMode(CameraMode.TRACKING_GPS);
                getLifecycle().addObserver(locationLayerPlugin);
            } else {
                permissionsManager = new PermissionsManager(this);
                permissionsManager.requestLocationPermissions(this);
            }
        }
    }

    public void loadValues() {

        PlaceInfoBasic[] placesContainerFromFile = Utils.loadHitchwikiSpotsFromLocalFile();

        if (spotList == null)
            spotList = new ArrayList<>();
        else
            spotList.clear();

        if (placesContainerFromFile != null) {
            for (int i = 0; i < placesContainerFromFile.length; i++) {
                try {
                    Spot s = Utils.convertToSpot(placesContainerFromFile[i]);
                    if (s.getId() == null || s.getId() == 0)
                        s.setId((long) i);
                    spotList.add(s);
                } catch (Exception ex) {
                    Crashlytics.logException(ex);
                }
            }
        }

        updateCurrentPage();
    }


    //onMapReady is called after onResume()
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        Crashlytics.log(Log.INFO, TAG, "mapReady called");
        // Customize map with markers, polylines, etc.
        this.mapboxMap = mapboxMap;
        prefs.edit().putBoolean(Constants.PREFS_MAPBOX_WAS_EVER_LOADED, true).apply();

        /*// Customize the user location icon using the getMyLocationViewSettings object.
        //this.mapboxMap.getMyLocationViewSettings().setPadding(0, 500, 0, 0);
        this.mapboxMap.getMyLocationViewSettings().setForegroundTintColor(ContextCompat.getColor(getBaseContext(), R.color.mapbox_my_location_ring_copy));//Color.parseColor("#56B881")*/

        enableLocationPlugin();

        this.mapboxMap.setOnInfoWindowClickListener(new MapboxMap.OnInfoWindowClickListener() {
            @Override
            public boolean onInfoWindowClick(@NonNull Marker marker) {
                ExtendedMarkerView myMarker = (ExtendedMarkerView) marker;
                Crashlytics.setString("Clicked marker tag", myMarker.getTag());
                Spot spot = null;
                for (Spot spot2 :
                        spotList) {
                    String id = spot2.getId().toString();
                    if (id.equals(myMarker.getTag())) {
                        spot = spot2;
                        break;
                    }
                }

                if (spot != null) {
                    /*Spot mCurrentWaitingSpot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getCurrentSpot();

                    //If the user is currently waiting at a spot and the clicked spot is not the one he's waiting at, show a Toast.
                    if (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null &&
                            mCurrentWaitingSpot.getIsWaitingForARide()) {
                        if (mCurrentWaitingSpot.getId() == spot.getId())
                            spot.setAttemptResult(null);
                        else {
                            Toast.makeText(getBaseContext(), getResources().getString(R.string.evaluate_running_spot_required), Toast.LENGTH_LONG).show();
                            return true;
                        }
                    }*/

                    //Create a record to track of HW spots viewed by the user
                    Answers.getInstance().logCustom(new CustomEvent("HW spot viewed"));

                    Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);
                    //Maybe we should send mCurrentWaitingSpot on the intent.putExtra so that we don't need to call spot.setAttemptResult(null) ?
                    intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
                    intent.putExtra(Constants.SHOULD_RETRIEVE_HITCHWIKI_DETAILS_KEY, true);

                    startActivityForResult(intent, EDIT_SPOT_REQUEST);
                } else
                    Crashlytics.log(Log.WARN, TAG,
                            "A spot corresponding to the clicked InfoWindow was not found on the list. If a spot isn't in the list, how a marker was added to it? The open marker's tag was: " + myMarker.getTag());


                return true;
            }
        });

        final MarkerViewManager markerViewManager = mapboxMap.getMarkerViewManager();
        // if you want to customise a ViewMarker you need to extend ViewMarker and provide an adapter implementation
        // set adapters for child classes of ViewMarker
        markerViewManager.addMarkerViewAdapter(new ExtendedMarkerViewAdapter(HitchwikiMapViewActivity.this));

        updateUI();
    }

    void updateUI() {
        Crashlytics.log(Log.INFO, TAG, "updateUI was called");

        loadAll();
    }

    SharedPreferences prefs;

    void loadAll() {
        Location loc = null;
        try {
            loc = (locationLayerPlugin != null) ? locationLayerPlugin.getLastKnownLocation() : null;
        } catch (SecurityException ex) {
        }

        if (loc != null)
            moveCamera(new LatLng(loc));

        Long millisecondsAtRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD, 0);
        if (millisecondsAtRefresh > 0) {
            //Check if we still have read permission so that we can read the file containing the downloaded HW spots
            if (!isReadStoragePermissionGranted(this))
                requestReadStoragePermission(this);
            else {
                //Load spots and display them as markers and polylines on the map
                new DrawAnnotations().execute();
            }
        } else {
            showDialogDownloadHWSpots();
        }
    }

    private void showDialogDownloadHWSpots() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.menu_hitchwiki_maps))
                .setMessage(String.format(getString(R.string.empty_list_dialog_message), getString(R.string.tools_title)))
                .setPositiveButton(getString(R.string.tools_title), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                    }
                })
                .setNegativeButton(getResources().getString(R.string.general_cancel_option), null).show();
    }


    protected boolean isDrawingAnnotations = false;

    static String locationSeparator = ", ";

    private static String spotLocationToString(Spot spot) {

        ArrayList<String> loc = new ArrayList();
        try {
            //Show location string only if GpsResolved is set to true
            if (spot.getGpsResolved() != null && spot.getGpsResolved())
                loc = Utils.spotLocationToList(spot);

            //Join the strings
            return TextUtils.join(locationSeparator, loc);
        } catch (Exception ex) {
            Crashlytics.log(Log.WARN, TAG, "Generating a string for the spot's address has failed");
            Crashlytics.logException(ex);
        }
        return "";
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
    public void onResume() {
        super.onResume();
        Crashlytics.log(Log.INFO, TAG, "onResume called");
        mapView.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == RESULT_OBJECT_ADDED || resultCode == RESULT_OBJECT_EDITED)
            showSpotSavedSnackbar();

        if (resultCode == RESULT_OBJECT_DELETED)
            showSpotDeletedSnackbar();

      /*
        // Check which request we're responding to
        if (requestCode == SAVE_SPOT_REQUEST || requestCode == EDIT_SPOT_REQUEST) {
            // Make sure the request was successful
            if (resultCode > RESULT_FIRST_USER)
                updateUI();
        }*/
    }

    Icon ic_single_spot, ic_typeunknown_spot, ic_took_a_break_spot, ic_waiting_spot, ic_arrival_spot = null;
    Icon ic_got_a_ride_spot0, ic_got_a_ride_spot1, ic_got_a_ride_spot2, ic_got_a_ride_spot3, ic_got_a_ride_spot4;

    List<Spot> spotList = new ArrayList<Spot>();


    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();

        dismissSnackbar();
        dismissProgressDialog();
    }

    protected static final String SNACKBAR_SHOWED_KEY = "snackbar-showed";


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        mapView.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean(SNACKBAR_SHOWED_KEY, wasSnackbarShown);
    }


    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Crashlytics.log(Log.INFO, TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(SNACKBAR_SHOWED_KEY))
                wasSnackbarShown = savedInstanceState.getBoolean(SNACKBAR_SHOWED_KEY);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.hitchwiki_map_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean selectionHandled = false;

        switch (item.getItemId()) {
            case R.id.action_toggle_icons:
                shouldDisplayIcons = !shouldDisplayIcons;
                if (shouldDisplayIcons) {
                    fabLocateUser.setVisibility(View.VISIBLE);
                    fabZoomIn.setVisibility(View.VISIBLE);
                    fabZoomOut.setVisibility(View.VISIBLE);
                    item.setTitle(getString(R.string.general_hide_icons_label));
                } else {
                    fabLocateUser.setVisibility(View.GONE);
                    fabZoomIn.setVisibility(View.GONE);
                    fabZoomOut.setVisibility(View.GONE);
                    item.setTitle(getString(R.string.general_show_icons_label));
                }
                break;
            case R.id.action_zoom_to_fit_all:
                if (mapboxMap != null) {
                    zoomOutToFitAllMarkers();
                }
                break;
        }

        if (selectionHandled)
            return true;
        else
            return super.onOptionsItemSelected(item);
    }

    protected void zoomOutToFitAllMarkers() {
        try {
            if (mapboxMap != null) {
                Location mCurrentLocation = null;
                try {
                    mCurrentLocation = (locationLayerPlugin != null) ? locationLayerPlugin.getLastKnownLocation() : null;
                } catch (SecurityException ex) {
                }

                List<LatLng> lst = new ArrayList<>();
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : mapboxMap.getMarkers()) {
                    lst.add(marker.getPosition());
                }

                if (mCurrentLocation != null)
                    lst.add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));

                //Add current location to camera bounds
                if (lst.size() == 1)
                    moveCamera(new LatLng(lst.get(0).getLatitude(), lst.get(0).getLongitude()), Constants.ZOOM_TO_SEE_FARTHER_DISTANCE);
                else if (lst.size() > 1) {
                    builder.includes(lst);
                    LatLngBounds bounds = builder.build();

                    //If there's only 2 points in the list and the currentlocation is known, that means only one of them is a saved spot
                    if (mCurrentLocation != null && lst.size() == 2)
                        mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150), 5000);
                    else
                        mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 5000);
                }
            }

        } catch (Exception ex) {
            Crashlytics.logException(ex);
            showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                    "Show all markers failed - " + ex.getMessage()));
        }
    }


    /**
     * Move the map camera to the given position
     *
     * @param latLng Target location to change to
     * @param zoom   Zoom level to change to
     */
    private void moveCamera(LatLng latLng, long zoom) {
        if (latLng != null) {
            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        }
    }

    /**
     * Move the map camera to the given position with zoom Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT
     *
     * @param latLng Target location to change to
     */
    private void moveCamera(LatLng latLng) {
        moveCamera(latLng, Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT);
    }

    private void moveCameraToLastKnownLocation() {
        LatLng moveCameraPositionTo = null;

        //If we know the current position of the user, move the map camera to there
        try {
            moveCameraPositionTo = (locationLayerPlugin != null) ? new LatLng(locationLayerPlugin.getLastKnownLocation()) : null;
        } catch (SecurityException ex) {
        }

        if (moveCameraPositionTo != null) {
            moveCameraPositionTo = new LatLng(moveCameraPositionTo);
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

    private ProgressDialog loadingDialog;

    private void showProgressDialog() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(HitchwikiMapViewActivity.this);
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
            loadingDialog.setMessage(getResources().getString(R.string.map_loading_dialog));
        }
        loadingDialog.show();
    }

    private void dismissProgressDialog() {
        try {
            if (loadingDialog != null && loadingDialog.isShowing())
                loadingDialog.dismiss();
        } catch (Exception e) {
        }
    }

    private class DrawAnnotations extends AsyncTask<Void, Void, List<List<ExtendedMarkerViewOptions>>> {
        private final ProgressDialog dialog = new ProgressDialog(HitchwikiMapViewActivity.this);

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected List<List<ExtendedMarkerViewOptions>> doInBackground(Void... voids) {
            try {
                loadValues();

                isDrawingAnnotations = true;

                List<List<ExtendedMarkerViewOptions>> trips = new ArrayList<>();
                ArrayList<ExtendedMarkerViewOptions> spots = new ArrayList<>();
                ArrayList<ExtendedMarkerViewOptions> singleSpots = new ArrayList<>();

                //The spots are ordered from the last saved ones to the first saved ones, so we need to
                // go through the list in the oposite direction in order to sum up the route's totals from their origin to their destinations
                for (int i = spotList.size() - 1; i >= 0; i--) {
                    Spot spot = spotList.get(i);

                    ExtendedMarkerViewOptions markerViewOptions = new ExtendedMarkerViewOptions()
                            .position(new LatLng(spot.getLatitude(), spot.getLongitude()));

                    if (spot.getId() != null)
                        markerViewOptions.tag(spot.getId().toString());


                    markerViewOptions.icon(ic_single_spot);
                    markerViewOptions.spotType(Constants.SPOT_TYPE_HITCHHIKING_SPOT);


                    //Get location string
                    String firstLine = spotLocationToString(spot).trim();
                    String secondLine = "";

                    //Add date time if it is set
                    if (spot.getStartDateTime() != null)
                        secondLine += SpotListAdapter.dateTimeToString(spot.getStartDateTime());

                    /*//Add type
                    if (!spotType.isEmpty()) {
                        if (!secondLine.isEmpty())
                            secondLine += " - ";
                        secondLine += spotType;
                    }*/

                    //Add waiting time
                    if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot() && spot.getWaitingTime() != null) {
                        if (!secondLine.isEmpty())
                            secondLine += " ";
                        secondLine += "(" + Utils.getWaitingTimeAsString(spot.getWaitingTime(), getBaseContext()) + ")";
                    }

                    //Add note
                    if (spot.getNote() != null && !spot.getNote().isEmpty()) {
                        if (!secondLine.isEmpty())
                            secondLine += "\n";
                        secondLine += spot.getNote();
                    }

                    String snippetAllLines = firstLine;
                    if (!snippetAllLines.isEmpty())
                        snippetAllLines += "\n";
                    snippetAllLines += secondLine;

                    //Get a hitchability string to set as title
                    String title = "";
                    if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot())
                        title = Utils.getRatingOrDefaultAsString(getBaseContext(), spot.getHitchability() != null ? spot.getHitchability() : 0);

                    //Set hitchability as title
                    markerViewOptions.title(title.toUpperCase());

                    // Customize map with markers, polylines, etc.
                    markerViewOptions.snippet(snippetAllLines);

                    if (spot.getIsPartOfARoute() != null && spot.getIsPartOfARoute()) {
                        spots.add(markerViewOptions);

                        if (spot.getIsDestination() != null && spot.getIsDestination() || i == 0) {
                            trips.add(spots);
                            spots = new ArrayList<>();
                        }
                    } else
                        singleSpots.add(markerViewOptions);
                }

                if (singleSpots.size() > 0) {
                    trips.add(singleSpots);
                    isLastArrayForSingleSpots = true;
                }

                return trips;
            } catch (final Exception ex) {
                Crashlytics.logException(ex);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showErrorAlert(getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                                "Loading spots failed.\n" + ex.getMessage()));
                    }
                });
            }
            return new ArrayList<>();
        }

        Boolean isLastArrayForSingleSpots = false;

        @Override
        protected void onPostExecute(List<List<ExtendedMarkerViewOptions>> trips) {
            super.onPostExecute(trips);
            if (HitchwikiMapViewActivity.this.isFinishing())
                return;

            try {
                mapboxMap.clear();

                for (int lc = 0; lc < trips.size(); lc++) {
                    List<ExtendedMarkerViewOptions> spots = trips.get(lc);

                    //If it's the last array and isLastArrayForSingleSpots is true, add the markers with no polyline connecting them
                    if (isLastArrayForSingleSpots && lc == trips.size() - 1) {
                        for (ExtendedMarkerViewOptions spot : spots) {
                            //Add marker to map
                            mapboxMap.addMarker(spot);
                        }
                    } else {
                        PolylineOptions line = new PolylineOptions()
                                .width(2)
                                .color(Color.parseColor(getResources().getString(getPolylineColorAsId(lc))));//Color.parseColor(getPolylineColorAsString(lc)));

                        //Add route to the map with markers and polylines connecting them
                        for (ExtendedMarkerViewOptions spot : spots) {
                            //Add marker to map
                            mapboxMap.addMarker(spot);

                            //Add polyline connecting this marker
                            line.add(spot.getPosition());
                        }

                        if (line.getPoints().size() > 1) {
                            //Add polylines to map
                            mapboxMap.addPolyline(line);
                        }

                        zoomOutToFitAllMarkers();
                    }
                }
            } catch (Exception ex) {
                Crashlytics.logException(ex);
                showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                        "Adding markers failed - " + ex.getMessage()));
            }

            dismissProgressDialog();

            //If there's no spot to show, display dialog trying to encourage the user to go and download some HW spots
            //This should be particularly useful when user had downloaded HW spots but the local file was manually deleted or got corrupted for some reason
            if (trips.size() == 0 || (trips.size() == 1 && trips.get(0).size() == 0))
                showDialogDownloadHWSpots();

            isDrawingAnnotations = false;

        }

    }

    protected static final String TAG = "hitchwikimap-view-activity";

    private Icon getGotARideIconForRoute(int routeIndex) {
        Icon i = ic_typeunknown_spot;

        if (routeIndex > -1) {
            int value = routeIndex;
            if (value < 5)
                value += 5;

            switch (value % 5) {
                case 0:
                    i = ic_got_a_ride_spot0;
                    break;
                case 1:
                    i = ic_got_a_ride_spot1;
                    break;
                case 2:
                    i = ic_got_a_ride_spot2;
                    break;
                case 3:
                    i = ic_got_a_ride_spot3;
                    break;
                case 4:
                    i = ic_got_a_ride_spot4;
                    break;
            }
        }

        return i;
    }

    private int getIdentifierColor(int routeIndex) {
        int polylineColor = Color.GRAY;

        if (routeIndex > -1) {
            int value = routeIndex;
            if (value < 5)
                value += 5;

            switch (value % 5) {
                case 0:
                    polylineColor = Color.BLUE;
                    break;
                case 1:
                    polylineColor = Color.GREEN;
                    break;
                case 2:
                    polylineColor = Color.YELLOW;
                    break;
                case 3:
                    polylineColor = Color.MAGENTA;
                    break;
                case 4:
                    polylineColor = Color.BLACK;
                    break;
            }
        }

        return polylineColor;
    }

    private ColorStateList getIdentifierColorStateList(int routeIndex) {
        return ContextCompat.getColorStateList(getBaseContext(), getPolylineColorAsId(routeIndex));
    }

    private int getPolylineColorAsId(int routeIndex) {
        int polylineColor = R.color.route_color_unknown;

        if (routeIndex > -1) {
            int value = routeIndex;
            if (value < 5)
                value += 5;

            switch (value % 5) {
                case 0:
                    polylineColor = R.color.route_color_0;
                    break;
                case 1:
                    polylineColor = R.color.route_color_1;
                    break;
                case 2:
                    polylineColor = R.color.route_color_2;
                    break;
                case 3:
                    polylineColor = R.color.route_color_3;
                    break;
                case 4:
                    polylineColor = R.color.route_color_4;
                    break;
            }
        }

        return polylineColor;
    }


}
