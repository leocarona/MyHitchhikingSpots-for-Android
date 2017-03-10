package com.myhitchhikingspots;

import android.content.Intent;
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
import android.view.Menu;
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

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

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
                    GotARideShortcut();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
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
            MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
            mCurrentSpot = appContext.getCurrentSpot();

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
                mCurrentSpot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
                mCurrentSpot.setIsWaitingForARide(false);

                if (mSectionsPagerAdapter != null)
                    mCurrentSpot.setHitchability(mSectionsPagerAdapter.getSelectedHitchability());
            }

            //Persist on DB
            DaoSession daoSession = appContext.getDaoSession();
            SpotDao spotDao = daoSession.getSpotDao();
            spotDao.insertOrReplace(mCurrentSpot);
            ((MyHitchhikingSpotsApplication) getApplicationContext()).setCurrentSpot(mCurrentSpot);
            Toast.makeText(getApplicationContext(), R.string.spot_saved_successfuly, Toast.LENGTH_LONG).show();

            loadValues();

        } catch (Exception ex) {
            Log.e(TAG, "Shortcut event handler failed", ex);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.shortcut_save_button_failed), Toast.LENGTH_LONG).show();
        }
    }

    List<Spot> mSpotList;
    Spot mCurrentSpot;


    @Override
    public void onResume() {
        super.onResume();
        Log.i("tracking-main-activity", "onResume called");

        loadValues();
    }

    void loadValues() {
        Log.i("tracking-main-activity", "loadValues called");
        MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
        DaoSession daoSession = appContext.getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();
        mSpotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();
        mCurrentSpot = appContext.getCurrentSpot();

        //Update fragments
        if (mSectionsPagerAdapter != null)
            mSectionsPagerAdapter.setValues(mSpotList, mCurrentSpot);
    }

  /*  @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check which request we're responding to
        if (requestCode == SAVE_SPOT_REQUEST || requestCode == EDIT_SPOT_REQUEST) {
            // Make sure the request was successful
            if (resultCode > RESULT_FIRST_USER) {
                loadValues();

                //Update fragments
                if (mSectionsPagerAdapter != null)
                    mSectionsPagerAdapter.setValues(mSpotList, mCurrentSpot);
            }
        }
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.master, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(getApplicationContext(), MapViewActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private MyLocationFragment fragment;
        private SpotListFragment fragment2;

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
                    MyLocationFragment frag = new MyLocationFragment();
                    Bundle args = new Bundle();
                    frag.setArguments(args);
                    return frag;
                case 1:
                    SpotListFragment frag2 = new SpotListFragment();
                    Bundle args2 = new Bundle();
                    frag2.setArguments(args2);
                    return frag2;
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
            Log.i("tracking-main-activity", "SectionsPagerAdapter.instantiateItem called for position " + position);

            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            // save the appropriate reference depending on position
            switch (position) {
                case 0:
                    MyLocationFragment f = (MyLocationFragment) createdFragment;
                    f.setValues(mSpotList, mCurrentSpot);
                    fragment = f;
                    break;
                case 1:
                    SpotListFragment f2 = (SpotListFragment) createdFragment;
                    f2.setValues(mSpotList);
                    fragment2 = f2;
                    break;
            }
            return createdFragment;
        }

        @Override
        public int getCount() {
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

        public void setValues(List<Spot> lst, Spot spot) {
            Log.i("tracking-main-activity", "SectionsPagerAdapter.setValues called");
            try {
                if (fragment != null)
                    fragment.setValues(lst, spot);

                if (fragment2 != null)
                    fragment2.setValues(lst);
            } catch (Exception ex) {
                Log.e(TAG, "Calling fragments has failed", ex);
            }
        }
    }
}
