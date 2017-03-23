/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.myhitchhikingspots;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;

import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.model.Spot;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Getting Location Updates.
 * <p>
 * Demonstrates how to use the Fused Location Provider API to get updates about a device's
 * location. The Fused Location Provider is part of the Google Play services location APIs.
 * <p>
 * For a simpler example that shows the use of Google Play services to fetch the last known location
 * of a device, see
 * https://github.com/googlesamples/android-play-location/tree/master/BasicLocation.
 * <p>
 * This sample uses Google Play services, but it does not require authentication. For a sample that
 * uses Google Play services for authentication, see
 * https://github.com/googlesamples/android-google-accounts/tree/master/QuickStart.
 */
public class MyLocationFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, RatingBar.OnRatingBarChangeListener {

    // UI Widgets
    protected Switch mGetLocationSwitch;
    public ImageButton mSaveSpotButton, mArrivedButton;
    public ImageButton mGotARideButton, mTookABreakButton;
    public LinearLayout mSaveSpotPanel, mEvaluatePanel, mArrivedPanel, mCurrentLocationPanel;
    protected TextView mLastUpdateTimeTextView, mLatitudeTextView, mLongitudeTextView, mWaitingToGetCurrentLocationTextView, mHitchabilityTextView, mAccuracyTextView;
    protected RatingBar hitchability_ratingbar;

    // UI Labels.
    protected String mLatitudeLabel, mLongitudeLabel, mAccuracyLabel, mLastUpdateTimeLabel;

    protected TrackLocationBaseActivity parentActivity;

    protected static final String TAG = "my-location-fragment";

