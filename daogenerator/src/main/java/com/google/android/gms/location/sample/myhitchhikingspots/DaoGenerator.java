package com.myhitchhikingspots;

import org.greenrobot.greendao.generator.Entity;
import org.greenrobot.greendao.generator.Schema;

/**
 * Created by leoboaventura on 03/03/2016.
 */
public class DaoGenerator {
    private static final String PROJECT_DIR = System.getProperty("user.dir").replace("\\", "/");
    public static void main(String args[]) throws Exception {

        Schema schema = new Schema(3, "com.myhitchhikingspots.model");
        /*Entity person = schema.addEntity("Person");
        person.addIdProperty();
        person.addStringProperty("name");
        person.addStringProperty("comment");

        Entity lease = schema.addEntity("Lease");
        lease.addIdProperty();
        lease.addStringProperty("item");
        lease.addStringProperty("comment");
        lease.addLongProperty("leasedate");
        lease.addLongProperty("returndate");

        Property personId = lease.addLongProperty("personId").getProperty();
        lease.addToOne(person, personId);

        ToMany personToLease = person.addToMany(lease, personId);
        personToLease.setName("leases");*/

        Entity spot = schema.addEntity("Spot");
        spot.addIdProperty();
        spot.addStringProperty("Name");
        spot.addStringProperty("Street");
        spot.addStringProperty("Zip");
        spot.addStringProperty("City");
        spot.addStringProperty("State");
        spot.addStringProperty("Country");

        spot.addDoubleProperty("Longitude");
        spot.addDoubleProperty("Latitude");

        spot.addBooleanProperty("GpsResolved");
        spot.addBooleanProperty("IsReverseGeocoded");

        spot.addStringProperty("Note");
        spot.addStringProperty("Description");

        spot.addDateProperty("StartDateTime");
        spot.addIntProperty("WaitingTime");

        spot.addIntProperty("Hitchability");
        spot.addIntProperty("AttemptResult");

        spot.addBooleanProperty("IsWaitingForARide");
        spot.addBooleanProperty("IsDestination");

        //3 fields added on schema of database version 3 on November 06, 2016:
        spot.addStringProperty("CountryCode");
        spot.addBooleanProperty("HasAccuracy");
        spot.addFloatProperty("Accuracy");

        spot.implementsSerializable();

        new org.greenrobot.greendao.generator.DaoGenerator().generateAll(schema, PROJECT_DIR + "/app/src/main/java");
    }
}
