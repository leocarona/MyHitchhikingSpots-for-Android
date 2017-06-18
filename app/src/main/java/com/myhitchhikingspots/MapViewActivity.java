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
import com.mapbox.mapboxsdk.Mapbox;
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
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
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

public class MapViewActivity extends BaseActivity implements OnMapReadyCallback {
    private MapView mapView;
    private MapboxMap mapboxMap;
    //private LocationEngine locationEngine;
    //private LocationEngineListener locationEngineListener;
    private FloatingActionButton fabLocateUser, fabShowAll;
    private FloatingActionButton fabSpotAction1, fabSpotAction2;
    //private TextView mWaitingToGetCurrentLocationTextView;
    private CoordinatorLayout coordinatorLayout;

    boolean wasSnackbarShown;

    //WARNING: in order to use BaseActivity the method onCreate must be overridden
    // calling first setContentView to the view you want to use
    // and then calling super.onCreate AFTER setContentView.
    // Please always make sure this is been done!
    protected void onCreate(Bundle savedInstanceState) {

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getResources().getString(R.string.mapBoxKey));

        setContentView(R.layout.mapview_master_layout);

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
                    if (mapboxMap.getMyLocation() != null)
                        moveCamera(new LatLng(mapboxMap.getMyLocation()));
                    else
                        locateUser();
                }
            }
        });

        fabShowAll = (FloatingActionButton) findViewById(R.id.fab_show_all);
        fabShowAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null) {
                    zoomOutToFitAllMarkers();
                }
            }
        });

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

        // Get the location engine object for later use.
        //locationEngine = LocationSource.getLocationEngine(this);
        //locationEngine.activate();

        mapView = (MapView) findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        loadMarkerIcons();


        moveCameraToFirstLocationReceived = new MapboxMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (location != null) {
                    if (!wasFirstLocationReceived) {
                        updateUISaveButtons();
                        wasFirstLocationReceived = true;
                    }

                    mapboxMap.setOnMyLocationChangeListener(null);

                    //Place the map camera at the received GPS position
                    moveCamera(new LatLng(location.getLatitude(), location.getLongitude()), Constants.KEEP_ZOOM_LEVEL);
                }
            }
        };

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
    }

    private static final int PERMISSIONS_LOCATION = 0;

    void locateUser() {
        // Check if user has granted location permission
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            Snackbar.make(coordinatorLayout, getResources().getString(R.string.waiting_for_gps), Snackbar.LENGTH_LONG)
                    .setAction("enable", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MapViewActivity.this, new String[]{
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
                        }
                    }).show();
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.waiting_for_gps), Toast.LENGTH_SHORT).show();
            // Enable the location layer on the map
            if (!mapboxMap.isMyLocationEnabled())
                mapboxMap.setMyLocationEnabled(true);
            //Place the map camera at the next GPS position that we receive
            mapboxMap.setOnMyLocationChangeListener(null);
            mapboxMap.setOnMyLocationChangeListener(moveCameraToFirstLocationReceived);
        }
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

    MapboxMap.OnMyLocationChangeListener moveCameraToFirstLocationReceived;

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locateUser();
            }
        }
    }


    public void loadValues() {
        MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
        DaoSession daoSession = appContext.getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();

        //TODO: Check if this query is really helping us to detect if isWaitingForARide after we've added IsPartOfARoute
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
        //this.mapboxMap.getMyLocationViewSettings().setPadding(0, 500, 0, 0);
        this.mapboxMap.getMyLocationViewSettings().setForegroundTintColor(ContextCompat.getColor(getBaseContext(), R.color.mapbox_my_location_ring));//Color.parseColor("#56B881")

        // Enable the location layer on the map
        if (PermissionsManager.areLocationPermissionsGranted(MapViewActivity.this) && !mapboxMap.isMyLocationEnabled())
            mapboxMap.setMyLocationEnabled(true);

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
        markerViewManager.addMarkerViewAdapter(new ExtendedMarkerViewAdapter(MapViewActivity.this));

        updateUI();
    }

    SharedPreferences prefs;

    void updateUI() {
        Crashlytics.log(Log.INFO, TAG, "updateUI was called");

        if (!Utils.isNetworkAvailable(this) && !Utils.shouldLoadCurrentView(prefs)) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.map_error_alert_map_not_loaded_title))
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
            if (spot.getGpsResolved() != null && spot.getGpsResolved()) {
                if (spot.getCity() != null && !spot.getCity().trim().isEmpty())
                    loc.add(spot.getCity().trim());
                if (spot.getState() != null && !spot.getState().trim().isEmpty())
                    loc.add(spot.getState().trim());
                if (spot.getCountry() != null && !spot.getCountry().trim().isEmpty())
                    loc.add(spot.getCountry().trim());
            }

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

    Icon ic_single_spot, ic_typeunknown_spot, ic_took_a_break_spot, ic_waiting_spot, ic_arrival_spot = null;
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

        if (snackbar != null)
            snackbar.dismiss();
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
        getMenuInflater().inflate(R.menu.mapview_menu, menu);
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
                            .setMessage(getString(R.string.map_error_alert_map_not_loaded_title)
                                    + ". To fetch your location we need the map which was not loaded. But you can still try to save a new spot by using the feature that doesn't use maps.")
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
        }

        if (selectionHandled)
            return true;
        else
            return super.onOptionsItemSelected(item);
    }

    void openSpotsListView(Boolean... shouldShowYouTab) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, true);
        //When set to true, shouldShowYouTab will open MainActivity presenting the tab "You" instead of the tab "List"
        intent.putExtra(Constants.SHOULD_SHOW_YOU_TAB_KEY, shouldShowYouTab);
        startActivity(intent);
        //startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

    protected void zoomOutToFitAllMarkers() {
        try {
            if (mapboxMap != null) {
                Location mCurrentLocation = mapboxMap.getMyLocation();
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

    private class DrawAnnotations extends AsyncTask<Void, Void, List<List<ExtendedMarkerViewOptions>>> {
        private final ProgressDialog dialog = new ProgressDialog(MapViewActivity.this);

        @Override
        protected void onPreExecute() {
            this.dialog.setIndeterminate(true);
            this.dialog.setCancelable(false);
            this.dialog.setMessage(getResources().getString(R.string.map_loading_dialog));
            this.dialog.show();
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
                    String spotType = "";

                    ExtendedMarkerViewOptions markerViewOptions = new ExtendedMarkerViewOptions()
                            .position(new LatLng(spot.getLatitude(), spot.getLongitude()));

                    if (spot.getId() != null)
                        markerViewOptions.tag(spot.getId().toString());

                    if (spot.getIsDestination() != null && spot.getIsDestination()) {
                        //AT THIS SPOT USER HAS ARRIVED TO HIS DESTINATION

                        spotType = getResources().getString(R.string.map_infoview_spot_type_destination);
                        markerViewOptions.icon(ic_arrival_spot);
                        markerViewOptions.spotType(Constants.SPOT_TYPE_DESTINATION);

                        //Center icon
                        markerViewOptions.anchor((float) 0.5, (float) 0.5);
                    } else if (spot.getIsWaitingForARide() != null && spot.getIsWaitingForARide()) {
                        //AT THIS SPOT USER IS WAITING FOR A RIDE

                        spotType = getResources().getString(R.string.map_infoview_spot_type_waiting);
                        markerViewOptions.icon(ic_waiting_spot);
                        markerViewOptions.spotType(Constants.SPOT_TYPE_WAITING);

                    } else if (spot.getAttemptResult() != null && spot.getAttemptResult() > 0) {
                        if (spot.getAttemptResult() == Constants.ATTEMPT_RESULT_TOOK_A_BREAK) {
                            //AT THIS SPOT USER TOOK A BREAK SPOT

                            spotType = getResources().getString(R.string.map_infoview_spot_type_break);
                            markerViewOptions.icon(ic_took_a_break_spot);
                            markerViewOptions.spotType(Constants.SPOT_TYPE_TOOK_A_BREAK);

                            //Center icon
                            markerViewOptions.anchor((float) 0.5, (float) 0.5);
                        } else if (spot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE) {
                            //AT THIS SPOT USER GOT A RIDE

                            if (spot.getIsPartOfARoute() != null && spot.getIsPartOfARoute())
                                markerViewOptions.icon(getGotARideIconForRoute(trips.size()));
                            else
                                markerViewOptions.icon(ic_single_spot);

                            markerViewOptions.spotType(Constants.SPOT_TYPE_GOT_A_RIDE);
                        }
                    } else {
                        markerViewOptions.icon(getGotARideIconForRoute(-1));
                    }

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
                    if (spot.getWaitingTime() != null && (spot.getIsDestination() == null || !spot.getIsDestination())) {
                        if (!secondLine.isEmpty())
                            secondLine += " ";
                        secondLine += "(" + SpotListAdapter.getWaitingTimeAsString(spot.getWaitingTime()) + ")";
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
                    if (!spotType.isEmpty())
                        title = spotType;
                    else if (spot.getHitchability() != null && spot.getHitchability() > 0)
                        title = Utils.getRatingAsString(getBaseContext(), Utils.findTheOpposite(spot.getHitchability()));

                    if (title.isEmpty())
                        title = "Not evaluated";

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

                zoomOutToFitAllMarkers();

            } catch (Exception ex) {
                Crashlytics.logException(ex);
                showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                        "Adding markers failed - " + ex.getMessage()));
            }

            this.dialog.dismiss();

            isDrawingAnnotations = false;

        }

    }


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
        Spot spot = null;
        if (!mIsWaitingForARide) {
            spot = new Spot();
            spot.setIsDestination(isDestination);
            spot.setIsPartOfARoute(true);
            if (mapboxMap != null) {
                Location mCurrentLocation = mapboxMap.getMyLocation();
                if (mCurrentLocation != null) {
                    spot.setLatitude(mCurrentLocation.getLatitude());
                    spot.setLongitude(mCurrentLocation.getLongitude());
                    spot.setAccuracy(mCurrentLocation.getAccuracy());
                    spot.setHasAccuracy(mCurrentLocation.hasAccuracy());
                }
            }
            Crashlytics.log(Log.INFO, TAG, "Save spot button handler: a new spot is being created.");
        } else {
            spot = mCurrentWaitingSpot;
            Crashlytics.log(Log.INFO, TAG, "Save spot button handler: a spot is being edited.");
        }

        Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);
        intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
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
