package com.myhitchhikingspots;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.crashlytics.android.Crashlytics;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.myhitchhikingspots.interfaces.ListListener;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import java.util.ArrayList;
import java.util.List;


public class MyRoutesActivity extends AppCompatActivity {

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    CoordinatorLayout coordinatorLayout;
    static final String LAST_TAB_OPENED_KEY = "last-tab-opened-key";
    static final String TAG = "main-activity";
    ListListener spotsListListener = null;
    Spot mCurrentWaitingSpot;
    private boolean isHandlingRequestToOpenSpotForm = false;

    int indexOfLastOpenTab = 0;

    /**
     * Set shouldGoBackToPreviousActivity to true if instead of opening a new map, the action bar option should just finish current activity
     */
    Boolean shouldGoBackToPreviousActivity = false;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_routes_activity_layout);

        //mWaitingToGetCurrentLocationTextView = (TextView) findViewById(R.id.waiting_location_textview);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        prefs = getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        // Set a Toolbar to replace the ActionBar.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //savedInstanceState will be not null when a screen is rotated, for example. But will be null when activity is first created
        if (savedInstanceState != null && savedInstanceState.keySet().contains(LAST_TAB_OPENED_KEY))
            indexOfLastOpenTab = savedInstanceState.getInt(LAST_TAB_OPENED_KEY, 0);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        if (mSectionsPagerAdapter == null)
            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


        spotsListListener = new ListListener() {
            @Override
            public void onListOfSelectedSpotsChanged() {
                showSpotDeletedSnackbar();
                invalidateOptionsMenu();
                prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, true).apply();
            }

            @Override
            public void onSpotClicked(Spot spot) {
                indexOfLastOpenTab = mViewPager.getCurrentItem();
                //onSaveInstanceState will be executed right after onSpotClicked because when a spot is clicked, the fragment starts SpotFormActivity
            }
        };
    }

    public void selectTab(int tab_index) {
        mViewPager.setCurrentItem(tab_index);
    }

    Snackbar snackbar;

    void showSnackbar(@NonNull CharSequence text, CharSequence action, View.OnClickListener listener) {
        snackbar = Snackbar.make(coordinatorLayout, text.toString().toUpperCase(), Snackbar.LENGTH_LONG)
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

    void showSpotSavedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_saved_successfuly),
                String.format(getString(R.string.action_button_label), getString(R.string.view_map_button_label)), v -> {
                    if (shouldGoBackToPreviousActivity)
                        finish();
                    else {
                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
                        intent.putExtra(MainActivity.ARG_REQUEST_TO_OPEN_FRAGMENT, R.id.nav_my_map);
                        startActivity(intent);
                    }
                });
    }

    void showSpotDeletedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_deleted_successfuly),
                null, null);
    }

    List<Spot> mSpotList;

    @Override
    public void onResume() {
        super.onResume();
        Crashlytics.log(Log.INFO, TAG, "onResume called");

        loadValues();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (snackbar != null)
            snackbar.dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mViewPager != null)
            mViewPager.clearOnPageChangeListeners();
    }

    void loadValues() {
        Crashlytics.log(Log.INFO, TAG, "loadValues called");
        MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
        DaoSession daoSession = appContext.getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();

        mSpotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.IsPartOfARoute, SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();
        mCurrentWaitingSpot = appContext.getCurrentSpot();

        //Update fragments
        if (mSectionsPagerAdapter != null) {
            mSectionsPagerAdapter.setValues(mSpotList);
            mViewPager.setAdapter(mSectionsPagerAdapter);
            selectTab(indexOfLastOpenTab);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Boolean spotListWasChanged = false;
        if (resultCode == Constants.RESULT_OBJECT_ADDED || resultCode == Constants.RESULT_OBJECT_EDITED) {
            spotListWasChanged = true;
            showSpotSavedSnackbar();
        } else if (resultCode == Constants.RESULT_OBJECT_DELETED) {
            spotListWasChanged = true;
            showSpotDeletedSnackbar();
        }

        isHandlingRequestToOpenSpotForm = false;

        if (mSectionsPagerAdapter != null)
            mSectionsPagerAdapter.onActivityResultFromSpotForm();

        if (spotListWasChanged)
            prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, true).apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_routes_menu, menu);

        MenuItem item = menu.findItem(R.id.action_select_all);

        boolean isEditModeOn = mSectionsPagerAdapter.getIsEditMode(mViewPager.getCurrentItem());
        item.setVisible(isEditModeOn);

        if (isEditModeOn) {
            String itemTitle = getString(R.string.general_select_all);
            if (mSectionsPagerAdapter.getIsAllSpotsSelected(mViewPager.getCurrentItem()))
                itemTitle = getString(R.string.general_deselect_all);
            item.setTitle(itemTitle);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean selectionHandled = false;

        switch (item.getItemId()) {
            case R.id.action_new_spot:
                if (!isHandlingRequestToOpenSpotForm)
                    saveSpotButtonHandler(false);
                break;
            case R.id.action_select_all:
                if (mSectionsPagerAdapter != null) {
                    if (mSectionsPagerAdapter.getIsAllSpotsSelected(mViewPager.getCurrentItem())) {
                        mSectionsPagerAdapter.deselectAllSpots(mViewPager.getCurrentItem());
                    } else {
                        mSectionsPagerAdapter.selectAllSpots(mViewPager.getCurrentItem());
                    }
                }
                break;
            case R.id.action_edit_list:
                if (mSectionsPagerAdapter != null) {
                    switch (mViewPager.getCurrentItem()) {
                        case SectionsPagerAdapter.TAB_ROUTES_INDEX:
                            mSectionsPagerAdapter.toggleRoutesListEditMode();
                            break;
                        case SectionsPagerAdapter.TAB_SPOTS_INDEX:
                            mSectionsPagerAdapter.toggleSpotsListEditMode();
                            break;
                    }

                    //Call invalidateOptionsMenu so that it fires onCreateOptionsMenu and "Select all" option will be displayed.
                    invalidateOptionsMenu();
                }
                break;
        }

        if (selectionHandled)
            return true;
        else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        indexOfLastOpenTab = mViewPager.getCurrentItem();
        savedInstanceState.putInt(LAST_TAB_OPENED_KEY, indexOfLastOpenTab);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private SpotListFragment tab_route_spots_list;
        private SpotListFragment tab_single_spots_list;

        List<Spot> routeSpots = new ArrayList<>(), singleSpots = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        //Called before instantiateItem(..)
        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case TAB_ROUTES_INDEX:
                    SpotListFragment listFrag = new SpotListFragment();
                    Bundle args1 = new Bundle();
                    listFrag.setArguments(args1);
                    return listFrag;
                case TAB_SPOTS_INDEX:
                    SpotListFragment singleSpotsListFrag = new SpotListFragment();
                    Bundle args2 = new Bundle();
                    singleSpotsListFrag.setArguments(args2);
                    return singleSpotsListFrag;
            }
            return null;
        }

        // Here we can finally safely save a reference to the created
        // Fragment, no matter where it came from (either getItem() or
        // FragmentManger). Simply save the returned Fragment from
        // super.instantiateItem() into an appropriate reference depending
        // on the ViewPager position. This solution was copied from:
        // http://stackoverflow.com/a/29288093/1094261
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Crashlytics.log(Log.INFO, TAG, "SectionsPagerAdapter.instantiateItem called for position " + position);

            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            // save the appropriate reference depending on position
            switch (position) {
                case TAB_ROUTES_INDEX:
                    SpotListFragment tab_list = (SpotListFragment) createdFragment;
                    tab_list.setValues(routeSpots);
                    this.tab_route_spots_list = tab_list;
                    this.tab_route_spots_list.setOnOneOrMoreSpotsDeleted(spotsListListener);
                    break;
                case TAB_SPOTS_INDEX:
                    SpotListFragment tab_single_spots_list = (SpotListFragment) createdFragment;
                    tab_single_spots_list.setValues(singleSpots);
                    this.tab_single_spots_list = tab_single_spots_list;
                    this.tab_single_spots_list.setOnOneOrMoreSpotsDeleted(spotsListListener);
                    break;
            }
            return createdFragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        public final static int TAB_ROUTES_INDEX = 0;
        public final static int TAB_SPOTS_INDEX = 1;

        @Override
        public CharSequence getPageTitle(int position) {
            CharSequence res = null;
            switch (position) {
                case TAB_ROUTES_INDEX:
                    res = getString(R.string.main_activity_list_tab);
                    break;
                case TAB_SPOTS_INDEX:
                    res = getString(R.string.main_activity_single_spots_list_tab);
                    break;
            }
            return res;
        }

        public void setValues(List<Spot> lst) {
            Crashlytics.log(Log.INFO, TAG, "SectionsPagerAdapter.setValues called");
            try {
                routeSpots = new ArrayList<>();
                singleSpots = new ArrayList<>();
                for (Spot s : lst)
                    if (s.getIsPartOfARoute() == null || !s.getIsPartOfARoute())
                        singleSpots.add(s);
                    else
                        routeSpots.add(s);
            } catch (Exception ex) {
                Crashlytics.logException(ex);
            }

            try {
                if (tab_route_spots_list != null)
                    tab_route_spots_list.setValues(routeSpots);

                if (tab_single_spots_list != null)
                    tab_single_spots_list.setValues(singleSpots);
            } catch (Exception ex) {
                Crashlytics.logException(ex);
            }
        }

        public void toggleRoutesListEditMode() {
            if (tab_route_spots_list != null)
                tab_route_spots_list.setIsEditMode(!tab_route_spots_list.getIsEditMode());
            invalidateOptionsMenu();
        }

        public void toggleSpotsListEditMode() {
            if (tab_single_spots_list != null)
                tab_single_spots_list.setIsEditMode(!tab_single_spots_list.getIsEditMode());
            invalidateOptionsMenu();
        }

        public void onActivityResultFromSpotForm() {
            if (tab_route_spots_list != null)
                tab_route_spots_list.onActivityResultFromSpotForm();
            if (tab_single_spots_list != null)
                tab_single_spots_list.onActivityResultFromSpotForm();
        }

        /**
         * Selects all spots on the list.
         *
         * @param tabPosition index of the tab which all list items should be selected.
         **/
        private void selectAllSpots(int tabPosition) {
            switch (tabPosition) {
                case TAB_ROUTES_INDEX:
                    if (tab_route_spots_list != null)
                        tab_route_spots_list.selectAllSpots();
                    break;
                case TAB_SPOTS_INDEX:
                    if (tab_single_spots_list != null)
                        tab_single_spots_list.selectAllSpots();
                    break;
            }
        }

        private boolean getIsOneOrMoreSpotsSelected(int tabPosition) {
            switch (tabPosition) {
                case TAB_ROUTES_INDEX:
                    if (tab_route_spots_list != null)
                        return tab_route_spots_list.getIsOneOrMoreSpotsSelected();
                    break;
                case TAB_SPOTS_INDEX:
                    if (tab_single_spots_list != null)
                        return tab_single_spots_list.getIsOneOrMoreSpotsSelected();
                    break;
            }
            return false;
        }

        private boolean getIsAllSpotsSelected(int tabPosition) {
            switch (tabPosition) {
                case TAB_ROUTES_INDEX:
                    if (tab_route_spots_list != null)
                        return tab_route_spots_list.getIsAllSpotsSelected();
                    break;
                case TAB_SPOTS_INDEX:
                    if (tab_single_spots_list != null)
                        return tab_single_spots_list.getIsAllSpotsSelected();
                    break;
            }
            return false;
        }

        /**
         * Deselects all spots on the list.
         *
         * @param tabPosition index of the tab which all list items should be deselected.
         **/
        private void deselectAllSpots(int tabPosition) {
            switch (tabPosition) {
                case TAB_ROUTES_INDEX:
                    if (tab_route_spots_list != null)
                        tab_route_spots_list.deselectAllSpots();
                    break;
                case TAB_SPOTS_INDEX:
                    if (tab_single_spots_list != null)
                        tab_single_spots_list.deselectAllSpots();
                    break;
            }
        }

        private boolean getIsEditMode(int tabPosition) {
            switch (tabPosition) {
                case TAB_ROUTES_INDEX:
                    if (tab_route_spots_list != null)
                        return tab_route_spots_list.getIsEditMode();
                    break;
                case TAB_SPOTS_INDEX:
                    if (tab_single_spots_list != null)
                        return tab_single_spots_list.getIsEditMode();
                    break;
            }
            return false;
        }
    }

    private void saveSpotButtonHandler(boolean isDestination) {
        double cameraZoom = -1;
        Spot spot = null;
        int requestId = -1;
        if (!isWaitingForARide()) {
            requestId = Constants.SAVE_SPOT_REQUEST;
            spot = new Spot();
            spot.setIsHitchhikingSpot(!isDestination);
            spot.setIsDestination(isDestination);
            spot.setIsPartOfARoute(true);

        } else {
            requestId = Constants.EDIT_SPOT_REQUEST;
            spot = mCurrentWaitingSpot;
        }

        isHandlingRequestToOpenSpotForm = true;
        startSpotFormActivityForResult(spot, cameraZoom, requestId, true, false);
    }

    private boolean isWaitingForARide() {
        return (mCurrentWaitingSpot != null && mCurrentWaitingSpot.getIsWaitingForARide() != null) ?
                mCurrentWaitingSpot.getIsWaitingForARide() : false;
    }

    /**
     * @param shouldGoBack            should be set to true when you want the user to be sent to a new instance of MainActivity once they're done editing/adding a spot.
     * @param shouldRetrieveHWDetails should be set to true when the given spot is a Hitchwiki spot, so that we'll download more data from HW and display them on SpotFormActivity.
     */
    private void startSpotFormActivityForResult(Spot spot, double cameraZoom, int requestId, boolean shouldGoBack, boolean shouldRetrieveHWDetails) {
        Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);
        intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
        intent.putExtra(Constants.SPOT_BUNDLE_MAP_ZOOM_KEY, cameraZoom);
        intent.putExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, shouldGoBack);
        intent.putExtra(Constants.SHOULD_RETRIEVE_HITCHWIKI_DETAILS_KEY, shouldRetrieveHWDetails);
        startActivityForResult(intent, requestId);
    }
}
