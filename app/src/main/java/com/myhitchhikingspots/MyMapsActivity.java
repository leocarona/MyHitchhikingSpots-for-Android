package com.myhitchhikingspots;

import android.Manifest;
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
import android.os.Build;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
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
import com.mapbox.mapboxsdk.plugins.locationlayer.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;
import com.myhitchhikingspots.utilities.ExtendedMarkerView;
import com.myhitchhikingspots.utilities.ExtendedMarkerViewAdapter;
import com.myhitchhikingspots.utilities.ExtendedMarkerViewOptions;
import com.myhitchhikingspots.utilities.IconUtils;
import com.myhitchhikingspots.utilities.Utils;

import java.util.ArrayList;
import java.util.List;

public class MyMapsActivity extends BaseActivity implements OnMapReadyCallback, PermissionsListener {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private FloatingActionButton fabLocateUser, fabZoomIn, fabZoomOut, fabShowAll;

    private FloatingActionButton fabSpotAction1, fabSpotAction2;
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

        setContentView(R.layout.my_map_master_layout);

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
                    Location loc = null;
                    try {
                        loc = (locationLayerPlugin != null) ? locationLayerPlugin.getLastKnownLocation() : null;
                    } catch (SecurityException ex) {
                    }

                    if (loc != null)
                        moveCamera(new LatLng(loc));
                    else
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

        fabSpotAction1 = (FloatingActionButton) findViewById(R.id.fab_spot_action_1);
        fabSpotAction1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //While waiting for a ride, MainActionButton is a "Got a ride" button
                //when not waiting for a ride, it's a "Save spot" button

                if (mIsWaitingForARide)
                    gotARideButtonHandler();
                else
                    saveRegularSpotButtonHandler();


