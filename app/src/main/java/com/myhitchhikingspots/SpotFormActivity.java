package com.myhitchhikingspots;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Address;
import android.location.Location;
import android.os.Build;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationListener;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.ArrayList;
import java.util.Date;

import android.content.Intent;
import android.os.Handler;
import android.os.ResultReceiver;

public class SpotFormActivity extends BaseActivity implements RatingBar.OnRatingBarChangeListener, OnMapReadyCallback {


    private Button mSaveButton, mDeleteButton;
    private EditText note_edittext, waiting_time_edittext;
    private DatePicker date_datepicker;
    private TimePicker time_timepicker;
    private Spinner attempt_results_spinner;
    private Spot mCurrentSpot;
    private CheckBox is_destination_check_box;
    private TextView form_title, hitchabilityLabel, location_changed;
    private LinearLayout spot_form_evaluate, spot_form_basic, spot_form_more_options, hitchability_options, attempt_result_panel;
    private RatingBar hitchability_ratingbar;

    protected static final String TAG = "save_spot";
    protected final static String CURRENT_SPOT_KEY = "current-spot-key";

    //----BEGIN: Part related to reverse geocoding
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";

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
    private com.mapbox.mapboxsdk.location.LocationServices locationServices;
    private static final int PERMISSIONS_LOCATION = 0;
    private ImageView dropPinView;

