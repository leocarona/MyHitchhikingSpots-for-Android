package com.myhitchhikingspots;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.myhitchhikingspots.model.Spot;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private AppBarConfiguration mAppBarConfiguration;

    public static String ARG_REQUEST_TO_OPEN_FRAGMENT = "request-to-open-resource-id";
    protected static final String TAG = "main-activity";

    SharedPreferences prefs;

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

        /*int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            //additional code
        } else {
            getSupportFragmentManager().popBackStack();
        }*/

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

    public void navigateToCreateOrEditSpotForm(@Nullable LatLng selectedLocation, double cameraZoom, boolean isDestination) {
        Spot mCurrentWaitingSpot = viewModel.getWaitingSpot().getValue();

        if (mCurrentWaitingSpot != null)
            navigateToEditSpotForm(mCurrentWaitingSpot, cameraZoom);
        else
            navigateToCreateSpotForm(selectedLocation, cameraZoom, isDestination);
    }

    public void navigateToCreateSpotForm(@Nullable LatLng selectedLocation, double cameraZoom, boolean isDestination) {
        Spot mCurrentWaitingSpot = new Spot();
        mCurrentWaitingSpot.setIsHitchhikingSpot(!isDestination);
        mCurrentWaitingSpot.setIsNotHitchhikedFromHere(isDestination);
        mCurrentWaitingSpot.setIsDestination(isDestination);
        mCurrentWaitingSpot.setIsPartOfARoute(true);

        if (selectedLocation != null) {
            mCurrentWaitingSpot.setLatitude(selectedLocation.getLatitude());
            mCurrentWaitingSpot.setLongitude(selectedLocation.getLongitude());
        }
        navigateToEditSpotForm(mCurrentWaitingSpot, cameraZoom);
    }

    public void navigateToEditSpotForm(Spot spot, double cameraZoom) {
        Bundle bundle = new Bundle();
        bundle.putDouble(Constants.SPOT_BUNDLE_MAP_ZOOM_KEY, cameraZoom);

        SpotFormViewModel spotFormViewModel = new ViewModelProvider(this).get(SpotFormViewModel.class);
        spotFormViewModel.setCurrentSpot(spot, false);

        navigateToDestination(R.id.nav_spot_form, bundle);
    }

    public void navigateToHWSpotForm(Spot spot, double cameraZoom) {
        Bundle bundle = new Bundle();
        bundle.putDouble(Constants.SPOT_BUNDLE_MAP_ZOOM_KEY, cameraZoom);

        SpotFormViewModel spotFormViewModel = new ViewModelProvider(this).get(SpotFormViewModel.class);
        spotFormViewModel.setCurrentSpot(spot, true);

        navigateToDestination(R.id.nav_spot_form, bundle);
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