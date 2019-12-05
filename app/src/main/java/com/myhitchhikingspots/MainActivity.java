package com.myhitchhikingspots;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.navigation.NavigationView;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.Utils;

import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LoadSpotsAndRoutesTask.onPostExecute, NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;

    List<Spot> spotList = new ArrayList();
    Spot mCurrentWaitingSpot;

    public static String ARG_SPOTLIST_KEY = "spot_list_arg";
    public static String ARG_CURRENTSPOT_KEY = "current_spot_arg";
    public static String ARG_REQUEST_TO_OPEN_FRAGMENT = "request-to-open-resource-id";
    public static String ARG_CHECKED_MENU_ITEM_ID_KEY = "my-fragment-title-arg";
    protected static final String TAG = "main-activity";

    // Make sure to be using android.support.v7.app.ActionBarDrawerToggle version.
    // The android.support.v4.app.ActionBarDrawerToggle has been deprecated.
    private ActionBarDrawerToggle drawerToggle;

    private AsyncTask loadTask;
    private AsyncTask fixSpotsStartDateTimeTask;

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

        int resourceToOpen = defaultFragmentResourceId;

        //If it is first time the activity is loaded, then load the list of spots with loadSpotList(),
        //If the activity is being restored, the activity's state will be restored by onRestoreInstanceState.
        if (savedInstanceState == null) {
            //A resourceId was received through ARG_REQUEST_TO_OPEN_FRAGMENT
            if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(ARG_REQUEST_TO_OPEN_FRAGMENT))
                resourceToOpen = getIntent().getExtras().getInt(ARG_REQUEST_TO_OPEN_FRAGMENT);
        }

        updateLoginOptionVisibility();

        loadSpotList(resourceToOpen);
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

        if (shouldLoadSpotListFirst)
            loadSpotList(resourceId);
        else
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
        else if (selectedItemId == R.id.nav_login)
            logIn();
        else if (selectedItemId == R.id.nav_logout)
            logOut();
        else {
            CharSequence title = menuItem.getTitle();
            setupSelectedFragment(menuItem, title.toString());

            //If spotList needs to be reloaded, let's do it here first.
            //Once reload is completed, the fragment corresponding to the selected menuItem will be opened with the updated spot list.
            if (prefs.getBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, false))
                loadSpotList(selectedItemId);
            else {
                // Create a new fragment and specify the fragment to show based on nav item clicked
                Class fragmentClass = getMenuItemFragment(selectedItemId);
                replaceFragmentContainerWith(fragmentClass);
            }
        }

        // Close the navigation drawer
        mDrawer.closeDrawers();
    }

    public void startToolsActivityForResult() {
        Intent intent = new Intent(getBaseContext(), ToolsActivity.class);
        startActivityForResult(intent, 1);
    }

    private void updateLoginOptionVisibility() {
        Menu menu = nvDrawer.getMenu();
        View header = nvDrawer.getHeaderView(0);
        AppCompatTextView headerLabel = header.findViewById(R.id.app_header_label);
        String username = (prefs == null) ? null : prefs.getString(Constants.PREFS_USER_CURRENTLY_LOGGED_IN, null);
        if (username != null) {
            menu.findItem(R.id.nav_login).setVisible(false);
            menu.findItem(R.id.nav_logout).setVisible(true);
            headerLabel.setText(getString(R.string.general_welcome_user, username));
        } else {
            menu.findItem(R.id.nav_login).setVisible(true);
            menu.findItem(R.id.nav_logout).setVisible(false);
            headerLabel.setText(getString(R.string.app_name));
        }
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
                Fragment currentFragment = getCurrentFragment(currentFragmentTag);
                if (currentFragment instanceof OnMainActivityUpdated) {
                    //Set the active fragment.
                    OnMainActivityUpdated frag = (OnMainActivityUpdated) currentFragment;
                    //Update the active fragment so that it has the most recent data.
                    frag.onSpotListChanged();
                }
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

        if (this.fixSpotsStartDateTimeTask != null) {
            this.fixSpotsStartDateTimeTask.cancel(false);
            dismissProgressDialog();
        }
    }

    /**
     * Loads the spot list from database and then opens a fragment once the list is loaded.
     *
     * @param fragmentResourceId The resource id of the fragment to be loaded once loading finishes.
     *                           if -1 then the updated values will be passed over to the current fragment
     */
    void loadSpotList(int fragmentResourceId) {
        showProgressDialog(getResources().getString(R.string.map_loading_dialog));

        this.fragmentIdToBeOpenedOnSpotsListLoaded = fragmentResourceId;

        //Load markers and polylines
        this.loadTask = new LoadSpotsAndRoutesTask(this).execute(((MyHitchhikingSpotsApplication) getApplicationContext()));

        prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, false).apply();
    }


    @Override
    /**
     * This method is called when LoadSpotsAndRoutesTask completes.
     * **/
    public void setupData(List<Spot> spotList, Spot mCurrentWaitingSpot, String errMsg) {
        Crashlytics.log(Log.INFO, TAG, "LoadSpotsAndRoutesTask has finished. setupData was called");
        if (!errMsg.isEmpty()) {
            showErrorAlert(getResources().getString(R.string.general_error_dialog_title), errMsg);
            return;
        }

        // If spots StartDateTime were not fixed yet, then execute FixSpotsStartDateTimeAsyncTask to fix them.
        // Once FixSpotsStartDateTimeAsyncTask is finished, this method (setupData) will be called again.
        // Make sure FixSpotsStartDateTimeAsyncTask is called only once at the first time when the app is updated.
        if (!prefs.getBoolean(Constants.PREFS_SPOTSSTARTDATETIME_WERE_FIXED, false)) {
            this.fixSpotsStartDateTimeTask = new FixSpotsStartDateTimeAsyncTask(this, spotList, mCurrentWaitingSpot).execute(((MyHitchhikingSpotsApplication) getApplicationContext()));
            prefs.edit().putBoolean(Constants.PREFS_SPOTSSTARTDATETIME_WERE_FIXED, true).apply();
            return;
        }

        this.spotList = spotList;
        this.mCurrentWaitingSpot = mCurrentWaitingSpot;
        ((MyHitchhikingSpotsApplication) getApplicationContext()).setCurrentSpot(mCurrentWaitingSpot);

        //Select fragment
        if (fragmentIdToBeOpenedOnSpotsListLoaded > -1) {
            selectDrawerItem(fragmentIdToBeOpenedOnSpotsListLoaded);
            fragmentIdToBeOpenedOnSpotsListLoaded = -1;
        } else
            updateUI();

        dismissProgressDialog();
    }

    void updateUI() {
        //Update the active fragment's data
        Fragment currentFragment = getCurrentFragment();
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

            //If user is navigating back to Dashboard or My Maps, reload spot list
            if (frag instanceof DashboardFragment || frag instanceof MyMapsFragment)
                loadSpotList(-1);
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


    public void logIn() {
        getToken();
    }

    public void logOut() {
        tryLogout();
    }

    final String hw_request_token_api_url = "https://hitchwiki.org/en/api.php?action=query&meta=tokens&type=login&format=json";
    final String hw_clientlogin_api_url = "https://hitchwiki.org/en/api.php?action=clientlogin&format=json&loginreturnurl=http://hitchwiki.org/";
    final String hw_logout_api_url = "https://hitchwiki.org/en/api.php?action=logout&format=json";
    final String hw_create_account_url = "https://hitchwiki.org/en/index.php?title=Special:CreateAccount&returnto=Main+Page";

    void getToken() {
        if (!Utils.isNetworkAvailable(this)) {
            showErrorAlert(getString(R.string.general_internet_unavailable_title), getString(R.string.general_network_unavailable_message));
            return;
        }
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, hw_request_token_api_url, null, response -> {
                    try {
                        String logintoken = response.getJSONObject("query").getJSONObject("tokens").getString("logintoken");

                        if (prefs != null)
                            prefs.edit().putString(Constants.PREFS_LOGIN_TOKEN, logintoken).apply();

                        showLoginDialog(logintoken);
                    } catch (Exception ex) {
                        showErrorAlert(getString(R.string.login_error_requesting_token), ex.getLocalizedMessage());
                        Crashlytics.logException(ex);
                    }
                }, error -> {
                    // TODO: Handle error
                    showErrorAlert(getString(R.string.login_error_requesting_token), error.getLocalizedMessage());
                    Crashlytics.logException(new Exception(error.getLocalizedMessage()));
                });

        //Let's set default cookie manager so that Volley will manage the necessary cookies. Cookies are used in order to login to Hitchwiki/Mediawiki.
        //More info on CookieManager here: https://stackoverflow.com/questions/16680701/using-cookies-with-android-volley-library
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);

        jsonObjectRequest.setShouldCache(false);

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }


    public void showLoginDialog(String logintoken) {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.dialog_login_layout, null);
        final EditText etUsername = alertLayout.findViewById(R.id.username);
        final EditText etPassword = alertLayout.findViewById(R.id.password);

        alertLayout.findViewById(R.id.create_account).setOnClickListener((view) -> {
            //Open browser with Hitchwiki create account page
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(hw_create_account_url));
            startActivity(browserIntent);
        });

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.login_dialog_title));
        // this is set the view from XML inside AlertDialog
        alert.setView(alertLayout);
        // disallow cancel of AlertDialog on click of back button and outside touch
        alert.setCancelable(false);
        alert.setNegativeButton(getString(R.string.general_cancel_option), (dialog, which) -> Toast.makeText(getBaseContext(), "Cancel clicked", Toast.LENGTH_SHORT).show());

        alert.setPositiveButton(getString(R.string.general_log_in_label), (d, w) -> {
            String user = etUsername.getText().toString();
            String pass = etPassword.getText().toString();
            tryLogin(logintoken, user, pass);
        });

        AlertDialog dialog = alert.create();
        dialog.show();
    }


    void tryLogin(String logintoken, String username, String password) {
        if (!Utils.isNetworkAvailable(this)) {
            showErrorAlert(getString(R.string.general_internet_unavailable_title), getString(R.string.general_network_unavailable_message));
            return;
        }
        tryLoginOnServerSide(logintoken, username, password);
    }

    void tryLoginOnServerSide(String logintoken, String username, String password) {
        if (logintoken == null) return;

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest jsonObjectRequest = new StringRequest
                (Request.Method.POST, hw_clientlogin_api_url, response -> {
                    try {
                        JSONObject responseObj = new JSONObject(response);

                        if (responseObj.has("error"))
                            showErrorAlert(getString(R.string.general_error_dialog_title), responseObj.getJSONObject("error").getString("info"));
                        else if (responseObj.getJSONObject("clientlogin").getString("status").equals("PASS")) {

                            if (prefs != null)
                                prefs.edit().putString(Constants.PREFS_USER_CURRENTLY_LOGGED_IN, username).apply();

                            showSuccessAndTryAssignAuthorDialog(this, getString(R.string.login_succeeded), getString(R.string.general_welcome_user, username));

                            updateLoginOptionVisibility();
                        }
                    } catch (Exception ex) {
                        showErrorAlert(getString(R.string.general_error_dialog_title), ex.getLocalizedMessage());
                    }

                }, (error) -> {
                    // TODO: Handle error
                    showErrorAlert(getString(R.string.login_failed), error.getLocalizedMessage());
                }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("logintoken", logintoken);
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }

    public static void showSuccessAndTryAssignAuthorDialog(Context context, String dialogTitle, String dialogMessage) {
        new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_check_circle_black_24dp)
                .setTitle(dialogTitle)
                .setMessage(dialogMessage)
                .setNeutralButton(context.getResources().getString(R.string.general_ok_option), (d, i) -> {
                    if (((MyHitchhikingSpotsApplication) context.getApplicationContext()).IsAnySpotMissingAuthor())
                        tryAssignAuthorToSpots(context);
                })
                .show();
    }

    private void tryLogout() {
        /*
        NOTE: Trying to log user out on the server side doesn't seem to work with action=logout when the user has
        logged in using action=clientlogin. I did not find any action=clientlogout option, and action=login does
        exist but on their documentation they recommend using action=clientlogin instead. Because action=logout
        is always giving a "badtoken" error, then we're commenting it out.
        Perhaps in the future we can consider learning more about how to use action=login and try that way.
        if (Utils.isNetworkAvailable(this))
            tryLogoutOnServerSide();*/

        onLogoutFinished();
    }

    private void tryLogoutOnServerSide() {
        String logintoken = (prefs == null) ? null : prefs.getString(Constants.PREFS_LOGIN_TOKEN, null);
        if (logintoken == null) return;

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest jsonObjectRequest = new StringRequest
                (Request.Method.POST, hw_logout_api_url, response -> {

                    try {
                        JSONObject responseObj = new JSONObject(response);
                        if (responseObj.has("error"))
                            Crashlytics.logException(new Exception(responseObj.getJSONObject("error").getString("info")));
                    } catch (Exception ex) {
                        Crashlytics.logException(ex);
                    }

                }, (error) -> {
                    Crashlytics.logException(new Exception(error.getLocalizedMessage()));
                }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("token", logintoken);
                return params;
            }
        };

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }

    private void onLogoutFinished() {
        showErrorAlert(getString(R.string.general_done_label), getString(R.string.logout_successful));

        prefs.edit().remove(Constants.PREFS_LOGIN_TOKEN).apply();
        prefs.edit().remove(Constants.PREFS_USER_CURRENTLY_LOGGED_IN).apply();

        updateLoginOptionVisibility();
    }

    private static void tryAssignAuthorToSpots(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);
        String username = (prefs == null) ? null : prefs.getString(Constants.PREFS_USER_CURRENTLY_LOGGED_IN, null);
        if (username == null)
            return;
        showShouldAssignSpotsMissingAuthorToUserDialog(context, username);
    }

    private static void showShouldAssignSpotsMissingAuthorToUserDialog(Context context, String username) {
        new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_check_circle_black_24dp)
                .setTitle(context.getString(R.string.assign_spots_dialog_title))
                .setMessage(context.getString(R.string.assign_spots_dialog_message))
                .setPositiveButton(context.getString(R.string.general_yes_option), (dialog, which) -> {
                    ((MyHitchhikingSpotsApplication) context.getApplicationContext()).AssignMissingAuthorTo(username);
                })
                .setNegativeButton(context.getString(R.string.general_no_option), (dialog, which) -> {
                })
                .show();
    }
}