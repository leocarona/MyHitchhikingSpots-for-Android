package com.myhitchhikingspots;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.myhitchhikingspots.utilities.SpotsListHelper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import java.util.ArrayList;

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

    DateTime firstRoute_oldestDate = new DateTime(2000, 01, 01, 01, 00, 00, DateTimeZone.UTC);
    DateTime firstRoute_mostRecentDate = new DateTime(2001, 01, 01, 01, 00, 00, DateTimeZone.UTC);

    DateTime secondRoute_oldestDate = new DateTime(2001, 01, 01, 01, 00, 00, DateTimeZone.UTC);
    DateTime secondRoute_mostRecentDate = new DateTime(2002, 01, 01, 01, 00, 00, DateTimeZone.UTC);

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
}
