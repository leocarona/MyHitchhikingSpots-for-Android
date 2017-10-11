package hitchwikiMapsSDK.entities;

public class CountryInfoBasic {
    private String iso;
    private String name;
    private String places;
    private String lat;
    private String lon;

    public CountryInfoBasic(String iso, String name, String places, String lat, String lon) {
        this.iso = iso;
        this.name = name;
        this.places = places;
        this.lat = lat;
        this.lon = lon;
    }

    public String getIso() {
        return iso;
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaces() {
        return places;
    }

    public void setPlaces(String places) {
        this.places = places;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }
}
