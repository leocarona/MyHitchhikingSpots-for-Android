package com.myhitchhikingspots;

/**
 * Created by leoboaventura on 08/03/2016.
 */
public final class Constants {
    public static String dbName = "my_hitchhiking_spots";

    public static final int SUCCESS_RESULT = 0;

    public static final int FAILURE_RESULT = 1;

    public static final String PACKAGE_NAME = "com.myhitchhikingspots";

    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";

    public static final String RESULT_ADDRESS_KEY = PACKAGE_NAME + ".RESULT_ADDRESS_KEY";
    public static final String RESULT_STRING_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";

    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";

    public static final Integer hitchabilityNumOfOptions = 5;

    //If the user current location is added to the SpotList it should be distinguished from the other saved spots with this ID
    public static final Long USER_CURRENT_LOCATION_SPOTLIST_ID = (long) -1;

    public static final int ATTEMPT_RESULT_UNKNOWN = 0;
    public static final int ATTEMPT_RESULT_GOT_A_RIDE = 1;
    public static final int ATTEMPT_RESULT_TOOK_A_BREAK = 2;

    public static final int SPOT_TYPE_UNKNOWN = 0;
    public static final int SPOT_TYPE_ORIGIN = 1;
    public static final int SPOT_TYPE_GOT_A_RIDE = 2;
    public static final int SPOT_TYPE_TOOK_A_BREAK = 3;
    public static final int SPOT_TYPE_WAITING = 4;
    public static final int SPOT_TYPE_DESTINATION = 5;

    public static final String SPOT_BUNDLE_EXTRA_KEY = PACKAGE_NAME + ".SPOT";
    public static final String SHOULD_SHOW_SPOT_SAVED_SNACKBAR_KEY = PACKAGE_NAME + ".SHOULD_SHOW_SPOT_SAVED_SNACKBAR_KEY";
    public static final String SHOULD_SHOW_SPOT_DELETED_SNACKBAR_KEY = PACKAGE_NAME + ".SHOULD_SHOW_SPOT_DELETED_SNACKBAR_KEY";
    public static final String SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY = PACKAGE_NAME + ".SHOULD_GO_BACK_TO_PREVIOUS_ACTIVITY_KEY";
    public static final String SHOULD_SHOW_BUTTONS_KEY = PACKAGE_NAME + ".SHOULD_SHOW_BUTTONS_KEY";
    public static final String SHOULD_RETRIEVE_HITCHWIKI_DETAILS_KEY = PACKAGE_NAME + ".SHOULD_RETRIEVE_HITCHWIKI_DETAILS_KEY";

    //Zoom to a level that makes it easier for the user to find his new position (he might not be too far from the location shown)
    public static final int ZOOM_TO_SEE_FARTHER_DISTANCE = 6;
    public static final int ZOOM_TO_SEE_CLOSE_TO_SPOT = 12;
    public static final int KEEP_ZOOM_LEVEL = -1;

    public static final String FILE_NAME_FOR_STORING_MARKERS = "markersStorageFile";
    public static final String FILE_NAME_FOR_STORING_COUNTRIES_LIST = "countriesStorageFile";
    public static final String FOLDERFORSTORINGMARKERS = "/markersStorageFolder";

    public static final String PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD = "hitchwikiSpotsTimestamp";
    public static final String PREFS_TIMESTAMP_OF_BACKUP = "backupTimestamp";
    public static final String PREFS_TIMESTAMP_OF_COUNTRIES_DOWNLOAD = "countriesDownloadedTimestamp";
    public static final String PREFS_TIMESTAMP_OF_LAST_OFFLINE_MODE_WARN = "lastOfflineModeWarnTimestamp";
    public static final String PREFS_OFFLINE_MODE_SHOULD_LOAD_CURRENT_VIEW = "shouldLoadCurrentView";

    public static final int CROUTON_DURATION_2500 = 2500;
    public static final int CROUTON_DURATION_5000 = 5000;
    public static final int CROUTON_DURATION_1500 = 1500;


    public static final String SHOULD_SYNC_EU = "SHOULD_SYNC_EU";
    public static final String SHOULD_SYNC_AS = "SHOULD_SYNC_AS";
    public static final String SHOULD_SYNC_AF = "SHOULD_SYNC_AF";
    public static final String SHOULD_SYNC_NA = "SHOULD_SYNC_NA";
    public static final String SHOULD_SYNC_SA = "SHOULD_SYNC_SA";
    public static final String SHOULD_SYNC_AN = "SHOULD_SYNC_AN";
    public static final String SHOULD_SYNC_OC = "SHOULD_SYNC_OC";

}
