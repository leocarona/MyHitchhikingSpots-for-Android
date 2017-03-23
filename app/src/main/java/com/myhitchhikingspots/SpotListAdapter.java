package com.myhitchhikingspots;

import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.model.Spot;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

/**
 * Created by leocarona on 04/03/2016.
 */
public class SpotListAdapter extends RecyclerView.Adapter<SpotListAdapter.ViewHolder> {
    protected static final String TAG = "spot-list-adapter";
    private List<Spot> mData;
    private SpotListFragment spotListFragment;
    public static Hashtable<Long, String> totalsToDestinations;

    public SpotListAdapter(List<Spot> data, SpotListFragment spotListFragment) {
        this.mData = data;
        this.spotListFragment = spotListFragment;

        SumRouteTotalsAndUpdateTheirDestinationNotes(data);
    }


    private void SumRouteTotalsAndUpdateTheirDestinationNotes(List<Spot> data) {
        Crashlytics.log(Log.INFO, TAG, "Summing up the total of rides gotten and hours traveling");
        totalsToDestinations = new Hashtable<>();
        //Integer totalWaitingTimeMinutes = 0;
        Integer totalRides = 0;
        Date startDate = null;

        if (data.size() > 1)
            startDate = data.get(data.size() - 1).getStartDateTime();

        //The spots are ordered from the last saved ones to the first saved ones, so we need to
        // go through the list in the oposite direction in order to sum up the route's totals from their origin to their destinations
        for (int i = data.size() - 1; i >= 0; i--) {
            try {
                Spot spot = data.get(i);
                if (spot.getIsDestination() == null || !spot.getIsDestination()) {
                    /*if (spot.getWaitingTime() != null)
                        totalWaitingTimeMinutes += spot.getWaitingTime();*/

                    //If user gave up on hitchhiking on this spot, then we must not count it as a ride
                    if (spot.getAttemptResult() == null || spot.getAttemptResult() != Constants.ATTEMPT_RESULT_TOOK_A_BREAK)
                        totalRides++;
                } else {
                    Integer minutes = 0;

                    if (startDate != null) {
                        DateTime startDateTime = new DateTime(startDate);
                        DateTime endDateTime = new DateTime(spot.getStartDateTime());
                        minutes = Minutes.minutesBetween(startDateTime, endDateTime).getMinutes();
                    }

                    String formatedStr = String.format(spotListFragment.getResources().getString(R.string.destination_spot_totals_format),
                            totalRides, getWaitingTimeAsString(minutes));
                    totalsToDestinations.put(spot.getId(), formatedStr);

                    //totalWaitingTimeMinutes = 0;
                    totalRides = 0;
                    startDate = null;

                    if (i - 1 >= 0)
                        startDate = data.get(i - 1).getStartDateTime();
                }
            } catch (Exception ex) {
                Crashlytics.logException(ex);
            }
        }
    }

