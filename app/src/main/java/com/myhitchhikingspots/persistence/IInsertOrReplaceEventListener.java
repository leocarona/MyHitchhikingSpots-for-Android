package com.myhitchhikingspots.persistence;

import androidx.annotation.NonNull;

public interface IInsertOrReplaceEventListener {
    void onSuccess(int numberOfSpotsOnTheListNow);

    void onFailed(@NonNull Exception ex);
}
