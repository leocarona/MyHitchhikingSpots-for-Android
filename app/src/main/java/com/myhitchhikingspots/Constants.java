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

    public static final String SPOT_BUNDLE_EXTRA_KEY = PACKAGE_NAME + ".SPOT";
    public static final String SPOT_BUNDLE_EXTRA_ID_KEY = PACKAGE_NAME + ".SPOT_ID";

    //Zoom to a level that makes it easier for the user to find his new position (he might not be too far from the location shown)
    public static final int ZOOM_TO_SEE_FARTHER_DISTANCE = 6;
    public static final int ZOOM_TO_SEE_CLOSE_TO_SPOT = 17;
}
