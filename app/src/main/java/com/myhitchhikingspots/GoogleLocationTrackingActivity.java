package com.myhitchhikingspots;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.util.Date;
import java.util.List;

/** @deprecated **/
public class GoogleLocationTrackingActivity extends TrackLocationBaseActivity {
    List<Spot> mSpotList;
    Spot mCurrentSpot;

    //WARNING: in order to use BaseActivity the method onCreate must be overridden
    // calling first setContentView to the view you want to use
    // and then calling super.onCreate AFTER setContentView.
    // Please always make sure this is been done!
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.google_location_master_layout);


        /*
                TO REUSE THIS ACTIVITY FILE, REPLACE:
                setContentView(..)
        AND
                REMEMBER TO ADD AN "<activity>" TAG FOR THE NEW FILE INTO /app/src/main/AndroidManifest.xml

                AND IF YOU WISH TO USE mShouldShowLeftMenu, MAKE SURE YOU HAVE nav_view ON YOUR LAYOUT FILE
        */


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_spot_action_1);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                startActivity(new Intent(getApplicationContext(), MyLocationFragment.class));
            }
        });

        // TO USE LeftMenu (drawer) MAKE SURE YOU ALSO HAVE

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Crashlytics.log("onResume called");


        Crashlytics.log("loadValues called");
        MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
        DaoSession daoSession = appContext.getDaoSession();
        SpotDao spotDao = daoSession.getSpotDao();

        mSpotList = spotDao.queryBuilder().orderDesc(SpotDao.Properties.IsPartOfARoute, SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();
        mCurrentSpot = appContext.getCurrentSpot();

        //Update fragments
       // my_location_fragment.setValues(getWillBeFirstSpotOfARoute(mSpotList), mCurrentSpot);
    }

    Boolean getWillBeFirstSpotOfARoute(List<Spot> lst) {
        return lst.size() == 0 ||
                (lst.get(0).getIsPartOfARoute() == null || !lst.get(0).getIsPartOfARoute()) ||
                (lst.get(0).getIsDestination() != null && lst.get(0).getIsDestination());
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
                mCurrentSpot.setIsHitchhikingSpot(true);
                mCurrentSpot.setStartDateTime(new DateTime());
                mCurrentSpot.setIsWaitingForARide(true);
                mCurrentSpot.setIsDestination(false);
                mCurrentSpot.setLatitude(mCurrentLocation.getLatitude());
                mCurrentSpot.setLongitude(mCurrentLocation.getLongitude());
                mCurrentSpot.setIsPartOfARoute(true);
            } else {
                DateTime date = new DateTime(mCurrentSpot.getStartDateTime());
                Integer waiting_time = Minutes.minutesBetween(date, DateTime.now()).getMinutes();

                mCurrentSpot.setWaitingTime(waiting_time);
                mCurrentSpot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
                mCurrentSpot.setIsWaitingForARide(false);

                    //mCurrentSpot.setHitchability(my_location_fragment.getSelectedHitchability());
            }

            //Persist on DB
            DaoSession daoSession = appContext.getDaoSession();
            SpotDao spotDao = daoSession.getSpotDao();
            spotDao.insertOrReplace(mCurrentSpot);
            ((MyHitchhikingSpotsApplication) getApplicationContext()).setCurrentSpot(mCurrentSpot);
            Toast.makeText(getApplicationContext(), R.string.spot_saved_successfuly, Toast.LENGTH_LONG).show();

        } catch (Exception ex) {
            Crashlytics.logException(ex);
            ((BaseActivity) getParent()).showErrorAlert("Save shortcut", getString(R.string.shortcut_save_button_failed));
        }
    }
}
