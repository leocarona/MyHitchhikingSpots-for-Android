package com.myhitchhikingspots;

import com.mapbox.geojson.Point;
import com.myhitchhikingspots.model.Route;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.utilities.Utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class UtilsUnitTests_parseDBSpotList {

    @Test
    public void parseDBSpotList_routeWithOnlyHitchhikingSpots_ReturnsOnlyOneSubRoute() {
        //User has traveled by hitchhiking from A to B, and from B to his destination C
        Spot A = newSpot();
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        A.setIsPartOfARoute(true);

        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        B.setIsPartOfARoute(true);

        Spot C = newSpot();
        C.setIsHitchhikingSpot(true);
        C.setIsDestination(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.size()).isEqualTo(1);
    }

    @Test
    public void parseDBSpotList_routeWith3NonHitchhikingSpots_ReturnsSubRouteOfNonHitchhikingSpots() {
        //User has traveled non-hitchhiking from A to B, and from B to C, but his destination after C is unknown
        List<Spot> spotList = getRouteWith3NonHitchhikingSpots();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.get(0).isHitchhikingRoute).isEqualTo(false);
    }

    @Test
    public void parseDBSpotList_routeWith3NonHitchhikingSpots_ReturnsNumberOfPointsEqualsNumberOfSpots() {
        //User has traveled non-hitchhiking from A to B, and from B to C, but his destination after C is unknown
        List<Spot> spotList = getRouteWith3NonHitchhikingSpots();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //The number of points should be 3, because spotList had nothing more than 3 non-hitchhiking spots (A, B and C)
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(3);
    }

    @Test
    public void parseDBSpotList_hitchhikingRouteWithUnknownDestination_ReturnsNumberOfPointsEqualsNumberOfSpots() {
        //User has traveled by hitchhiking from A to B, and from B to C, but his destination after C is unknown
        Spot A = newSpot();
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        A.setIsPartOfARoute(true);

        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        B.setIsPartOfARoute(true);

        Spot C = newSpot();
        C.setIsHitchhikingSpot(true);
        C.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        C.setIsPartOfARoute(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //The number of points should be 3, because spotList had nothing more than 3 hitchhiking spots (A, B and C)
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(3);
    }

    @Test
    public void parseDBSpotList_routeWithHitchhikingAndNonHitchhikingSpots_Returns2SubRoutes() {
        List<Spot> spotList = getRouteWithHitchhikingAndNonHitchhikingSpots();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_routeWithHitchhikingAndNonHitchhikingSpots_Returns2PointsOnFirstSubRoute() {
        List<Spot> spotList = getRouteWithHitchhikingAndNonHitchhikingSpots();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //First sub route should have 2 points (A and B)
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_routeWithHitchhikingAndNonHitchhikingSpots_Returns1PointOnSecondSubRoute() {
        List<Spot> spotList = getRouteWithHitchhikingAndNonHitchhikingSpots();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //Second sub route should have 1 point only (B)
        assertThat(routes.get(0).subRoutes.get(1).points.size()).isEqualTo(1);
    }

    @Test
    public void parseDBSpotList_routeWith2HitchhikingSpots_Returns2Points() {
        //User has traveled by hitchhiking at A and at B, but his destination after B is unknown
        Spot A = newSpot();
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        A.setIsPartOfARoute(true);

        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        B.setIsPartOfARoute(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_hitchhikingRouteStartingWithNonHitchhikingSpot_ReturnsTwoSubRoutes() {
        //User has saved a non-hitchhiking spot at A, and then got a ride from B to an unknown destination
        Spot A = newSpot();
        A.setIsPartOfARoute(true);

        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        B.setIsPartOfARoute(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.size()).isEqualTo(1);
        assertThat(routes.get(0).subRoutes.size()).isEqualTo(2);
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(2);
        assertThat(routes.get(0).subRoutes.get(1).points.size()).isEqualTo(1);
        assertThat(routes.get(0).subRoutes.get(0).isHitchhikingRoute).isEqualTo(false);
        assertThat(routes.get(0).subRoutes.get(1).isHitchhikingRoute).isEqualTo(true);
    }

    @Test
    public void parseDBSpotList_nonHitchhikingRouteStartingWithNonHitchhikingSpot_ReturnsOneSubRoute() {
        //User has saved a non-hitchhiking spot at A, and then took a break at spot B
        Spot A = newSpot();
        A.setIsPartOfARoute(true);

        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        B.setIsPartOfARoute(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.size()).isEqualTo(1);
        assertThat(routes.get(0).subRoutes.size()).isEqualTo(1);
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(2);
        assertThat(routes.get(0).subRoutes.get(0).isHitchhikingRoute).isEqualTo(false);
    }

    @Test
    public void parseDBSpotList_routeWith2HitchhikingSpotsAndANonHitchhikingSpot_Returns2SubRoutes() {
        List<Spot> spotList = getRouteWith2HitchhikingSpotsAndABreakSpot();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_routeWith2HitchhikingSpotsAndANonHitchhikingSpot_Returns3PointsOnTheFirstSubRoute() {
        List<Spot> spotList = getRouteWith2HitchhikingSpotsAndABreakSpot();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //First route should have 3 points (A, B and C)
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(3);
    }

    @Test
    public void parseDBSpotList_routeWith2HitchhikingSpotsAndAKnownDestination_Returns3Points() {
        List<Spot> spotList = getRouteWith2HitchhikingSpotsAndAKnownDestination();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(3);
    }

    @Test
    public void parseDBSpotList_routeWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot_Return3SubRoutes() {
        List<Spot> spotList = getRouteWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.size()).isEqualTo(3);
    }

    @Test
    public void parseDBSpotList_routeWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot_ReturnFirstSubRouteAsHitchhikingRoute() {
        List<Spot> spotList = getRouteWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.get(0).isHitchhikingRoute).isEqualTo(true);
    }

    @Test
    public void parseDBSpotList_routeWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot_ReturnSecondSubRouteAsNonHitchhikingRoute() {
        List<Spot> spotList = getRouteWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.get(1).isHitchhikingRoute).isEqualTo(false);
    }

    @Test
    public void parseDBSpotList_routeWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot_ReturnSecondSubRouteWith2Points() {
        List<Spot> spotList = getRouteWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //Second sub route should have 2 points, which correspond to C and D
        assertThat(routes.get(0).subRoutes.get(1).points.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_routeWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot_ReturnThirdSubRouteWith1Point() {
        List<Spot> spotList = getRouteWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //Second sub route should have 1 point, which correspond D
        assertThat(routes.get(0).subRoutes.get(2).points.size()).isEqualTo(1);
    }

    @Test
    public void parseDBSpotList_twoRoutesWithKnownDestinations_ReturnTwoRoutes() {
        List<Spot> spotList = getTwoRoutesWithKnownDestinations();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_twoRoutesWithKnownDestinations_Return2Routes() {
        List<Spot> spotList = getTwoRoutesWithKnownDestinations();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_twoRoutesWithKnownDestinations_ReturnFirstRouteWith1SubRoute() {
        List<Spot> spotList = getTwoRoutesWithKnownDestinations();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //First route has 3 points (A, B, C)
        assertThat(routes.get(0).subRoutes.size()).isEqualTo(1);
    }

    @Test
    public void parseDBSpotList_twoRoutesWithKnownDestinations_ReturnSecondRouteWith1SubRoute() {
        List<Spot> spotList = getTwoRoutesWithKnownDestinations();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //Second route has 2 points (D, E)
        assertThat(routes.get(1).subRoutes.size()).isEqualTo(1);
    }

    @Test
    public void parseDBSpotList_twoRoutesWithKnownDestinations_ReturnFirstRouteWith3Points() {
        List<Spot> spotList = getTwoRoutesWithKnownDestinations();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //First sub route has 3 points (A, B, C)
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(3);
    }

    @Test
    public void parseDBSpotList_twoRoutesWithKnownDestinations_ReturnSecondRouteWith2Points() {
        List<Spot> spotList = getTwoRoutesWithKnownDestinations();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //Second sub route has 2 points (D, E)
        assertThat(routes.get(1).subRoutes.get(0).points.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_spotsListWith2HitchhikingSpotsAnd1SingleSpot_Returns1PointOnTheSingleSpotList() {
        List<Spot> spotList = getSpotsListWith2HitchhikingSpotsAnd1SingleSpot();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(singleSpots.size()).isEqualTo(1);
    }

    @Test
    public void parseDBSpotList_spotsListWith2HitchhikingSpotsAnd1SingleSpot_ReturnsRoutesWithPointsCorrespondingToTheHitchhikingSpots() {
        List<Spot> spotList = getSpotsListWith2HitchhikingSpotsAnd1SingleSpot();

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_tookABreakOnFirstAttemptThenSavedANonHitchhikingSpot_ReturnsNonHitchhikingSubRoute() {
        //Took a break at A, saved a non-hitchhiking spot at B and then arrived at destination B
        Spot A = newSpot();
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        A.setIsPartOfARoute(true);

        Spot B = newSpot();
        B.setIsPartOfARoute(true);
        B.setIsDestination(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.size()).isEqualTo(1);
        assertThat(routes.get(0).subRoutes.get(0).isHitchhikingRoute).isEqualTo(false);
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_spotListWithOneGotARideSpotAndOneNonHitchhikingSpots_ReturnsNonHitchhikingSubRoute() {
        //Hitchhiked at A, then saved a non-hitchhiking spot B, and finally saved the destination C
        Spot A = newSpot();
        A.setIsPartOfARoute(true);
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);

        //Saved a random non-hitchhiking spot B which is part of the route
        Spot B = newSpot();
        B.setIsPartOfARoute(true);

        //Arrived at destination C
        Spot C = newSpot();
        C.setIsPartOfARoute(true);
        C.setIsDestination(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.size()).isEqualTo(1);
        assertThat(routes.get(0).subRoutes.get(0).isHitchhikingRoute).isEqualTo(true);
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(3);
    }

    @Test
    public void parseDBSpotList_spotListWithOneBreakSpotAndOneNonHitchhikingSpot_ReturnsNonHitchhikingSubRoute() {
        //Took a break at A, then saved a non-hitchhiking spot at B, and finally arrived at the destination C
        Spot A = newSpot();
        A.setIsPartOfARoute(true);
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);

        //Saved a random non-hitchhiking spot B which is part of the route
        Spot B = newSpot();
        B.setIsPartOfARoute(true);

        //Arrived at destination C
        Spot C = newSpot();
        C.setIsPartOfARoute(true);
        C.setIsDestination(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.size()).isEqualTo(1);
        assertThat(routes.get(0).subRoutes.size()).isEqualTo(1);
        assertThat(routes.get(0).subRoutes.get(0).isHitchhikingRoute).isEqualTo(false);
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(3);
    }

    @Test
    public void parseDBSpotList_spotListWithOneGotARideSpotAndOneGotOffHereSpot_ReturnsTwoSubRoutes() {
        //Got a ride at A, then saved a non-hitchhiking spot at B, and finally arrived at the destination C
        Spot A = newSpot();
        A.setIsPartOfARoute(true);
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);

        //Got off at B
        Spot B = newSpot();
        B.setIsPartOfARoute(true);
        B.setIsGotOffHere(true);

        //Arrived at destination C
        Spot C = newSpot();
        C.setIsPartOfARoute(true);
        C.setIsDestination(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.size()).isEqualTo(1);
        //Two sub routes (hitchhiking from A to B, non-hitchhiking from B to C)
        assertThat(routes.get(0).subRoutes.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_spotListWithOneGotARideSpotAndOneGotOffHereSpot_ReturnsNonHitchhikingSubRouteStartingFromGotOffHereSpot() {
        //Got a ride at A, then saved a non-hitchhiking spot at B, and finally arrived at the destination C
        Spot A = newSpot();
        A.setIsPartOfARoute(true);
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);

        //Got off at B
        Spot B = newSpot();
        B.setIsPartOfARoute(true);
        B.setIsGotOffHere(true);

        //Arrived at destination C
        Spot C = newSpot();
        C.setIsPartOfARoute(true);
        C.setIsDestination(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        //Did not hitchhike from B to C
        assertThat(routes.get(0).subRoutes.get(1).isHitchhikingRoute).isEqualTo(false);
        //Second sub route should have 2 points (B, C)
        assertThat(routes.get(0).subRoutes.get(1).points.size()).isEqualTo(2);
    }

    @Test
    public void parseDBSpotList_spotListWithTwoGotARideSpotsAndTwoNonHitchhikingSpotsAndDestination_Returns() {
        //Got a ride from A to B, and from B to C, but C was not the destination
        Spot A = newSpot();
        A.setIsPartOfARoute(true);
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);

        Spot B = newSpot();
        B.setIsPartOfARoute(true);
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);

        //Between B and the destination E user has saved 2 non-hitchhiking spots C and D
        Spot C = newSpot();
        C.setIsPartOfARoute(true);

        Spot D = newSpot();
        D.setIsPartOfARoute(true);

        //Arrived at destination E
        Spot E = newSpot();
        E.setIsPartOfARoute(true);
        E.setIsDestination(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);
        spotList.add(D);
        spotList.add(E);

        List<Route> routes = new ArrayList<>();
        List<Spot> singleSpots = new ArrayList<>();

        Utils.parseDBSpotList(spotList, routes, singleSpots);

        assertThat(routes.get(0).subRoutes.size()).isEqualTo(1);
        assertThat(routes.get(0).subRoutes.get(0).isHitchhikingRoute).isEqualTo(true);
        assertThat(routes.get(0).subRoutes.get(0).points.size()).isEqualTo(5);
    }

    private static List<Spot> getRouteWith3NonHitchhikingSpots() {
        //User has traveled non-hitchhiking from A to B, and from B to C
        Spot A = newSpot();
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        A.setIsPartOfARoute(true);

        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        B.setIsPartOfARoute(true);

        Spot C = newSpot();
        C.setIsHitchhikingSpot(true);
        C.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        C.setIsPartOfARoute(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);
        return spotList;
    }

    private static List<Spot> getRouteWithHitchhikingAndNonHitchhikingSpots() {
        //User has traveled by hitchhiking from A to B
        Spot A = newSpot();
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        A.setIsPartOfARoute(true);

        //User has traveled non-hitchhiking from B to an unknown destination
        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        B.setIsPartOfARoute(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        return spotList;
    }

    private static List<Spot> getTwoRoutesWithKnownDestinations() {
        //Hitchhiked from A to B and also from B to the destination C
        Spot A = newSpot();
        A.setIsPartOfARoute(true);
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);

        Spot B = newSpot();
        B.setIsPartOfARoute(true);
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);

        Spot C = newSpot();
        C.setIsPartOfARoute(true);
        C.setIsDestination(true);

        //Hitchhiked from D to the destination E
        Spot D = newSpot();
        D.setIsPartOfARoute(true);
        D.setIsHitchhikingSpot(true);
        D.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);

        Spot E = newSpot();
        E.setIsPartOfARoute(true);
        E.setIsDestination(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);
        spotList.add(D);
        spotList.add(E);
        return spotList;
    }

    private static List<Spot> getRouteWith2HitchhikingSpotsAndABreakSpot() {
        //User has traveled by hitchhiking from A to B, and from B to C
        Spot A = newSpot();
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        A.setIsPartOfARoute(true);

        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        B.setIsPartOfARoute(true);

        //User has taken a break at C
        Spot C = newSpot();
        C.setIsHitchhikingSpot(true);
        C.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        C.setIsPartOfARoute(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);
        return spotList;
    }

    private static List<Spot> getRouteWith2HitchhikingSpotsAndAKnownDestination() {
        //User has traveled by hitchhiking from A to B, and from B to his destination C
        Spot A = newSpot();
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        A.setIsPartOfARoute(true);

        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        B.setIsPartOfARoute(true);

        Spot C = newSpot();
        C.setIsPartOfARoute(true);
        C.setIsDestination(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);
        return spotList;
    }

    private static List<Spot> getRouteWithSequenceOf2HitchhikingSpots1NonHitchhikingSpotAnd1HitchhikingSpot() {
        //Hitchhiked from A to B and from B to C
        Spot A = newSpot();
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        A.setIsPartOfARoute(true);

        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        B.setIsPartOfARoute(true);

        //Went non-hitchhiking from C to D
        Spot C = newSpot();
        C.setIsHitchhikingSpot(true);
        C.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        C.setIsPartOfARoute(true);

        //Went from D hitchhiking to an unknown destination
        Spot D = newSpot();
        D.setIsHitchhikingSpot(true);
        D.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        D.setIsPartOfARoute(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);
        spotList.add(D);

        return spotList;
    }

    private static List<Spot> getSpotsListWith2HitchhikingSpotsAnd1SingleSpot() {
        //1 hitchhiking spot that's not part of any route (a.k.a single spot)
        Spot A = newSpot();
        A.setIsHitchhikingSpot(true);
        A.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        A.setIsPartOfARoute(false);

        //2 hitchhiking spots belonging to a same route
        Spot B = newSpot();
        B.setIsHitchhikingSpot(true);
        B.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        B.setIsPartOfARoute(true);

        Spot C = newSpot();
        C.setIsHitchhikingSpot(true);
        C.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        C.setIsPartOfARoute(true);

        List<Spot> spotList = new ArrayList<>();
        spotList.add(A);
        spotList.add(B);
        spotList.add(C);

        return spotList;
    }

    /**
     * Returns a new instance of Spot with Latitude and Longitude different than null.
     **/
    static Spot newSpot() {
        Spot A = new Spot();
        A.setLatitude(0.0);
        A.setLongitude(0.0);
        return A;
    }
}
