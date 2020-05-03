package com.myhitchhikingspots;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.github.florent37.viewtooltip.ViewTooltip;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin;
import com.myhitchhikingspots.adapters.CommentsListViewAdapter;
import com.myhitchhikingspots.interfaces.FirstLocationUpdateListener;
import com.myhitchhikingspots.model.MyLocation;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.LocationUpdatesCallback;
import com.myhitchhikingspots.utilities.Utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.List;

import hitchwikiMapsSDK.classes.APICallCompletionListener;
import hitchwikiMapsSDK.classes.APIConstants;
import hitchwikiMapsSDK.classes.ApiManager;
import hitchwikiMapsSDK.entities.Error;
import hitchwikiMapsSDK.entities.PlaceInfoComplete;
import hitchwikiMapsSDK.entities.PlaceInfoCompleteComment;
import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

/*
      A spot is just a coordinate and some extra data. Currently we allow 6 types of spots (listed below).
      The logic of the checkboxes should allow the user to save a spot as:
          got a ride -            is a hitchhiking spot;       x                          is not a destination
          took a break -          is a hitchhiking spot;       x                          is not a destination
          other -                 is not a hitchhiking spot;   x                          is not a destination

          waiting spot -          is a hitchhiking spot;       is part of a route;        is not a destination
          single spot -           is a hitchhiking spot;       is not part of a route;    is not a destination

          got off here -          is not a hitchhiking spot;  is part of a route;         x
          destination -           is not a hitchhiking spot;  is part of a route;         is a destination

      Additionally, the names used should be understood as follow:
          is NOT part of a route = is single spot
          is NOT a hitchhiking spot = is other type of spot
*/
public class SpotFormActivity extends AppCompatActivity implements RatingBar.OnRatingBarChangeListener, OnMapReadyCallback,
        View.OnClickListener, CompoundButton.OnCheckedChangeListener, PermissionsListener, FirstLocationUpdateListener {


    private Button mSaveButton, mDeleteButton;
    private Button mViewMapButton; //mNewSpotButton
    private EditText note_edittext, waiting_time_edittext;
    private DatePicker date_datepicker;
    private TimePicker time_timepicker;
    private Spot mCurrentSpot;
    private CheckBox is_part_of_a_route_check_box, is_destination_check_box, is_hitchhiking_spot_check_box, is_not_hitchhiked_from_here_check_box;
    private TextView hitchabilityLabel, selected_date;
    private ScrollView spot_form_evaluate;
    private LinearLayout spot_form_more_options, hitchability_options;
    private RatingBar hitchability_ratingbar;
    private BottomNavigationView menu_bottom;
    SharedPreferences prefs;

    private BottomNavigationItemView evaluate_menuitem;

    protected static final String TAG = "spot-form-activity";
    protected final static String CURRENT_SPOT_KEY = "current-spot-key";

    //----BEGIN: Part related to reverse geocoding
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";
    protected static final String SELECTED_ATTEMPT_RESULT_KEY = "selected-attempt-result";
    protected static final String SNACKBAR_SHOWED_KEY = "snackbar-showed";
    protected static final String BUTTONS_PANEL_IS_VISIBLE_KEY = "buttons-panel-is-visible";
    protected static final String LAST_SELECTED_TAB_ID_KEY = "last-selected-tab";
    protected static final String REFRESH_DATETIME_ALERT_SHOWN_KEY = "PREF_REFRESH_DATETIME_ALERT_SHOWN_KEY";

    private Button placeButtonComments;

    Toast waiting_GPS_update;

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
    private Style style;
    private static final int PERMISSIONS_LOCATION = 0;
    private ImageView dropPinView;

    private View coordinatorLayout, spot_form_basic;
    private FloatingActionButton fabLocateUser, fabZoomIn, fabZoomOut;

    private NestedScrollView scrollView;
    BottomSheetBehavior mBottomSheetBehavior;
    public AppCompatImageButton mGotARideButton, mTookABreakButton;

    boolean shouldGoBackToPreviousActivity, shouldShowButtonsPanel, refreshDatetimeAlertDialogWasShown;
    int lastSelectedTab = -1;

    LinearLayout panel_buttons, panel_info;
    TextView panel_map_not_displayed;
    RelativeLayout datePanel;
    MenuItem saveMenuItem;
    boolean wasSnackbarShown;
    Context context;
    Boolean shouldRetrieveDetailsFromHW = false;
    Boolean shouldMoveMapCameraToUserLocationOnMapLoad = false;
    double cameraZoomFromBundle = -1;

    // Variables needed to add the location engine
    private LocationEngine locationEngine;
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    // Variables needed to listen to location updates
    private LocationUpdatesCallback callback = new LocationUpdatesCallback(this);

    private PermissionsManager locationPermissionsManager;

    SpotFormViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spot_form_master_layout);

        //Set CompatVectorFromResourcesEnabled to true in order to be able to use ContextCompat.getDrawable
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        //Prevent keyboard to be shown when activity starts
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        context = this;
        coordinatorLayout = (View) findViewById(R.id.coordinatorLayout);

        prefs = getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        viewModel = new ViewModelProvider(this).get(SpotFormViewModel.class);

        // Set a Toolbar to replace the ActionBar.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //savedInstanceState will be not null when a screen is rotated, for example. But will be null when activity is first created
        if (savedInstanceState == null) {
            if (!wasSnackbarShown) {
                if (getIntent().getBooleanExtra(Constants.SHOULD_SHOW_SPOT_SAVED_SNACKBAR_KEY, false))
                    showViewMapSnackbar();
            }
            mCurrentSpot = (Spot) getIntent().getSerializableExtra(Constants.SPOT_BUNDLE_EXTRA_KEY);
            cameraZoomFromBundle = getIntent().getDoubleExtra(Constants.SPOT_BUNDLE_MAP_ZOOM_KEY, -1);
            shouldGoBackToPreviousActivity = getIntent().getBooleanExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, false);
            wasSnackbarShown = true;
            shouldShowButtonsPanel = getIntent().getBooleanExtra(Constants.SHOULD_SHOW_BUTTONS_KEY, false);
            shouldRetrieveDetailsFromHW = getIntent().getBooleanExtra(Constants.SHOULD_RETRIEVE_HITCHWIKI_DETAILS_KEY, false);

            if (mCurrentSpot != null && mCurrentSpot.getLongitude() == null || mCurrentSpot.getLatitude() == null)
                shouldMoveMapCameraToUserLocationOnMapLoad = true;
        } else
            updateValuesFromBundle(savedInstanceState);

        // If user is currently waiting for a ride at the current spot, show him the Evaluate form. If he is not,
        // that means he's saving a new spot so we need to show him the Create form instead.
        if (mCurrentSpot == null)
            mFormType = FormType.Unknown;
        else if (shouldRetrieveDetailsFromHW)
            mFormType = FormType.ReadOnly;
        else {
            // If Id greater than zero, this means the user is editing a spot that was already saved in the database. So show full form.
            if (mCurrentSpot.getId() != null && mCurrentSpot.getId() > 0) {
                if (isWaitingForARide())
                    mFormType = FormType.Evaluate;
                else
                    mFormType = FormType.Edit;
            } else
                mFormType = FormType.Create;
        }

        Crashlytics.setString("mFormType", mFormType.toString());

        /*if (mFormType == FormType.Unknown) {
            mCurrentSpot = new Spot();
            mCurrentSpot.setIsPartOfARoute(true);
            mCurrentSpot.setStartDateTime(new Date());
            mFormType = FormType.Create;
        }*/

        if (mFormType == FormType.Unknown) {
            //Always that SpotFormActivity is called a Spot must have been provided to it.
            // This way we know a little about what the user wants to add/edit - single spot, destination spot, maybe some previously informed coordinates, etc
            showErrorAlert(getString(R.string.general_error_dialog_title), "Some data were missing. Please navigate back and try again.");
            Crashlytics.log(Log.WARN, TAG, "");
            return;
        } else if (mFormType == FormType.Edit) {
            //Prevent user from editing a spot if he's currently waiting for a ride somewhere else.
            Spot s = viewModel.getCurrentSpot().getValue();

            if (s != null &&
                    s.getIsWaitingForARide() != null && s.getIsWaitingForARide() &&
                    s.getId().equals(mCurrentSpot.getId())) {
                String actionRequiredText = getString(R.string.evaluate_running_spot_required, getString(R.string.got_a_ride_button_text), getString(R.string.break_button_text));
                showErrorAlert(getString(R.string.general_error_dialog_title),
                        getString(R.string.spot_form_not_allowed_to_edit) + "\n" + actionRequiredText);
                return;
            }
        }

        mSaveButton = (Button) findViewById(R.id.save_button);
        mDeleteButton = (Button) findViewById(R.id.delete_button);
        placeButtonComments = (Button) findViewById(R.id.placeButtonComments);
        mViewMapButton = (Button) findViewById(R.id.view_map_button);
        note_edittext = (EditText) findViewById(R.id.spot_form_note_edittext);
        date_datepicker = (DatePicker) findViewById(R.id.spot_form_date_datepicker);
        time_timepicker = (TimePicker) findViewById(R.id.spot_form_time_timepicker);
        waiting_time_edittext = (EditText) findViewById(R.id.spot_form_waiting_time_edittext);
        spot_form_more_options = (LinearLayout) findViewById(R.id.save_spot_form_more_options);
        is_part_of_a_route_check_box = (CheckBox) findViewById(R.id.save_spot_form_is_part_of_route_check_box);
        is_destination_check_box = (CheckBox) findViewById(R.id.save_spot_form_is_destination_check_box);
        is_hitchhiking_spot_check_box = (CheckBox) findViewById(R.id.save_spot_form_is_hitchhiking_spot_check_box);
        is_not_hitchhiked_from_here_check_box = (CheckBox) findViewById(R.id.save_spot_form_is_not_hitchhiked_from_here_check_box);
        hitchability_ratingbar = (RatingBar) findViewById(R.id.spot_form_hitchability_ratingbar);
        hitchability_options = (LinearLayout) findViewById(R.id.save_spot_form_hitchability_options);
        hitchabilityLabel = (TextView) findViewById(R.id.spot_form_hitchability_selectedvalue);
        selected_date = (TextView) findViewById(R.id.spot_form_selected_date);

        spot_form_basic = (View) findViewById(R.id.save_spot_form_basic);
        spot_form_evaluate = (ScrollView) findViewById(R.id.save_spot_form_evaluate);
        panel_buttons = (LinearLayout) findViewById(R.id.panel_buttons);
        panel_info = (LinearLayout) findViewById(R.id.panel_info);
        panel_map_not_displayed = (TextView) findViewById(R.id.save_spot_map_not_displayed);
        datePanel = (RelativeLayout) findViewById(R.id.date_panel);

        menu_bottom = (BottomNavigationView) findViewById(R.id.bottom_navigation);

        evaluate_menuitem = (BottomNavigationItemView) findViewById(R.id.action_evaluate);

        scrollView = (NestedScrollView) findViewById(R.id.spot_form_scrollview);

        mBottomSheetBehavior = BottomSheetBehavior.from(scrollView);

        mGotARideButton = (AppCompatImageButton) findViewById(R.id.got_a_ride_button);
        mTookABreakButton = (AppCompatImageButton) findViewById(R.id.break_button);
        mGotARideButton.setOnClickListener(this);
        mTookABreakButton.setOnClickListener(this);

        mViewMapButton.setText(String.format(getString(R.string.action_button_label), getString(R.string.view_map_button_label)));

        //----BEGIN: Part related to reverse geocoding
        mResultReceiver = new AddressResultReceiver(new Handler());

        mLocationAddressTextView = (TextView) findViewById(R.id.location_address_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mFetchAddressButton = (Button) findViewById(R.id.fetch_address_button);

        // Set defaults, then update using values stored in the Bundle.
        mAddressRequested = false;
        mAddressOutput = null;

        updateLocationWidgets();
        //----END: Part related to reverse geocoding

        hitchability_ratingbar.setNumStars(Constants.hitchabilityNumOfOptions);
        hitchability_ratingbar.setStepSize(1);
        hitchability_ratingbar.setOnRatingBarChangeListener(this);

        mapView = (MapView) findViewById(R.id.mapview2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        updateMapVisibility();


        ((FloatingActionButton) findViewById(R.id.fab_open_external_map_app)).setOnClickListener(view -> {
            if (mapboxMap == null || style == null || !style.isFullyLoaded())
                return;

            LatLng selectedLocation = mapboxMap.getCameraPosition().target;

            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("geo:" + selectedLocation.getLatitude() + "," + selectedLocation.getLongitude() + "?q=" + selectedLocation.getLatitude() + "," + selectedLocation.getLongitude()));
            startActivity(intent);
        });


        fabLocateUser = (FloatingActionButton) findViewById(R.id.fab_locate_user);
        fabLocateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null && style != null && style.isFullyLoaded()) {
                    if (locateUserTooltip != null && locateUserTooltip.isShown())
                        locateUserTooltip.closeNow();

                    enableLocationLayer(style);

                    callback.moveMapCameraToNextLocationReceived();
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


        note_edittext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandBottomSheet();
            }
        });


        menu_bottom.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                lastSelectedTab = item.getItemId();
                switch (lastSelectedTab) {
                    case R.id.action_basic:
                        spot_form_basic.setVisibility(View.VISIBLE);
                        spot_form_evaluate.setVisibility(View.GONE);

                        hideKeyboard();
                        break;
                    case R.id.action_evaluate:
                        if (mFormType == FormType.Create || !is_hitchhiking_spot_check_box.isChecked()) {
                            //Show message to let the user know that Evaluate tab will be enabled after he clicks the Save button
                            Toast.makeText(getBaseContext(), String.format(getString(R.string.spot_form_enable_evaluate_tab_message),
                                    getString(R.string.spot_form_bottommenu_evaluate_tile)),
                                    Toast.LENGTH_SHORT).show();

                            return false;
                        } else {
                            spot_form_basic.setVisibility(View.GONE);
                            spot_form_evaluate.setVisibility(View.VISIBLE);
                        }
                        break;
                }
                return true;
            }
        });

        placeButtonComments.setVisibility(View.GONE);

        placeButtonComments.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {


                //if number of comments is 0, we won't open comments dialog with listview as there's
                //nothing to show, but will only inform user that there are no comments
                if (placeWithCompleteDetails.getComments_count().contentEquals("0")) {
                    showErrorAlert(getString(R.string.spot_form_comments_title), getString(R.string.spot_form_comments_empty_list));
                } else {
                    //If dialog was already created, we can just show it again. The list of comments shown in the dialog is still the same as of when the dialog was created.
                    if (dialog == null) {
                        //populate arrayList of comments first, only if there are comments (comment count not 0)
                        ArrayList<PlaceInfoCompleteComment> arrayListOfComments = new ArrayList<PlaceInfoCompleteComment>();

                        //Add comments
                        for (int i = 0; i < placeWithCompleteDetails.getComments().length; i++) {
                            arrayListOfComments.add(placeWithCompleteDetails.getComments()[i]);
                        }

                        showCommentsDialog(arrayListOfComments);
                    } else
                        dialog.show();
                }
            }
        });

        /*if (shouldRetrieveDetailsFromHW)
            note_edittext.setHint(getString(R.string.spot_form_note_hint));
        else
            note_edittext.setHint(getString(R.string.spot_form_add_note_hint));*/


        //Load UI - Make sure all the relevant listeners were set BEFORE updateUI() is called
        updateUI();

        if (shouldRetrieveDetailsFromHW) {
            if (!Utils.isNetworkAvailable(this)) {
                //panel_buttons.setVisibility(View.GONE);
                //panel_info.setVisibility(View.GONE);
                showErrorAlert(getString(R.string.general_offline_mode_label), getString(R.string.spot_form_unable_to_download_details, getString(R.string.spot_form_bottommenu_evaluate_tile)));
            } else {
                //we use getSnippet() for id because original hitchwiki id is stored as snippet in our markers
                //this avoids extending Marker class to add additional parameter for point id
                //and snippet will never be used as we have custom info window and not info balloon window
                if (taskThatRetrievesCompleteDetails != null) {
                    //check if there's this task already running (for previous marker), if so, cancel it
                    if (taskThatRetrievesCompleteDetails.getStatus() == AsyncTask.Status.PENDING ||
                            taskThatRetrievesCompleteDetails.getStatus() == AsyncTask.Status.RUNNING) {
                        taskThatRetrievesCompleteDetails.cancel(true);
                    }
                }

                //execute new asyncTask that will retrieve marker details for clickedMarker
                taskThatRetrievesCompleteDetails = new retrievePlaceDetailsAsyncTask().execute(mCurrentSpot.getId().toString());
            }
        }
    }

    private void expandBottomSheet() {
        //Expand mBottomSheetBehavior
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        //Hide locateUserTooltip so that it doesn't cover note_edittext
        if (locateUserTooltip != null && locateUserTooltip.isShown())
            locateUserTooltip.closeNow();
    }

    boolean isWaitingForARide() {
        return (mCurrentSpot != null && mCurrentSpot.getIsWaitingForARide() != null) ?
                mCurrentSpot.getIsWaitingForARide() : false;
    }

    void updateMapVisibility() {
        if (prefs.getBoolean(Constants.PREFS_MAPBOX_WAS_EVER_LOADED, false)) {
            //mapView.setVisibility(View.VISIBLE);
            panel_map_not_displayed.setVisibility(View.GONE);
        } else {
            //mapView.setVisibility(View.GONE);
            panel_map_not_displayed.setVisibility(View.VISIBLE);
            //For some strange reason the note field is getting focus when map is not loaded and that causes the panel to expand, hiding the message.
            //hideKeyboard();
        }
    }

    protected void showErrorAlert(String title, String msg) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.general_ok_option), null)
                .show();
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

    void makeMapCameraFollowGPSUpdates(@RenderMode.Mode int compassRenderMode) {
        LocationComponent locationComponent = mapboxMap.getLocationComponent();

        if (locationComponent.isLocationComponentActivated()) {
            // Make map display the user's location, but the map camera shouldn't be moved to such location yet.
            locationComponent.setCameraMode(CameraMode.TRACKING_GPS_NORTH);

            //Show an arrow considering the compass of the device.
            locationComponent.setRenderMode(compassRenderMode);
        }
    }

    void makeMapCameraStopFollowGPSUpdates(@RenderMode.Mode int compassRenderMode) {
        LocationComponent locationComponent = mapboxMap.getLocationComponent();

        //Make sure location component has been activated, otherwise using any of its methods will throw an exception.
        if (locationComponent.isLocationComponentActivated()) {
            //Map camera should stop following gps updates
            locationComponent.setCameraMode(CameraMode.NONE);

            //Stop showing an arrow considering the compass of the device.
            locationComponent.setRenderMode(compassRenderMode);
        }
    }

    void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        note_edittext.clearFocus();
        waiting_time_edittext.clearFocus();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.spot_form_menu, menu);
        saveMenuItem = menu.findItem(R.id.action_save);
        updateSaveButtonState();
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
        if (mFormType != FormType.Edit && mFormType != FormType.ReadOnly && is_part_of_a_route_check_box.isChecked())
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

    private boolean isFirstTimeCameraIsIdle = true;

    @Override
    public void onMapReady(final MapboxMap mapboxMap) {
        prefs.edit().putBoolean(Constants.PREFS_MAPBOX_WAS_EVER_LOADED, true).commit();

        this.mapboxMap = mapboxMap;

        this.mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments.
            SpotFormActivity.this.style = style;
            updateMapVisibility();

            this.mapboxMap.getUiSettings().setCompassEnabled(true);
            this.mapboxMap.getUiSettings().setLogoEnabled(false);
            this.mapboxMap.getUiSettings().setAttributionEnabled(false);
            this.mapboxMap.getUiSettings().setTiltGesturesEnabled(false);

            this.mapboxMap.addOnCameraIdleListener(() -> {
                //If spot is being edited and its address has already been fetched (when an address is fetched, GpsResolved is set to true),
                // then avoid requesting to fetch the address of the same location again by ignoring the first time the camera gets idle.
                if (mCurrentSpot != null && mCurrentSpot.getGpsResolved() != null && mCurrentSpot.getGpsResolved() && isFirstTimeCameraIsIdle) {
                    isFirstTimeCameraIsIdle = false;
                    return;
                }

                collapseBottomSheet();
                hideKeyboard();

                //As the map camera was moved, we should clear the previous address data
                mAddressOutput = null;
                displayAddressOutput();

                if (Utils.isNetworkAvailable(this))
                    fetchAddressButtonHandler(null);

                //Stop showing an arrow considering the compass of the device.
                if (this.mapboxMap.getLocationComponent().isLocationComponentActivated())
                    this.mapboxMap.getLocationComponent().setRenderMode(RenderMode.NORMAL);
            });

            if (style.isFullyLoaded()) {
                LocalizationPlugin localizationPlugin = new LocalizationPlugin(mapView, mapboxMap, style);

                try {
                    localizationPlugin.matchMapLanguageWithDeviceDefault();
                } catch (RuntimeException exception) {
                    Crashlytics.logException(exception);
                }

                enableLocationLayer(style);

                mapCameraWasMoved = moveCameraToSpotLocation(mCurrentSpot);

                //Set listeners only after requested camera position is reached
                if (!(mapCameraWasMoved && !shouldMoveMapCameraToUserLocationOnMapLoad)) {
                    shouldMoveMapCameraToUserLocationOnMapLoad = false;
                    callback.moveMapCameraToNextLocationReceived();
                }

                if (!shouldRetrieveDetailsFromHW && mFormType == FormType.Create && !shouldShowButtonsPanel)
                    highlightCheckboxes();

                addPinToCenter();
            }
        });
    }

    boolean mapCameraWasMoved = false;

    /**
     * Move the map camera to the given position with zoom Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT
     *
     * @param latLng Target location to change to
     */
    private void moveCamera(LatLng latLng) {
        moveCamera(latLng, Constants.KEEP_ZOOM_LEVEL);
    }

    /**
     * Move the map camera to the given position
     *
     * @param latLng Target location to change to
     * @param zoom   Zoom level to change to
     */
    private void moveCamera(LatLng latLng, long zoom) {
        if (latLng != null) {
            if (mapboxMap == null)
                Crashlytics.log(Log.INFO, TAG, "For some reason map was not loaded, therefore mapboxMap.moveCamera() was skipped to avoid crash. Shouldn't the map be loaded at this point?");
            else {
                if (zoom == Constants.KEEP_ZOOM_LEVEL)
                    mapboxMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                else
                    mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
            }
        }
    }

    private Location tryGetLastKnownLocation() {
        if (mapboxMap == null)
            return null;

        LocationComponent locationComponent = mapboxMap.getLocationComponent();
        Location loc = null;
        try {
            //Make sure location component has been activated, otherwise using any method of it will throw an exception.
            if (locationComponent.isLocationComponentActivated())
                loc = locationComponent.getLastKnownLocation();
        } catch (SecurityException ex) {
        }
        return loc;
    }

    int FAVORITE_ZOOM_LEVEL_NOT_INFORMED = -1;

    @Override
    public void moveCameraToLastKnownLocation() {
        moveCameraToLastKnownLocation(FAVORITE_ZOOM_LEVEL_NOT_INFORMED);
    }

    /**
     * Move map camera to the last GPS location OR if it's not available,
     * we'll try to move the map camera to the location of the last saved spot.
     *
     * @param zoomLevel The zoom level that should be used or FAVORITE_ZOOM_LEVEL_NOT_INFORMED if we should use what we think could be the best zoom level.
     */
    @SuppressWarnings({"MissingPermission"})
    public void moveCameraToLastKnownLocation(int zoomLevel) {
        if (!style.isFullyLoaded())
            return;

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
            Spot lastAddedSpot = viewModel.getLastAddedRouteSpot(getContext());
            if (lastAddedSpot != null && lastAddedSpot.getLatitude() != null && lastAddedSpot.getLongitude() != null
                    && lastAddedSpot.getLatitude() != 0.0 && lastAddedSpot.getLongitude() != 0.0) {
                moveCameraPositionTo = new LatLng(lastAddedSpot.getLatitude(), lastAddedSpot.getLongitude());
            }
        }

        int bestZoomLevel = Constants.KEEP_ZOOM_LEVEL;

        //If current zoom level is default (world level)
        if (mapboxMap.getCameraPosition().zoom == mapboxMap.getMinZoomLevel())
            bestZoomLevel = Constants.ZOOM_TO_SEE_FARTHER_DISTANCE;

        if (moveCameraPositionTo != null)
            moveCamera(moveCameraPositionTo, bestZoomLevel);
    }

    /**
     * Move map camera to the given spot's location OR if it's not available,
     * we'll try to move the map camera to the last GPS location.
     *
     * @return True if the map camera was moved anywhere.
     */
    @SuppressWarnings({"MissingPermission"})
    boolean moveCameraToSpotLocation(Spot spot) {
        LatLng cameraPositionTo = null;
        int cameraZoomTo = Constants.KEEP_ZOOM_LEVEL;

        //Move camera manually
        if (spot != null && spot.getLatitude() != null && spot.getLatitude() != 0
                && spot.getLongitude() != null && spot.getLongitude() != 0) {
            //Set start position for map camera: set it to the current waiting spot
            cameraPositionTo = new LatLng(spot.getLatitude(), spot.getLongitude());

            if (mFormType == FormType.Create && cameraZoomFromBundle != -1)
                cameraZoomTo = (int) cameraZoomFromBundle;
            else
                cameraZoomTo = Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT;
        } else {
            Location loc = tryGetLastKnownLocation();

            if (loc != null) {
                cameraPositionTo = new LatLng(loc);
                cameraZoomTo = Constants.ZOOM_TO_SEE_CLOSE_TO_SPOT;
                //Boolean equals = cameraPositionTo.getLatitude() == cameraPositionTo2.getLatitude() && cameraPositionTo.getLongitude() == cameraPositionTo2.getLongitude();
                //Crashlytics.setBool("are equals", equals);
                //}
            } else {
                //Set start position for map camera: set it to the last spot saved
                Spot lastAddedSpot = viewModel.getLastAddedRouteSpot(getContext());
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
        }

        if (cameraPositionTo != null) {
            moveCamera(cameraPositionTo, cameraZoomTo);
            return true;
        }
        return false;
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

        mapView.onResume();

        //To avoid many alertdialogs been displayed when the screen is rotated many times, we're saving refreshDatetimeAlertDialogWasShown to a bundle in onSaveInstanceState and checking its value here
        if (!refreshDatetimeAlertDialogWasShown) {
            if (mFormType == FormType.Create && !shouldShowButtonsPanel) {
                DateTime dateTime = GetDateTime(date_datepicker, time_timepicker);
                final DateTime dateTimeNow = Utils.getLocalDateTimeNowAsUTC();
                int minutesPast = Minutes.minutesBetween(dateTime, dateTimeNow).getMinutes();

                if (minutesPast > 10) {
                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getString(R.string.refresh_datetime_dialog_title))
                            .setMessage(String.format(
                                    getString(R.string.refresh_datetime_dialog_message),
                                    minutesPast, Utils.dateTimeToString(dateTime), Utils.dateTimeToString(dateTimeNow)))
                            .setPositiveButton(getString(R.string.general_refresh_label), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    refreshDatetimeAlertDialogWasShown = true;
                                    SetDateTime(date_datepicker, time_timepicker, dateTimeNow);
                                }

                            })
                            .setNegativeButton(String.format(getString(R.string.general_thank_you_label), getString(R.string.general_no_option)), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    refreshDatetimeAlertDialogWasShown = true;
                                }
                            })
                            .show();
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mapView.onPause();

        dismissSnackbar();
        dismissProgressDialog();
        dismissCommetsDialog(null);
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
    public void onBackPressed() {
        if (saveMenuItem != null && saveMenuItem.isVisible() && saveMenuItem.isEnabled()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.confirm_back_button_click_dialog_title))
                    .setMessage(getResources().getString(R.string.confirm_back_button_click_dialog_message))
                    .setPositiveButton(getResources().getString(R.string.general_yes_option), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }

                    })
                    .setNegativeButton(getResources().getString(R.string.general_no_option), null)
                    .show();
        } else {
            finish();
        }
    }

    private void addPinToCenter() {
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
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, getString(R.string.spot_form_user_location_permission_not_granted), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        //As we request at least two different permissions (location and storage) for the users,
        //instead of handling the results for location permission here alone, we've opted to handle it within onRequestPermissionsResult.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            locationPermissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (style.isFullyLoaded()) {
                    enableLocationLayer(style);
                    callback.moveMapCameraToNextLocationReceived();
                }
            } else {
                Toast.makeText(this, getString(R.string.spot_form_user_location_permission_not_granted), Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressWarnings({"MissingPermission"})
    /**
     * Setup location component to display the user location on a map.
     * Map camera won't follow location updates by deafult.
     */
    private void setupLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            //Stop showing an arrow considering the compass of the device.
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();

        } else {
            if (locationPermissionsManager == null)
                locationPermissionsManager = new PermissionsManager(this);
            locationPermissionsManager.requestLocationPermissions(this);
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

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
    public Context getContext() {
        return this;
    }

    private void collapseBottomSheet() {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {

        hitchabilityLabel.setText(Utils.getRatingAsString(this, Math.round(rating)));
    }


    private enum FormType {
        Unknown,
        Create,
        Evaluate,
        ReadOnly,
        Edit
    }

    private FormType mFormType = FormType.Unknown;
    private boolean updateUIFirstCalled = false;

    private void updateUI() {
        try {
            // If user is currently waiting for a ride at the current spot, who him the Evaluate form. If he is not,
            // that means he's saving a new spot so we need to show him the Create form instead.

            attemptResult = Constants.ATTEMPT_RESULT_UNKNOWN;
            if (mCurrentSpot.getAttemptResult() != null)
                attemptResult = mCurrentSpot.getAttemptResult();
            updateAttemptResultButtonsState();

            updateToolbarTitle();

            if (shouldShowButtonsPanel) {
                panel_buttons.setVisibility(View.VISIBLE);
                panel_info.setVisibility(View.GONE);
            } else {
                panel_buttons.setVisibility(View.GONE);
                panel_info.setVisibility(View.VISIBLE);
            }

            spot_form_more_options.setVisibility(View.GONE);


            //Automatically calculate the waiting time if the spot is still on Evaluate phase (if calculating when editing a spot already evaluated it could mess the waiting time without the user expecting/noticing)
            //if (mFormType == FormType.Evaluate)
            //    calculateWaitingTime(null);

            if (mFormType == FormType.Unknown) {
                Crashlytics.logException(new Exception("mFormType is Unkonwn"));
                mSaveButton.setEnabled(false);
                showErrorAlert(getResources().getString(R.string.general_error_dialog_title), "Please try opening your spot again.");
            }

            //Show delete button when the spot is been edited
            updateDeleteButtonState();

            if (shouldRetrieveDetailsFromHW)
                findViewById(R.id.imageView4).setEnabled(false);

            // In Create mode, SetDateTime will be called when the user clicks in "New spot" button (newSpotButtonHandler)
            if (mFormType != FormType.Create || !shouldShowButtonsPanel) {
                DateTime spotStartDT = Utils.getLocalDateTimeNowAsUTC();
                if (mCurrentSpot.getStartDateTime() != null)
                    spotStartDT = mCurrentSpot.getStartDateTime();
                SetDateTime(date_datepicker, time_timepicker, spotStartDT);
            }

            //If mFormType is Evaluate or WaitingTime wasn't set, leave the waiting time field empty
            if (mFormType != FormType.Evaluate && mCurrentSpot.getWaitingTime() != null) {
                String val = mCurrentSpot.getWaitingTime().toString();
                waiting_time_edittext.setText(val);
            } else
                waiting_time_edittext.setText("");

            if (mFormType == FormType.ReadOnly)
                findViewById(R.id.spot_form_waiting_time_refresh_button).setVisibility(View.GONE);
            else
                findViewById(R.id.spot_form_waiting_time_refresh_button).setVisibility(View.VISIBLE);


            if (mCurrentSpot.getNote() != null)
                note_edittext.setText(mCurrentSpot.getNote());
            else
                note_edittext.setText("");

            Boolean isDestination = mCurrentSpot.getIsDestination() != null && mCurrentSpot.getIsDestination();
            Boolean isNotHitchhikedFromHere = mCurrentSpot.getIsNotHitchhikedFromHere() != null && mCurrentSpot.getIsNotHitchhikedFromHere();
            Boolean isPartOfARouteSpot = mCurrentSpot.getIsPartOfARoute() != null && mCurrentSpot.getIsPartOfARoute();
            Boolean isHitchhikingSpot = mCurrentSpot.getIsHitchhikingSpot() != null && mCurrentSpot.getIsHitchhikingSpot();

            updateCheckboxesStates(isDestination, isNotHitchhikedFromHere, isPartOfARouteSpot, isHitchhikingSpot);

            if (shouldRetrieveDetailsFromHW) {
                is_not_hitchhiked_from_here_check_box.setVisibility(View.GONE);
                is_destination_check_box.setVisibility(View.GONE);
                is_part_of_a_route_check_box.setVisibility(View.GONE);
                is_hitchhiking_spot_check_box.setVisibility(View.VISIBLE);
                is_hitchhiking_spot_check_box.setEnabled(false);
            } else
                updateCheckboxesStates();

            int h = 0;
            if (mCurrentSpot.getHitchability() != null) {
                //getHitchability() is always the position of the selected star on the ratingbar.
                if (mCurrentSpot.getHitchability() > hitchability_ratingbar.getNumStars() || mCurrentSpot.getHitchability() < 0) {
                    h = 0;
                    Crashlytics.setInt("mCurrentSpot.getHitchability", mCurrentSpot.getHitchability());
                    Crashlytics.setInt("hitchability_ratingbar.getNumStars", hitchability_ratingbar.getNumStars());
                    Crashlytics.log(Log.WARN, TAG, "The selected hitchability is smaller than 0 or bigger than the number of stars in the rating bar. Nothing was selected, but this is a very unexpected bug that deserves a close check.");
                } else
                    h = mCurrentSpot.getHitchability();
            }

            hitchability_ratingbar.setRating(Utils.findTheOpposite(h));
            if (h == 0)
                hitchabilityLabel.setText("");
            else
                hitchabilityLabel.setText(Utils.getRatingAsString(this, Utils.findTheOpposite(h)));


            updateSpotLocationOnUI();

            updateSelectedTab();

            updateSaveButtonState();

        } catch (Exception ex) {
            //setTitle(getResources().getString(R.string.spot_form_bottommenu_map_tile));
            Crashlytics.logException(ex);
            showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message), ex.getMessage()));
        }

        updateUIFirstCalled = true;
    }

    private void updateToolbarTitle() {
        String title = "";
        if (mFormType == FormType.Create)
            title = getResources().getString(R.string.save_spot_button_text);
        else if (mFormType == FormType.Edit)
            title = getResources().getString(R.string.spot_form_title_edit);
        else if (mFormType == FormType.ReadOnly) {
            if (shouldRetrieveDetailsFromHW)
                title = "Hitchwiki";
            else
                title = "Spot";
        } else
            title = getResources().getString(R.string.spot_form_title_evaluate);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(title.toUpperCase());
    }

    private void updateDeleteButtonState() {
        //Show delete button when the spot is been edited
        if (mFormType == FormType.Evaluate || mFormType == FormType.Edit)
            mDeleteButton.setVisibility(View.VISIBLE);
        else
            mDeleteButton.setVisibility(View.GONE);
    }

    private void updateSaveButtonState() {
        Boolean showSaveButton = true;
        Boolean enableSaveButton = panel_buttons.getVisibility() != View.VISIBLE;

        if (mFormType == FormType.ReadOnly)
            showSaveButton = false;
        else if (!shouldShowButtonsPanel)
            enableSaveButton = true;

        if (saveMenuItem != null) {
            saveMenuItem.setVisible(showSaveButton);
            saveMenuItem.setEnabled(enableSaveButton);
        }

        //Hide mSaveButton if panel_buttons is visible
        if (panel_buttons.getVisibility() == View.VISIBLE || !showSaveButton)
            mSaveButton.setVisibility(View.GONE);
        else
            mSaveButton.setVisibility(View.VISIBLE);
    }

    private void updateSelectedTab() {
        if (mFormType == FormType.Edit || mFormType == FormType.Evaluate) {
            if (lastSelectedTab == -1) {
                if (mFormType == FormType.Evaluate && is_hitchhiking_spot_check_box.isChecked())
                    lastSelectedTab = R.id.action_evaluate;
                else
                    lastSelectedTab = R.id.action_basic;
            }
        } else {
            if (lastSelectedTab == -1)
                lastSelectedTab = R.id.action_basic;
        }

        menu_bottom.setSelectedItemId(lastSelectedTab);
    }


    private void updateSpotLocationOnUI() {
        String address = "";

        if (mCurrentSpot != null && mCurrentSpot.getGpsResolved() != null && mCurrentSpot.getGpsResolved())
            address = spotLocationToString(mCurrentSpot).trim();
        else
            address = getString(R.string.spot_form_location_selected_label);

        mLocationAddressTextView.setText(address);
    }

    public void calculateWaitingTime(View view) {
        DateTime date = GetDateTime(date_datepicker, time_timepicker);
        Integer minutes = Minutes.minutesBetween(date, Utils.getLocalDateTimeNowAsUTC()).getMinutes();
        waiting_time_edittext.setText(minutes.toString());
        Toast.makeText(this, getResources().getString(R.string.spot_form_waiting_time_label) + ": " + Utils.getWaitingTimeAsString(minutes, this), Toast.LENGTH_LONG).show();
    }


    MaterialShowcaseView lastShowCaseDisplayed;

    /**
     * Display showcase highlighting the Comment button.
     * The showcase will never be displayed again after the first time this method is called, unless .resetSingleUse() is purposely called.
     **/
    void highlightCommentsButton() {
        lastShowCaseDisplayed = new MaterialShowcaseView.Builder(this)
                .setTarget(placeButtonComments)
                .setDelay(1000) // a second between each showcase view
                .setDismissText(getString(R.string.general_showCase_button))
                .setContentText(getString(R.string.spot_form_read_comments_showCase))
                //.setDelay(withDelay) // optional but starting animations immediately in onCreate can make them choppy
                .singleUse("spotFormCommentsShowcase") // provide a unique ID used to ensure it is only shown once
                .setDismissOnTouch(true)
                .show();
    }

    /**
     * Display showcase highlighting checkboxes.
     * The showcase will never be displayed again after the first time this method is called, unless .resetSingleUse() is purposely called.
     **/
    void highlightCheckboxes() {
        String showCaseID = "spotFormCheckboxesShowcase";
        ShowcaseListener showCaseListener = new ShowcaseListener();
        MaterialShowcaseSequence p = new MaterialShowcaseSequence(this, showCaseID);

        //If showCase has been already displayed, only onShowcaseDismissed needs to be called
        if (p.hasFired()) {
            showCaseListener.onShowcaseDismissed();
            return;
        }

        //Highlight checkboxes
        lastShowCaseDisplayed = new MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.save_spot_form_checkboxes))
                .withOvalShape()
                .setDelay(1500) // a second between each showcase view
                .setDismissText(getString(R.string.general_showCase_button))
                .setContentText(getString(R.string.spot_form_extra_options_showCase))
                .setListener(showCaseListener)
                .singleUse(showCaseID) // provide a unique ID used to ensure it is only shown once
                .setDismissOnTouch(true)
                .show();
    }

    public class ShowcaseListener implements IShowcaseListener {
        @Override
        public void onShowcaseDisplayed(MaterialShowcaseView showcaseView) {
            //Get buttons out of the way
            mSaveButton.setVisibility(View.GONE);
            mDeleteButton.setVisibility(View.GONE);
        }

        @Override
        public void onShowcaseDismissed(MaterialShowcaseView showcaseView) {
            onShowcaseDismissed();
        }

        public void onShowcaseDismissed() {
            //Get buttons back to their place
            updateSaveButtonState();
            updateDeleteButtonState();

            if (mapCameraWasMoved && !shouldMoveMapCameraToUserLocationOnMapLoad && mFormType == FormType.Create)
                highlightLocateButton();
        }
    }

    void highlightLocateButton() {
        locateUserTooltip = ViewTooltip
                .on(fabLocateUser)
                .autoHide(true, 7000)
                .corner(30)
                .position(ViewTooltip.Position.RIGHT)
                .text(getString(R.string.general_locate_button_label))
                .show();
    }

    public void locationAddressButtonHandler(View v) {
        String strToCopy = "";

        if (mCurrentSpot.getGpsResolved() != null && mCurrentSpot.getGpsResolved())
            strToCopy = spotLocationToString(mCurrentSpot).trim();

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
        shouldShowButtonsPanel = false;

        SetDateTime(date_datepicker, time_timepicker, Utils.getLocalDateTimeNowAsUTC());
        panel_buttons.setVisibility(View.GONE);
        panel_info.setVisibility(View.VISIBLE);

        makeMapCameraStopFollowGPSUpdates(RenderMode.NORMAL);

        updateSaveButtonState();
        updateSelectedTab();

        //Automatically resolve gps
        fetchAddressButtonHandler(null);

        //Display showcase highlighting the checkboxes, if this wasn't done yet.
        highlightCheckboxes();
    }

    public void viewMapButtonHandler(View view) {
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.putExtra(MainActivity.ARG_REQUEST_TO_OPEN_FRAGMENT, R.id.nav_my_map);
        startActivity(intent);
    }

    public void saveButtonHandler(View view) {
        //If it is a hitchhiking spot and it's part of a route (!is_single_spot) and waiting time wasn't informed, show alert
        if (mFormType != FormType.Create &&
                is_hitchhiking_spot_check_box.isChecked() &&
                is_part_of_a_route_check_box.isChecked() &&
                waiting_time_edittext.getText().toString().isEmpty()) {
            //Show a dialog alerting that a waiting time was not informed
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
            //Try to get the selected coordinates from the map. If map was not loaded, keep the coordinates already set to mCurrentSpot.
            // If map was not loaded and no coordinates were already set, show an error message and prevent the user from saving a spot with no coordinates.
            if (mapboxMap != null && mapboxMap.getCameraPosition() != null && mapboxMap.getCameraPosition().target != null) {
                LatLng selectedLocation = mapboxMap.getCameraPosition().target;

                mCurrentSpot.setLatitude(selectedLocation.getLatitude());
                mCurrentSpot.setLongitude(selectedLocation.getLongitude());
            } else if ((mCurrentSpot.getLatitude() == null || mCurrentSpot.getLatitude() == 0) || (mCurrentSpot.getLongitude() == null || mCurrentSpot.getLongitude() == 0)) {
                Crashlytics.log(Log.WARN, TAG, "No coordinates were previously set and mapbox was not loaded, so we couldn't get any location data for this spot and the user was prevented from executing saveSpot()!");
                showErrorAlert(getString(R.string.save_spot_button_text), getString(R.string.save_spot_error_map_not_loaded));
                return;
            }

            mCurrentSpot.setIsPartOfARoute(is_part_of_a_route_check_box.isChecked());

            mCurrentSpot.setIsHitchhikingSpot(is_hitchhiking_spot_check_box.isChecked());

            mCurrentSpot.setIsNotHitchhikedFromHere(is_not_hitchhiked_from_here_check_box.isChecked());

            mCurrentSpot.setIsDestination(is_destination_check_box.isChecked());

            //Set note
            mCurrentSpot.setNote(note_edittext.getText().toString().trim());

            //Set chosen date & time
            DateTime dateTime = GetDateTime(date_datepicker, time_timepicker);
            mCurrentSpot.setStartDateTime(dateTime);

            if (!evaluate_menuitem.isEnabled()) {
                mCurrentSpot.setAttemptResult(Constants.ATTEMPT_RESULT_UNKNOWN);
                mCurrentSpot.setHitchability(0);
                mCurrentSpot.setIsWaitingForARide(false);
                mCurrentSpot.setWaitingTime(null);
            } else {
                mCurrentSpot.setAttemptResult(attemptResult);
                mCurrentSpot.setHitchability(Utils.findTheOpposite(Math.round(hitchability_ratingbar.getRating())));

                //If user is saving a new hitchhiking spot that belongs to a route, setIsWaitingForARide to true
                if (mFormType == FormType.Create &&
                        is_hitchhiking_spot_check_box.isChecked() && is_part_of_a_route_check_box.isChecked())
                    mCurrentSpot.setIsWaitingForARide(true);
                else
                    mCurrentSpot.setIsWaitingForARide(false);

                String vals = waiting_time_edittext.getText().toString();
                if (!vals.isEmpty())
                    mCurrentSpot.setWaitingTime(Integer.valueOf(vals));
            }

            //Set chosen location
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
                viewModel.insertOrReplace(getContext(), mCurrentSpot);

                // code runs in a thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int result = Constants.RESULT_OBJECT_ADDED;
                        if (mFormType == FormType.Evaluate || mFormType == FormType.Edit)
                            result = Constants.RESULT_OBJECT_EDITED;

                        prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, true).apply();
                        finishSaving(result);
                    }
                });
            }
        }.start();
    }

    public void deleteButtonHandler(View view) {
        if (mCurrentSpot == null || mCurrentSpot.getId() == null || mCurrentSpot.getId() <= 0) {
            Crashlytics.setBool("mCurrentSpot is null", mCurrentSpot == null);
            Crashlytics.logException(new Exception("Delete button was visible when spot id was unknown, and user clicked it."));
            mDeleteButton.setVisibility(View.GONE);
            return;
        }

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
                                viewModel.deleteSpot(getContext(), mCurrentSpot);
                                viewModel.setCurrentSpot(null);

                                // code runs in a thread
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ComponentName callingActivity = getCallingActivity();

                                        //Create a record to track usage of Delete button when a spot is deleted
                                        Answers.getInstance().logCustom(new CustomEvent("Spot deleted"));

                                        setResult(Constants.RESULT_OBJECT_DELETED);
                                        prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, true).apply();
                                        finish();

                                        if (!shouldGoBackToPreviousActivity && (callingActivity == null
                                                || !callingActivity.getClassName().equals(MainActivity.class.getName()))) {
                                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                                            intent.putExtra(Constants.SHOULD_SHOW_SPOT_DELETED_SNACKBAR_KEY, true);
                                            intent.putExtra(MainActivity.ARG_REQUEST_TO_OPEN_FRAGMENT, R.id.nav_my_map);
                                            startActivity(intent);
                                        }
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
        setResult(result);

        if (mCurrentSpot.getIsWaitingForARide() != null && mCurrentSpot.getIsWaitingForARide())
            viewModel.setCurrentSpot(mCurrentSpot);
        else
            viewModel.setCurrentSpot(null);

        //If a hitchhiking spot is being saved for the first time, then let the user evaluate it.
        if (mFormType == FormType.Create && is_hitchhiking_spot_check_box.isChecked()) {
            showEvaluatePanel();
            return;
        }

        //If the spot being saved is part of a route and it is not a destination,
        // then show options to save a new spot or to view the map.
        if (mFormType != FormType.Edit &&
                is_part_of_a_route_check_box.isChecked() &&
                !is_destination_check_box.isChecked()) {
            showSaveNewOrViewMapPanel();

            if (mapboxMap != null && style != null && style.isFullyLoaded()) {
                //Request permission of access to GPS updates or
                // directly initialize and enable the location plugin if such permission was already granted.
                enableLocationLayer(style);

                //Make map camera follow GPS. When user clicks on "save spot" button the map camera will stop following GPS updates so that user can adjust location.
                makeMapCameraFollowGPSUpdates(RenderMode.COMPASS);
            }
            return;
        }

        finish();

        /*if (!is_hitchhiking_spot_check_box.isChecked()) {
            Intent i = new Intent(getBaseContext(), MyRoutesActivity.class);
            i.putExtra(MyRoutesActivity.LAST_TAB_OPENED_KEY, MyRoutesActivity.SectionsPagerAdapter.TAB_SPOTS_INDEX);
            i.putExtra(Constants.SHOULD_SHOW_SPOT_SAVED_SNACKBAR_KEY, true);
            startActivity(i);
        } else */

        if (!shouldGoBackToPreviousActivity) {
            Intent i = new Intent(getBaseContext(), MainActivity.class);
            i.putExtra(MainActivity.ARG_REQUEST_TO_OPEN_FRAGMENT, R.id.nav_my_map);
            i.putExtra(Constants.SHOULD_SHOW_SPOT_SAVED_SNACKBAR_KEY, true);
            startActivity(i);
        }
    }

    void showSaveNewOrViewMapPanel() {
        mFormType = FormType.Create;

        Crashlytics.setString("mFormType", mFormType.toString());

        refreshDatetimeAlertDialogWasShown = false;

        collapseBottomSheet();
        hideKeyboard();

        mCurrentSpot = new Spot();
        mCurrentSpot.setIsHitchhikingSpot(true);
        mCurrentSpot.setIsPartOfARoute(true);

        shouldShowButtonsPanel = true;

        mAddressRequested = false;
        mAddressOutput = null;

        showViewMapSnackbar();

        //We want to show the Basic tab.
        lastSelectedTab = R.id.action_basic;

        // updateUI() will call setSelectedItemId(lastSelectedTab).
        updateUI();
    }

    void showEvaluatePanel() {
        mFormType = FormType.Evaluate;

        Crashlytics.setString("mFormType", mFormType.toString());

        //Create a record to track usage of Save button when a new spot is saved for the first time
        Answers.getInstance().logCustom(new CustomEvent("Spot created"));

        refreshDatetimeAlertDialogWasShown = false;

        //If tooltip is still being shown, hide it
        if (locateUserTooltip != null && ViewCompat.isAttachedToWindow(locateUserTooltip))
            locateUserTooltip.closeNow();

        //If showcase is still being shown, hide it
        if (lastShowCaseDisplayed != null && ViewCompat.isAttachedToWindow(lastShowCaseDisplayed))
            lastShowCaseDisplayed.hide();

        //If toast is being shown, hide it
        if (msgResult != null && ViewCompat.isAttachedToWindow(msgResult.getView()))
            msgResult.cancel();

        collapseBottomSheet();
        hideKeyboard();

        showViewMapSnackbar();

        //We want to show the Evaluate tab.
        lastSelectedTab = R.id.action_evaluate;

        if (mapboxMap != null) {
            makeMapCameraStopFollowGPSUpdates(RenderMode.NORMAL);
        }

        // updateUI() will call setSelectedItemId(lastSelectedTab).
        updateUI();
    }

    ProgressDialog loadingDialog;

    private void showProgressDialog() {
        if (loadingDialog == null) {
            ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            loadingDialog = new ProgressDialog(SpotFormActivity.this);
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
            loadingDialog.setTitle(getString(R.string.general_downloading_data_message));
            loadingDialog.setMessage(getString(R.string.general_loading_dialog_message));
        }
        loadingDialog.show();
    }

    private void dismissProgressDialog() {
        //first set progressBar to invisible
        ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            if (loadingDialog != null && loadingDialog.isShowing())
                loadingDialog.dismiss();

        } catch (Exception e) {
        }
    }

    Dialog dialog;

    //dialog for showing comments
    @SuppressWarnings("deprecation")
    private void showCommentsDialog(ArrayList<PlaceInfoCompleteComment> arrayListOfComments) {
        if (dialog == null) {
            //custom dialog
            dialog = new Dialog(SpotFormActivity.this, android.R.style.Theme_Translucent);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);   //if clicked on dim dialog will disappear
            dialog.setCanceledOnTouchOutside(true);

            //get dialog's window to set parameters about its appearance
            Window window = dialog.getWindow();
            WindowManager.LayoutParams wlp = window.getAttributes();

            Display display = getWindowManager().getDefaultDisplay();
            float screenWidth = display.getWidth();
            float screenHeight = display.getHeight();

            wlp.gravity = Gravity.CENTER;
            wlp.width = (int) ((screenWidth * (0.9f)));
            wlp.height = (int) (screenHeight * (0.9f));
            wlp.dimAmount = 0.6f;
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setAttributes(wlp);
        }

        //Set the data
        CommentsListViewAdapter commentsAdapter = new CommentsListViewAdapter(SpotFormActivity.this, arrayListOfComments);

        RelativeLayout commentsLayout = (RelativeLayout) View.inflate(SpotFormActivity.this, R.layout.dialog_comments_layout, null);
        ListView commentsListView = (ListView) commentsLayout.findViewById(R.id.layout_comments_listview);

        //set adapter and bound it to commentsListView
        commentsListView.setAdapter(commentsAdapter);

        //set the whole inflated layout into dialog
        dialog.setContentView(commentsLayout);


        dialog.show();
    }


    public void dismissCommetsDialog(View view) {
        try {
            if (dialog != null && dialog.isShowing())
                dialog.dismiss();
        } catch (Exception e) {
        }
    }

    public void editDateButtonHandler(View view) {
        if (spot_form_more_options.getVisibility() == View.VISIBLE) {
            spot_form_more_options.setVisibility(View.GONE);
            collapseBottomSheet();
            hideKeyboard();
        } else {
            spot_form_more_options.setVisibility(View.VISIBLE);
            expandBottomSheet();
        }
    }

    public void onCheckedChanged(CompoundButton checkBox, boolean isChecked) {
        //Clear all checkedChangedListener - it should be set again before the end of this method
        is_destination_check_box.setOnCheckedChangeListener(null);
        is_not_hitchhiked_from_here_check_box.setOnCheckedChangeListener(null);
        is_part_of_a_route_check_box.setOnCheckedChangeListener(null);
        is_hitchhiking_spot_check_box.setOnCheckedChangeListener(null);

        switch (checkBox.getId()) {
            case R.id.save_spot_form_is_hitchhiking_spot_check_box:
                is_destination_check_box.setChecked(false);
                is_not_hitchhiked_from_here_check_box.setChecked(false);
                break;
            case R.id.save_spot_form_is_destination_check_box:
                is_hitchhiking_spot_check_box.setChecked(false);
                is_not_hitchhiked_from_here_check_box.setChecked(false);
                is_part_of_a_route_check_box.setChecked(true);
                break;
            case R.id.save_spot_form_is_not_hitchhiked_from_here_check_box:
                is_hitchhiking_spot_check_box.setChecked(false);
                is_part_of_a_route_check_box.setChecked(true);
                break;
        }

        updateCheckboxesStates();

        //Set all checkedChangedListener
        is_destination_check_box.setOnCheckedChangeListener(this);
        is_not_hitchhiked_from_here_check_box.setOnCheckedChangeListener(this);
        is_part_of_a_route_check_box.setOnCheckedChangeListener(this);
        is_hitchhiking_spot_check_box.setOnCheckedChangeListener(this);
    }

    void updateCheckboxesStates() {
        boolean isHitchhikingSpotChecked = is_hitchhiking_spot_check_box.isChecked();
        boolean isPartOfARouteChecked = is_part_of_a_route_check_box.isChecked();
        boolean isNotHitchhikedFromHereChecked = is_not_hitchhiked_from_here_check_box.isChecked();
        boolean isDestinationChecked = is_destination_check_box.isChecked();

        if (!isPartOfARouteChecked) {
            is_not_hitchhiked_from_here_check_box.setEnabled(false);
            is_destination_check_box.setEnabled(false);

            is_not_hitchhiked_from_here_check_box.setChecked(false);
            is_destination_check_box.setChecked(false);
        } else {
            boolean shouldDisplayNotHitchhikedCheckbox = true;

            if (isDestinationChecked) {
                is_hitchhiking_spot_check_box.setEnabled(true);
                is_part_of_a_route_check_box.setEnabled(true);

                shouldDisplayNotHitchhikedCheckbox = false;
            } else if (isHitchhikingSpotChecked) {
                is_hitchhiking_spot_check_box.setEnabled(true);
                is_part_of_a_route_check_box.setEnabled(true);
                is_destination_check_box.setEnabled(true);

                shouldDisplayNotHitchhikedCheckbox = false;
            } else if (isNotHitchhikedFromHereChecked) {
                is_part_of_a_route_check_box.setEnabled(false);
                is_hitchhiking_spot_check_box.setEnabled(false);
                is_destination_check_box.setEnabled(false);
            } else {
                is_hitchhiking_spot_check_box.setEnabled(true);
                is_destination_check_box.setEnabled(true);
                is_part_of_a_route_check_box.setEnabled(true);
                is_not_hitchhiked_from_here_check_box.setEnabled(true);
            }

            if (shouldDisplayNotHitchhikedCheckbox)
                is_not_hitchhiked_from_here_check_box.setVisibility(View.VISIBLE);
            else
                is_not_hitchhiked_from_here_check_box.setVisibility(View.GONE);
        }

        //Apply styles
        if (is_destination_check_box.isChecked())
            is_destination_check_box.setTypeface(null, Typeface.BOLD);
        else
            is_destination_check_box.setTypeface(null, Typeface.NORMAL);

        if (is_not_hitchhiked_from_here_check_box.isChecked())
            is_not_hitchhiked_from_here_check_box.setTypeface(null, Typeface.BOLD);
        else
            is_not_hitchhiked_from_here_check_box.setTypeface(null, Typeface.NORMAL);

        if (is_part_of_a_route_check_box.isChecked())
            is_part_of_a_route_check_box.setTypeface(null, Typeface.BOLD);
        else
            is_part_of_a_route_check_box.setTypeface(null, Typeface.NORMAL);

        if (is_hitchhiking_spot_check_box.isChecked())
            is_hitchhiking_spot_check_box.setTypeface(null, Typeface.BOLD);
        else
            is_hitchhiking_spot_check_box.setTypeface(null, Typeface.NORMAL);
    }

    private void updateCheckboxesStates(Boolean isDestination, Boolean isNotHitchhikedFromHere, Boolean isPartOfARouteSpot, Boolean isHitchhikingSpot) {
        //Add checkboxes listeners
        is_destination_check_box.setOnCheckedChangeListener(null);
        is_part_of_a_route_check_box.setOnCheckedChangeListener(null);
        is_not_hitchhiked_from_here_check_box.setOnCheckedChangeListener(null);
        is_hitchhiking_spot_check_box.setOnCheckedChangeListener(null);

        is_destination_check_box.setChecked(isDestination);
        is_part_of_a_route_check_box.setChecked(isPartOfARouteSpot);
        is_not_hitchhiked_from_here_check_box.setChecked(isNotHitchhikedFromHere);
        is_hitchhiking_spot_check_box.setChecked(isHitchhikingSpot);

        //Add checkboxes listeners
        is_destination_check_box.setOnCheckedChangeListener(this);
        is_part_of_a_route_check_box.setOnCheckedChangeListener(this);
        is_not_hitchhiked_from_here_check_box.setOnCheckedChangeListener(this);
        is_hitchhiking_spot_check_box.setOnCheckedChangeListener(this);

        updateCheckboxesStates();
    }

    public void SetDateTime(DatePicker datePicker, TimePicker timePicker, DateTime date) {
        SetSelectedDateTimeString(date);

        DateTime dateTime = new DateTime(date);

        // Must always subtract 1 here as DatePicker month is 0 based
        datePicker.init(dateTime.getYear(), dateTime.getMonthOfYear() - 1, dateTime.getDayOfMonth(), new DatePicker.OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth) {
                DateTime selectedDateTime = GetDateTime(date_datepicker, time_timepicker);
                SetSelectedDateTimeString(selectedDateTime);
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
                SetSelectedDateTimeString(selectedDateTime);
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
                hour, minute, DateTimeZone.UTC); // Must always add 1 to datePickers getMounth returned value, as it is 0 based
        return dateTime;
    }

    public void SetSelectedDateTimeString(DateTime dateTime) {
        String dateTimeFormat = Utils.getDateTimeFormat(dateTime, " 'EEEE', ");
        selected_date.setText(Utils.dateTimeToString(dateTime, dateTimeFormat));
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

        savedInstanceState.putBoolean(SNACKBAR_SHOWED_KEY, wasSnackbarShown);

        savedInstanceState.putBoolean(BUTTONS_PANEL_IS_VISIBLE_KEY, shouldShowButtonsPanel);

        savedInstanceState.putInt(LAST_SELECTED_TAB_ID_KEY, lastSelectedTab);

        savedInstanceState.putBoolean(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, shouldGoBackToPreviousActivity);

        savedInstanceState.putBoolean(REFRESH_DATETIME_ALERT_SHOWN_KEY, refreshDatetimeAlertDialogWasShown);

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

            if (savedInstanceState.keySet().contains(SNACKBAR_SHOWED_KEY))
                wasSnackbarShown = savedInstanceState.getBoolean(SNACKBAR_SHOWED_KEY);

            if (savedInstanceState.keySet().contains(BUTTONS_PANEL_IS_VISIBLE_KEY))
                shouldShowButtonsPanel = savedInstanceState.getBoolean(BUTTONS_PANEL_IS_VISIBLE_KEY);

            if (savedInstanceState.keySet().contains(LAST_SELECTED_TAB_ID_KEY))
                lastSelectedTab = savedInstanceState.getInt(LAST_SELECTED_TAB_ID_KEY);

            if (savedInstanceState.keySet().contains(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY))
                shouldGoBackToPreviousActivity = savedInstanceState.getBoolean(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY);

            if (savedInstanceState.keySet().contains(REFRESH_DATETIME_ALERT_SHOWN_KEY))
                refreshDatetimeAlertDialogWasShown = savedInstanceState.getBoolean(REFRESH_DATETIME_ALERT_SHOWN_KEY);
        }
    }

    //----BEGIN: Part related to reverse geocoding
    Snackbar snackbar;

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
        int snackbarTextId = com.google.android.material.R.id.snackbar_text;
        TextView textView = (TextView) snackbarView.findViewById(snackbarTextId);
        if (textView != null) textView.setTextColor(Color.WHITE);


        // change snackbar background
        snackbarView.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.ic_regular_spot_color));

        snackbar.show();
    }

    void showViewMapSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_saved_successfuly),
                String.format(getString(R.string.action_button_label), getString(R.string.view_map_button_label)), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
                        intent.putExtra(MainActivity.ARG_REQUEST_TO_OPEN_FRAGMENT, R.id.nav_my_map);
                        startActivity(intent);
                    }
                });
    }

    void dismissSnackbar() {
        try {
            if (snackbar != null && snackbar.isShown())
                snackbar.dismiss();
        } catch (Exception e) {
        }
    }


    /**
     * Runs when user clicks the Fetch Address button. Starts the service to fetch the address if
     * GoogleApiClient is connected.
     */
    public void fetchAddressButtonHandler(View view) {
        if (mapboxMap == null)
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
        updateLocationWidgets();
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
        updateLocationWidgets();
    }

    /**
     * Updates the address in the UI.
     */
    protected void displayAddressOutput() {
        if (mAddressOutput != null) {
            String cityName = mAddressOutput.getLocality();
            if (cityName == null)
                cityName = mAddressOutput.getSubLocality();

            String subAdminArea = mAddressOutput.getSubAdminArea();
            String adminArea = mAddressOutput.getAdminArea();

            //If admin area was not found, then use sub admin area instead,
            // otherwise (if area was found but city name was not), then use sub admin area as city name.
            if (adminArea == null && subAdminArea != null)
                adminArea = subAdminArea;
            else if (subAdminArea != null)
                cityName = subAdminArea;

            mCurrentSpot.setCity(cityName);
            mCurrentSpot.setState(adminArea);
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

        updateSpotLocationOnUI();
    }

    static String locationSeparator = ", ";

    private static String spotLocationToString(Spot spot) {
        Crashlytics.log(Log.INFO, TAG, "Generating a string for the spot's address");

        ArrayList<String> loc = new ArrayList();
        try {
            loc = Utils.spotLocationToList(spot);

            //Join the strings
            return TextUtils.join(locationSeparator, loc);
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }
        return "";
    }


    /**
     * Toggles the visibility of the progress bar. Enables or disables the Fetch Address button.
     */
    private void updateLocationWidgets() {
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

    ViewTooltip.TooltipView locateUserTooltip;
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
         * Receives data sent from FetchAddressIntentService and updates the UI in MyRoutesActivity.
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
            if (lastShowCaseDisplayed == null)
                showToast(strResult);
            updateLocationWidgets();
        }
    }

    private AsyncTask<String, Void, String> taskThatRetrievesCompleteDetails = null;
    public PlaceInfoComplete placeWithCompleteDetails;

    APICallCompletionListener<PlaceInfoComplete> getPlaceCompleteDetails = new APICallCompletionListener<PlaceInfoComplete>() {
        @Override
        public void onComplete(boolean success, int intParam, String stringParam, Error error, PlaceInfoComplete object) {
            if (success) {
                placeWithCompleteDetails = object;
                if (placeWithCompleteDetails != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            placeButtonComments.setVisibility(View.VISIBLE);
                            placeButtonComments.setText(String.format(getString(R.string.spot_form_view_comments_button_label), placeWithCompleteDetails.getComments_count()));
                        }
                    });
                }
            } else {
                showErrorAlert(getString(R.string.general_error_dialog_title), String.format(getString(R.string.general_error_dialog_message), error.getErrorDescription()));
                Crashlytics.setBool("success", success);
                Crashlytics.setInt("intParam", intParam);
                Crashlytics.logException(new Exception("getPlaceCompleteDetails error description:\n" + error.getErrorDescription()));
            }
        }
    };

    //async task to retrieve details about clicked marker (point) on a map
    private class retrievePlaceDetailsAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected String doInBackground(String... params) {
            if (isCancelled()) {
                return "Canceled";
            }

            String result = "Executed";
            Crashlytics.log(Log.INFO, TAG, "Calling ApiManager getPlaceCompleteDetails");
            try {
                //hwSpotId of clicked marker, passed here as parameter in .execute(_id);
                int hwSpotId = Integer.valueOf(params[0]);
                Crashlytics.setInt("hwSpotId", hwSpotId);

                ApiManager hitchwikiAPI = new ApiManager();
                hitchwikiAPI.getPlaceCompleteDetails(hwSpotId, getPlaceCompleteDetails);
            } catch (Exception ex) {
                Crashlytics.logException(ex);
                result = ex.getMessage();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (SpotFormActivity.this.isFinishing())
                return;
            //button listeners
           /* placeButtonNavigate.setOnClickListener(new Button.OnClickListener()
            {
                public void onClick(View v)
                {
                    //intent that fires up Google Maps or Browser and gets Google navigation
                    //to chosen marker, mode is walking (more suitable for hitchhikers)
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("http://maps.google.com/maps?saddr="
                                    + latLng.latitude
                                    + ","
                                    + latLng.longitude
                                    + "&daddr="
                                    + placeWithCompleteDetails.getLat()
                                    + ","
                                    + placeWithCompleteDetails.getLon()
                                    + "&mode=walking"
                            ));
                    startActivity(intent);
                }
            });*/

            String errMsgToShow = "";
            if (result.equalsIgnoreCase("Executed")) {
                try {
                    String note = placeWithCompleteDetails.getDescriptionENdescription();
                    if (note != null)
                        note = note.trim();
                    mCurrentSpot.setNote(note);
                    mCurrentSpot.setCountryCode(placeWithCompleteDetails.getCountry_iso());
                    mCurrentSpot.setCountry(placeWithCompleteDetails.getCountry_name());
                    mCurrentSpot.setCity(placeWithCompleteDetails.getLocality());

                    if (placeWithCompleteDetails.getDescriptionENdatetime() != null &&
                            placeWithCompleteDetails.getDescriptionENdatetime() != "null" &&
                            !placeWithCompleteDetails.getDescriptionENdatetime().isEmpty()) {
                        DateTime extractedDateTime = DateTimeFormat.forPattern(APIConstants.PLACE_INFO_DATETIME_FORMAT).withZone(DateTimeZone.UTC)
                                .parseDateTime(placeWithCompleteDetails.getDescriptionENdatetime());
                        mCurrentSpot.setStartDateTime(extractedDateTime);
                    }

                    //If a waiting time was informed, convert it into Integer
                    String waiting_stats_avg = placeWithCompleteDetails.getWaiting_stats_avg();
                    if (waiting_stats_avg != null && !waiting_stats_avg.equalsIgnoreCase("null") && !waiting_stats_avg.isEmpty())
                        mCurrentSpot.setWaitingTime(Integer.valueOf(waiting_stats_avg));

                    //If any location string can be built with the location data received, then we should set gpsResolved to true
                    if ((mCurrentSpot.getCity() != null && !mCurrentSpot.getCity().isEmpty()) ||
                            (mCurrentSpot.getCountry() != null && !mCurrentSpot.getCountry().isEmpty()) ||
                            (mCurrentSpot.getCountryCode() != null && !mCurrentSpot.getCountryCode().isEmpty()))
                        mCurrentSpot.setGpsResolved(true);
                } catch (Exception ex) {
                    Crashlytics.logException(ex);
                    errMsgToShow = "Failed to set mCurrentSpot. " + ex.getMessage();
                }
            } else {
                errMsgToShow = "Failed to download spot details from Hitchwiki Maps.\n\"" + result + "\"";
            }

            updateUI();

            dismissProgressDialog();

            if (!errMsgToShow.isEmpty())
                showErrorAlert(getString(R.string.general_error_dialog_title), String.format(getString(R.string.general_error_dialog_message), errMsgToShow));
            else {
                if (!placeWithCompleteDetails.getComments_count().contentEquals("0"))
                    highlightCommentsButton();
                else
                    Toast.makeText(context, getString(R.string.general_download_finished_successffull_message), Toast.LENGTH_SHORT).show();
            }
        }

    }

//----END: Part related to reverse geocoding

}
