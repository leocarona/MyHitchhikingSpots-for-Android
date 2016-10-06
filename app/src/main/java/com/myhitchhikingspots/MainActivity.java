package com.myhitchhikingspots;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


public class MainActivity extends TrackLocationBaseActivity {

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

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    //  private GoogleApiClient client;

   /* protected AudioManager mAudioManager;
    protected RemoteControlClient mRemoteControlClient;
                                                          */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.main_activity_layout);

        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

      /*  mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        ComponentName rec = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(rec);
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        i.setComponent(rec);
        PendingIntent pi = PendingIntent.getBroadcast(this , 0 , i , 0 );
        mRemoteControlClient = new RemoteControlClient(pi);
        mAudioManager.registerRemoteControlClient(mRemoteControlClient);
        int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;//  
        mRemoteControlClient.setTransportControlFlags(flags);


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
                                                         */
        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            }
        });*/

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    //TODO
                    GotARideShortcut();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    //TODO
                    GotARideShortcut();
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    protected void GotARideShortcut() {
        try {
            //Get currentSpot and update it
            Spot mCurrentSpot = ((MyHitchhikingSpotsApplication) getApplicationContext()).getCurrentSpot();

            if (mCurrentSpot == null || mCurrentSpot.getIsWaitingForARide() == null || !mCurrentSpot.getIsWaitingForARide()) {
                if (mCurrentLocation == null)
                    return;
                mCurrentSpot = new Spot();
                mCurrentSpot.setStartDateTime(new Date());
                mCurrentSpot.setIsWaitingForARide(true);
                mCurrentSpot.setIsDestination(false);
                mCurrentSpot.setLatitude(mCurrentLocation.getLatitude());
                mCurrentSpot.setLongitude(mCurrentLocation.getLongitude());
            } else {
                DateTime date = new DateTime(mCurrentSpot.getStartDateTime());
                Integer waiting_time = Minutes.minutesBetween(date, DateTime.now()).getMinutes();

                mCurrentSpot.setWaitingTime(waiting_time);
                //TODO: Do this in a not hardcoded way
                mCurrentSpot.setAttemptResult(1);
                mCurrentSpot.setIsWaitingForARide(false);

                if (fragment != null)
                    mCurrentSpot.setHitchability(fragment.getSelectedHitchability());
            }

            //Persist on DB
            DaoSession daoSession = ((MyHitchhikingSpotsApplication) getApplicationContext()).getDaoSession();
            SpotDao spotDao = daoSession.getSpotDao();
            spotDao.insertOrReplace(mCurrentSpot);
            ((MyHitchhikingSpotsApplication) getApplicationContext()).setCurrentSpot(mCurrentSpot);
            Toast.makeText(getApplicationContext(), R.string.spot_saved_successfuly, Toast.LENGTH_LONG).show();

            if (fragment != null)
                fragment.onResume();

            if (fragment2 != null)
                fragment2.onResume();

            //finish();
        } catch (Exception ex) {
            Log.e(TAG, "GotARideShortcut", ex);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.shortcut_save_button_failed), Toast.LENGTH_LONG).show();
        }
    }

   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    MyLocationFragment fragment;
    SpotListFragment fragment2;

    protected void updateUILabels() {
        if (fragment != null)
            fragment.updateUILabels();
    }

    protected void updateUILocationSwitch() {
        if (fragment != null)
            fragment.updateUILocationSwitch();
    }
   /*
    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.myhitchhikingspots/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);

        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(this.getPackageName(),
                RemoteControlReceiver.class.getName()));
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.myhitchhikingspots/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
    // mAudioManager.abandonAudioFocus(mAudioFocusListener);

    }   */

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    fragment = new MyLocationFragment();
                    Bundle args = new Bundle();
                    fragment.setArguments(args);

                    return fragment;
                case 1:
                    fragment2 = new SpotListFragment();
                    Bundle args2 = new Bundle();
                    fragment2.setArguments(args2);
                    return fragment2;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getResources().getString(R.string.main_activity_you_tab);
                case 1:
                    return getResources().getString(R.string.main_activity_list_tab);
            }
            return null;
        }
    }


}
