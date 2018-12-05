package com.myhitchhikingspots;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.JsonObject;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Route;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;
import com.myhitchhikingspots.utilities.ExtendedMarkerView;
import com.myhitchhikingspots.utilities.ExtendedMarkerViewOptions;
import com.myhitchhikingspots.utilities.IconUtils;
import com.myhitchhikingspots.utilities.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

public class MyMapsFragment extends Fragment implements OnMapReadyCallback, PermissionsListener,
        MapboxMap.OnMapClickListener {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private static final long CAMERA_ANIMATION_TIME = 1950;

    private FloatingActionButton fabLocateUser, fabZoomIn, fabZoomOut, fabShowAll;

    private FloatingActionButton fabSpotAction1, fabSpotAction2;
    //private TextView mWaitingToGetCurrentLocationTextView;
    private CoordinatorLayout coordinatorLayout;
    Boolean shouldDisplayIcons = true;
    Boolean shouldDisplayOldRoutes = true;
    boolean wasSnackbarShown;

    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationLayerPlugin;

    private static final String MARKER_SOURCE_ID = "markers-source";
    private static final String MARKER_STYLE_LAYER_ID = "markers-style-layer";
    private static final String CALLOUT_LAYER_ID = "mapbox.poi.callout";
    private static final int PERMISSIONS_LOCATION = 0;

    Toast waiting_GPS_update;

    Snackbar snackbar;

    /**
     * Represents a geographical location.
     */
    boolean wasFirstLocationReceived = false;

    SharedPreferences prefs;

    static String locationSeparator = ", ";

    Icon ic_single_spot, ic_typeunknown_spot, ic_took_a_break_spot, ic_waiting_spot, ic_point_on_the_route_spot, ic_arrival_spot = null;
    Icon ic_got_a_ride_spot0, ic_got_a_ride_spot1, ic_got_a_ride_spot2, ic_got_a_ride_spot3, ic_got_a_ride_spot4;

    List<Spot> spotList = new ArrayList();

    protected static final String SNACKBAR_SHOWED_KEY = "snackbar-showed";

    int routeIndexToKeepVisible = 0;

    private ProgressDialog loadingDialog;

    //Each hitchhiking spot is a feature
    FeatureCollection featureCollection;
    //Each route is an item of featuresArray and polylineOptionsArray
    Feature[] featuresArray;
    PolylineOptions[] polylineOptionsArray;
    GeoJsonSource source;
    SymbolLayer markerStyleLayer;

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

    Boolean shouldZoomToFitAllMarkers = true;

    protected static final String TAG = "map-view-activity";
    private static final String PROPERTY_ICONIMAGE = "iconImage",
            PROPERTY_ROUTEINDEX = "routeIndex", PROPERTY_TAG = "tag", PROPERTY_SPOTTYPE = "spotType",
            PROPERTY_TITLE = "title", PROPERTY_SNIPPET = "snippet",
            PROPERTY_SHOULDHIDE = "shouldHide", PROPERTY_SELECTED = "selected";

    MainActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //Set CompatVectorFromResourcesEnabled to true in order to be able to use ContextCompat.getDrawable
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        //mWaitingToGetCurrentLocationTextView = (TextView) findViewById(R.id.waiting_location_textview);
        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorLayout);

        prefs = activity.getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        //savedInstanceState will be not null when a screen is rotated, for example. But will be null when activity is first created
        if (savedInstanceState == null) {
            if (!wasSnackbarShown) {
                if (activity.getIntent().getBooleanExtra(Constants.SHOULD_SHOW_SPOT_SAVED_SNACKBAR_KEY, false))
                    showSpotSavedSnackbar();
                else if (activity.getIntent().getBooleanExtra(Constants.SHOULD_SHOW_SPOT_DELETED_SNACKBAR_KEY, false))
                    showSpotDeletedSnackbar();
            }
            wasSnackbarShown = true;
        } else
            updateValuesFromBundle(savedInstanceState);

        fabLocateUser = (FloatingActionButton) view.findViewById(R.id.fab_locate_user);
        fabLocateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null) {
                    moveCameraToLastKnownLocation();

                    if (waiting_GPS_update == null)
                        waiting_GPS_update = Toast.makeText(activity.getBaseContext(), getString(R.string.waiting_for_gps), Toast.LENGTH_SHORT);
                    waiting_GPS_update.show();

                    locateUser();
                }
            }
        });

        fabZoomIn = (FloatingActionButton) view.findViewById(R.id.fab_zoom_in);
        fabZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null)
                    mapboxMap.moveCamera(CameraUpdateFactory.zoomIn());
            }
        });

        fabZoomOut = (FloatingActionButton) view.findViewById(R.id.fab_zoom_out);
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

        fabSpotAction1 = (FloatingActionButton) view.findViewById(R.id.fab_spot_action_1);
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
                //startActivity(new Intent(activity, MyLocationFragment.class));
            }
        });

        fabSpotAction2 = (FloatingActionButton) view.findViewById(R.id.fab_spot_action_2);
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
                //startActivity(new Intent(activity, MyLocationFragment.class));
            }
        });

        fabSpotAction1.setVisibility(View.INVISIBLE);
        fabSpotAction2.setVisibility(View.INVISIBLE);

        mapView = (MapView) view.findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        loadMarkerIcons();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Important: setHasOptionsMenu must be called so that onOptionsItemSelected works
        setHasOptionsMenu(true);
    }

    void locateUser() {
        enableLocationPlugin();

        // Enable the location layer on the map
        if (PermissionsManager.areLocationPermissionsGranted(activity) && !locationLayerPlugin.isLocationLayerEnabled())
            locationLayerPlugin.setLocationLayerEnabled(true);
    }

    void showSpotSavedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_saved_successfuly),
                null, null);
    }

    void showSpotDeletedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_deleted_successfuly),
                null, null);
    }

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
        snackbarView.setBackgroundColor(ContextCompat.getColor(activity.getBaseContext(), R.color.ic_regular_spot_color));

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
        ic_single_spot = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, -1);

        ic_point_on_the_route_spot = IconUtils.drawableToIcon(activity, R.drawable.ic_point_on_the_route_black_24dp, -1, 0.9, 0.9);
        ic_took_a_break_spot = IconUtils.drawableToIcon(activity, R.drawable.ic_break_spot_icon, -1, 0.9, 0.9);
        ic_waiting_spot = IconUtils.drawableToIcon(activity, R.drawable.ic_marker_waiting_for_a_ride_24dp, -1, 0.9, 0.9);
        ic_arrival_spot = IconUtils.drawableToIcon(activity, R.drawable.ic_arrival_icon, -1, 0.9, 0.9);

        ic_typeunknown_spot = IconUtils.drawableToIcon(activity, R.drawable.ic_edit_location_black_24dp, getIdentifierColorStateList(-1));
        ic_got_a_ride_spot0 = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(0));
        ic_got_a_ride_spot1 = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(1));
        ic_got_a_ride_spot2 = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(2));
        ic_got_a_ride_spot3 = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(3));
        ic_got_a_ride_spot4 = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(4));
    }

    protected void configureBottomFABButtons() {
        switch (currentPage) {
            case NOT_FETCHING_LOCATION:
            default:
                fabSpotAction1.setVisibility(View.INVISIBLE);
                fabSpotAction2.setVisibility(View.INVISIBLE);
                break;
            case WILL_BE_FIRST_SPOT_OF_A_ROUTE:
                fabSpotAction1.setImageResource(R.drawable.ic_regular_spot_icon);
                fabSpotAction1.setBackgroundTintList(ContextCompat.getColorStateList(activity.getBaseContext(), R.color.ic_regular_spot_color));
                fabSpotAction1.setRippleColor(ContextCompat.getColor(activity.getBaseContext(), R.color.ic_regular_spot_color_lighter));

                fabSpotAction1.setVisibility(View.VISIBLE);
                fabSpotAction2.setVisibility(View.INVISIBLE);
                break;
            case WILL_BE_REGULAR_SPOT:
                fabSpotAction1.setImageResource(R.drawable.ic_regular_spot_icon);
                fabSpotAction1.setBackgroundTintList(ContextCompat.getColorStateList(activity.getBaseContext(), R.color.ic_regular_spot_color));
                fabSpotAction1.setRippleColor(ContextCompat.getColor(activity.getBaseContext(), R.color.ic_regular_spot_color_lighter));
                fabSpotAction2.setImageResource(R.drawable.ic_arrival_icon);
                fabSpotAction2.setBackgroundTintList(ContextCompat.getColorStateList(activity.getBaseContext(), R.color.ic_arrival_color));
                fabSpotAction2.setRippleColor(ContextCompat.getColor(activity.getBaseContext(), R.color.ic_arrival_color_lighter));

                fabSpotAction1.setVisibility(View.VISIBLE);
                fabSpotAction2.setVisibility(View.VISIBLE);
                break;
            case WAITING_FOR_A_RIDE:
                fabSpotAction1.setImageResource(R.drawable.ic_got_a_ride_spot_icon);
                fabSpotAction1.setBackgroundTintList(ContextCompat.getColorStateList(activity.getBaseContext(), R.color.ic_got_a_ride_color));
                fabSpotAction1.setRippleColor(ContextCompat.getColor(activity.getBaseContext(), R.color.ic_got_a_ride_color_lighter));
                fabSpotAction2.setImageResource(R.drawable.ic_break_spot_icon);
                fabSpotAction2.setBackgroundTintList(ContextCompat.getColorStateList(activity.getBaseContext(), R.color.ic_break_color));
                fabSpotAction2.setRippleColor(ContextCompat.getColor(activity.getBaseContext(), R.color.ic_break_color_lighter));

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
        Toast.makeText(activity.getBaseContext(), "R.string.user_location_permission_not_granted", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        }
    }


    @SuppressWarnings({"MissingPermission"})
    private void enableLocationPlugin() {
        if (locationLayerPlugin == null) {
            // Check if permissions are enabled and if not request
            if (PermissionsManager.areLocationPermissionsGranted(activity.getBaseContext())) {

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
                locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
                getLifecycle().addObserver(locationLayerPlugin);
            } else {
                permissionsManager = new PermissionsManager(this);
                permissionsManager.requestLocationPermissions(activity);
            }
        }
    }

    //onMapReady is called after onResume()
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        Crashlytics.log(Log.INFO, TAG, "onMapReady was called");
        prefs.edit().putBoolean(Constants.PREFS_MAPBOX_WAS_EVER_LOADED, true).apply();

        this.mapboxMap = mapboxMap;

        this.mapboxMap.getUiSettings().setCompassEnabled(false);
        this.mapboxMap.getUiSettings().setLogoEnabled(false);
        this.mapboxMap.getUiSettings().setAttributionEnabled(false);

        this.mapboxMap.addOnMapClickListener(this);

        this.mapboxMap.setOnInfoWindowClickListener(new MapboxMap.OnInfoWindowClickListener() {
            @Override
            public boolean onInfoWindowClick(@NonNull Marker marker) {
                ExtendedMarkerView myMarker = (ExtendedMarkerView) marker;
                onItemClick(myMarker.getTag());
                return true;
            }
        });

        setupIconImages();

        locateUser();

        updateUI();
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, CALLOUT_LAYER_ID);
        if (!features.isEmpty()) {
            // we received a click event on the callout layer
            Feature feature = features.get(0);
            handleClickCallout(feature);
        } else {
            // we didn't find a click event on callout layer, try clicking maki layer
            handleClickIcon(screenPoint);
        }
    }

    /**
     * This method handles click events for callout symbols.
     * <p>
     * It creates a hit rectangle based on the the textView, offsets that rectangle to the location
     * of the symbol on screen and hit tests that with the screen point.
     * </p>
     *
     * @param feature the feature that was clicked
     */
    private void handleClickCallout(Feature feature) {
        onItemClick(feature.getStringProperty(PROPERTY_TAG));
    }

    private void onItemClick(String spotId) {
        Crashlytics.setString("Clicked marker tag", spotId);
        Spot spot = null;
        for (Spot spot2 :
                spotList) {
            String id = spot2.getId().toString();
            if (id.equals(spotId)) {
                spot = spot2;
                break;
            }
        }

        if (spot != null) {
            Spot mCurrentWaitingSpot = ((MyHitchhikingSpotsApplication) activity.getApplicationContext()).getCurrentSpot();

            //If the user is currently waiting at a spot and the clicked spot is not the one he's waiting at, show a Toast.
            if (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null &&
                    mCurrentWaitingSpot.getIsWaitingForARide()) {
                if (mCurrentWaitingSpot.getId() == spot.getId())
                    spot.setAttemptResult(null);
                else {
                    Toast.makeText(activity.getBaseContext(), getResources().getString(R.string.evaluate_running_spot_required), Toast.LENGTH_LONG).show();
                    return;
                }
            }

            Intent intent = new Intent(activity.getBaseContext(), SpotFormActivity.class);
            //Maybe we should send mCurrentWaitingSpot on the intent.putExtra so that we don't need to call spot.setAttemptResult(null) ?
            intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
            intent.putExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, true);
            startActivityForResult(intent, Constants.EDIT_SPOT_REQUEST);
        } else
            Crashlytics.log(Log.WARN, TAG,
                    "A spot corresponding to the clicked InfoWindow was not found on the list. If a spot isn't in the list, how a marker was added to it? The open marker's tag was: " + spotId);


    }

    /**
     * This method handles click events for maki symbols.
     * <p>
     * When a maki symbol is clicked, we moved that feature to the selected state.
     * </p>
     *
     * @param screenPoint the point on screen clicked
     */
    private void handleClickIcon(PointF screenPoint) {
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, MARKER_STYLE_LAYER_ID);
        if (!features.isEmpty()) {
            String tag = features.get(0).getStringProperty(PROPERTY_TAG);
            List<Feature> featureList = featureCollection.features();
            for (int i = 0; i < featureList.size(); i++) {
                if (featureList.get(i).getStringProperty(PROPERTY_TAG).equals(tag)) {
                    setSelected(i, true);
                }
            }
        } else {
            deselectAll(false);
            refreshSource();
        }
    }

    /**
     * Set a feature selected state with the ability to scroll the RecycleViewer to the provided index.
     *
     * @param index      the index of selected feature
     * @param withScroll indicates if the recyclerView position should be updated
     */
    private void setSelected(int index, boolean withScroll) {
        /*if (recyclerView.getVisibility() == View.GONE) {
            recyclerView.setVisibility(View.VISIBLE);
        }*/

        deselectAll(false);

        Feature feature = featureCollection.features().get(index);
        if (mapboxMap.getImage(feature.id()) == null) {
            showProgressDialog("Loading data..");

            //Generate bitmaps from the layout_callout view that should appear when a icon is clicked
            new GenerateBalloonsTask(this, featureCollection.features().get(index)).execute();
        } else
            showBalloon(feature);

        /*//Fetch pictures from around the feature location
        loadMapillaryData(feature);

        if (withScroll) {
            recyclerView.scrollToPosition(index);
        }*/
    }

    private void showBalloon(Feature feature) {
        dismissProgressDialog();
        selectFeature(feature);
        refreshSource();
    }

    /**
     * Deselects the state of all the features
     */
    private void deselectAll(boolean hideRecycler) {
        for (Feature feature : featureCollection.features()) {
            feature.properties().addProperty(PROPERTY_SELECTED, false);
        }

        /*if (hideRecycler) {
            recyclerView.setVisibility(View.GONE);
        }*/
    }

    /**
     * Selects the state of a feature
     *
     * @param feature the feature to be selected.
     */
    private void selectFeature(Feature feature) {
        feature.properties().addProperty(PROPERTY_SELECTED, true);
    }

    private void setupIconImages() {
        this.mapboxMap.addImage(ic_single_spot.getId(), ic_single_spot.getBitmap());
        this.mapboxMap.addImage(ic_point_on_the_route_spot.getId(), ic_point_on_the_route_spot.getBitmap());
        this.mapboxMap.addImage(ic_waiting_spot.getId(), ic_waiting_spot.getBitmap());
        this.mapboxMap.addImage(ic_arrival_spot.getId(), ic_arrival_spot.getBitmap());
        this.mapboxMap.addImage(ic_typeunknown_spot.getId(), ic_typeunknown_spot.getBitmap());
        this.mapboxMap.addImage(ic_got_a_ride_spot0.getId(), ic_got_a_ride_spot0.getBitmap());
        this.mapboxMap.addImage(ic_got_a_ride_spot1.getId(), ic_got_a_ride_spot1.getBitmap());
        this.mapboxMap.addImage(ic_got_a_ride_spot2.getId(), ic_got_a_ride_spot2.getBitmap());
        this.mapboxMap.addImage(ic_got_a_ride_spot3.getId(), ic_got_a_ride_spot3.getBitmap());
        this.mapboxMap.addImage(ic_got_a_ride_spot4.getId(), ic_got_a_ride_spot4.getBitmap());
    }

    private LatLng convertToLatLng(Feature feature) {
        Point symbolPoint = (Point) feature.geometry();
        return new LatLng(symbolPoint.latitude(), symbolPoint.longitude());
    }

    void updateUI() {
        Crashlytics.log(Log.INFO, TAG, "updateUI was called");

        if (!Utils.isNetworkAvailable(activity) && !Utils.shouldLoadCurrentView(prefs)) {
            new AlertDialog.Builder(activity)
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
        //Load markers and polylines
        new LoadSpotsAndRoutesTask(this).execute();
    }

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

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onStart() {
        Crashlytics.log(Log.INFO, TAG, "onStart called");
        super.onStart();
        mapView.onStart();
        if (locationLayerPlugin != null)
            locationLayerPlugin.onStart();
    }

    @Override
    public void onStop() {
        Crashlytics.log(Log.INFO, TAG, "onStop called");
        super.onStop();
        mapView.onStop();
        if (locationLayerPlugin != null)
            locationLayerPlugin.onStop();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == Constants.RESULT_OBJECT_ADDED || resultCode == Constants.RESULT_OBJECT_EDITED)
            showSpotSavedSnackbar();

        if (resultCode == Constants.RESULT_OBJECT_DELETED)
            showSpotDeletedSnackbar();

      /*
        // Check which request we're responding to
        if (requestCode == SAVE_SPOT_REQUEST || requestCode == EDIT_SPOT_REQUEST) {
            // Make sure the request was successful
            if (resultCode > RESULT_FIRST_USER)
                updateUI();
        }*/
    }

    @Override
    public void onPause() {
        Crashlytics.log(Log.INFO, TAG, "onPause was called");
        super.onPause();
        mapView.onPause();

        dismissSnackbar();
        dismissProgressDialog();
    }


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
    public void onDestroy() {
        Crashlytics.log(Log.INFO, TAG, "onDestroy was called");
        super.onDestroy();
        if (mapboxMap != null) {
            mapboxMap.removeOnMapClickListener(this);
        }
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Crashlytics.log(Log.WARN, TAG, "onLowMemory was called");
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.my_maps_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
                    new AlertDialog.Builder(activity)
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
                    showAllRoutesOnMap();

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
            case R.id.action_toggle_old_routes:
                shouldDisplayOldRoutes = !shouldDisplayOldRoutes;
                if (shouldDisplayOldRoutes)
                    showAllRoutesOnMap();
                else
                    hideOldRoutesFromMap();
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

    void showAllRoutesOnMap() {
        //Update all features setting PROPERTY_SHOULDHIDE to false
        for (Feature f : featureCollection.features())
            f.properties().addProperty(PROPERTY_SHOULDHIDE, false);

        refreshSource();

        mapboxMap.addPolylines(Arrays.asList(polylineOptionsArray));
    }

    void hideOldRoutesFromMap() {
        //Update all features that should be hidden
        for (Feature f : featureCollection.features()) {
            if (f.getNumberProperty("routeIndex").equals(routeIndexToKeepVisible))
                f.properties().addProperty(PROPERTY_SHOULDHIDE, false);
            else
                f.properties().addProperty(PROPERTY_SHOULDHIDE, true);
        }

        refreshSource();

        //Remove all polylines
        for (Polyline p : mapboxMap.getPolylines())
            p.remove();

        //Add all polylines that have been hidden
        if (polylineOptionsArray != null) {
            List<PolylineOptions> polylinesToShow = new ArrayList<>();
            for (int i = 0; i < polylineOptionsArray.length; i++) {
                if (i == routeIndexToKeepVisible)
                    polylinesToShow.add(polylineOptionsArray[i]);
            }
            mapboxMap.addPolylines(polylinesToShow);
        }
    }

    void openSpotsListView(Boolean... shouldShowYouTab) {
        Intent intent = new Intent(activity, MyRoutesActivity.class);
        intent.putExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, true);
        /*//When set to true, shouldShowYouTab will open MyRoutesActivity presenting the tab "You" instead of the tab "List"
        if (shouldShowYouTab.length > 0)
            intent.putExtra(Constants.SHOULD_SHOW_YOU_TAB_KEY, shouldShowYouTab[0]);*/
        startActivity(intent);
        //startActivity(new Intent(activity, MyRoutesActivity.class));
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
                for (Spot spot : spotList) {
                    lst.add(new LatLng(spot.getLatitude(), spot.getLongitude()));
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

    private void showProgressDialog(String message) {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(activity);
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
        }
        loadingDialog.setMessage(message);
        loadingDialog.show();
    }

    private void dismissProgressDialog() {
        try {
            if (loadingDialog != null && loadingDialog.isShowing())
                loadingDialog.dismiss();
        } catch (Exception e) {
        }
    }

    private static class LoadSpotsAndRoutesTask extends AsyncTask<Void, Void, List<Route>> {
        private List<Spot> spotList;
        private final WeakReference<MyMapsFragment> activityRef;
        String errMsg = "";
        Boolean isLastArrayForSingleSpots = false;

        LoadSpotsAndRoutesTask(MyMapsFragment activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            MyMapsFragment activity = activityRef.get();
            if (activity == null)
                return;

            activity.showProgressDialog(activity.getResources().getString(R.string.map_loading_dialog));
        }

        @Override
        protected List<Route> doInBackground(Void... voids) {
            MyMapsFragment activity = activityRef.get();
            if (activity == null)
                return null;

            try {
                loadValues();
            } catch (final Exception ex) {
                Crashlytics.logException(ex);
                errMsg = "Loading spots failed.\n" + ex.getMessage();
                return new ArrayList<>();
            }

            List<List<ExtendedMarkerViewOptions>> trips = new ArrayList<>();
            ArrayList<ExtendedMarkerViewOptions> spots = new ArrayList<>();
            ArrayList<ExtendedMarkerViewOptions> singleSpots = new ArrayList<>();

            //The spots are ordered from the last saved ones to the first saved ones, so we need to
            // go through the list in the opposite direction in order to sum up the route's totals from their origin to their destinations
            for (int i = spotList.size() - 1; i >= 0; i--) {
                try {
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

                            markerTitle = activity.getString(R.string.map_infoview_spot_type_waiting);
                            icon = activity.ic_waiting_spot;
                            markerViewOptions.spotType(Constants.SPOT_TYPE_WAITING);

                        } else if (spot.getIsDestination() != null && spot.getIsDestination()) {
                            //The spot is a destination

                            markerTitle = activity.getString(R.string.map_infoview_spot_type_destination);
                            icon = activity.ic_arrival_spot;
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
                                        icon = activity.getGotARideIconForRoute(trips.size());
                                        markerViewOptions.anchor((float) 0.5, (float) 0.5);
                                        markerTitle = Utils.getRatingOrDefaultAsString(activity.activity.getBaseContext(), spot.getHitchability() != null ? spot.getHitchability() : 0);
                                        break;
                                    case Constants.ATTEMPT_RESULT_TOOK_A_BREAK:
                                        //The spot is a hitchhiking spot that was already evaluated
                                        //icon = ic_took_a_break_spot;
                                        icon = activity.ic_point_on_the_route_spot;
                                        markerViewOptions.anchor((float) 0.5, (float) 0.5);
                                        markerViewOptions.alpha((float) 0.5);
                                        markerTitle = activity.getString(R.string.map_infoview_spot_type_break);
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
                                icon = activity.ic_point_on_the_route_spot;
                                markerViewOptions.spotType(Constants.SPOT_TYPE_POINT_ON_THE_ROUTE);
                                markerViewOptions.anchor((float) 0.5, (float) 0.5);
                                markerViewOptions.alpha((float) 0.5);
                            }

                            if (spots.size() == 0) {
                                //The spot is the origin of a route
                                markerViewOptions.spotType(Constants.SPOT_TYPE_ORIGIN);
                                if (!markerTitle.isEmpty())
                                    markerTitle = activity.getString(R.string.map_infoview_spot_type_origin) + " " + markerTitle;
                                else
                                    markerTitle = activity.getString(R.string.map_infoview_spot_type_origin);
                            }
                        }
                    } else {
                        //This spot doesn't belong to a route (it's a single spot)

                        if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot()) {
                            markerTitle = Utils.getRatingOrDefaultAsString(activity.activity.getBaseContext(), spot.getHitchability() != null ? spot.getHitchability() : 0);

                            icon = activity.ic_single_spot;
                            markerViewOptions.spotType(Constants.SPOT_TYPE_SINGLE_SPOT);
                        } else {
                            icon = activity.ic_point_on_the_route_spot;
                            markerViewOptions.spotType(Constants.SPOT_TYPE_POINT_ON_THE_ROUTE);
                            markerViewOptions.anchor((float) 0.5, (float) 0.5);
                            markerViewOptions.alpha((float) 0.5);
                        }
                    }

                    if (icon != null)
                        markerViewOptions.icon(icon);

                    //Set hitchability as title
                    markerViewOptions.title(markerTitle.toUpperCase());

                    // Customize map with markers, polylines, etc.
                    String snippet = getSnippet(activity, spot, "\n", " ", "\n");
                    markerViewOptions.snippet(snippet);

                    if (spot.getIsPartOfARoute() != null && spot.getIsPartOfARoute()) {
                        spots.add(markerViewOptions);

                        if (spot.getIsDestination() != null && spot.getIsDestination() || i == 0) {
                            trips.add(spots);
                            spots = new ArrayList<>();
                        }
                    } else
                        singleSpots.add(markerViewOptions);
                } catch (final Exception ex) {
                    Crashlytics.logException(ex);
                    errMsg = "Failed to load a spot";
                }
            }

            if (singleSpots.size() > 0) {
                trips.add(singleSpots);
                isLastArrayForSingleSpots = true;
            }

            if (!errMsg.isEmpty()) {
                activity.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.showErrorAlert(activity.getResources().getString(R.string.general_error_dialog_title), "Loading spots failed.\n" + errMsg);
                    }
                });
            }


            List<Route> routes = new ArrayList<Route>();

            for (int lc = 0; lc < trips.size(); lc++) {
                try {
                    List<ExtendedMarkerViewOptions> spots2 = trips.get(lc);
                    Route route = new Route();
                    route.features = new Feature[spots2.size()];

                    /* Source: A data source specifies the geographic coordinate where the image markers get placed. */
                    //Feature for

                    //If it's the last array and isLastArrayForSingleSpots is true, add the markers with no polyline connecting them
                    if (isLastArrayForSingleSpots && lc == trips.size() - 1) {
                        for (int li = 0; li < spots2.size(); li++) {
                            ExtendedMarkerViewOptions spot = spots2.get(li);
                            //Add marker to map
                            route.features[li] = GetFeature(spot, lc);
                        }
                    } else {
                        PolylineOptions line = new PolylineOptions()
                                .width(2)
                                .color(Color.parseColor(activity.getResources().getString(activity.getPolylineColorAsId(lc))));//Color.parseColor(getPolylineColorAsString(lc)));


                        //Add route to the map with markers and polylines connecting them
                        for (int li = 0; li < spots2.size(); li++) {
                            ExtendedMarkerViewOptions spot = spots2.get(li);
                            route.features[li] = GetFeature(spot, lc);

                            //Add polyline connecting this marker
                            line.add(spot.getPosition());
                        }

                        route.polylines = line;
                    }

                    routes.add(route);
                } catch (Exception ex) {
                    Crashlytics.logException(ex);
                    errMsg = "Adding markers failed - " + ex.getMessage();
                }

            }

            return routes;
        }

        @NonNull
        /* Get snippet in the following format: "spotLocationToString{firstSeparator}{dateTimeToString}{secondSeparator}({getWaitingTimeAsString}){thirdSeparator}{getNote}" */
        private String getSnippet(MyMapsFragment activity, Spot spot, String firstSeparator, String secondSeparator, String thirdSeparator) {
            //Get location string
            String snippet = spotLocationToString(spot).trim();

            if (!snippet.isEmpty())
                snippet += firstSeparator;

            //Add date time if it is set
            if (spot.getStartDateTime() != null)
                snippet += SpotListAdapter.dateTimeToString(spot.getStartDateTime());

            //Add waiting time
            if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot() &&
                    spot.getWaitingTime() != null) {
                if (!snippet.isEmpty())
                    snippet += secondSeparator;
                snippet += "(" + Utils.getWaitingTimeAsString(spot.getWaitingTime(), activity.activity.getBaseContext()) + ")";
            }

            //Add note
            if (spot.getNote() != null && !spot.getNote().isEmpty()) {
                if (!snippet.isEmpty())
                    snippet += thirdSeparator;
                snippet += spot.getNote();
            }

            return snippet;
        }

        @Override
        protected void onPostExecute(List<Route> routes) {
            super.onPostExecute(routes);
            MyMapsFragment activity = activityRef.get();
            if (activity == null)
                return;

            activity.setupData(spotList, routes, errMsg);
        }

        public void loadValues() {
            MyMapsFragment activity = activityRef.get();
            if (activity == null)
                return;

            MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) activity.activity.getApplicationContext());
            DaoSession daoSession = appContext.getDaoSession();
            SpotDao spotDao = daoSession.getSpotDao();

            spotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.IsPartOfARoute, SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();

            activity.mCurrentWaitingSpot = appContext.getCurrentSpot();

            if (activity.mCurrentWaitingSpot == null || activity.mCurrentWaitingSpot.getIsWaitingForARide() == null)
                activity.mIsWaitingForARide = false;
            else
                activity.mIsWaitingForARide = activity.mCurrentWaitingSpot.getIsWaitingForARide();


            activity.mWillItBeFirstSpotOfARoute = spotList.size() == 0 || (spotList.get(0).getIsDestination() != null && spotList.get(0).getIsDestination());
        }

        static Feature GetFeature(ExtendedMarkerViewOptions oldMarker, int routeIndex) {
            LatLng pos = oldMarker.getPosition();

            JsonObject properties = new JsonObject();
            properties.addProperty(PROPERTY_ICONIMAGE, oldMarker.getIcon().getId());
            properties.addProperty(PROPERTY_ROUTEINDEX, routeIndex);
            properties.addProperty(PROPERTY_TAG, oldMarker.getTag());
            properties.addProperty(PROPERTY_SPOTTYPE, oldMarker.getSpotType());
            properties.addProperty(PROPERTY_TITLE, oldMarker.getTitle());
            properties.addProperty(PROPERTY_SNIPPET, oldMarker.getSnippet());
            properties.addProperty(PROPERTY_SHOULDHIDE, false);

            return Feature.fromGeometry(Point.fromLngLat(pos.getLongitude(), pos.getLatitude()), properties, oldMarker.getTag());
        }

    }

    /**
     * AsyncTask to generate Bitmap from Views to be used as iconImage in a SymbolLayer.
     * Note: This task only adds to the mapview a layer with the balloons that are shown when the markers are clicked. It does not add the markers themselves.
     * <p>
     * Call be optionally be called to update the underlying data source after execution.
     * </p>
     * <p>
     * Generating Views on background thread since we are not going to be adding them to the view hierarchy.
     * </p>
     */
    private static class GenerateBalloonsTask extends AsyncTask<Void, Void, HashMap<String, Bitmap>> {
        private final WeakReference<MyMapsFragment> activityRef;
        private final Feature feature;

        GenerateBalloonsTask(MyMapsFragment activity, Feature feature) {
            this.activityRef = new WeakReference<>(activity);
            this.feature = feature;
        }

        @SuppressWarnings("WrongThread")
        @Override
        protected HashMap<String, Bitmap> doInBackground(Void... x) {
            MyMapsFragment activity = activityRef.get();
            if (activity != null) {
                HashMap<String, Bitmap> imagesMap = new HashMap<>();
                LayoutInflater inflater = LayoutInflater.from(activity.activity);

                View view = inflater.inflate(R.layout.layout_callout, null);

                String name = feature.getStringProperty(PROPERTY_TITLE);
                TextView titleTv = (TextView) view.findViewById(R.id.title);
                titleTv.setText(name);

                String style = feature.getStringProperty(PROPERTY_SNIPPET);
                TextView styleTv = (TextView) view.findViewById(R.id.style);
                styleTv.setText(style);

                /*boolean favourite = feature.getBooleanProperty(PROPERTY_FAVOURITE);
                ImageView imageView = (ImageView) view.findViewById(R.id.logoView);
                imageView.setImageResource(favourite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);*/

                String tag = feature.getStringProperty(PROPERTY_TAG);
                Bitmap bitmap = SymbolGenerator.generate(view);
                imagesMap.put(tag, bitmap);
                //viewMap.put(name, view);

                return imagesMap;
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(HashMap<String, Bitmap> bitmapHashMap) {
            super.onPostExecute(bitmapHashMap);
            MyMapsFragment activity = activityRef.get();
            if (activity != null && bitmapHashMap != null) {
                activity.setImageGenResults(bitmapHashMap);
                activity.showBalloon(feature);
            }
        }
    }

    /**
     * Utility class to generate Bitmaps for Symbol.
     * <p>
     * Bitmaps can be added to the map with {@link MapboxMap#addImage(String, Bitmap)}
     * </p>
     */
    private static class SymbolGenerator {

        /**
         * Generate a Bitmap from an Android SDK View.
         *
         * @param view the View to be drawn to a Bitmap
         * @return the generated bitmap
         */
        static Bitmap generate(@NonNull View view) {
            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(measureSpec, measureSpec);

            int measuredWidth = view.getMeasuredWidth();
            int measuredHeight = view.getMeasuredHeight();

            view.layout(0, 0, measuredWidth, measuredHeight);
            Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.TRANSPARENT);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        }
    }

    /**
     * Invoked when the balloons bitmaps have been generated for all features.
     */
    public void setImageGenResults(HashMap<String, Bitmap> imageMap) {
        if (mapboxMap != null) {
            // calling addImages is faster as separate addImage calls for each bitmap.
            mapboxMap.addImages(imageMap);
        }
        // need to store reference to views to be able to use them as hitboxes for click events.
        //this.viewMap = viewMap;
    }

    void setupData(List<Spot> spotList, List<Route> routes, String errMsg) {
        PolylineOptions[] allPolylines = new PolylineOptions[routes.size()];
        List<Feature> allFeatures = new ArrayList<>();

        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            allPolylines[i] = route.polylines == null ? new PolylineOptions() : route.polylines;
            allFeatures.addAll(Arrays.asList(route.features));
        }

        this.spotList = spotList;
        Feature[] array = new Feature[allFeatures.size()];
        this.polylineOptionsArray = allPolylines;
        this.featuresArray = allFeatures.toArray(array);

        if (mapboxMap == null) {
            return;
        }

        mapboxMap.clear();

        updateUISaveButtons();

        featureCollection = FeatureCollection.fromFeatures(featuresArray);

        setupSource();
        setupStyleLayer();
        setupPolylines();
        //Setup a layer with Android SDK call-outs (title of the feature is used as key for the iconImage)
        setupCalloutLayer();

        try {
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
                        Spot lastAddedSpot = ((MyHitchhikingSpotsApplication) activity.getApplicationContext()).getLastAddedRouteSpot();
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
            errMsg = String.format(getResources().getString(R.string.general_error_dialog_message),
                    "Adding markers failed - " + ex.getMessage());
        }

        if (!errMsg.isEmpty()) {
            showErrorAlert(getResources().getString(R.string.general_error_dialog_title), errMsg);
        }

        dismissProgressDialog();
    }

    protected void showErrorAlert(String title, String msg) {
        new AlertDialog.Builder(activity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.general_ok_option), null)
                .show();
    }

    private void setupSource() {
        if (mapboxMap.getSource(MARKER_SOURCE_ID) == null) {
            source = new GeoJsonSource(MARKER_SOURCE_ID, featureCollection);
            mapboxMap.addSource(source);
        } else
            refreshSource();
    }

    /* Setup style layer */
    private void setupStyleLayer() {
        if (mapboxMap.getLayer(MARKER_STYLE_LAYER_ID) == null) {
            //A style layer ties together the source and image and specifies how they are displayed on the map
            markerStyleLayer = new SymbolLayer(MARKER_STYLE_LAYER_ID, MARKER_SOURCE_ID)
                    .withProperties(
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconImage("{" + PROPERTY_ICONIMAGE + "}")
                    )
                    /* add a filter to show only features with PROPERTY_SHOULDHIDE set to false */
                    .withFilter(eq((get(PROPERTY_SHOULDHIDE)), literal(false)));

            //Add markers layer
            mapboxMap.addLayer(markerStyleLayer);
        }
    }

    private void setupPolylines() {
        //Add spots and polylines to map
        mapboxMap.addPolylines(Arrays.asList(polylineOptionsArray));
    }

    /**
     * Setup a layer with Android SDK call-outs (balloons)
     * <p>
     * tag of the feature is used as key for the iconImage
     * </p>
     */
    private void setupCalloutLayer() {
        if (mapboxMap.getLayer(CALLOUT_LAYER_ID) == null) {
            mapboxMap.addLayer(new SymbolLayer(CALLOUT_LAYER_ID, MARKER_SOURCE_ID)
                    .withProperties(
                            /* show image with id based on the value of the tag feature property */
                            iconImage("{" + PROPERTY_TAG + "}"),

                            /* set anchor of icon to bottom-left */
                            iconAnchor(Property.ICON_ANCHOR_BOTTOM_LEFT),

                            /* offset icon slightly to match bubble layout */
                            iconOffset(new Float[]{-120.0f, -10.0f})
                    )

                    /* add a filter to show only when selected feature property is true */
                    .withFilter(eq((get(PROPERTY_SELECTED)), literal(true))));
        }
    }

    private void refreshSource() {
        if (source != null && featureCollection != null) {
            source.setGeoJson(featureCollection);
        }
    }

    public void saveRegularSpotButtonHandler() {
        saveSpotButtonHandler(false);
    }

    public void saveDestinationSpotButtonHandler() {
        saveSpotButtonHandler(true);
    }

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

        Intent intent = new Intent(activity.getBaseContext(), SpotFormActivity.class);
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
            Intent intent = new Intent(activity.getBaseContext(), SpotFormActivity.class);
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

    private ColorStateList getIdentifierColorStateList(int routeIndex) {
        return ContextCompat.getColorStateList(activity.getBaseContext(), getPolylineColorAsId(routeIndex));
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
