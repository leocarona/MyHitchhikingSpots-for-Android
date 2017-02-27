package com.myhitchhikingspots;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
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
    private FloatingActionButton fabMainActionButton, fabSecondaryActionButton;
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

        fabLocateUser = (FloatingActionButton) findViewById(R.id.location_toggle_fab);
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
                        if (!mRequestingLocationUpdates)
                            enableLocation(true);
                        else
                            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationServices.getLastLocation()), 16));
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

        fabMainActionButton = (FloatingActionButton) findViewById(R.id.fab);
        fabMainActionButton.setOnClickListener(new View.OnClickListener() {
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

        fabSecondaryActionButton = (FloatingActionButton) findViewById(R.id.fabSecondary);
        fabSecondaryActionButton.setOnClickListener(new View.OnClickListener() {
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

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
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


        if (spotList.size() == 0 || (spotList.get(0).getIsDestination() != null && spotList.get(0).getIsDestination()))
            mWillItBeFirstSpotOfARoute = true;
        else
            mWillItBeFirstSpotOfARoute = false;
    }

    public enum pageType {
        NOT_FETCHING_LOCATION,
        WILL_BE_FIRST_SPOT_OF_A_ROUTE, //user sees "save spot" but doesn't see "arrival" button
        WILL_BE_REGULAR_SPOT, //user sees both "save spot" and "arrival" buttons
        WAITING_FOR_A_RIDE //user sees "got a ride" and "take a break" buttons
    }

    private pageType currentPage;

    protected void showCurrentPage() {
        /*if (!mRequestingLocationUpdates)
            fabLocateUser.setImageResource(R.drawable.ic_my_location_24dp);
        else
            fabLocateUser.setImageResource(R.drawable.ic_location_disabled_24dp);*/

        if (currentPage == pageType.WILL_BE_FIRST_SPOT_OF_A_ROUTE || currentPage == pageType.WILL_BE_REGULAR_SPOT) {
            //mWaitingToGetCurrentLocationTextView.setVisibility(View.INVISIBLE);
            //fabLocateUser.setVisibility(View.GONE);

            fabMainActionButton.setImageResource(R.drawable.ic_regular_spot_icon);
            fabMainActionButton.setBackgroundColor(getResources().getColor(R.color.ic_regular_spot_color));
            fabMainActionButton.setVisibility(View.VISIBLE);
        }


        switch (currentPage) {
            case NOT_FETCHING_LOCATION:
            default:
                //mWaitingToGetCurrentLocationTextView.setVisibility(View.VISIBLE);
                fabMainActionButton.setVisibility(View.GONE);
                fabSecondaryActionButton.setVisibility(View.GONE);
                //fabLocateUser.setVisibility(View.VISIBLE);
                break;
            case WILL_BE_FIRST_SPOT_OF_A_ROUTE:
                fabSecondaryActionButton.setVisibility(View.GONE);
                break;
            case WILL_BE_REGULAR_SPOT:
                fabSecondaryActionButton.setImageResource(R.drawable.ic_arrival_icon);
                fabSecondaryActionButton.setBackgroundColor(getResources().getColor(R.color.ic_arrival_color));
                fabSecondaryActionButton.setVisibility(View.VISIBLE);
                break;
            case WAITING_FOR_A_RIDE:
                fabMainActionButton.setImageResource(R.drawable.ic_got_a_ride_spot_icon);
                fabMainActionButton.setBackgroundColor(getResources().getColor(R.color.ic_got_a_ride_color));
                fabSecondaryActionButton.setImageResource(R.drawable.ic_break_spot_icon);
                fabSecondaryActionButton.setBackgroundColor(getResources().getColor(R.color.ic_break_color));

                //fabLocateUser.setVisibility(View.GONE);
                //mWaitingToGetCurrentLocationTextView.setVisibility(View.INVISIBLE);
                fabMainActionButton.setVisibility(View.VISIBLE);
                fabSecondaryActionButton.setVisibility(View.VISIBLE);
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
        mRequestingLocationUpdates = enabled;
    }

    private boolean mRequestingLocationUpdates;

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
                            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location), 16));
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

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        // Customize map with markers, polylines, etc.
        this.mapboxMap = mapboxMap;

        if (locationServices.areLocationPermissionsGranted())
            enableLocation(true);


    /*    mapboxMap.setInfoWindowAdapter(new MapboxMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(@NonNull Marker marker) {

                // The info window layout is created dynamically, parent is the info window
                // container
                LinearLayout parent = new LinearLayout(MapViewActivity.this);
                parent.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                parent.setOrientation(LinearLayout.VERTICAL);

                // Depending on the marker title, the correct image source is used. If you
                // have many markers using different images, extending Marker and
                // baseMarkerOptions, adding additional options such as the image, might be
                // a better choice.
                ImageView countryFlagImage = new ImageView(MapViewActivity.this);
                 /switch (marker.getTitle()) {
                    case "spain":
                        countryFlagImage.setImageDrawable(ContextCompat.getDrawable(
                                MapViewActivity.this, R.drawable.flag_of_spain));
                        break;
                    case "egypt":
                        countryFlagImage.setImageDrawable(ContextCompat.getDrawable(
                                MapViewActivity.this, R.drawable.flag_of_egypt));
                        break;
                    default:
                        // By default all markers without a matching title will use the
                        // Germany flag
                        countryFlagImage.setImageDrawable(ContextCompat.getDrawable(
                                MapViewActivity.this, R.drawable.flag_of_germany));
                        break;
                }/

                // Set the size of the image
                countryFlagImage.setLayoutParams(new android.view.ViewGroup.LayoutParams(150, 100));

                TextView tx = new TextView(MapViewActivity.this);
                tx.setText(marker.getTitle());
                parent.addView(tx);

                TextView tx2 = new TextView(MapViewActivity.this);
                tx2.setText(marker.getSnippet());
                parent.addView(tx2);

                // add the image view to the parent layout
                parent.addView(countryFlagImage);


                return parent;
            }
        });
        */
        this.mapboxMap.setOnInfoWindowClickListener(new MapboxMap.OnInfoWindowClickListener() {
            @Override
            public boolean onInfoWindowClick(@NonNull Marker marker) {
               /* Spot mCurrentWaitingSpot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getCurrentSpot();

                //If the user is currently waiting at a spot and the clicked spot is not the one he's waiting at, show a Toast.
                if (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null &&
                        mCurrentWaitingSpot.getIsWaitingForARide()) {
                    if (mCurrentWaitingSpot.getId() == spot.getId())
                        spot.setAttemptResult(null);
                    else {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.evaluate_running_spot_required), Toast.LENGTH_LONG).show();
                        return false;
                    }
                }

                Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);
                //Maybe we should send mCurrentWaitingSpot on the intent.putExtra so that we don't need to call spot.setAttemptResult(null) ?
                intent.putExtra("Spot", spot);
                startActivityForResult(intent, 1);
                return true;
           */
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
                    intent.putExtra("Spot", spot);
                    startActivityForResult(intent, 1);
                }

                return true;
            }
        });

        /*this.mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                Toast.makeText(getBaseContext(), marker.getTitle(), Toast.LENGTH_LONG).show();
                return false;
            }
        });*/

       /* // If we have the last location of the user, we can move the camera to that position.
        Location lastLocation = locationServices.getLastLocation();
        if (lastLocation != null)
            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16));*/

        //Load polylines
        new DrawAnnotations().execute();
        isMapReady = true;
    }

    boolean isMapReady = false;


    @NonNull
    private static String getString(Spot mCurrentSpot) {
        String spotLoc = spotLocationToString(mCurrentSpot).trim();
        if (spotLoc != null && !spotLoc.isEmpty()) {// spotLoc = "- " + spotLoc;
        } else if (mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null)
            spotLoc = "(" + mCurrentSpot.getLatitude() + ", " + mCurrentSpot.getLongitude() + ")";
        return spotLoc;
    }

    static String locationSeparator = ", ";

    private static String spotLocationToString(Spot spot) {

        ArrayList<String> loc = new ArrayList();
        try {

            if (spot.getCity() != null && !spot.getCity().trim().isEmpty())
                loc.add(spot.getCity().trim());
            if (spot.getState() != null && !spot.getState().trim().isEmpty())
                loc.add(spot.getState().trim());
            if (spot.getCountry() != null && !spot.getCountry().trim().isEmpty())
                loc.add(spot.getCountry().trim());

            return TextUtils.join(locationSeparator, loc);
        } catch (Exception ex) {
            Log.w("spotLocationToString", "Err msg: " + ex.getMessage());

        }
        return "";
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
        mapView.onResume();

        //TODO: Find out why the drawable vector aren't found
      /*
        IconFactory iconFactory = IconFactory.getInstance(getBaseContext());
        Drawable iconDrawable = ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_break_spot_icon);
         if(iconDrawable!=null)
             icon = iconFactory.fromDrawable(iconDrawable);*/

        loadValues();
        updateUISaveButtons();

        if (isMapReady)
            new DrawAnnotations().execute();
    }

    Icon icon = null;
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
        if (mapboxMap != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker : mapboxMap.getMarkers()) {
                builder.include(marker.getPosition());
            }

            Location mCurrentLocation = locationServices.getLastLocation();

            //Add current location to camera bounds
            if (mapboxMap.getMarkers().size() == 0) {
                if (mCurrentLocation != null)
                    mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationServices.getLastLocation()), 16));
            } else {
                if (mCurrentLocation != null)
                    builder.include(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));

                LatLngBounds bounds = builder.build();
                mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 5000);
            }
        }
    }

    private class DrawAnnotations extends AsyncTask<Void, Void, List<List<ExtendedMarkerViewOptions>>> {

        @Override
        protected List<List<ExtendedMarkerViewOptions>> doInBackground(Void... voids) {
            List<List<ExtendedMarkerViewOptions>> trips = new ArrayList<>();
            ArrayList<ExtendedMarkerViewOptions> spots = new ArrayList<>();

            //The spots are ordered from the last saved ones to the first saved ones, so we need to
            // go through the list in the oposite direction in order to sum up the route's totals from their origin to their destinations
            for (int i = spotList.size() - 1; i >= 0; i--) {
                Spot spot = spotList.get(i);
                String title = getString(spot);
                String snippet = "";
                float iconAlpha = (float) 1.0;

                     /*
                        // Create an Icon object for the marker to use
                        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
                        Drawable iconDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.blue_marker);
                        Icon arrivalIcon = iconFactory.fromDrawable(iconDrawable);
                        Icon waitingIcon = iconFactory.fromDrawable(iconDrawable);
                        Icon currentLocationIcon = iconFactory.fromDrawable(iconDrawable);
                    */

                if (spot.getIsDestination() != null && spot.getIsDestination()) {
                    //ARRIVAL SPOT

                    snippet = "(DESTINATION)";

                    //markerViewOptions.icon(arrivalIcon);
                } else if (spot.getIsWaitingForARide() != null && spot.getIsWaitingForARide()) {
                    //USER IS WAITING FOR A RIDE

                    snippet = "(WAITING)";
                    iconAlpha = (float) 0.5;

                    //markerViewOptions.icon(waitingIcon);
                } else {
                    if (spot.getAttemptResult() != null && spot.getAttemptResult() == Constants.ATTEMPT_RESULT_TOOK_A_BREAK &&
                            (spot.getIsWaitingForARide() == null || !spot.getIsWaitingForARide())) {
                        //THIS IS A BREAK SPOT

                        snippet = "(BREAK)";
                        iconAlpha = (float) 0.5;
                    } else if (spot.getId() == Constants.USER_CURRENT_LOCATION_SPOTLIST_ID) {
                        //THIS IS THE USER CURRENT LOCATION (not saved)

                        title = USER_CURRENT_LOCATION_TITLE;
                        iconAlpha = (float) 0.5;
                        //markerViewOptions.icon(currentLocationIcon);
                    } else {
                        snippet = "(" + spot.getWaitingTime() + "min)";
                    }
                }

                snippet = dateTimeToString(spot.getStartDateTime()) + " - " + snippet + " " + spot.getNote();

                Icon icon = null;
                // Create an Icon object for the marker to use
                IconFactory iconFactory = IconFactory.getInstance(MapViewActivity.this);
                if (spot.getIsWaitingForARide()) {
                    // Drawable iconDrawable = ContextCompat.getDrawable(MapViewActivity.this, R.drawable.default_marker);
                    // icon = iconFactory.fromDrawable(iconDrawable);
                } else if (spot.getIsDestination()) {
                    Drawable iconDrawable = ContextCompat.getDrawable(MapViewActivity.this, R.drawable.ic_arrival_icon);
                    icon = iconFactory.fromDrawable(iconDrawable);
                } else if (spot.getAttemptResult() == Constants.ATTEMPT_RESULT_TOOK_A_BREAK) {
//                    Drawable iconDrawable = ContextCompat.getDrawable(MapViewActivity.this, R.drawable.ic_break_spot_icon);
                    // icon = iconFactory.fromDrawable(iconDrawable);
                } else {
                    //Drawable iconDrawable = ContextCompat.getDrawable(MapViewActivity.this, R.drawable.ic_place_black_24dp);
                    //icon = iconFactory.fromDrawable(iconDrawable);
                }


                // Customize map with markers, polylines, etc.
                ExtendedMarkerViewOptions markerViewOptions = new ExtendedMarkerViewOptions()
                        .position(new LatLng(spot.getLatitude(), spot.getLongitude()))
                        .title(title)
                        .snippet(snippet)
                        .tag(spot.getId().toString());
                //.icon(icon);
                //.alpha(iconAlpha);

                if (icon != null)
                    markerViewOptions.icon(icon);

                spots.add(markerViewOptions);

                if (spot.getIsDestination() != null && spot.getIsDestination() || i == 0) {
                    trips.add(spots);
                    spots = new ArrayList<>();
                }
            }

            return trips;
        }

        String USER_CURRENT_LOCATION_TITLE = "You are here";

        @Override
        protected void onPostExecute(List<List<ExtendedMarkerViewOptions>> trips) {
            super.onPostExecute(trips);
            mapboxMap.clear();

            for (int lc = 0; lc < trips.size(); lc++) {
                List<ExtendedMarkerViewOptions> spots = trips.get(lc);

                PolylineOptions line = new PolylineOptions()
                        .width(2)
                        .color(Color.parseColor(getPolylineColor(lc)));

                for (BaseMarkerOptions spot : spots) {

                    //DrawableCompat.setTint(iconDrawable, Color.WHITE);
                    //Add marker to map
                    mapboxMap.addMarker(spot);

                    // Draw polyline on map
                    if (spot.getMarker().getTitle() != USER_CURRENT_LOCATION_TITLE)
                        line.add(spot.getMarker().getPosition());
                }

                if (spotList.size() > 1) {
                    //Add polylines to map
                    mapboxMap.addPolyline(line);
                }
            }

            zoomOutToFitAllMarkers();
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
            spot.setLatitude(mCurrentLocation.getLatitude());
            spot.setLongitude(mCurrentLocation.getLongitude());
            spot.setAccuracy(mCurrentLocation.getAccuracy());
            spot.setHasAccuracy(mCurrentLocation.hasAccuracy());
            Log.i(TAG, "Save spot button handler: a new spot is being created.");
        } else {
            spot = mCurrentWaitingSpot;
            Log.i(TAG, "Save spot button handler: a spot is being edited.");
        }

        Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);
        intent.putExtra("Spot", spot);
        startActivityForResult(intent, 1);
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
            intent.putExtra("Spot", mCurrentWaitingSpot);
            startActivityForResult(intent, 1);
            //mIsWaitingForARide = false;
            //updateUISaveButtons();
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
