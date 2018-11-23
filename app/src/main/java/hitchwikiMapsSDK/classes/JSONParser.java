package hitchwikiMapsSDK.classes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import hitchwikiMapsSDK.entities.CountryInfoBasic;
import hitchwikiMapsSDK.entities.Error;
import hitchwikiMapsSDK.entities.PlaceInfoBasic;
import hitchwikiMapsSDK.entities.PlaceInfoComplete;
import hitchwikiMapsSDK.entities.PlaceInfoCompleteComment;

public class JSONParser {
    public Object parseGetPlaceBasicDetails(JSONObject json) {
        String errMsg = "";
        try {
            if (json.has("error"))
                errMsg = json.get("error_description").toString();
            else {
                JSONObject success = new JSONObject();
                success = json;

                PlaceInfoBasic placeInfoBasic = new PlaceInfoBasic(
                        success.get("id").toString(),
                        success.get("lat").toString(),
                        success.get("lon").toString(),
                        success.get("rating").toString());

                return placeInfoBasic;
            }
        } catch (Exception e) {
            errMsg = String.format("Error parsing parseGetPlaceBasicDetails!\n\"%s\"", e.getMessage());
        }

        if (!errMsg.isEmpty())
            return new Error(true, errMsg);

        return null;
    }

    public Object parseGetPlaceCompleteDetails(JSONObject json) {
        String errMsg = "";
        try {
            if (json.has("error"))
                errMsg = json.get("error_description").toString();
            else {
                JSONObject success = new JSONObject();
                success = json;

                //first, lets take comments because that's the only json array of objects
                int commentsCount = Integer.parseInt(success.get("comments_count").toString());

                PlaceInfoCompleteComment[] allComments = null;

                //if there are comments at all
                if (commentsCount > 0) {
                    allComments = new PlaceInfoCompleteComment[commentsCount];

                    JSONArray commentsArray = success.getJSONArray("comments");

                    for (int i = 0; i < commentsCount; i++) {
                        JSONObject rec = commentsArray.getJSONObject(i);

                        PlaceInfoCompleteComment comment = new PlaceInfoCompleteComment(
                                rec.get("id").toString(),
                                rec.get("comment").toString(),
                                rec.get("datetime").toString(),
                                "",
                                "");
                        if (rec.has("user")) {
                            JSONObject userFromComment = rec.getJSONObject("user");

                            if (userFromComment.has("id")) {
                                comment.setUserId(userFromComment.get("id").toString());
                            }

                            if (userFromComment.has("name")) {
                                comment.setUserName(userFromComment.get("name").toString());
                            }

                            if (userFromComment.has("nick")) {
                                comment.setUserName(userFromComment.get("nick").toString());
                            }
                        }

                        allComments[i] = comment;
                    }
                }

                PlaceInfoComplete placeInfoComplete = new PlaceInfoComplete(
                        success.get("id").toString(),
                        success.get("lat").toString(),
                        success.get("lon").toString(),
                        success.get("elevation").toString(),
                        "",
                        "",
                        "",
                        "",
                        "",
                        success.get("link").toString(),
                        "",
                        success.get("rating").toString(),
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        success.get("comments_count").toString(),
                        allComments
                );

                //locality:
                if (success.has("location")) {
                    JSONObject locationObject = success.getJSONObject("location");

                    placeInfoComplete.setLocality(locationObject.get("locality").toString());

                    if (locationObject.has("country")) {
                        JSONObject countrySubObject = locationObject.getJSONObject("country");
                        placeInfoComplete.setCountry_iso(countrySubObject.get("iso").toString());
                        placeInfoComplete.setCountry_name(countrySubObject.get("name").toString());
                    }

                    if (locationObject.has("continent")) {
                        JSONObject continentSubObject = locationObject.getJSONObject("continent");
                        placeInfoComplete.setContinent_code(continentSubObject.get("code").toString());
                        placeInfoComplete.setContinent_name(continentSubObject.get("name").toString());
                    }
                }

                //rating_count:
                if (success.has("rating_stats")) {
                    JSONObject ratingStatsObject = success.getJSONObject("rating_stats");

                    placeInfoComplete.setRating_count(ratingStatsObject.get("rating_count").toString());
                }

                //waiting_stats:
                if (success.has("waiting_stats")) {
                    JSONObject waitingStatsObject = success.getJSONObject("waiting_stats");

                    placeInfoComplete.setWaiting_stats_avg(waitingStatsObject.get("avg").toString());
                    placeInfoComplete.setWaiting_stats_avg_textual(waitingStatsObject.get("avg_textual").toString());
                    placeInfoComplete.setWaiting_stats_count(waitingStatsObject.get("count").toString());
                }

                //NOTE: In previous versions we were only retrieving English descriptions, all related to description
                // (author, description and datetime) below is a temporary workaround to include all descriptions.
                //description:
                if (success.has("description")) {
                    JSONObject descriptionObject = success.getJSONObject("description");

                    String allDescriptions = "";
                    Iterator<?> i = descriptionObject.keys();
                    do {
                        String k = i.next().toString();
                        JSONObject rec = descriptionObject.getJSONObject(k);

                        String languageTitle = "";
                        if (k.toString().toLowerCase().contains("en_"))
                            languageTitle = "English";
                        else if (k.toString().toLowerCase().contains("pt_"))
                            languageTitle = "Português";
                        else if (k.toString().toLowerCase().contains("es_"))
                            languageTitle = "Español";
                        else if (k.toString().toLowerCase().contains("de_"))
                            languageTitle = "German";
                        else if (k.toString().toLowerCase().contains("da_"))
                            languageTitle = "Deutsch";
                        else
                            languageTitle = k;

                        System.out.println(k);

                        allDescriptions += "\n\n(" + languageTitle + ")\n" + rec.get("description").toString();

                        //Set the author, but if there are more authors this value will be changed after the loop
                        placeInfoComplete.setDescriptionENfk_user(rec.get("fk_user").toString());

                        //Just set any of the dates
                        placeInfoComplete.setDescriptionENdatetime(rec.get("datetime").toString());

                    } while (i.hasNext());

                    //Set all descriptions, not only the English ones
                    placeInfoComplete.setDescriptionENdescription(allDescriptions);

                    //If there were many descriptions, change author to "many"
                    if (descriptionObject.length() > 1)
                        placeInfoComplete.setDescriptionENfk_user("(many)");
                }

                //finally
                return placeInfoComplete;
            }
        } catch (Exception e) {
            errMsg = String.format("Error parsing parseGetPlaceCompleteDetails!\n\"%s\"", e.getMessage());
        }

        if (!errMsg.isEmpty())
            return new Error(true, errMsg);

        return null;
    }

