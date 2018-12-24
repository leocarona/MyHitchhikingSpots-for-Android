package com.myhitchhikingspots;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.myhitchhikingspots.model.Spot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoadSpotsAndRoutesTask.onPostExecute, NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;

    List<Spot> spotList = new ArrayList();
    Spot mCurrentWaitingSpot;

    public static String ARG_SPOTLIST_KEY = "spot_list_arg";
    public static String ARG_CURRENTSPOT_KEY = "current_spot_arg";
    public static String ARG_FRAGMENT_KEY = "my-fragment-arg";
    public static String ARG_REQUEST_TO_OPEN_FRAGMENT = "request-to-open-resource-id";
    public static String ARG_CHECKED_MENU_ITEM_ID_KEY = "my-fragment-title-arg";

    // Make sure to be using android.support.v7.app.ActionBarDrawerToggle version.
    // The android.support.v4.app.ActionBarDrawerToggle has been deprecated.
    private ActionBarDrawerToggle drawerToggle;

    private AsyncTask loadTask;

    SharedPreferences prefs;

    OnSpotsListChanged activeFragmentListening;

    int fragmentResourceId = -1;

    //Default fragment that will open on the app startup
    int defaultFragmentResourceId = R.id.nav_my_map;

    public interface OnSpotsListChanged {
        void updateSpotList(List<Spot> spotList, Spot mCurrentWaitingSpot);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        prefs = getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        // Set a Toolbar to replace the ActionBar.
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Find our drawer view
        mDrawer = findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();

        // Tie DrawerLayout events to the ActionBarToggle
        mDrawer.addDrawerListener(drawerToggle);

        // Find our drawer view
        nvDrawer = findViewById(R.id.nvView);
        // Setup drawer view
        nvDrawer.setNavigationItemSelectedListener(this);

        //If it is first time the activity is loaded, then load the list of spots with loadSpotList(),
        //If the activity is being restored, the activity's state will be restored by onRestoreInstanceState.
        if (savedInstanceState == null) {
            int resourceToOpen = defaultFragmentResourceId;

            //A resourceId was received through ARG_REQUEST_TO_OPEN_FRAGMENT
            if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(ARG_REQUEST_TO_OPEN_FRAGMENT))
                resourceToOpen = getIntent().getExtras().getInt(ARG_REQUEST_TO_OPEN_FRAGMENT);

            loadSpotList(resourceToOpen);
        }
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        // NOTE: Make sure you pass in a valid toolbar reference.  ActionBarDrawToggle() does not require it
        // and will not render the hamburger icon without it.
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // `onPostCreate` called when activity start-up is complete after `onStart()`
    // NOTE 1: Make sure to override the method with only a single `Bundle` argument
    // Note 2: Make sure you implement the correct `onPostCreate(Bundle savedInstanceState)` method.
    // There are 2 signatures and only `onPostCreate(Bundle state)` shows the hamburger icon.
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        Boolean shouldLoadSpotListFirst = false;
        int resourceId = menuItem.getItemId();

        //If spotList has been updated, we should load the list here prior to opening the new fragment
        if (prefs.getBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, false) &&
                (resourceId == R.id.nav_my_dashboard || resourceId == R.id.nav_my_map))
            shouldLoadSpotListFirst = true;

        if (shouldLoadSpotListFirst) {
            loadSpotList(resourceId);
            prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, false).apply();
        } else
            selectDrawerItem(menuItem);

        return true;
    }

    int getMenuItemIndex(int resourceId) {
        Integer menuItemIndex = -1;
        switch (resourceId) {
            case R.id.nav_my_dashboard:
                menuItemIndex = 0;
                break;
            case R.id.nav_my_map:
                menuItemIndex = 1;
                break;
            case R.id.nav_hitchwiki_map:
                menuItemIndex = 2;
                break;
            case R.id.nav_offline_map:
                menuItemIndex = 3;
                break;
            case R.id.nav_tools:
                menuItemIndex = 4;
                break;
        }
        return menuItemIndex;
    }

    int getMenuItemIdFromClassName(String className) {
        int menuItemResourceId = -1;

        if (className.equals(DashboardFragment.class.getName()))
            menuItemResourceId = R.id.nav_my_dashboard;
        else if (className.equals(MyMapsFragment.class.getName()))
            menuItemResourceId = R.id.nav_my_map;
        else if (className.equals(HitchwikiMapViewFragment.class.getName()))
            menuItemResourceId = R.id.nav_hitchwiki_map;
        else if (className.equals(OfflineMapManagerFragment.class.getName()))
            menuItemResourceId = R.id.nav_offline_map;
        else if (className.equals(SettingsFragment.class.getName()))
            menuItemResourceId = R.id.nav_tools;

        return menuItemResourceId;
    }

    public void selectDrawerItem(int resourceId) {
        int menuItemIndex = getMenuItemIndex(resourceId);
        selectDrawerItem(nvDrawer.getMenu().getItem(menuItemIndex));
    }

    public void selectDrawerItem(MenuItem menuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        Class fragmentClass;
        CharSequence title = menuItem.getTitle();

        switch (menuItem.getItemId()) {
            case R.id.nav_my_dashboard:
                fragmentClass = DashboardFragment.class;
                title = getString(R.string.app_name);
                break;
            case R.id.nav_my_map:
                fragmentClass = MyMapsFragment.class;
                break;
            case R.id.nav_hitchwiki_map:
                fragmentClass = HitchwikiMapViewFragment.class;
                break;
            case R.id.nav_offline_map:
                fragmentClass = OfflineMapManagerFragment.class;
                break;
            case R.id.nav_tools:
                fragmentClass = SettingsFragment.class;
                break;
            default:
                fragmentClass = BasicFragment.class;
        }

        replaceFragmentContainerWith(fragmentClass);

        setupSelectedFragment(menuItem, title.toString());

        // Close the navigation drawer
        mDrawer.closeDrawers();
    }

    private void setupSelectedFragment(MenuItem menuItem, String title) {
        // Highlight the selected item has been done by NavigationView
        menuItem.setChecked(true);

        // Set action bar title
        setTitle(title);
    }

    private void replaceFragmentContainerWith(Class fragmentClass) {
        Fragment fragment = null;

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        replaceFragmentContainerWith(fragment);
    }

    private void replaceFragmentContainerWith(Fragment fragment) {
        Bundle bundle = new Bundle();

        //Classes that implement OnSpotsListChanged expect spotList and mCurrentWaitingSpot when it's created and when these variables are updated.
        if (fragment instanceof OnSpotsListChanged) {
            Spot[] spotArray = new Spot[spotList.size()];
            bundle.putSerializable(MainActivity.ARG_SPOTLIST_KEY, spotList.toArray(spotArray));
            bundle.putSerializable(MainActivity.ARG_CURRENTSPOT_KEY, mCurrentWaitingSpot);

            //Keep the fragment so that we can fire the event listeners when spotList is updated.
            activeFragmentListening = (OnSpotsListChanged) fragment;
        } else
            activeFragmentListening = null;

        fragment.setArguments(bundle);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment);

        //Adding a transaction to the back stack, guarantees that when user clicks on the Back button
        // they're sent back to the fragment they're come from.
        //If it's the first fragment being loaded, we want the app to close when user clicks on the Back button,
        // therefore we don't want to add the fragment to the back stack.
        if (fragmentManager.getFragments().size() > 0)
            fragmentTransaction.addToBackStack(fragment.getClass().getName());

        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        activeFragmentListening = null;

        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {

            int lastOpenedFragmentIndex = getSupportFragmentManager().getBackStackEntryCount() - 2;
            String lastOpenedFragment = null;
            if (lastOpenedFragmentIndex >= 0)
                lastOpenedFragment = getSupportFragmentManager().getBackStackEntryAt(lastOpenedFragmentIndex).getName();

            int menuItemResourceId = defaultFragmentResourceId;
            if (lastOpenedFragment != null)
                menuItemResourceId = getMenuItemIdFromClassName(lastOpenedFragment);

            int menuItemIndex = getMenuItemIndex(menuItemResourceId);
            restoreLastCheckedMenuItem(menuItemIndex);


            super.onBackPressed();
        }
    }

    /*onSaveInstanceState is called before an activity may be killed so that when it comes back some time in the future it can restore its state. */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int checkedMenuItemIndex = getCheckedItem(nvDrawer);

        Spot[] spotArray = new Spot[spotList.size()];
        outState.putSerializable(MainActivity.ARG_SPOTLIST_KEY, spotList.toArray(spotArray));
        outState.putSerializable(MainActivity.ARG_CURRENTSPOT_KEY, mCurrentWaitingSpot);

        if (checkedMenuItemIndex != -1) {
            MenuItem checkedMenuItem = nvDrawer.getMenu().getItem(checkedMenuItemIndex);
            outState.putInt(MainActivity.ARG_CHECKED_MENU_ITEM_ID_KEY, checkedMenuItem.getItemId());
        }

        //Save the fragment's instance
        if (activeFragmentListening != null)
            getSupportFragmentManager().putFragment(outState, ARG_FRAGMENT_KEY, (Fragment) activeFragmentListening);
    }

    /*This method is called after onStart() when the activity is being re-initialized from a previously saved state (saved within onSaveInstanceState).
     * onRestoreInstanceState is always called BEFORE onResume. */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //Restoring state can also be done within onCreate(Bundle).
        // Though, doing it here makes the code cleaner and we also don't need to check if savedInstanceState is null.

        if (savedInstanceState.containsKey(MainActivity.ARG_SPOTLIST_KEY)) {
            Spot[] bundleSpotList = (Spot[]) savedInstanceState.getSerializable(MainActivity.ARG_SPOTLIST_KEY);
            spotList = Arrays.asList(bundleSpotList);
        }

        if (savedInstanceState.containsKey(MainActivity.ARG_CURRENTSPOT_KEY)) {
            mCurrentWaitingSpot = (Spot) savedInstanceState.getSerializable(MainActivity.ARG_CURRENTSPOT_KEY);
        }

        if (savedInstanceState.containsKey(ARG_CHECKED_MENU_ITEM_ID_KEY)) {
            int lastCheckedMenuItemId = savedInstanceState.getInt(MainActivity.ARG_CHECKED_MENU_ITEM_ID_KEY);
            int lastCheckedMenuItemIndex = getMenuItemIndex(lastCheckedMenuItemId);
            restoreLastCheckedMenuItem(lastCheckedMenuItemIndex);
        }

        //Restore the fragment's instance
        if (savedInstanceState.containsKey(ARG_FRAGMENT_KEY)) {
            activeFragmentListening = (OnSpotsListChanged) getSupportFragmentManager().getFragment(savedInstanceState, ARG_FRAGMENT_KEY);
        }
    }

    void restoreLastCheckedMenuItem(int menuItemIndex) {
        MenuItem lastCheckedMenuItem = nvDrawer.getMenu().getItem(menuItemIndex);
        restoreLastCheckedMenuItem(lastCheckedMenuItem);
    }

    void restoreLastCheckedMenuItem(MenuItem lastCheckedMenuItem) {
        nvDrawer.setNavigationItemSelectedListener(null);
        setupSelectedFragment(lastCheckedMenuItem, lastCheckedMenuItem.getTitle().toString());
        nvDrawer.setNavigationItemSelectedListener(this);
    }

    private int getCheckedItem(NavigationView navigationView) {
        Menu menu = navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.isChecked()) {
                return i;
            }
        }

        return -1;
    }

    @Override
    protected void onStop() {
        super.onStop();

        /*
         * The device may have been rotated and the activity is going to be destroyed
         * you always should be prepared to cancel your AsnycTasks before the Activity
         * which created them is going to be destroyed.
         * And dont rely on mayInteruptIfRunning
         */
        if (this.loadTask != null) {
            this.loadTask.cancel(false);
            dismissProgressDialog();
        }
    }

    /**
     * Loads the spot list from database and then opens a fragment once the list is loaded.
     *
     * @param fragmentResourceId The resource id of the fragment to be loaded once loading finishes.
     */
    void loadSpotList(int fragmentResourceId) {
        showProgressDialog(getResources().getString(R.string.map_loading_dialog));

        this.fragmentResourceId = fragmentResourceId;

        //Load markers and polylines
        loadTask = new LoadSpotsAndRoutesTask(this).execute(((MyHitchhikingSpotsApplication) getApplicationContext()));
    }


    @Override
    public void setupData(List<Spot> spotList, Spot mCurrentWaitingSpot, String errMsg) {
        if (!errMsg.isEmpty()) {
            showErrorAlert(getResources().getString(R.string.general_error_dialog_title), errMsg);
            return;
        }

        this.spotList = spotList;
        this.mCurrentWaitingSpot = mCurrentWaitingSpot;

        //Select fragment
        if (fragmentResourceId > -1) {
            selectDrawerItem(fragmentResourceId);
            fragmentResourceId = -1;
        } else
            updateUI();

        dismissProgressDialog();
    }

    void updateUI() {
        //Update the active fragment's data
        if (activeFragmentListening != null && ((Fragment) activeFragmentListening).isVisible())
            activeFragmentListening.updateSpotList(spotList, mCurrentWaitingSpot);
    }

    protected void showErrorAlert(String title, String msg) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.general_ok_option), null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        //If any spot has been created, edited or deleted, let's reload the spotList from the database.
        if (resultCode == Constants.RESULT_OBJECT_ADDED || resultCode == Constants.RESULT_OBJECT_EDITED || resultCode == Constants.RESULT_OBJECT_DELETED)
            loadSpotList(-1);

        //If user is navigating back after spotList has been changed, let's reload it.
        if (prefs.getBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, false)) {
            loadSpotList(-1);
            prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, false).apply();
        }
    }

    private ProgressDialog loadingDialog;

    private void showProgressDialog(String message) {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(false);
        }
        loadingDialog.setMessage(message);
        loadingDialog.show();
    }

    private void dismissProgressDialog() {
        try {
            if (loadingDialog != null && loadingDialog.isShowing())
                loadingDialog.dismiss();
        } catch (Exception e) {
        }
    }
}