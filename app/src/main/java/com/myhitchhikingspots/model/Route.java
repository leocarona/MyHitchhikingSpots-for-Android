package com.myhitchhikingspots.model;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;

import java.util.List;

/**
 * Represents a route. A route has many spots (features) and subRoutes.
 * Let's say a user hitchhikes from A to B and B to C, and then takes a bus from C to D, and hitchhikes again from D to his final destination E.
 * The spots (features) of this route would be A, B, C, D, E - where A, B, D and E are hitchhiking spots, and C is a non-hitchhiking spot.
 * The total of subRoutes on this route would be 3 - where subRoute[0] = A, B, C subRoute[1] = C, D subRoute[2] = D, E.
 **/
public class Route {
    /**
     * All spots that belong to this route.
     **/
    public List<Spot> spots;

    /**
     * All spots that belong to this route split into sub routes. A sub route
     **/
    public List<SubRoute> subRoutes;
}


