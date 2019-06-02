package com.myhitchhikingspots.model;

import com.mapbox.geojson.Point;

import java.util.List;

/**
 * A part of a route where the user has traveled only hitchhiking or only non-hitchhiking.
 * E.g. If user starts traveling from A to D;
 * he hitchhikes from A to B, and from B to C;
 * but he goes non-hitchhiking (by bus, flight, walk, whatever) from C to D;
 * then his route from A to D consists in 2 sub routes: a hitchhiking SubRoute from A to C, and a non-hitchhiking SubRoute from C to D.
 * Notice that on this example, the SubRoute from A to C consisted of 2 hitchhiking spots,
 * but the number of 'points' on this SubRoute is 3, being the 2 first points the hitchhiking spots and the third point corresponds to (C) where the user has arrived.
 **/
public class SubRoute {
    /**
     * True if the user has traveled this sub route by hitchhiking;
     * False if he did it by another matter of transportation (by bus, walk, flight, etc).
     **/
    public boolean isHitchhikingRoute;

    /**
     * All spots of the sub route as points.
     * If the last spot of the sub route was not the route's destination, then the last point on this list corresponds to the next spot on the route.
     * For more information, see {@link SubRoute} class summary.
     **/
    public List<Point> points;
}