    /*protected AudioManager mAudioManager;
    protected ComponentName mReceiverComponent;*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.my_location_fragment_layout, container, false);

        // Locate the UI widgets.
        mGetLocationSwitch = (Switch) rootView.findViewById(R.id.update_location_switch);

        mSaveSpotPanel = (LinearLayout) rootView.findViewById(R.id.save_spot_panel);
        mEvaluatePanel = (LinearLayout) rootView.findViewById(R.id.evaluate_panel);
        mArrivedPanel = (LinearLayout) rootView.findViewById(R.id.arrived_panel);
        mCurrentLocationPanel = (LinearLayout) rootView.findViewById(R.id.current_location_info_panel);

        mSaveSpotButton = (ImageButton) rootView.findViewById(R.id.save_hitchhiking_spot_button);
        mArrivedButton = (ImageButton) rootView.findViewById(R.id.arrived_button);
        mGotARideButton = (ImageButton) rootView.findViewById(R.id.got_a_ride_button);
        mTookABreakButton = (ImageButton) rootView.findViewById(R.id.break_button);

        mAccuracyTextView = (TextView) rootView.findViewById(R.id.accuracy_text);


        hitchability_ratingbar = (RatingBar) rootView.findViewById(R.id.spot_form_hitchability_ratingbar);
        mHitchabilityTextView = (TextView) rootView.findViewById(R.id.spot_form_hitchability_selectedvalue);

        mLatitudeTextView = (TextView) rootView.findViewById(R.id.latitude_text);
        mLongitudeTextView = (TextView) rootView.findViewById(R.id.longitude_text);
        mLastUpdateTimeTextView = (TextView) rootView.findViewById(R.id.last_update_time_text);
        mWaitingToGetCurrentLocationTextView = (TextView) rootView.findViewById(R.id.waiting_location_textview);
        //extra_image_button = (ImageButton) rootView.findViewById(R.id.extra_image_button);

        // Set UI labels.
        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mAccuracyLabel = getResources().getString(R.string.accuracy_label);
        mLastUpdateTimeLabel = getResources().getString(R.string.last_update_time_label);


        //mGetLocationSwitch.setEnabled(false);
        mSaveSpotPanel.setVisibility(View.GONE);//setEnabled(false);
        mArrivedPanel.setVisibility(View.GONE);
        mEvaluatePanel.setVisibility(View.GONE);//setEnabled(false);
        mCurrentLocationPanel.setVisibility(View.GONE);

        hitchability_ratingbar.setNumStars(Constants.hitchabilityNumOfOptions);
        hitchability_ratingbar.setStepSize(1);
        hitchability_ratingbar.setOnRatingBarChangeListener(this);
        mHitchabilityTextView.setText("");

        parentActivity = (TrackLocationBaseActivity) getActivity();

        mGetLocationSwitch.setOnCheckedChangeListener(this);
        mSaveSpotButton.setOnClickListener(this);
        mArrivedButton.setOnClickListener(this);
        mGotARideButton.setOnClickListener(this);
        mTookABreakButton.setOnClickListener(this);

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        Crashlytics.log(Log.INFO, TAG, "onResume was called");
        updateUI();
    }

    //RemoteControlReceiver receiver;
    Spot mCurrentWaitingSpot;
    boolean mIsWaitingForARide;
    boolean mWillItBeFirstSpotOfARoute;

    public void setValues(List<Spot> spotList, Spot currentWaitingSpot) {
        Crashlytics.log(Log.INFO, TAG, "setValues was called");
        try {
            mCurrentWaitingSpot = currentWaitingSpot;

            if (mCurrentWaitingSpot == null || mCurrentWaitingSpot.getIsWaitingForARide() == null)
                mIsWaitingForARide = false;
            else
                mIsWaitingForARide = mCurrentWaitingSpot.getIsWaitingForARide();


            mWillItBeFirstSpotOfARoute = spotList.size() == 0 || (spotList.get(0).getIsDestination() != null && spotList.get(0).getIsDestination());

        } catch (Exception ex) {
            Crashlytics.logException(ex);
            parentActivity.showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                    "Setting values of fragment 1 - " + ex.getMessage()));
        }

        if (this.isResumed())
            updateUI();
    }

    void updateUI() {
        Crashlytics.log(Log.INFO, TAG, "updateUI");
        try {
            if (!mIsWaitingForARide)
                hitchability_ratingbar.setRating(0);

            updateUILocationSwitch();
            updateUISaveButtons();
        } catch (Exception ex) {
            Crashlytics.logException(ex);
            parentActivity.showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                    "Updating UI on fragment 1 - " + ex.getMessage()));
        }
    }

    public Integer getSelectedHitchability() {
        return findTheOpposit(Math.round(hitchability_ratingbar.getRating()));
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
        Spot spot = null;
        int requestCode = BaseActivity.EDIT_SPOT_REQUEST;
        if (!mIsWaitingForARide) {
            requestCode = BaseActivity.SAVE_SPOT_REQUEST;
            spot = new Spot();
            spot.setIsDestination(isDestination);
            if (parentActivity.mCurrentLocation != null) {
                spot.setLatitude(parentActivity.mCurrentLocation.getLatitude());
                spot.setLongitude(parentActivity.mCurrentLocation.getLongitude());
                spot.setAccuracy(parentActivity.mCurrentLocation.getAccuracy());
                spot.setHasAccuracy(parentActivity.mCurrentLocation.hasAccuracy());
            }
            Crashlytics.log(Log.INFO, TAG, "Save spot button handler: a new spot is being created.");
        } else {
            spot = mCurrentWaitingSpot;
            Crashlytics.log(Log.INFO, TAG, "Save spot button handler: a spot is being edited.");
        }

        Intent intent = new Intent(getContext(), SpotFormActivity.class);
        intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
        startActivityForResult(intent, requestCode);
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
        mCurrentWaitingSpot.setHitchability(findTheOpposit(Math.round(hitchability_ratingbar.getRating())));

        if (mIsWaitingForARide) {
            Intent intent = new Intent(parentActivity.getApplicationContext(), SpotFormActivity.class);
            intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, mCurrentWaitingSpot);
            startActivityForResult(intent, BaseActivity.EDIT_SPOT_REQUEST);
        }
    }

    public void getLocationSwitchHandler(View view) {
        Switch s = (Switch) view;
        if (s.isChecked()) {
            parentActivity.mRequestingLocationUpdates = true;
            if (parentActivity.mGoogleApiClient.isConnected())
                parentActivity.startLocationUpdates();
            else
                Toast.makeText(getContext(), getResources().getString(R.string.waiting_for_connection), Toast.LENGTH_LONG);
        } else {
            parentActivity.mRequestingLocationUpdates = false;
            parentActivity.stopLocationUpdates();
        }
        updateUISaveButtons();
    }

    /**
     * Updates the latitude, the longitude, and the last location time in the UI.
     */
    protected void updateUILabels() {
        if (parentActivity.mCurrentLocation != null) {
            mLatitudeTextView.setText(String.format("%s: %f", mLatitudeLabel,
                    parentActivity.mCurrentLocation.getLatitude()));
            mLongitudeTextView.setText(String.format("%s: %f", mLongitudeLabel,
                    parentActivity.mCurrentLocation.getLongitude()));
            if (parentActivity.mCurrentLocation.hasAccuracy())
                mAccuracyTextView.setText(String.format("%s: %.2f m", mAccuracyLabel,
                        parentActivity.mCurrentLocation.getAccuracy()));
            mLastUpdateTimeTextView.setText(String.format("%s: %s", mLastUpdateTimeLabel,
                    dateToString(parentActivity.mLastUpdateTime)));
        }
    }

