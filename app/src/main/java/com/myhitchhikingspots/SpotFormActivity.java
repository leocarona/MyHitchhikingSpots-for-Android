package com.myhitchhikingspots;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Address;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


import com.crashlytics.android.Crashlytics;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.content.Intent;
import android.os.Handler;
import android.os.ResultReceiver;

public class SpotFormActivity extends BaseActivity implements RatingBar.OnRatingBarChangeListener, OnMapReadyCallback, View.OnClickListener {


    private Button mSaveButton, mDeleteButton;
    private Button mNewSpotButton, mViewMapButton;
    private EditText note_edittext, waiting_time_edittext;
    private DatePicker date_datepicker;
    private TimePicker time_timepicker;
    private Spot mCurrentSpot;
    private CheckBox is_destination_check_box;
    private TextView hitchabilityLabel, location_changed, selected_date;
    private LinearLayout spot_form_evaluate, spot_form_location, spot_form_more_options, hitchability_options;
    private RatingBar hitchability_ratingbar;
    private BottomNavigationView menu_bottom;

    private BottomNavigationItemView spot_menuitem, evaluate_menuitem;

    protected static final String TAG = "spot-form-activity";
    protected final static String CURRENT_SPOT_KEY = "current-spot-key";

    //----BEGIN: Part related to reverse geocoding
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";
    protected static final String SELECTED_ATTEMPT_RESULT_KEY = "selected-attempt-result";


    /**
     * Tracks whether the user has requested an address. Becomes true when the user requests an
     * address and false when the address (or an error message) is delivered.
     * The user requests an address by pressing the Fetch Address button. This may happen
     * before GoogleApiClient connects. This activity uses this boolean to keep track of the
     * user's intent. If the value is true, the activity tries to fetch the address as soon as
     * GoogleApiClient connects.
     */
    protected boolean mAddressRequested;

    /**
     * The formatted location address.
     */
    protected Address mAddressOutput;

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;

    /**
     * Displays the location address.
     */
    protected TextView mLocationAddressTextView;

    /**
     * Visible while the address is being fetched.
     */
    ProgressBar mProgressBar;

    /**
     * Kicks off the request to fetch an address when pressed.
     */
    Button mFetchAddressButton;
    //----END: Part related to reverse geocoding

    private MapView mapView;
    protected MapboxMap mapboxMap;
    private LocationEngine locationEngine;
    private LocationEngineListener locationEngineListener, locationEngineListener2;
    private static final int PERMISSIONS_LOCATION = 0;
    private ImageView dropPinView;
    //private android.support.v4.widget.NestedScrollView sv;

    private CoordinatorLayout coordinatorLayout, spot_form_basic;
    private android.support.design.widget.FloatingActionButton fabLocateUser, fabZoomIn, fabZoomOut;

    private NestedScrollView scrollView;
    BottomSheetBehavior mBottomSheetBehavior;
    public AppCompatImageButton mGotARideButton, mTookABreakButton;

    boolean shouldGoBackToPreviousActivity, shouldShowButtonsPanel;

