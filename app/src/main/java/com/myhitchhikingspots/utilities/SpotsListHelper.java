package com.myhitchhikingspots.utilities;

import androidx.core.util.Pair;

import com.mapbox.geojson.Feature;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.MyMapsFragment;
import com.myhitchhikingspots.model.Spot;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

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

    public static List<Pair<Integer, Integer>> getNumberOfOccurrences(List<Spot> spotList) {
        if (spotList.isEmpty())
            return new ArrayList<>();
        int[] numberOfOccurrencesPerHour = getNumberOfOccurrencesPerHour(spotList);

        List<Pair<Integer, Integer>> result = new ArrayList<>();
        //Add a position to each hour of the day (0-23h) and how many rides were hitched at each hour
        for (int hour = 0; hour < numberOfOccurrencesPerHour.length; hour++)
            result.add(new Pair<>(hour, numberOfOccurrencesPerHour[hour]));

        // Sort from the hour where more rides were gotten to the hour where less rides were gotten.
        Collections.sort(result, (integerIntegerPair, t1) -> {
            if (integerIntegerPair.second < t1.second)
                return 1;
            else if (integerIntegerPair.second > t1.second)
                return -1;
            else
                return 0;
        });

        return result;
    }

    public static int[] getNumberOfOccurrencesPerHour(List<Spot> spots) {
        int[] result = new int[24];
        for (Spot spot : spots) {
            if (spot.getAttemptResult() != null && spot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE && spot.getWaitingTime() != null) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTime(spot.getStartDateTime().toDate());
                cal.add(Calendar.MINUTE, spot.getWaitingTime());
                int hourOfOccurrence = cal.get(Calendar.HOUR_OF_DAY);
                result[hourOfOccurrence]++;
            }
        }
        return result;
    }

    /**
     * Get how many rides were hitched at each interval of 30 min.
     *
     * @return A list of pair where
     * the first element is another pair informing the period of time (0-30min; 30-60min; 60-90min; etc) and
     * the second element is the number of spots hitched within that interval.
     **/
    public static List<Pair<Pair<Integer, Integer>, Integer>> getWaitingTimeOccurrences(List<Spot> spotList) {
        if (spotList.isEmpty())
            return new ArrayList<>();
        List<Integer> numberOfOccurrencesPerHalfHour = getWaitingTimeOccurrencesEachHalfHour(spotList);

        List<Pair<Pair<Integer, Integer>, Integer>> result = new ArrayList<>();
        //Add a position to each interval of half hour (0-30; 30-60; 60-90; etc) and how many rides were hitched at interval
        for (int coefficient = 1; coefficient <= numberOfOccurrencesPerHalfHour.size(); coefficient++) {
            Pair<Integer, Integer> periodOfTime = new Pair<>((coefficient - 1) * 30, coefficient * 30);
            result.add(new Pair<>(periodOfTime, numberOfOccurrencesPerHalfHour.get(coefficient - 1)));
        }

        // Sort from the period of time where more rides were gotten to the period of time where less rides were gotten.
        Collections.sort(result, (integerIntegerPair, t1) -> {
            if (integerIntegerPair.second < t1.second)
                return 1;
            else if (integerIntegerPair.second > t1.second)
                return -1;
            else
                return 0;
        });

        return result;
    }

    /**
     * Get how many times user got rides within periods of half hour.
     * The index on the list is used to calculate the period of time, and the number on the list represents the number of rides gotten.
     *
     * @return List of number of occurrences within the period of time.
     * The index on the list plus 1 or 2 respectively represent the coefficient to be multiplied by 30 in order to find what's the beginning and the ending of the period of time.
     * E.g: if you find the number 25 on the index 2 of the list, you know that between 60-90min. How?
     * Do the math as follows:
     * (2[indexOnTheList] + 1) = 2[coefficient to find the beginning of the period];
     * (2[indexOnTheList] + 2) = 3[coefficient to find the ending of the period];
     * 2[coefficient to find the beginning of the period]*30 = 60;
     * 3[coefficient to find the beginning of the period]*30 = 90.
     * Because 25 is at the index 2 on the list you interpret that between 60-90min of waiting the user has gotten 25 rides.
     **/
    public static List<Integer> getWaitingTimeOccurrencesEachHalfHour(List<Spot> spots) {
        int numOfSpotsToCheck = getNumberOfRidesGotten(spots);
        List<Integer> result = new ArrayList<>();
        int spotsAccounted = 0;
        int times = 1;
        int min, max;
        // For each interval of 30 min, check how many Got A Ride spots have their waiting times within such interval
        while (spotsAccounted < numOfSpotsToCheck) {
            min = 30 * (times - 1);
            max = 30 * times;
            int occurancesWithinPeriodOfTime = 0;
            for (Spot spot : spots) {
                if (spot.getAttemptResult() != null && spot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE &&
                        spot.getWaitingTime() != null && spot.getWaitingTime() >= min && spot.getWaitingTime() < max) {
                    occurancesWithinPeriodOfTime++;
                    spotsAccounted++;

                    //Nothing else to check, stop for loop
                    if (spotsAccounted == numOfSpotsToCheck)
                        break;
                }
            }
            result.add(occurancesWithinPeriodOfTime);
            times++;
        }
        return result;
    }

    public static int getNumberOfRidesGotten(List<Spot> spotList) {
        int numOfRides = 0;
        for (Spot spot : spotList)
            if (spot.getAttemptResult() != null && spot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE)
                numOfRides++;
        return numOfRides;
    }

    public static int getShortestWaitingTime(List<Spot> spotList){
        Integer  shortestWaitingTime = 0;

        if (spotList.size() > 0)
            shortestWaitingTime = spotList.get(0).getWaitingTime() == null ? 0 : spotList.get(0).getWaitingTime();

        for (Spot spot : spotList) {
            Boolean isDestination = spot.getIsDestination() == null ? false : spot.getIsDestination();
            Boolean isHitchhikingSpot = spot.getIsHitchhikingSpot() == null ? false : spot.getIsHitchhikingSpot();

            if (isHitchhikingSpot) {

                if (!isDestination) {
                    Integer waitingTime = spot.getWaitingTime() == null ? 0 : spot.getWaitingTime();
                    //Only consider spots where the user has gotten rides
                    if (spot.getAttemptResult() != null && spot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE) {
                        if (waitingTime < shortestWaitingTime)
                            shortestWaitingTime = waitingTime;
                    }
                }
            }
        }
        return shortestWaitingTime;
    }

    public static int getLongestWaitingTime(List<Spot> spotList){
        int longestWaitingTime=0;
        if (spotList.size() > 0)
            longestWaitingTime = spotList.get(0).getWaitingTime() == null ? 0 : spotList.get(0).getWaitingTime();;

        for (Spot spot : spotList) {
            Boolean isDestination = spot.getIsDestination() == null ? false : spot.getIsDestination();
            Boolean isHitchhikingSpot = spot.getIsHitchhikingSpot() == null ? false : spot.getIsHitchhikingSpot();

            if (isHitchhikingSpot) {
                if (!isDestination) {
                    Integer waitingTime = spot.getWaitingTime() == null ? 0 : spot.getWaitingTime();
                    if (waitingTime > longestWaitingTime)
                        longestWaitingTime = waitingTime;
                }
            }
        }
        return longestWaitingTime;
    }
}