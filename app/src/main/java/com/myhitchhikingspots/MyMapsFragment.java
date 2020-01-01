package com.myhitchhikingspots;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.crashlytics.android.Crashlytics;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
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
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.myhitchhikingspots.interfaces.FirstLocationUpdateListener;
import com.myhitchhikingspots.model.Route;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SubRoute;
import com.myhitchhikingspots.utilities.DateRangePickerDialog;
import com.myhitchhikingspots.utilities.IconUtils;
import com.myhitchhikingspots.utilities.LocationUpdatesCallback;
import com.myhitchhikingspots.utilities.SpotsListHelper;
import com.myhitchhikingspots.utilities.Utils;

import org.joda.time.DateTime;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import static android.os.Looper.getMainLooper;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgb;
import static com.mapbox.mapboxsdk.style.expressions.Expression.step;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

public class MyMapsFragment extends Fragment implements OnMapReadyCallback, PermissionsListener,
        MapboxMap.OnMapClickListener, FirstLocationUpdateListener {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style style;

    private FloatingActionButton fabLocateUser, fabZoomIn, fabZoomOut;

    private boolean spotListWasChanged = false;
    private boolean isHandlingRequestToOpenSpotForm = false;
    private FloatingActionButton fabSpotAction1, fabSpotAction2;
    ConstraintLayout coordinatorLayout;
    Boolean mapButtonsAreDisplayed = true;
    boolean wasSnackbarShown;
    DateRangePickerDialog dateRangeDialog;

    /**
     * Maximum number of spots to fit within the map camera when {@link #zoomOutToFitMostRecentRoute()} is called.
     **/
    int NUMBER_OF_SPOTS_TO_FIT = 10;

    private PermissionsManager locationPermissionsManager;

    private final int ic_add_zoom_level = 12;

    private static final String SPOTS_SOURCE_ID = "spots-source";
    private static final String ROUTES_SPOTS_STYLE_LAYER_ID = "spots-style-layer";
    private static final String SUB_ROUTE_SOURCE_ID = "sub-routes-source";
    private static final String LINES_STYLE_LAYER_ID = "lines-layer";
    private static final String CALLOUT_LAYER_ID = "mapbox.poi.callout";
    private static final int PERMISSIONS_LOCATION = 0;

    Snackbar snackbar;

    AsyncTask loadTask;

    SharedPreferences prefs;

    static String locationSeparator = ", ";

    Icon ic_single_spot, ic_typeunknown_spot, ic_took_a_break_spot, ic_waiting_spot, ic_point_on_the_route_spot, ic_arrival_spot = null;
    Icon ic_got_a_ride_spot0, ic_got_a_ride_spot1, ic_got_a_ride_spot2, ic_got_a_ride_spot3, ic_got_a_ride_spot4, ic_target;

    protected static final String SNACKBAR_SHOWED_KEY = "snackbar-showed";

    private ProgressDialog loadingDialog;

    //Each hitchhiking spot is a feature
    FeatureCollection spotsCollection;
    FeatureCollection subRoutesCollection;
    GeoJsonSource spotSource;
    GeoJsonSource subRoutesSource;

    // Variables needed to add the location engine
    private LocationEngine locationEngine;
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    // Variables needed to listen to location updates
    private LocationUpdatesCallback callback = new LocationUpdatesCallback(this);

    public enum pageType {
        UNKNOWN,
        WILL_BE_FIRST_SPOT_OF_A_ROUTE, //user sees "save spot" but doesn't see "arrival" button
        WILL_BE_REGULAR_SPOT, //user sees both "save spot" and "arrival" buttons
        WAITING_FOR_A_RIDE //user sees "got a ride" and "take a break" buttons
    }

    private pageType currentPage;

    Boolean shouldZoomToFitAllMarkers = true;

    protected static final String TAG = "map-view-activity";
    public static final String PROPERTY_ICONIMAGE = "iconImage",
            PROPERTY_ROUTEINDEX = "routeIndex", PROPERTY_TAG = "tag", PROPERTY_SPOTTYPE = "spotType",
            PROPERTY_TITLE = "title", PROPERTY_SNIPPET = "snippet",
            PROPERTY_SHOULDHIDE = "shouldHide", PROPERTY_SELECTED = "selected",
            PROPERTY_STARTDATETIME_IN_MILLISECS = "startDateTime";

    SpotsListViewModel viewModel;
    myMapsMediatorLiveData ld;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        prefs = context.getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);
        viewModel = new ViewModelProvider(requireActivity()).get(SpotsListViewModel.class);
        ld = new myMapsMediatorLiveData(viewModel.getSpots(), viewModel.getWaitingSpot());
    }

    private static class myMapsMediatorLiveData extends MediatorLiveData<Pair<List<Spot>, Spot>> {
        public myMapsMediatorLiveData(LiveData<List<Spot>> spots, LiveData<Spot> waitingSpot) {
            addSource(spots, first -> setValue(Pair.create(first, waitingSpot.getValue())));
            addSource(waitingSpot, second -> setValue(Pair.create(spots.getValue(), second)));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Set CompatVectorFromResourcesEnabled to true in order to be able to use ContextCompat.getDrawable
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        //mWaitingToGetCurrentLocationTextView = (TextView) findViewById(R.id.waiting_location_textview);
        coordinatorLayout = view.findViewById(R.id.constraintLayout);

        //savedInstanceState will be not null when a screen is rotated, for example. But will be null when activity is first created
        if (savedInstanceState == null) {
            if (!wasSnackbarShown) {
                if (requireActivity().getIntent().getBooleanExtra(Constants.SHOULD_SHOW_SPOT_SAVED_SNACKBAR_KEY, false))
                    showSpotSavedSnackbar();
                else if (requireActivity().getIntent().getBooleanExtra(Constants.SHOULD_SHOW_SPOT_DELETED_SNACKBAR_KEY, false))
                    showSpotDeletedSnackbar();
            }
            wasSnackbarShown = true;
        } else
            updateValuesFromBundle(savedInstanceState);

        fabLocateUser = (FloatingActionButton) view.findViewById(R.id.fab_locate_user);
        fabLocateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null && style != null && style.isFullyLoaded()) {
                    callback.moveMapCameraToNextLocationReceived();
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

        fabSpotAction1 = (FloatingActionButton) view.findViewById(R.id.fab_spot_action_1);
        fabSpotAction1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //While waiting for a ride, MainActionButton is a "Got a ride" button
                //when not waiting for a ride, it's a "Save spot" button
                //Prevent from handling multiple clicks
                if (isHandlingRequestToOpenSpotForm)
                    return;

                Spot mCurrentWaitingSpot = viewModel.getWaitingSpot().getValue();
                boolean isWaitingForARide = (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null) ?
                        mCurrentWaitingSpot.getIsWaitingForARide() : false;

                if (isWaitingForARide)
                    gotARideButtonHandler(mCurrentWaitingSpot);
                else
                    saveRegularSpotButtonHandler(mCurrentWaitingSpot);
            }
        });

        fabSpotAction2 = (FloatingActionButton) view.findViewById(R.id.fab_spot_action_2);
        fabSpotAction2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //While waiting for a ride, SecondaryActionButton is a "Arrived to destination" button
                //when not waiting for a ride, it's a "Take a break" button
                //Prevent from handling multiple clicks
                if (isHandlingRequestToOpenSpotForm)
                    return;

                Spot mCurrentWaitingSpot = viewModel.getWaitingSpot().getValue();
                boolean isWaitingForARide = (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null) ?
                        mCurrentWaitingSpot.getIsWaitingForARide() : false;

                if (isWaitingForARide)
                    tookABreakButtonHandler(mCurrentWaitingSpot);
                else
                    saveDestinationSpotButtonHandler(mCurrentWaitingSpot);
            }
        });

        fabSpotAction1.hide();
        fabSpotAction2.hide();

        mapView = (MapView) view.findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        ld.observe(requireActivity(), p -> {
            updateUI(p.first);
            updateUISaveFABs(p.first, p.second);
        });

        //onMapReady will take care of drawing the spots and routes on the map.
    }

    //onMapReady is called after onResume()
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        Crashlytics.log(Log.INFO, TAG, "onMapReady was called");
        prefs.edit().putBoolean(Constants.PREFS_MAPBOX_WAS_EVER_LOADED, true).apply();

        loadMarkerIcons(requireActivity());

        this.mapboxMap = mapboxMap;

        this.mapboxMap.getUiSettings().setCompassEnabled(true);
        this.mapboxMap.getUiSettings().setLogoEnabled(false);
        this.mapboxMap.getUiSettings().setAttributionEnabled(false);
        this.mapboxMap.getUiSettings().setTiltGesturesEnabled(false);

        this.mapboxMap.addOnMapClickListener(this);

        this.mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments.
            MyMapsFragment.this.style = style;

            if (style.isFullyLoaded()) {
                LocalizationPlugin localizationPlugin = new LocalizationPlugin(mapView, mapboxMap, style);

                try {
                    localizationPlugin.matchMapLanguageWithDeviceDefault();
                } catch (RuntimeException exception) {
                    Crashlytics.logException(exception);
                }

                setupIconImages(style);

                enableLocationLayer(style);
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Important: setHasOptionsMenu must be called so that onOptionsItemSelected works
        setHasOptionsMenu(true);
    }

    boolean isFirstSpotOfARoute(List<Spot> spotList) {
        return ((spotList == null || spotList.size() == 0) ||
                (spotList.get(0).getIsDestination() != null && spotList.get(0).getIsDestination()));
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationLayer(@NonNull Style loadedMapStyle) {
        //Setup location plugin to display the user location on a map.
        // NOTE: map camera won't follow location updates by default here.
        setupLocationComponent(loadedMapStyle);

        LocationComponent locationComponent = mapboxMap.getLocationComponent();

        // Enable the location layer on the map
        if (locationComponent.isLocationComponentActivated() && !locationComponent.isLocationComponentEnabled())
            locationComponent.setLocationComponentEnabled(true);
    }

    void showSpotSavedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_saved_successfuly),
                null, null);
    }

    void showSpotDeletedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_deleted_successfuly),
                null, null);
    }

    void showSnackbar(@NonNull CharSequence text, CharSequence action, View.OnClickListener
            listener) {
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
        int snackbarTextId = com.google.android.material.R.id.snackbar_text;
        TextView textView = (TextView) snackbarView.findViewById(snackbarTextId);
        if (textView != null) textView.setTextColor(Color.WHITE);

        // change snackbar background
        snackbarView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ic_regular_spot_color));

        snackbar.show();
    }

    void dismissSnackbar() {
        try {
            if (snackbar != null && snackbar.isShown())
                snackbar.dismiss();
        } catch (Exception e) {
        }
    }

    private void loadMarkerIcons(@NonNull Context context) {
        ic_single_spot = IconUtils.drawableToIcon(context, R.drawable.ic_route_point_black_24dp, -1);

        ic_point_on_the_route_spot = IconUtils.drawableToIcon(context, R.drawable.ic_point_on_the_route_black_24dp, -1, 0.9, 0.9);
        ic_took_a_break_spot = IconUtils.drawableToIcon(context, R.drawable.ic_break_spot_icon, -1, 0.9, 0.9);
        ic_waiting_spot = IconUtils.drawableToIcon(context, R.drawable.ic_marker_waiting_for_a_ride_24dp, -1, 0.9, 0.9);
        ic_arrival_spot = IconUtils.drawableToIcon(context, R.drawable.ic_arrival_icon, -1, 0.9, 0.9);

        ic_typeunknown_spot = IconUtils.drawableToIcon(context, R.drawable.ic_edit_location_black_24dp, getIdentifierColorStateList(context, -1));
        ic_got_a_ride_spot0 = IconUtils.drawableToIcon(context, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(context, 0));
        ic_got_a_ride_spot1 = IconUtils.drawableToIcon(context, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(context, 1));
        ic_got_a_ride_spot2 = IconUtils.drawableToIcon(context, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(context, 2));
        ic_got_a_ride_spot3 = IconUtils.drawableToIcon(context, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(context, 3));
        ic_got_a_ride_spot4 = IconUtils.drawableToIcon(context, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(context, 4));

        ic_target = IconUtils.drawableToIcon(context, R.drawable.ic_add, ContextCompat.getColorStateList(context, R.color.mapboxGrayExtraDark));
    }

    /**
     * Setup fabSpotAction1 to be a button that allows saving a new spot.
     **/
    private void setupCreateNewSpotFAB() {
        fabSpotAction1.setImageResource(R.drawable.ic_regular_spot_icon);
        fabSpotAction1.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.ic_regular_spot_color));
        fabSpotAction1.setRippleColor(ContextCompat.getColor(requireContext(), R.color.ic_regular_spot_color_lighter));
        fabSpotAction1.setContentDescription(getString(R.string.save_spot_button_text));
    }

    /**
     * Setup fabSpotAction1 to be a button that allows updating the current spot to a got a ride type.
     **/
    private void setupGotARideFAB() {
        fabSpotAction1.setImageResource(R.drawable.ic_got_a_ride_spot_icon);
        fabSpotAction1.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.ic_got_a_ride_color));
        fabSpotAction1.setRippleColor(ContextCompat.getColor(requireContext(), R.color.ic_got_a_ride_color_lighter));
        fabSpotAction1.setContentDescription(getString(R.string.got_a_ride_button_text));
    }

    /**
     * Setup fabSpotAction2 to be a button that allows updating the current spot to a break type.
     **/
    private void setupTakeABreakFAB() {
        fabSpotAction2.setImageResource(R.drawable.ic_break_spot_icon);
        fabSpotAction2.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.ic_break_color));
        fabSpotAction2.setRippleColor(ContextCompat.getColor(requireContext(), R.color.ic_break_color_lighter));
        fabSpotAction2.setContentDescription(getString(R.string.break_button_text));
    }

    /**
     * Setup fabSpotAction2 to be a button that allows saving a destination spot.
     **/
    private void setupDestinationFAB() {
        fabSpotAction2.setImageResource(R.drawable.ic_arrival_icon);
        fabSpotAction2.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.ic_arrival_color));
        fabSpotAction2.setRippleColor(ContextCompat.getColor(requireContext(), R.color.ic_arrival_color_lighter));
        fabSpotAction2.setContentDescription(getString(R.string.arrived_button_text));
    }

    /**
     * Determines which buttons (save a new hitchhiking spot; save a new destination spot; got a ride; take a break) that should be displayed
     * and displays them as Floating Action Buttons (FAB).
     **/
    private void setupSaveFABs() {
        switch (currentPage) {
            case UNKNOWN:
            default:
                fabSpotAction1.hide();
                fabSpotAction2.hide();
                break;
            case WILL_BE_FIRST_SPOT_OF_A_ROUTE:
                setupCreateNewSpotFAB();

                fabSpotAction1.show();
                fabSpotAction2.hide();
                break;
            case WILL_BE_REGULAR_SPOT:
                setupCreateNewSpotFAB();
                setupDestinationFAB();

                fabSpotAction1.show();
                fabSpotAction2.show();
                break;
            case WAITING_FOR_A_RIDE:
                setupGotARideFAB();
                setupTakeABreakFAB();

                fabSpotAction1.show();
                fabSpotAction2.show();
                break;
        }
    }

    /**
     * Determines what type is the current page type and sets up the save buttons accordinly.
     **/
    private void updateUISaveFABs(List<Spot> spotList, Spot mCurrentWaitingSpot) {
        boolean isWaitingForARide = (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null) ?
                mCurrentWaitingSpot.getIsWaitingForARide() : false;
        if (!isWaitingForARide) {
            if (isFirstSpotOfARoute(spotList))
                currentPage = pageType.WILL_BE_FIRST_SPOT_OF_A_ROUTE;
            else {
                currentPage = pageType.WILL_BE_REGULAR_SPOT;
            }
        } else {
            currentPage = pageType.WAITING_FOR_A_RIDE;
        }

        //Make sure all desired Floating Action Buttons are displayed.
        showAllFAB();
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(requireContext(), getString(R.string.spot_form_user_location_permission_not_granted), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        //As we request at least two different permissions (location and storage) for the users,
        //instead of handling the results for location permission here alone, we've opted to handle it within onRequestPermissionsResult.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (locationPermissionsManager != null)
                locationPermissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationLayer(style);
                if (callback != null)
                    callback.moveMapCameraToNextLocationReceived();
            } else
                Toast.makeText(requireActivity(), getString(R.string.spot_form_user_location_permission_not_granted), Toast.LENGTH_LONG).show();
        }
    }


    @SuppressWarnings({"MissingPermission"})
    /**
     * Setup location component to display the user location on a map.
     * Map camera won't follow location updates by deafult.
     */
    private void setupLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireActivity())) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(requireActivity(), loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            //Map camera should stop following gps updates
            locationComponent.setCameraMode(CameraMode.NONE);

            //Stop showing an arrow considering the compass of the device.
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();
        } else {
            if (locationPermissionsManager == null)
                locationPermissionsManager = new PermissionsManager(this);
            locationPermissionsManager.requestLocationPermissions(requireActivity());
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(requireActivity());

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
    public boolean onMapClick(@NonNull LatLng point) {

        try {
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

            return true;
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }
        return false;
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
        //Prevent from handling multiple clicks
        if (isHandlingRequestToOpenSpotForm)
            return;

        String spotId = feature.getStringProperty(PROPERTY_TAG);
        Crashlytics.setString("Clicked marker tag", spotId);
        Spot spot = null;
        for (Spot spot2 :
                getSpotList()) {
            String id = spot2.getId().toString();
            if (id.equals(spotId)) {
                spot = spot2;
                break;
            }
        }

        if (spot != null) {
            Spot mCurrentWaitingSpot = viewModel.getWaitingSpot().getValue();

            boolean isWaitingForARide = (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null) ?
                    mCurrentWaitingSpot.getIsWaitingForARide() : false;
            //If the user is currently waiting at a spot and the clicked spot is not the one he's waiting at, show a Toast.
            if (isWaitingForARide) {
                if (mCurrentWaitingSpot.getId().equals(spot.getId()))
                    spot.setAttemptResult(null);
                else {
                    Resources res = getResources();
                    String actionRequiredText = res.getString(R.string.evaluate_running_spot_required, res.getString(R.string.got_a_ride_button_text), res.getString(R.string.break_button_text));
                    Toast.makeText(requireContext(), actionRequiredText, Toast.LENGTH_LONG).show();
                    return;
                }
            }

            isHandlingRequestToOpenSpotForm = true;

            SpotFormViewModel spotFormViewModel = new ViewModelProvider(requireActivity()).get(SpotFormViewModel.class);
            spotFormViewModel.setCurrentSpot(spot, false);

            ((MainActivity) requireActivity()).startSpotFormActivityForResult(null, Constants.KEEP_ZOOM_LEVEL, true);
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
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, ROUTES_SPOTS_STYLE_LAYER_ID);
        if (!features.isEmpty()) {
            String tag = features.get(0).getStringProperty(PROPERTY_TAG);
            List<Feature> featureList = spotsCollection.features();

            for (int i = 0; i < featureList.size(); i++) {
                if (featureList.get(i).getStringProperty(PROPERTY_TAG).equals(tag)) {
                    setSelected(i, true);
                }
            }
        } else {
            deselectAll();
            refreshSpotsSource();
            refreshSubRoutesSource();
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

        deselectAll();

        Feature feature = spotsCollection.features().get(index);

        //If spotList has been changed, we want to remove any balloon that might have been generated for this feature and then regenerate it below with updated data.
        //TODO: Looks like it would make more sense to call removeImage removing all balloons already generated always that spotList is updated. Consider moving it into setupAnnotations(). All the other features are already been removed and re-added within that method.
        if (spotListWasChanged && style.getImage(feature.id()) != null)
            style.removeImage(feature.id());

        if (style.getImage(feature.id()) == null) {
            showProgressDialog("Loading data..");

            //Generate bitmaps from the layout_callout view that should appear when a icon is clicked
            new GenerateBalloonsTask(this, spotsCollection.features().get(index)).execute();
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
        refreshSpotsSource();
    }

    /**
     * Deselects the state of all the features
     */
    private void deselectAll() {
        if (spotsCollection != null && spotsCollection.features() != null) {
            for (Feature feature : spotsCollection.features()) {
                feature.properties().addProperty(PROPERTY_SELECTED, false);
            }
        }
    }

    /**
     * Selects the state of a feature
     *
     * @param feature the feature to be selected.
     */
    private void selectFeature(Feature feature) {
        feature.properties().addProperty(PROPERTY_SELECTED, true);
    }

    private void setupIconImages(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage(ic_single_spot.getId(), ic_single_spot.getBitmap());
        loadedMapStyle.addImage(ic_point_on_the_route_spot.getId(), ic_point_on_the_route_spot.getBitmap());
        loadedMapStyle.addImage(ic_waiting_spot.getId(), ic_waiting_spot.getBitmap());
        loadedMapStyle.addImage(ic_arrival_spot.getId(), ic_arrival_spot.getBitmap());
        loadedMapStyle.addImage(ic_typeunknown_spot.getId(), ic_typeunknown_spot.getBitmap());
        loadedMapStyle.addImage(ic_got_a_ride_spot0.getId(), ic_got_a_ride_spot0.getBitmap());
        loadedMapStyle.addImage(ic_got_a_ride_spot1.getId(), ic_got_a_ride_spot1.getBitmap());
        loadedMapStyle.addImage(ic_got_a_ride_spot2.getId(), ic_got_a_ride_spot2.getBitmap());
        loadedMapStyle.addImage(ic_got_a_ride_spot3.getId(), ic_got_a_ride_spot3.getBitmap());
        loadedMapStyle.addImage(ic_got_a_ride_spot4.getId(), ic_got_a_ride_spot4.getBitmap());
        loadedMapStyle.addImage(ic_target.getId(), ic_target.getBitmap());
    }

    private LatLng convertToLatLng(Feature feature) {
        Point symbolPoint = (Point) feature.geometry();
        return new LatLng(symbolPoint.latitude(), symbolPoint.longitude());
    }

    public void updateUI(List<Spot> spotList) {
        //We want the map camera also updated.
        this.shouldZoomToFitAllMarkers = true;
        this.spotListWasChanged = true;

        if (!Utils.isNetworkAvailable(requireActivity()) && !Utils.shouldLoadCurrentView(prefs)) {
            prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_LAST_OFFLINE_MODE_WARN, System.currentTimeMillis()).apply();
            showInternetUnavailableAlertDialog(spotList);
            dismissProgressDialog();
        } else
            drawAnnotationsOnFinishLoadingStyle(spotList);

        if (dateRangeDialog != null) {
            dateRangeDialog.dismiss();
            dateRangeDialog = null;
        }
    }

    private void showInternetUnavailableAlertDialog(List<Spot> spotList) {
        new AlertDialog.Builder(requireActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.general_network_unavailable_message))
                .setMessage(getResources().getString(R.string.map_error_alert_map_not_loaded_message))
                .setPositiveButton(getResources().getString(R.string.map_error_alert_map_not_loaded_positive_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putBoolean(Constants.PREFS_OFFLINE_MODE_SHOULD_LOAD_CURRENT_VIEW, false).apply();

                        startMyRoutesActivity();
                    }
                })
                .setNegativeButton(String.format(getString(R.string.action_button_label), getString(R.string.view_map_button_label)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putBoolean(Constants.PREFS_OFFLINE_MODE_SHOULD_LOAD_CURRENT_VIEW, true).apply();

                        drawAnnotationsOnFinishLoadingStyle(spotList);
                    }
                }).show();
    }

    private void drawAnnotationsOnFinishLoadingStyle(List<Spot> spotList) {
        if (mapboxMap == null || style == null || !style.isFullyLoaded()) {
            //Call drawAnnotations(spotList) again once map style finishes loading.
            mapView.addOnDidFinishLoadingStyleListener(onDidFinishLoadingStyle);
            return;
        }
        drawAnnotations(spotList);
    }

    private void drawAnnotations(List<Spot> spotList) {
        showProgressDialog(getString(R.string.map_drawing_progress_text));
        Spot[] spotArray = new Spot[spotList.size()];
        this.loadTask = new DrawAnnotationsTask(this).execute(spotList.toArray(spotArray));
    }

    /**
     * Listener that should be called once map style finishes loading.
     **/
    private MapView.OnDidFinishLoadingStyleListener onDidFinishLoadingStyle = () -> {
        moveCameraToLastKnownLocation((int) mapboxMap.getMinZoomLevel(), new MapboxMap.CancelableCallback() {
            @Override
            public void onCancel() {
                removeOnDidFinishLoadingStyleListener();
                drawAnnotations(getSpotList());
            }

            @Override
            public void onFinish() {
                removeOnDidFinishLoadingStyleListener();
                drawAnnotations(getSpotList());
            }
        });
    };

    private void removeOnDidFinishLoadingStyleListener() {
        mapView.removeOnDidFinishLoadingStyleListener(onDidFinishLoadingStyle);
    }

    private List<Spot> getSpotList() {
        Activity activity = requireActivity();

        if (activity instanceof MainActivity) {
            List<Spot> lst = viewModel.getSpots().getValue();
            if (lst != null) return lst;
        }

        return new ArrayList<>();
    }

    private void setupAnnotations
            (ArrayList<Feature> spots, ArrayList<Feature> linesForSubRoutes,
             int numberOfSpotsOnMostRecentRoute) {
        //Define the number of spots to be considered when zoomOutToFitMostRecentRoute is called.
        //We want to include at least the spots from the most recent route.
        if (numberOfSpotsOnMostRecentRoute > NUMBER_OF_SPOTS_TO_FIT)
            NUMBER_OF_SPOTS_TO_FIT = numberOfSpotsOnMostRecentRoute;

        this.spotsCollection = FeatureCollection.fromFeatures(spots);
        this.subRoutesCollection = FeatureCollection.fromFeatures(linesForSubRoutes);

        if (mapboxMap == null) {
            return;
        }

        if (style.isFullyLoaded()) {
            setupSpotsSource(style);
            setupSubRoutesSource(style);
            setupSpotsStyleLayer(style);
            setupSubRoutesStyleLayer(style);
            //Setup a layer with Android SDK call-outs (title of the feature is used as key for the iconImage)
            setupCalloutLayer(style);

            try {
                //Automatically zoom out to fit all markers only the first time that spots are loaded.
                // Otherwise it can be annoying to loose your zoom when navigating back after editing a spot. In anyways, there's a button to do this zoom if/when the user wish.
                if (shouldZoomToFitAllMarkers) {
                    if (getSpotList().size() == 0) {
                        //If there's no spot to show, make map camera follow the GPS updates.
                        //Because it might take some time til Location Engine gets started and the first location update is received,
                        //let's display a "waiting for GPS" message.
                        Toast.makeText(requireActivity(), getString(R.string.waiting_for_gps), Toast.LENGTH_LONG).show();
                        callback.moveMapCameraToNextLocationReceived();
                    } else
                        zoomOutToFitMostRecentRoute();
                }

            } catch (Exception ex) {
                Crashlytics.logException(ex);
                showErrorAlert(getResources().getString(R.string.general_error_dialog_title),
                        String.format(getResources().getString(R.string.general_error_dialog_message),
                                "Adding markers failed - " + ex.getMessage()));
            }
        }

        dismissProgressDialog();
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
        if (mapView != null)
            mapView.onStart();
    }

    @Override
    public void onStop() {
        Crashlytics.log(Log.INFO, TAG, "onStop called");
        super.onStop();
        if (mapView != null)
            mapView.onStop();

        /*
         * The device may have been rotated and the activity is going to be destroyed
         * you always should be prepared to cancel your AsnycTasks before the Activity
         * which created them is going to be destroyed.
         * And dont rely on mayInteruptIfRunning
         */
        if (this.loadTask != null) {
            this.loadTask.cancel(false);
            dismissProgressDialog();
        }
    }

    @Override
    public void onResume() {
        Crashlytics.log(Log.INFO, TAG, "onResume called");
        super.onResume();
        if (mapView != null)
            mapView.onResume();
        isHandlingRequestToOpenSpotForm = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        isHandlingRequestToOpenSpotForm = false;

        if (resultCode == Constants.RESULT_OBJECT_ADDED || resultCode == Constants.RESULT_OBJECT_EDITED) {
            showSpotSavedSnackbar();
        }

        if (resultCode == Constants.RESULT_OBJECT_DELETED) {
            showSpotDeletedSnackbar();
        }

        //We should get PREFS_MYSPOTLIST_WAS_CHANGED equals to true always that resultCode is RESULT_OBJECT_ADDED, RESULT_OBJECT_EDITED or RESULT_OBJECT_DELETED,
        // so the other verifications together with it below should actually never be reached.
        if (prefs.getBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, false) ||
                resultCode == Constants.RESULT_OBJECT_ADDED || resultCode == Constants.RESULT_OBJECT_EDITED || resultCode == Constants.RESULT_OBJECT_DELETED) {
            spotListWasChanged = true;

            //We want the map camera to be adjusted when one or more spots have been added, edited or deleted.
            // Please note: we do not want it to be adjusted always that spotList is reloaded, that's why shouldZoomToFitAllMarkers is been set here and not on onSpotListChanged().
            //TODO: Consider also zooming out to fit all markers always when user navigates back AND there's no features within the viewport.
            shouldZoomToFitAllMarkers = true;
        } else {
            //We don't want the map camera to be moved if the user navigates back without doing any change to the spot list.
            shouldZoomToFitAllMarkers = false;
        }

        //NOTE: onSpotListChanged() will be called after MyMapsFragment.onActivityResult() by MainActivity.onActivityResult()
    }

    @Override
    public void onPause() {
        Crashlytics.log(Log.INFO, TAG, "onPause was called");
        super.onPause();

        if (mapView != null)
            mapView.onPause();

        dismissSnackbar();
        dismissProgressDialog();

        if (dateRangeDialog != null)
            dateRangeDialog.dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Crashlytics.log(Log.INFO, TAG, "onSaveInstanceState called");
        super.onSaveInstanceState(savedInstanceState);

        if (mapView != null)
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
    public void onDestroyView() {
        Crashlytics.log(Log.INFO, TAG, "onDestroyView was called");
        super.onDestroyView();
        if (mapboxMap != null) {
            mapboxMap.removeOnMapClickListener(this);
        }
        if (mapView != null)
            mapView.onDestroy();
        ld.removeObservers(requireActivity());
    }

    @Override
    public void onLowMemory() {
        Crashlytics.log(Log.WARN, TAG, "onLowMemory was called");
        super.onLowMemory();
        if (mapView != null)
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
                startMyRoutesActivity();
                selectionHandled = true;
                break;
            case R.id.action_new_spot:
                //Prevent from handling multiple clicks
                if (isHandlingRequestToOpenSpotForm)
                    break;

                //If mapboxMap was not loaded, we can't track the user location using MapBox.
                if (mapboxMap == null) {
                    new AlertDialog.Builder(requireActivity())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getString(R.string.save_spot_button_text))
                            .setMessage(getString(R.string.general_offline_mode_label)
                                    + ". " + getString(R.string.map_error_alert_map_not_loaded_alternative_message))
                            .setPositiveButton(getResources().getString(R.string.map_error_alert_map_not_loaded_positive_button), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startMyRoutesActivity();
                                }
                            })
                            .setNegativeButton(getResources().getString(R.string.general_cancel_option), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (getSpotList().size() == 0) {
                                        //If there's no spot to show, move the map camera to the current GPS location.
                                        callback.moveMapCameraToNextLocationReceived();
                                    }
                                }
                            }).show();
                } else
                    saveSpotButtonHandler(false);
                selectionHandled = true;
                break;

            case R.id.action_toggle_icons:
                toggleAllFAB();
                break;
            case R.id.action_filter_by_date:
                showSpotsSavedBetweenPeriodOfTimeFromMap();
                break;
            case R.id.action_zoom_to_fit_all:
                zoomOutToFitMostRecentRoute();
                break;
        }

        if (selectionHandled)
            return true;
        else
            return super.onOptionsItemSelected(item);
    }

    /**
     * Hides or shows all Floating Action Buttons (FAB) from the view.
     **/
    private void toggleAllFAB() {
        if (!mapButtonsAreDisplayed) {
            showAllFAB();
        } else {
            hideAllFAB();
        }
    }

    /**
     * Shows all Floating Action Buttons (FAB) from the view.
     **/
    private void showAllFAB() {
        fabLocateUser.show();
        fabZoomIn.show();
        fabZoomOut.show();

        //Call setupSaveFABs to show only the save buttons that should be shown
        setupSaveFABs();

        mapButtonsAreDisplayed = true;
    }

    /**
     * Hides all Floating Action Buttons (FAB) from the view.
     **/
    private void hideAllFAB() {
        fabLocateUser.hide();
        fabZoomIn.hide();
        fabZoomOut.hide();

        //Hide save buttons
        fabSpotAction1.hide();
        fabSpotAction2.hide();

        mapButtonsAreDisplayed = false;
    }

    private void showAllRoutesOnMap() {
        if (spotsCollection == null) {
            Toast.makeText(requireActivity(), getString(R.string.general_spots_list_not_loaded), Toast.LENGTH_SHORT).show();
            return;
        }

        //Update all features setting PROPERTY_SHOULDHIDE to false
        for (Feature f : spotsCollection.features())
            f.properties().addProperty(PROPERTY_SHOULDHIDE, false);

        refreshSpotsSource();

        for (Feature f : subRoutesCollection.features())
            f.properties().addProperty(PROPERTY_SHOULDHIDE, false);

        refreshSubRoutesSource();
    }

    /*
     * Hides old routes, showing on the map only polylines and spots that belong to the most recent route.
     * Spots that don't belong to any route shall be hidden as well.
     * */
    private void hideOldRoutesFromMap() {
        if (subRoutesCollection == null || subRoutesCollection.features() == null || subRoutesCollection.features().isEmpty())
            return;

        int lastFeatureIndex = subRoutesCollection.features().size() - 1;
        Feature lastFeature = subRoutesCollection.features().get(lastFeatureIndex);
        int lastRouteIndex = (int) lastFeature.getNumberProperty(PROPERTY_ROUTEINDEX);

        hideOldRoutesFromMap(lastRouteIndex);
    }

    private void hideOldRoutesFromMap(int lastRouteIndex) {
        //Remove all spots except the ones belonging to the most recent route.
        // Updates all features defining if they should be hidden or not.
        for (Feature f : spotsCollection.features()) {
            int spotRouteIndex = (int) f.getNumberProperty(PROPERTY_ROUTEINDEX);

            if (spotRouteIndex == lastRouteIndex)
                f.properties().addProperty(PROPERTY_SHOULDHIDE, false);
            else
                f.properties().addProperty(PROPERTY_SHOULDHIDE, true);
        }

        refreshSpotsSource();

        for (Feature f : subRoutesCollection.features()) {
            int spotRouteIndex = (int) f.getNumberProperty(PROPERTY_ROUTEINDEX);

            if (spotRouteIndex == lastRouteIndex)
                f.properties().addProperty(PROPERTY_SHOULDHIDE, false);
            else
                f.properties().addProperty(PROPERTY_SHOULDHIDE, true);
        }

        refreshSubRoutesSource();
    }

    /*
     * Hides old routes, showing on the map only polylines and spots that belong to the most recent route.
     * Spots that don't belong to any route shall be hidden as well.
     * */
    private void showSpotsSavedBetweenPeriodOfTimeFromMap() {
        if (subRoutesCollection == null || subRoutesCollection.features() == null || subRoutesCollection.features().isEmpty())
            return;

        // Gets all DateTimes in UTC.
        List<DateTime> startDates = SpotsListHelper.getOldestDatesOnEachRoute(spotsCollection.features()),
                endDates = SpotsListHelper.getMostRecentDatesOnEachRoute(spotsCollection.features());

        // Add the StartDateTime of single spots to startDates so that user may choose their dates as well. These dates are also in UTC.
        startDates.addAll(SpotsListHelper.getSingleSpotsDates(spotsCollection.features()));

        openDateRangePickerDialog(startDates, endDates);
    }

    private void openDateRangePickerDialog(List<DateTime> startDates, List<DateTime> endDates) {
        if (dateRangeDialog != null) {
            dateRangeDialog.showAgain();
            return;
        }

        Context context = getContext();
        if (context == null)
            return;

        ArrayList<Date> startDatesDate = new ArrayList<>();
        for (DateTime startDate : startDates)
            startDatesDate.add(startDate.toDate());
        ArrayList<Date> endDatesDate = new ArrayList<>();
        for (DateTime endDate : endDates)
            endDatesDate.add(endDate.toDate());

        dateRangeDialog = new DateRangePickerDialog(context);
        dateRangeDialog.setRangeOptions(startDatesDate, endDatesDate, new DateRangePickerDialog.DateRangeListener() {
            @Override
            public void onRangeSelected(List<Date> selectedDates) {
                // Close balloons if any is open
                deselectAll();
                onDateRangeWasPicked(new DateTime(selectedDates.get(0)), new DateTime(selectedDates.get(selectedDates.size() - 1)));
                dateRangeDialog.dismiss();
            }

            @Override
            public void onRangeCleared() {
                // Close balloons if any is open
                deselectAll();
                showAllRoutesOnMap();
                dateRangeDialog.dismiss();
            }
        });
        dateRangeDialog.show();
    }

    void onDateRangeWasPicked(DateTime periodBeginsOn, DateTime periodEndsOn) {
        if (periodBeginsOn.isAfter(periodEndsOn))
            return;

        List<Integer> routeIndexesWithinPeriodOfTime = SpotsListHelper.getAllRouteIndexesOfFeaturesWithinDates(spotsCollection.features(), periodBeginsOn, periodEndsOn);
        showSpotsSavedBetweenPeriodOfTimeFromMap(routeIndexesWithinPeriodOfTime, periodBeginsOn, periodEndsOn);
    }

    private void showSpotsSavedBetweenPeriodOfTimeFromMap(List<Integer> routeIndexesToShow, DateTime periodStartsOn, DateTime periodEndsOn) {
        // For each spot drawn on the map as feature
        for (Feature f : spotsCollection.features()) {
            boolean shouldHide;

            // If spot is of type single
            if ((int) f.getNumberProperty(MyMapsFragment.PROPERTY_SPOTTYPE) == Constants.SPOT_TYPE_SINGLE_SPOT) {
                DateTime dateTime = SpotsListHelper.getStartDateTimeFrom(f);
                // Show only if it was saved on the given dates or between them.
                shouldHide = !SpotsListHelper.shouldIncludeSpot(dateTime, periodStartsOn, periodEndsOn);
            } else {
                // Spot is not of type single, let's check its route index.
                int spotRouteIndex = (int) f.getNumberProperty(PROPERTY_ROUTEINDEX);
                // Show only if it belongs to any of the routes in routeIndexesToShow.
                shouldHide = !routeIndexesToShow.contains(spotRouteIndex);
            }
            f.properties().addProperty(PROPERTY_SHOULDHIDE, shouldHide);
        }

        refreshSpotsSource();

        // For each route line drawn on the map as a feature
        for (Feature f : subRoutesCollection.features()) {
            int spotRouteIndex = (int) f.getNumberProperty(PROPERTY_ROUTEINDEX);
            // Show only if it belongs to any of the routes in routeIndexesToShow.
            f.properties().addProperty(PROPERTY_SHOULDHIDE, !routeIndexesToShow.contains(spotRouteIndex));
        }

        refreshSubRoutesSource();
    }

    private void startMyRoutesActivity() {
        Intent intent = new Intent(requireActivity(), MyRoutesActivity.class);
        intent.putExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, true);
        startActivityForResult(intent, Constants.EDIT_SPOT_REQUEST);
    }

    /**
     * Zoom out to fit the most recent (@link #NUMBER_OF_SPOTS_TO_FIT) spots saved to the map AND the user's last known location.
     **/
    @SuppressWarnings({"MissingPermission"})
    private void zoomOutToFitMostRecentRoute() {
        try {
            if (mapboxMap != null) {
                Location mCurrentLocation = tryGetLastKnownLocation();

                List<LatLng> lst = new ArrayList<>();
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                List<Feature> features = spotsCollection == null ? new ArrayList<>() : spotsCollection.features();

                //Consider the last saved spots
                if (!features.isEmpty()) {
                    for (int i = features.size() - 1; i >= 0 && lst.size() < NUMBER_OF_SPOTS_TO_FIT; i--) {
                        Feature feature = features.get(i);
                        //Include only features that are actually seen on the map (hidden features are excluded)
                        if (!feature.getBooleanProperty(PROPERTY_SHOULDHIDE)) {
                            Point p = ((Point) feature.geometry());
                            lst.add(new LatLng(p.latitude(), p.longitude()));
                        }
                    }
                }

                //Add current location to camera bounds
                if (mCurrentLocation != null)
                    lst.add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));

                if (lst.size() == 1)
                    moveCamera(new LatLng(lst.get(0).getLatitude(), lst.get(0).getLongitude()), Constants.ZOOM_TO_SEE_FARTHER_DISTANCE);
                else if (lst.size() > 1) {
                    builder.includes(lst);
                    LatLngBounds bounds = builder.build();

                    int bestPadding = 120;
                    //If there's only 2 points in the list and the current location is known (which means only one of them is a saved spot) we want a longer padding.
                    if (mCurrentLocation != null && lst.size() == 2)
                        bestPadding = 150;

                    //The change that should be applied to the camera.
                    final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, bestPadding);

                    MapboxMap.CancelableCallback callback = new MapboxMap.CancelableCallback() {
                        @Override
                        public void onCancel() {
                            mapboxMap.easeCamera(cameraUpdate, 5000);
                        }

                        @Override
                        public void onFinish() {
                            mapboxMap.easeCamera(cameraUpdate, 5000);
                        }
                    };

                    /*
                     * We want to animate the map to nicely display the points added to the bounds.
                     * We've chosen to do it by calling moveCamera first and then easeCamera.
                     * Here's the reason:
                     * Mapbox.easeCamera animates the map camera by first moving it towards a point in the middle of the map,
                     * and then finally moving it on a way to display all the points added to the bounds.
                     * Example: If zoom level is at the minimum (so the entire world map is being displayed) and user has only saved spots
                     * at one extreme of the world (let's say, northern Canada or southern Argentina) then what Mapbox.easeCamera does is-
                     * firstly it zooms towards a point at the middle of the map (he was seeing the world map, then now the camera zooms to a point in the middle
                     * of the world, very far from where the saved spots are) and secondly it "flies" towards where the points added to the bounds actually are.
                     * Which means that during a few seconds the user watched the map flying over random regions where spots haven't been saved.
                     * To see how it would look like (if you feel the need to it) you can test by saving spots as in this example's scenario and then
                     * removing the following line (containing Mapbox.moveCamera) and calling mapboxMap.easeCamera directly.
                     * Here's how the chosen solution works:
                     * By doing these two steps (calling moveCamera then easeCamera) we start by immediately placing the map camera
                     * in a way that all points added to the bounds are already visible to the user though in a more distant level (we do that using a longer padding);
                     * and then we call easeCamera to add the animation which will zoom closer to the bounds (using a smaller padding).
                     */
                    mapboxMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 500), callback);
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

    int FAVORITE_ZOOM_LEVEL_NOT_INFORMED = -1;

    @Override
    public void moveCameraToLastKnownLocation() {
        moveCameraToLastKnownLocation(FAVORITE_ZOOM_LEVEL_NOT_INFORMED, null);
    }

    /**
     * Move map camera to the last GPS location OR if it's not available,
     * we'll try to move the map camera to the location of the last saved spot.
     *
     * @param zoomLevel The zoom level that should be used or FAVORITE_ZOOM_LEVEL_NOT_INFORMED if we should use what we think could be the best zoom level.
     */
    @SuppressWarnings({"MissingPermission"})
    public void moveCameraToLastKnownLocation(int zoomLevel,
                                              @Nullable MapboxMap.CancelableCallback callback) {
        if (!style.isFullyLoaded())
            return;

        //Request permission of access to GPS updates or
        // directly initialize and enable the location plugin if such permission was already granted.
        enableLocationLayer(style);

        //Zoom close to current position or to the last saved position
        LatLng cameraPositionTo = null;
        int bestZoomLevel = Constants.KEEP_ZOOM_LEVEL;

        if (zoomLevel == FAVORITE_ZOOM_LEVEL_NOT_INFORMED && !isCameraZoomChangedByUser())
            bestZoomLevel = Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT;

        Location loc = tryGetLastKnownLocation();

        if (loc != null) {
            cameraPositionTo = new LatLng(loc);
        } else {
            //Set start position for map camera: set it to the last spot saved
            Spot lastAddedSpot = viewModel.getLastAddedRouteSpot(getContext());
            if (lastAddedSpot != null && lastAddedSpot.getLatitude() != null && lastAddedSpot.getLongitude() != null
                    && lastAddedSpot.getLatitude() != 0.0 && lastAddedSpot.getLongitude() != 0.0) {
                cameraPositionTo = new LatLng(lastAddedSpot.getLatitude(), lastAddedSpot.getLongitude());

                //If at the last added spot the user has gotten a ride, then he might be far from that spot.
                // Let's zoom farther away from it.
                if (zoomLevel == FAVORITE_ZOOM_LEVEL_NOT_INFORMED &&
                        (lastAddedSpot.getAttemptResult() != null && lastAddedSpot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE))
                    bestZoomLevel = Constants.ZOOM_TO_SEE_FARTHER_DISTANCE;
            }
        }

        //If a zoomLevel has been informed, use it
        if (zoomLevel != FAVORITE_ZOOM_LEVEL_NOT_INFORMED)
            bestZoomLevel = zoomLevel;

        if (cameraPositionTo != null)
            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraPositionTo, bestZoomLevel), callback);