    public Object parseGetPlacesFromArea(String json) {
        String errMsg = "";
        try {
            if (json.startsWith("{") && json.contains("error")) {
                JSONObject success = new JSONObject(json);
                errMsg = success.get("error_description").toString();
            } else {
                JSONArray successArray = new JSONArray(json);

                PlaceInfoBasic[] placesFromAreaArray = null;

                if (successArray.length() > 0) {
                    placesFromAreaArray = new PlaceInfoBasic[successArray.length()];

                    for (int i = 0; i < successArray.length(); i++) {
                        JSONObject rec = successArray.getJSONObject(i);

                        placesFromAreaArray[i] = new PlaceInfoBasic(rec.get("id").toString(),
                                rec.get("lat").toString(),
                                rec.get("lon").toString(),
                                rec.get("rating").toString());
                    }
                    return placesFromAreaArray;
                } else {
                    return placesFromAreaArray;
                }
            }
        } catch (Exception e) {
            errMsg = String.format("Error parsing parseGetPlacesFromArea!\n\"%s\"", e.getMessage());
            ;
        }

        if (!errMsg.isEmpty())
            return new Error(true, errMsg);

        return null;
    }

    public Object parseGetPlacesByCountry(String json) {
        String errMsg = "";
        try {
            if (json.startsWith("{") && json.contains("error")) {
                JSONObject success = new JSONObject(json);
                errMsg = success.get("error_description").toString();
            } else {
                JSONArray successArray = new JSONArray(json);

                PlaceInfoBasic[] placesFromAreaArray = null;

                if (successArray.length() > 0) {
                    placesFromAreaArray = new PlaceInfoBasic[successArray.length()];

                    for (int i = 0; i < successArray.length(); i++) {
                        JSONObject rec = successArray.getJSONObject(i);

                        placesFromAreaArray[i] = new PlaceInfoBasic(rec.get("id").toString(),
                                rec.get("lat").toString(),
                                rec.get("lon").toString(),
                                rec.get("rating").toString());
                    }
                    return placesFromAreaArray;
                } else {
                    return placesFromAreaArray;
                }
            }
        } catch (Exception e) {
            errMsg = String.format("Error parsing parseGetPlacesByCountry\n\"%s\"", e.getMessage());
            ;
        }

        if (!errMsg.isEmpty())
            return new Error(true, errMsg);

        return null;
    }

    public Object parseGetCountriesWithCoordinates(String json) {
        //TODO: It looks like there's an obvious bug on this method - it should be fixed later
        String errMsg = "";
        try {
            String jsonCorrected = json.replaceFirst("\\{", "[");
            jsonCorrected = jsonCorrected.substring(0, jsonCorrected.length() - 2);

            String arr[] = jsonCorrected.split("\\},\"");

            for (int i = 0; i < arr.length; i++) {
                arr[i] = arr[i].concat("}, ");
                arr[i] = arr[i].substring(arr[i].indexOf("{"));
            }

            arr[arr.length - 1] = arr[arr.length - 1].substring(0, arr[arr.length - 1].length() - 2);

            jsonCorrected = "[";

            for (int i = 0; i < arr.length; i++) {
                jsonCorrected = jsonCorrected.concat(arr[i]);
            }

            jsonCorrected = jsonCorrected.concat("]");

            if (jsonCorrected.startsWith("{"))
            //this gives Error object as "success" String value is false
            {
                JSONObject success = new JSONObject(json);
                errMsg = success.get("error_description").toString();
            } else {
                JSONArray successArray = new JSONArray(jsonCorrected);

                CountryInfoBasic[] countriesWithCoordinatesArray = null;

                if (successArray.length() > 0) {
                    countriesWithCoordinatesArray = new CountryInfoBasic[successArray.length()];

                    for (int i = 0; i < successArray.length(); i++) {
                        JSONObject rec = successArray.getJSONObject(i);

                        countriesWithCoordinatesArray[i] = new CountryInfoBasic
                                (
                                        rec.get("iso").toString(),
                                        rec.get("name").toString(),
                                        rec.get("places").toString(),
                                        rec.get("lat").toString(),
                                        rec.get("lon").toString()
                                );
                    }
                    return countriesWithCoordinatesArray;
                } else {
                    return countriesWithCoordinatesArray;
                }
            }
        } catch (Exception e) {
            errMsg = String.format("Error parsing parseGetCountriesWithCoordinates\n\"%s\"", e.getMessage());
            ;
        }

        if (!errMsg.isEmpty())
            return new Error(true, errMsg);

        return null;
    }
}