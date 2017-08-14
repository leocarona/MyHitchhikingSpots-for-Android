package hitchwikiMapsSDK.classes;

public class APIConstants {
    public static String ENDPOINT_PREFIX = "http://hitchwiki.org/maps/api/";

    public static String PLACE_INFO = "?place=";
//	Place info
//	/api/?place=[ID]
//	Get all info about place by ID.
    //example:

//	{	"id":"355",
//		"lat":"51.2029594207479",
//		"lon":"4.38469022512436",
//		"elevation":"7",
//		"location":{
//*				"locality":"Antwerp",
//*				"country":{"iso":"BE","name":"Belgium"},
//				"continent":{"code":"EU","name":"Europe"}
//				   },
//		"link":"http:\/\/hitchwiki.org\/maps\/?place=355",
//		"datetime":null,
//*		"rating":"2",
//		"rating_stats":{
//					   "ratings":{
//					   			  "1":{"rating":"1","rating_count":"1"},
//					   			  "2":{"rating":"2","rating_count":"2"},
//					   			  "3":{"rating":"3","rating_count":"1"}
//					   			 },
//					   	"exact_rating":"2.0000",
//*		                "rating_count":"4",
//		                "different_ratings":"3"
//				   	   },
//		"waiting_stats":{
//*				   	"avg":"15",
//				   	"avg_textual":"15min",
//*				   	"count":"1"
//				   		},
//		"description":{
//				   			"en_UK":{
//				   			"datetime":null,
//				   			"fk_user":null,
//*				   			"description":"To get to Gent you can wait where the ramp splits. Plenty of room to pull over and trafic isn't flying.",
//				   			"versions":"1"
//				   				    }
//				   	   },
//*		"comments":[
//		            {"id":"2295","comment":"Just be careful when you walk under the tunnel","datetime":"2012-07-03 11:40:06","user":{"id":"5360","name":"Quentin"}},
//		            {"id":"4212","comment":"The motorway goes through the tunnel, which means it's forbidden and very dangerous to walk there.","datetime":"2013-05-06 17:25:40"}
//		           ],
//		"comments_count":2
//	}

    public static String PLACE_INFO_BASIC_PREFIX = "?place=";
    public static String PLACE_INFO_BASIC_POSTFIX = "&dot";
//	/api/?place=[ID]&dot
//	Get very basic info about place by ID by adding a dot to the query. Just what you might need to draw a point to the map.
//	JSON Example:
//
//	{
//		"id":"355",
//		"lat":"51.2029594207479",
//		"lon":"4.38469022512436",
//		"rating":"0"
//	}

    public static String LIST_PLACES_FROM_AREA = "?bounds=";
//	List places from area
//	/api/?bounds=59.375129767984,64.208716083434,22.544799804826,35.190063476196
//	JSON Example:
//
//	[
//		{
//			"id":"957",
//			"lat":"61.4893999989479",
//			"lon":"23.7795531749725",
//			"rating":"2"
//		},
//		{
//			"id":"1591",
//			"lat":"61.1811048903562",
//			"lon":"23.8870024681091",
//			"rating":"1"
//		},
//		...
//
//	]

    public static String LIST_PLACES_BY_CITY = "?city=";
//	List places by city
//	/api/?city=[CITYNAME IN ENGLISH]
//	See an example from "List places from area"-section.

    public static String LIST_PLACES_BY_COUNTRY = "?country=";
//	List places by country
//	/api/?country=[COUNTRY ISO CODE]
//	Get all places from a country by country ISO-code. See the list of codes.

    public static String LIST_PLACES_BY_CONTINENT = "?continent=";
    public static String CODE_CONTINENT_ASIA = "AS";
    public static String CODE_CONTINENT_AFRICA = "AF";
    public static String CODE_CONTINENT_NORTH_AMERICA = "NA";
    public static String CODE_CONTINENT_SOUTH_AMERICA = "SA";
    public static String CODE_CONTINENT_ANTARTICA = "AN";
    public static String CODE_CONTINENT_EUROPE = "EU";
    public static String CODE_CONTINENT_AUSTRALIA = "OC";

//	List places by continent
//	/api/?continent=[CONTINENT CODE]
//
//	    AS: Asia
//	    AF: Africa
//	    NA: North America
//	    SA: South America
//	    AN: Antarctica
//	    EU: Europe
//	    OC: Australia and Oceania

