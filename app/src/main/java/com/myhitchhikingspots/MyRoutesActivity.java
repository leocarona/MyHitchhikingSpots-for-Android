package com.myhitchhikingspots;

import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.interfaces.ListListener;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import java.util.ArrayList;
import java.util.List;


public class MyRoutesActivity extends BaseActivity {

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
    boolean wasSnackbarShown;
    static final String SNACKBAR_SHOWED_KEY = "snackbar-showed-key";
    static final String LAST_TAB_OPENED_KEY = "last-tab-opened-key";
    static final String TAG = "main-activity";
    ListListener spotsListListener = null;

    int indexOfLastOpenTab = 0;

    /**
     * Set shouldGoBackToPreviousActivity to true if instead of opening a new map, the action bar option should just finish current activity
     */
    Boolean shouldGoBackToPreviousActivity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.my_routes_activity_layout);

        //mWaitingToGetCurrentLocationTextView = (TextView) findViewById(R.id.waiting_location_textview);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        //savedInstanceState will be not null when a screen is rotated, for example. But will be null when activity is first created
        if (savedInstanceState == null) {
            shouldGoBackToPreviousActivity = getIntent().getBooleanExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, false);
            if (!wasSnackbarShown) {
                if (getIntent().getBooleanExtra(Constants.SHOULD_SHOW_SPOT_SAVED_SNACKBAR_KEY, false))
                    showSpotSavedSnackbar();
                else if (getIntent().getBooleanExtra(Constants.SHOULD_SHOW_SPOT_DELETED_SNACKBAR_KEY, false))
                    showSpotDeletedSnackbar();
            }
            wasSnackbarShown = true;
        } else
            updateValuesFromBundle(savedInstanceState);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        if (mSectionsPagerAdapter == null)
            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


        spotsListListener = new ListListener() {
            @Override
            public void onListOfSelectedSpotsChanged() {
                showSpotDeletedSnackbar();
            }

            @Override
            public void onSpotClicked(Spot spot) {
                indexOfLastOpenTab = mViewPager.getCurrentItem();
                //onSaveInstanceState will be executed right after onSpotClicked because when a spot is clicked, the fragment starts SpotFormActivity
            }
        };

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
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
        int snackbarTextId = android.support.design.R.id.snackbar_text;
        TextView textView = (TextView) snackbarView.findViewById(snackbarTextId);
        if (textView != null) textView.setTextColor(Color.WHITE);


        // change snackbar background
        snackbarView.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.ic_regular_spot_color));

        snackbar.show();
    }

    void showSpotSavedSnackbar() {
        showSnackbar(getResources().getString(R.string.spot_saved_successfuly),
                String.format(getString(R.string.action_button_label), getString(R.string.view_map_button_label)), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (shouldGoBackToPreviousActivity)
                            finish();
                        else
                        {
                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                            intent.putExtra(MainActivity.ARG_REQUEST_TO_OPEN_FRAGMENT,R.id.nav_my_map);
                            startActivity(intent);
                        }
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

    public void onPause() {
        super.onPause();

        if (snackbar != null)
            snackbar.dismiss();
    }

    void loadValues() {
        Crashlytics.log(Log.INFO, TAG, "loadValues called");
        MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
        DaoSession daoSession = appContext.getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();

        mSpotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.IsPartOfARoute, SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();

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

        // Check which request we're responding to
        //if (requestCode == SAVE_SPOT_REQUEST || requestCode == EDIT_SPOT_REQUEST) {
        // Make sure the request was successful
       /* if (resultCode != RESULT_CANCELED) {
            showViewMapSnackbar();
        }*/


        /*if (resultCode == RESULT_OBJECT_ADDED || resultCode == RESULT_OBJECT_EDITED)
            showSpotSavedSnackbar();

        if (resultCode == RESULT_OBJECT_DELETED)
            showSpotDeletedSnackbar();*/

        if (data != null)
            updateValuesFromBundle(data.getExtras());

        // }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_routes_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean selectionHandled = false;

        switch (item.getItemId()) {
            case R.id.action_edit_list:
                if (mSectionsPagerAdapter != null)
                    switch (mViewPager.getCurrentItem()) {
                        case SectionsPagerAdapter.TAB_ROUTES_INDEX:
                            //Toggle isEditMode
                            mSectionsPagerAdapter.toggleRoutesListEditMode();

                          /*Commenting this out because when user changes the selected tab, the isEditMode value is different for the other tab/fragment
                          //Update string to show "Edit list" or "Close edit mode" depending on isEditMode
                            if (!mSectionsPagerAdapter.getTabRoutesIsEditMode())
                                item.setTitle(getString(R.string.general_edit_list));
                            else
                                item.setTitle(getString(R.string.general_close_editing_mode_label));*/

                            break;
                        case SectionsPagerAdapter.TAB_SPOTS_INDEX:
                            //Toggle isEditMode
                            mSectionsPagerAdapter.toggleSpotsListEditMode();

                            /*Commenting this out because when user changes the selected tab, the isEditMode value is different for the other tab/fragment
                            //Update string to show "Edit list" or "Close edit mode" depending on isEditMode
                            if (!mSectionsPagerAdapter.getTabSpotsIsEditMode())
                                item.setTitle(getString(R.string.general_edit_list));
                            else
                                item.setTitle(getString(R.string.general_close_editing_mode_label));*/

                            break;
                        default:
                            Toast.makeText(this,
                                    String.format("Select tab '%1$s' or '%2$s'",
                                            getString(R.string.main_activity_list_tab),
                                            getString(R.string.main_activity_single_spots_list_tab)),
                                    Toast.LENGTH_LONG).show();
                            break;
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
        savedInstanceState.putBoolean(SNACKBAR_SHOWED_KEY, wasSnackbarShown);
        savedInstanceState.putInt(LAST_TAB_OPENED_KEY, indexOfLastOpenTab);
    }


    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Crashlytics.log(Log.INFO, TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(SNACKBAR_SHOWED_KEY))
                wasSnackbarShown = savedInstanceState.getBoolean(SNACKBAR_SHOWED_KEY);
            if (savedInstanceState.keySet().contains(LAST_TAB_OPENED_KEY))
                indexOfLastOpenTab = savedInstanceState.getInt(LAST_TAB_OPENED_KEY, 0);
        }
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
                case 0:
                    SpotListFragment listFrag = new SpotListFragment();
                    Bundle args1 = new Bundle();
                    listFrag.setArguments(args1);
                    return listFrag;
                case 1:
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
                case 0:
                    SpotListFragment tab_list = (SpotListFragment) createdFragment;
                    tab_list.setValues(routeSpots);
                    this.tab_route_spots_list = tab_list;
                    this.tab_route_spots_list.setOnOneOrMoreSpotsDeleted(spotsListListener);
                    break;
                case 1:
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
                case 0:
                    res = getString(R.string.main_activity_list_tab);
                    break;
                case 1:
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
        }

        public void toggleSpotsListEditMode() {
            if (tab_single_spots_list != null)
                tab_single_spots_list.setIsEditMode(!tab_single_spots_list.getIsEditMode());
        }
    }
}
