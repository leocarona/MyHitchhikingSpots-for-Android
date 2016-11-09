package com.myhitchhikingspots;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.location.Address;
import android.media.Rating;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.ArrayList;
import java.util.Date;

import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

public class SpotFormActivity extends BaseActivity implements RatingBar.OnRatingBarChangeListener {


    private Button mSaveButton, mDeleteButton;
    private EditText note_edittext, waiting_time_edittext;
    private DatePicker date_datepicker;
    private TimePicker time_timepicker;
    private Spinner attempt_results_spinner;
    private Spot mCurrentSpot;
    private CheckBox is_destination_check_box;
    private TextView form_title, hitchabilityLabel;
    private LinearLayout spot_form_evaluate, spot_form_basic, spot_form_more_options, hitchability_options, attempt_result_panel;
    private RatingBar hitchability_ratingbar;

    protected static final String TAG = "save_spot";
    protected final static String CURRENT_SPOT_KEY = "current-spot-key";

    //----BEGIN: Part related to reverse geocoding
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";

    /**
     * Represents a geographical location.
     */
    protected MyLocation mLastLocation;

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

        try {
            if (savedInstanceState != null)
                updateValuesFromBundle(savedInstanceState);
            else {
                mCurrentSpot = (Spot) getIntent().getSerializableExtra("Spot");
                mLocationAddressTextView.setText(getString(mCurrentSpot));
            }

            // If user is currently waiting for a ride at the current spot, show him the Evaluate form. If he is not,
            // that means he's saving a new spot so we need to show him the Basic form instead.
            if (mCurrentSpot == null)
                mFormType = FormType.Unknown;
            else {
                if (mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null) {
                    mLastLocation = new MyLocation(mCurrentSpot.getLatitude(), mCurrentSpot.getLongitude());
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
            }
        } catch (Exception ex) {
            Log.e(TAG, "onCreate", ex);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.generall_error_message), Toast.LENGTH_LONG).show();
        }

        updateUI();

        super.onCreate(savedInstanceState);
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
                Log.e(TAG, "Something went wrong - mCurrentSpot is null");
                Toast.makeText(this, getResources().getString(R.string.generall_error_message) + " Please open your spot again.", Toast.LENGTH_LONG).show();
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


                if ((mFormType == FormType.Basic || mFormType == FormType.Destination) &&
                        (mCurrentSpot.getGpsResolved() == null || !mCurrentSpot.getGpsResolved()))
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

                //TODO: make this check in a not hardcoded way!
                if (mCurrentSpot.getAttemptResult() != null && mCurrentSpot.getAttemptResult() >= 0 && mCurrentSpot.getAttemptResult() < attempt_results_spinner.getCount()) {
                    attempt_results_spinner.setSelection(mCurrentSpot.getAttemptResult());

                    //TODO: make this check in a not hardcoded way!
                    switch (mCurrentSpot.getAttemptResult()) {
                        case 1:
                            form_title.setText(getResources().getString(R.string.got_a_ride_button_text));
                            attempt_result_panel.setVisibility(View.GONE);
                            break;
                        case 2:
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
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.generall_error_message), Toast.LENGTH_LONG).show();
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
        //TODO: copy a string with format "address (lat, lng)" to the memory so that the user can paste it wherever he wants

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
        SaveSpot();

        /*if (!mAddressRequested)
            SaveSpot();
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.spot_form_save_while_requesting_addr_dialog_title))
                    .setMessage(getResources().getString(R.string.spot_form_save_while_requesting_addr_dialog_text))
                    .setPositiveButton(getResources().getString(R.string.spot_form_save_button_text), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new Thread() {
                                @Override
                                public void run() {
                                    // code runs in a thread
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            stopIntentService();
                                            SaveSpot();
                                        }
                                    });
                                }
                            }.start();
                        }

                    })
                    .setNegativeButton(getResources().getString(R.string.spot_form_delete_dialog_no_option), null)
                    .show();
        }*/
    }

    private void SaveSpot() {
        try {
            DateTime dateTime = GetDateTime(date_datepicker, time_timepicker);
            mCurrentSpot.setStartDateTime(dateTime.toDate());

            if (mFormType == FormType.Basic || mFormType == FormType.Destination || mFormType == FormType.All) {
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
                if (vals != null && !vals.isEmpty())
                    mCurrentSpot.setWaitingTime(Integer.parseInt(vals));
                mCurrentSpot.setAttemptResult(attempt_results_spinner.getSelectedItemPosition());
                mCurrentSpot.setHitchability(findTheOpposit(Math.round(hitchability_ratingbar.getRating())));

                if (mFormType == FormType.Evaluate)
                    mCurrentSpot.setIsWaitingForARide(false);
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
                            finish();
                        }
                    });
                }
            }.start();

        } catch (Exception ex) {
            Log.e(TAG, "saveButtonHandler", ex);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.generall_error_message), Toast.LENGTH_LONG).show();
        }
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
                                    }
                                });
                            }
                        }.start();
                    }

                })
                .setNegativeButton(getResources().getString(R.string.spot_form_delete_dialog_no_option), null)
                .show();

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
        super.onSaveInstanceState(savedInstanceState);
    }


    //----BEGIN: Part related to reverse geocoding


    /**
     * Runs when user clicks the Fetch Address button. Starts the service to fetch the address if
     * GoogleApiClient is connected.
     */
    public void fetchAddressButtonHandler(View view) {
        // We only start the service to fetch the address if GoogleApiClient is connected.
        if (mLastLocation == null)
            return;

        startIntentService();

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
    protected void startIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        fetchAddressServiceIntent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        fetchAddressServiceIntent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        fetchAddressServiceIntent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

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
        }

        mLocationAddressTextView.setText(getString(mCurrentSpot));

    }

    @NonNull
    private String getString(Spot mCurrentSpot) {
        String spotLoc = "";
        try {
            spotLoc = spotLocationToString(mCurrentSpot).trim();
            if ((spotLoc == null || spotLoc.isEmpty()) && (mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null))
                spotLoc = String.format(getResources().getString(R.string.spot_form_lat_lng_label),
                        mCurrentSpot.getLatitude().toString(), mCurrentSpot.getLongitude().toString());
        } catch (Exception ex) {
            Log.w("getString", "Err msg: " + ex.getMessage());
        }
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

    /**
     * Shows a toast with the given text.
     */
    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
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
            if (resultCode == Constants.FAILURE_RESULT)
                strResult = resultData.getString(Constants.RESULT_STRING_KEY);
            else {
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
