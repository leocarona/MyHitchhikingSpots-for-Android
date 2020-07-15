package com.myhitchhikingspots;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.databinding.SpotListFragmentLayoutBinding;
import com.myhitchhikingspots.interfaces.ListListener;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import java.util.ArrayList;
import java.util.List;

public class SpotListFragment extends Fragment {
    MediatorLiveData<List<Spot>> spotList;
    public ArrayList<Integer> previouslySelectedSpots = new ArrayList<>();
    static String SELECTED_SPOTS_KEY = "SELECTED_SPOTS_KEY";
    static String IS_EDIT_MODE_KEY = "IS_EDIT_MODE_KEY";
    static final String TAG = "spot-list-fragment";
    SpotListAdapter mAdapter;
    private boolean isHandlingRequestToOpenSpotForm = false;

    private SharedPreferences prefs;

    final String sqlDeleteStatement = "DELETE FROM %1$s WHERE %2$s";
    SpotsListViewModel spotsListViewModel;
    MyRoutesViewModel myRoutesViewModel;
    SpotListFragmentLayoutBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        spotList = new MediatorLiveData<>();

        //Retrieve saved state in onCreate. This method is called even when this fragment is on the back stack
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_SPOTS_KEY)) {
            previouslySelectedSpots = savedInstanceState.getIntegerArrayList(SELECTED_SPOTS_KEY);
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(IS_EDIT_MODE_KEY)) {
            setIsEditMode(savedInstanceState.getBoolean(IS_EDIT_MODE_KEY));
        }

        if (getContext() != null)
            prefs = getContext().getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        spotsListViewModel = new ViewModelProvider(requireActivity()).get(SpotsListViewModel.class);
        myRoutesViewModel = new ViewModelProvider(requireActivity()).get(MyRoutesViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.spot_list_fragment_layout, container, false);

        binding.fabDeleteAction.setOnClickListener(view -> {
            new AlertDialog.Builder(requireContext())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.spot_form_delete_dialog_title_text))
                    .setMessage(getString(R.string.spot_form_delete_dialog_message_for_many_text, mAdapter.getSelectedSpots().size()))
                    .setPositiveButton(String.format(getString(R.string.spot_form_delete_dialog_yes_for_many_option), mAdapter.getSelectedSpots().size()),
                            (dialog, which) -> deleteSelectedSpots())
                    .setNegativeButton(getResources().getString(R.string.spot_form_delete_dialog_no_option), null)
                    .show();
        });

        setupRecyclerView();

        spotList.observe(requireActivity(), this::updateUI);

        if (type == MyRoutesSpotsType.SINGLESPOTS)
            subscribeToSingleSpotsChange();
        else
            subscribeToRouteSpotsChange();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        mAdapter = new SpotListAdapter(listener, requireActivity());

        //Use the state retrieved in onCreate and set it on your views etc in onCreateView
        //This method is not called if the device is rotated when your fragment is on the back stack.
        //That's OK since the next time the device is rotated, we save the state we had retrieved in onCreate
        //instead of saving current state. See getSelectedSpots for more details.
        if (previouslySelectedSpots != null)
            mAdapter.setSelectedSpotsList(previouslySelectedSpots);

        binding.mainActivityRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.mainActivityRecyclerView.setAdapter(mAdapter);
    }

    ListListener listener = new ListListener() {
        @Override
        public void onListOfSelectedSpotsChanged() {
            //Show or hide delete button. When one or more spot are delete, onOneOrMoreSpotsDeleted.onListOfSelectedSpotsChanged() is fired
            updateDeleteButtons();
            Activity activity = getActivity();
            if (activity != null) activity.invalidateOptionsMenu();
            isHandlingRequestToOpenSpotForm = false;
        }

        @Override
        public void onSpotClicked(Spot spot) {
            if (isHandlingRequestToOpenSpotForm)
                return;
            Spot mCurrentWaitingSpot = spotsListViewModel.getWaitingSpot().getValue();

            //If the user is currently waiting at a spot and the clicked spot is not the one he's waiting at, show a Toast.
            if (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null &&
                    mCurrentWaitingSpot.getIsWaitingForARide()) {

                if (mCurrentWaitingSpot.getId().equals(spot.getId()))
                    spot.setAttemptResult(null);
                else {
                    Resources res = getResources();
                    String actionRequiredText = res.getString(R.string.evaluate_running_spot_required, res.getString(R.string.got_a_ride_button_text), res.getString(R.string.break_button_text));
                    Toast.makeText(getContext(), actionRequiredText, Toast.LENGTH_LONG).show();
                    return;
                }
            }

            isHandlingRequestToOpenSpotForm = true;
            ((MainActivity) requireActivity()).navigateToEditSpotForm(spot, -1);

            if (onOneOrMoreSpotsDeleted != null)
                onOneOrMoreSpotsDeleted.onSpotClicked(spot);
        }
    };


    public boolean getIsAllSpotsSelected() {
        if (mAdapter != null)
            return mAdapter.getIsAllSpotsSelected();
        return false;
    }

    public void selectAllSpots() {
        if (mAdapter != null)
            mAdapter.selectAllSpots();
    }

    public void deselectAllSpots() {
        if (mAdapter != null)
            mAdapter.deselectAllSpots();
    }

    private void deleteSelectedSpots() {
        String errorMessage = "";
        try {
            deleteSelectedSpots(mAdapter.getSelectedSpots());
            spotsListViewModel.reloadSpots(requireActivity());

            if (prefs != null)
                prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, true).apply();

            /*List<Spot> remainingSpots = extractRemainingSpots(mAdapter.getSelectedSpots());

            setIsEditMode(false);

            //Replace spotList with the list not containing the removed spots, and call updateUI
            setValues(remainingSpots);

            //Clear selectedSpotsList
            deselectAllSpots();

            mAdapter.notifyDataSetChanged();*/
        } catch (Exception ex) {
            Crashlytics.logException(ex);
            errorMessage = "An error occurred: " + ex.getMessage();
        }

        if (!errorMessage.isEmpty()) {
            //if(!errorMessage.isEmpty()) {
            new AlertDialog.Builder(getContext())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.general_error_dialog_title))
                    .setMessage(errorMessage)
                    .setNeutralButton(getResources().getString(R.string.general_ok_option), null)
                    .show();
        }
    }

    /*@NonNull
    private List<Spot> extractRemainingSpots(ArrayList<Integer> selectedSpots) {
        List<Spot> remainingSpots = new ArrayList<>();

        //Go through all the spots in the list
        for (int i = 0; i < spotList.size(); i++) {
            //Check if this spot was in the list to be deleted
            if (!selectedSpots.contains(spotList.get(i).getId().intValue())) {
                //Add spot to remaining list if it was not selected to be deleted
                remainingSpots.add(spotList.get(i));
            } else {
                //Create recordd to track usage of Delete button for each spot deleted
                Answers.getInstance().logCustom(new CustomEvent("Spot deleted"));
            }
        }
        return remainingSpots;
    }*/

    private void deleteSelectedSpots(ArrayList<Integer> selectedSpots) {
        Spot mCurrentWaitingSpot = spotsListViewModel.getWaitingSpot().getValue();
        Boolean isWaitingForARide = mCurrentWaitingSpot != null &&
                mCurrentWaitingSpot.getIsWaitingForARide() != null && mCurrentWaitingSpot.getIsWaitingForARide();
        ArrayList<String> spotsToBeDeleted_idList = new ArrayList<>();

        for (int i = 0; i < selectedSpots.size(); i++) {
            Integer selectedSpotId = selectedSpots.get(i);

            //If the user is currently waiting at a spot and the clicked spot is not the one he's waiting at, show a Toast.
            if (isWaitingForARide && mCurrentWaitingSpot.getId().intValue() == selectedSpotId)
                spotsListViewModel.setWaitingSpot(null);

            //Concatenate Id in a list as "Id.columnName = x"
            spotsToBeDeleted_idList.add(" " + SpotDao.Properties.Id.columnName + " = '" + selectedSpotId + "' ");
        }

        spotsListViewModel.execSQL(getContext(), String.format(sqlDeleteStatement,
                SpotDao.TABLENAME,
                TextUtils.join(" OR ", spotsToBeDeleted_idList)));
    }

    ListListener onOneOrMoreSpotsDeleted;

    void setOnOneOrMoreSpotsDeleted(ListListener listListener) {
        this.onOneOrMoreSpotsDeleted = listListener;
    }

    void updateDeleteButtons() {
        if (mAdapter != null && mAdapter.getSelectedSpots().size() > 0 && getIsEditMode())
            binding.fabDeleteAction.show();
        else
            binding.fabDeleteAction.hide();
    }

    public void setIsEditMode(Boolean isEditMode) {
        if (mAdapter != null)
            mAdapter.setIsEditMode(isEditMode);

        updateDeleteButtons();
    }

    public Boolean getIsEditMode() {
        if (mAdapter != null)
            return mAdapter.getIsEditMode();
        return false;
    }

    void updateUI(List<Spot> spots) {
        Crashlytics.log(Log.INFO, TAG, "updateUI was called");
        try {
            setIsEditMode(false);
            //Clear selectedSpotsList
            deselectAllSpots();
            mAdapter.setSpotList(spots);
        } catch (Exception ex) {
            Crashlytics.logException(ex);
            showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                    "Updating UI on fragment 2 - " + ex.getMessage()));
        }
    }

    protected void showErrorAlert(String title, String msg) {
        new AlertDialog.Builder(getContext())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.general_ok_option), null)
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAdapter != null) {
            //This case is for when the fragment is at the top of the stack. onCreateView was called and hence there is state to save
            previouslySelectedSpots = mAdapter.getSelectedSpots();
        }

        //However, remember that this method is called when the device is rotated even if your fragment is on the back stack.
        //In such cases, the onCreateView was not called, hence there is nothing to save.
        //Hence, we just re-save the state that we had retrieved in onCreate. We sort of relay the state from onCreate to getSelectedSpots.
        outState.putIntegerArrayList(SELECTED_SPOTS_KEY, previouslySelectedSpots);

        outState.putBoolean(IS_EDIT_MODE_KEY, getIsEditMode());

    }

    public enum MyRoutesSpotsType {ROUTESPOTS, SINGLESPOTS}

    MyRoutesSpotsType type;

    public void subscribeTo(MyRoutesSpotsType type) {
        this.type = type;
    }

    void subscribeToRouteSpotsChange() {
        Crashlytics.log(Log.INFO, TAG, "setValues was called");
        myRoutesViewModel.getRouteSpots().observe(requireActivity(), lst -> spotList.setValue(lst));
    }

    void subscribeToSingleSpotsChange() {
        Crashlytics.log(Log.INFO, TAG, "setValues was called");
        myRoutesViewModel.getSingleSpots().observe(requireActivity(), lst -> spotList.setValue(lst));
    }

    public void onActivityResultFromSpotForm() {
        isHandlingRequestToOpenSpotForm = false;
    }
}
