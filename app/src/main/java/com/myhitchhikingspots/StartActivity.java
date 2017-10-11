package com.myhitchhikingspots;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.myhitchhikingspots.model.Spot;

public class StartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());
        Spot lastRouteSpot = appContext.getLastAddedRouteSpot();

        //If the last route spot saved was a destination, show the map. Otherwise, the user must be on the road right now, so:
        // If it was a spot that is still waiting for a ride, show SpotForm so that the user can evalute it.
        // If it was none of the previous cases, then the user is not waiting for a ride and we can present SpotForm so that a next spot can be added to the route.
        if (lastRouteSpot == null || (lastRouteSpot.getIsDestination() != null && lastRouteSpot.getIsDestination()))
            startActivity(new Intent(getApplicationContext(), MyMapsActivity.class));
        else {
            //User is probably on the road right now!
            Intent intent = new Intent(getBaseContext(), SpotFormActivity.class);

            Spot spot = null;
            if (lastRouteSpot.getIsWaitingForARide() != null && lastRouteSpot.getIsWaitingForARide()) {
                spot = lastRouteSpot;
                intent.putExtra(Constants.SHOULD_SHOW_BUTTONS_KEY, false);
            } else {
                //Let the user add a new spot to his route
                spot = new Spot();
                spot.setIsHitchhikingSpot(true);
                spot.setIsPartOfARoute(true);
                intent.putExtra(Constants.SHOULD_SHOW_BUTTONS_KEY, true);
            }

            intent.putExtra(Constants.SPOT_BUNDLE_EXTRA_KEY, spot);
            finish();
            startActivity(intent);
        }

    }
}
