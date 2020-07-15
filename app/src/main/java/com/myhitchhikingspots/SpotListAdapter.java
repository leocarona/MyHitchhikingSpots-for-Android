package com.myhitchhikingspots;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.databinding.SpotListItemBinding;
import com.myhitchhikingspots.interfaces.CheckboxListener;
import com.myhitchhikingspots.interfaces.ListListener;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.SpotsListHelper;
import com.myhitchhikingspots.utilities.Utils;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by leocarona on 04/03/2016.
 */
public class SpotListAdapter extends RecyclerView.Adapter<SpotListAdapter.ViewHolder> implements ListListener {
    protected static final String TAG = "spot-list-adapter";
    private List<Spot> mData = new ArrayList<>();
    private Activity activity;
    public final Hashtable<Long, String> totalsToDestinations = new Hashtable<>();
    public ArrayList<Integer> selectedSpots = new ArrayList<>();
    ListListener onSelectedSpotsListChangedListener;

    public SpotListAdapter(ListListener listListener, Activity activity) {
        this.activity = activity;
        this.onSelectedSpotsListChangedListener = listListener;
    }

    void setSpotList(List<Spot> spotsList) {
        if (spotsList == null)
            mData = new ArrayList<>();
        else
            mData = spotsList;

        totalsToDestinations.clear();
        Crashlytics.log(Log.INFO, TAG, "Summing up the total of rides gotten and hours traveling");
        totalsToDestinations.putAll(SpotsListHelper.SumRouteTotalsAndUpdateTheirDestinationNotes(activity, mData));
    }

    public boolean getIsOneOrMoreSpotsSelected() {
        return !selectedSpots.isEmpty();
    }

    public boolean getIsAllSpotsSelected() {
        if (mData.isEmpty())
            return false;
        boolean isAllSpotsSelected = true;
        for (Spot spot : mData) {
            Integer spotId = spot.getId().intValue();
            if (!selectedSpots.contains(spotId)) {
                isAllSpotsSelected = false;
                break;
            }
        }
        return isAllSpotsSelected;
    }

    private void selectSpot(Spot spot) {
        Integer spotId = spot.getId().intValue();
        if (!selectedSpots.contains(spotId))
            selectedSpots.add(spotId);

        onListOfSelectedSpotsChanged();
    }

    private void deselectSpot(Spot spot) {
        //Find position on the list
        int index = selectedSpots.indexOf(spot.getId().intValue());

        //Remove it from previouslySelectedSpots
        if (index > -1)
            selectedSpots.remove(index);

        onListOfSelectedSpotsChanged();
    }

    private void selectAllSpots(List<Spot> spotList) {
        for (Spot spot : spotList) {
            Integer spotId = spot.getId().intValue();
            if (!selectedSpots.contains(spotId))
                selectedSpots.add(spotId);
        }
        notifyDataSetChanged();
        onListOfSelectedSpotsChanged();
    }

    public void selectAllSpots() {
        selectAllSpots(mData);
    }

    public void deselectAllSpots() {
        selectedSpots.clear();
        notifyDataSetChanged();
        onListOfSelectedSpotsChanged();
    }

    @NonNull
    @Override
    public SpotListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        SpotListItemBinding itemBinding = SpotListItemBinding.inflate(layoutInflater, parent, false);

        CheckboxListener onCheckedChanged = new CheckboxListener() {
            @Override
            public void notifySpotCheckedChanged(Spot spot, Boolean isChecked) {

                //If isChecked add spot id to previouslySelectedSpots
                if (isChecked)
                    selectSpot(spot);
                else
                    deselectSpot(spot);
            }

            @Override
            public void notifySpotClicked(Spot spot) {
                onSpotClicked(spot);
            }
        };

        SpotListAdapter.ViewHolder viewHolder = new SpotListAdapter.ViewHolder(itemBinding);
        viewHolder.setSpotCheckedChangedListener(onCheckedChanged);
        viewHolder.updateCheckboxVisibility();

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Spot spot = mData.get(position);

        String secondLine = "";
        if (spot.getIsDestination() != null && spot.getIsDestination())
            secondLine = totalsToDestinations.get(spot.getId());
        else if (spot.getNote() != null)
            secondLine = spot.getNote();

        //Find position on the list
        int index = -1;
        for (int i = 0; i < selectedSpots.size() && index == -1; i++) {
            if (selectedSpots.get(i).equals(spot.getId().intValue()))
                index = i;
        }

