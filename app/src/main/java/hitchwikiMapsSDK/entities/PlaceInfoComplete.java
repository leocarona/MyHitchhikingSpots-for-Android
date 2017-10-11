package hitchwikiMapsSDK.entities;

public class PlaceInfoComplete {
    private String id;
    private String lat;
    private String lon;
    private String elevation;
    private String locality;
    private String country_iso;
    private String country_name;
    private String continent_code;
    private String continent_name;
    private String link;
    private String datetime;
    private String rating;
    private String ratings1amount;
    private String ratings2amount;
    private String ratings3amount;
    private String ratings4amount;
    private String ratings5amount;
    private String exact_rating;
    private String rating_count;
    private String different_ratings;
    private String waiting_stats_avg;
    private String waiting_stats_avg_textual;
    private String waiting_stats_count;
    private String descriptionENdatetime;
    private String descriptionENfk_user;
    private String descriptionENdescription;
    private String descriptionENversions;
    private String comments_count;
    private PlaceInfoCompleteComment[] comments;

    //default constructor:

    /**
     * @param id
     * @param lat
     * @param lon
     * @param elevation
     * @param locality
     * @param country_iso
     * @param country_name
     * @param continent_code
     * @param continent_name
     * @param link
     * @param datetime
     * @param rating
     * @param ratings1amount
     * @param ratings2amount
     * @param ratings3amount
     * @param ratings4amount
     * @param ratings5amount
     * @param exact_rating
     * @param rating_count
     * @param different_ratings
     * @param waiting_stats_avg
     * @param waiting_stats_avg_textual
     * @param waiting_stats_count
     * @param descriptionENdatetime
     * @param descriptionENfk_user
     * @param descriptionENdescription
     * @param descriptionENversions
     * @param comments_count
     * @param comments
     */
    public PlaceInfoComplete(String id, String lat, String lon,
                             String elevation, String locality, String country_iso,
                             String country_name, String continent_code, String continent_name,
                             String link, String datetime, String rating, String ratings1amount,
                             String ratings2amount, String ratings3amount,
                             String ratings4amount, String ratings5amount, String exact_rating,
                             String rating_count, String different_ratings,
                             String waiting_stats_avg, String waiting_stats_avg_textual,
                             String waiting_stats_count, String descriptionENdatetime,
                             String descriptionENfk_user, String descriptionENdescription,
                             String descriptionENversions, String comments_count,
                             PlaceInfoCompleteComment[] comments) {
        super();
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.elevation = elevation;
        this.locality = locality;
        this.country_iso = country_iso;
        this.country_name = country_name;
        this.continent_code = continent_code;
        this.continent_name = continent_name;
        this.link = link;
        this.datetime = datetime;
        this.rating = rating;
        this.ratings1amount = ratings1amount;
        this.ratings2amount = ratings2amount;
        this.ratings3amount = ratings3amount;
        this.ratings4amount = ratings4amount;
        this.ratings5amount = ratings5amount;
        this.exact_rating = exact_rating;
        this.rating_count = rating_count;
        this.different_ratings = different_ratings;
        this.waiting_stats_avg = waiting_stats_avg;
        this.waiting_stats_avg_textual = waiting_stats_avg_textual;
        this.waiting_stats_count = waiting_stats_count;
        this.descriptionENdatetime = descriptionENdatetime;
        this.descriptionENfk_user = descriptionENfk_user;
        this.descriptionENdescription = descriptionENdescription;
        this.descriptionENversions = descriptionENversions;
        this.comments_count = comments_count;
        this.comments = comments;
    }

    //getters and setters:

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the lat
     */
    public String getLat() {
        return lat;
    }

    /**
     * @param lat the lat to set
     */
    public void setLat(String lat) {
        this.lat = lat;
    }

    /**
     * @return the lon
     */
    public String getLon() {
        return lon;
    }

    /**
     * @param lon the lon to set
     */
    public void setLon(String lon) {
        this.lon = lon;
    }

    /**
     * @return the elevation
     */
    public String getElevation() {
        return elevation;
    }

    /**
     * @param elevation the elevation to set
     */
    public void setElevation(String elevation) {
        this.elevation = elevation;
    }

    /**
     * @return the locality
     */
    public String getLocality() {
        return locality;
    }

    /**
     * @param locality the locality to set
     */
    public void setLocality(String locality) {
        this.locality = locality;
    }

    /**
     * @return the country_iso
     */
    public String getCountry_iso() {
        return country_iso;
    }

    /**
     * @param country_iso the country_iso to set
     */
    public void setCountry_iso(String country_iso) {
        this.country_iso = country_iso;
    }

    /**
     * @return the country_name
     */
    public String getCountry_name() {
        return country_name;
    }

    /**
     * @param country_name the country_name to set
     */
    public void setCountry_name(String country_name) {
        this.country_name = country_name;
    }

    /**
     * @return the continent_code
     */
    public String getContinent_code() {
        return continent_code;
    }

    /**
     * @param continent_code the continent_code to set
     */
    public void setContinent_code(String continent_code) {
        this.continent_code = continent_code;
    }

    /**
     * @return the continent_name
     */
    public String getContinent_name() {
        return continent_name;
    }

    /**
     * @param continent_name the continent_name to set
     */
    public void setContinent_name(String continent_name) {
        this.continent_name = continent_name;
    }

    /**
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * @return the datetime
     */
    public String getDatetime() {
        return datetime;
    }

