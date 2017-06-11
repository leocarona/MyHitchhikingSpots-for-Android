package com.myhitchhikingspots;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.myhitchhikingspots.model.Spot;

import java.util.Date;

public class StartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
        Spot lastAdded = appContext.getLastAddedSpot();

        if (lastAdded == null || lastAdded.getIsDestination())
            startActivity(new Intent(getApplicationContext(), MapViewActivity.class));
        else {
            Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);

            Spot spot = null;
            if (lastAdded.getIsWaitingForARide()) {
                spot = lastAdded;
                intent.putExtra(Constants.SHOULD_SHOW_BUTTONS_KEY, false);
            } else
                intent.putExtra(Constants.SHOULD_SHOW_BUTTONS_KEY, true);

            intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
            finish();
            startActivity(intent);
        }

    }
}