    private CoordinatorLayout coordinatorLayout;
    private android.support.design.widget.FloatingActionButton fabLocateUser, fabZoomIn, fabZoomOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.spot_form_master_layout);

        mSaveButton = (Button) findViewById(R.id.save_button);
        mDeleteButton = (Button) findViewById(R.id.delete_button);
        note_edittext = (EditText) findViewById(R.id.spot_form_note_edittext);
        date_datepicker = (DatePicker) findViewById(R.id.spot_form_date_datepicker);
        time_timepicker = (TimePicker) findViewById(R.id.spot_form_time_timepicker);
        waiting_time_edittext = (EditText) findViewById(R.id.spot_form_waiting_time_edittext);
        attempt_results_spinner = (Spinner) findViewById(R.id.spot_form_attempt_result_spinner);
        spot_form_basic = (LinearLayout) findViewById(R.id.save_spot_form_basic);
        spot_form_evaluate = (LinearLayout) findViewById(R.id.save_spot_form_evaluate);
        spot_form_more_options = (LinearLayout) findViewById(R.id.save_spot_form_more_options);
        is_destination_check_box = (CheckBox) findViewById(R.id.save_spot_form_is_destination_check_box);
        attempt_result_panel = (LinearLayout) findViewById(R.id.save_spot_form_attempt_result_panel);
        form_title = (TextView) findViewById(R.id.save_spot_form_title);
        hitchability_ratingbar = (RatingBar) findViewById(R.id.spot_form_hitchability_ratingbar);
        hitchability_options = (LinearLayout) findViewById(R.id.save_spot_form_hitchability_options);
        hitchabilityLabel = (TextView) findViewById(R.id.spot_form_hitchability_selectedvalue);
        location_changed = (TextView) findViewById(R.id.location_changed_text_view);


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

        if (savedInstanceState != null)
            updateValuesFromBundle(savedInstanceState);
        else
            mCurrentSpot = (Spot) getIntent().getSerializableExtra(Constants.SPOT_BUNDLE_EXTRA_KEY);

        // If user is currently waiting for a ride at the current spot, show him the Evaluate form. If he is not,
        // that means he's saving a new spot so we need to show him the Basic form instead.
        if (mCurrentSpot == null)
            mFormType = FormType.Unknown;
        else {
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
        }

        if (mFormType != FormType.Evaluate) {
            //----BEGIN: Map related stuff ----
            locationServices = com.mapbox.mapboxsdk.location.LocationServices.getLocationServices(SpotFormActivity.this);

            // Mapbox access token is configured here. This needs to be called either in your application
            // object or in the same activity which contains the mapview.
            MapboxAccountManager.start(getApplicationContext(), getResources().getString(R.string.mapBoxKey));


            mapView = (MapView) findViewById(R.id.mapview2);
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);

            mapIsDisplayed = true;

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
                                            ActivityCompat.requestPermissions(SpotFormActivity.this, new String[]{
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
            //----END: Map related stuff ----
        }

        updateUI();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onMapReady(final MapboxMap mapboxMap) {
        // Customize map with markers, polylines, etc.
        this.mapboxMap = mapboxMap;

        if (mCurrentSpot != null && mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null) {
            //Set start position for map camera: set it to the current waiting spot
            moveCamera(new LatLng(mCurrentSpot.getLatitude(), mCurrentSpot.getLongitude()));
            mLocationAddressTextView.setText(getString((Spot) mCurrentSpot));
        } else {
            //Set start position for map camera: set it to the last spot saved
            if (mFormType == FormType.Basic || mFormType == FormType.Destination) {
                //Set zoom level to 6 to make it easier for the user to find his new position easier
                LatLng spot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getLastAddedSpotPosition();
                if (spot != null)
                    moveCamera(spot, 6);
            }

            locationServices.addLocationListener(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Move the map camera to where the user location is and then remove the
                        // listener so the camera isn't constantly updating when the user location
                        // changes. When the user disables and then enables the location again, this
                        // listener is registered again and will adjust the camera once again.
                        moveCamera(new LatLng(location));
                        locationServices.removeLocationListener(this);
                    }
                }
            });
        }

        showLocation();

        addPinToCenter();

        // Camera change listener
        mapboxMap.setOnCameraChangeListener(new MapboxMap.OnCameraChangeListener() {
                                                @Override
                                                public void onCameraChange(@NonNull CameraPosition point) {

                                                    if (isCameraPositionChangingByCodeRequest) {
                                                        if (positionAt != null && positionAt.getLatitude() == point.target.getLatitude() &&
                                                                positionAt.getLongitude() == point.target.getLongitude())
                                                            isCameraPositionChangingByCodeRequest = false;
                                                    } else {
                                                        //The camera position was changed by the user

                                                        mAddressOutput = null;
                                                        mCurrentSpot.setGpsResolved(false);
                                                        mLocationAddressTextView.setText(getResources().getString(R.string.spot_form_location_selected_label));
                                                        //location_changed.setVisibility(View.VISIBLE);
                                                        locationManuallyChanged = true;
                                                    }
                                                }
                                            }

        );
    }

    private LatLng positionAt = null;
    private boolean isCameraPositionChangingByCodeRequest = false;
    private boolean locationManuallyChanged = false;

    /**
     * Move the map camera to the given position with zoom 16
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
     * Move the map camera to the given position with zoom 17
     *
     * @param latLng Target location to change to
     */
    private void moveCamera(LatLng latLng) {
        moveCamera(latLng, 17);
    }

    private LatLng getPinPosition() {
        if (!mapIsDisplayed)
            return null;

        //Copied from: https://github.com/mapbox/mapbox-gl-native/blob/e2da260a8ee0dd0b213ec0e30db2c6e3188c7c9b/platform/android/MapboxGLAndroidSDKTestApp/src/main/java/com/mapbox/mapboxsdk/testapp/GeocoderActivity.java
        LatLng centerLatLng = new LatLng(mapboxMap.getProjection().fromScreenLocation(getCenterPoint()));

        return centerLatLng;
    }

    private PointF getCenterPoint() {
        if (!mapIsDisplayed)
            return null;

        final int width = mapView.getMeasuredWidth();
        final int height = mapView.getMeasuredHeight();

        return new PointF(width / 2, (height + dropPinView.getHeight()) / 2);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mapIsDisplayed)
            mapView.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapIsDisplayed)
            mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapIsDisplayed)
            mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapIsDisplayed)
            mapView.onLowMemory();
    }

    private boolean mapIsDisplayed = false;

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.confirm_back_button_click_dialog_title))
                .setMessage(getResources().getString(R.string.confirm_back_button_click_dialog_message))
                .setPositiveButton(getResources().getString(R.string.general_yes_option), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Set result to RESULT_CANCELED so that the activity who opened the current SpotFormActivity knows that nothing was changed in the dataset
                        finishSuccessful(RESULT_CANCELED);
                    }

                })
                .setNegativeButton(getResources().getString(R.string.general_no_option), null)
                .show();

    }

    protected void showLocation() {
        // Check if user has granted location permission
        if (!locationServices.areLocationPermissionsGranted()) {
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
            enableLocation(true);
    }

    /**
     * Represents a geographical location.
     * <p>
     * protected Location mCurrentLocation;
     * boolean wasFirstLocationReceived = false;
     */

    private void enableLocation(boolean enabled) {
        // Enable or disable the location layer on the map
        mapboxMap.setMyLocationEnabled(enabled);
    }

    private void addPinToCenter() {
        if (!mapIsDisplayed)
            return;

        Context context = new ContextThemeWrapper(getBaseContext(), R.style.Theme_Base_NoActionBar);
        IconFactory iconFactory = IconFactory.getInstance(SpotFormActivity.this);
        //DrawableCompat.setTint(iconDrawable, Color.WHITE);
        FloatingActionButton img = new FloatingActionButton(context);
        img.setImageResource(R.drawable.ic_target_location);


        dropPinView = new ImageView(this);
        dropPinView.setImageDrawable(img.getDrawable());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        dropPinView.setLayoutParams(params);
        mapView.addView(dropPinView);
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

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
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
                displayAddressOutput();
            }
            //----END: Part related to reverse geocoding
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

    private void updateUI() {
        try {
            // If user is currently waiting for a ride at the current spot, who him the Evaluate form. If he is not,
            // that means he's saving a new spot so we need to show him the Basic form instead.
            spot_form_basic.setVisibility(View.GONE);
            spot_form_evaluate.setVisibility(View.GONE);

            if (mFormType == FormType.Unknown) {
                mSaveButton.setEnabled(false);
                showErrorAlert(getResources().getString(R.string.general_error_dialog_title), "Please try opening your spot again.");
            }

            //Show delete button when the spot is been edited and it's not of type Evaluate
            if ((mCurrentSpot.getId() != null && mCurrentSpot.getId() > 0) && mFormType != FormType.Evaluate) {
                mDeleteButton.setVisibility(View.VISIBLE);
                is_destination_check_box.setVisibility(View.VISIBLE);
            } else {
                mDeleteButton.setVisibility(View.GONE);
                is_destination_check_box.setVisibility(View.GONE);
            }

            if (mFormType == FormType.Evaluate || mFormType == FormType.All) {
                spot_form_evaluate.setVisibility(View.VISIBLE);

                Integer h = 0;
                if (mCurrentSpot.getHitchability() != null)
                    h = mCurrentSpot.getHitchability();
                hitchability_ratingbar.setRating(findTheOpposit(h));
            } else if (mFormType == FormType.Destination)
                spot_form_evaluate.setVisibility(View.GONE);


            if (mFormType == FormType.Basic || mFormType == FormType.Destination || mFormType == FormType.All) {
                spot_form_basic.setVisibility(View.VISIBLE);


                if ((mFormType == FormType.Basic) &&
                        (mCurrentSpot.getGpsResolved() == null || !mCurrentSpot.getGpsResolved())) // || mFormType == FormType.Destination
                    fetchAddressButtonHandler(null);
            }

            if (mFormType == FormType.All)
                attempt_result_panel.setVisibility(View.VISIBLE);
            else
                attempt_result_panel.setVisibility(View.GONE);

            Date spotStartDT = new Date();
            if (mCurrentSpot.getStartDateTime() != null)
                spotStartDT = mCurrentSpot.getStartDateTime();
            SetDateTime(date_datepicker, time_timepicker, spotStartDT);

            //If mFormType == FormType.Destination || mFormType == FormType.All this means the spot is a destination or it was already evaluated and now it's being edited.
            if (mFormType == FormType.Destination || mFormType == FormType.All) {
                if (mCurrentSpot.getNote() != null)
                    note_edittext.setText(mCurrentSpot.getNote());

                if (mFormType == FormType.All && mCurrentSpot.getWaitingTime() != null) {
                    String val = mCurrentSpot.getWaitingTime().toString();
                    waiting_time_edittext.setText(val);
                }

                if (mFormType == FormType.Destination) {
                    is_destination_check_box.setChecked(true);
                    hitchability_options.setVisibility(View.GONE);
                } else {
                    is_destination_check_box.setChecked(false);
                    hitchability_options.setVisibility(View.VISIBLE);
                }

                is_destination_check_box.setEnabled(false);


                if (mFormType == FormType.All) {
                    if (mCurrentSpot.getAttemptResult() != null && mCurrentSpot.getAttemptResult() >= 0 &&
                            mCurrentSpot.getAttemptResult() < attempt_results_spinner.getCount())
                        attempt_results_spinner.setSelection(mCurrentSpot.getAttemptResult());

                    form_title.setText(getResources().getString(R.string.spot_form_title_edit));
                } else {
                    form_title.setText(getResources().getString(R.string.arrived_button_text));
                }
            } else if (mFormType == FormType.Evaluate) {
                DateTime date = new DateTime(mCurrentSpot.getStartDateTime());
                Integer minutes = Minutes.minutesBetween(date, DateTime.now()).getMinutes();
                waiting_time_edittext.setText(minutes.toString());

                mFetchAddressButton.setVisibility(View.GONE);


                if (mCurrentSpot.getAttemptResult() != null && mCurrentSpot.getAttemptResult() != Constants.ATTEMPT_RESULT_UNKNOWN
                        && mCurrentSpot.getAttemptResult() < attempt_results_spinner.getCount()) {
                    attempt_results_spinner.setSelection(mCurrentSpot.getAttemptResult());

                    switch (mCurrentSpot.getAttemptResult()) {
                        case Constants.ATTEMPT_RESULT_GOT_A_RIDE:
                            form_title.setText(getResources().getString(R.string.got_a_ride_button_text));
                            attempt_result_panel.setVisibility(View.GONE);
                            break;
                        case Constants.ATTEMPT_RESULT_TOOK_A_BREAK:
                            form_title.setText(getResources().getString(R.string.break_button_text));
                            attempt_result_panel.setVisibility(View.GONE);
                            break;
                        default:
                            //Should actually never fall in this case!
                            form_title.setText(getResources().getString(R.string.spot_form_title_evaluate));
                            attempt_result_panel.setVisibility(View.VISIBLE);
                            break;
                    }
                } else {
                    form_title.setText(getResources().getString(R.string.spot_form_title_evaluate));
                    attempt_result_panel.setVisibility(View.VISIBLE);
                }
            } else if (mFormType == FormType.Basic) {
                form_title.setText(getResources().getString(R.string.save_spot_button_text));
            }

        } catch (Exception ex) {
            Log.e(TAG, "updateUI", ex);
            showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message), ex.getMessage()));
        }
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

    public void saveButtonHandler(View view) {
        try {
            if (mFormType == FormType.Basic || mFormType == FormType.Destination || mFormType == FormType.All) {
                if (locationManuallyChanged) {
                    if (mapboxMap.getCameraPosition() == null || mapboxMap.getCameraPosition().target == null) {
                        showErrorAlert(getResources().getString(R.string.save_spot_button_text), getResources().getString(R.string.save_spot_error_map_not_loaded));
                        return;
                    } else {
                        LatLng selectedLocation = mapboxMap.getCameraPosition().target;

                        mCurrentSpot.setLatitude(selectedLocation.getLatitude());
                        mCurrentSpot.setLongitude(selectedLocation.getLongitude());
                    }
                }

                mCurrentSpot.setNote(note_edittext.getText().toString());

                if (is_destination_check_box.isChecked()) {
                    mCurrentSpot.setIsDestination(true);
                    mCurrentSpot.setHitchability(0);
                    mCurrentSpot.setIsWaitingForARide(false);
                } else {
                    mCurrentSpot.setIsDestination(false);
                    mCurrentSpot.setHitchability(findTheOpposit(Math.round(hitchability_ratingbar.getRating())));
                    if (mFormType == FormType.Basic)
                        mCurrentSpot.setIsWaitingForARide(true);
                    else if (mFormType == FormType.Destination)
                        mCurrentSpot.setIsWaitingForARide(false);
                }
            }
            if (mFormType == FormType.Evaluate || mFormType == FormType.All) {
                String vals = waiting_time_edittext.getText().toString();
                if (!vals.isEmpty())
                    mCurrentSpot.setWaitingTime(Integer.parseInt(vals));
                mCurrentSpot.setAttemptResult(attempt_results_spinner.getSelectedItemPosition());
                mCurrentSpot.setHitchability(findTheOpposit(Math.round(hitchability_ratingbar.getRating())));

                if (mFormType == FormType.Evaluate)
                    mCurrentSpot.setIsWaitingForARide(false);
            }

            DateTime dateTime = GetDateTime(date_datepicker, time_timepicker);
            mCurrentSpot.setStartDateTime(dateTime.toDate());


            if (mCurrentSpot.getLatitude() == null || mCurrentSpot.getLongitude() == null) {
                showErrorAlert(getResources().getString(R.string.save_spot_button_text), getResources().getString(R.string.save_spot_error_coordinate_not_informed_error_message));
                return;
            }

        } catch (Exception ex) {
            Log.e(TAG, "saveButtonHandler", ex);
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
                        Toast.makeText(getApplicationContext(), R.string.spot_saved_successfuly, Toast.LENGTH_LONG).show();

                        int result = RESULT_OBJECT_ADDED;
                        if (mFormType == FormType.Evaluate || mFormType == FormType.All)
                            result = RESULT_OBJECT_EDITED;

                        finishSuccessful(result);
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
                                        finishSuccessful(RESULT_OBJECT_DELETED);
                                    }
                                });
                            }
                        }.start();
                    }

                })
                .setNegativeButton(getResources().getString(R.string.spot_form_delete_dialog_no_option), null)
                .show();
    }

    private void finishSuccessful(int result) {
        Intent intent = new Intent();

        //If action was canceled means that nothing changed in the object and therefore we don't need to use processing time serializing the object here
        if (result != RESULT_CANCELED) {
            Bundle conData = new Bundle();
            conData.putString(Constants.SPOT_BUNDLE_EXTRA_ID_KEY, mCurrentSpot.getId().toString());
            conData.putSerializable(Constants.SPOT_BUNDLE_EXTRA_KEY, mCurrentSpot);

            intent.putExtras(conData);
        }

        //Set result so that the activity who opened the current SpotFormActivity knows that the dataset was changed and it should make the necessary updates on the UI
        setResult(result, intent);

        finish();
    }

    public void moreOptionsButtonHandler(View view) {
        if (spot_form_more_options.isShown())
            spot_form_more_options.setVisibility(View.GONE);
        else
            spot_form_more_options.setVisibility(View.VISIBLE);

    }

    public void isDestinationHandleChecked(View view) {
        if (is_destination_check_box.isChecked()) {
            hitchability_options.setVisibility(View.GONE);
            spot_form_evaluate.setVisibility(View.GONE);
        } else {
            hitchability_options.setVisibility(View.VISIBLE);

            if (mFormType == FormType.Destination || mFormType == FormType.All)
                spot_form_evaluate.setVisibility(View.VISIBLE);
        }
    }

    public void SetDateTime(DatePicker datePicker, TimePicker timePicker, Date date) {
        DateTime dateTime = new DateTime(date);
        datePicker.updateDate(dateTime.getYear(), dateTime.getMonthOfYear() - 1, dateTime.getDayOfMonth()); // Must always subtract 1 here as DatePicker month is 0 based

        if (Build.VERSION.SDK_INT >= 23) {
            timePicker.setHour(dateTime.getHourOfDay());
            timePicker.setMinute(dateTime.getMinuteOfHour());
        } else {
            timePicker.setCurrentHour(dateTime.getHourOfDay());
            timePicker.setCurrentMinute(dateTime.getMinuteOfHour());
        }
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


        if (mapIsDisplayed)
            mapView.onSaveInstanceState(savedInstanceState);

        super.onSaveInstanceState(savedInstanceState);
    }


    //----BEGIN: Part related to reverse geocoding


    /**
     * Runs when user clicks the Fetch Address button. Starts the service to fetch the address if
     * GoogleApiClient is connected.
     */
    public void fetchAddressButtonHandler(View view) {
        if (!mapIsDisplayed || mapboxMap == null)
            return;

        LatLng pinPosition = mapboxMap.getCameraPosition().target;

        // We only start the service to fetch the address if GoogleApiClient is connected.
        if (pinPosition == null)
            return;

        startIntentService(new MyLocation(pinPosition.getLatitude(), pinPosition.getLongitude()));

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

        mLocationAddressTextView.setText(getString((Spot) mCurrentSpot));

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
            Log.w("getString", "Err msg: " + ex.getMessage());
        }
        return spotLoc;
    }


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
            Log.w("spotLocationToString", "Err msg: " + ex.getMessage());

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
