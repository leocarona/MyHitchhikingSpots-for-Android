package com.myhitchhikingspots;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import android.os.Handler;

/**
 * Created by leoboaventura on 11/03/2016.
 */
public class RemoteControlReceiver extends BroadcastReceiver {
    protected static String TAG_MEDIA = "mytag";
    /*
    // Constructor is mandatory
    public RemoteControlReceiver ()
    {
        super ();
    }*/

    /* private final Handler handler; // Handler used to execute code on the UI thread

     public RemoteControlReceiver(Handler handler) {
         this.handler = handler;
     }
 */
    public RemoteControlReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
              /* handle media button intent here by reading contents */
            /* of EXTRA_KEY_EVENT to know which key was pressed    */

            String intentAction = intent.getAction();
            Log.i(TAG_MEDIA, intentAction.toString() + " happended");
            if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
                Log.i(TAG_MEDIA, "no media button information");
                return;
            }
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                Log.i(TAG_MEDIA, "no keypress");
                return;
            }
            // other stuff you want to do

            try {
                //Get currentSpot and update it
                Spot mCurrentSpot = ((MyHitchhikingSpotsApplication) context.getApplicationContext()).getCurrentSpot();

                DateTime date = new DateTime(mCurrentSpot.getStartDateTime());
                Integer waiting_time = Minutes.minutesBetween(date, DateTime.now()).getMinutes();

                mCurrentSpot.setWaitingTime(waiting_time);
                mCurrentSpot.setAttemptResult(Constants.ATTEMPT_RESULT_UNKNOWN);
                mCurrentSpot.setIsWaitingForARide(false);

                //Persist on DB
                DaoSession daoSession = ((MyHitchhikingSpotsApplication) context.getApplicationContext()).getDaoSession();
                SpotDao spotDao = daoSession.getSpotDao();
                spotDao.insertOrReplace(mCurrentSpot);
                ((MyHitchhikingSpotsApplication) context.getApplicationContext()).setCurrentSpot(mCurrentSpot);
                Toast.makeText(context.getApplicationContext(), R.string.spot_saved_successfuly, Toast.LENGTH_LONG).show();
                //finish();
            } catch (Exception ex) {
                Log.e(TAG_MEDIA, "saveButtonHandler", ex);
                //Toast.makeText(getApplicationContext(), "Something went wrong :(", Toast.LENGTH_LONG).show();
            }

            /*
            // Post the UI updating code to our Handler
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Toast from broadcast receiver", Toast.LENGTH_SHORT).show();
                }
            });*/
        }
    }

}