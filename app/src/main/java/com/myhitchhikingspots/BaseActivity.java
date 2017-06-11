package com.myhitchhikingspots;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mShouldShowLeftMenu)
            ShowMenu();
    }

    protected void ShowMenu() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer != null) {
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                drawer.setDrawerListener(toggle);
                toggle.syncState();

                //Set listener to the menu icon click (the icon placed on the top left side of the screen)
                NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                if (navigationView != null) {
                    navigationView.setVisibility(View.VISIBLE);
                    navigationView.setNavigationItemSelectedListener(this);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_tools)
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        else if (id == R.id.nav_home)
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        else if (id == R.id.nav_map)
            startActivity(new Intent(getApplicationContext(), MapViewActivity.class));

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
