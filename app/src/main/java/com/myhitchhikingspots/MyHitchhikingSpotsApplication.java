package com.myhitchhikingspots;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SpotDao;

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

        loadDatabase();
    }

    public void loadDatabase() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, Constants.dbName, null);
        SQLiteDatabase db = helper.getWritableDatabase();
        Integer v1 = db.getVersion();
        //helper.onUpgrade(db, 1, 2);
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();

        //insertSampleData(daoSession);

        LoadCurrentWaitingSpot();
    }

    private void LoadCurrentWaitingSpot() {
        SpotDao spotDao = daoSession.getSpotDao();
        List<Spot> areWaitingForARide = spotDao.queryBuilder().where(SpotDao.Properties.IsWaitingForARide.eq(true))
                .orderDesc(SpotDao.Properties.StartDateTime, SpotDao.Properties.Id).list();

        if (areWaitingForARide.size() > 1)
            Log.i("LoadCurrentWaitingSpot", "More than 1 spot was found with IsWaitingForARide set to true! This should never happen - Please be aware of this.");
        else if (areWaitingForARide.size() == 1)
            currentSpot = areWaitingForARide.get(0);
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }


    public Spot getCurrentSpot() {
        return currentSpot;
    }

    public void setCurrentSpot(Spot currentSpot) {
        this.currentSpot = currentSpot;
    }


    public void insertSampleData(DaoSession daoSession) {
        Spot spot1 = new Spot();
        spot1.setId(new Long(1));
        spot1.setNote("My first spot");
        spot1.setCity("Sofia");
        spot1.setState("Sofia Province");
        spot1.setCountry("Bulgaria");
        spot1.setLatitude(-22.9068);
        spot1.setLongitude(-43.1729);
        spot1.setHitchability(1);
        spot1.setAttemptResult(1);
        spot1.setStartDateTime(new GregorianCalendar(2011, 11, 11, 8, 00, 00).getTime());

        Spot spot2 = new Spot();
        spot2.setId(new Long(2));
        spot2.setNote("My second spot");
        spot2.setCity("Plovdiv");
        spot2.setState("Plovdiv Province");
        spot2.setCountry("Bulgaria");
        spot2.setLatitude(-23.5500);
        spot2.setLongitude(-46.6333);
        spot2.setHitchability(2);
        spot2.setAttemptResult(2);
        spot2.setStartDateTime(new GregorianCalendar(2011, 11, 11, 10, 10, 00).getTime());

        Spot spot3 = new Spot();
        spot3.setId(new Long(3));
        spot3.setNote("My third spot");
        spot3.setCity("Istambul");
        //spot3.setState("São Paulo");
        spot3.setCountry("Turkey");
        spot3.setLatitude(-19.9167);
        spot3.setLongitude(-43.9333);
        spot3.setStartDateTime(new GregorianCalendar(2011, 11, 11, 16, 30, 00).getTime());
        spot3.setIsDestination(true);

        Spot spot4 = new Spot();
        spot4.setId(new Long(4));
        spot4.setCity("Istambul");
        //spot4.setState("São Paulo");
        spot4.setCountry("Turkey");
        spot4.setLatitude(-19.9167);
        spot4.setLongitude(-43.9333);
        spot4.setHitchability(1);
        spot4.setAttemptResult(1);
        spot4.setWaitingTime(50);
        spot4.setStartDateTime(new GregorianCalendar(2012, 11, 11, 10, 00, 00).getTime());


        Spot spot5 = new Spot();
        spot5.setId(new Long(5));
        spot5.setNote("My fifth spot");
        spot5.setLatitude(-19.9167);
        spot5.setLongitude(-43.9333);
        spot5.setHitchability(3);
        spot5.setAttemptResult(1);
        spot5.setWaitingTime(15);
        spot5.setCity("Burgas");
        spot5.setState("Burgas Province");
        spot5.setCountry("Bulgaria");
        spot5.setStartDateTime(new GregorianCalendar(2012, 11, 11, 12, 00, 00).getTime());


        Spot spot6 = new Spot();
        spot6.setId(new Long(6));
        spot6.setCity("Varna");
        //spot5.setState("São Paulo");
        spot6.setCountry("Bulgaria");
        spot6.setLatitude(-19.9167);
        spot6.setLongitude(-43.9333);
        spot6.setIsDestination(true);
        spot6.setStartDateTime(new GregorianCalendar(2012, 11, 11, 14, 10, 00).getTime());


        Spot spot7 = new Spot();
        spot7.setId(new Long(7));
        spot7.setNote("My seventh spot");
        spot7.setLatitude(-19.9167);
        spot7.setLongitude(-43.9333);
        spot7.setHitchability(3);
        spot7.setAttemptResult(1);
        spot7.setWaitingTime(10);
        spot7.setCity("Varna");
        //spot7.setState("Varna");
        spot7.setCountry("Bulgaria");
        GregorianCalendar gcDate = new GregorianCalendar();
        gcDate.add(Calendar.MINUTE, -240);
        spot7.setStartDateTime(gcDate.getTime());

        Spot spot8 = new Spot();
        spot8.setId(new Long(8));
        spot8.setNote("My eith spot");
        spot8.setLatitude(-19.9167);
        spot8.setLongitude(-43.9333);
        spot8.setHitchability(3);
        spot8.setCity("Bucharest");
        //spot8.setState("Minas Gerais");
        spot8.setCountry("Romania");
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