    LinearLayout panel_buttons, panel_info;
MenuItem saveMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getResources().getString(R.string.mapBoxKey));

        setContentView(R.layout.spot_form_master_layout);

        //Set CompatVectorFromResourcesEnabled to true in order to be able to use ContextCompat.getDrawable
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        //Prevent keyboard to be shown when activity starts
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        if (savedInstanceState != null)
            updateValuesFromBundle(savedInstanceState);
        else {
            mCurrentSpot = (Spot) getIntent().getSerializableExtra(Constants.SPOT_BUNDLE_EXTRA_KEY);
            shouldShowButtonsPanel = getIntent().getBooleanExtra(Constants.SHOULD_SHOW_BUTTONS_KEY, false);

            if (getIntent().getBooleanExtra(Constants.SHOULD_SHOW_MAPVIEW_SNACKBAR_KEY, false))
                showViewMapSnackbar();

            shouldGoBackToPreviousActivity = getIntent().getBooleanExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, false);
        }

        mSaveButton = (Button) findViewById(R.id.save_button);
        mDeleteButton = (Button) findViewById(R.id.delete_button);
        mNewSpotButton = (Button) findViewById(R.id.new_spot_button);
        mViewMapButton = (Button) findViewById(R.id.view_map_button);
        note_edittext = (EditText) findViewById(R.id.spot_form_note_edittext);
        date_datepicker = (DatePicker) findViewById(R.id.spot_form_date_datepicker);
        time_timepicker = (TimePicker) findViewById(R.id.spot_form_time_timepicker);
        waiting_time_edittext = (EditText) findViewById(R.id.spot_form_waiting_time_edittext);
        spot_form_more_options = (LinearLayout) findViewById(R.id.save_spot_form_more_options);
        is_destination_check_box = (CheckBox) findViewById(R.id.save_spot_form_is_destination_check_box);
        hitchability_ratingbar = (RatingBar) findViewById(R.id.spot_form_hitchability_ratingbar);
        hitchability_options = (LinearLayout) findViewById(R.id.save_spot_form_hitchability_options);
        hitchabilityLabel = (TextView) findViewById(R.id.spot_form_hitchability_selectedvalue);
        location_changed = (TextView) findViewById(R.id.location_changed_text_view);
        selected_date = (TextView) findViewById(R.id.spot_form_selected_date);

        spot_form_location = (LinearLayout) findViewById(R.id.save_spot_form_location);
        spot_form_basic = (CoordinatorLayout) findViewById(R.id.save_spot_form_basic);
        spot_form_evaluate = (LinearLayout) findViewById(R.id.save_spot_form_evaluate);
        panel_buttons = (LinearLayout) findViewById(R.id.panel_buttons);
        panel_info = (LinearLayout) findViewById(R.id.panel_info);

        saveMenuItem = (MenuItem)findViewById(R.id.action_save);
        menu_bottom = (BottomNavigationView) findViewById(R.id.bottom_navigation);

        spot_menuitem = (BottomNavigationItemView) findViewById(R.id.action_basic);
        evaluate_menuitem = (BottomNavigationItemView) findViewById(R.id.action_evaluate);

        scrollView = (NestedScrollView) findViewById(R.id.spot_form_scrollview);

        mBottomSheetBehavior = BottomSheetBehavior.from(scrollView);

        mGotARideButton = (AppCompatImageButton) findViewById(R.id.got_a_ride_button);
        mTookABreakButton = (AppCompatImageButton) findViewById(R.id.break_button);
        mGotARideButton.setOnClickListener(this);
        mTookABreakButton.setOnClickListener(this);

        //----BEGIN: Part related to reverse geocoding
        mResultReceiver = new AddressResultReceiver(new Handler());

        mLocationAddressTextView = (TextView) findViewById(R.id.location_address_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mFetchAddressButton = (Button) findViewById(R.id.fetch_address_button);

        // Set defaults, then update using values stored in the Bundle.
        mAddressRequested = false;
        mAddressOutput = null;

        updateUIWidgets();
        //----END: Part related to reverse geocoding


        hitchability_ratingbar.setNumStars(Constants.hitchabilityNumOfOptions);
        hitchability_ratingbar.setStepSize(1);
        hitchability_ratingbar.setOnRatingBarChangeListener(this);
        hitchabilityLabel.setText("");
        mLocationAddressTextView.setText("");

        // Get the location engine object for later use.
        locationEngine = LocationSource.getLocationEngine(this);
        locationEngine.activate();

        mapView = (MapView) findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        setupMap();


        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.action_basic:
                                spot_form_basic.setVisibility(View.VISIBLE);
                                spot_form_evaluate.setVisibility(View.GONE);

                                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                                break;
                            case R.id.action_evaluate:
                                spot_form_basic.setVisibility(View.GONE);
                                spot_form_evaluate.setVisibility(View.VISIBLE);

                                break;
                        }
                        return true;
                    }
                });

        // If user is currently waiting for a ride at the current spot, show him the Evaluate form. If he is not,
        // that means he's saving a new spot so we need to show him the Basic form instead.
        if (mCurrentSpot == null) {
            mCurrentSpot = new Spot();
            mCurrentSpot.setStartDateTime(new Date());
            //mCurrentSpot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        }

        if (mCurrentSpot.getIsWaitingForARide() != null && mCurrentSpot.getIsWaitingForARide())
            mFormType = FormType.Evaluate;
        else if (mCurrentSpot.getIsDestination() != null && mCurrentSpot.getIsDestination())
            mFormType = FormType.Destination;
        else {
            // If Id greater than zero, this means the user is editing a spot that was already saved in the database. So show full form.
            if (mCurrentSpot.getId() != null && mCurrentSpot.getId() > 0)
                mFormType = FormType.All;
            else
                mFormType = FormType.Basic;
        }


        if (mFormType == FormType.Evaluate)
            menu_bottom.setSelectedItemId(R.id.action_evaluate);
        else
            menu_bottom.setSelectedItemId(R.id.action_basic);

        updateUI();

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);

    }

    protected void setupMap() {
        //----BEGIN: Map related stuff ----
        if (!mapWasSetUp) {
            mapWasSetUp = true;

            fabLocateUser = (FloatingActionButton) findViewById(R.id.fab_locate_user);
            fabLocateUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mapboxMap != null) {
                        // Check if user has granted location permission
                        if (!PermissionsManager.areLocationPermissionsGranted(SpotFormActivity.this)) {
                            showSnackbar(getString(R.string.waiting_for_gps),
                                    "enable", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            ActivityCompat.requestPermissions(SpotFormActivity.this, new String[]{
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
                                        }
                                    });
                        } else {
                            if (locationEngine.getLastLocation() != null)
                                moveCamera(new LatLng(locationEngine.getLastLocation()));
                            else
                                enableLocation(true);
                            //Toast.makeText(getBaseContext(), getResources().getString(R.string.waiting_for_gps), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });

            fabZoomIn = (FloatingActionButton) findViewById(R.id.fab_zoom_in);
            fabZoomIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mapboxMap != null) {
                        mapboxMap.moveCamera(CameraUpdateFactory.zoomIn());
                    }
                }
            });

            fabZoomOut = (FloatingActionButton) findViewById(R.id.fab_zoom_out);
            fabZoomOut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mapboxMap != null) {
                        mapboxMap.moveCamera(CameraUpdateFactory.zoomOut());
                    }
                }
            });
        }
        //----END: Map related stuff ----
    }

    private static String dateTimeToString(Date dt) {
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.spot_form_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            saveButtonHandler(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    int attemptResult = Constants.ATTEMPT_RESULT_UNKNOWN;

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.got_a_ride_button:
                attemptResult = Constants.ATTEMPT_RESULT_GOT_A_RIDE;
                break;
            case R.id.break_button:
                attemptResult = Constants.ATTEMPT_RESULT_TOOK_A_BREAK;
                break;
            default:
                attemptResult = Constants.ATTEMPT_RESULT_UNKNOWN;
                break;
        }

        //Calculate the waiting time if the spot is still on Evaluate phase (if calculating when editing a spot already evaluated it could mess the waiting time without the user expecting/noticing)
        if (mFormType != FormType.All)
            calculateWaitingTime(null);

        updateAttemptResultButtonsState();
    }

    void updateAttemptResultButtonsState() {
        mGotARideButton.setAlpha((float) 0.5);
        mTookABreakButton.setAlpha((float) 0.5);

        switch (attemptResult) {
            case Constants.ATTEMPT_RESULT_GOT_A_RIDE:
                mGotARideButton.setAlpha((float) 1);
                break;
            case Constants.ATTEMPT_RESULT_TOOK_A_BREAK:
                mTookABreakButton.setAlpha((float) 1);
                break;
        }
    }

    @Override
    public void onMapReady(final MapboxMap mapboxMap) {
        // Customize map with markers, polylines, etc.
        this.mapboxMap = mapboxMap;

        // Customize the user location icon using the getMyLocationViewSettings object.
        this.mapboxMap.getMyLocationViewSettings().setForegroundTintColor(ContextCompat.getColor(getBaseContext(), R.color.mapbox_my_location_ring));//Color.parseColor("#56B881")

        if (mCurrentSpot != null && mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null) {
            // Set camera listener only after the camera has been first moved to the location of the spot being edited
            mapboxMap.setOnCameraChangeListener(new MapboxMap.OnCameraChangeListener() {
                                                    @Override
                                                    public void onCameraChange(@NonNull CameraPosition point) {
                                                        //If the desired position was reached
                                                        if (requestToPositionAt != null && requestToPositionAt.getLatitude() == point.target.getLatitude() &&
                                                                requestToPositionAt.getLongitude() == point.target.getLongitude()) {
                                                            mapboxMap.setOnCameraChangeListener(null);
                                                            requestToPositionAt = null;
                                                            setGeneralOnCameraChangeListener();
                                                        }
                                                    }
                                                }

            );

            //Set start position for map camera: set it to the current waiting spot
            moveCamera(new LatLng(mCurrentSpot.getLatitude(), mCurrentSpot.getLongitude()));
            showLocation(false);

        } else {
            setGeneralOnCameraChangeListener();

            //Set start position for map camera: set it to the last spot saved
            Spot lastAddedSpot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getLastAddedSpot();
            if (lastAddedSpot != null && lastAddedSpot.getLatitude() != null && lastAddedSpot.getLongitude() != null
                    && lastAddedSpot.getLatitude() != 0.0 && lastAddedSpot.getLongitude() != 0.0) {
                LatLng pos = new LatLng(lastAddedSpot.getLatitude(), lastAddedSpot.getLongitude());

                //If at the last added spot the user took a break, then he might be still close to that spot - zoom close to it! Otherwise, we zoom a bit out/farther.
                if (lastAddedSpot.getAttemptResult() != null && lastAddedSpot.getAttemptResult() == Constants.ATTEMPT_RESULT_TOOK_A_BREAK)
                    moveCamera(pos, Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT);
                else
                    moveCamera(pos, Constants.ZOOM_TO_SEE_FARTHER_DISTANCE);
            } else if (locationEngine.getLastLocation() != null)
                moveCamera(new LatLng(locationEngine.getLastLocation()));

            showLocation(true);
        }


        addPinToCenter();
    }

    void setGeneralOnCameraChangeListener() {
        //The camera position was changed by the user
        mapboxMap.setOnCameraChangeListener(new MapboxMap.OnCameraChangeListener() {
                                                @Override
                                                public void onCameraChange(@NonNull CameraPosition point) {
                                                    //If the desired position was reached
                                                    if (requestToPositionAt != null && requestToPositionAt.getLatitude() == point.target.getLatitude() &&
                                                            requestToPositionAt.getLongitude() == point.target.getLongitude()) {
                                                        requestToPositionAt = null;

                                                        mAddressOutput = null;
                                                        mCurrentSpot.setGpsResolved(false);
                                                        mLocationAddressTextView.setText(getString(R.string.spot_form_location_selected_label));
                                                        locationManuallyChanged = true;
                                                    }
                                                }
                                            }

        );
    }

    private LatLng requestToPositionAt = null;
    private boolean locationManuallyChanged = false;

    /**
     * Move the map camera to the given position
     *
     * @param latLng Target location to change to
     * @param zoom   Zoom level to change to
     */


    /**
     * Move the map camera to the given position with zoom Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT
     *
     * @param latLng Target location to change to
     */
    private void moveCamera(LatLng latLng) {
        moveCamera(latLng, Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT);
    }

    private void moveCamera(LatLng latLng, long zoom) {
        if (latLng != null) {
            if (mapboxMap == null)
                Crashlytics.log(Log.INFO, TAG, "For some reason map was not loaded, therefore mapboxMap.moveCamera() was skipped to avoid crash. Shouldn't the map be loaded at this point?");
            else {
                requestToPositionAt = latLng;
                mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
            }
        }
    }


    private LatLng getPinPosition() {
        if (!mapWasSetUp)
            return null;

        //Copied from: https://github.com/mapbox/mapbox-gl-native/blob/e2da260a8ee0dd0b213ec0e30db2c6e3188c7c9b/platform/android/MapboxGLAndroidSDKTestApp/src/main/java/com/mapbox/mapboxsdk/testapp/GeocoderActivity.java
        LatLng centerLatLng = new LatLng(mapboxMap.getProjection().fromScreenLocation(getCenterPoint()));

        return centerLatLng;
    }

    private PointF getCenterPoint() {
        if (!mapWasSetUp)
            return null;

        final int width = mapView.getMeasuredWidth();
        final int height = mapView.getMeasuredHeight();

        return new PointF(width / 2, (height + dropPinView.getHeight()) / 2);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();


        if (mapWasSetUp)
            mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapWasSetUp)
            mapView.onPause();

        if (snackbar != null)
            snackbar.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapWasSetUp) {
            mapView.onDestroy();

            // Ensure no memory leak occurs if we register the location listener but the call hasn't
            // been made yet.
            if (locationEngineListener != null)
                locationEngine.removeLocationEngineListener(locationEngineListener);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapWasSetUp)
            mapView.onLowMemory();
    }

    private boolean mapWasSetUp = false;

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.confirm_back_button_click_dialog_title))
                    .setMessage(getResources().getString(R.string.confirm_back_button_click_dialog_message))
                    .setPositiveButton(getResources().getString(R.string.general_yes_option), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Set result to RESULT_CANCELED so that the activity who opened the current SpotFormActivity knows that nothing was changed in the dataset
                            //Set result so that the activity who opened the current SpotFormActivity knows that the dataset was changed and it should make the necessary updates on the UI
                            Intent intent = new Intent();
                            setResult(RESULT_CANCELED, intent);
                            finish();
                        }

                    })
                    .setNegativeButton(getResources().getString(R.string.general_no_option), null)
                    .show();
        }
    }


    protected void showLocation(boolean shouldMoveToFirstLocation) {
        // Check if user has granted location permission
        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            /*Snackbar.make(coordinatorLayout, getResources().getString(R.string.waiting_for_gps), Snackbar.LENGTH_LONG)
                    .setAction("enable", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {*/
            ActivityCompat.requestPermissions(SpotFormActivity.this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            //            }
            //        }).show();
        } else
            enableLocation(shouldMoveToFirstLocation);
    }

    /**
     * Represents a geographical location.
     * <p>
     * protected Location mCurrentLocation;
     * boolean wasFirstLocationReceived = false;
     */

    private void enableLocation(boolean shouldMoveToFirstLocation) {
        if (shouldMoveToFirstLocation) {
            locationEngineListener = new LocationEngineListener() {
                @Override
                public void onConnected() {
                    // No action needed here.
                }

                @Override
                public void onLocationChanged(Location location) {
                    // Move the map camera to where the user location is and then remove the
                    // listener so the camera isn't constantly updating when the user location
                    // changes. When the user disables and then enables the location again, this
                    // listener is registered again and will adjust the camera once again.
                    if (location != null) {
                        //Fetch address for the first location found
                        if ((mCurrentSpot.getId() == null || mCurrentSpot.getId() == 0)
                                && mCurrentSpot.getGpsResolved() == null || !mCurrentSpot.getGpsResolved())
                            fetchAddress(new MyLocation(location.getLatitude(), location.getLongitude()));


                        moveCamera(new LatLng(location));
                        locationEngine.removeLocationEngineListener(this);
                    }
                }
            };

            locationEngine.addLocationEngineListener(locationEngineListener);
        }

        // Enable or disable the location layer on the map
        mapboxMap.setMyLocationEnabled(true);
    }

    private void addPinToCenter() {
        if (!mapWasSetUp)
            return;

        try {
            //Drawable d = ContextCompat.getDrawable(this, R.drawable.ic_add);

            dropPinView = new ImageView(this);
            dropPinView.setImageResource(R.drawable.ic_add);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            dropPinView.setLayoutParams(params);
            mapView.addView(dropPinView);

        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation(true);
                //updateUISaveButtons();
            }
        }
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {

        hitchabilityLabel.setText(getRatingString(Math.round(rating)));
    }

    private String getRatingString(Integer rating) {
        String res = "";
        switch (rating) {
            case 1:
                res = getResources().getString(R.string.hitchability_senseless);
                break;
            case 2:
                res = getResources().getString(R.string.hitchability_bad);
                break;
            case 3:
                res = getResources().getString(R.string.hitchability_average);
                break;
            case 4:
                res = getResources().getString(R.string.hitchability_good);
                break;
            case 5:
                res = getResources().getString(R.string.hitchability_very_good);
                break;
           /* default:
                res = getResources().getString(R.string.hitchability_no_answer);
                break;*/
        }
        return res;
    }

    private enum FormType {
        Unknown,
        Basic,
        Evaluate,
        Destination,
        All
    }

    private FormType mFormType = FormType.Unknown;
    private boolean updateUIFirstCalled = false;

    private void updateUI() {
        try {
            // If user is currently waiting for a ride at the current spot, who him the Evaluate form. If he is not,
            // that means he's saving a new spot so we need to show him the Basic form instead.

            attemptResult = Constants.ATTEMPT_RESULT_UNKNOWN;
            if (mCurrentSpot.getAttemptResult() != null)
                attemptResult = mCurrentSpot.getAttemptResult();
            updateAttemptResultButtonsState();

            String title = "";
            if (mFormType == FormType.Basic || mCurrentSpot == null || mCurrentSpot.getId() == null || mCurrentSpot.getId() == 0)
                title = getResources().getString(R.string.save_spot_button_text);
            else if (mFormType == FormType.Destination || mFormType == FormType.All)
                title = getResources().getString(R.string.spot_form_title_edit);
            else {
               /* switch (attemptResult) {
                    case Constants.ATTEMPT_RESULT_GOT_A_RIDE:
                        title = getResources().getString(R.string.got_a_ride_button_text);
                        break;
                    case Constants.ATTEMPT_RESULT_TOOK_A_BREAK:
                        title = getResources().getString(R.string.break_button_text);
                        break;
                    default:*/
                title = getResources().getString(R.string.spot_form_title_evaluate);
                       /* break;
                }*/
            }

            if (shouldShowButtonsPanel) {
                saveMenuItem.setEnabled(false);
                panel_buttons.setVisibility(View.VISIBLE);
                panel_info.setVisibility(View.GONE);

                locationEngineListener2 = new LocationEngineListener() {
                    @Override
                    public void onConnected() {
                        // No action needed here.
                    }

                    @Override
                    public void onLocationChanged(Location location) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        if (location != null)
                            moveCamera(new LatLng(location));
                    }
                };

                locationEngine.addLocationEngineListener(locationEngineListener2);
            } else {
                saveMenuItem.setEnabled(true);
                panel_buttons.setVisibility(View.GONE);
                panel_info.setVisibility(View.VISIBLE);
                locationEngine.removeLocationEngineListener(locationEngineListener2);
            }

            if (mCurrentSpot != null && mCurrentSpot.getGpsResolved() != null && mCurrentSpot.getGpsResolved())
                mLocationAddressTextView.setText(getString(mCurrentSpot));
            else
                mLocationAddressTextView.setText(getResources().getString(R.string.spot_form_location_selected_label));

            //Automatically calculate the waiting time if the spot is still on Evaluate phase (if calculating when editing a spot already evaluated it could mess the waiting time without the user expecting/noticing)
            if (getCallingActivity() != null && mFormType == FormType.Evaluate)
                calculateWaitingTime(null);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTitle(title.toUpperCase());

            if (mFormType == FormType.Unknown) {
                Crashlytics.logException(new Exception("mFormType is Unkonwn"));
                mSaveButton.setEnabled(false);
                showErrorAlert(getResources().getString(R.string.general_error_dialog_title), "Please try opening your spot again.");
            }

            //Show delete button when the spot is been edited and it's not of type Evaluate
            if ((mCurrentSpot.getId() != null && mCurrentSpot.getId() > 0) && mFormType != FormType.Evaluate)
                mDeleteButton.setVisibility(View.VISIBLE);
            else
                mDeleteButton.setVisibility(View.GONE);

            if (mFormType == FormType.Basic || mFormType == FormType.Destination)
                evaluate_menuitem.setEnabled(false);
            else
                evaluate_menuitem.setEnabled(true);

            //To prevent the values and listeners of datepicker and timepicker been set more than once, call SetDateTime only when !updateUIFirstCalled
            if (!updateUIFirstCalled) {
                Date spotStartDT = new Date();
                if (mCurrentSpot.getStartDateTime() != null)
                    spotStartDT = mCurrentSpot.getStartDateTime();
                SetDateTime(date_datepicker, time_timepicker, spotStartDT);
            }

            if (mCurrentSpot.getWaitingTime() != null) {
                String val = mCurrentSpot.getWaitingTime().toString();
                waiting_time_edittext.setText(val);
            }

            if (mCurrentSpot.getNote() != null)
                note_edittext.setText(mCurrentSpot.getNote());


            if (mFormType == FormType.Destination)
                is_destination_check_box.setChecked(true);
            else
                is_destination_check_box.setChecked(false);

            int h = 0;
            if (mCurrentSpot.getHitchability() != null) {
                //getHitchability() is always the position of the selected star on the ratingbar.
                if (mCurrentSpot.getHitchability() >= hitchability_ratingbar.getNumStars() || mCurrentSpot.getHitchability() < 0) {
                    h = 0;
                    Crashlytics.setInt("mCurrentSpot.getHitchability", mCurrentSpot.getHitchability());
                    Crashlytics.setInt("hitchability_ratingbar.getNumStars", hitchability_ratingbar.getNumStars());
                    Crashlytics.log(Log.WARN, TAG, "The selected hitchability is smaller than 0 or bigger than the number of stars in the rating bar. Nothing was selected, but this is a very unexpected bug that deserves a close check.");
                } else
                    h = mCurrentSpot.getHitchability();
            }
            hitchability_ratingbar.setRating(findTheOpposit(h));

        } catch (Exception ex) {
            //setTitle(getResources().getString(R.string.spot_form_bottommenu_map_tile));
            Crashlytics.logException(ex);
            showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message), ex.getMessage()));
        }

        updateUIFirstCalled = true;
    }

    public void calculateWaitingTime(View view) {
        DateTime date = GetDateTime(date_datepicker, time_timepicker);
        Integer minutes = Minutes.minutesBetween(date, DateTime.now()).getMinutes();
        waiting_time_edittext.setText(minutes.toString());
        Toast.makeText(this, getResources().getString(R.string.spot_form_waiting_time_label) + ": " + minutes, Toast.LENGTH_LONG).show();
    }

    private static Integer findTheOpposit(Integer rating) {
        //NOTE: For sure there should be a math formula to find this result, I just didn't feel like using
        // more time on this so why not a switch until you make it better =)
        Integer res = 0;
        switch (rating) {
            case 1:
                res = 5;
                break;
            case 2:
                res = 4;
                break;
            case 3:
                res = 3;
                break;
            case 4:
                res = 2;
                break;
            case 5:
                res = 1;
                break;
        }
        return res;
    }

    public void locationAddressButtonHandler(View v) {
        String strToCopy = spotLocationToString(mCurrentSpot).trim();

        if ((strToCopy != null && !strToCopy.isEmpty()))
            strToCopy += " ";

        if (mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null)
            strToCopy += String.format("(%1$s, %2$s)",
                    mCurrentSpot.getLatitude().toString(), mCurrentSpot.getLongitude().toString());

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Location", strToCopy);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getApplicationContext(), getResources().getString(R.string.spot_form_location_info_copied), Toast.LENGTH_LONG).show();

    }

    public void newSpotButtonHandler(View view) {
        SetDateTime(date_datepicker, time_timepicker, new Date());
        locationEngine.removeLocationEngineListener(locationEngineListener2);
        panel_buttons.setVisibility(View.GONE);
        panel_info.setVisibility(View.VISIBLE);
        saveMenuItem.setEnabled(true);
    }

    public void viewMapButtonHandler(View view) {
        startActivity(new Intent(getBaseContext(), MapViewActivity.class));
    }

    public void saveButtonHandler(View view) {
        if (mFormType != FormType.Basic && !is_destination_check_box.isChecked() &&
                waiting_time_edittext.getText().toString().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.waiting_time_missing_dialog_title))
                    .setMessage(getString(R.string.waiting_time_missing_dialog_message))
                    .setPositiveButton(getString(R.string.general_yes_option), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            calculateWaitingTime(null);
                            saveSpot();
                        }

                    })
                    .setNegativeButton(getString(R.string.general_no_option), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveSpot();
                        }

                    })
                    .show();
        } else
            saveSpot();
    }

    void saveSpot() {
        try {
            if (!mapWasSetUp || mapboxMap == null ||
                    mapboxMap.getCameraPosition() == null || mapboxMap.getCameraPosition().target == null) {
                Crashlytics.log(Log.INFO, TAG, "For some reason map was not loaded, so we couldn't get the chosen location");
                showErrorAlert(getResources().getString(R.string.save_spot_button_text), getResources().getString(R.string.save_spot_error_map_not_loaded));
                return;
            } else {
                LatLng selectedLocation = mapboxMap.getCameraPosition().target;

                mCurrentSpot.setLatitude(selectedLocation.getLatitude());
                mCurrentSpot.setLongitude(selectedLocation.getLongitude());
            }

            mCurrentSpot.setNote(note_edittext.getText().toString());

            if (is_destination_check_box.isChecked()) {
                mCurrentSpot.setIsDestination(true);
                mCurrentSpot.setHitchability(0);
                mCurrentSpot.setIsWaitingForARide(false);
                mCurrentSpot.setAttemptResult(Constants.ATTEMPT_RESULT_UNKNOWN);
            } else {
                mCurrentSpot.setIsDestination(false);
                mCurrentSpot.setHitchability(findTheOpposit(Math.round(hitchability_ratingbar.getRating())));
                if (mFormType == FormType.Basic)
                    mCurrentSpot.setIsWaitingForARide(true);
                else
                    mCurrentSpot.setIsWaitingForARide(false);

                String vals = waiting_time_edittext.getText().toString();
                if (!vals.isEmpty())
                    mCurrentSpot.setWaitingTime(Integer.parseInt(vals));
                else
                    mCurrentSpot.setWaitingTime(0);
                mCurrentSpot.setAttemptResult(attemptResult);
            }

            DateTime dateTime = GetDateTime(date_datepicker, time_timepicker);
            mCurrentSpot.setStartDateTime(dateTime.toDate());


            if (mCurrentSpot.getLatitude() == null || mCurrentSpot.getLongitude() == null) {
                Crashlytics.logException(new Exception("User tried to save a spot without coordinates?"));
                showErrorAlert(getResources().getString(R.string.save_spot_button_text), getResources().getString(R.string.save_spot_error_coordinate_not_informed_error_message));
                return;
            }

        } catch (Exception ex) {
            Crashlytics.logException(ex);
            showErrorAlert(getResources().getString(R.string.save_spot_button_text), String.format(getResources().getString(R.string.save_spot_error_general), ex.getMessage()));
        }

        new Thread() {
            @Override
            public void run() {
                DaoSession daoSession = ((MyHitchhikingSpotsApplication) getApplicationContext()).getDaoSession();
                SpotDao spotDao = daoSession.getSpotDao();
                spotDao.insertOrReplace(mCurrentSpot);
                ((MyHitchhikingSpotsApplication) getApplicationContext()).setCurrentSpot(mCurrentSpot);

                // code runs in a thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int result = RESULT_OBJECT_ADDED;
                        if (mFormType == FormType.Evaluate || mFormType == FormType.All)
                            result = RESULT_OBJECT_EDITED;

                        finishSaving(result);
                    }
                });
            }
        }.start();
    }

    public void deleteButtonHandler(View view) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.spot_form_delete_dialog_title_text))
                .setMessage(getResources().getString(R.string.spot_form_delete_dialog_message_text))
                .setPositiveButton(getResources().getString(R.string.spot_form_delete_dialog_yes_option), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread() {
                            @Override
                            public void run() {
                                DaoSession daoSession = ((MyHitchhikingSpotsApplication) getApplicationContext()).getDaoSession();
                                SpotDao spotDao = daoSession.getSpotDao();
                                spotDao.delete(mCurrentSpot);
                                ((MyHitchhikingSpotsApplication) getApplicationContext()).setCurrentSpot(null);

                                // code runs in a thread
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), R.string.spot_deleted_successfuly, Toast.LENGTH_LONG).show();
                                        finish();

                                        ComponentName callingActivity = getCallingActivity();
                                        if (!shouldGoBackToPreviousActivity || callingActivity == null || callingActivity.getClassName() == null
                                                || !callingActivity.getClassName().equals(MapViewActivity.class.getName()))
                                            startActivity(new Intent(getBaseContext(), MapViewActivity.class));
                                    }
                                });
                            }
                        }.start();
                    }

                })
                .setNegativeButton(getResources().getString(R.string.spot_form_delete_dialog_no_option), null)
                .show();
    }

    private void finishSaving(int result) {
        ComponentName callingActivity = getCallingActivity();
        if (mCurrentSpot.getIsDestination() != null && mCurrentSpot.getIsDestination()) {
            finish();
            if (callingActivity == null || !shouldGoBackToPreviousActivity)
                startActivity(new Intent(getBaseContext(), MapViewActivity.class));
            return;
        }

        //If SpotFormActivity was called by StartActivityForResult then getCallingActivity won't be null and we should call finish() to return to the calling activity
        if (callingActivity != null) {
            Intent intent = new Intent();

            Bundle conData = new Bundle();
            //If action was canceled means that nothing changed in the object and therefore we don't need to use processing time serializing the object here
            if (result != RESULT_CANCELED) {
                conData.putString(Constants.SPOT_BUNDLE_EXTRA_ID_KEY, mCurrentSpot.getId().toString());
                conData.putSerializable(Constants.SPOT_BUNDLE_EXTRA_KEY, mCurrentSpot);
            }

            conData.putBoolean(Constants.SHOULD_SHOW_MAPVIEW_SNACKBAR_KEY, true);
            intent.putExtras(conData);

            //Set result so that the activity who opened the current SpotFormActivity knows that the dataset was changed and it should make the necessary updates on the UI
            setResult(result, intent);
            finish();
        } else {
            Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);

            Bundle conData = new Bundle();
            if (mFormType == FormType.Basic)
                conData.putSerializable(Constants.SPOT_BUNDLE_EXTRA_KEY, mCurrentSpot);

            if (mFormType != FormType.Basic)
                conData.putBoolean(Constants.SHOULD_SHOW_BUTTONS_KEY, true);

            conData.putBoolean(Constants.SHOULD_SHOW_MAPVIEW_SNACKBAR_KEY, true);
            intent.putExtras(conData);

            finish();
            startActivity(intent);
        }
    }

    public void moreOptionsButtonHandler(View view) {
        if (spot_form_more_options.isShown())
            spot_form_more_options.setVisibility(View.GONE);
        else
            spot_form_more_options.setVisibility(View.VISIBLE);

    }

    public void isDestinationHandleChecked(View view) {
        if (is_destination_check_box.isChecked())
            evaluate_menuitem.setEnabled(false);
        else if (mFormType != FormType.Basic)
            evaluate_menuitem.setEnabled(true);
    }

    public void SetDateTime(DatePicker datePicker, TimePicker timePicker, Date date) {
        selected_date.setText(dateTimeToString(date));

        DateTime dateTime = new DateTime(date);

        // Must always subtract 1 here as DatePicker month is 0 based
        date_datepicker.init(dateTime.getYear(), dateTime.getMonthOfYear() - 1, dateTime.getDayOfMonth(), new DatePicker.OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth) {
                DateTime selectedDateTime = GetDateTime(date_datepicker, time_timepicker);
                selected_date.setText(dateTimeToString(selectedDateTime.toDate()));
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            timePicker.setHour(dateTime.getHourOfDay());
            timePicker.setMinute(dateTime.getMinuteOfHour());
        } else {
            timePicker.setCurrentHour(dateTime.getHourOfDay());
            timePicker.setCurrentMinute(dateTime.getMinuteOfHour());
        }

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {

            @Override
            public void onTimeChanged(TimePicker var1, int var2, int var3) {
                DateTime selectedDateTime = GetDateTime(date_datepicker, time_timepicker);
                selected_date.setText(dateTimeToString(selectedDateTime.toDate()));
            }
        });
    }


    public DateTime GetDateTime(DatePicker datePicker, TimePicker timePicker) {
        Integer hour, minute;

        if (Build.VERSION.SDK_INT >= 23) {
            hour = timePicker.getHour();
            minute = timePicker.getMinute();
        } else {
            hour = timePicker.getCurrentHour();
            minute = timePicker.getCurrentMinute();
        }

        DateTime dateTime = new DateTime(datePicker.getYear(), datePicker.getMonth() + 1, datePicker.getDayOfMonth(),
                hour, minute); // Must always add 1 to datePickers getMounth returned value, as it is 0 based
        return dateTime;
    }

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putSerializable(CURRENT_SPOT_KEY, mCurrentSpot);

        //----BEGIN: Part related to reverse geocoding
        // Save whether the address has been requested.
        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);

        // Save the address string.
        savedInstanceState.putParcelable(LOCATION_ADDRESS_KEY, mAddressOutput);
        //----END: Part related to reverse geocoding

        savedInstanceState.putInt(SELECTED_ATTEMPT_RESULT_KEY, attemptResult);

        if (mapWasSetUp)
            mapView.onSaveInstanceState(savedInstanceState);

        super.onSaveInstanceState(savedInstanceState);
    }


    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Crashlytics.log(Log.INFO, TAG, "Updating values from bundle");
        if (savedInstanceState != null) {

            // Update the value of mCurrentSpot from the Bundle
            if (savedInstanceState.keySet().contains(CURRENT_SPOT_KEY)) {
                // Since CURRENT_SPOT_KEY was found in the Bundle, we can be sure that mCurrentSpot
                // is not null.
                mCurrentSpot = (Spot) savedInstanceState.getSerializable(CURRENT_SPOT_KEY);
            }

            //----BEGIN: Part related to reverse geocoding
            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getParcelable(LOCATION_ADDRESS_KEY);
            }
            //----END: Part related to reverse geocoding

            if (savedInstanceState.keySet().contains(SELECTED_ATTEMPT_RESULT_KEY)) {
                attemptResult = savedInstanceState.getInt(SELECTED_ATTEMPT_RESULT_KEY);
            }

        }
    }

    //----BEGIN: Part related to reverse geocoding
    Snackbar snackbar;

    void showSnackbar(@NonNull CharSequence text, CharSequence action, View.OnClickListener listener) {
        if (snackbar == null)
            snackbar = Snackbar.make(coordinatorLayout, text, Snackbar.LENGTH_LONG)
                    .setAction(action, listener);
        snackbar.show();
    }

    void showViewMapSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_saved_successfuly),
                getString(R.string.map_error_alert_map_not_loaded_negative_button), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(getBaseContext(), MapViewActivity.class));
                    }
                });
    }

    /**
     * Runs when user clicks the Fetch Address button. Starts the service to fetch the address if
     * GoogleApiClient is connected.
     */
    public void fetchAddressButtonHandler(View view) {
        if (!mapWasSetUp || mapboxMap == null)
            return;

        LatLng pinPosition = mapboxMap.getCameraPosition().target;

        // We only start the service to fetch the address if GoogleApiClient is connected.
        if (pinPosition == null)
            return;

        fetchAddress(new MyLocation(pinPosition.getLatitude(), pinPosition.getLongitude()));
    }

    public void fetchAddress(MyLocation loc) {
        startIntentService(loc);

        // If GoogleApiClient isn't connected, we process the user's request by setting
        // mAddressRequested to true. Later, when GoogleApiClient connects, we launch the service to
        // fetch the address. As far as the user is concerned, pressing the Fetch Address button
        // immediately kicks off the process of getting the address.
        mAddressRequested = true;
        updateUIWidgets();
    }

    Intent fetchAddressServiceIntent;

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    protected void startIntentService(MyLocation location) {
        // Create an intent for passing to the intent service responsible for fetching the address.
        fetchAddressServiceIntent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        fetchAddressServiceIntent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        fetchAddressServiceIntent.putExtra(Constants.LOCATION_DATA_EXTRA, location);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(fetchAddressServiceIntent);
    }

    protected void stopIntentService() {
        if (!mAddressRequested || fetchAddressServiceIntent == null)
            return;

        stopService(fetchAddressServiceIntent);
        mAddressRequested = false;
        updateUIWidgets();
    }

    /**
     * Updates the address in the UI.
     */
    protected void displayAddressOutput() {
        if (mAddressOutput != null) {
            mCurrentSpot.setCity(mAddressOutput.getLocality());
            mCurrentSpot.setState(mAddressOutput.getAdminArea());
            mCurrentSpot.setCountry(mAddressOutput.getCountryName());
            mCurrentSpot.setCountryCode(mAddressOutput.getCountryCode());
            mCurrentSpot.setGpsResolved(true);
        } else {
            mCurrentSpot.setCity("");
            mCurrentSpot.setState("");
            mCurrentSpot.setCountry("");
            mCurrentSpot.setCountryCode("");
            mCurrentSpot.setGpsResolved(false);
        }

        mLocationAddressTextView.setText(getString(mCurrentSpot));

    }

    @NonNull
    private String getString(Spot mCurrentSpot) {
        String spotLoc = "";
        try {
            spotLoc = spotLocationToString(mCurrentSpot).trim();
           /* if ((spotLoc == null || spotLoc.isEmpty()) && (mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null))
                spotLoc = getResources().getString(R.string.spot_form_location_selected_label);
            spotLoc = String.format(getResources().getString(R.string.spot_form_lat_lng_label),
                        mCurrentSpot.getLatitude().toString(), mCurrentSpot.getLongitude().toString());*/
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }
        return spotLoc;
    }


    static String locationSeparator = ", ";

    private static String spotLocationToString(Spot spot) {
        Crashlytics.log(Log.INFO, TAG, "Generating a string for the spot's address");

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
            Crashlytics.logException(ex);
        }
        return "";
    }


    /**
     * Toggles the visibility of the progress bar. Enables or disables the Fetch Address button.
     */
    private void updateUIWidgets() {
        if (mAddressRequested) {
            mLocationAddressTextView.setVisibility(View.GONE);
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            mFetchAddressButton.setEnabled(false);
        } else {
            mLocationAddressTextView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(ProgressBar.GONE);
            mFetchAddressButton.setEnabled(true);
        }
    }

    Toast msgResult;

    /**
     * Shows a toast with the given text.
     */
    protected void showToast(String text) {
        if (msgResult == null)
            msgResult = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        else {
            msgResult.cancel();
            msgResult.setText(text);
        }
        msgResult.show();
    }


    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         * Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String strResult = "";

            // Show a toast message notifying whether an address was found.
            if (resultCode == Constants.FAILURE_RESULT) {
                strResult = resultData.getString(Constants.RESULT_STRING_KEY);
                mAddressOutput = null;
            } else {
                strResult = getString(R.string.address_found);

                // Display the address string or an error message sent from the intent service.
                mAddressOutput = resultData.getParcelable(Constants.RESULT_ADDRESS_KEY);
            }

            displayAddressOutput();

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            mAddressRequested = false;
            showToast(strResult);
            updateUIWidgets();
        }
    }

//----END: Part related to reverse geocoding

}