    protected void updateUILocationSwitch() {
        if (parentActivity.mGoogleApiClient == null || !parentActivity.mGoogleApiClient.isConnected()) {
            //mGetLocationSwitch.setEnabled(false);
        } else {
            //mGetLocationSwitch.setEnabled(true);
            mGetLocationSwitch.setChecked(parentActivity.mRequestingLocationUpdates);
            //updateUISaveButtons();
        }

    }


    private MapViewActivity.pageType currentPage;

    protected void showCurrentPage() {
        if (currentPage == MapViewActivity.pageType.WILL_BE_FIRST_SPOT_OF_A_ROUTE || currentPage == MapViewActivity.pageType.WILL_BE_REGULAR_SPOT) {
            mWaitingToGetCurrentLocationTextView.setVisibility(View.GONE);
            mSaveSpotPanel.setVisibility(View.VISIBLE);
            mCurrentLocationPanel.setVisibility(View.VISIBLE);
        }

        if (currentPage != MapViewActivity.pageType.WAITING_FOR_A_RIDE) {
            mGetLocationSwitch.setVisibility(View.VISIBLE);
            mEvaluatePanel.setVisibility(View.GONE);//setEnabled(false);
        }

        switch (currentPage) {
            case NOT_FETCHING_LOCATION:
            default:
                mWaitingToGetCurrentLocationTextView.setVisibility(View.VISIBLE);
                mSaveSpotPanel.setVisibility(View.GONE);//setEnabled(false);
                mCurrentLocationPanel.setVisibility(View.GONE);
                break;
            case WILL_BE_FIRST_SPOT_OF_A_ROUTE:
                mArrivedPanel.setVisibility(View.GONE);
                break;
            case WILL_BE_REGULAR_SPOT:
                mArrivedPanel.setVisibility(View.VISIBLE);
                break;
            case WAITING_FOR_A_RIDE:
                mGetLocationSwitch.setVisibility(View.GONE);
                mCurrentLocationPanel.setVisibility(View.GONE);
                mWaitingToGetCurrentLocationTextView.setVisibility(View.GONE);
                mSaveSpotPanel.setVisibility(View.GONE);//setEnabled(false);
                mEvaluatePanel.setVisibility(View.VISIBLE);//setEnabled(true);
                break;
        }
    }

    protected void updateUISaveButtons() {
        //If it's not waiting for a ride
        if (!mIsWaitingForARide) {
           /* if (parentActivity.mGoogleApiClient == null || parentActivity.mCurrentLocation == null || !parentActivity.mGoogleApiClient.isConnected()
                    || !parentActivity.mRequestingLocationUpdates) {
                currentPage = MapViewActivity.pageType.NOT_FETCHING_LOCATION;
            } else {
                if (parentActivity.mRequestingLocationUpdates) {*/
            if (mWillItBeFirstSpotOfARoute)
                currentPage = MapViewActivity.pageType.WILL_BE_FIRST_SPOT_OF_A_ROUTE;
            else
                currentPage = MapViewActivity.pageType.WILL_BE_REGULAR_SPOT;
            //    }
            // }


        } else {
            currentPage = MapViewActivity.pageType.WAITING_FOR_A_RIDE;
        }

        showCurrentPage();
    }


    private String dateToString(Date dt) {
        return DateFormat.getTimeInstance().format(dt);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save_hitchhiking_spot_button:
                saveRegularSpotButtonHandler();
                break;
            case R.id.arrived_button:
                saveDestinationSpotButtonHandler();
                break;
            case R.id.got_a_ride_button:
                gotARideButtonHandler();
                break;
            case R.id.break_button:
                tookABreakButtonHandler();
                break;

            default:
                //extra_image_button.setImageAlpha(255);
                if (mIsWaitingForARide) {
                    Toast.makeText(getContext(), getResources().getString(R.string.got_a_ride_button_text), Toast.LENGTH_LONG).show();
                    evaluateSpotButtonHandler();
                } else {
                    Toast.makeText(getContext(), getResources().getString(R.string.save_spot_button_text), Toast.LENGTH_LONG).show();
                    saveRegularSpotButtonHandler();
                }
                break;

        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.update_location_switch)
            getLocationSwitchHandler(buttonView);
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        mHitchabilityTextView.setText(getRatingString(Math.round(rating)));
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
}
