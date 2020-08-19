package com.myhitchhikingspots.model;

import org.joda.time.DateTime;

public class Usuario {
    /**
     * A random id generated for the user even before they've logged in with their Hitchwiki credentials.
     * This is also the Usuario's key on the databae.
     **/
    public String usuarioId;
    /**
     * Datetime when user has opened the app for the first time.
     **/
    public String registeredSince;
    /**
     * Last time this user has opened the app, regardless if the user is logged in with their Hitchwiki credentials or not.
     **/
    public String lastFbLoginAt;
    /**
     * Last Firebase authentication/login id generated for this user.
     **/
    public String lastFbLoginId;
    /**
     * Datetime of last time when user has logged in with their Hitchwiki credentials.
     **/
    public String hwLoginAt;
    /**
     * Hitchwiki username, if the user has logged in with their Hitchwiki credentials.
     **/
    public String hwUsername;
    /**
     * Last Firebase token for this user.
     **/
    public String lastFBToken;

    public Usuario(String usuarioId) {
        this.usuarioId = usuarioId;
        this.registeredSince = DateTime.now().toString();
    }

    public Usuario() {
    }
}
