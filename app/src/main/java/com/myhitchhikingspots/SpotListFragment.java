package com.myhitchhikingspots;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
    Boolean isEditMode = null;
    static String SELECTED_SPOTS_KEY = "SELECTED_SPOTS_KEY";
    static String IS_EDIT_MODE_KEY = "IS_EDIT_MODE_KEY";
    static final String TAG = "spot-list-fragment";
    SpotListAdapter mAdapter;

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
    private SpotFormActivity.AddressResultReceiver mResultReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Retrieve saved state in onCreate. This method is called even when this fragment is on the back stack
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_SPOTS_KEY)) {
            previouslySelectedSpots = savedInstanceState.getIntegerArrayList(SELECTED_SPOTS_KEY);
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(IS_EDIT_MODE_KEY)) {
            isEditMode = savedInstanceState.getBoolean(IS_EDIT_MODE_KEY);
        }
    }


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
                                        String errorMessage = "";
                                        try {
                                            ArrayList<String> where = new ArrayList<>();
                                            for (int i = 0; i < mAdapter.getSelectedSpots().size(); i++)
                                                where.add(" " + SpotDao.Properties.Id.columnName + " = '" + mAdapter.getSelectedSpots().get(i) + "' ");

                                            String sqlDeleteStatement = "DELETE FROM %1$s WHERE %2$s";
                                            Database db = DaoMaster.newDevSession(getContext(), Constants.INTERNAL_DB_FILE_NAME).getDatabase();
                                            db.execSQL(String.format(sqlDeleteStatement,
                                                    SpotDao.TABLENAME,
                                                    TextUtils.join(" OR ", where)));

                                            ArrayList<String> spotsToBeDeleted_coordinateList = new ArrayList<>();
                                            List<Spot> newList = new ArrayList<>();
                                            for (int i = 0; i < spotList.size(); i++)
                                                //Add spot if it was not deleted
                                                if (!mAdapter.getSelectedSpots().contains(spotList.get(i).getId().intValue()))
                                                    newList.add(spotList.get(i));
                                                else {
                                                    //Add spot coordinate to the list of coordinates of deleted spots
                                                    spotsToBeDeleted_coordinateList.add(spotList.get(i).getLatitude() + "," + spotList.get(i).getLongitude());
                                                }

                                            //Create a record to track usage of Delete button when one or more spots is deleted
                                            Answers.getInstance().logCustom(new CustomEvent("Spots deleted")
                                                    .putCustomAttribute("Amount", mAdapter.getSelectedSpots().size())
                                                    .putCustomAttribute("Coordinates", TextUtils.join(";", spotsToBeDeleted_coordinateList)));

                                            //Clear selectedSpotsList
                                            mAdapter.setSelectedSpotsList(new ArrayList<Integer>());

                                            //Replace spotList with the list not containing the removed spots
                                            spotList = newList;

                                            mAdapter.setSpotList(spotList);
                                            setIsEditMode(false);
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
                                        } else
                                            //Let parent activity handle this
                                            if (onOneOrMoreSpotsDeleted != null)
                                                onOneOrMoreSpotsDeleted.onListOfSelectedSpotsChanged();
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
                }

                @Override
                public void onSpotClicked(Spot spot) {
                    Spot mCurrentWaitingSpot = ((MyHitchhikingSpotsApplication) getContext().getApplicationContext()).getCurrentSpot();

                    //If the user is currently waiting at a spot and the clicked spot is not the one he's waiting at, show a Toast.
                    if (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null &&
                            mCurrentWaitingSpot.getIsWaitingForARide()) {

                        if (mCurrentWaitingSpot.getId().equals(spot.getId()))
                            spot.setAttemptResult(null);
                        else {
                            Toast.makeText(getContext(), getResources().getString(R.string.evaluate_running_spot_required), Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    Bundle args = new Bundle();
                    //Maybe we should send mCurrentWaitingSpot on the intent.putExtra so that we don't need to call spot.setAttemptResult(null) ?
                    args.putSerializable(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
                    args.putBoolean(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, true);

                    Intent intent = new Intent(getContext(), SpotFormActivity.class);
                    intent.putExtras(args);
                    startActivityForResult(intent, BaseActivity.EDIT_SPOT_REQUEST);

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

        if (isEditMode != null)
            mAdapter.setIsEditMode(isEditMode);

        recyclerView.setAdapter(mAdapter);

        return rootView;
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
        if (mAdapter != null && mAdapter.getSelectedSpots().size() > 0)
            fabDelete.setVisibility(View.VISIBLE);
        else
            fabDelete.setVisibility(View.GONE);
    }

    public void setIsEditMode(Boolean isEditMode) {
        if (mAdapter != null)
            mAdapter.setIsEditMode(isEditMode);
    }

    public Boolean getIsEditMode() {
        if (mAdapter != null)
            return mAdapter.isEditMode;
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
            ((TrackLocationBaseActivity) getActivity()).showErrorAlert(getResources().getString(R.string.general_error_dialog_title), String.format(getResources().getString(R.string.general_error_dialog_message),
                    "Updating UI on fragment 2 - " + ex.getMessage()));
        }
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

   /* @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }*/

    /**
     * Runs when user clicks the Fetch Address button. Starts the service to fetch the address if
     * GoogleApiClient is connected.
     *
     public void fetchMissingAddressButtonHandler(View view) {

     Spot lastAddedSpot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getLastAddedRouteSpot();
     if (lastAddedSpot != null) {
     LatLng pinPosition = new LatLng(lastAddedSpot.getLatitude(), lastAddedSpot.getLongitude());

     // We only start the service to fetch the address if GoogleApiClient is connected.
     if (pinPosition == null)
     return;

     fetchAddress(new MyLocation(pinPosition.getLatitude(), pinPosition.getLongitude()));
     }else
     Toast.makeText(getContext(), "No address to be fetched.", Toast.LENGTH_LONG).show();
     }

     public void fetchAddress(MyLocation loc) {
     startIntentService(loc);

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
     *
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
     //updateUIWidgets();
     }


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

     mLocationAddressTextView.setText(getString(mCurrentSpot));

     }

     @NonNull private String getString(Spot mCurrentSpot) {
     String spotLoc = "";
     try {
     spotLoc = spotLocationToString(mCurrentSpot).trim();
     } catch (Exception ex) {
     Crashlytics.logException(ex);
     }
     return spotLoc;
     }*/
}
