package com.myhitchhikingspots;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.myhitchhikingspots.model.Spot;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private AppBarConfiguration mAppBarConfiguration;

    public static String ARG_REQUEST_TO_OPEN_FRAGMENT = "request-to-open-resource-id";
    protected static final String TAG = "main-activity";

    // Make sure to be using android.support.v7.app.ActionBarDrawerToggle version.
    // The android.support.v4.app.ActionBarDrawerToggle has been deprecated.
    // private ActionBarDrawerToggle drawerToggle;

    SharedPreferences prefs;

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

        setUpToolbarAndNavController();
    }

    private void setUpToolbarAndNavController() {
        // Set a Toolbar to replace the ActionBar.
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Find our drawer view
        mDrawer = findViewById(R.id.drawer_layout);
        mDrawer.addDrawerListener(drawerListener);

        // Find our drawer view
        NavigationView nvDrawer = findViewById(R.id.nvView);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_my_dashboard, R.id.nav_my_map, R.id.nav_hitchwiki_map, R.id.nav_offline_map, R.id.nav_about_us,
                R.id.nav_tools)
                .setOpenableLayout(mDrawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(nvDrawer, navController);
    }

    /**
     * Called when navigate back button is clicked.
     **/
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) ||
                super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp();
    }

    DrawerLayout.DrawerListener drawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
        }

        @Override
        public void onDrawerStateChanged(int newState) {
        }
    };

    /**
     * Shows the given fragment.
     *
     * @param destinationResourceId The fragment's resource id.
     */
    public void navigateToDestination(@IdRes int destinationResourceId) {
        navigateToDestination(destinationResourceId, null);
    }

    public void navigateToDestination(@IdRes int destinationResourceId, @Nullable Bundle args) {
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(destinationResourceId, args);
    }

    public void startToolsActivityForResult() {
        navigateToDestination(R.id.nav_tools);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        /*Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof OnMainActivityUpdated) {
            //Set the active fragment.
            OnMainActivityUpdated frag = (OnMainActivityUpdated) currentFragment;
            frag.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }*/
    }

    @Override
    protected void onStop() {
        super.onStop();

        dismissProgressDialog();
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

       /* Fragment currentFragment = getCurrentFragment();
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
        }*/
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