    @Override
    public SpotListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View itemLayoutView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.spot_list_item, null);

        // create ViewHolder

        ViewHolder viewHolder = new SpotListAdapter.ViewHolder(itemLayoutView, spotListFragment);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Spot spot = mData.get(position);
        viewHolder.setFields(spot);
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
            Crashlytics.logException(ex);
        }
        return "";
    }

    private static String dateTimeToString(Date dt) {
        SimpleDateFormat res;

        String dateFormat = "dd/MM'\n'HH:mm";
        if (Locale.getDefault() == Locale.US)
            dateFormat = "MM/dd'\n'HH:mm";

        try {
            res = new SimpleDateFormat(dateFormat);
            return res.format(dt);
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }

        return "";
    }

    @NonNull
    public static String getWaitingTimeAsString(Integer waitingTime) {
        GregorianCalendar myDate = new GregorianCalendar(0, 0, 0, 0, 0, 0);
        myDate.add(GregorianCalendar.MINUTE, waitingTime);
        String dateFormated = "";

       /* if (myDate.get(Calendar.DAY_OF_WEEK_IN_MONTH) > 0) {
            dateFormated += myDate.get(Calendar.DAY_OF_WEEK_IN_MONTH) + "d";
            if (myDate.get(Calendar.HOUR_OF_DAY) > 0 || myDate.get(Calendar.MINUTE) > 0)
                dateFormated += myDate.get(Calendar.HOUR_OF_DAY) + "h";
        } else */
        if (myDate.get(Calendar.HOUR_OF_DAY) > 0) {
            /*if (myDate.get(Calendar.HOUR_OF_DAY) <= 9)
                dateFormated += "0";*/
            dateFormated += myDate.get(Calendar.HOUR_OF_DAY) + " h";
        }

        if (!dateFormated.isEmpty())
            dateFormated += " ";

        if (myDate.get(Calendar.MINUTE) <= 9)
            dateFormated += "0";

        dateFormated += myDate.get(Calendar.MINUTE) + " min";

        return dateFormated;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView dateTime, cityNameText, notesText, waitingTimeText;
        public ImageView waitingIcon, arrivalIcon, pauseIcon;
        public SpotListFragment spotListFragment;
        public Spot spot;
        public View viewParent;

        public ViewHolder(View itemLayoutView, SpotListFragment spotListFragment) {
            super(itemLayoutView);
            this.spotListFragment = spotListFragment;

            dateTime = (TextView) itemLayoutView.findViewById(R.id.date_time_layout_textview);
            cityNameText = (TextView) itemLayoutView.findViewById(R.id.spot_city_name_layout_textview);
            notesText = (TextView) itemLayoutView.findViewById(R.id.spot_notes_layout_textview);
            waitingTimeText = (TextView) itemLayoutView.findViewById(R.id.waiting_time_layout_textview);
            waitingIcon = (ImageView) itemLayoutView.findViewById(R.id.waiting_icon_layout_imageview);
            arrivalIcon = (ImageView) itemLayoutView.findViewById(R.id.arrival_icon_layout_imageview);
            pauseIcon = (ImageView) itemLayoutView.findViewById(R.id.break_icon_layout_imageview);

            viewParent = itemLayoutView.findViewById(R.id.spot_list_item_parent);
            viewParent.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Spot mCurrentWaitingSpot = ((MyHitchhikingSpotsApplication) spotListFragment.getActivity().getApplicationContext()).getCurrentSpot();

            //If the user is currently waiting at a spot and the clicked spot is not the one he's waiting at, show a Toast.
            if (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null &&
                    mCurrentWaitingSpot.getIsWaitingForARide()) {

                if (mCurrentWaitingSpot.getId() == spot.getId())
                    spot.setAttemptResult(null);
                else {
                    Toast.makeText(spotListFragment.getActivity(), viewParent.getResources().getString(R.string.evaluate_running_spot_required), Toast.LENGTH_LONG).show();
                    return;
                }
            }

            Intent intent = new Intent(spotListFragment.getActivity(), SpotFormActivity.class);
            //Maybe we should send mCurrentWaitingSpot on the intent.putExtra so that we don't need to call spot.setAttemptResult(null) ?
            intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
            spotListFragment.startActivity(intent);
        }

        public void setFields(Spot spot) {
            try {
                this.spot = spot;

                String captilizedNote = "";
                if (spot.getNote() != null)
                    captilizedNote = spot.getNote();

                if (spot.getIsDestination() != null && spot.getIsDestination()) {
                    //ARRIVAL SPOT
                    viewParent.setBackgroundColor(ContextCompat.getColor(viewParent.getContext(), R.color.ic_arrival_color));
                    arrivalIcon.setVisibility(View.VISIBLE);
                    waitingTimeText.setVisibility(View.GONE);
                    waitingIcon.setVisibility(View.GONE);
                    captilizedNote = totalsToDestinations.get(spot.getId());
                } else if (spot.getIsWaitingForARide() != null && spot.getIsWaitingForARide()) {
                    //USER IS WAITING FOR A RIDE
                    viewParent.setBackgroundColor(ContextCompat.getColor(viewParent.getContext(), R.color.ic_regular_spot_color));
                    arrivalIcon.setVisibility(View.GONE);
                    waitingTimeText.setVisibility(View.GONE);
                    waitingIcon.setVisibility(View.VISIBLE);
                } else {
                    viewParent.setBackgroundColor(Color.TRANSPARENT);
                    Integer waitingTime = 0;
                    if (spot.getWaitingTime() != null)
                        waitingTime = spot.getWaitingTime();
                    waitingTimeText.setText(getWaitingTimeAsString(waitingTime));
                    arrivalIcon.setVisibility(View.GONE);
                    waitingTimeText.setVisibility(View.VISIBLE);
                    waitingIcon.setVisibility(View.GONE);
                }

                if (spot.getStartDateTime() != null)
                    dateTime.setText(dateTimeToString(spot.getStartDateTime()));

                String spotLoc = getString(spot);
                cityNameText.setText(spotLoc);

                if (captilizedNote != null && !captilizedNote.isEmpty())
                    captilizedNote = captilizedNote.substring(0, 1).toUpperCase() + captilizedNote.substring(1);

                if (spot.getAttemptResult() != null && spot.getAttemptResult() == Constants.ATTEMPT_RESULT_TOOK_A_BREAK
                        && (spot.getIsWaitingForARide() == null || !spot.getIsWaitingForARide()))
                    pauseIcon.setVisibility(View.VISIBLE);
                else
                    pauseIcon.setVisibility(View.GONE);
                //captilizedNote = viewParent.getResources().getString(R.string.break_spot_state_label) + " - " + captilizedNote;

                notesText.setText(captilizedNote);
            } catch (Exception ex) {
                Crashlytics.logException(ex);
            }
        }


        @NonNull
        private static String getString(Spot mCurrentSpot) {
            String spotLoc = spotLocationToString(mCurrentSpot).trim();
            if (spotLoc != null && !spotLoc.isEmpty())
                spotLoc = "- " + spotLoc;
            else if (mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null)
                spotLoc = "- (" + mCurrentSpot.getLatitude() + "," + mCurrentSpot.getLongitude() + ")";
            return spotLoc;
        }
    }
}
