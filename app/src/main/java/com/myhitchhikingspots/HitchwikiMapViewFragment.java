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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.gson.JsonObject;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
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
import com.myhitchhikingspots.model.Route;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.ExtendedMarkerView;
import com.myhitchhikingspots.utilities.ExtendedMarkerViewOptions;
import com.myhitchhikingspots.utilities.IconUtils;
import com.myhitchhikingspots.utilities.Utils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import hitchwikiMapsSDK.entities.PlaceInfoBasic;

import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

public class HitchwikiMapViewFragment extends Fragment implements OnMapReadyCallback, PermissionsListener,
        MapboxMap.OnMapClickListener, LoadHitchwikiSpotsListTask.onPostExecute {
    private MapView mapView;
    private MapboxMap mapboxMap;
    //private LocationEngine locationEngine;
    //private LocationEngineListener locationEngineListener;
    private FloatingActionButton fabLocateUser, fabZoomIn, fabZoomOut;//, fabShowAll;
    //private TextView mWaitingToGetCurrentLocationTextView;
    CoordinatorLayout coordinatorLayout;
    Boolean shouldDisplayIcons = true;

    boolean wasSnackbarShown;

    static String locationSeparator = ", ";

    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationLayerPlugin;

    protected static final String SNACKBAR_SHOWED_KEY = "snackbar-showed";

    Boolean shouldZoomToFitAllMarkers = true;

    protected static final String TAG = "hitchwikimap-view-activity";
    private static final String PROPERTY_ICONIMAGE = "iconImage",
            PROPERTY_ROUTEINDEX = "routeIndex", PROPERTY_TAG = "tag", PROPERTY_SPOTTYPE = "spotType",
            PROPERTY_TITLE = "title", PROPERTY_SNIPPET = "snippet",
            PROPERTY_SHOULDHIDE = "shouldHide", PROPERTY_SELECTED = "selected";

    // Permissions variables
    private static final int PERMISSIONS_LOCATION = 0;
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 1;
    Toast waiting_GPS_update;

    private static final String MARKER_SOURCE_ID = "markers-source";
    private static final String MARKER_STYLE_LAYER_ID = "markers-style-layer";
    private static final String CALLOUT_LAYER_ID = "mapbox.poi.callout";

    AsyncTask loadTask;

    /**
     * Represents a geographical location.
     */
    boolean wasFirstLocationReceived = false;

    Icon ic_single_spot, ic_took_a_break_spot, ic_waiting_spot, ic_arrival_spot = null;
    Icon ic_hitchability_unknown, ic_hitchability_very_good, ic_hitchability_good, ic_hitchability_average, ic_hitchability_bad, ic_hitchability_senseless;

    List<Spot> spotList = new ArrayList();
    //Each hitchhiking spot is a feature
    FeatureCollection featureCollection;
    //Each route is an item of featuresArray and polylineOptionsArray
    Feature[] featuresArray;
    GeoJsonSource source;
    SymbolLayer markerStyleLayer;

    Snackbar snackbar;

    MainActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hitchwiki_maps, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Set CompatVectorFromResourcesEnabled to true in order to be able to use ContextCompat.getDrawable
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        //mWaitingToGetCurrentLocationTextView = (TextView) findViewById(R.id.waiting_location_textview);
        coordinatorLayout = view.findViewById(R.id.coordinatiorLayout);

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

        mapView = (MapView) view.findViewById(R.id.mapview2);
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

        Bundle bundle = null;
        if (savedInstanceState != null)
            bundle = savedInstanceState;
        else if (getArguments() != null)
            savedInstanceState = getArguments();

        if (savedInstanceState.containsKey(MainActivity.ARG_SPOTLIST_KEY)) {
            Spot[] bundleSpotList = (Spot[]) savedInstanceState.getSerializable(MainActivity.ARG_SPOTLIST_KEY);
            setupData(Arrays.asList(bundleSpotList), "");
        }
    }

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
        if (text.length() > 0)
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

        ic_took_a_break_spot = IconUtils.drawableToIcon(activity, R.drawable.ic_break_spot_icon, -1);
        ic_waiting_spot = IconUtils.drawableToIcon(activity, R.drawable.ic_marker_waiting_for_a_ride_24dp, -1);
        ic_arrival_spot = IconUtils.drawableToIcon(activity, R.drawable.ic_arrival_icon, -1);

        ic_hitchability_unknown = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(-1));
        ic_hitchability_very_good = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(1));
        ic_hitchability_good = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(2));
        ic_hitchability_average = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(3));
        ic_hitchability_bad = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(4));
        ic_hitchability_senseless = IconUtils.drawableToIcon(activity, R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(5));
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
        Toast.makeText(activity, "R.string.user_location_permission_not_granted", Toast.LENGTH_LONG).show();
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
            if (PermissionsManager.areLocationPermissionsGranted(activity)) {

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
        Crashlytics.log(Log.INFO, TAG, "mapReady called");
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

        moveCameraToLastKnownLocation();

        locateUser();

        if (spotList == null || spotList.size() == 0)
            loadHWSpotsIfTheyveBeenDownloaded();
        else
            drawAnnotations();
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
            //Create a record to track of HW spots viewed by the user
            Answers.getInstance().logCustom(new CustomEvent("HW spot viewed"));

            Intent intent = new Intent(activity, SpotFormActivity.class);
            //Maybe we should send mCurrentWaitingSpot on the intent.putExtra so that we don't need to call spot.setAttemptResult(null) ?
            intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
            intent.putExtra(Constants.SHOULD_RETRIEVE_HITCHWIKI_DETAILS_KEY, true);

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
            new GenerateBalloonsTask(this, feature).execute();
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
        //this.mapboxMap.addImage(ic_point_on_the_route_spot.getId(), ic_point_on_the_route_spot.getBitmap());
        this.mapboxMap.addImage(ic_waiting_spot.getId(), ic_waiting_spot.getBitmap());
        this.mapboxMap.addImage(ic_arrival_spot.getId(), ic_arrival_spot.getBitmap());
        this.mapboxMap.addImage(ic_hitchability_unknown.getId(), ic_hitchability_unknown.getBitmap());
        this.mapboxMap.addImage(ic_hitchability_very_good.getId(), ic_hitchability_very_good.getBitmap());
        this.mapboxMap.addImage(ic_hitchability_good.getId(), ic_hitchability_good.getBitmap());
        this.mapboxMap.addImage(ic_hitchability_average.getId(), ic_hitchability_average.getBitmap());
        this.mapboxMap.addImage(ic_hitchability_bad.getId(), ic_hitchability_bad.getBitmap());
        this.mapboxMap.addImage(ic_hitchability_senseless.getId(), ic_hitchability_senseless.getBitmap());
    }

    SharedPreferences prefs;

    void loadHWSpotsIfTheyveBeenDownloaded() {
        Crashlytics.log(Log.INFO, TAG, "loadHWSpotsIfTheyveBeenDownloaded was called");

        Long millisecondsAtRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD, 0);
        if (millisecondsAtRefresh > 0) {
            //Check if we still have read permission so that we can read the file containing the downloaded HW spots
            if (!isReadStoragePermissionGranted(activity))
                requestReadStoragePermission(activity);
            else {
                showProgressDialog(activity.getResources().getString(R.string.map_loading_dialog));
                //Load spots and display them as markers and polylines on the map
                this.loadTask = new LoadHitchwikiSpotsListTask(this).execute();
            }
        } else {
            showDialogDownloadHWSpots();
        }
    }

    @Override
    public void setupData(List<Spot> spotList, String errMsg) {
        if (!errMsg.isEmpty()) {
            showErrorAlert(getResources().getString(R.string.general_error_dialog_title), errMsg);
            return;
        }

        this.spotList = spotList;

        //Update number of HW spots
        prefs.edit().putInt(Constants.PREFS_NUM_OF_HW_SPOTS_DOWNLOADED, spotList.size()).apply();

        dismissProgressDialog();

        if (mapboxMap != null)
            drawAnnotations();
    }

    void drawAnnotations() {
        if (mapboxMap != null) {
            showProgressDialog("Drawing hitchwiki spots..");
            Spot[] spotArray = new Spot[spotList.size()];
            this.loadTask = new DrawAnnotationsTask(this).execute(spotList.toArray(spotArray));
        }
    }


    private void setupAnnotations(List<Route> routes, String errMsg) {
        List<Feature> allFeatures = new ArrayList<>();

        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            allFeatures.addAll(Arrays.asList(route.features));
        }

        Feature[] array = new Feature[allFeatures.size()];
        this.featuresArray = allFeatures.toArray(array);

        if (mapboxMap == null) {
            return;
        }

        mapboxMap.clear();

        featureCollection = FeatureCollection.fromFeatures(featuresArray);

        setupSource();
        setupStyleLayer();
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
        } else {
            //If there's no spot to show, display dialog trying to encourage the user to go and download some HW spots
            //This should be particularly useful when user had downloaded HW spots but the local file was manually deleted or got corrupted for some reason
            if (routes.size() == 0)
                showDialogDownloadHWSpots();
        }

        dismissProgressDialog();
    }

    private void showDialogDownloadHWSpots() {
        new AlertDialog.Builder(activity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.menu_hitchwiki_maps))
                .setMessage(String.format(getString(R.string.empty_list_dialog_message), getString(R.string.tools_title)))
                .setPositiveButton(getString(R.string.tools_title), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.selectDrawerItem(R.id.nav_tools);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.general_cancel_option), null).show();
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


    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        if (locationLayerPlugin != null)
            locationLayerPlugin.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        if (locationLayerPlugin != null)
            locationLayerPlugin.onStop();

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
        super.onResume();
        Crashlytics.log(Log.INFO, TAG, "onResume called");
        mapView.onResume();
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
        super.onPause();
        mapView.onPause();

        dismissSnackbar();
        dismissProgressDialog();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        mapView.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean(SNACKBAR_SHOWED_KEY, wasSnackbarShown);
        Spot[] spotArray = new Spot[spotList.size()];
        savedInstanceState.putSerializable(MainActivity.ARG_SPOTLIST_KEY, spotList.toArray(spotArray));
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
        super.onDestroy();
        if (mapboxMap != null) {
            mapboxMap.removeOnMapClickListener(this);
        }
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.hitchwiki_map_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Important: setHasOptionsMenu must be called so that onOptionsItemSelected works
        setHasOptionsMenu(true);
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

                //Include only features that are actually seen on the map
                for (Feature feature : featureCollection.features()) {
                    Point p = ((Point) feature.geometry());
                    lst.add(new LatLng(p.latitude(), p.longitude()));
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
            if (locationLayerPlugin != null) {
                Location lastLoc = locationLayerPlugin.getLastKnownLocation();
                if (lastLoc != null)
                    moveCameraPositionTo = new LatLng(lastLoc);
            }
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

    private ProgressDialog loadingDialog;

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

    /*
       Draws spots downloaded from Hitchwiki on the map
    */
    private static class DrawAnnotationsTask extends AsyncTask<Spot, Void, List<Route>> {
        private final WeakReference<HitchwikiMapViewFragment> activityRef;
        String errMsg = "";

        DrawAnnotationsTask(HitchwikiMapViewFragment activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected List<Route> doInBackground(Spot... spotList) {
            HitchwikiMapViewFragment activity = activityRef.get();
            if (activity == null)
                return null;

            List<List<ExtendedMarkerViewOptions>> trips = new ArrayList<>();
            ArrayList<ExtendedMarkerViewOptions> spots = new ArrayList<>();
            ArrayList<ExtendedMarkerViewOptions> singleSpots = new ArrayList<>();
            Boolean isLastArrayForSingleSpots = false;

            //The spots are ordered from the last saved ones to the first saved ones, so we need to
            // go through the list in the oposite direction in order to sum up the route's totals from their origin to their destinations
            for (int i = spotList.length - 1; i >= 0; i--) {
                Spot spot = spotList[i];

                ExtendedMarkerViewOptions markerViewOptions = new ExtendedMarkerViewOptions()
                        .position(new LatLng(spot.getLatitude(), spot.getLongitude()));

                if (spot.getId() != null)
                    markerViewOptions.tag(spot.getId().toString());

                Icon ic = activity.ic_hitchability_unknown;
                if (spot.getHitchability() != null) {
                    switch (spot.getHitchability()) {
                        case 1:
                            ic = activity.ic_hitchability_senseless;
                            break;
                        case 2:
                            ic = activity.ic_hitchability_bad;
                            break;
                        case 3:
                            ic = activity.ic_hitchability_average;
                            break;
                        case 4:
                            ic = activity.ic_hitchability_good;
                            break;
                        case 5:
                            ic = activity.ic_hitchability_very_good;
                            break;
                    }
                }

                markerViewOptions.icon(ic);
                markerViewOptions.spotType(Constants.SPOT_TYPE_HITCHHIKING_SPOT);

                //Get a hitchability string to set as title
                String title = "";
                if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot())
                    title = Utils.getRatingOrDefaultAsString(activity.getContext(), spot.getHitchability() != null ? spot.getHitchability() : 0);

                //Set hitchability as title
                markerViewOptions.title(title.toUpperCase());

                // Customize map with markers, polylines, etc.
                String snippet = "(" + spot.getLatitude() + "," + spot.getLongitude() + ")";// getSnippet(activity, spot, "\n ", " ", "\n ");
                markerViewOptions.snippet(snippet);

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

            if (!errMsg.isEmpty()) {
                activity.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.showErrorAlert(activity.getString(R.string.general_error_dialog_title), String.format(activity.getResources().getString(R.string.general_error_dialog_message),
                                errMsg));
                    }
                });
                return new ArrayList<>();
            }

            List<Route> routes = new ArrayList<Route>();

            for (int lc = 0; lc < trips.size(); lc++) {
                try {
                    List<ExtendedMarkerViewOptions> spots2 = trips.get(lc);
                    Route route = new Route();
                    route.features = new Feature[spots2.size()];

                    //If it's the last array and isLastArrayForSingleSpots is true, add the markers with no polyline connecting them
                    if (isLastArrayForSingleSpots && lc == trips.size() - 1) {
                        for (int li = 0; li < spots2.size(); li++) {
                            ExtendedMarkerViewOptions spot = spots2.get(li);
                            //Add marker to map
                            route.features[li] = GetFeature(spot, lc);
                        }
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
        private String getSnippet(HitchwikiMapViewFragment activity, Spot spot, String firstSeparator, String secondSeparator, String thirdSeparator) {
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
        protected void onPostExecute(List<Route> routes) {
            super.onPostExecute(routes);
            HitchwikiMapViewFragment activity = activityRef.get();
            if (activity == null)
                return;

            activity.setupAnnotations(routes, errMsg);
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

        //private final HashMap<String, View> viewMap = new HashMap<>();
        private final WeakReference<HitchwikiMapViewFragment> activityRef;
        private final Feature feature;

        GenerateBalloonsTask(HitchwikiMapViewFragment activity, Feature feature) {
            this.activityRef = new WeakReference<>(activity);
            this.feature = feature;
        }

        @SuppressWarnings("WrongThread")
        @Override
        protected HashMap<String, Bitmap> doInBackground(Void... params) {
            HitchwikiMapViewFragment activity = activityRef.get();
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
                Bitmap bitmap = HitchwikiMapViewFragment.SymbolGenerator.generate(view);
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
            HitchwikiMapViewFragment activity = activityRef.get();
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

    private int getHitchabilityColor(int hitchability) {
        int hitchabilityColor = R.color.hitchability_unknown;

        switch (hitchability) {
            case 1:
                hitchabilityColor = R.color.hitchability_senseless;
                break;
            case 2:
                hitchabilityColor = R.color.hitchability_bad;
                break;
            case 3:
                hitchabilityColor = R.color.hitchability_average;
                break;
            case 4:
                hitchabilityColor = R.color.hitchability_good;
                break;
            case 5:
                hitchabilityColor = R.color.hitchability_very_good;
                break;
        }

        return hitchabilityColor;
    }

    private ColorStateList getIdentifierColorStateList(int hitchability) {
        return ContextCompat.getColorStateList(getContext(), getHitchabilityColor(hitchability));
    }

}
