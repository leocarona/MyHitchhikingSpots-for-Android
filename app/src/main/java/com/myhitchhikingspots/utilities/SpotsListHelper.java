package com.myhitchhikingspots.utilities;

import com.mapbox.geojson.Feature;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.MyMapsFragment;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
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

    public static ArrayList<Integer> getAllRouteIndexesOfFeaturesWithinDates(List<Feature> spotFeatures, DateTime startsOn, DateTime endsOn) {
        if (spotFeatures == null)
            return new ArrayList<>();

        ArrayList<Integer> lst = new ArrayList<>();
        for (Feature spotFeature : spotFeatures) {
            Integer routeIndex = (Integer) spotFeature.getNumberProperty(MyMapsFragment.PROPERTY_ROUTEINDEX);
            if (lst.contains(routeIndex))
                continue;

            DateTime dateTime = getStartDateTimeFrom(spotFeature);
            if (dateTime.isAfter(startsOn) && dateTime.isBefore(endsOn))
                lst.add(routeIndex);
        }
        return lst;
    }

    private static DateTime getStartDateTimeFrom(Feature feature) {
        return new DateTime((long) feature.getNumberProperty(MyMapsFragment.PROPERTY_STARTDATETIME_IN_MILLISECS), DateTimeZone.UTC);
    }
}
