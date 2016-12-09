package com.myhitchhikingspots;

import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.Date;
import java.util.List;


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

                if (mSectionsPagerAdapter != null)
                    mCurrentSpot.setHitchability(mSectionsPagerAdapter.getSelectedHitchability());
            }

            //Persist on DB
            DaoSession daoSession = ((MyHitchhikingSpotsApplication) getApplicationContext()).getDaoSession();
            SpotDao spotDao = daoSession.getSpotDao();
            spotDao.insertOrReplace(mCurrentSpot);
            ((MyHitchhikingSpotsApplication) getApplicationContext()).setCurrentSpot(mCurrentSpot);
            Toast.makeText(getApplicationContext(), R.string.spot_saved_successfuly, Toast.LENGTH_LONG).show();

            if (mSectionsPagerAdapter != null)
                mSectionsPagerAdapter.loadValues();

            //finish();
        } catch (Exception ex) {
            Log.e(TAG, "GotARideShortcut", ex);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.shortcut_save_button_failed), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSectionsPagerAdapter != null)
            mSectionsPagerAdapter.loadValues();
    }

   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }*/

    /*
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
*/

    protected void updateUILabels() {
        if (mSectionsPagerAdapter != null)
            mSectionsPagerAdapter.updateUILabels();
    }

    protected void updateUILocationSwitch() {
        if (mSectionsPagerAdapter != null)
            mSectionsPagerAdapter.updateUILocationSwitch();
    }

    protected void updateUISaveButtons() {
        if (mSectionsPagerAdapter != null)
            mSectionsPagerAdapter.updateUISaveButtons();
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
        private MyLocationFragment fragment;
        private SpotListFragment fragment2;
        private MyMapFragment fragment3;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        // Here we can finally safely save a reference to the created
        // Fragment, no matter where it came from (either getItem() or
        // FragmentManger). Simply save the returned Fragment from
        // super.instantiateItem() into an appropriate reference depending
        // on the ViewPager position. This solution was copied from:
        // http://stackoverflow.com/a/29288093/1094261
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            // save the appropriate reference depending on position
            switch (position) {
                case 0:
                    fragment = (MyLocationFragment) createdFragment;
                    break;
                case 1:
                    fragment2 = (SpotListFragment) createdFragment;
                    break;
                case 2:
                    fragment3 = (MyMapFragment) createdFragment;
                    break;
            }
            return createdFragment;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    MyLocationFragment frag = new MyLocationFragment();
                    Bundle args = new Bundle();
                    frag.setArguments(args);
                    frag.setValues(spotList, currentSpot);
                    return frag;
                case 1:
                    SpotListFragment frag2 = new SpotListFragment();
                    Bundle args2 = new Bundle();
                    frag2.setArguments(args2);
                    frag2.setValues(spotList);
                    return frag2;
                case 2:
                    MyMapFragment frag3 = new MyMapFragment();
                    Bundle args3 = new Bundle();
                    frag3.setArguments(args3);
                    frag3.setValues(spotList);
                    return frag3;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getResources().getString(R.string.main_activity_you_tab);
                case 1:
                    return getResources().getString(R.string.main_activity_list_tab);
                case 2:
                    return "map";//getResources().getString(R.string.main_activity_list_tab);
            }
            return null;
        }

        public void updateUILabels() {
            if (fragment != null)
                fragment.updateUILabels();
        }

        public void updateUILocationSwitch() {
            if (fragment != null)
                fragment.updateUILocationSwitch();
        }

        public void updateUISaveButtons() {
            if (fragment != null)
                fragment.updateUISaveButtons();
        }

        public Integer getSelectedHitchability() {
            if (fragment == null)
                return 0;
            else
                return fragment.getSelectedHitchability();
        }

        public void resumeFragments() {
            if (fragment != null)
                fragment.onResume();

            if (fragment2 != null)
                fragment2.onResume();

            if (fragment3 != null)
                fragment3.onResume();
        }

        List<Spot> spotList;
        Spot currentSpot;

        public void loadValues() {
            MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
            DaoSession daoSession = appContext.getDaoSession();
            SpotDao spotDao = daoSession.getSpotDao();
            spotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();
            currentSpot = appContext.getCurrentSpot();

            if (fragment != null)
                fragment.setValues(spotList, currentSpot);

            if (fragment2 != null)
                fragment2.setValues(spotList);

            if (fragment3 != null)
                fragment3.setValues(spotList);

            resumeFragments();
        }
    }


}
