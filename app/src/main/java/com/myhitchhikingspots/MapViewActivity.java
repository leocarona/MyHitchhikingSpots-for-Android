package com.myhitchhikingspots;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewManager;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.location.LocationServices;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapViewActivity extends BaseActivity implements OnMapReadyCallback {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private LocationServices locationServices;
    private static final int PERMISSIONS_LOCATION = 0;
    private FloatingActionButton fabLocateUser, fabShowAll;
    private FloatingActionButton fabSpotAction1, fabSpotAction2;
    //private TextView mWaitingToGetCurrentLocationTextView;
    private CoordinatorLayout coordinatorLayout;

    //WARNING: in order to use BaseActivity the method onCreate must be overridden
    // calling first setContentView to the view you want to use
    // and then calling super.onCreate AFTER setContentView.
    // Please always make sure this is been done!
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.mapview_master_layout);

        //mWaitingToGetCurrentLocationTextView = (TextView) findViewById(R.id.waiting_location_textview);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        fabLocateUser = (FloatingActionButton) findViewById(R.id.fab_locate_user);
        fabLocateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null) {
                    // Check if user has granted location permission
                    if (!locationServices.areLocationPermissionsGranted()) {
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
                        if (!mapboxMap.isMyLocationEnabled())
                            enableLocation(true);
                        else if (locationServices.getLastLocation() != null)
                            moveCamera(new LatLng(locationServices.getLastLocation()));
                        else
                            Toast.makeText(getBaseContext(), getResources().getString(R.string.waiting_for_gps), Toast.LENGTH_LONG).show();
                    }
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

        locationServices = LocationServices.getLocationServices(MapViewActivity.this);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        MapboxAccountManager.start(getApplicationContext(), getResources().getString(R.string.mapBoxKey));

        mapView = (MapView) findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        loadMarkerIcons();

        if (!isNetworkAvailable()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.map_error_alert_map_not_loaded_title))
                    .setMessage(getResources().getString(R.string.map_error_alert_map_not_loaded_message))
                    .setPositiveButton(getResources().getString(R.string.map_error_alert_map_not_loaded_positive_button), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.map_error_alert_map_not_loaded_negative_button), null)
                    .show();
        }

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
    }

    private void loadMarkerIcons() {
        Context context = new ContextThemeWrapper(getBaseContext(), R.style.Theme_Base_NoActionBar);
        IconFactory iconFactory = IconFactory.getInstance(MapViewActivity.this);
        //DrawableCompat.setTint(iconDrawable, Color.WHITE);
        FloatingActionButton img = new FloatingActionButton(context);

        try {
            //Got a ride at this spot
            img.setImageResource(R.drawable.ic_marker_got_a_ride_24dp);
            Drawable d1 = img.getDrawable();
            ic_got_a_ride_spot = iconFactory.fromDrawable(d1);
        } catch (Exception ex) {
        }

        try {
            //Took a break at this spot
            img.setImageResource(R.drawable.ic_break_spot_icon);
            Drawable d1 = img.getDrawable();
            ic_took_a_break_spot = iconFactory.fromDrawable(d1);
        } catch (Exception ex) {
        }

        try {
            //Is waiting at this spot
            img.setImageResource(R.drawable.ic_marker_waiting_for_a_ride_24dp);
            Drawable d1 = img.getDrawable();
            ic_waiting_spot = iconFactory.fromDrawable(d1);
        } catch (Exception ex) {
        }

        try {
            //Arrival spot
            img.setImageResource(R.drawable.ic_arrival_icon);
            Drawable d1 = img.getDrawable();
            ic_arrival_spot = iconFactory.fromDrawable(d1);
        } catch (Exception ex) {
        }
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

    protected void showCurrentPage() {
        if (currentPage == pageType.WILL_BE_FIRST_SPOT_OF_A_ROUTE || currentPage == pageType.WILL_BE_REGULAR_SPOT) {
            fabSpotAction1.setImageResource(R.drawable.ic_regular_spot_icon);
            fabSpotAction1.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ic_regular_spot_color)));
            fabSpotAction1.setVisibility(View.VISIBLE);
        }


        switch (currentPage) {
            case NOT_FETCHING_LOCATION:
            default:
                fabSpotAction1.setVisibility(View.GONE);
                fabSpotAction2.setVisibility(View.GONE);
                break;
            case WILL_BE_FIRST_SPOT_OF_A_ROUTE:
                fabSpotAction2.setVisibility(View.GONE);
                break;
            case WILL_BE_REGULAR_SPOT:
                fabSpotAction2.setImageResource(R.drawable.ic_arrival_icon);
                fabSpotAction2.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ic_arrival_color)));
                fabSpotAction2.setVisibility(View.VISIBLE);
                break;
            case WAITING_FOR_A_RIDE:
                fabSpotAction1.setImageResource(R.drawable.ic_got_a_ride_spot_icon);
                fabSpotAction1.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ic_got_a_ride_color)));
                fabSpotAction2.setImageResource(R.drawable.ic_break_spot_icon);
                fabSpotAction2.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.ic_break_color)));
                fabSpotAction1.setVisibility(View.VISIBLE);
                fabSpotAction2.setVisibility(View.VISIBLE);
                break;
        }
    }

    protected void updateUISaveButtons() {
        //If it's not waiting for a ride, show only ('get location' switch, 'current location' panel and 'save spot' button)
        // { If it's first spot of route, hide 'arrived' button
        //   else, show 'arrived' button }
        //If it's waiting for a ride, show only ('rating' panel, 'got a ride' and 'take a break' buttons)
        //If 'get location' switch is set to Off or (googleApiClient is null or disconnected, or currentLocation is null)

        //If it's not waiting for a ride
        if (!mIsWaitingForARide) {
            /*if (!locationServices.areLocationPermissionsGranted() || locationServices.getLastLocation() == null
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

        showCurrentPage();
    }

    /**
     * Represents a geographical location.
     */
    boolean wasFirstLocationReceived = false;

    private void enableLocation(boolean enabled) {
        // Enable or disable the location layer on the map
        mapboxMap.setMyLocationEnabled(enabled);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationServices.addLocationListener(new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (location != null) {
                            if (!wasFirstLocationReceived) {
                                updateUISaveButtons();
                                wasFirstLocationReceived = true;
                            }

                            // Move the map camera to where the user location is and then remove the
                            // listener so the camera isn't constantly updating when the user location
                            // changes. When the user disables and then enables the location again, this
                            // listener is registered again and will adjust the camera once again.
                            moveCamera(new LatLng(location));
                            locationServices.removeLocationListener(this);
                        }
                    }
                });
                enableLocation(true);
            }
        }
    }

    protected List<Spot> getSpotListWithCurrentLocation() {
        //If user isn't waiting for a ride, add the the current location to the list so that it's included on the map
        List<Spot> newList = new ArrayList<>();

        /*
        Spot mCurrentSpot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getCurrentSpot();

        //As of December 2016, if the user is waiting for a ride we stop fetching his location. So we better don't try to add it to the map.
        if (mCurrentSpot == null || mCurrentSpot.getIsWaitingForARide() == null || !mCurrentSpot.getIsWaitingForARide()) {
            if (mCurrentLocation != null) {
                Spot myLocationSpot = new Spot();
                myLocationSpot.setId(Constants.USER_CURRENT_LOCATION_SPOTLIST_ID);
                myLocationSpot.setLatitude(mCurrentLocation.getLatitude());
                myLocationSpot.setLongitude(mCurrentLocation.getLongitude());
                newList.add(myLocationSpot);
            }
        }*/

        newList.addAll(spotList);
        return newList;
    }


    public void loadValues() {
        MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
        DaoSession daoSession = appContext.getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();
        spotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();

        setValues(spotList, appContext.getCurrentSpot());
        setValues(getSpotListWithCurrentLocation());
    }

    //onMapReady is called after onResume()
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        Log.i("tracking-map", "mapReady called");
        // Customize map with markers, polylines, etc.
        this.mapboxMap = mapboxMap;

        if (locationServices.areLocationPermissionsGranted())
            enableLocation(true);

        this.mapboxMap.setOnInfoWindowClickListener(new MapboxMap.OnInfoWindowClickListener() {
            @Override
            public boolean onInfoWindowClick(@NonNull Marker marker) {
                ExtendedMarkerView myMarker = (ExtendedMarkerView) marker;
                Spot spot = null;
                for (Spot spot2 :
                        spotList) {
                    if (spot2.getId().toString() == myMarker.getTag()) {
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
                }

                return true;
            }
        });

        final MarkerViewManager markerViewManager = mapboxMap.getMarkerViewManager();
        // if you want to customise a ViewMarker you need to extend ViewMarker and provide an adapter implementation
        // set adapters for child classes of ViewMarker
        markerViewManager.addMarkerViewAdapter(new ExtendedMarkerViewAdapter(MapViewActivity.this));

        updateUI();
    }

    void updateUI() {
        Log.i("tracking-map", "updateUI was called");

        loadValues();
        updateUISaveButtons();

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
            Log.w(TAG, "Generating a string for the spot's address has failed", ex);
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

    private static String dateTimeToString(Date dt) {
        SimpleDateFormat res;

        String dateFormat = "dd/MMM', 'HH:mm";
        /*if (Locale.getDefault() == Locale.US)
            dateFormat = "MMM/dd', 'HH:mm";*/

        try {
            res = new SimpleDateFormat(dateFormat);
            return res.format(dt);
        } catch (Exception ex) {
            Log.w("dateTimeToString", "Err msg: " + ex.getMessage());
        }

        return "";
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.i("tracking-map", "onResume called");
        mapView.onResume();


        //If mapbox was already loaded, we should call updateUI() here in order to update its data
        if (mapboxMap != null)
            updateUI();
    }

  /*  @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check which request we're responding to
        if (requestCode == SAVE_SPOT_REQUEST || requestCode == EDIT_SPOT_REQUEST) {
            // Make sure the request was successful
            if (resultCode > RESULT_FIRST_USER)
                updateUI();
        }
    }*/

    Icon ic_got_a_ride_spot, ic_took_a_break_spot, ic_waiting_spot, ic_arrival_spot = null;
    List<Spot> spotList = new ArrayList<Spot>();

    public void setValues(final List<Spot> list) {
        spotList = list;
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
        getMenuInflater().inflate(R.menu.mapview_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void zoomOutToFitAllMarkers() {
        try {
            if (mapboxMap != null) {
                Location mCurrentLocation = locationServices.getLastLocation();
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
                    mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120), 5000);
                }
            }

        } catch (Exception ex) {
            Log.e(TAG, "Show all markers failed", ex);
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

        @Override
        protected List<List<ExtendedMarkerViewOptions>> doInBackground(Void... voids) {
            try {
                isDrawingAnnotations = true;

                List<List<ExtendedMarkerViewOptions>> trips = new ArrayList<>();
                ArrayList<ExtendedMarkerViewOptions> spots = new ArrayList<>();

                //The spots are ordered from the last saved ones to the first saved ones, so we need to
                // go through the list in the oposite direction in order to sum up the route's totals from their origin to their destinations
                for (int i = spotList.size() - 1; i >= 0; i--) {
                    Spot spot = spotList.get(i);
                    String snippet = "";

                    ExtendedMarkerViewOptions markerViewOptions = new ExtendedMarkerViewOptions()
                            .tag(spot.getId().toString())
                            .title(getString(spot))
                            .position(new LatLng(spot.getLatitude(), spot.getLongitude()));


                    if (spot.getIsDestination() != null && spot.getIsDestination()) {
                        //AT THIS SPOT USER HAS ARRIVED TO HIS DESTINATION

                        snippet = getResources().getString(R.string.map_infoview_spot_type_destination);
                        markerViewOptions.icon(ic_arrival_spot);

                        //Center icon
                        markerViewOptions.anchor((float) 0.5, (float) 0.5);
                    } else if (spot.getIsWaitingForARide() != null && spot.getIsWaitingForARide()) {
                        //AT THIS SPOT USER IS WAITING FOR A RIDE

                        snippet = getResources().getString(R.string.map_infoview_spot_type_waiting);
                        markerViewOptions.icon(ic_waiting_spot);
                    } else {
                        if (spot.getAttemptResult() != null)
                            if (spot.getAttemptResult() == Constants.ATTEMPT_RESULT_TOOK_A_BREAK) {
                                //AT THIS SPOT USER TOOK A BREAK SPOT

                                snippet = getResources().getString(R.string.map_infoview_spot_type_break);
                                markerViewOptions.icon(ic_took_a_break_spot);

                                //Center icon
                                markerViewOptions.anchor((float) 0.5, (float) 0.5);
                            } else if (spot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE) {
                                //AT THIS SPOT USER GOT A RIDE

                                int waitingTime = 0;
                                if (spot.getWaitingTime() != null)
                                    spot.getWaitingTime();
                                snippet = String.format(getResources().getString(R.string.map_infoview_spot_type_regular), waitingTime);
                                markerViewOptions.icon(ic_got_a_ride_spot);
                            }
                    }

                    String note = "";
                    if (spot.getNote() != null)
                        note = " " + spot.getNote();

                    // Customize map with markers, polylines, etc.
                    markerViewOptions.snippet(dateTimeToString(spot.getStartDateTime()) + " - " + snippet + note);

                    spots.add(markerViewOptions);

                    if (spot.getIsDestination() != null && spot.getIsDestination() || i == 0) {
                        trips.add(spots);
                        spots = new ArrayList<>();
                    }
                }

                return trips;
            } catch (Exception ex) {
                Log.e(TAG, "Loaded spots failed", ex);
                showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                        "Loading spots failed - " + ex.getMessage()));
            }
            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<List<ExtendedMarkerViewOptions>> trips) {
            super.onPostExecute(trips);
            try {
                mapboxMap.clear();

                for (int lc = 0; lc < trips.size(); lc++) {
                    List<ExtendedMarkerViewOptions> spots = trips.get(lc);

                    PolylineOptions line = new PolylineOptions()
                            .width(2)
                            .color(Color.parseColor(getPolylineColor(lc)));

                    for (ExtendedMarkerViewOptions spot : spots) {

                        //Add marker to map
                        mapboxMap.addMarker(spot);

                        //Add polyline connecting this marker
                        line.add(spot.getPosition());
                    }

                    if (spotList.size() > 1) {
                        //Add polylines to map
                        mapboxMap.addPolyline(line);
                    }
                }

                zoomOutToFitAllMarkers();
            } catch (Exception ex) {
                Log.e(TAG, "Adding markers failed", ex);
                showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                        "Adding markers failed - " + ex.getMessage()));
            }

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
            Location mCurrentLocation = locationServices.getLastLocation();
            if (mCurrentLocation != null) {
                spot.setLatitude(mCurrentLocation.getLatitude());
                spot.setLongitude(mCurrentLocation.getLongitude());
                spot.setAccuracy(mCurrentLocation.getAccuracy());
                spot.setHasAccuracy(mCurrentLocation.hasAccuracy());
            }
            Log.i(TAG, "Save spot button handler: a new spot is being created.");
        } else {
            spot = mCurrentWaitingSpot;
            Log.i(TAG, "Save spot button handler: a spot is being edited.");
        }

        Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);
        intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
        startActivityForResult(intent, SAVE_SPOT_REQUEST);
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
            startActivityForResult(intent, EDIT_SPOT_REQUEST);
        }
    }

    private String getPolylineColor(int routeIndex) {
        String polylineColor = "";

        if (routeIndex % 2 == 0)
            polylineColor = "#85cf3a";
        else
            polylineColor = "#3bb2d0";

                /*switch (routeIndex) {
                case 0:
                    polylineColor = "#3bb2d0";
                    break;
                case 1:
                    polylineColor = "#3bb2d0";
                    break;
                case 2:
                    polylineColor = "#3bb2d0";
                    break;
                case 3:
                    polylineColor = "#3bb2d0";
                    break;
                case 4:
                    polylineColor = "#3bb2d0";
                    break;
                case 5:
                    polylineColor = "#3bb2d0";
                    break;
                case 6:
                    polylineColor = "#3bb2d0";
                    break;
                case 7:
                    polylineColor = "#3bb2d0";
                    break;
                case 8:
                    polylineColor = "#3bb2d0";
                    break;
                case 9:
                    polylineColor = "#3bb2d0";
                    break;
                case 10:
                    polylineColor = "#3bb2d0";
                    break;
                case 11:
                    polylineColor = "#3bb2d0";
                    break;
                default:
                    polylineColor = "#000000";
                    break;
            }*/

        return polylineColor;
    }
}
