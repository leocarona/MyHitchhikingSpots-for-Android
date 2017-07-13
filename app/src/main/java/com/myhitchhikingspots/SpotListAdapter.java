package com.myhitchhikingspots;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import java.util.Date;
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
    public Hashtable<Long, String> totalsToDestinations;

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
                    if ((spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot()) &&
                            (spot.getIsPartOfARoute() != null && spot.getIsPartOfARoute()) &&
                            spot.getAttemptResult() == null || spot.getAttemptResult() != Constants.ATTEMPT_RESULT_TOOK_A_BREAK)
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

        String secondLine = "";
        if (spot.getIsDestination() != null && spot.getIsDestination())
            secondLine = totalsToDestinations.get(spot.getId());
        else if (spot.getNote() != null)
            secondLine = spot.getNote();

        viewHolder.setFields(spot, secondLine, spotListFragment.getContext());
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

    @NonNull
    public static String dateTimeToString(Date dt) {
        return dateTimeToString(dt, ", ");
    }

    @NonNull
    public static String dateTimeToString(Date dt, String separator) {
        if (dt != null) {
            SimpleDateFormat res;
            String dateFormat = "dd/MMM'" + separator + "'HH:mm";

            if (Locale.getDefault() == Locale.US)
                dateFormat = "MMM/dd'" + separator + "'HH:mm";

            try {
                res = new SimpleDateFormat(dateFormat);
                return res.format(dt);
            } catch (Exception ex) {
                Crashlytics.setString("date", dt.toString());
                Crashlytics.log(Log.WARN, "dateTimeToString", "Err msg: " + ex.getMessage());
                Crashlytics.logException(ex);
            }
        }
        return "";
    }

    @NonNull
    public static String getWaitingTimeAsString(Integer waitingTime) {
        int weeks = waitingTime / 7 / 24 / 60;
        int days = waitingTime / 24 / 60;
        int hours = waitingTime / 60 % 24;
        int minutes = waitingTime % 60;
        String format = "%02d";
        String dateFormated = "";

        if (weeks > 0)
            days = days % 7;

        if (weeks > 0)
            dateFormated += String.format(format, weeks) + "w";

        if ((days > 0 || hours > 0 || minutes > 0) && !dateFormated.isEmpty())
            dateFormated += " ";

        if (days > 0 || ((hours > 0 || minutes > 0) && !dateFormated.isEmpty()))
            dateFormated += String.format(format, days) + "d";

        if ((hours > 0 || minutes > 0) && !dateFormated.isEmpty())
            dateFormated += " ";

        if (hours > 0 || (minutes > 0 && !dateFormated.isEmpty()))
            dateFormated += String.format(format, hours) + "h";

        if (minutes > 0 && !dateFormated.isEmpty())
            dateFormated += " ";

        if (minutes > 0 || dateFormated.isEmpty())
            dateFormated += String.format(format, minutes) + "min";

        return dateFormated;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView dateTime, cityNameText, notesText, waitingTimeText;
        public ImageView waitingIcon, destinationIcon, singleSpotIcon, breakIcon;
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
            destinationIcon = (ImageView) itemLayoutView.findViewById(R.id.arrival_icon_layout_imageview);
            singleSpotIcon = (ImageView) itemLayoutView.findViewById(R.id.single_icon_layout_imageview);
            breakIcon = (ImageView) itemLayoutView.findViewById(R.id.break_icon_layout_imageview);


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

            Bundle args = new Bundle();
            //Maybe we should send mCurrentWaitingSpot on the intent.putExtra so that we don't need to call spot.setAttemptResult(null) ?
            args.putSerializable(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
            args.putBoolean(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, true);

            Intent intent = new Intent(spotListFragment.getActivity(), SpotFormActivity.class);
            intent.putExtras(args);
            spotListFragment.startActivityForResult(intent, BaseActivity.EDIT_SPOT_REQUEST);
        }

        public void setFields(Spot spot, String secondLine, Context context) {
            try {
                this.spot = spot;

                destinationIcon.setVisibility(View.GONE);
                waitingIcon.setVisibility(View.GONE);
                breakIcon.setVisibility(View.GONE);
                singleSpotIcon.setVisibility(View.GONE);
                waitingTimeText.setVisibility(View.GONE);
                viewParent.setBackgroundColor(Color.TRANSPARENT);


                //If spot belongs to a route (it's not a single spot)
                if (spot.getIsPartOfARoute() != null && spot.getIsPartOfARoute()) {

                    //If spot is a hitchhiking spot where the user is waiting for a ride
                    if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot() &&
                            spot.getIsWaitingForARide() != null && spot.getIsWaitingForARide()) {
                        //The spot is where the user is waiting for a ride
                        viewParent.setBackgroundColor(ContextCompat.getColor(viewParent.getContext(), R.color.ic_regular_spot_color));
                        waitingIcon.setVisibility(View.VISIBLE);

                    } else if (spot.getIsDestination() != null && spot.getIsDestination()) {
                        //The spot is a destination

                        viewParent.setBackgroundColor(ContextCompat.getColor(viewParent.getContext(), R.color.ic_arrival_color));
                        destinationIcon.setVisibility(View.VISIBLE);

                    } else {
                        if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot()) {
                            waitingTimeText.setVisibility(View.VISIBLE);

                            switch (spot.getAttemptResult()) {
                                case Constants.ATTEMPT_RESULT_GOT_A_RIDE:
                                default:

                                    //The spot is a hitchhiking spot that was already evaluated
                                    //icon = getGotARideIconForRoute(trips.size());
                                    break;
                                case Constants.ATTEMPT_RESULT_TOOK_A_BREAK:
                                    //The spot is a hitchhiking spot that was already evaluated
                                    //icon = ic_took_a_break_spot;
                                    breakIcon.setImageResource(R.drawable.ic_break_spot_icon);
                                    breakIcon.setVisibility(View.VISIBLE);
                                    breakIcon.setAlpha((float) 1);
                                    break;
                                /*default:
                                    //The spot is a hitchhiking spot that was not evaluated yet
                                    //icon = getGotARideIconForRoute(-1);
                                    //markerTitle = getString(R.string.map_infoview_spot_type_not_evaluated);
                                    breakIcon.setImageResource(R.drawable.ic_point_in_the_route_black_24dp);
                                    breakIcon.setVisibility(View.VISIBLE);
                                    breakIcon.setAlpha((float) 0.5);
                                    break;*/
                            }
                        } else {
                            //The spot belongs to a route but it's not a hitchhiking spot, neither a destination
                            breakIcon.setImageResource(R.drawable.ic_point_on_the_route_black_24dp);
                            breakIcon.setVisibility(View.VISIBLE);
                            breakIcon.setAlpha((float) 0.5);
                        }
                    }
                } else {
                    //This spot doesn't belong to a route (it's a single spot)

                    if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot()) {
                        waitingTimeText.setVisibility(View.VISIBLE);

                        //if(spot.getHitchability() != null)
                        //  hitchability = Utils.getRatingOrDefaultAsString(context, Utils.findTheOpposite(spot.getHitchability()));

                        singleSpotIcon.setVisibility(View.VISIBLE);

                    } else{
                        breakIcon.setImageResource(R.drawable.ic_point_on_the_route_black_24dp);
                        breakIcon.setVisibility(View.VISIBLE);
                        breakIcon.setAlpha((float) 0.5);
                    }
                }

                Integer waitingTime = 0;
                if (spot.getWaitingTime() != null)
                    waitingTime = spot.getWaitingTime();
                waitingTimeText.setText(getWaitingTimeAsString(waitingTime));

                //Set the date and time
                if (spot.getStartDateTime() != null)
                    dateTime.setText(dateTimeToString(spot.getStartDateTime(), ",\n"));

                //Set the address or coordinates
                String spotLoc = getString(spot);
                cityNameText.setText(spotLoc);

                //Set the second line, show the first letter capitalized
                if (secondLine != null && !secondLine.isEmpty())
                    secondLine = secondLine.substring(0, 1).toUpperCase() + secondLine.substring(1);

                notesText.setText(secondLine);
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
