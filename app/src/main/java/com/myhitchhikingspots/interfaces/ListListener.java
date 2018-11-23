package com.myhitchhikingspots.interfaces;

import com.myhitchhikingspots.model.Spot;

/**
 * Created by leoboaventura on 28/07/2017.
 */

public interface ListListener {
    void onSpotClicked(Spot spot);
    void onListOfSelectedSpotsChanged();
}