    public static String LIST_OF_CONTINENTS = "?continents";
//	List of continents
//	/api/?continents
//
//	    Short code
//	        AS: Asia
//	        AF: Africa
//	        NA: North America
//	        SA: South America
//	        AN: Antarctica
//	        EU: Europe
//	        OC: Australia and Oceania
//	    Name
//	    Places count
//
//	JSON Example:
//
//	{
//		"AS":
//		{
//			"name":"Asia",
//			"code":"AS",
//			"places":"89"
//		},
//		"AF":
//		{
//			"name":"Africa",
//			"code":"AF",
//			"places":"19"
//		},
//		...
//	}

    public static String LIST_OF_COUNTRIES = "?countries";
    public static String LIST_OF_COUNTRIES_AND_COORDINATES = "?countries&coordinates";
//	List of countries
//	List only countries with places:
//	/api/?countries
//
//	List all countries, also with 0 places:
//	/api/?countries=all
//
//	Get also country coordinates with coordinates-variable:
//	/api/?countries&coordinates
//
//	    ISO short code
//	    Name
//	    Places count
//	    Latitude (not by default)
//	    Longitude (not by default)
//
//	JSON Example with coordinates:
//
//	{
//		"Germany":
//		{
//			"iso":"DE",
//			"name":"Germany",
//			"places":"662"
//		},
//		"France":
//		{
//			"iso":"FR",
//			"name":"France",
//			"places":"423"
//		},
//		...
//	}

    public static String LIST_OF_LANGUAGES = "?languages";
//	List of languages
//	Get a list of available languages. /api/?languages
//
//	    ISO code
//	    Language name in requested language (default en_UK)
//	    "In Language" in original language
//
//	JSON Example:
//
//	{
//		"en_UK":
//		{
//			"code":"en_UK",
//			"name":"English",
//			"name_original":"In English"
//		},
//		"de_DE":
//		{
//			"code":"de_DE",
//			"name":"German",
//			"name_original":"Auf Deutsch"
//		},
//		...
//	}

    public static String TEST_PING = "?ping";
//	Test API
//	To test if you can connect to the API, just ask /api/?ping
//
//	JSON Example:
//
//	{"ping":"pong"}


//	In case of error
//	If API produces an error, it returns "error":"true" and possible error description.
//
//	JSON Example where calling /api/?place=351 didn't find a place with this ID:
//
//	{
//		"error":"true",
//		"error_description":"Place not found."
//	}


    //getters and setters:

    public static String getPLACE_INFO() {
        return PLACE_INFO;
    }

    public static void setPLACE_INFO(String pLACE_INFO) {
        PLACE_INFO = pLACE_INFO;
    }

    public static String getPLACE_INFO_BASIC_PREFIX() {
        return PLACE_INFO_BASIC_PREFIX;
    }

    public static void setPLACE_INFO_BASIC_PREFIX(String pLACE_INFO_BASIC_PREFIX) {
        PLACE_INFO_BASIC_PREFIX = pLACE_INFO_BASIC_PREFIX;
    }

    public static String getPLACE_INFO_BASIC_POSTFIX() {
        return PLACE_INFO_BASIC_POSTFIX;
    }

    public static void setPLACE_INFO_BASIC_POSTFIX(String pLACE_INFO_BASIC_POSTFIX) {
        PLACE_INFO_BASIC_POSTFIX = pLACE_INFO_BASIC_POSTFIX;
    }

    public static String getLIST_PLACES_FROM_AREA() {
        return LIST_PLACES_FROM_AREA;
    }

    public static void setLIST_PLACES_FROM_AREA(String lIST_PLACES_FROM_AREA) {
        LIST_PLACES_FROM_AREA = lIST_PLACES_FROM_AREA;
    }

    public static String getLIST_PLACES_BY_CITY() {
        return LIST_PLACES_BY_CITY;
    }

    public static void setLIST_PLACES_BY_CITY(String lIST_PLACES_BY_CITY) {
        LIST_PLACES_BY_CITY = lIST_PLACES_BY_CITY;
    }

    public static String getLIST_PLACES_BY_COUNTRY() {
        return LIST_PLACES_BY_COUNTRY;
    }