        viewHolder.setFields(spot, secondLine, index > -1, activity);
        viewHolder.updateCheckboxVisibility();
        viewHolder.bind(spot);
    }

    static String locationSeparator = ", ";

    private static String spotLocationToString(Spot spot) {

        ArrayList<String> loc = new ArrayList();
        try {
            //Show location string only if GpsResolved is set to true
            if (spot.getGpsResolved() != null && spot.getGpsResolved())
                loc = Utils.spotLocationToList(spot);

            //Join the strings
            return TextUtils.join(locationSeparator, loc);
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }
        return "";
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public void setSelectedSpotsList(ArrayList<Integer> selectedSpots) {
        this.selectedSpots = selectedSpots;
    }

    public ArrayList<Integer> getSelectedSpots() {
        return this.selectedSpots;
    }

    @Override
    public void onListOfSelectedSpotsChanged() {
        // Notify everybody that may be interested.
        if (onSelectedSpotsListChangedListener != null)
            onSelectedSpotsListChangedListener.onListOfSelectedSpotsChanged();
    }

    @Override
    public void onSpotClicked(Spot spot) {
        if (onSelectedSpotsListChangedListener != null)
            onSelectedSpotsListChangedListener.onSpotClicked(spot);
    }

    private Boolean isEditMode = false;

    public void setIsEditMode(Boolean isEditMode) {
        this.isEditMode = isEditMode;
        notifyDataSetChanged();
    }

    public boolean getIsEditMode() {
        return this.isEditMode;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

        public Spot spot;
        CheckboxListener itemListener = null;
        int ic_selected_bg_color, ic_regular_spot_color, ic_arrival_color;
        private final SpotListItemBinding binding;

        public ViewHolder(SpotListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            ic_selected_bg_color = ContextCompat.getColor(binding.getRoot().getContext(), R.color.ic_selected_bg_color);
            ic_regular_spot_color = ContextCompat.getColor(binding.getRoot().getContext(), R.color.ic_regular_spot_color);
            ic_arrival_color = ContextCompat.getColor(binding.getRoot().getContext(), R.color.ic_arrival_color);

            binding.spotListItemParent.setOnClickListener(this);
            binding.spotListItemParent.setOnLongClickListener((v) -> {
                if (!isEditMode)
                    setIsEditMode(true);
                setChecked(getIsEditMode());
                return true;
            });
        }

        public void bind(Spot item) {
            binding.setSpot(item);
            binding.executePendingBindings();
        }

        private void setChecked(boolean value) {
            binding.spotDeleteCheckbox.setChecked(value);
            updateCheckboxVisibility();
        }

        private void toggleChecked() {
            setChecked(!binding.spotDeleteCheckbox.isChecked());
        }

        @Override
        public void onClick(View v) {
            if (getIsEditMode())
                toggleChecked();
            else if (itemListener != null)
                itemListener.notifySpotClicked(spot);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView.getId() == R.id.spot_delete_checkbox && itemListener != null) {
                //Set background color
                if (isChecked)
                    buttonView.setBackgroundColor(ic_selected_bg_color);
                else
                    buttonView.setBackgroundColor(Color.TRANSPARENT);

                if (itemListener != null)
                    itemListener.notifySpotCheckedChanged(spot, isChecked);
            }
        }

        public void setSpotCheckedChangedListener(CheckboxListener itemListener) {
            this.itemListener = itemListener;
        }

        public void updateCheckboxVisibility() {
            if (getIsEditMode())
                binding.spotDeleteCheckbox.setVisibility(View.VISIBLE);
            else
                binding.spotDeleteCheckbox.setVisibility(View.GONE);
        }

        public void setFields(Spot spot, String secondLine, Boolean isChecked, Context context) {
            try {
                this.spot = spot;

                binding.arrivalIconLayoutImageview.setVisibility(View.GONE);
                binding.waitingIconLayoutImageview.setVisibility(View.GONE);
                binding.breakIconLayoutImageview.setVisibility(View.GONE);
                binding.singleIconLayoutImageview.setVisibility(View.GONE);
                binding.waitingTimeLayoutTextview.setVisibility(View.GONE);
                binding.spotListItemParent.setBackgroundColor(Color.TRANSPARENT);

                //Remove listener, apply checked/unchecked and set listener again
                binding.spotDeleteCheckbox.setOnCheckedChangeListener(null);
                binding.spotDeleteCheckbox.setChecked(isChecked);
                binding.spotDeleteCheckbox.setOnCheckedChangeListener(this);

                //Set background color
                if (isChecked)
                    binding.spotDeleteCheckbox.setBackgroundColor(ic_selected_bg_color);
                else
                    binding.spotDeleteCheckbox.setBackgroundColor(Color.TRANSPARENT);

                //String hitchability = "";

                //If spot belongs to a route (it's not a single spot)
                if (spot.getIsPartOfARoute() != null && spot.getIsPartOfARoute()) {

                    //If spot is a hitchhiking spot where the user is waiting for a ride
                    if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot() &&
                            spot.getIsWaitingForARide() != null && spot.getIsWaitingForARide()) {
                        //The spot is where the user is waiting for a ride
                        binding.spotListItemParent.setBackgroundColor(ic_regular_spot_color);
                        binding.waitingIconLayoutImageview.setVisibility(View.VISIBLE);

                    } else if (spot.getIsDestination() != null && spot.getIsDestination()) {
                        //The spot is a destination

                        binding.spotListItemParent.setBackgroundColor(ic_arrival_color);
                        binding.arrivalIconLayoutImageview.setVisibility(View.VISIBLE);

                    } else {
                        if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot()) {
                            binding.waitingTimeLayoutTextview.setVisibility(View.VISIBLE);

                            switch (spot.getAttemptResult()) {
                                case Constants.ATTEMPT_RESULT_GOT_A_RIDE:
                                default:

                                    //The spot is a hitchhiking spot that was already evaluated
                                    //icon = getGotARideIconForRoute(trips.size());

                                    //hitchability = Utils.getRatingOrDefaultAsString(activity.getContext(), spot.getHitchability() != null ? spot.getHitchability() : 0);
                                    break;
                                case Constants.ATTEMPT_RESULT_TOOK_A_BREAK:
                                    //The spot is a hitchhiking spot that was already evaluated
                                    //icon = ic_took_a_break_spot;
                                    binding.breakIconLayoutImageview.setImageResource(R.drawable.ic_break_spot_icon);
                                    binding.breakIconLayoutImageview.setVisibility(View.VISIBLE);
                                    binding.breakIconLayoutImageview.setAlpha((float) 1);
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
                            binding.breakIconLayoutImageview.setImageResource(R.drawable.ic_point_on_the_route_black_24dp);
                            binding.breakIconLayoutImageview.setVisibility(View.VISIBLE);
                            binding.breakIconLayoutImageview.setAlpha((float) 0.5);
                        }
                    }
                } else {
                    //This spot doesn't belong to a route (it's a single spot)

                    if (spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot()) {
                        binding.waitingTimeLayoutTextview.setVisibility(View.VISIBLE);

                        //if(spot.getHitchability() != null)
                        //  hitchability = Utils.getRatingOrDefaultAsString(context, Utils.findTheOpposite(spot.getHitchability()));

                        binding.singleIconLayoutImageview.setVisibility(View.VISIBLE);

                    } else {
                        binding.breakIconLayoutImageview.setImageResource(R.drawable.ic_point_on_the_route_black_24dp);
                        binding.breakIconLayoutImageview.setVisibility(View.VISIBLE);
                        binding.breakIconLayoutImageview.setAlpha((float) 0.5);
                    }
                }

                Integer waitingTime = 0;
                if (spot.getWaitingTime() != null)
                    waitingTime = spot.getWaitingTime();

                //Replace "a few secs" for "<01min" to shorten the space taken
                String waitingTimeStr = Utils.getWaitingTimeAsString(waitingTime, context);
                if (waitingTimeStr.equals(context.getString(R.string.general_seconds_label)))
                    waitingTimeStr = "<" + context.getString(R.string.general_minutes_label, 1);

                binding.waitingTimeLayoutTextview.setText(waitingTimeStr);

                //Set the date and time
                if (spot.getStartDateTime() != null) {
                    DateTime startDateTime = spot.getStartDateTime();
                    String dateTimeFormat = Utils.getDateTimeFormat(startDateTime, ",\n");
                    binding.dateTimeLayoutTextview.setText(Utils.dateTimeToString(startDateTime, dateTimeFormat));
                }

                //Set the address or coordinates
                String spotLoc = getString(spot);
                binding.spotCityNameLayoutTextview.setText(spotLoc);

                //Set the second line, show the first letter capitalized
                if (secondLine != null && !secondLine.isEmpty())
                    secondLine = secondLine.substring(0, 1).toUpperCase() + secondLine.substring(1);

                //Add hitchability to second line
                //if (!hitchability.isEmpty())
                //    secondLine = "(" + hitchability + ") " + secondLine;

                binding.spotNotesLayoutTextview.setText(secondLine);

            } catch (Exception ex) {
                Crashlytics.logException(ex);
            }
        }

        @NonNull
        private String getString(Spot mCurrentSpot) {
            String spotLoc = spotLocationToString(mCurrentSpot).trim();
            if (!spotLoc.isEmpty())
                spotLoc = "- " + spotLoc;
            else if (mCurrentSpot.getLatitude() != null && mCurrentSpot.getLongitude() != null)
                spotLoc = "- (" + mCurrentSpot.getLatitude() + "," + mCurrentSpot.getLongitude() + ")";
            return spotLoc;
        }
    }
}
