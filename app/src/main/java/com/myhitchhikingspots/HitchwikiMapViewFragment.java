package com.myhitchhikingspots;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
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
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.myhitchhikingspots.interfaces.FirstLocationUpdateListener;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.DownloadCountriesListAsyncTask;
import com.myhitchhikingspots.utilities.DownloadCountriesListAsyncTask.onPlacesDownloadedListener;
import com.myhitchhikingspots.utilities.DownloadHWSpotsDialog;
import com.myhitchhikingspots.utilities.DownloadPlacesAsyncTask;
import com.myhitchhikingspots.utilities.IconUtils;
import com.myhitchhikingspots.utilities.LocationUpdatesCallback;
import com.myhitchhikingspots.utilities.PairParcelable;
import com.myhitchhikingspots.utilities.Utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import hitchwikiMapsSDK.classes.APICallCompletionListener;
import hitchwikiMapsSDK.classes.APIConstants;
import hitchwikiMapsSDK.entities.CountryInfoBasic;
import hitchwikiMapsSDK.entities.Error;
import hitchwikiMapsSDK.entities.PlaceInfoBasic;

import static android.os.Looper.getMainLooper;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.step;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

public class HitchwikiMapViewFragment extends Fragment implements OnMapReadyCallback, PermissionsListener,
        MapboxMap.OnMapClickListener, LoadHitchwikiSpotsListTask.onPostExecute, MainActivity.OnMainActivityUpdated, FirstLocationUpdateListener {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private Style style;
    private FloatingActionButton fabLocateUser, fabZoomIn, fabZoomOut;//, fabShowAll;
    CoordinatorLayout coordinatorLayout;
    Boolean mapButtonsAreDisplayed = true;

    /**
     * True if the map camera should follow the GPS updates once the user grants the necessary permission.
     * This flag is necessary because when the app starts up we automatically request permission to access the user location,
     * and in this case we don't want the map camera to follow the GPS updates ({@link #zoomOutToFitAllMarkers()} will be called instead).
     **/
    boolean isLocationRequestedByUser = false;

    static String locationSeparator = ", ";

    private PermissionsManager locationPermissionsManager;

    private ProgressDialog loadingDialog;
    private DownloadHWSpotsDialog dialog;

    Boolean shouldZoomToFitAllMarkers = true;

    protected static final String TAG = "hitchwikimap-view-activity";
    private static final String PROPERTY_ICONIMAGE = "iconImage",
            PROPERTY_ROUTEINDEX = "routeIndex", PROPERTY_TAG = "tag", PROPERTY_SPOTTYPE = "spotType",
            PROPERTY_TITLE = "title", PROPERTY_SNIPPET = "snippet",
            PROPERTY_SHOULDHIDE = "shouldHide", PROPERTY_SELECTED = "selected";

    // Permissions variables
    private static final int PERMISSIONS_LOCATION = 0;
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 1;

    private final int ic_add_zoom_level = 12;

    private static final String MARKER_SOURCE_ID = "markers-source";
    private static final String MARKER_STYLE_LAYER_ID = "markers-style-layer";
    private static final String CALLOUT_LAYER_ID = "mapbox.poi.callout";

    AsyncTask loadTask, downloadPlacesAsyncTask, downloadCountriesListAsyncTask;

    Icon ic_single_spot, ic_took_a_break_spot, ic_waiting_spot, ic_arrival_spot = null;
    Icon ic_hitchability_unknown, ic_hitchability_very_good, ic_hitchability_good, ic_hitchability_average, ic_hitchability_bad, ic_hitchability_senseless, ic_target;

    List<Spot> spotList = new ArrayList();
    //Each hitchhiking spot is a feature
    FeatureCollection featureCollection;
    GeoJsonSource source;
    SymbolLayer markerStyleLayer;

    // Variables needed to add the location engine
    private LocationEngine locationEngine;
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    // Variables needed to listen to location updates
    private LocationUpdatesCallback callback = new LocationUpdatesCallback(this);

    public static CountryInfoBasic[] countriesContainer = new CountryInfoBasic[0];

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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

        prefs = getActivity().getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        fabLocateUser = (FloatingActionButton) view.findViewById(R.id.fab_locate_user);
        fabLocateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null && style != null && style.isFullyLoaded()) {
                    callback.moveMapCameraToNextLocationReceived();
                    isLocationRequestedByUser = true;
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

        hitchwikiStorageFolder = new File(Constants.HITCHWIKI_MAPS_STORAGE_PATH);

        //setupContinentsContainer();

        //Rename old Hitchwiki Maps directory to something more intuitive for the user
        if (prefs.getBoolean(Constants.PREFS_HITCHWIKI_STORAGE_RENAMED, false)) {
            File oldFolder = new File(Constants.HITCHWIKI_MAPS_STORAGE_OLDPATH);
            File newFolder = new File(Constants.HITCHWIKI_MAPS_STORAGE_PATH);
            oldFolder.renameTo(newFolder);
            prefs.edit().putBoolean(Constants.PREFS_HITCHWIKI_STORAGE_RENAMED, true).apply();
        }

        //Let's try to guarantee that we always zoom out to fit all markers, except when user navigates back from spot form.
        shouldZoomToFitAllMarkers = true;
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationLayer(@NonNull Style loadedMapStyle) {
        if (mapboxMap == null)
            return;

        //Setup location plugin to display the user location on a map.
        // NOTE: map camera won't follow location updates by default here.
        setupLocationComponent(loadedMapStyle);

        LocationComponent locationComponent = mapboxMap.getLocationComponent();

        // Enable the location layer on the map
        if (locationComponent.isLocationComponentActivated() && !locationComponent.isLocationComponentEnabled())
            locationComponent.setLocationComponentEnabled(true);
    }

    private void loadMarkerIcons() {
        ic_single_spot = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_route_point_black_24dp, -1);

        ic_took_a_break_spot = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_break_spot_icon, -1);
        ic_waiting_spot = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_marker_waiting_for_a_ride_24dp, -1);
        ic_arrival_spot = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_arrival_icon, -1);

        ic_hitchability_unknown = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(-1));
        ic_hitchability_very_good = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(1));
        ic_hitchability_good = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(2));
        ic_hitchability_average = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(3));
        ic_hitchability_bad = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(4));
        ic_hitchability_senseless = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_route_point_black_24dp, getIdentifierColorStateList(5));

        ic_target = IconUtils.drawableToIcon(getActivity(), R.drawable.ic_add, ContextCompat.getColorStateList(getActivity(), R.color.mapboxGrayExtraDark));
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getActivity(), getString(R.string.spot_form_user_location_permission_not_granted), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        //As we request at least two different permissions (location and storage) for the users,
        //instead of handling the results for location permission here alone, we've opted to handle it within onRequestPermissionsResult.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_LOCATION:
                locationPermissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isLocationRequestedByUser) {
                        enableLocationLayer(style);
                        callback.moveMapCameraToNextLocationReceived();
                        isLocationRequestedByUser = false;
                    }
                } else {
                    Toast.makeText(getActivity(), getString(R.string.spot_form_user_location_permission_not_granted), Toast.LENGTH_LONG).show();
                }

                //Ask for storage permission if necessary, load spots list and finally draw annotations.
                onFirstLoad();
                break;
            case PERMISSIONS_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onFirstLoad();
                }
                break;
        }
    }

    @SuppressWarnings({"MissingPermission"})
    /**
     * Setup location component to display the user location on a map.
     * Map camera won't follow location updates by deafult.
     */
    private void setupLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (areLocationPermissionsGranted(getActivity())) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(getActivity(), loadedMapStyle)
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
            requestLocationsPermissions(getActivity());
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(getActivity());

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

    //onMapReady is called after onResume()
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        Crashlytics.log(Log.INFO, TAG, "mapReady called");
        prefs.edit().putBoolean(Constants.PREFS_MAPBOX_WAS_EVER_LOADED, true).apply();

        this.mapboxMap = mapboxMap;

        this.mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments.
            HitchwikiMapViewFragment.this.style = style;

            this.mapboxMap.getUiSettings().setCompassEnabled(true);
            this.mapboxMap.getUiSettings().setLogoEnabled(false);
            this.mapboxMap.getUiSettings().setAttributionEnabled(false);
            this.mapboxMap.getUiSettings().setTiltGesturesEnabled(false);

            this.mapboxMap.addOnMapClickListener(this);

            if (style.isFullyLoaded()) {
                LocalizationPlugin localizationPlugin = new LocalizationPlugin(mapView, mapboxMap, style);

                try {
                    localizationPlugin.matchMapLanguageWithDeviceDefault();
                } catch (RuntimeException exception) {
                    Crashlytics.logException(exception);
                }

                setupIconImages(style);

                //If location permissions were not granted yet, let's request them.
                // As soon as the user answers the request for location permissions, onFirstLoad() will be called.
                if (!areLocationPermissionsGranted(getActivity()))
                    requestLocationsPermissions(getActivity());
                else {
                    enableLocationLayer(style);

                    //Move map camera to last known location so that if we call zoomOutToFitAllMarkers()
                    // the map will get nicely zoomed closer once spots list is loaded.
                    moveCameraToLastKnownLocation((int) mapboxMap.getMinZoomLevel(), new MapboxMap.CancelableCallback() {
                        @Override
                        public void onCancel() {
                            onFirstLoad();
                        }

                        @Override
                        public void onFinish() {
                            onFirstLoad();
                        }
                    });
                }
            }
        });
    }

    /**
     * Ask for storage permission if necessary, load spots list and finally draw annotations.
     **/
    void onFirstLoad() {
        Activity activity = getActivity();
        if (activity == null)
            return;

        if (!areStoragePermissionsGranted(activity)) {
            requestStoragePermissions(activity);
            return;
        }

        if (wasHWSpotsDownloaded())
            loadHWSpotsFromLocalStorage();
        else
            downloadCountriesList();
    }

    void downloadCountriesList() {
        downloadCountriesListAsyncTask = new DownloadCountriesListAsyncTask(onPlacesDownloadedListener).execute();
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
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
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null)
            return;

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

            activity.startSpotFormActivityForResult(spot, Constants.KEEP_ZOOM_LEVEL, Constants.EDIT_SPOT_REQUEST, true, true);
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
        if (style.getImage(feature.id()) == null) {
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
        if (featureCollection != null && featureCollection.features() != null) {
            for (Feature feature : featureCollection.features()) {
                feature.properties().addProperty(PROPERTY_SELECTED, false);
            }
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

    private void setupIconImages(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage(ic_single_spot.getId(), ic_single_spot.getBitmap());
        //this.mapboxMap.addImage(ic_point_on_the_route_spot.getId(), ic_point_on_the_route_spot.getBitmap());
        loadedMapStyle.addImage(ic_waiting_spot.getId(), ic_waiting_spot.getBitmap());
        loadedMapStyle.addImage(ic_arrival_spot.getId(), ic_arrival_spot.getBitmap());
        loadedMapStyle.addImage(ic_hitchability_unknown.getId(), ic_hitchability_unknown.getBitmap());
        loadedMapStyle.addImage(ic_hitchability_very_good.getId(), ic_hitchability_very_good.getBitmap());
        loadedMapStyle.addImage(ic_hitchability_good.getId(), ic_hitchability_good.getBitmap());
        loadedMapStyle.addImage(ic_hitchability_average.getId(), ic_hitchability_average.getBitmap());
        loadedMapStyle.addImage(ic_hitchability_bad.getId(), ic_hitchability_bad.getBitmap());
        loadedMapStyle.addImage(ic_hitchability_senseless.getId(), ic_hitchability_senseless.getBitmap());
        loadedMapStyle.addImage(ic_target.getId(), ic_target.getBitmap());
    }

    SharedPreferences prefs;

    void loadHWSpotsFromLocalStorage() {
        showProgressDialog(getActivity().getResources().getString(R.string.map_loading_dialog));
        //Load spots and display them as markers and polylines on the map
        this.loadTask = new LoadHitchwikiSpotsListTask(this).execute();
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

        if (mapboxMap != null) {
            updateAnnotations();
        }
    }

    void updateAnnotations() {
        if (mapboxMap != null && style != null && style.isFullyLoaded()) {
            showProgressDialog(getString(R.string.map_drawing_progress_text));
            Spot[] spotArray = new Spot[spotList.size()];
            this.loadTask = new DrawAnnotationsTask(this).execute(spotList.toArray(spotArray));
        }
    }


    private void setupAnnotations(ArrayList<Feature> features, String errMsg) {
        if (!errMsg.isEmpty()) {
            showErrorAlert(getResources().getString(R.string.general_error_dialog_title), errMsg);
        }

        this.featureCollection = FeatureCollection.fromFeatures(features);

        if (style.isFullyLoaded()) {
            setupSource(style);
            setupStyleLayer(style);
            //Setup a layer with Android SDK call-outs (title of the feature is used as key for the iconImage)
            setupCalloutLayer(style);

            try {
                //Automatically zoom out to fit all markers only the first time that spots are loaded.
                // Otherwise it can be annoying to loose your zoom when navigating back after editing a spot. In anyways, there's a button to do this zoom if/when the user wish.
                if (shouldZoomToFitAllMarkers) {
                    if (spotList.size() == 0) {
                        callback.moveMapCameraToNextLocationReceived();
                    } else
                        zoomOutToFitAllMarkers();
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

    File hitchwikiStorageFolder = new File(Constants.HITCHWIKI_MAPS_STORAGE_PATH);

    onPlacesDownloadedListener onPlacesDownloadedListener = new onPlacesDownloadedListener() {
        @Override
        public void onDownloadBegins() {
            showProgressDialog(getString(R.string.hwmaps_downloadCountriesList_button_label));

            //create folder if not already created
            if (!hitchwikiStorageFolder.exists()) {
                //create folder for the first time
                hitchwikiStorageFolder.mkdirs();
                Crashlytics.log(Log.INFO, TAG, "Directory created. " + hitchwikiStorageFolder.getPath());
            }
        }

        @Override
        public void onDownloadedFinished(String result, CountryInfoBasic[] countries) {
            boolean isListJustDownloaded = result.contentEquals("countriesListDownloaded");
            boolean isListLoadedFromLocalStorage = false;

            //If something went wrong while downloading countries list,
            // let's try to use a countries list previously downloaded.
            // (this list might be not so updated, but it's better than nothing!)
            if (!result.isEmpty() && !isListJustDownloaded && wasCountriesListDownloaded()) {
                try {
                    countries = Utils.loadCountriesListFromLocalFile();
                    isListLoadedFromLocalStorage = true;
                } catch (Exception ex) {
                    Crashlytics.logException(ex);
                    countries = new CountryInfoBasic[0];
                    result = ex.getLocalizedMessage();
                }
            }

            if (countries == null || countries.length == 0) {
                showErrorAlert(getString(R.string.general_error_dialog_title), String.format(getString(R.string.hwmaps_countriesList_download_failed_message), result));
                dismissProgressDialog();
                return;
            }

            countriesContainer = countries;

            if (isListJustDownloaded) {
                try {
                    saveCountriesListLocally(countriesContainer);

                    //also write into prefs that markers sync has occurred
                    Long currentMillis = System.currentTimeMillis();
                    prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_COUNTRIES_DOWNLOAD, currentMillis).apply();
                } catch (Exception exception) {
                    Crashlytics.logException(exception);
                }
            }

            if (isListJustDownloaded || isListLoadedFromLocalStorage) {
                showCountriesListDialog();
            }

            dismissProgressDialog();
        }
    };

    void saveCountriesListLocally(CountryInfoBasic[] countriesList) throws Exception {
        File file = new File(hitchwikiStorageFolder, Constants.HITCHWIKI_MAPS_COUNTRIES_LIST_FILE_NAME);

        if (!file.exists())
            file.createNewFile();

        FileOutputStream fileOutput = new FileOutputStream(file);

        Gson gsonC = new Gson();
        String placesContainerAsString = gsonC.toJson(countriesList);

        InputStream inputStream = new ByteArrayInputStream(placesContainerAsString.getBytes("UTF-8"));

        //create a buffer...
        byte[] buffer = new byte[1024];
        int bufferLength = 0; //used to store a temporary size of the buffer

        while ((bufferLength = inputStream.read(buffer)) > 0) {
            //add the data in the buffer to the file in the file output stream (the file on the sd card
            fileOutput.write(buffer, 0, bufferLength);
        }

        //close the output stream when done
        fileOutput.close();
    }

    void showCountriesListDialog() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing())
            return;

        PairParcelable[] lst = new PairParcelable[countriesContainer.length];
        for (int i = 0; i < countriesContainer.length; i++) {
            CountryInfoBasic country = countriesContainer[i];

            //Build string to show
            String c2 = country.getName();
            if (country.getPlaces() != null && !country.getPlaces().isEmpty())
                c2 += " (" + country.getPlaces() + " " + activity.getString(R.string.main_activity_single_spots_list_tab) + ")";

            PairParcelable item = new PairParcelable(country.getIso(), c2);
            lst[i] = item;
        }

        showSelectionDialog(lst, DownloadHWSpotsDialog.DIALOG_TYPE_COUNTRY);
    }

    private boolean wasCountriesListDownloaded() {
        Long millisecondsLastCountriesRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_COUNTRIES_DOWNLOAD, 0);
        //If the countries list were previously downloaded (we know that by checking if there's a date set from countries download)
        return (millisecondsLastCountriesRefresh > 0);
    }

    private boolean wasHWSpotsDownloaded() {
        int millisecondsAtRefresh = prefs.getInt(Constants.PREFS_NUM_OF_HW_SPOTS_DOWNLOADED, 0);
        return (millisecondsAtRefresh > 0);
    }

    private static boolean areLocationPermissionsGranted(Activity activity) {
        return PermissionsManager.areLocationPermissionsGranted(activity);
    }

    private void requestLocationsPermissions(Activity activity) {
        if (locationPermissionsManager == null)
            locationPermissionsManager = new PermissionsManager(this);
        locationPermissionsManager.requestLocationPermissions(activity);
    }

    private static boolean areStoragePermissionsGranted(Activity activity) {
        // Check if we have read and write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        // Check if user has granted location permission
        return (writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED);
    }

    private static void requestStoragePermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSIONS_EXTERNAL_STORAGE
        );
    }

    DownloadHWSpotsDialog.DownloadHWSpotsDialogListener downloadHWSpotsDialogListener = new DownloadHWSpotsDialog.DownloadHWSpotsDialogListener() {
        @Override
        public void onDownloadConfirmClicked(String selectedCodes, String dialog_type) {
            downloadHWSpots(selectedCodes, dialog_type);
        }

        @Override
        public String getContinentsContainer(int item) {
            return null;
        }

        @Override
        public String getCountryContainer(int item) {
            return countriesContainer[item].getIso();
        }
    };


    private void downloadHWSpots(String selectedCodes, String dialogType) {
        String places = "";
        switch (dialogType) {
            case DownloadHWSpotsDialog.DIALOG_TYPE_CONTINENT:
                prefs.edit().putString(Constants.PREFS_SELECTED_CONTINENTS_TO_DOWNLOAD, selectedCodes).apply();
                prefs.edit().remove(Constants.PREFS_SELECTED_COUNTRIES_TO_DOWNLOAD).apply();
                places = "Selected continents: " + prefs.getString(Constants.PREFS_SELECTED_CONTINENTS_TO_DOWNLOAD, "");
                break;
            case DownloadHWSpotsDialog.DIALOG_TYPE_COUNTRY:
                prefs.edit().putString(Constants.PREFS_SELECTED_COUNTRIES_TO_DOWNLOAD, selectedCodes).apply();
                prefs.edit().remove(Constants.PREFS_SELECTED_CONTINENTS_TO_DOWNLOAD).apply();
                places = "Selected countries: " + prefs.getString(Constants.PREFS_SELECTED_COUNTRIES_TO_DOWNLOAD, "");
                break;
        }

        if (!Utils.isNetworkAvailable(getActivity())) {
            showErrorAlert(getString(R.string.general_offline_mode_label), getString(R.string.general_network_unavailable_message));
        } else {
            String lstToDownload = "";
            switch (dialogType) {
                case DownloadHWSpotsDialog.DIALOG_TYPE_CONTINENT:
                    lstToDownload = prefs.getString(Constants.PREFS_SELECTED_CONTINENTS_TO_DOWNLOAD, "");
                    break;
                case DownloadHWSpotsDialog.DIALOG_TYPE_COUNTRY:
                    lstToDownload = prefs.getString(Constants.PREFS_SELECTED_COUNTRIES_TO_DOWNLOAD, "");
                    break;
            }

            showProgressDialog(getString(R.string.hwmaps_downloadHDSpots_button_label), String.format(getString(R.string.general_downloading_something_message), lstToDownload));

            //create folder if not already created
            if (!hitchwikiStorageFolder.exists()) {
                //create folder for the first time
                hitchwikiStorageFolder.mkdirs();
                Crashlytics.log(Log.INFO, TAG, "Directory created. " + hitchwikiStorageFolder.getPath());
            }

            //recreate placesContainer, it might not be empty
            if (placesContainer == null)
                placesContainer = new ArrayList<>();
            else
                placesContainer.clear();

            //Clear countries container
            countriesContainer = new CountryInfoBasic[0];

            downloadPlacesAsyncTask = new DownloadPlacesAsyncTask(hitchwikiStorageFolder, dialogType, lstToDownload, getPlacesByArea, this::onDownloadedFinished).execute();
        }
    }

    private void onDownloadedFinished(String result) {
        String errMsg = "";
        try {
            if (result.contentEquals("spotsDownloaded")) {
                savePlacesListLocally(placesContainer);

                //Get current datetime in milliseconds
                Long millisecondsAtRefresh = System.currentTimeMillis();

                //also write into prefs that markers sync has occurred
                prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD, millisecondsAtRefresh).apply();
                prefs.edit().putBoolean(Constants.PREFS_HWSPOTLIST_WAS_CHANGED, true).apply();
            } else if (result.contentEquals("nothingToSync")) {
                //also write into prefs that markers sync has occurred
                prefs.edit().remove(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD).apply();
                prefs.edit().putBoolean(Constants.PREFS_HWSPOTLIST_WAS_CHANGED, true).apply();

                savePlacesListLocally(placesContainer);
            } else if (!result.isEmpty())
                errMsg = result;
        } catch (Exception ex) {
            Crashlytics.logException(ex);
            errMsg = ex.getLocalizedMessage();
        }

        Activity activity = getActivity();
        if (activity != null) {
            if (!errMsg.isEmpty())
                showErrorAlert(getString(R.string.general_error_dialog_title), String.format(getString(R.string.hwmaps_hitchwikiMapsSpots_download_failed_message), errMsg));
            else if (result.contentEquals("nothingToSync"))
                showErrorAlert(getString(R.string.hwmaps_spotslist_cleared_title), getString(R.string.hwmaps_spotslist_cleared_message));
            else if (result.contentEquals("spotsDownloaded"))
                Toast.makeText(activity.getBaseContext(), getString(R.string.general_download_finished_successffull_message), Toast.LENGTH_LONG).show();
        }

        dismissProgressDialog();

        shouldZoomToFitAllMarkers = true;
        loadHWSpotsFromLocalStorage();
    }

    private void showSelectionDialog(PairParcelable[] result, String dialogType) {
        FragmentActivity activity = getActivity();
        if (activity == null)
            return;

        Bundle args = new Bundle();
        args.putParcelableArray(Constants.DIALOG_STRINGLIST_BUNDLE_KEY, result);
        args.putString(Constants.DIALOG_TYPE_BUNDLE_KEY, dialogType);

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        dialog = new DownloadHWSpotsDialog(downloadHWSpotsDialogListener);
        //dialog.setTargetFragment(this, 0);
        dialog.setArguments(args);
        dialog.show(fragmentManager, "tagSelection");
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
    public void onSpotListChanged() {
        //spotList here is the one used by My Maps. Nothing needs to be done here.
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        if (dialog != null)
            dialog.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        if (dialog != null) {
            dialog.dismiss();
            dialog.onStop();
        }

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
        if (downloadPlacesAsyncTask != null)
            downloadPlacesAsyncTask.cancel(false);
        if (downloadCountriesListAsyncTask != null)
            downloadCountriesListAsyncTask.cancel(false);
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

        if (prefs.getBoolean(Constants.PREFS_HWSPOTLIST_WAS_CHANGED, false)) {
            prefs.edit().putBoolean(Constants.PREFS_HWSPOTLIST_WAS_CHANGED, false).apply();
            shouldZoomToFitAllMarkers = false;
            onFirstLoad();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (dialog != null) {
            dialog.dismiss();
            dialog.onPause();
        }
        dismissProgressDialog();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        mapView.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapboxMap != null) {
            mapboxMap.removeOnMapClickListener(this);
        }
        mapView.onDestroy();
        if (dialog != null)
            dialog.onDestroy();
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

    private void setupContinentsContainer() {
        continentsContainer = new PairParcelable[7];
        continentsContainer[0] = new PairParcelable(APIConstants.CODE_CONTINENT_ANTARTICA, getString(R.string.continent_name_antarctica));
        continentsContainer[1] = new PairParcelable(APIConstants.CODE_CONTINENT_AFRICA, getString(R.string.continent_name_africa));
        continentsContainer[2] = new PairParcelable(APIConstants.CODE_CONTINENT_ASIA, getString(R.string.continent_name_asia));
        continentsContainer[3] = new PairParcelable(APIConstants.CODE_CONTINENT_EUROPE, getString(R.string.continent_name_europe));
        continentsContainer[4] = new PairParcelable(APIConstants.CODE_CONTINENT_NORTH_AMERICA, getString(R.string.continent_name_north_america));
        continentsContainer[5] = new PairParcelable(APIConstants.CODE_CONTINENT_SOUTH_AMERICA, getString(R.string.continent_name_south_america));
        continentsContainer[6] = new PairParcelable(APIConstants.CODE_CONTINENT_AUSTRALIA, getString(R.string.continent_name_oceania));
    }

    public static List<PlaceInfoBasic> placesContainer = new ArrayList<>();
    public static PairParcelable[] continentsContainer = new PairParcelable[0];
    public boolean placesContainerIsEmpty = true;

    APICallCompletionListener<PlaceInfoBasic[]> getPlacesByArea = new APICallCompletionListener<PlaceInfoBasic[]>() {
        @Override
        public void onComplete(boolean success, int intParam, String stringParam, Error error, PlaceInfoBasic[] object) {
            if (success) {
                for (int i = 0; i < object.length; i++) {
                    placesContainer.add(object[i]);
                }

            } else {
                System.out.println("Error message : " + error.getErrorDescription());
            }
        }
    };

    void savePlacesListLocally(List<PlaceInfoBasic> places) throws Exception {
        //in this case, we have full placesContainer, processed to fulfill Clusterkraf model requirements and all,
        //so we have to create file in storage folder and stream placesContainer into it using gson
        File fileToStoreMarkersInto = new File(hitchwikiStorageFolder, Constants.HITCHWIKI_MAPS_MARKERS_LIST_FILE_NAME);

        if (!fileToStoreMarkersInto.exists())
            fileToStoreMarkersInto.createNewFile();

        FileOutputStream fileOutput = new FileOutputStream(fileToStoreMarkersInto);

        Gson gsonC = new Gson();
        String placesContainerAsString = gsonC.toJson(places);

        InputStream inputStream = new ByteArrayInputStream(placesContainerAsString.getBytes("UTF-8"));

        //create a buffer...
        byte[] buffer = new byte[1024];
        int bufferLength = 0; //used to store a temporary size of the buffer

        while ((bufferLength = inputStream.read(buffer)) > 0) {
            //add the data in the buffer to the file in the file output stream (the file on the sd card
            fileOutput.write(buffer, 0, bufferLength);
        }

        //close the output stream when done
        fileOutput.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download_hw_spots:
                if (countriesContainer != null && countriesContainer.length > 0)
                    showCountriesListDialog();
                else if (!areStoragePermissionsGranted(getActivity()))
                    requestStoragePermissions(getActivity());
                else
                    downloadCountriesList();
                break;
            case R.id.action_toggle_icons:
                toggleAllFAB();
                break;
            case R.id.action_zoom_to_fit_all:
                if (mapboxMap != null) {
                    zoomOutToFitAllMarkers();
                }
                break;
        }

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

        mapButtonsAreDisplayed = true;
    }

    /**
     * Hides all Floating Action Buttons (FAB) from the view.
     **/
    private void hideAllFAB() {
        fabLocateUser.hide();
        fabZoomIn.hide();
        fabZoomOut.hide();

        mapButtonsAreDisplayed = false;
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

    /**
     * Zoom out to fit all markers AND the user's last known location.
     **/
    @SuppressWarnings({"MissingPermission"})
    protected void zoomOutToFitAllMarkers() {
        try {
            if (mapboxMap != null) {
                Location mCurrentLocation = tryGetLastKnownLocation();
                List<LatLng> lst = new ArrayList<>();
                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                //Include only features that are actually seen on the map
                if (featureCollection != null) {
                    for (Feature feature : featureCollection.features()) {
                        Point p = ((Point) feature.geometry());
                        lst.add(new LatLng(p.latitude(), p.longitude()));
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

        } catch (
                Exception ex) {
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
    public void moveCameraToLastKnownLocation(int zoomLevel, @Nullable MapboxMap.CancelableCallback callback) {
        if (!style.isFullyLoaded())
            return;

        //Request permission of access to GPS updates or
        // directly initialize and enable the location plugin if such permission was already granted.
        enableLocationLayer(style);

        LatLng moveCameraPositionTo = null;

        //If we know the current position of the user, move the map camera to there
        Location lastLoc = tryGetLastKnownLocation();
        if (lastLoc != null)
            moveCameraPositionTo = new LatLng(lastLoc);

        if (moveCameraPositionTo != null) {
            moveCameraPositionTo = new LatLng(moveCameraPositionTo);
        } else {
            //The user might still be close to the last spot saved, move the map camera there
            Spot lastAddedSpot = ((MyHitchhikingSpotsApplication) getActivity().getApplicationContext()).getLastAddedRouteSpot();
            if (lastAddedSpot != null && lastAddedSpot.getLatitude() != null && lastAddedSpot.getLongitude() != null
                    && lastAddedSpot.getLatitude() != 0.0 && lastAddedSpot.getLongitude() != 0.0) {
                moveCameraPositionTo = new LatLng(lastAddedSpot.getLatitude(), lastAddedSpot.getLongitude());
            }
        }

        int bestZoomLevel = Constants.KEEP_ZOOM_LEVEL;

        //If a zoomLevel has been informed, use it
        if (zoomLevel != FAVORITE_ZOOM_LEVEL_NOT_INFORMED)
            bestZoomLevel = zoomLevel;
        else {
            //If current zoom level is default (world level)
            if (mapboxMap.getCameraPosition().zoom == mapboxMap.getMinZoomLevel())
                bestZoomLevel = Constants.ZOOM_TO_SEE_FARTHER_DISTANCE;
        }

        if (moveCameraPositionTo != null)
            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(moveCameraPositionTo, bestZoomLevel), callback);

    }

    private void showProgressDialog(String message) {
        showProgressDialog(message, null);
    }

    private void showProgressDialog(String message, String title) {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(getActivity());
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
        }
        if (title == null)
            title = "";
        loadingDialog.setTitle(title);
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
    private static class DrawAnnotationsTask extends AsyncTask<Spot, Void, ArrayList<Feature>> {
        private final WeakReference<HitchwikiMapViewFragment> activityRef;
        String errMsg = "";

        DrawAnnotationsTask(HitchwikiMapViewFragment activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        protected ArrayList<Feature> doInBackground(Spot... spotList) {
            ArrayList<Feature> features = new ArrayList<>();
            if (isCancelled())
                return features;

            for (int j = 0; j < spotList.length; j++) {
                Spot s = spotList[j];
                features.add(GetFeature(s, 0, activityRef));
            }
            return features;
        }

        private static String getIconId(HitchwikiMapViewFragment activity, int hitchability) {
            Icon ic = activity.ic_hitchability_unknown;
            switch (hitchability) {
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
            return ic.getId();
        }

        @Override
        protected void onPostExecute(ArrayList<Feature> features) {
            super.onPostExecute(features);
            HitchwikiMapViewFragment activity = activityRef.get();
            if (activity == null || isCancelled())
                return;

            activity.setupAnnotations(features, errMsg);
        }

        static Feature GetFeature(Spot spot, int routeIndex, WeakReference<HitchwikiMapViewFragment> activityRef) {
            HitchwikiMapViewFragment activity = activityRef.get();
            if (activity == null)
                return null;

            LatLng pos = new LatLng(spot.getLatitude(), spot.getLongitude());

            String tag = spot.getId() != null ? spot.getId().toString() : "";
            String icon = getIconId(activity, (spot.getHitchability() != null) ? spot.getHitchability() : -1);
            int type = Constants.SPOT_TYPE_HITCHHIKING_SPOT;

            //Get a hitchability string to set as title
            String title = "";
            if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot())
                title = Utils.getRatingOrDefaultAsString(activity.getContext(), spot.getHitchability() != null ? spot.getHitchability() : 0);

            //Set hitchability as title
            title = title.toUpperCase();

            // Customize map with markers, polylines, etc.
            String snippet = "(" + spot.getLatitude() + "," + spot.getLongitude() + ")";// getSnippet(getActivity(), spot, "\n ", " ", "\n ");


            JsonObject properties = new JsonObject();
            properties.addProperty(PROPERTY_ROUTEINDEX, routeIndex);
            properties.addProperty(PROPERTY_SHOULDHIDE, false);
            properties.addProperty(PROPERTY_ICONIMAGE, icon);
            properties.addProperty(PROPERTY_TAG, tag);
            properties.addProperty(PROPERTY_SPOTTYPE, type);
            properties.addProperty(PROPERTY_TITLE, title);
            properties.addProperty(PROPERTY_SNIPPET, snippet);

            return Feature.fromGeometry(Point.fromLngLat(pos.getLongitude(), pos.getLatitude()), properties, tag);
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
            if (isCancelled())
                return null;

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
            if (isCancelled())
                return;

            HitchwikiMapViewFragment activity = activityRef.get();
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
    }

    private void showErrorAlert(String title, String msg) {
        Activity activity = getActivity();
        if (activity == null)
            return;
        new AlertDialog.Builder(activity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.general_ok_option), null)
                .show();
    }

    private void setupSource(@NonNull Style loadedMapStyle) {
        if (loadedMapStyle.getSource(MARKER_SOURCE_ID) == null) {
            source = new GeoJsonSource(MARKER_SOURCE_ID, featureCollection);
            loadedMapStyle.addSource(source);
        } else
            refreshSource();
    }

    /* Setup style layer */
    private void setupStyleLayer(@NonNull Style loadedMapStyle) {
        if (loadedMapStyle.getLayer(MARKER_STYLE_LAYER_ID) == null) {
            //A style layer ties together the source and image and specifies how they are displayed on the map
            markerStyleLayer = new SymbolLayer(MARKER_STYLE_LAYER_ID, MARKER_SOURCE_ID)
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
                                    stop(ic_add_zoom_level, 1))))
                    /* add a filter to show only features with PROPERTY_SHOULDHIDE set to false */
                    .withFilter(eq((get(PROPERTY_SHOULDHIDE)), literal(false)));

            //Add markers layer
            loadedMapStyle.addLayer(markerStyleLayer);
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
            loadedMapStyle.addLayer(new SymbolLayer(CALLOUT_LAYER_ID, MARKER_SOURCE_ID)
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
