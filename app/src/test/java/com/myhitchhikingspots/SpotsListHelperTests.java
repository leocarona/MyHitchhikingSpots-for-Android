package com.myhitchhikingspots;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.SpotsListHelper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class SpotsListHelperTests {
    @Test
    public void getOldestDatesOnEachRoute_2routes_Returns2Dates() {
        ArrayList<Feature> features = getScenario();

        assertThat(SpotsListHelper.getOldestDatesOnEachRoute(features).size()).isEqualTo(2);
    }

    @Test
    public void getOldestDatesOnEachRoute_2Routes_ReturnsOldestDateOnFirstRoute() {
        ArrayList<Feature> features = getScenario();

        assertThat(SpotsListHelper.getOldestDatesOnEachRoute(features).get(0)).isEqualTo(firstRoute_oldestDate);
    }

    @Test
    public void getOldestDatesOnEachRoute_2Routes_ReturnsOldestDateOnSecondRoute() {
        ArrayList<Feature> features = getScenario();

        assertThat(SpotsListHelper.getOldestDatesOnEachRoute(features).get(1)).isEqualTo(secondRoute_oldestDate);
    }

    @Test
    public void getMostRecentDatesOnEachRoute_2routes_Returns2Dates() {
        ArrayList<Feature> features = getScenario();

        assertThat(SpotsListHelper.getMostRecentDatesOnEachRoute(features).size()).isEqualTo(2);
    }

    @Test
    public void getMostRecentDatesOnEachRoute_2Routes_ReturnsMostRecentDateOnFirstRoute() {
        ArrayList<Feature> features = getScenario();

        assertThat(SpotsListHelper.getMostRecentDatesOnEachRoute(features).get(1)).isEqualTo(secondRoute_mostRecentDate);
    }

    @Test
    public void getMostRecentDatesOnEachRoute_2Routes_ReturnsMostRecentDateOnSecondRoute() {
        ArrayList<Feature> features = getScenario();

        assertThat(SpotsListHelper.getMostRecentDatesOnEachRoute(features).get(1)).isEqualTo(secondRoute_mostRecentDate);
    }

    @Test
    public void getAllRouteIndexesOfFeaturesWithinDates_2Routes_ReturnsBothRouteIndexex() {
        ArrayList<Feature> features = getScenario();

        // Assert that getAllRouteIndexesOfFeaturesWithinDates returns
        // the routes that start on secondRoute_oldestDate and the routes that end on this same date.

        assertThat(SpotsListHelper.getAllRouteIndexesOfFeaturesWithinDates(features, secondRoute_oldestDate, secondRoute_mostRecentDate).size()).isEqualTo(2);
    }

    @Test
    public void getAllRouteIndexesOfFeaturesWithinDates_2Routes_ReturnsBothRoutesIndex() {
        ArrayList<Feature> features = getScenario();

        assertThat(SpotsListHelper.getAllRouteIndexesOfFeaturesWithinDates(features, firstRoute_oldestDate, secondRoute_mostRecentDate)).containsExactlyElementsIn(new Integer[]{1, 2});
    }

    @Test
    public void getWaitingTimeOccurrences_4spotGotARide_Returns3SpotsHitchedBetween60To90Minutes() {
        //Scenario: 4 spots where user got rides. At 3 of them he awaited for 1hour and at 1 of them he awaited for 25 hours.
        List<Spot> spotList = getScenario2();

        List<Integer> waitingTimeOccurences = SpotsListHelper.getWaitingTimeOccurrencesEachHalfHour(spotList);

        //Because 1 hour = 60 min, we expect to find the number of rides 3 on the period of time between 60-90min.
        //The period of time between 60-90min should be the index 2 (result of 60/30) on the list.
        //Because 25 hours = 1500min, we expect to find the number of rides 1 on the period of time between 1500-1530min.
        //The period of time between 1500-1530 should be the index 50 (result of 1500/30) on the list.

        assertThat(waitingTimeOccurences.get(2)).isEqualTo(3);
    }

    @Test
    public void getWaitingTimeOccurrences_4spotGotARide_Returns1SpotsHitchedBetween1500To1530Minutes() {
        //Scenario: 4 spots where user got rides. At 3 of them he awaited for 1hour and at 1 of them he awaited for 25 hours.
        List<Spot> spotList = getScenario2();

        List<Integer> waitingTimeOccurences = SpotsListHelper.getWaitingTimeOccurrencesEachHalfHour(spotList);

        //Because 25 hours = 1500min, we expect to find the number of rides 1 on the period of time between 1500-1530min.
        //The period of time between 1500-1530 should be the index 50 (result of 1500/30) on the list.
        //Because 1 hour = 60 min, we expect to find the number of rides 3 on the period of time between 60-90min.
        //The period of time between 60-90min should be the index 2 (result of 60/30) on the list.

        assertThat(waitingTimeOccurences.get(50)).isEqualTo(1);
    }

    @Test
    public void getNumberOfOccurrencesPerHour_4spotGotARideAt1oclock_Returns4AtPosition1() {
        //Scenario: 4 spots where user got rides. At 3 of them he got a ride at 1am of the same and at 1 of them he got a ride at 1am of the next day.
        List<Spot> spots = getScenario2();

        assertThat(SpotsListHelper.getNumberOfOccurrencesPerHour(spots)[1]).isEqualTo(4);
    }

    @Test
    public void getSpotList__4spotGotARideAt1oclock_Returns4() {
        //Scenario: 4 spots where user got rides.
        List<Spot> spots = getScenario2();

        assertThat(SpotsListHelper.getNumberOfRidesGotten(spots)).isEqualTo(4);
    }

    DateTime firstRoute_oldestDate = new DateTime(2000, 01, 01, 01, 00, 00, DateTimeZone.UTC);
    DateTime firstRoute_mostRecentDate = new DateTime(2001, 01, 01, 01, 00, 00, DateTimeZone.UTC);

    DateTime secondRoute_oldestDate = new DateTime(2001, 01, 01, 01, 00, 00, DateTimeZone.UTC);
    DateTime secondRoute_mostRecentDate = new DateTime(2002, 01, 01, 01, 00, 00, DateTimeZone.UTC);

    DateTime randomDateAtMidnight = new DateTime(2000, 01, 01, 00, 00, 00, DateTimeZone.UTC);

    private ArrayList<Feature> getScenario() {
        ArrayList<Feature> features = new ArrayList<>();

        //Route 1 has 3 spots saved in the same day in different hours
        features.add(getFeature(1, firstRoute_oldestDate));
        features.add(getFeature(1, firstRoute_oldestDate.withDurationAdded(Duration.millis(100), 1)));
        features.add(getFeature(1, firstRoute_mostRecentDate));

        //Route 2 has 3 spots saved in the same day in different hours and features are not ordered by their time
        features.add(getFeature(2, secondRoute_mostRecentDate));
        features.add(getFeature(2, secondRoute_oldestDate));
        features.add(getFeature(2, secondRoute_oldestDate.withDurationAdded(Duration.millis(100), 1)));

        return features;
    }

    Feature getFeature(int routeIndex, int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour, int secondOfMinute) {
        return getFeature(routeIndex,
                new DateTime(year,
                        monthOfYear,
                        dayOfMonth,
                        hourOfDay,
                        minuteOfHour,
                        secondOfMinute,
                        DateTimeZone.UTC));
    }

    Feature getFeature(int routeIndex, DateTime dateTime) {
        return Feature.fromGeometry(
                Point.fromLngLat(0, 0),
                getProperties(routeIndex, dateTime));
    }

    JsonObject getProperties(int routeIndex, DateTime startDateTime) {
        JsonObject properties = new JsonObject();
        properties.addProperty(MyMapsFragment.PROPERTY_ROUTEINDEX, routeIndex);
        properties.addProperty(MyMapsFragment.PROPERTY_SPOTTYPE, Constants.SPOT_TYPE_POINT_ON_THE_ROUTE);
        properties.addProperty(MyMapsFragment.PROPERTY_STARTDATETIME_IN_MILLISECS, (long) startDateTime.getMillis());
        return properties;
    }

    List<Spot> getScenario2() {
        List<Spot> list = new ArrayList<>();

        // In all following spots user has started hitching at midnight
        Spot spot1_GotARide_at_1pm = getGotARideSpotAt(1); // here user got a ride at 1am
        Spot spot2_GotARide_at_1pm = getGotARideSpotAt(1);
        Spot spot3_GotARide_at_1pm = getGotARideSpotAt(1);
        Spot spot4_GotARide_at_1pm = getGotARideSpotAt(1 + 24); // here user got a ride at 1am of the next day (24hours later)

        list.add(spot1_GotARide_at_1pm);
        list.add(spot2_GotARide_at_1pm);
        list.add(spot3_GotARide_at_1pm);
        list.add(spot4_GotARide_at_1pm);

        return list;
    }

    Spot getGotARideSpotAt(int waitingTimeInMinutes) {
        Spot spot_GotARide_at_4pm = new Spot();
        spot_GotARide_at_4pm.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot_GotARide_at_4pm.setStartDateTime(randomDateAtMidnight);
        spot_GotARide_at_4pm.setWaitingTime(getHourInMinutes(waitingTimeInMinutes));
        return spot_GotARide_at_4pm;
    }

    int getHourInMinutes(int whatTimeIsIt) {
        return whatTimeIsIt * 60;
    }
}
