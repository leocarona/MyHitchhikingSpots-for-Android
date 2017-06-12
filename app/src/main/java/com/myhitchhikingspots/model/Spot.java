package com.myhitchhikingspots.model;

import org.greenrobot.greendao.annotation.*;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit.

/**
 * Entity mapped to table "SPOT".
 */
@Entity
public class Spot implements java.io.Serializable {

    @Id
    private Long id;
    private String Name;
    private String Street;
    private String Zip;
    private String City;
    private String State;
    private String Country;
    private Double Longitude;
    private Double Latitude;
    private Boolean GpsResolved;
    private Boolean IsReverseGeocoded;
    private String Note;
    private String Description;
    private java.util.Date StartDateTime;
    private Integer WaitingTime;
    private Integer Hitchability;
    private Integer AttemptResult;
    private Boolean IsWaitingForARide;
    private Boolean IsDestination;
    private String CountryCode;
    private Boolean HasAccuracy;
    private Float Accuracy;
    private Boolean IsPartOfARoute;

    @Generated
    public Spot() {
    }

    public Spot(Long id) {
        this.id = id;
    }

    @Generated
    public Spot(Long id, String Name, String Street, String Zip, String City, String State, String Country, Double Longitude, Double Latitude, Boolean GpsResolved, Boolean IsReverseGeocoded, String Note, String Description, java.util.Date StartDateTime, Integer WaitingTime, Integer Hitchability, Integer AttemptResult, Boolean IsWaitingForARide, Boolean IsDestination, String CountryCode, Boolean HasAccuracy, Float Accuracy, Boolean IsPartOfARoute) {
        this.id = id;
        this.Name = Name;
        this.Street = Street;
        this.Zip = Zip;
        this.City = City;
        this.State = State;
        this.Country = Country;
        this.Longitude = Longitude;
        this.Latitude = Latitude;
        this.GpsResolved = GpsResolved;
        this.IsReverseGeocoded = IsReverseGeocoded;
        this.Note = Note;
        this.Description = Description;
        this.StartDateTime = StartDateTime;
        this.WaitingTime = WaitingTime;
        this.Hitchability = Hitchability;
        this.AttemptResult = AttemptResult;
        this.IsWaitingForARide = IsWaitingForARide;
        this.IsDestination = IsDestination;
        this.CountryCode = CountryCode;
        this.HasAccuracy = HasAccuracy;
        this.Accuracy = Accuracy;
        this.IsPartOfARoute = IsPartOfARoute;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.Name = Name;
    }

    public String getStreet() {
        return Street;
    }

    public void setStreet(String Street) {
        this.Street = Street;
    }

    public String getZip() {
        return Zip;
    }

    public void setZip(String Zip) {
        this.Zip = Zip;
    }

    public String getCity() {
        return City;
    }

    public void setCity(String City) {
        this.City = City;
    }

    public String getState() {
        return State;
    }

    public void setState(String State) {
        this.State = State;
    }

    public String getCountry() {
        return Country;
    }

    public void setCountry(String Country) {
        this.Country = Country;
    }

    public Double getLongitude() {
        return Longitude;
    }

    public void setLongitude(Double Longitude) {
        this.Longitude = Longitude;
    }

    public Double getLatitude() {
        return Latitude;
    }

    public void setLatitude(Double Latitude) {
        this.Latitude = Latitude;
    }

    public Boolean getGpsResolved() {
        return GpsResolved;
    }

    public void setGpsResolved(Boolean GpsResolved) {
        this.GpsResolved = GpsResolved;
    }

    public Boolean getIsReverseGeocoded() {
        return IsReverseGeocoded;
    }

    public void setIsReverseGeocoded(Boolean IsReverseGeocoded) {
        this.IsReverseGeocoded = IsReverseGeocoded;
    }

    public String getNote() {
        return Note;
    }

    public void setNote(String Note) {
        this.Note = Note;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String Description) {
        this.Description = Description;
    }

    public java.util.Date getStartDateTime() {
        return StartDateTime;
    }

    public void setStartDateTime(java.util.Date StartDateTime) {
        this.StartDateTime = StartDateTime;
    }

    public Integer getWaitingTime() {
        return WaitingTime;
    }

    public void setWaitingTime(Integer WaitingTime) {
        this.WaitingTime = WaitingTime;
    }

    public Integer getHitchability() {
        return Hitchability;
    }

    public void setHitchability(Integer Hitchability) {
        this.Hitchability = Hitchability;
    }

    public Integer getAttemptResult() {
        return AttemptResult;
    }

    public void setAttemptResult(Integer AttemptResult) {
        this.AttemptResult = AttemptResult;
    }

    public Boolean getIsWaitingForARide() {
        return IsWaitingForARide;
    }

    public void setIsWaitingForARide(Boolean IsWaitingForARide) {
        this.IsWaitingForARide = IsWaitingForARide;
    }

    public Boolean getIsDestination() {
        return IsDestination;
    }

    public void setIsDestination(Boolean IsDestination) {
        this.IsDestination = IsDestination;
    }

    public String getCountryCode() {
        return CountryCode;
    }

    public void setCountryCode(String CountryCode) {
        this.CountryCode = CountryCode;
    }

    public Boolean getHasAccuracy() {
        return HasAccuracy;
    }

    public void setHasAccuracy(Boolean HasAccuracy) {
        this.HasAccuracy = HasAccuracy;
    }

    public Float getAccuracy() {
        return Accuracy;
    }

    public void setAccuracy(Float Accuracy) {
        this.Accuracy = Accuracy;
    }

    public Boolean getIsPartOfARoute() {
        return IsPartOfARoute;
    }

    public void setIsPartOfARoute(Boolean IsPartOfARoute) {
        this.IsPartOfARoute = IsPartOfARoute;
    }

}