    /**
     * @param datetime the datetime to set
     */
    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    /**
     * @return the rating
     */
    public String getRating() {
        return rating;
    }

    /**
     * @param rating the rating to set
     */
    public void setRating(String rating) {
        this.rating = rating;
    }

    /**
     * @return the ratings1amount
     */
    public String getRatings1amount() {
        return ratings1amount;
    }

    /**
     * @param ratings1amount the ratings1amount to set
     */
    public void setRatings1amount(String ratings1amount) {
        this.ratings1amount = ratings1amount;
    }

    /**
     * @return the ratings2amount
     */
    public String getRatings2amount() {
        return ratings2amount;
    }

    /**
     * @param ratings2amount the ratings2amount to set
     */
    public void setRatings2amount(String ratings2amount) {
        this.ratings2amount = ratings2amount;
    }

    /**
     * @return the ratings3amount
     */
    public String getRatings3amount() {
        return ratings3amount;
    }

    /**
     * @param ratings3amount the ratings3amount to set
     */
    public void setRatings3amount(String ratings3amount) {
        this.ratings3amount = ratings3amount;
    }

    /**
     * @return the ratings4amount
     */
    public String getRatings4amount() {
        return ratings4amount;
    }

    /**
     * @param ratings4amount the ratings4amount to set
     */
    public void setRatings4amount(String ratings4amount) {
        this.ratings4amount = ratings4amount;
    }

    /**
     * @return the ratings5amount
     */
    public String getRatings5amount() {
        return ratings5amount;
    }

    /**
     * @param ratings5amount the ratings5amount to set
     */
    public void setRatings5amount(String ratings5amount) {
        this.ratings5amount = ratings5amount;
    }

    /**
     * @return the exact_rating
     */
    public String getExact_rating() {
        return exact_rating;
    }

    /**
     * @param exact_rating the exact_rating to set
     */
    public void setExact_rating(String exact_rating) {
        this.exact_rating = exact_rating;
    }

    /**
     * @return the rating_count
     */
    public String getRating_count() {
        return rating_count;
    }

    /**
     * @param rating_count the rating_count to set
     */
    public void setRating_count(String rating_count) {
        this.rating_count = rating_count;
    }

    /**
     * @return the different_ratings
     */
    public String getDifferent_ratings() {
        return different_ratings;
    }

    /**
     * @param different_ratings the different_ratings to set
     */
    public void setDifferent_ratings(String different_ratings) {
        this.different_ratings = different_ratings;
    }

    /**
     * @return the waiting_stats_avg
     */
    public String getWaiting_stats_avg() {
        return waiting_stats_avg;
    }

    /**
     * @param waiting_stats_avg the waiting_stats_avg to set
     */
    public void setWaiting_stats_avg(String waiting_stats_avg) {
        this.waiting_stats_avg = waiting_stats_avg;
    }

    /**
     * @return the waiting_stats_avg_textual
     */
    public String getWaiting_stats_avg_textual() {
        return waiting_stats_avg_textual;
    }

    /**
     * @param waiting_stats_avg_textual the waiting_stats_avg_textual to set
     */
    public void setWaiting_stats_avg_textual(String waiting_stats_avg_textual) {
        this.waiting_stats_avg_textual = waiting_stats_avg_textual;
    }

    /**
     * @return the waiting_stats_count
     */
    public String getWaiting_stats_count() {
        return waiting_stats_count;
    }

    /**
     * @param waiting_stats_count the waiting_stats_count to set
     */
    public void setWaiting_stats_count(String waiting_stats_count) {
        this.waiting_stats_count = waiting_stats_count;
    }

    /**
     * @return the descriptionENdatetime
     */
    public String getDescriptionENdatetime() {
        return descriptionENdatetime;
    }

    /**
     * @param descriptionENdatetime the descriptionENdatetime to set
     */
    public void setDescriptionENdatetime(String descriptionENdatetime) {
        this.descriptionENdatetime = descriptionENdatetime;
    }

    /**
     * @return the descriptionENfk_user
     */
    public String getDescriptionENfk_user() {
        return descriptionENfk_user;
    }

    /**
     * @param descriptionENfk_user the descriptionENfk_user to set
     */
    public void setDescriptionENfk_user(String descriptionENfk_user) {
        this.descriptionENfk_user = descriptionENfk_user;
    }

    /**
     * @return the descriptionENdescription
     */
    public String getDescriptionENdescription() {
        return descriptionENdescription;
    }

    /**
     * @param descriptionENdescription the descriptionENdescription to set
     */
    public void setDescriptionENdescription(String descriptionENdescription) {
        this.descriptionENdescription = descriptionENdescription;
    }

    /**
     * @return the descriptionENversions
     */
    public String getDescriptionENversions() {
        return descriptionENversions;
    }

    /**
     * @param descriptionENversions the descriptionENversions to set
     */
    public void setDescriptionENversions(String descriptionENversions) {
        this.descriptionENversions = descriptionENversions;
    }

    /**
     * @return the comments_count
     */
    public String getComments_count() {
        return comments_count;
    }

    /**
     * @param comments_count the comments_count to set
     */
    public void setComments_count(String comments_count) {
        this.comments_count = comments_count;
    }

    /**
     * @return the comments
     */
    public PlaceInfoCompleteComment[] getComments() {
        return comments;
    }

    /**
     * @param comments the comments to set
     */
    public void setComments(PlaceInfoCompleteComment[] comments) {
        this.comments = comments;
    }
}
