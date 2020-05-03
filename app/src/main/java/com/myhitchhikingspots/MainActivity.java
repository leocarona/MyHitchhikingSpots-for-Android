package com.myhitchhikingspots;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.crashlytics.android.Crashlytics;
import com.google.android.material.navigation.NavigationView;
import com.myhitchhikingspots.model.Spot;

import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;

    public static String ARG_REQUEST_TO_OPEN_FRAGMENT = "request-to-open-resource-id";
    public static String ARG_CHECKED_MENU_ITEM_ID_KEY = "my-fragment-title-arg";
    protected static final String TAG = "main-activity";

    // Make sure to be using android.support.v7.app.ActionBarDrawerToggle version.
    // The android.support.v4.app.ActionBarDrawerToggle has been deprecated.
    private ActionBarDrawerToggle drawerToggle;

    SharedPreferences prefs;

    int fragmentIdToBeOpenedOnSpotsListLoaded = -1;

    //Default fragment that will open on the app startup
    int defaultFragmentResourceId = R.id.nav_my_map;

    public interface OnMainActivityUpdated {
        /**
         * Method called always that spotList is loaded or reloaded from the database.
         **/
        void onSpotListChanged();

        void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);

        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    SpotsListViewModel viewModel;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        viewModel = new ViewModelProvider(this).get(SpotsListViewModel.class);

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

        int resourceToOpen = defaultFragmentResourceId;

        //If it is first time the activity is loaded, then load the list of spots with loadSpotList(),
        //If the activity is being restored, the activity's state will be restored by onRestoreInstanceState.
        if (savedInstanceState == null) {
            //A resourceId was received through ARG_REQUEST_TO_OPEN_FRAGMENT
            if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(ARG_REQUEST_TO_OPEN_FRAGMENT))
                resourceToOpen = getIntent().getExtras().getInt(ARG_REQUEST_TO_OPEN_FRAGMENT);
            else if (prefs.contains(Constants.PREFS_DEFAULT_STARTUP_FRAGMENT)) {
                String favStartupFragment = prefs.getString(Constants.PREFS_DEFAULT_STARTUP_FRAGMENT, "");
                resourceToOpen = getResourceIdForFragment(favStartupFragment);
            }
        }

        selectDrawerItem(resourceToOpen);
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
            case R.id.nav_about_us:
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
        else if (className.equals(AboutUsFragment.class.getName()))
            menuItemResourceId = R.id.nav_about_us;

        return menuItemResourceId;
    }

    int getResourceIdForFragment(String fragmentClassName) {
        if (fragmentClassName.equals(DashboardFragment.class.getName()))
            return R.id.nav_my_dashboard;
        if (fragmentClassName.equals(HitchwikiMapViewFragment.class.getName()))
            return R.id.nav_hitchwiki_map;
        else return R.id.nav_my_map;
    }

    public void selectDrawerItem(int resourceId) {
        Crashlytics.log(Log.INFO, TAG, "selectDrawerItem was called with resourceId = " + resourceId);
        int menuItemIndex = getMenuItemIndex(resourceId);
        Crashlytics.setInt("menuItemIndex", menuItemIndex);
        selectDrawerItem(nvDrawer.getMenu().getItem(menuItemIndex));
    }

    public void selectDrawerItem(MenuItem menuItem) {
        int selectedItemId = menuItem.getItemId();

        if (selectedItemId == R.id.nav_tools)
            startToolsActivityForResult();
        else {
            CharSequence title = menuItem.getTitle();
            setupSelectedFragment(menuItem, title.toString());

            // Create a new fragment and specify the fragment to show based on nav item clicked
            Class fragmentClass = getMenuItemFragment(selectedItemId);
            replaceFragmentContainerWith(fragmentClass);
        }

        // Close the navigation drawer
        mDrawer.closeDrawers();
    }

    public void startToolsActivityForResult() {
        Intent intent = new Intent(getBaseContext(), ToolsActivity.class);
        startActivityForResult(intent, 1);
    }

    /**
     * @param shouldGoBack            should be set to true when you want the user to be sent to a new instance of MainActivity once they're done editing/adding a spot.
     * @param shouldRetrieveHWDetails should be set to true when the given spot is a Hitchwiki spot, so that we'll download more data from HW and display them on SpotFormActivity.
     */
    public void startSpotFormActivityForResult(Spot spot, double cameraZoom, int requestId, boolean shouldGoBack, boolean shouldRetrieveHWDetails) {
        Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);
        intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
        intent.putExtra(Constants.SPOT_BUNDLE_MAP_ZOOM_KEY, cameraZoom);
        intent.putExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, shouldGoBack);
        intent.putExtra(Constants.SHOULD_RETRIEVE_HITCHWIKI_DETAILS_KEY, shouldRetrieveHWDetails);
        startActivityForResult(intent, requestId);
    }

    @NonNull
    private Class getMenuItemFragment(int selectedItemId) {
        Class fragmentClass;

        switch (selectedItemId) {
            case R.id.nav_my_dashboard:
                fragmentClass = DashboardFragment.class;
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
            case R.id.nav_about_us:
                fragmentClass = AboutUsFragment.class;
                break;
            default:
                fragmentClass = BasicFragment.class;
        }
        return fragmentClass;
    }

    private void setupSelectedFragment(MenuItem menuItem, String title) {
        Crashlytics.setString("title", title);

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
        if (isFinishing())
            return;

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, fragment.getClass().getName())
                //Adding a transaction to the back stack through addToBackStack guarantees that when user clicks on the Back button
                // they're sent back to the fragment they're come from.
                .addToBackStack(fragment.getClass().getName());

        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        Crashlytics.log("User has pressed the back button.");

        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            Crashlytics.log("The drawer was closed and nothing more will happen.");
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            //If there's nowhere to navigate back to, then close the app.
            if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
                finish();
                return;
            }

            //Call onBackPressed for it should remove the active fragment from the last position of the back stack, and then display the previous fragment to the user.
            //Warning: onBackPressed MUST be called before restoreUIFor().
            super.onBackPressed();

            //After onBackPressed is called, the back stack should now have at its last position the fragment entry which the user is navigating back to.
            //Let's restore the UI for this fragment.
            restoreUIFor(getSupportFragmentManager().getBackStackEntryCount() - 1);
        }
    }

    /**
     * Updates appbar title and the selected menu item so that they correspond to the fragment at the given back stack index.
     * Also updates the fragment itself passing the most recent data to it.
     **/
    public void restoreUIFor(int backStackEntryIndex) {
        try {
            String currentFragmentTag = getSupportFragmentManager().getBackStackEntryAt(backStackEntryIndex).getName();

            if (currentFragmentTag != null && !currentFragmentTag.isEmpty()) {
                int menuItemResourceId = getMenuItemIdFromClassName(currentFragmentTag);
                int menuItemIndex = getMenuItemIndex(menuItemResourceId);

                //Update the appbar title and apply selection style to the menu item that correspond to the fragment.
                restoreLastCheckedMenuItem(menuItemIndex);

                //Note: The fragment tag here is the same that we've defined earlier when we called fragmentManager.beginTransaction().replace(param1,param2,tagName).
                notifyFragment(getCurrentFragment(currentFragmentTag));
            }
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }
    }

    Fragment getCurrentFragment() {
        int currentFragmentIndexOnBackStack = getSupportFragmentManager().getBackStackEntryCount() - 1;

        String currentFragmentTag = getSupportFragmentManager().getBackStackEntryAt(currentFragmentIndexOnBackStack).getName();
        if (currentFragmentTag == null || currentFragmentTag.isEmpty())
            return null;

        return getCurrentFragment(currentFragmentTag);
    }

    Fragment getCurrentFragment(String currentFragmentTag) {
        //Note: The fragment tag here is the same that we've defined earlier when we called fragmentManager.beginTransaction().replace(param1,param2,tagName).
        return getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
    }

    /*onSaveInstanceState is called before an activity may be killed so that when it comes back some time in the future it can restore its state. */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int checkedMenuItemIndex = getCheckedItem(nvDrawer);

        if (checkedMenuItemIndex != -1) {
            MenuItem checkedMenuItem = nvDrawer.getMenu().getItem(checkedMenuItemIndex);
            outState.putInt(MainActivity.ARG_CHECKED_MENU_ITEM_ID_KEY, checkedMenuItem.getItemId());
        }
    }

    /*This method is called after onStart() when the activity is being re-initialized from a previously saved state (saved within onSaveInstanceState).
     * onRestoreInstanceState is always called BEFORE onResume. */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //Restoring state can also be done within onCreate(Bundle).
        // Though, doing it here makes the code cleaner and we also don't need to check if savedInstanceState is null.

        if (savedInstanceState.containsKey(ARG_CHECKED_MENU_ITEM_ID_KEY)) {
            int lastCheckedMenuItemId = savedInstanceState.getInt(MainActivity.ARG_CHECKED_MENU_ITEM_ID_KEY);
            int lastCheckedMenuItemIndex = getMenuItemIndex(lastCheckedMenuItemId);
            restoreLastCheckedMenuItem(lastCheckedMenuItemIndex);
        }

        fragmentIdToBeOpenedOnSpotsListLoaded = -1;
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof OnMainActivityUpdated) {
            //Set the active fragment.
            OnMainActivityUpdated frag = (OnMainActivityUpdated) currentFragment;
            frag.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        dismissProgressDialog();
    }

    /**
     * Loads the spot list from database and then opens a fragment once the list is loaded.
     *
     * @param fragmentResourceId The resource id of the fragment to be loaded once loading finishes.
     *                           if -1 then the updated values will be passed over to the current fragment
     */
    void loadSpotList(int fragmentResourceId) {
        this.fragmentIdToBeOpenedOnSpotsListLoaded = fragmentResourceId;

        // update UI
        setupData(null, null, "");

        prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, false).apply();
    }


    /**
     * This method is called when LoadSpotsAndRoutesTask completes.
     **/
    public void setupData(List<Spot> spotList, Spot mCurrentWaitingSpot, String errMsg) {
        Crashlytics.log(Log.INFO, TAG, "setupData was called");

        //Select fragment
        if (fragmentIdToBeOpenedOnSpotsListLoaded > -1) {
            selectDrawerItem(fragmentIdToBeOpenedOnSpotsListLoaded);
            fragmentIdToBeOpenedOnSpotsListLoaded = -1;
        } else
            notifyFragment(getCurrentFragment());

    }

    private void notifyFragment(Fragment currentFragment) {
        //Update the active fragment's data
        if (currentFragment instanceof OnMainActivityUpdated) {
            //Set the active fragment.
            OnMainActivityUpdated frag = (OnMainActivityUpdated) currentFragment;
            frag.onSpotListChanged();
        }
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

        Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof OnMainActivityUpdated) {
            //Set the active fragment.
            OnMainActivityUpdated frag = (OnMainActivityUpdated) currentFragment;
            frag.onActivityResult(requestCode, resultCode, data);

            if (prefs.getBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, false)) {
                //If user is navigating back to Dashboard or My Maps, reload spot list
                if (frag instanceof DashboardFragment || frag instanceof MyMapsFragment) {
                    notifyFragment(getCurrentFragment());
                    prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, false).apply();
                }
            }
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