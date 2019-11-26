package com.myhitchhikingspots;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Address;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.interfaces.ListListener;
import com.myhitchhikingspots.model.SpotDao;

import org.greenrobot.greendao.database.Database;

import java.util.ArrayList;
import java.util.List;

public class SpotListFragment extends Fragment {
    private RecyclerView recyclerView;
    List<Spot> spotList = new ArrayList<>();
    FloatingActionButton fabDelete;
    public ArrayList<Integer> previouslySelectedSpots = new ArrayList<>();
    static String SELECTED_SPOTS_KEY = "SELECTED_SPOTS_KEY";
    static String IS_EDIT_MODE_KEY = "IS_EDIT_MODE_KEY";
    static final String TAG = "spot-list-fragment";
    SpotListAdapter mAdapter;
    private boolean isHandlingRequestToOpenSpotForm = false;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Retrieve saved state in onCreate. This method is called even when this fragment is on the back stack
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_SPOTS_KEY)) {
            previouslySelectedSpots = savedInstanceState.getIntegerArrayList(SELECTED_SPOTS_KEY);
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(IS_EDIT_MODE_KEY)) {
            setIsEditMode(savedInstanceState.getBoolean(IS_EDIT_MODE_KEY));
        }
    }

    final String sqlDeleteStatement = "DELETE FROM %1$s WHERE %2$s";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.spot_list_fragment_layout, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.main_activity_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fabDelete = (FloatingActionButton) rootView.findViewById(R.id.fab_delete_action);

        fabDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(getContext())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getString(R.string.spot_form_delete_dialog_title_text))
                        .setMessage(getString(R.string.spot_form_delete_dialog_message_for_many_text, mAdapter.getSelectedSpots().size()))
                        .setPositiveButton(String.format(getString(R.string.spot_form_delete_dialog_yes_for_many_option), mAdapter.getSelectedSpots().size()),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteSelectedSpots();
                                    }
                                })
                        .setNegativeButton(getResources().getString(R.string.spot_form_delete_dialog_no_option), null)
                        .show();
            }
        });

        //When we go to next fragment and return back here, the adapter is already present and populated.
        //Don't create it again in such cases. Hence the null check.
        if (mAdapter == null) {
            ListListener listener = new ListListener() {
                @Override
                public void onListOfSelectedSpotsChanged() {
                    //Show or hide delete button. When one or more spot are delete, onOneOrMoreSpotsDeleted.onListOfSelectedSpotsChanged() is fired
                    updateDeleteButtons();
                    Activity activity = getActivity();
                    if(activity != null) activity.invalidateOptionsMenu();
                    isHandlingRequestToOpenSpotForm = false;
                }

                @Override
                public void onSpotClicked(Spot spot) {
                    if (isHandlingRequestToOpenSpotForm)
                        return;
                    Spot mCurrentWaitingSpot = ((MyHitchhikingSpotsApplication) getContext().getApplicationContext()).getCurrentSpot();

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
                    Intent intent = new Intent(getContext(), SpotFormActivity.class);
                    intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
                    intent.putExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, true);
                    startActivityForResult(intent, Constants.EDIT_SPOT_REQUEST);

                    if (onOneOrMoreSpotsDeleted != null)
                        onOneOrMoreSpotsDeleted.onSpotClicked(spot);
                }
            };

            mAdapter = new SpotListAdapter(listener, getActivity());
        }

        //Use the state retrieved in onCreate and set it on your views etc in onCreateView
        //This method is not called if the device is rotated when your fragment is on the back stack.
        //That's OK since the next time the device is rotated, we save the state we had retrieved in onCreate
        //instead of saving current state. See getSelectedSpots for more details.
        if (previouslySelectedSpots != null)
            mAdapter.setSelectedSpotsList(previouslySelectedSpots);

        recyclerView.setAdapter(mAdapter);

        return rootView;
    }

    public boolean getIsAllSpotsSelected() {
        if(mAdapter != null)
            return mAdapter.getIsAllSpotsSelected();
        return false;
    }

    public boolean getIsOneOrMoreSpotsSelected() {
        if(mAdapter != null)
           return mAdapter.getIsOneOrMoreSpotsSelected();
        return false;
    }

    public void selectAllSpots() {
        if(mAdapter != null)
            mAdapter.selectAllSpots();
    }

    public void deselectAllSpots() {
        if(mAdapter != null)
            mAdapter.deselectAllSpots();
    }

    private void deleteSelectedSpots() {
        String errorMessage = "";
        try {
            Spot mCurrentWaitingSpot = ((MyHitchhikingSpotsApplication) getContext().getApplicationContext()).getCurrentSpot();
            Boolean isWaitingForARide = mCurrentWaitingSpot != null &&
                    mCurrentWaitingSpot.getIsWaitingForARide() != null && mCurrentWaitingSpot.getIsWaitingForARide();
            ArrayList<String> spotsToBeDeleted_idList = new ArrayList<>();

            for (int i = 0; i < mAdapter.getSelectedSpots().size(); i++) {
                Integer selectedSpotId = mAdapter.getSelectedSpots().get(i);

                //If the user is currently waiting at a spot and the clicked spot is not the one he's waiting at, show a Toast.
                if (isWaitingForARide && mCurrentWaitingSpot.getId().intValue() == selectedSpotId)
                    ((MyHitchhikingSpotsApplication) getContext().getApplicationContext()).setCurrentSpot(null);

                //Concatenate Id in a list as "Id.columnName = x"
                spotsToBeDeleted_idList.add(" " + SpotDao.Properties.Id.columnName + " = '" + selectedSpotId + "' ");
            }

            //Get a DB session
            Database db = DaoMaster.newDevSession(getContext(), Constants.INTERNAL_DB_FILE_NAME).getDatabase();

            //Delete selected spots from DB
            db.execSQL(String.format(sqlDeleteStatement,
                    SpotDao.TABLENAME,
                    TextUtils.join(" OR ", spotsToBeDeleted_idList)));

            List<Spot> remainingSpots = new ArrayList<>();

            //Go through all the spots in the list
            for (int i = 0; i < spotList.size(); i++) {
                //Check if this spot was in the list to be deleted
                if (!mAdapter.getSelectedSpots().contains(spotList.get(i).getId().intValue())) {
                    //Add spot to remaining list if it was not selected to be deleted
                    remainingSpots.add(spotList.get(i));
                } else {
                    //Create recordd to track usage of Delete button for each spot deleted
                    Answers.getInstance().logCustom(new CustomEvent("Spot deleted"));
                }
            }

            setIsEditMode(false);

            //Replace spotList with the list not containing the removed spots, and call updateUI
            setValues(remainingSpots);

            //Clear selectedSpotsList
            deselectAllSpots();

            mAdapter.notifyDataSetChanged();
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

    ListListener onOneOrMoreSpotsDeleted;

    void setOnOneOrMoreSpotsDeleted(ListListener listListener) {
        this.onOneOrMoreSpotsDeleted = listListener;
    }

    @Override
    public void onResume() {
        super.onResume();
        Crashlytics.log(Log.INFO, TAG, "onResume was called");
        updateUI();
    }

    void updateDeleteButtons() {
        if(fabDelete == null)
            return;

        if (mAdapter != null && mAdapter.getSelectedSpots().size() > 0 && getIsEditMode())
            fabDelete.show();
        else
            fabDelete.hide();
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

    void updateUI() {
        Crashlytics.log(Log.INFO, TAG, "updateUI was called");
        try {
            if (recyclerView != null) {
                updateDeleteButtons();
                mAdapter.setSpotList(spotList);
            }
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

    public void setValues(List list) {
        Crashlytics.log(Log.INFO, TAG, "setValues was called");
        spotList = list;

        if (this.isResumed())
            updateUI();
    }

    public void onActivityResultFromSpotForm() {
        isHandlingRequestToOpenSpotForm = false;
    }
}
