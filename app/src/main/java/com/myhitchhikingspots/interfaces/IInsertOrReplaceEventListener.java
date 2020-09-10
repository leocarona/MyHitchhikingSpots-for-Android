package com.myhitchhikingspots.interfaces;

import androidx.annotation.NonNull;

public interface IInsertOrReplaceEventListener {
    void onSuccess(int numberOfSpotsOnTheListNow);

    void onFailed(@NonNull Exception ex);
}
