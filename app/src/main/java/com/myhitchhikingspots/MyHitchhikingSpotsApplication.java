package com.myhitchhikingspots;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Debug;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

import io.fabric.sdk.android.Fabric;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by leoboaventura on 08/03/2016.
 */
public class MyHitchhikingSpotsApplication extends Application {
    public DaoSession daoSession;
    public Spot currentSpot;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        loadDatabase();
    }

    public void loadDatabase() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, Constants.dbName, null);
        SQLiteDatabase db = helper.getWritableDatabase();
        Integer v1 = db.getVersion();
        //helper.onUpgrade(db, 1, 2);
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();

        //insertSampleData2(daoSession);

        LoadCurrentWaitingSpot();
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, Constants.dbName, null);
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.rawQuery(sql, selectionArgs);
    }

    private void LoadCurrentWaitingSpot() {
        SpotDao spotDao = daoSession.getSpotDao();
        List<Spot> areWaitingForARide = spotDao.queryBuilder().where(SpotDao.Properties.IsWaitingForARide.eq(true))
                .orderDesc(SpotDao.Properties.IsPartOfARoute, SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();

        if (areWaitingForARide.size() > 1)
            Crashlytics.log(Log.INFO, "LoadCurrentWaitingSpot", "More than 1 spot was found with IsWaitingForARide set to true! This should never happen - Please be aware of this.");
        else if (areWaitingForARide.size() == 1)
            currentSpot = areWaitingForARide.get(0);
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }


    public Spot getCurrentSpot() {
        return currentSpot;
    }

    public Spot getLastAddedSpot() {
        SpotDao spotDao = daoSession.getSpotDao();
        Spot spot = spotDao.queryBuilder()
                .orderDesc(SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).limit(1).unique();

        return spot;
    }

    public Spot getLastNotGPSResolvedSpot() {
        SpotDao spotDao = daoSession.getSpotDao();
        Spot spot = spotDao.queryBuilder()
                .where(SpotDao.Properties.GpsResolved.eq(false))
                .orderDesc(SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).limit(1).unique();

        return spot;
    }

    public void setCurrentSpot(Spot currentSpot) {
        this.currentSpot = currentSpot;
    }

    final String TAG = "application-class";

    public void insertSampleData2(DaoSession daoSession) {
        //To prevent shit happening, don't execute this method unless it's not DEBUG mode
        if (!BuildConfig.DEBUG)
            return;

        SpotDao spotDao = daoSession.getSpotDao();

        //WARNING: Clear database here
        spotDao.deleteAll();
        Crashlytics.log(Log.WARN, TAG, "Spot list cleared");

        //Start hitchhiking at 8am
        GregorianCalendar spotStartDateTime = new GregorianCalendar(2016, 03, 10, 8, 00, 00);

        Spot spot;

        //FIRST DAY - Odense to Copenhagen
        spot = new Spot();
        spot.setCity("Odense");
        spot.setCountry("Denmark");
        spot.setLatitude(55.351012);
        spot.setLongitude(10.409609);
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setGpsResolved(true);
        spot.setWaitingTime(2);
        spotDao.insertOrReplace(spot);

        spotStartDateTime.add(Calendar.HOUR, 1);
        spot = new Spot();
        spot.setCity("Slagelse");
        spot.setCountry("Denmark");
        spot.setLatitude(55.389869);
        spot.setLongitude(11.359358);
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setGpsResolved(true);
        spot.setWaitingTime(25);
        spotDao.insertOrReplace(spot);


        spotStartDateTime.add(Calendar.HOUR, 1);
        spot = new Spot();
        spot.setCity("Copenhagen");
        spot.setCountry("Denmark");
        spot.setLatitude(55.668537);
        spot.setLongitude(12.556718);
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(true);
        spot.setGpsResolved(true);
        spot.setWaitingTime(45);
        spotDao.insertOrReplace(spot);


        //NEXT DAY - Copenhagen to Skagen
        spotStartDateTime = new GregorianCalendar(2016, 03, 11, 8, 00, 00);

        spot = new Spot();
        spot.setCity("Copenhagen");
        spot.setCountry("Denmark");
        spot.setLatitude(55.650766);
        spot.setLongitude(12.507314);
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setGpsResolved(true);
        spot.setWaitingTime(6);
        spotDao.insertOrReplace(spot);


        spotStartDateTime.add(Calendar.HOUR, 1);
        spot = new Spot();
        spot.setCity("Holbæk");
        spot.setCountry("Denmark");
        spot.setLatitude(55.659183);
        spot.setLongitude(11.675878);
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setGpsResolved(true);
        spot.setWaitingTime(23);
        spotDao.insertOrReplace(spot);


        spotStartDateTime.add(Calendar.HOUR, 1);
        spot = new Spot();
        spot.setCity("Sjællands Odde");
        spot.setCountry("Denmark");
        spot.setLatitude(55.978212);
        spot.setLongitude(11.301391);
        spot.setNote("Decided to pay for the ferry, didn't get a ride.");
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_TOOK_A_BREAK);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setGpsResolved(true);
        spot.setWaitingTime(5);
        spotDao.insertOrReplace(spot);


        spotStartDateTime.add(Calendar.HOUR, 1);
        spot = new Spot();
        spot.setCity("Aarhus");
        spot.setCountry("Denmark");
        spot.setLatitude(56.161744);
        spot.setLongitude(10.220832);
        spot.setNote("Hitchhiked a car leaving the ferry.");
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setGpsResolved(true);
        spot.setWaitingTime(10);
        spotDao.insertOrReplace(spot);


        spotStartDateTime.add(Calendar.HOUR, 1);
        spot = new Spot();
        spot.setCity("Skagen");
        spot.setCountry("Denmark");
        spot.setLatitude(57.738293);
        spot.setLongitude(10.632600);
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(true);
        spot.setGpsResolved(true);
        spot.setWaitingTime(15);
        spotDao.insertOrReplace(spot);


        //NEXT DAY - Skagen to Odense
        spotStartDateTime = new GregorianCalendar(2016, 03, 12, 8, 00, 00);

        spot = new Spot();
        spot.setCity("Skagen");
        spot.setCountry("Denmark");
        spot.setLatitude(57.725531);
        spot.setLongitude(10.559977);
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setGpsResolved(true);
        spot.setWaitingTime(67);
        spotDao.insertOrReplace(spot);


        spotStartDateTime.add(Calendar.HOUR, 1);
        spot = new Spot();
        spot.setCity("Viborg");
        spot.setCountry("Denmark");
        spot.setLatitude(56.426606);
        spot.setLongitude(9.389536);
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setGpsResolved(true);
        spot.setWaitingTime(2);
        spotDao.insertOrReplace(spot);

        spotStartDateTime.add(Calendar.HOUR, 1);
        spot = new Spot();
        spot.setCity("Herning");
        spot.setCountry("Denmark");
        spot.setLatitude(56.153862);
        spot.setLongitude(9.009223);
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setGpsResolved(true);
        spot.setWaitingTime(30);
        spotDao.insertOrReplace(spot);


        spotStartDateTime.add(Calendar.HOUR, 1);
        spot = new Spot();
        spot.setCity("Vejle");
        spot.setCountry("Denmark");
        spot.setLatitude(55.683155);
        spot.setLongitude(9.562681);
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setGpsResolved(true);
        spot.setWaitingTime(15);
        spotDao.insertOrReplace(spot);


        spotStartDateTime.add(Calendar.HOUR, 1);
        spot = new Spot();
        spot.setCity("Fredericia");
        spot.setCountry("Denmark");
        spot.setLatitude(55.556310);
        spot.setLongitude(9.728092);
        spot.setStartDateTime(spotStartDateTime.getTime());
        spot.setIsDestination(false);
        spot.setIsWaitingForARide(true);
        spot.setGpsResolved(true);
        spotDao.insertOrReplace(spot);

    }

    public void insertSampleData(DaoSession daoSession) {
        //To prevent shit happening, don't execute this method unless it's not DEBUG mode
        if (!BuildConfig.DEBUG)
            return;

        Spot spot1 = new Spot();
        spot1.setId(new Long(1));
        spot1.setNote("My first spot");
        spot1.setCity("Belo Horizonte");
        spot1.setState("Minas Gerais");
        spot1.setCountry("Brazil");
        spot1.setLatitude(-19.912998);
        spot1.setLongitude(-43.940933);
        spot1.setHitchability(1);
        spot1.setAttemptResult(1);
        spot1.setStartDateTime(new GregorianCalendar(2011, 11, 11, 8, 00, 00).getTime());

        Spot spot2 = new Spot();
        spot2.setId(new Long(2));
        spot2.setNote("My second spot");
        spot2.setCity("Rio de Janeiro");
        spot2.setState("Rio de Janeiro");
        spot2.setCountry("Brazil");
        spot2.setLatitude(-22.90833);
        spot2.setLongitude(-43.19639);
        spot2.setHitchability(2);
        spot2.setAttemptResult(2);
        spot2.setStartDateTime(new GregorianCalendar(2011, 11, 11, 10, 10, 00).getTime());

        Spot spot3 = new Spot();
        spot3.setId(new Long(3));
        spot3.setNote("My third spot");
        spot3.setCity("Salvador");
        //spot3.setState("São Paulo");
        spot3.setCountry("Brazil");
        spot3.setLatitude(13.69000);
        spot3.setLongitude(-89.19000);
        spot3.setStartDateTime(new GregorianCalendar(2011, 11, 11, 16, 30, 00).getTime());
        spot3.setIsDestination(true);

        Spot spot4 = new Spot();
        spot4.setId(new Long(4));
        spot4.setCity("Odense");
        //spot4.setState("Frankfurt");
        spot4.setCountry("Denmark");
        spot4.setLatitude(55.39583);
        spot4.setLongitude(10.38861);
        spot4.setHitchability(1);
        spot4.setAttemptResult(1);
        spot4.setWaitingTime(50);
        spot4.setStartDateTime(new GregorianCalendar(2012, 11, 11, 10, 00, 00).getTime());


        Spot spot5 = new Spot();
        spot5.setId(new Long(5));
        spot5.setNote("My fifth spot");
        spot5.setCity("Berlin");
        spot5.setState("Berlin Province");
        spot5.setCountry("Germany");
        spot5.setLatitude(52.520645);
        spot5.setLongitude(13.409779);
        spot5.setHitchability(3);
        spot5.setAttemptResult(1);
        spot5.setWaitingTime(15);
        spot5.setStartDateTime(new GregorianCalendar(2012, 11, 11, 12, 00, 00).getTime());


        Spot spot6 = new Spot();
        spot6.setId(new Long(6));
        spot6.setCity("Sofia");
        //spot5.setState("São Paulo");
        spot6.setCountry("Bulgaria");
        spot6.setLatitude(42.698334);
        spot6.setLongitude(23.319941);
        spot6.setIsDestination(true);
        spot6.setStartDateTime(new GregorianCalendar(2012, 11, 11, 14, 10, 00).getTime());


        Spot spot7 = new Spot();
        spot7.setId(new Long(7));
        spot7.setNote("My seventh spot");
        spot7.setCity("Sofia");
        //spot7.setState("Berlin");
        spot7.setCountry("Bulgaria");
        spot7.setLatitude(42.698334);
        spot7.setLongitude(23.319941);
        spot7.setHitchability(3);
        spot7.setAttemptResult(1);
        spot7.setWaitingTime(10);
        GregorianCalendar gcDate = new GregorianCalendar();
        gcDate.add(Calendar.MINUTE, -240);
        spot7.setStartDateTime(gcDate.getTime());

        Spot spot8 = new Spot();
        spot8.setId(new Long(8));
        spot8.setNote("My eith spot");
        spot8.setCity("Istanbul");
        //spot8.setState("Minas Gerais");
        spot8.setCountry("Turkey");
        spot8.setLatitude(41.015137);
        spot8.setLongitude(28.979530);
        spot8.setHitchability(3);
        spot8.setStartDateTime(new Date());
        spot8.setIsWaitingForARide(true);

        SpotDao spotDao = daoSession.getSpotDao();
        spotDao.insertOrReplace(spot1);
        spotDao.insertOrReplace(spot2);
        spotDao.insertOrReplace(spot3);
        spotDao.insertOrReplace(spot4);
        spotDao.insertOrReplace(spot5);
        spotDao.insertOrReplace(spot6);
        spotDao.insertOrReplace(spot7);
        spotDao.insertOrReplace(spot8);
    }


}
