package hitchwikiMapsSDK.classes;

import com.crashlytics.android.Crashlytics;

import org.json.JSONObject;

import hitchwikiMapsSDK.entities.CountryInfoBasic;
import hitchwikiMapsSDK.entities.PlaceInfoBasic;
import hitchwikiMapsSDK.entities.Error;
import hitchwikiMapsSDK.entities.PlaceInfoComplete;

public class ApiManager {
    private ServerRequest mServerRequest;

    public ApiManager() {
        //we use ServerRequest for post and get methods
        mServerRequest = new ServerRequest();
    }

    public void getPlaceBasicDetails(final int id, final APICallCompletionListener<PlaceInfoBasic> callback) {
        JSONObject response = mServerRequest.postRequest
                (
                        APIConstants.ENDPOINT_PREFIX +
                                APIConstants.PLACE_INFO_BASIC_PREFIX +
                                String.valueOf(id) +
                                APIConstants.PLACE_INFO_BASIC_POSTFIX
                );

        JSONParser parser = new JSONParser();

        Object resultObject = parser.parseGetPlaceBasicDetails(response);

        if (resultObject == null || resultObject.getClass().isAssignableFrom(Error.class)) {
            String errMsg = "Unable to download data. Please try again later and if the problem persists, let us know!";

            if (resultObject != null && resultObject.getClass().isAssignableFrom(Error.class))
                errMsg = ((Error) resultObject).getErrorDescription();

            //NOTE: The right thing to do here would be to call onComplete, but
            // all the logic in the apps (Hitchwiki Maps app and MyHitchhikingSpots app) that call the old version of this method
            // were developed considering that an exception would be thrown if an error
            // would happen in parseGetPlaceBasicDetails, so we better throw an exception here.
            //-----callback.onComplete(false, -1, "", (Error) resultObject, null);
            throw new RuntimeException(errMsg);
        } else
            callback.onComplete(true, -1, "", null, (PlaceInfoBasic) resultObject);
    }

    public void getPlaceCompleteDetails(final int id, final APICallCompletionListener<PlaceInfoComplete> callback) {
        JSONObject response = mServerRequest.postRequest
                (
                        APIConstants.ENDPOINT_PREFIX +
                                APIConstants.PLACE_INFO_BASIC_PREFIX +
                                String.valueOf(id)
                );

        JSONParser parser = new JSONParser();

        Object resultObject = parser.parseGetPlaceCompleteDetails(response);

        if (resultObject == null || resultObject.getClass().isAssignableFrom(Error.class)) {
            String errMsg = "Unable to download further information about the chosen spot. Please try again later and if the problem persists, let us know!";

            if (resultObject != null && resultObject.getClass().isAssignableFrom(Error.class))
                errMsg = ((Error) resultObject).getErrorDescription();

            //NOTE: The right thing to do here would be to call onComplete, but
            // all the logic in the apps (Hitchwiki Maps app and MyHitchhikingSpots app) that call the old version of this method
            // were developed considering that an exception would be thrown if an error
            // would happen in parseGetPlaceCompleteDetails, so we better throw an exception here.
            //-----callback.onComplete(false, -1, "", (Error) resultObject, null);
            throw new RuntimeException(errMsg);
        } else
            callback.onComplete(true, -1, "", null, (PlaceInfoComplete) resultObject);
    }

    public void getPlacesFromArea(final float latFrom,
                                  final float latTo,
                                  final float lonFrom,
                                  final float lonTo,
                                  final APICallCompletionListener<PlaceInfoBasic[]> callback) {
        String response = mServerRequest.postRequestString
                (
                        APIConstants.ENDPOINT_PREFIX +
                                APIConstants.LIST_PLACES_FROM_AREA +
                                String.valueOf(latFrom) +
                                "," +
                                String.valueOf(latTo) +
                                "," +
                                String.valueOf(lonFrom) +
                                "," +
                                String.valueOf(lonTo)
                );

        JSONParser parser = new JSONParser();

        Object resultObject = parser.parseGetPlacesFromArea(response);

        if (resultObject == null || resultObject.getClass().isAssignableFrom(Error.class)) {
            String errMsg = "Unable to download by area. Please try again later and if the problem persists, let us know!";

            if (resultObject != null && resultObject.getClass().isAssignableFrom(Error.class))
                errMsg = ((Error) resultObject).getErrorDescription();

            //NOTE: The right thing to do here would be to call onComplete, but
            // all the logic in the apps (Hitchwiki Maps app and MyHitchhikingSpots app) that call the old version of this method
            // were developed considering that an exception would be thrown if an error
            // would happen in parseGetPlacesFromArea, so we better throw an exception here.
            //-----callback.onComplete(false, -1, "", (Error) resultObject, null);
            throw new RuntimeException(errMsg);
        } else
            callback.onComplete(true, -1, "", null, (PlaceInfoBasic[]) resultObject);
    }

