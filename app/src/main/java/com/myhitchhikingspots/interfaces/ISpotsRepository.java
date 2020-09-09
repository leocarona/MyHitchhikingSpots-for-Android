package com.myhitchhikingspots.interfaces;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.persistence.IInsertOrReplaceEventListener;

import java.util.List;

public interface ISpotsRepository {
    LiveData<List<Spot>> getSpots(@Nullable Context context);

    void loadSpots(@Nullable Context context);

    void reloadSpots(@Nullable Context context);

    Spot getWaitingSpot(@Nullable Context context);

    Spot getLastAddedRouteSpot(@Nullable Context context);

    void insertOrReplace(@Nullable Context context, @NonNull Spot spot) throws Exception;

    void insertOrReplace(@Nullable Context context, @NonNull List<Spot> spots, boolean shouldGenerateNewIds, @NonNull final IInsertOrReplaceEventListener callback) throws Exception;

    void deleteSpots(@NonNull List<String> spotsToBeDeleted_idList, @Nullable Context context);

    void deleteSpot(@NonNull Spot spot, @Nullable Context context);

    void deleteAllSpots(@Nullable Context context);

    boolean isAnySpotMissingAuthor(@Nullable Context context);

    void assignMissingAuthorTo(@Nullable Context context, @Nullable String userId, @NonNull String username);
}
