package com.myhitchhikingspots.interfaces;

import com.myhitchhikingspots.model.Spot;

/**
 * Created by leoboaventura on 28/07/2017.
 */

public interface CheckboxListener {
    void notifySpotCheckedChanged(Spot spot, Boolean isChecked);
    void notifySpotClicked(Spot spot);
}
