package com.myhitchhikingspots;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.myhitchhikingspots.model.Spot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyMapFragment extends Fragment implements OnMapReadyCallback {
    private MapView mapView;
    private MapboxMap mapboxMap;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Mapbox.getInstance(getContext(), getResources().getString(R.string.mapBoxKey));

        View rootView = inflater.inflate(R.layout.my_map_fragment_layout, container, false);

        mapView = (MapView) rootView.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

/*
        recyclerView = (RecyclerView) rootView.findViewById(R.id.main_activity_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
*/
        return rootView;
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        // Customize map with markers, polylines, etc.
        this.mapboxMap = mapboxMap;

        this.mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                Toast.makeText(getContext(), marker.getTitle(), Toast.LENGTH_LONG).show();
                return true;
            }
        });

        //Load polylines
        new DrawAnnotations().execute();
    }


    @NonNull
    private static String getString(Spot mCurrentSpot) {
        String spotLoc = spotLocationToString(mCurrentSpot).trim();
        if (spotLoc != null && !spotLoc.isEmpty())
            spotLoc = "- " + spotLoc;
        else if (mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null)
            spotLoc = "- (" + mCurrentSpot.getLatitude() + "," + mCurrentSpot.getLongitude() + ")";
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
            Crashlytics.logException(ex);
        }
        return "";
    }

    /*private static String dateTimeToString(Date dt) {
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
    public void onResume() {
        super.onResume();
        mapView.onResume();

        new DrawAnnotations().execute();
    }

    List<Spot> spotList;

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

    private class DrawAnnotations extends AsyncTask<Void, Void, List<List<MarkerViewOptions>>> {
        @Override
        protected List<List<MarkerViewOptions>> doInBackground(Void... voids) {
            List<List<MarkerViewOptions>> trips = new ArrayList<>();
            ArrayList<MarkerViewOptions> spots = new ArrayList<>();

            //The spots are ordered from the last saved ones to the first saved ones, so we need to
            // go through the list in the oposite direction in order to sum up the route's totals from their origin to their destinations
            for (int i = spotList.size() - 1; i >= 0; i--) {
                Spot spot = spotList.get(i);
                String title = SpotListAdapter.dateTimeToString(spot.getStartDateTime()) + getString(spot);
                String snippet = spot.getNote();
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

                    snippet = "(DESTINATION) " + snippet;

                    //markerViewOptions.icon(arrivalIcon);
                } else if (spot.getIsWaitingForARide() != null && spot.getIsWaitingForARide()) {
                    //USER IS WAITING FOR A RIDE

                    snippet = "(WAITING) " + snippet;
                    iconAlpha = (float) 0.5;

                    //markerViewOptions.icon(waitingIcon);
                } else {
                    if (spot.getAttemptResult() != null && spot.getAttemptResult() == Constants.ATTEMPT_RESULT_TOOK_A_BREAK
                            && (spot.getIsWaitingForARide() == null || !spot.getIsWaitingForARide())) {
                        //THIS IS A BREAK SPOT

                        snippet = "(BREAK) " + snippet;
                        iconAlpha = (float) 0.5;
                    } else if (spot.getId() == Constants.USER_CURRENT_LOCATION_SPOTLIST_ID) {
                        //THIS IS THE USER CURRENT LOCATION (not saved)

                        title = USER_CURRENT_LOCATION_TITLE;
                        iconAlpha = (float) 0.5;
                        //markerViewOptions.icon(currentLocationIcon);
                    }
                }


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
