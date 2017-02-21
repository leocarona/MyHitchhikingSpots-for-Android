package com.myhitchhikingspots;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
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
import java.util.Locale;

public class MapViewActivity extends BaseActivity implements OnMapReadyCallback {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private LocationServices locationServices;

    //WARNING: in order to use BaseActivity the method onCreate must be overridden
    // calling first setContentView to the view you want to use
    // and then calling super.onCreate AFTER setContentView.
    // Please always make sure this is been done!
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.mapview_master_layout);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                //startActivity(new Intent(getApplicationContext(), MyLocationFragment.class));
            }
        });

        //locationServices = LocationServices.getLocationServices(BasicUserLocation.this);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        MapboxAccountManager.start(this, getResources().getString(R.string.mapBoxKey));


        mapView = (MapView) findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        loadValues();
        setValues(getSpotListWithCurrentLocation());

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
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


    // Spot currentSpot;

    public void loadValues() {
        MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
        DaoSession daoSession = appContext.getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();
        spotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();
        // currentSpot = appContext.getCurrentSpot();
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        // Customize map with markers, polylines, etc.
        this.mapboxMap = mapboxMap;

        /*this.mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                Toast.makeText(getBaseContext(), marker.getTitle(), Toast.LENGTH_LONG).show();
                return false;
            }
        });*/

        //Load polylines
        //todo: make spotListChanged be called when list is changed!
        new DrawAnnotations().execute();
    }


    @NonNull
    private static String getString(Spot mCurrentSpot) {
        String spotLoc = spotLocationToString(mCurrentSpot).trim();
        if (spotLoc != null && !spotLoc.isEmpty()) {// spotLoc = "- " + spotLoc;
        }else if (mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null)
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

        new DrawAnnotations().execute();
    }

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

    private class DrawAnnotations extends AsyncTask<Void, Void, List<List<MarkerViewOptions>>> {
        @Override
        protected List<List<MarkerViewOptions>> doInBackground(Void... voids) {
            List<List<MarkerViewOptions>> trips = new ArrayList<>();
            ArrayList<MarkerViewOptions> spots = new ArrayList<>();

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
                    //TODO: make this check in a not hardcoded way!
                    if (spot.getAttemptResult() != null && spot.getAttemptResult() == 2 && (spot.getIsWaitingForARide() == null || !spot.getIsWaitingForARide())) {
                        //THIS IS A BREAK SPOT

                        snippet = "(BREAK)";
                        iconAlpha = (float) 0.5;
                    } else if (spot.getId() == Constants.USER_CURRENT_LOCATION_SPOTLIST_ID) {
                        //THIS IS THE USER CURRENT LOCATION (not saved)

                        title = USER_CURRENT_LOCATION_TITLE;
                        iconAlpha = (float) 0.5;
                        //markerViewOptions.icon(currentLocationIcon);
                    }
                }

                snippet = dateTimeToString(spot.getStartDateTime()) + " - " + snippet + " " + spot.getNote();


                // Customize map with markers, polylines, etc.
                MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                        .position(new LatLng(spot.getLatitude(), spot.getLongitude()))
                        .title(title)
                        .snippet(snippet)
                        .alpha(iconAlpha);

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
        protected void onPostExecute(List<List<MarkerViewOptions>> trips) {
            super.onPostExecute(trips);
            mapboxMap.clear();

            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (int lc = 0; lc < trips.size(); lc++) {
                List<MarkerViewOptions> spots = trips.get(lc);

                PolylineOptions line = new PolylineOptions()
                        .width(2)
                        .color(Color.parseColor(getPolylineColor(lc)));

                for (MarkerViewOptions spot : spots) {
                    //Add marker to map
                    mapboxMap.addMarker(spot);

                    // Draw polyline on map
                    if (spot.getTitle() != USER_CURRENT_LOCATION_TITLE)
                        line.add(spot.getPosition());

                    boundsBuilder.include(spot.getPosition());
                }

                if (spotList.size() > 1) {
                    //Add polylines to map
                    mapboxMap.addPolyline(line);
                }
            }

            if (spotList.size() > 1) {
                LatLngBounds latLngBounds = boundsBuilder.build();
                mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100), 5000);
            } else {
                mapboxMap.easeCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(spotList.get(0).getLatitude(), spotList.get(0).getLongitude()), 13), 5000);
            }
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