//  moveCamera(cameraPositionTo, bestZoomLevel);
    }

    boolean isCameraZoomChangedByUser() {
        //If current zoom level different than the default (world level)
        return mapboxMap.getCameraPosition().zoom != mapboxMap.getMinZoomLevel();
    }

    private void showProgressDialog(String message) {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(requireActivity());
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


    /*
    Draws spots and routes on the map
     */
    private static class DrawAnnotationsTask extends AsyncTask<Spot, Void, Boolean> {
        private final WeakReference<MyMapsFragment> activityRef;
        ArrayList<Feature> spotsListAsFeatures = new ArrayList<>();
        ArrayList<Feature> linesForSubRoutes = new ArrayList<>();
        int numberOfSpotsOnMostRecentRoute = -1;

        DrawAnnotationsTask(MyMapsFragment activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected Boolean doInBackground(Spot... spotList) {
            if (isCancelled())
                return false;

            MyMapsFragment activity = activityRef.get();
            if (activity == null)
                return false;

            Hashtable<Long, String> totalsToDestinations = SpotsListHelper.SumRouteTotalsAndUpdateTheirDestinationNotes(activity.getContext(), Arrays.asList(spotList));
            List<Route> routes = new ArrayList<>();
            List<Spot> singleSpots = new ArrayList<>();

            List<Spot> inverseList = new ArrayList<>();
            for (int i = spotList.length - 1; i >= 0; i--) {
                inverseList.add(spotList[i]);
            }

            Utils.parseDBSpotList(inverseList, routes, singleSpots);

            if (routes.size() > 0)
                numberOfSpotsOnMostRecentRoute = routes.get(routes.size() - 1).spots.size();

            //Convert every spot that belong to no route (a.k.a single spot) into a Feature with properties
            for (int j = 0; j < singleSpots.size(); j++) {
                spotsListAsFeatures.add(GetFeature(singleSpots.get(j), 0, false, "", activityRef));
            }

            for (int i = 0; i < routes.size(); i++) {
                Route r = routes.get(i);

                //Convert every spot that belong to a route into a Feature with properties
                for (int j = 0; j < r.spots.size(); j++) {
                    Spot s = r.spots.get(j);
                    spotsListAsFeatures.add(GetFeature(s, i, (j == 0), totalsToDestinations.get(s.getId()), activityRef));
                }

                //Convert every SubRoute into a LineString with properties (color, style and routeIndex)
                for (SubRoute sr : r.subRoutes) {
                    // Create the LineString from the list of coordinates and then make a GeoJSON
                    // FeatureCollection so we can add the line to our map as a layer.
                    linesForSubRoutes.add(getLineFeature(sr, i));
                }
            }

            return true;
        }

        @NonNull
        /* Get snippet in the following format: "spotLocationToString{firstSeparator}{dateTimeToString}{secondSeparator}({getWaitingTimeAsString}){thirdSeparator}{getNote}" */
        private static String getSnippet(MyMapsFragment activity, Spot spot, String firstSeparator, String secondSeparator, String thirdSeparator) {
            //Get location string
            String snippet = spotLocationToString(spot).trim();

            if (!snippet.isEmpty())
                snippet += firstSeparator;

            //Add date time if it is set
            if (spot.getStartDateTime() != null) {
                DateTime startDateTime = spot.getStartDateTime();
                String separator = activity.getString(R.string.general_date_at_time, " 'EEE'", "");
                String dateTimeFormat = Utils.getDateTimeFormat(startDateTime, separator);
                snippet += Utils.dateTimeToString(startDateTime, dateTimeFormat);
            }

            //Add waiting time
            if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot() &&
                    spot.getWaitingTime() != null) {
                if (!snippet.isEmpty())
                    snippet += secondSeparator;
                snippet += "(" + Utils.getWaitingTimeAsString(spot.getWaitingTime(), activity.getContext()) + ")";
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
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (isCancelled())
                return;

            MyMapsFragment activity = activityRef.get();
            if (activity == null)
                return;

            activity.setupAnnotations(spotsListAsFeatures, linesForSubRoutes, numberOfSpotsOnMostRecentRoute);
        }

        static Feature GetFeature(Spot spot, int routeIndex, boolean isOrigin, String routeTotalsForDestinationSpotsOnly, WeakReference<MyMapsFragment> activityRef) {
            MyMapsFragment activity = activityRef.get();
            if (activity == null)
                return null;

            LatLng pos = new LatLng(spot.getLatitude(), spot.getLongitude());
            String tag = spot.getId() != null ? spot.getId().toString() : "";
            String snippet = getSnippet(activity, spot, "\n", " ", "\n");

            if (spot.getIsDestination() != null && spot.getIsDestination())
                snippet += "\n" + routeTotalsForDestinationSpotsOnly;

            JsonObject properties = new JsonObject();
            properties.addProperty(PROPERTY_ROUTEINDEX, routeIndex);
            properties.addProperty(PROPERTY_TAG, tag);
            properties.addProperty(PROPERTY_SNIPPET, snippet);
            properties.addProperty(PROPERTY_SHOULDHIDE, false);
            properties.addProperty(PROPERTY_STARTDATETIME_IN_MILLISECS, spot.getStartDateTimeMillis());

            setTitleIconAndType(spot, properties, isOrigin, routeIndex, activity);

            return Feature.fromGeometry(Point.fromLngLat(pos.getLongitude(), pos.getLatitude()), properties, tag);
        }


        static Feature getLineFeature(SubRoute sr, int routeIndex) {
            JsonObject properties = new JsonObject();
            properties.addProperty(PROPERTY_ROUTEINDEX, routeIndex);
            properties.addProperty(PROPERTY_SHOULDHIDE, false);
            properties.addProperty("isHitchhikingRoute", sr.isHitchhikingRoute);

            int lineColorId = -1;
            if (sr.isHitchhikingRoute)
                lineColorId = getLineColorId(routeIndex);
            properties.addProperty("lineColor", lineColorId);

            return Feature.fromGeometry(LineString.fromLngLats(sr.points), properties);
        }

        static void setTitleIconAndType(Spot spot, JsonObject properties, boolean isOrigin, int routeIndex, MyMapsFragment activity) {
            String markerTitle = "";
            String icon;
            int type;

            //If spot belongs to a route (it's not a single spot)
            if (spot.getIsPartOfARoute() != null && spot.getIsPartOfARoute()) {

                //If spot is a hitchhiking spot where the user is waiting for a ride
                if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot() &&
                        spot.getIsWaitingForARide() != null && spot.getIsWaitingForARide()) {
                    //The spot is where the user is waiting for a ride

                    markerTitle = activity.getString(R.string.map_infoview_spot_type_waiting);
                    icon = activity.ic_waiting_spot.getId();
                    type = Constants.SPOT_TYPE_WAITING;

                } else if (spot.getIsDestination() != null && spot.getIsDestination()) {
                    //The spot is a destination

                    markerTitle = activity.getString(R.string.map_infoview_spot_type_destination);
                    icon = activity.ic_arrival_spot.getId();
                    type = Constants.SPOT_TYPE_DESTINATION;
                } else if (spot.getIsNotHitchhikedFromHere() != null && spot.getIsNotHitchhikedFromHere()) {
                    //The spot is a destination

                    markerTitle = activity.getString(R.string.map_infoview_spot_type_got_off_here);
                    icon = activity.ic_point_on_the_route_spot.getId();
                    type = Constants.SPOT_TYPE_GOT_OFF_HERE;
                } else {
                    if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot()) {
                        //The spot is a hitchhiking spot
                        type = Constants.SPOT_TYPE_HITCHHIKING_SPOT;

                        switch (spot.getAttemptResult()) {
                            case Constants.ATTEMPT_RESULT_GOT_A_RIDE:
                                //The spot is a hitchhiking spot that was already evaluated
                                icon = activity.getGotARideIconForRoute(routeIndex).getId();
                                markerTitle = Utils.getRatingOrDefaultAsString(activity.getContext(), spot.getHitchability() != null ? spot.getHitchability() : 0);
                                break;
                            case Constants.ATTEMPT_RESULT_TOOK_A_BREAK:
                                //The spot is a hitchhiking spot that was already evaluated
                                //icon = ic_took_a_break_spot;
                                icon = activity.ic_point_on_the_route_spot.getId();
                                markerTitle = activity.getString(R.string.map_infoview_spot_type_break);
                                break;
                            default:
                                icon = activity.getGotARideIconForRoute(routeIndex).getId();
                                markerTitle = activity.getString(R.string.map_infoview_spot_type_unknown_attempt_result);
                                break;
                        }

                    } else {
                        //The spot belongs to a route but it's not a hitchhiking spot, neither a destination
                        icon = activity.ic_point_on_the_route_spot.getId();
                        type = Constants.SPOT_TYPE_POINT_ON_THE_ROUTE;
                    }

                    if (isOrigin) {
                        //The spot is the origin of a route
                        type = Constants.SPOT_TYPE_ORIGIN;
                        if (!markerTitle.isEmpty())
                            markerTitle = activity.getString(R.string.map_infoview_spot_type_origin) + " " + markerTitle;
                        else
                            markerTitle = activity.getString(R.string.map_infoview_spot_type_origin);
                    }
                }
            } else {
                //This spot doesn't belong to a route (it's a single spot)

                if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot()) {
                    if (spot.getAttemptResult() == null)
                        markerTitle = activity.getString(R.string.map_infoview_spot_type_unknown_attempt_result);
                    else
                        markerTitle = Utils.getRatingOrDefaultAsString(activity.getContext(), spot.getHitchability() != null ? spot.getHitchability() : 0);

                    icon = activity.ic_single_spot.getId();
                    type = Constants.SPOT_TYPE_SINGLE_SPOT;
                } else {
                    icon = activity.ic_point_on_the_route_spot.getId();
                    type = Constants.SPOT_TYPE_POINT_ON_THE_ROUTE;
                }
            }

            //Set hitchability as title
            markerTitle = markerTitle.toUpperCase();

            properties.addProperty(PROPERTY_ICONIMAGE, icon);
            properties.addProperty(PROPERTY_SPOTTYPE, type);
            properties.addProperty(PROPERTY_TITLE, markerTitle);
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
                LayoutInflater inflater = LayoutInflater.from(activity.getContext());

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
        if (style.isFullyLoaded()) {
            // calling addImages is faster as separate addImage calls for each bitmap.
            style.addImages(imageMap);
        }
        // need to store reference to views to be able to use them as hitboxes for click events.
        //this.viewMap = viewMap;
    }

    protected void showErrorAlert(String title, String msg) {
        new AlertDialog.Builder(requireActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.general_ok_option), null)
                .show();
    }

    private void setupSpotsSource(@NonNull Style loadedMapStyle) {
        if (loadedMapStyle.getSource(SPOTS_SOURCE_ID) == null) {
            spotSource = new GeoJsonSource(SPOTS_SOURCE_ID, spotsCollection);
            loadedMapStyle.addSource(spotSource);
        } else
            refreshSpotsSource();
    }

    private void setupSubRoutesSource(@NonNull Style loadedMapStyle) {
        if (loadedMapStyle.getSource(SUB_ROUTE_SOURCE_ID) == null) {
            subRoutesSource = new GeoJsonSource(SUB_ROUTE_SOURCE_ID, subRoutesCollection);
            loadedMapStyle.addSource(subRoutesSource);
        } else
            refreshSubRoutesSource();
    }

    /* Setup style layer */
    private void setupSpotsStyleLayer(@NonNull Style loadedMapStyle) {
        if (loadedMapStyle.getLayer(ROUTES_SPOTS_STYLE_LAYER_ID) == null) {
            //A style layer ties together the source and image and specifies how they are displayed on the map
            //Add markers layer
            loadedMapStyle.addLayer(new SymbolLayer(ROUTES_SPOTS_STYLE_LAYER_ID, SPOTS_SOURCE_ID)
                    .withProperties(
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconImage(step(zoom(), get(PROPERTY_ICONIMAGE),
                                    stop(ic_add_zoom_level, ic_target.getId()))),
                            PropertyFactory.iconSize(step(zoom(), 1,
                                    stop(4, 1.2),
                                    stop(5, 1.4),
                                    stop(6, 1.6),
                                    stop(7, 1.8),
                                    stop(8, 2),
                                    stop(ic_add_zoom_level, 1)))
                    )
                    /* add a filter to show only features with PROPERTY_SHOULDHIDE set to false */
                    .withFilter(eq((get(PROPERTY_SHOULDHIDE)), literal(false))));
        }
    }

    private void setupSubRoutesStyleLayer(@NonNull Style loadedMapStyle) {
        if (loadedMapStyle.getLayer(LINES_STYLE_LAYER_ID) == null) {
            //A style layer ties together the source and image and specifies how they are displayed on the map
            //Add markers layer
            loadedMapStyle.addLayer(new LineLayer(LINES_STYLE_LAYER_ID, SUB_ROUTE_SOURCE_ID)
                    .withProperties(
                            PropertyFactory.lineColor(
                                    match(get("lineColor"), rgb(169, 169, 169),
                                            stop(0, rgb(133, 207, 58)),
                                            stop(1, rgb(255, 187, 51)),
                                            stop(2, rgb(51, 201, 187)),
                                            stop(3, rgb(255, 64, 129)),
                                            stop(4, rgb(160, 130, 255)))),
                            PropertyFactory.lineWidth(2f),
                            PropertyFactory.visibility(Property.VISIBLE),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconImage("{" + PROPERTY_ICONIMAGE + "}")
                    )
                    /* add a filter to show only features with PROPERTY_SHOULDHIDE set to false */
                    .withFilter(eq((get(PROPERTY_SHOULDHIDE)), literal(false))));
        }
    }

    /**
     * Setup a layer with Android SDK call-outs (balloons)
     * <p>
     * tag of the feature is used as key for the iconImage
     * </p>
     */
    private void setupCalloutLayer(@NonNull Style loadedMapStyle) {
        if (loadedMapStyle.getLayer(CALLOUT_LAYER_ID) == null) {
            loadedMapStyle.addLayer(new SymbolLayer(CALLOUT_LAYER_ID, SPOTS_SOURCE_ID)
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

    private void refreshSpotsSource() {
        if (spotSource != null && spotsCollection != null) {
            spotSource.setGeoJson(spotsCollection);
        }
    }

    private void refreshSubRoutesSource() {
        if (subRoutesSource != null && subRoutesCollection != null)
            subRoutesSource.setGeoJson(subRoutesCollection);
    }

    public void saveRegularSpotButtonHandler(Spot mCurrentWaitingSpot) {
        saveSpotButtonHandler(mCurrentWaitingSpot, false);
    }

    public void saveDestinationSpotButtonHandler(Spot mCurrentWaitingSpot) {
        saveSpotButtonHandler(mCurrentWaitingSpot, true);
    }

    /**
     * Handles the Save Spot button and save current location. Does nothing if
     * updates have already been requested.
     */
    public void saveSpotButtonHandler(boolean isDestination) {
        //showProgressDialog(getResources().getString(R.string.map_loading_dialog));
        //viewModel.reloadSpots(requireContext());
        Spot mCurrentWaitingSpot = viewModel.getWaitingSpot().getValue();
        saveSpotButtonHandler(mCurrentWaitingSpot, isDestination);
    }

    /**
     * Handles the Save Spot button and save current location. Does nothing if
     * updates have already been requested.
     */
    public void saveSpotButtonHandler(Spot mCurrentWaitingSpot, boolean isDestination) {
        double cameraZoom = -1;
        boolean isWaitingForARide = (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null) ?
                mCurrentWaitingSpot.getIsWaitingForARide() : false;
        LatLng selectedLocation = null;

        if (!isWaitingForARide) {
            // Keep the same center point of the map.
            // The user will have a locate button to move the camera to his current position if he wants to do that.
            if (mapboxMap != null && mapboxMap.getCameraPosition().target != null) {
                selectedLocation = mapboxMap.getCameraPosition().target;
                cameraZoom = mapboxMap.getCameraPosition().zoom;
            }
            Crashlytics.log(Log.INFO, TAG, "Save spot button handler: a new spot is being created.");
        } else {
            Crashlytics.log(Log.INFO, TAG, "Save spot button handler: a spot is being edited.");
        }

        isHandlingRequestToOpenSpotForm = true;

        ((MainActivity) requireActivity()).saveSpotButtonHandler(selectedLocation, cameraZoom, isDestination);
    }

    public void gotARideButtonHandler(Spot mCurrentWaitingSpot) {
        if (mCurrentWaitingSpot == null) return;
        mCurrentWaitingSpot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        evaluateSpotButtonHandler(mCurrentWaitingSpot);
    }

    public void tookABreakButtonHandler(Spot mCurrentWaitingSpot) {
        if (mCurrentWaitingSpot == null) return;
        mCurrentWaitingSpot.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        evaluateSpotButtonHandler(mCurrentWaitingSpot);
    }

    /**
     * Handles the Got A Ride button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void evaluateSpotButtonHandler(Spot mCurrentWaitingSpot) {
        boolean isWaitingForARide = (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null) ?
                mCurrentWaitingSpot.getIsWaitingForARide() : false;

        if (isWaitingForARide) {
            isHandlingRequestToOpenSpotForm = true;
            SpotFormViewModel spotFormViewModel = new ViewModelProvider(requireActivity()).get(SpotFormViewModel.class);
            spotFormViewModel.setCurrentSpot(mCurrentWaitingSpot, false);
            ((MainActivity) requireActivity()).startSpotFormActivityForResult(null, Constants.KEEP_ZOOM_LEVEL, true);
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

    private ColorStateList getIdentifierColorStateList(Context context, int routeIndex) {
        return ContextCompat.getColorStateList(context, getPolylineColorAsId(routeIndex));
    }

    private static int getPolylineColorAsId(int routeIndex) {
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

    private static int getLineColorId(int routeIndex) {
        int polylineColor = -1;

        if (routeIndex > -1) {
            int value = routeIndex;
            if (value < 5)
                value += 5;

            return value % 5;
        }

        return polylineColor;
    }


}