    public void getPlacesByCountry
            (
                    final String countryIsoCode,
                    final APICallCompletionListener<PlaceInfoBasic[]> callback
            ) {
        String response = mServerRequest.postRequestString
                (
                        APIConstants.ENDPOINT_PREFIX
                                + APIConstants.LIST_PLACES_BY_COUNTRY
                                + countryIsoCode
                );

        JSONParser parser = new JSONParser();

        Object resultObject = parser.parseGetPlacesByCountry(response);

        if (resultObject == null || resultObject.getClass().isAssignableFrom(Error.class)) {
            String errMsg = "Unable to download by country. Please try again later and if the problem persists, let us know!";

            if (resultObject != null && resultObject.getClass().isAssignableFrom(Error.class))
                errMsg = ((Error) resultObject).getErrorDescription();

            //NOTE: The right thing to do here would be to call onComplete, but
            // all the logic in the apps (Hitchwiki Maps app and MyHitchhikingSpots app) that call the old version of this method
            // were developed considering that an exception would be thrown if an error
            // would happen in parseGetPlacesByCountry, so we better throw an exception here.
            //-----callback.onComplete(false, -1, "", (Error) resultObject, null);
            throw new RuntimeException(errMsg);
        } else
            callback.onComplete(true, -1, "", null, (PlaceInfoBasic[]) resultObject);
    }

    public void getPlacesByContinent
            (
                    final String continentCode,
                    final APICallCompletionListener<PlaceInfoBasic[]> callback
            ) {
        String response = mServerRequest.postRequestString
                (
                        APIConstants.ENDPOINT_PREFIX
                                + APIConstants.LIST_PLACES_BY_CONTINENT
                                + continentCode
                );

        JSONParser parser = new JSONParser();

        Object resultObject = parser.parseGetPlacesByCountry(response);

        if (resultObject == null || resultObject.getClass().isAssignableFrom(Error.class)) {
            String errMsg = "Unable to download by continent. Please try again later and if the problem persists, let us know!";

            if (resultObject != null && resultObject.getClass().isAssignableFrom(Error.class))
                errMsg = ((Error) resultObject).getErrorDescription();

            //NOTE: The right thing to do here would be to call onComplete, but
            // all the logic in the apps (Hitchwiki Maps app and MyHitchhikingSpots app) that call the old version of this method
            // were developed considering that an exception would be thrown if an error
            // would happen in parseGetPlacesByCountry, so we better throw an exception here.
            //-----callback.onComplete(false, -1, "", (Error) resultObject, null);
            throw new RuntimeException(errMsg);
        } else
            callback.onComplete(true, -1, continentCode, null, (PlaceInfoBasic[]) resultObject);

    }

    public PlaceInfoBasic[] getPlacesByContinenFromLocalFile(String responseFromLocalFile) {
        JSONParser parser = new JSONParser();

        Object resultObject = parser.parseGetPlacesByCountry(responseFromLocalFile);

        return (PlaceInfoBasic[]) resultObject;
    }

    public void getCountriesWithCoordinatesAndMarkersNumber
            (
                    final APICallCompletionListener<CountryInfoBasic[]> callback
            ) {
        String response = mServerRequest.postRequestString
                (
                        APIConstants.ENDPOINT_PREFIX
                                + APIConstants.LIST_OF_COUNTRIES_AND_COORDINATES
                );

        JSONParser parser = new JSONParser();

        Object resultObject = parser.parseGetCountriesWithCoordinates(response);

        if (resultObject == null || resultObject.getClass().isAssignableFrom(Error.class)) {
            String errMsg = "Unable to download countries list. Please try again later and if the problem persists, let us know!";
            ;

            if (resultObject != null && resultObject.getClass().isAssignableFrom(Error.class))
                errMsg = ((Error) resultObject).getErrorDescription();

            //NOTE: The right thing to do here would be to call onComplete, but
            // all the logic in the apps (Hitchwiki Maps app and MyHitchhikingSpots app) that call the old version of this method
            // were developed considering that an exception would be thrown if an error
            // would happen in parseGetCountriesWithCoordinates, so we better throw an exception here.
            //-----callback.onComplete(false, -1, "", (Error) resultObject, null);
            throw new RuntimeException(errMsg);
        } else
            callback.onComplete(true, -1, "", null, (CountryInfoBasic[]) resultObject);
    }


}
