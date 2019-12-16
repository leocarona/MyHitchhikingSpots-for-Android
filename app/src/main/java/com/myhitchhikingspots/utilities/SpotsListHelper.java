package com.myhitchhikingspots.utilities;

import com.mapbox.geojson.Feature;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.MyMapsFragment;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class SpotsListHelper {

    public static ArrayList<DateTime> getMostRecentDatesOnEachRoute(List<Feature> features) {
        // List<Feature> features = spotsCollection.features();
        if (features == null)
            return new ArrayList<>();

        ArrayList<DateTime> mostRecentDatesOfEachRoute = new ArrayList<>();

        Feature lastFeature = features.get(0);
        int routeIndexBeingSearched = (int) lastFeature.getNumberProperty(MyMapsFragment.PROPERTY_ROUTEINDEX);
        DateTime mostRecentStartDateTimeOnThisRoute = getStartDateTimeFrom(lastFeature);

        //Search for the oldest startDateTime of each route
        for (Feature currentFeature : features) {
            DateTime currentFeatureStartDateTime = getStartDateTimeFrom(currentFeature);
            int currentFeatureRouteIndex = (int) currentFeature.getNumberProperty(MyMapsFragment.PROPERTY_ROUTEINDEX);

            //If current feature belongs to a different route, add mostRecentStartDateTimeOnThisRoute and
            // reset values to move on to the searching the next route.
            if (currentFeatureRouteIndex != routeIndexBeingSearched) {
                mostRecentDatesOfEachRoute.add(mostRecentStartDateTimeOnThisRoute);
                routeIndexBeingSearched = currentFeatureRouteIndex;
                mostRecentStartDateTimeOnThisRoute = currentFeatureStartDateTime;
                continue;
            }

            if (currentFeatureStartDateTime.isAfter(mostRecentStartDateTimeOnThisRoute))
                mostRecentStartDateTimeOnThisRoute = currentFeatureStartDateTime;
        }

        //Make sure the last mostRecentStartDateTimeOnThisRoute is also added
        mostRecentDatesOfEachRoute.add(mostRecentStartDateTimeOnThisRoute);

        Collections.sort(mostRecentDatesOfEachRoute);

        return mostRecentDatesOfEachRoute;
    }

    public static ArrayList<DateTime> getOldestDatesOnEachRoute(List<Feature> features) {
        if (features == null)
            return new ArrayList<>();

        ArrayList<DateTime> oldestDatesOfEachRoute = new ArrayList<>();

        Feature lastFeature = features.get(0);
        int routeIndexBeingSearched = (int) lastFeature.getNumberProperty(MyMapsFragment.PROPERTY_ROUTEINDEX);
        DateTime oldestStartDateTimeOnThisRoute = getStartDateTimeFrom(lastFeature);

        //Search for the oldest startDateTime of each route
        for (Feature currentFeature : features) {
            if ((int) currentFeature.getNumberProperty(MyMapsFragment.PROPERTY_SPOTTYPE) == Constants.SPOT_TYPE_SINGLE_SPOT)
                continue;
            if ((int) currentFeature.getNumberProperty(MyMapsFragment.PROPERTY_SPOTTYPE) == Constants.SPOT_TYPE_SINGLE_SPOT)
                continue;

            DateTime currentFeatureStartDateTime = getStartDateTimeFrom(currentFeature);
            int currentFeatureRouteIndex = (int) currentFeature.getNumberProperty(MyMapsFragment.PROPERTY_ROUTEINDEX);

            //If current feature belongs to a different route, add oldestStartDateTimeOnThisRoute and
            // reset values to move on to the searching the next route.
            if (currentFeatureRouteIndex != routeIndexBeingSearched) {
                oldestDatesOfEachRoute.add(oldestStartDateTimeOnThisRoute);
                routeIndexBeingSearched = currentFeatureRouteIndex;
                oldestStartDateTimeOnThisRoute = currentFeatureStartDateTime;
                continue;
            }

            if (currentFeatureStartDateTime.isBefore(oldestStartDateTimeOnThisRoute))
                oldestStartDateTimeOnThisRoute = currentFeatureStartDateTime;
        }

        //Make sure the last oldestStartDateTimeOnThisRoute is also added
        oldestDatesOfEachRoute.add(oldestStartDateTimeOnThisRoute);

        Collections.sort(oldestDatesOfEachRoute);

        return oldestDatesOfEachRoute;
    }

    /**
     * Get all the route indexes of
     * - routes that begin on startsOn date;
     * - routes that end on endsOn date; and
     * - routes that have been saved within startsOn and endsOn.
     **/
    public static ArrayList<Integer> getAllRouteIndexesOfFeaturesWithinDates(List<Feature> spotFeatures, DateTime startsOn, DateTime endsOn) {
        if (spotFeatures == null)
            return new ArrayList<>();

        ArrayList<Integer> lst = new ArrayList<>();
        for (Feature spotFeature : spotFeatures) {
            Integer routeIndex = (Integer) spotFeature.getNumberProperty(MyMapsFragment.PROPERTY_ROUTEINDEX);
            if (lst.contains(routeIndex))
                continue;
            if ((int) spotFeature.getNumberProperty(MyMapsFragment.PROPERTY_SPOTTYPE) == Constants.SPOT_TYPE_SINGLE_SPOT)
                continue;

            // Ignore the time on the dates
            DateTime date = getStartDateTimeFrom(spotFeature);
            if (shouldIncludeSpot(date, startsOn, endsOn))
                lst.add(routeIndex);
        }
        return lst;
    }

    /**
     * Get a list with all the dates when single spots have been saved.
     **/
    public static ArrayList<DateTime> getSingleSpotsDates(List<Feature> features) {
        if (features == null)
            return new ArrayList<>();

        ArrayList<DateTime> singleSpotsDates = new ArrayList<>();

        for (Feature currentFeature : features) {
            if ((int) currentFeature.getNumberProperty(MyMapsFragment.PROPERTY_SPOTTYPE) == Constants.SPOT_TYPE_SINGLE_SPOT) {
                DateTime currentFeatureStartDateTime = SpotsListHelper.getStartDateTimeFrom(currentFeature);
                singleSpotsDates.add(currentFeatureStartDateTime);
            }
        }

        Collections.sort(singleSpotsDates);

        return singleSpotsDates;
    }

    public static DateTime getStartDateTimeFrom(Feature feature) {
        return new DateTime((long) feature.getNumberProperty(MyMapsFragment.PROPERTY_STARTDATETIME_IN_MILLISECS), DateTimeZone.UTC);
    }

    public static boolean shouldIncludeSpot(DateTime date, DateTime startsOn, DateTime endsOn) {
        Calendar calDate = Utils.getCalendarAtMidnight(date),
                calStartsOn = Utils.getCalendarAtMidnight(startsOn),
                calEndsOn = Utils.getCalendarAtMidnight(endsOn);
        return shouldIncludeSpot(calDate, calStartsOn, calEndsOn);
    }

    public static boolean shouldIncludeSpot(Calendar calDate, Calendar calStartsOn, Calendar calEndsOn) {
        return calDate.equals(calStartsOn) || calDate.equals(calEndsOn) ||
                (calDate.after(calStartsOn) && calDate.before(calEndsOn));
    }
}
