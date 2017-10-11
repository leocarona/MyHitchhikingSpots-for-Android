package com.myhitchhikingspots;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

/**
 * Created by leoboaventura on 07/03/2016.
 */
public class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected boolean mShouldShowLeftMenu = false;

    public static final int SAVE_SPOT_REQUEST = 2, EDIT_SPOT_REQUEST = 3;
    public static final int RESULT_OBJECT_ADDED = 2;
    public static final int RESULT_OBJECT_EDITED = 3;
    public static final int RESULT_OBJECT_DELETED = 4;
    DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (mShouldShowLeftMenu) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);

                if (drawer != null) {
                    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                    drawer.setDrawerListener(toggle);
                    toggle.syncState();

                    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                    if (navigationView != null)
                        navigationView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCheckedItem();
    }

    private void updateCheckedItem() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(null);

        if (navigationView != null) {
            //Apply selected style to the open activity
            String currentActivityName = getClass().getName();
            if (currentActivityName.equals(SettingsActivity.class.getName()))
                navigationView.setCheckedItem(R.id.nav_tools);
            else if (currentActivityName.equals(MyMapsActivity.class.getName()))
                navigationView.setCheckedItem(R.id.nav_my_map);
            else if (currentActivityName.equals(HitchwikiMapViewActivity.class.getName()))
                navigationView.setCheckedItem(R.id.nav_hitchwiki_map);
            /*else if (currentActivityName.equals(MainActivity.class.getName()))
                navigationView.setCheckedItem(R.id.nav_my_routes);*/
            else if (currentActivityName.equals(OfflineManagerActivity.class.getName()))
                navigationView.setCheckedItem(R.id.nav_offline_map);
            else
                navigationView.setCheckedItem(R.id.nav_unselect);
        }

        navigationView.setNavigationItemSelectedListener(this);
    }

    //Todo: consider using onActivityResult to information about new spots added/edited/deleted to update them in the spotList instead of fetching the whole list again from database
   /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check which request we're responding to
        if (requestCode == SAVE_SPOT_REQUEST || requestCode == EDIT_SPOT_REQUEST)
            setResult(resultCode);
    }
*/

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        String currentActivityName = getClass().getName();

        switch (item.getItemId()) {
            case R.id.nav_tools:
                if (!currentActivityName.equals(SettingsActivity.class.getName()))
                    startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                break;
            case R.id.nav_my_map:
                if (!currentActivityName.equals(MyMapsActivity.class.getName()))
                    startActivity(new Intent(getApplicationContext(), MyMapsActivity.class));
                break;
            case R.id.nav_hitchwiki_map:
                if (!currentActivityName.equals(HitchwikiMapViewActivity.class.getName()))
                    startActivity(new Intent(getApplicationContext(), HitchwikiMapViewActivity.class));
                break;
            /*case R.id.nav_my_routes:
                //If the current activity is MainActivity, select the tab "you"
                if (currentActivityName.equals(MainActivity.class.getName()))
                    ((MainActivity) this).selectTab(MainActivity.SectionsPagerAdapter.TAB_YOU_INDEX);
                else {
                //Start MainActivity presenting "you" tab.
                // If the current activity is MyMapsActivity, we want the user to be sent back here if he clicks in "map" button in the next activity - (SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY = true) will do that.

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    //intent.putExtra(Constants.SHOULD_SHOW_YOU_TAB_KEY, true);
                    intent.putExtra(Constants.SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY, currentActivityName.equals(MyMapsActivity.class.getName()));
                    startActivity(intent);
                }
                break;*/
            case R.id.nav_offline_map:
                if (!currentActivityName.equals(OfflineManagerActivity.class.getName()))
                    startActivity(new Intent(getApplicationContext(), OfflineManagerActivity.class));
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void showErrorAlert(String title, String msg) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.general_ok_option), null)
                .show();
    }
}