    public static void setLIST_PLACES_BY_COUNTRY(String lIST_PLACES_BY_COUNTRY) {
        LIST_PLACES_BY_COUNTRY = lIST_PLACES_BY_COUNTRY;
    }

    public static String getLIST_PLACES_BY_CONTINENT() {
        return LIST_PLACES_BY_CONTINENT;
    }

    public static void setLIST_PLACES_BY_CONTINENT(String lIST_PLACES_BY_CONTINENT) {
        LIST_PLACES_BY_CONTINENT = lIST_PLACES_BY_CONTINENT;
    }

    public static String getCODE_CONTINENT_ASIA() {
        return CODE_CONTINENT_ASIA;
    }

    public static void setCODE_CONTINENT_ASIA(String cODE_CONTINENT_ASIA) {
        CODE_CONTINENT_ASIA = cODE_CONTINENT_ASIA;
    }

    public static String getCODE_CONTINENT_AFRICA() {
        return CODE_CONTINENT_AFRICA;
    }

    public static void setCODE_CONTINENT_AFRICA(String cODE_CONTINENT_AFRICA) {
        CODE_CONTINENT_AFRICA = cODE_CONTINENT_AFRICA;
    }

    public static String getCODE_CONTINENT_NORTH_AMERICA() {
        return CODE_CONTINENT_NORTH_AMERICA;
    }

    public static void setCODE_CONTINENT_NORTH_AMERICA(
            String cODE_CONTINENT_NORTH_AMERICA) {
        CODE_CONTINENT_NORTH_AMERICA = cODE_CONTINENT_NORTH_AMERICA;
    }

    public static String getCODE_CONTINENT_SOUTH_AMERICA() {
        return CODE_CONTINENT_SOUTH_AMERICA;
    }

    public static void setCODE_CONTINENT_SOUTH_AMERICA(
            String cODE_CONTINENT_SOUTH_AMERICA) {
        CODE_CONTINENT_SOUTH_AMERICA = cODE_CONTINENT_SOUTH_AMERICA;
    }

    public static String getCODE_CONTINENT_ANTARTICA() {
        return CODE_CONTINENT_ANTARTICA;
    }

    public static void setCODE_CONTINENT_ANTARTICA(String cODE_CONTINENT_ANTARTICA) {
        CODE_CONTINENT_ANTARTICA = cODE_CONTINENT_ANTARTICA;
    }

    public static String getCODE_CONTINENT_EUROPE() {
        return CODE_CONTINENT_EUROPE;
    }

    public static void setCODE_CONTINENT_EUROPE(String cODE_CONTINENT_EUROPE) {
        CODE_CONTINENT_EUROPE = cODE_CONTINENT_EUROPE;
    }

    public static String getCODE_CONTINENT_AUSTRALIA() {
        return CODE_CONTINENT_AUSTRALIA;
    }

    public static void setCODE_CONTINENT_AUSTRALIA(String cODE_CONTINENT_AUSTRALIA) {
        CODE_CONTINENT_AUSTRALIA = cODE_CONTINENT_AUSTRALIA;
    }

    public static String getLIST_OF_CONTINENTS() {
        return LIST_OF_CONTINENTS;
    }

    public static void setLIST_OF_CONTINENTS(String lIST_OF_CONTINENTS) {
        LIST_OF_CONTINENTS = lIST_OF_CONTINENTS;
    }

    public static String getLIST_OF_COUNTRIES() {
        return LIST_OF_COUNTRIES;
    }

    public static void setLIST_OF_COUNTRIES(String lIST_OF_COUNTRIES) {
        LIST_OF_COUNTRIES = lIST_OF_COUNTRIES;
    }

    public static String getLIST_OF_LANGUAGES() {
        return LIST_OF_LANGUAGES;
    }

    public static void setLIST_OF_LANGUAGES(String lIST_OF_LANGUAGES) {
        LIST_OF_LANGUAGES = lIST_OF_LANGUAGES;
    }

    public static String getTEST_PING() {
        return TEST_PING;
    }

    public static void setTEST_PING(String tEST_PING) {
        TEST_PING = tEST_PING;
    }

    public static String getENDPOINT_PREFIX() {
        return ENDPOINT_PREFIX;
    }

    public static void setENDPOINT_PREFIX(String eNDPOINT_PREFIX) {
        ENDPOINT_PREFIX = eNDPOINT_PREFIX;
    }
}
