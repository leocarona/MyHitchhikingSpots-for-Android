package com.myhitchhikingspots;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.Date;

public class SpotFormActivity extends BaseActivity {
    private Button mSaveButton, mDeleteButton;
    private EditText note_edittext, waiting_time_edittext;
    private DatePicker date_datepicker;
    private TimePicker time_timepicker;
    private Spinner attempt_results_spinner, hitchability_spinner;
    private Spot mCurrentSpot;
    private CheckBox is_destination_check_box;
    private TextView form_title;
    private LinearLayout spot_form_evaluate, spot_form_basic, spot_form_more_options, hitchability_options, attempt_result_panel;
    protected static final String TAG = "save_spot";
    protected final static String CURRENT_SPOT_KEY = "current-spot-key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.spot_form_master_layout);

        mSaveButton = (Button) findViewById(R.id.save_button);
        mDeleteButton = (Button) findViewById(R.id.delete_button);
        note_edittext = (EditText) findViewById(R.id.spot_form_note_edittext);
        hitchability_spinner = (Spinner) findViewById(R.id.spot_form_hitchability_spinner);
        date_datepicker = (DatePicker) findViewById(R.id.spot_form_date_datepicker);
        time_timepicker = (TimePicker) findViewById(R.id.spot_form_time_timepicker);
        waiting_time_edittext = (EditText) findViewById(R.id.spot_form_waiting_time_edittext);
        attempt_results_spinner = (Spinner) findViewById(R.id.spot_form_attempt_result_spinner);
        spot_form_basic = (LinearLayout) findViewById(R.id.save_spot_form_basic);
        spot_form_evaluate = (LinearLayout) findViewById(R.id.save_spot_form_evaluate);
        spot_form_more_options = (LinearLayout) findViewById(R.id.save_spot_form_more_options);
        is_destination_check_box = (CheckBox) findViewById(R.id.save_spot_form_is_destination_check_box);
        hitchability_options = (LinearLayout) findViewById(R.id.save_spot_form_hitchability_options);
        attempt_result_panel = (LinearLayout) findViewById(R.id.save_spot_form_attempt_result_panel);
        form_title = (TextView) findViewById(R.id.save_spot_form_title);

        try {
            if (savedInstanceState != null)
                updateValuesFromBundle(savedInstanceState);
            else
                mCurrentSpot = (Spot) getIntent().getSerializableExtra("Spot");

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
        } catch (Exception ex) {
            Log.e(TAG, "onCreate", ex);
            Toast.makeText(getApplicationContext(), "Something went wrong :(", Toast.LENGTH_LONG).show();
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
        }
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
                Toast.makeText(this, "Something went wrong, please open your spot again :(", Toast.LENGTH_LONG).show();
            }

            if (mFormType == FormType.Evaluate || mFormType == FormType.All) {
                mDeleteButton.setVisibility(View.VISIBLE);
                spot_form_evaluate.setVisibility(View.VISIBLE);
            } else if (mFormType == FormType.Destination) {
                mDeleteButton.setVisibility(View.VISIBLE);
                spot_form_evaluate.setVisibility(View.GONE);
            }

            if (mFormType == FormType.Basic || mFormType == FormType.Destination || mFormType == FormType.All)
                spot_form_basic.setVisibility(View.VISIBLE);

            if (mFormType == FormType.All)
                attempt_result_panel.setVisibility(View.VISIBLE);
            else
                attempt_result_panel.setVisibility(View.GONE);


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

                SetDateTime(date_datepicker, time_timepicker, mCurrentSpot.getStartDateTime());

                if (mFormType == FormType.All) {
                    if (mCurrentSpot.getHitchability() != null)
                        hitchability_spinner.setSelection(mCurrentSpot.getHitchability());
                    if (mCurrentSpot.getAttemptResult() != null)
                        attempt_results_spinner.setSelection(mCurrentSpot.getAttemptResult());

                    form_title.setText(getResources().getString(R.string.spot_form_title_edit));
                } else {
                    form_title.setText(getResources().getString(R.string.arrived_button_text));
                    is_destination_check_box.setVisibility(View.VISIBLE);
                }
            } else if (mFormType == FormType.Evaluate) {
                DateTime date = new DateTime(mCurrentSpot.getStartDateTime());
                Integer minutes = Minutes.minutesBetween(date, DateTime.now()).getMinutes();
                waiting_time_edittext.setText(minutes.toString());

                //TODO: make this check in a not hardcoded way!
                if (mCurrentSpot.getAttemptResult() != null && mCurrentSpot.getAttemptResult() > 0) {
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
                is_destination_check_box.setVisibility(View.VISIBLE);
            }

        } catch (Exception ex) {
            Log.e(TAG, "updateUI", ex);
            Toast.makeText(getApplicationContext(), "Something went wrong :(", Toast.LENGTH_LONG).show();
        }
    }

    public void saveButtonHandler(View view) {
        try {
            if (mFormType == FormType.Basic || mFormType == FormType.Destination || mFormType == FormType.All) {
                mCurrentSpot.setNote(note_edittext.getText().toString());

                if (is_destination_check_box.isChecked()) {
                    mCurrentSpot.setIsDestination(true);
                    mCurrentSpot.setHitchability(null);
                    mCurrentSpot.setIsWaitingForARide(false);
                } else {
                    mCurrentSpot.setIsDestination(false);
                    mCurrentSpot.setHitchability(hitchability_spinner.getSelectedItemPosition());
                    if (mFormType == FormType.Basic)
                        mCurrentSpot.setIsWaitingForARide(true);
                    else if (mFormType == FormType.Destination)
                        mCurrentSpot.setIsWaitingForARide(false);
                }

                DateTime dateTime = GetDateTime(date_datepicker, time_timepicker);
                mCurrentSpot.setStartDateTime(dateTime.toDate());
            }
            if (mFormType == FormType.Evaluate || mFormType == FormType.All) {
                String vals = waiting_time_edittext.getText().toString();
                if (vals != null && !vals.isEmpty())
                    mCurrentSpot.setWaitingTime(Integer.parseInt(vals));
                mCurrentSpot.setAttemptResult(attempt_results_spinner.getSelectedItemPosition());

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
            Toast.makeText(getApplicationContext(), "Something went wrong :(", Toast.LENGTH_LONG).show();
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
        super.onSaveInstanceState(savedInstanceState);
    }
}
