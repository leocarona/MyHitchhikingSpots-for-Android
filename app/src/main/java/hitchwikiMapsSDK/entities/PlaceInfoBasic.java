package hitchwikiMapsSDK.entities;

public class PlaceInfoBasic 
{ 
	private String id;
	private String lat;
	private String lon;
	private String rating;
	
	public PlaceInfoBasic(String id, String lat, String lon, String rating) 
	{
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.rating = rating;
	}
	
//	{
//	"id":"355",
//	"lat":"51.2029594207479",
//	"lon":"4.38469022512436",
//	"rating":"0"
//	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}
}