                //   Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //         .setAction("Action", null).show();
                //startActivity(new Intent(getApplicationContext(), MyLocationFragment.class));
            }
        });

        fabSpotAction2 = (FloatingActionButton) findViewById(R.id.fab_spot_action_2);
        fabSpotAction2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //While waiting for a ride, SecondaryActionButton is a "Arrived to destination" button
                //when not waiting for a ride, it's a "Take a break" button

                if (mIsWaitingForARide)
                    tookABreakButtonHandler();
                else
                    saveDestinationSpotButtonHandler();

                // Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //       .setAction("Action", null).show();
                //startActivity(new Intent(getApplicationContext(), MyLocationFragment.class));
            }
        });

        fabSpotAction1.setVisibility(View.INVISIBLE);
        fabSpotAction2.setVisibility(View.INVISIBLE);

        mapView = (MapView) findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        loadMarkerIcons();

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
    }

    private static final int PERMISSIONS_LOCATION = 0;

    void locateUser() {
        enableLocationPlugin();

        // Enable the location layer on the map
        if (PermissionsManager.areLocationPermissionsGranted(MyMapsActivity.this) && !locationLayerPlugin.isLocationLayerEnabled())
            locationLayerPlugin.setLocationLayerEnabled(true);

        /*// Check if user has granted location permission
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            Snackbar.make(coordinatorLayout, getResources().getString(R.string.waiting_for_gps), Snackbar.LENGTH_LONG)
                    .setAction(R.string.general_enable_button_label, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MyMapsActivity.this, new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
                        }
                    }).show();
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.waiting_for_gps), Toast.LENGTH_SHORT).show();
            // Enable the location layer on the map
            if(locationLayerPlugin != null && !locationLayerPlugin.isLocationLayerEnabled())
                locationLayerPlugin.setLocationLayerEnabled(true);
            //Place the map camera at the next GPS position that we receive
            mapboxMap.setOnMyLocationChangeListener(null);
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
        ic_single_spot = IconUtils.drawableToIcon(this, R.drawable.ic_route_point_black_24dp, -1);

        ic_point_on_the_route_spot = IconUtils.drawableToIcon(this, R.drawable.ic_point_on_the_route_black_24dp, -1, 0.9, 0.9);
        ic_took_a_break_spot = IconUtils.drawableToIcon(this, R.drawable.ic_break_spot_icon, -1, 0.9, 0.9);
        ic_waiting_spot = IconUtils.drawableToIcon(this, R.drawable.ic_marker_waiting_for_a_ride_24dp, -1, 0.9, 0.9);
        ic_arrival_spot = IconUtils.drawableToIcon(this, R.drawable.ic_arrival_icon, -1, 0.9, 0.9);

        ic_typeunknown_spot = IconUtils.drawableToIcon(this, R.drawable.ic_edit_location_black_24dp, getIdentifierColorStateList(-1));
        ic_got_a_ride_spot0 = IconUtils.drawableToIcon(this, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(0));
        ic_got_a_ride_spot1 = IconUtils.drawableToIcon(this, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(1));
        ic_got_a_ride_spot2 = IconUtils.drawableToIcon(this, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(2));
        ic_got_a_ride_spot3 = IconUtils.drawableToIcon(this, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(3));
        ic_got_a_ride_spot4 = IconUtils.drawableToIcon(this, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(4));
    }

    Spot mCurrentWaitingSpot;
    boolean mIsWaitingForARide;
    boolean mWillItBeFirstSpotOfARoute;

    public enum pageType {
        NOT_FETCHING_LOCATION,
        WILL_BE_FIRST_SPOT_OF_A_ROUTE, //user sees "save spot" but doesn't see "arrival" button
        WILL_BE_REGULAR_SPOT, //user sees both "save spot" and "arrival" buttons
        WAITING_FOR_A_RIDE //user sees "got a ride" and "take a break" buttons
    }

    private pageType currentPage;

    protected void configureBottomFABButtons() {
        switch (currentPage) {
            case NOT_FETCHING_LOCATION:
            default:
                fabSpotAction1.setVisibility(View.INVISIBLE);
                fabSpotAction2.setVisibility(View.INVISIBLE);
                break;
            case WILL_BE_FIRST_SPOT_OF_A_ROUTE:
                fabSpotAction1.setImageResource(R.drawable.ic_regular_spot_icon);
                fabSpotAction1.setBackgroundTintList(ContextCompat.getColorStateList(getBaseContext(), R.color.ic_regular_spot_color));
                fabSpotAction1.setRippleColor(ContextCompat.getColor(getBaseContext(), R.color.ic_regular_spot_color_lighter));

                fabSpotAction1.setVisibility(View.VISIBLE);
                fabSpotAction2.setVisibility(View.INVISIBLE);
                break;
            case WILL_BE_REGULAR_SPOT:
                fabSpotAction1.setImageResource(R.drawable.ic_regular_spot_icon);
                fabSpotAction1.setBackgroundTintList(ContextCompat.getColorStateList(getBaseContext(), R.color.ic_regular_spot_color));
                fabSpotAction1.setRippleColor(ContextCompat.getColor(getBaseContext(), R.color.ic_regular_spot_color_lighter));
                fabSpotAction2.setImageResource(R.drawable.ic_arrival_icon);
                fabSpotAction2.setBackgroundTintList(ContextCompat.getColorStateList(getBaseContext(), R.color.ic_arrival_color));
                fabSpotAction2.setRippleColor(ContextCompat.getColor(getBaseContext(), R.color.ic_arrival_color_lighter));

                fabSpotAction1.setVisibility(View.VISIBLE);
                fabSpotAction2.setVisibility(View.VISIBLE);
                break;
            case WAITING_FOR_A_RIDE:
                fabSpotAction1.setImageResource(R.drawable.ic_got_a_ride_spot_icon);
                fabSpotAction1.setBackgroundTintList(ContextCompat.getColorStateList(getBaseContext(), R.color.ic_got_a_ride_color));
                fabSpotAction1.setRippleColor(ContextCompat.getColor(getBaseContext(), R.color.ic_got_a_ride_color_lighter));
                fabSpotAction2.setImageResource(R.drawable.ic_break_spot_icon);
                fabSpotAction2.setBackgroundTintList(ContextCompat.getColorStateList(getBaseContext(), R.color.ic_break_color));
                fabSpotAction2.setRippleColor(ContextCompat.getColor(getBaseContext(), R.color.ic_break_color_lighter));

                fabSpotAction1.setVisibility(View.VISIBLE);
                fabSpotAction2.setVisibility(View.VISIBLE);
                break;
        }
    }

    protected void updateUISaveButtons() {
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

        configureBottomFABButtons();
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
                            updateUISaveButtons();
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
        MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
        DaoSession daoSession = appContext.getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();

        spotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.IsPartOfARoute, SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();

        mCurrentWaitingSpot = appContext.getCurrentSpot();

        if (mCurrentWaitingSpot == null || mCurrentWaitingSpot.getIsWaitingForARide() == null)
            mIsWaitingForARide = false;
        else
            mIsWaitingForARide = mCurrentWaitingSpot.getIsWaitingForARide();


        mWillItBeFirstSpotOfARoute = spotList.size() == 0 || (spotList.get(0).getIsDestination() != null && spotList.get(0).getIsDestination());
    }

    //onMapReady is called after onResume()
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        Crashlytics.log(Log.INFO, TAG, "onMapReady was called");
        // Customize map with markers, polylines, etc.
        this.mapboxMap = mapboxMap;
        prefs.edit().putBoolean(Constants.PREFS_MAPBOX_WAS_EVER_LOADED, true).apply();

        // Customize the user location icon using the getMyLocationViewSettings object.
        //this.mapboxMap.getMyLocationViewSettings().setForegroundTintColor(ContextCompat.getColor(getBaseContext(), R.color.mapbox_my_location_ring_copy));//Color.parseColor("#56B881")

        enableLocationPlugin();

        // Enable the location layer on the map
        if (PermissionsManager.areLocationPermissionsGranted(MyMapsActivity.this) && !locationLayerPlugin.isLocationLayerEnabled())
            locationLayerPlugin.setLocationLayerEnabled(true);

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
                    Spot mCurrentWaitingSpot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getCurrentSpot();

                    //If the user is currently waiting at a spot and the clicked spot is not the one he's waiting at, show a Toast.
                    if (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null &&
                            mCurrentWaitingSpot.getIsWaitingForARide()) {
                        if (mCurrentWaitingSpot.getId() == spot.getId())
                            spot.setAttemptResult(null);
                        else {
                            Toast.makeText(getBaseContext(), getResources().getString(R.string.evaluate_running_spot_required), Toast.LENGTH_LONG).show();
                            return true;
                        }
                    }

                    Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);
                    //Maybe we should send mCurrentWaitingSpot on the intent.putExtra so that we don't need to call spot.setAttemptResult(null) ?
                    intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
                    intent.putExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, true);
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
        markerViewManager.addMarkerViewAdapter(new ExtendedMarkerViewAdapter(MyMapsActivity.this));

        updateUI();
    }

    SharedPreferences prefs;

    void updateUI() {
        Crashlytics.log(Log.INFO, TAG, "updateUI was called");

        if (!Utils.isNetworkAvailable(this) && !Utils.shouldLoadCurrentView(prefs)) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.general_network_unavailable_message))
                    .setMessage(getResources().getString(R.string.map_error_alert_map_not_loaded_message))
                    .setPositiveButton(getResources().getString(R.string.map_error_alert_map_not_loaded_positive_button), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_LAST_OFFLINE_MODE_WARN, System.currentTimeMillis()).apply();
                            prefs.edit().putBoolean(Constants.PREFS_OFFLINE_MODE_SHOULD_LOAD_CURRENT_VIEW, false).apply();

                            openSpotsListView(true);
                        }
                    })
                    .setNegativeButton(String.format(getString(R.string.action_button_label), getString(R.string.view_map_button_label)), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_LAST_OFFLINE_MODE_WARN, System.currentTimeMillis()).apply();
                            prefs.edit().putBoolean(Constants.PREFS_OFFLINE_MODE_SHOULD_LOAD_CURRENT_VIEW, true).apply();

                            loadAll();
                        }
                    }).show();
        } else {
            loadAll();
        }
    }

    void loadAll() {
        //Load polylines
        new DrawAnnotations().execute();
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

    @NonNull
    private static String getString(Spot mCurrentSpot) {
        String spotLoc = spotLocationToString(mCurrentSpot).trim();
        if (spotLoc != null && !spotLoc.isEmpty()) {// spotLoc = "- " + spotLoc;
        } else if (mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null)
            spotLoc = "(" + mCurrentSpot.getLatitude() + ", " + mCurrentSpot.getLongitude() + ")";
        return spotLoc;
    }

   /* private static String dateTimeToString(Date dt) {
        if (dt != null) {
            SimpleDateFormat res;
            String dateFormat = "dd/MMM', 'HH:mm";

            if (Locale.getDefault() == Locale.US)
                dateFormat = "MMM/dd', 'HH:mm";

            try {
                res = new SimpleDateFormat(dateFormat);
                return res.format(dt);
            } catch (Exception ex) {
                Crashlytics.setString("date", dt.toString());
                Crashlytics.logException(ex);
            }
        }
        return "";
    }*/

    @Override
    @SuppressWarnings({"MissingPermission"})
    protected void onStart() {
        Crashlytics.log(Log.INFO, TAG, "onStart called");
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        Crashlytics.log(Log.INFO, TAG, "onStop called");
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onResume() {
        Crashlytics.log(Log.INFO, TAG, "onResume called");
        super.onResume();
        mapView.onResume();


        //If mapbox was already loaded, we should call updateUI() here in order to update its data
        if (mapboxMap != null)
            updateUI();
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

    Icon ic_single_spot, ic_typeunknown_spot, ic_took_a_break_spot, ic_waiting_spot, ic_point_on_the_route_spot, ic_arrival_spot = null;
    Icon ic_got_a_ride_spot0, ic_got_a_ride_spot1, ic_got_a_ride_spot2, ic_got_a_ride_spot3, ic_got_a_ride_spot4;

    List<Spot> spotList = new ArrayList<Spot>();

    public void setValues(final List<Spot> list) {
        spotList = list;
    }


    @Override
    public void onPause() {
        Crashlytics.log(Log.INFO, TAG, "onPause was called");
        super.onPause();
        mapView.onPause();

        dismissSnackbar();
        dismissProgressDialog();
    }

    protected static final String SNACKBAR_SHOWED_KEY = "snackbar-showed";


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Crashlytics.log(Log.INFO, TAG, "onSaveInstanceState called");
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
        Crashlytics.log(Log.INFO, TAG, "onDestroy was called");
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Crashlytics.log(Log.WARN, TAG, "onLowMemory was called");
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_maps_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean selectionHandled = false;

        switch (item.getItemId()) {
            case R.id.action_view_list:
                openSpotsListView();
                selectionHandled = true;
                break;
            case R.id.action_new_spot:
                //If mapboxMap was not loaded, we can't track the user location using MapBox.
                // Let's give the user an option to track his location using Google Play services instead, which is done by {@link #TrackLocationBaseActivity}
                if (mapboxMap == null) {
                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getString(R.string.save_spot_button_text))
                            .setMessage(getString(R.string.general_offline_mode_label)
                                    + ". " + getString(R.string.map_error_alert_map_not_loaded_alternative_message))
                            .setPositiveButton(getResources().getString(R.string.map_error_alert_map_not_loaded_positive_button), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    openSpotsListView(true);
                                }
                            })
                            .setNegativeButton(getResources().getString(R.string.general_cancel_option), null).show();
                } else
                    saveSpotButtonHandler(false);
                selectionHandled = true;
                break;

            case R.id.action_toggle_icons:
                shouldDisplayIcons = !shouldDisplayIcons;
                if (shouldDisplayIcons) {
                    fabLocateUser.setVisibility(View.VISIBLE);
                    //fabShowAll.setVisibility(View.VISIBLE);
                    fabZoomIn.setVisibility(View.VISIBLE);
                    fabZoomOut.setVisibility(View.VISIBLE);
                    item.setTitle(getString(R.string.general_hide_icons_label));

                    //Call configureBottomFABButtons to show only the buttons that should be shown
                    configureBottomFABButtons();
                } else {
                    fabLocateUser.setVisibility(View.GONE);
                    //fabShowAll.setVisibility(View.GONE);
                    fabZoomIn.setVisibility(View.GONE);
                    fabZoomOut.setVisibility(View.GONE);
                    fabSpotAction1.setVisibility(View.GONE);
                    fabSpotAction2.setVisibility(View.GONE);
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

    void openSpotsListView(Boolean... shouldShowYouTab) {
        Intent intent = new Intent(getApplicationContext(), MyRoutesActivity.class);
        intent.putExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, true);
        /*//When set to true, shouldShowYouTab will open MyRoutesActivity presenting the tab "You" instead of the tab "List"
        if (shouldShowYouTab.length > 0)
            intent.putExtra(Constants.SHOULD_SHOW_YOU_TAB_KEY, shouldShowYouTab[0]);*/
        startActivity(intent);
        //startActivity(new Intent(getApplicationContext(), MyRoutesActivity.class));
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

    private LatLng positionAt = null;
    private boolean isCameraPositionChangingByCodeRequest = false;


    /**
     * Move the map camera to the given position
     *
     * @param latLng Target location to change to
     * @param zoom   Zoom level to change to
     */
    private void moveCamera(LatLng latLng, long zoom) {
        if (latLng != null) {
            positionAt = latLng;
            isCameraPositionChangingByCodeRequest = true;
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

    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch (keycode) {
            case KeyEvent.KEYCODE_MENU:
                Toast.makeText(this, "HALLO!", Toast.LENGTH_SHORT).show();
                return true;
        }

        return super.onKeyDown(keycode, e);
    }

    Boolean shouldShowOnlyGotARideMarkers = true;

    private ProgressDialog loadingDialog;

    private void showProgressDialog() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(MyMapsActivity.this);
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
                // go through the list in the opposite direction in order to sum up the route's totals from their origin to their destinations
                for (int i = spotList.size() - 1; i >= 0; i--) {
                    Spot spot = spotList.get(i);
                    String markerTitle = "";

                    ExtendedMarkerViewOptions markerViewOptions = new ExtendedMarkerViewOptions()
                            .position(new LatLng(spot.getLatitude(), spot.getLongitude()));

                    if (spot.getId() != null)
                        markerViewOptions.tag(spot.getId().toString());

                    Icon icon = null;

                    //If spot belongs to a route (it's not a single spot)
                    if (spot.getIsPartOfARoute() != null && spot.getIsPartOfARoute()) {

                        //If spot is a hitchhiking spot where the user is waiting for a ride
                        if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot() &&
                                spot.getIsWaitingForARide() != null && spot.getIsWaitingForARide()) {
                            //The spot is where the user is waiting for a ride

                            markerTitle = getString(R.string.map_infoview_spot_type_waiting);
                            icon = ic_waiting_spot;
                            markerViewOptions.spotType(Constants.SPOT_TYPE_WAITING);

                        } else if (spot.getIsDestination() != null && spot.getIsDestination()) {
                            //The spot is a destination

                            markerTitle = getString(R.string.map_infoview_spot_type_destination);
                            icon = ic_arrival_spot;
                            markerViewOptions.spotType(Constants.SPOT_TYPE_DESTINATION);

                            //Center icon
                            markerViewOptions.anchor((float) 0.5, (float) 0.5);
                        } else {
                            if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot()) {
                                //The spot is a hitchhiking spot
                                markerViewOptions.spotType(Constants.SPOT_TYPE_HITCHHIKING_SPOT);

                                switch (spot.getAttemptResult()) {
                                    case Constants.ATTEMPT_RESULT_GOT_A_RIDE:
                                    default:
                                        //The spot is a hitchhiking spot that was already evaluated
                                        icon = getGotARideIconForRoute(trips.size());
                                        markerViewOptions.anchor((float) 0.5, (float) 0.5);
                                        markerTitle = Utils.getRatingOrDefaultAsString(getBaseContext(), spot.getHitchability() != null ? spot.getHitchability() : 0);
                                        break;
                                    case Constants.ATTEMPT_RESULT_TOOK_A_BREAK:
                                        //The spot is a hitchhiking spot that was already evaluated
                                        //icon = ic_took_a_break_spot;
                                        icon = ic_point_on_the_route_spot;
                                        markerViewOptions.anchor((float) 0.5, (float) 0.5);
                                        markerViewOptions.alpha((float) 0.5);
                                        markerTitle = getString(R.string.map_infoview_spot_type_break);
                                        break;
                                   /* default:
                                        //The spot is a hitchhiking spot that was not evaluated yet
                                        //icon = getGotARideIconForRoute(-1);
                                        icon = ic_point_on_the_route_spot;
                                        markerTitle = getString(R.string.map_infoview_spot_type_not_evaluated);
                                        markerViewOptions.anchor((float) 0.5, (float) 0.5);
                                        marker'/ViewOptions.alpha((float) 0.5);
                                        break;*/
                                }

                            } else {
                                //The spot belongs to a route but it's not a hitchhiking spot, neither a destination
                                icon = ic_point_on_the_route_spot;
                                markerViewOptions.spotType(Constants.SPOT_TYPE_POINT_ON_THE_ROUTE);
                                markerViewOptions.anchor((float) 0.5, (float) 0.5);
                                markerViewOptions.alpha((float) 0.5);
                            }

                            if (spots.size() == 0) {
                                //The spot is the origin of a route
                                markerViewOptions.spotType(Constants.SPOT_TYPE_ORIGIN);
                                if (!markerTitle.isEmpty())
                                    markerTitle = getString(R.string.map_infoview_spot_type_origin) + " " + markerTitle;
                                else
                                    markerTitle = getString(R.string.map_infoview_spot_type_origin);
                            }
                        }
                    } else {
                        //This spot doesn't belong to a route (it's a single spot)

                        if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot()) {
                            markerTitle = Utils.getRatingOrDefaultAsString(getBaseContext(), spot.getHitchability() != null ? spot.getHitchability() : 0);

                            icon = ic_single_spot;
                            markerViewOptions.spotType(Constants.SPOT_TYPE_SINGLE_SPOT);
                        } else {
                            icon = ic_point_on_the_route_spot;
                            markerViewOptions.spotType(Constants.SPOT_TYPE_POINT_ON_THE_ROUTE);
                            markerViewOptions.anchor((float) 0.5, (float) 0.5);
                            markerViewOptions.alpha((float) 0.5);
                        }
                    }

                    if (icon != null)
                        markerViewOptions.icon(icon);

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
                    if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot() &&
                            spot.getWaitingTime() != null) {
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

                    //Set hitchability as title
                    markerViewOptions.title(markerTitle.toUpperCase());

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
                        showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
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
            if (MyMapsActivity.this.isFinishing())
                return;

            try {
                mapboxMap.clear();

                updateUISaveButtons();

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
                    }
                }

                //Automatically zoom out to fit all markers only the first time that spots are loaded.
                // Otherwise it can be annoying to loose your zoom when navigating back after editing a spot. In anyways, there's a button to do this zoom if/when the user wish.
                if (shouldZoomToFitAllMarkers) {
                    //If there's more than 30 spots on the list and it's an old version of Android, maybe the device will get too slower when it has all spots
                    // within the map camera, so let's just zoom close to a location. 30 is a random number chosen here.
                    if (spotList.size() > 30 && Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                        //Zoom close to current position or to the last saved position
                        LatLng cameraPositionTo = null;
                        int cameraZoomTo = -1;
                        Location loc = null;
                        try {
                            loc = (locationLayerPlugin != null) ? locationLayerPlugin.getLastKnownLocation() : null;
                        } catch (SecurityException ex) {
                        }

                        if (loc != null) {
                            cameraPositionTo = new LatLng(loc);
                            cameraZoomTo = Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT;
                        } else {
                            //Set start position for map camera: set it to the last spot saved
                            Spot lastAddedSpot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getLastAddedRouteSpot();
                            if (lastAddedSpot != null && lastAddedSpot.getLatitude() != null && lastAddedSpot.getLongitude() != null
                                    && lastAddedSpot.getLatitude() != 0.0 && lastAddedSpot.getLongitude() != 0.0) {
                                cameraPositionTo = new LatLng(lastAddedSpot.getLatitude(), lastAddedSpot.getLongitude());

                                //If at the last added spot the user took a break, then he might be still close to that spot - zoom close to it! Otherwise, we zoom a bit out/farther.
                                if (lastAddedSpot.getAttemptResult() != null && lastAddedSpot.getAttemptResult() == Constants.ATTEMPT_RESULT_TOOK_A_BREAK)
                                    cameraZoomTo = Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT;
                                else
                                    cameraZoomTo = Constants.ZOOM_TO_SEE_FARTHER_DISTANCE;
                            }
                        }
                        moveCamera(cameraPositionTo, cameraZoomTo);
                    } else
                        zoomOutToFitAllMarkers();
                    shouldZoomToFitAllMarkers = false;
                }

            } catch (Exception ex) {
                Crashlytics.logException(ex);
                showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                        "Adding markers failed - " + ex.getMessage()));
            }

            dismissProgressDialog();

            isDrawingAnnotations = false;

        }

    }

    Boolean shouldZoomToFitAllMarkers = true;

    public void saveRegularSpotButtonHandler() {
        saveSpotButtonHandler(false);
    }

    public void saveDestinationSpotButtonHandler() {
        saveSpotButtonHandler(true);
    }

    protected static final String TAG = "map-view-activity";


    /**
     * Handles the Save Spot button and save current location. Does nothing if
     * updates have already been requested.
     */
    public void saveSpotButtonHandler(boolean isDestination) {
        double cameraZoom = -1;
        Spot spot = null;
        if (!mIsWaitingForARide) {
            spot = new Spot();
            spot.setIsHitchhikingSpot(!isDestination);
            spot.setIsDestination(isDestination);
            spot.setIsPartOfARoute(true);
            if (mapboxMap != null) {
                // Keep the same center point of the map.
                // The user will have a locate button to move the camera to his current position if he wants to do that.
                if (mapboxMap.getCameraPosition() != null && mapboxMap.getCameraPosition().target != null) {
                    LatLng selectedLocation = mapboxMap.getCameraPosition().target;

                    spot.setLatitude(selectedLocation.getLatitude());
                    spot.setLongitude(selectedLocation.getLongitude());
                    cameraZoom = mapboxMap.getCameraPosition().zoom;
                }
                /* Location mCurrentLocation = mapboxMap.getMyLocation();
                if (mCurrentLocation != null) {
                    spot.setLatitude(mCurrentLocation.getLatitude());
                    spot.setLongitude(mCurrentLocation.getLongitude());
                    spot.setAccuracy(mCurrentLocation.getAccuracy());
                    spot.setHasAccuracy(mCurrentLocation.hasAccuracy());
                }*/
            }
            Crashlytics.log(Log.INFO, TAG, "Save spot button handler: a new spot is being created.");
        } else {
            spot = mCurrentWaitingSpot;
            Crashlytics.log(Log.INFO, TAG, "Save spot button handler: a spot is being edited.");
        }

        Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);
        intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
        intent.putExtra(Constants.SPOT_BUNDLE_MAP_ZOOM_KEY, cameraZoom);
        startActivity(intent);
    }

    public void gotARideButtonHandler() {
        mCurrentWaitingSpot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        evaluateSpotButtonHandler();
    }

    public void tookABreakButtonHandler() {
        mCurrentWaitingSpot.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        evaluateSpotButtonHandler();
    }

    /**
     * Handles the Got A Ride button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void evaluateSpotButtonHandler() {
        //mCurrentWaitingSpot.setHitchability(findTheOpposit(Math.round(hitchability_ratingbar.getRating())));

        if (mIsWaitingForARide) {
            Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);
            intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, mCurrentWaitingSpot);
            startActivity(intent);
        }
    }

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
