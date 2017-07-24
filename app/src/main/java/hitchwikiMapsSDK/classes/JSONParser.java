package hitchwikiMapsSDK.classes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import hitchwikiMapsSDK.entities.CountryInfoBasic;
import hitchwikiMapsSDK.entities.Error;
import hitchwikiMapsSDK.entities.PlaceInfoBasic;
import hitchwikiMapsSDK.entities.PlaceInfoComplete;
import hitchwikiMapsSDK.entities.PlaceInfoCompleteComment;

public class JSONParser 
{
	public Object parseGetPlaceBasicDetails(JSONObject json)
	{
		try
	   	 {    	
	    	JSONObject success = new JSONObject();
	    	success = json;
	    	     	
	      	if(success.has("error"))
	      	//this gives Error object as "success" String value is false
	      	{
				Error errorObject = new Error(
						true,
						success.get("error_description").toString()
	      									 );
	      		return errorObject;
	      	}
	      	else
	      	{
	      		PlaceInfoBasic placeInfoBasic = new PlaceInfoBasic(
	      				success.get("id").toString(),
	      				success.get("lat").toString(),
	      				success.get("lon").toString(),
	      				success.get("rating").toString());
	      			      			      		
	      		return placeInfoBasic;
	      	}
   	 	}
   	 	catch(JSONException e)        
	   	 	{
	        	 System.out.println("Error parsing parseGetPlaceBasicDetails!!!");
	        }
		
		return null;
	}
	
	public Object parseGetPlaceCompleteDetails(JSONObject json)
	{
		try
	   	 {    	
	    	JSONObject success = new JSONObject();
	    	success = json;
	    	     	
	      	if(success.has("error"))
	      	//this gives Error object as "success" String value is false
	      	{
				Error errorObject = new Error(
						true,
						success.get("error_description").toString()
	      									 );
	      		return errorObject;
	      	}
	      	else
	      	{
	      		//first, lets take comments because that's the only json array of objects
	      		int commentsCount = Integer.parseInt(success.get("comments_count").toString());
	      		
	      		PlaceInfoCompleteComment[] allComments = null;
	      		
	      		//if there are comments at all
	      		if(commentsCount > 0)
	      		{
	      			allComments = new PlaceInfoCompleteComment[commentsCount];
	      			
	      			JSONArray commentsArray = success.getJSONArray("comments");
	      			
	      			for(int i = 0; i < commentsCount; i++)
		      		{
	      				JSONObject rec = commentsArray.getJSONObject(i);
	      				
		      			PlaceInfoCompleteComment comment = new PlaceInfoCompleteComment(
		      																rec.get("id").toString(),
		      																rec.get("comment").toString(),
		      																rec.get("datetime").toString(),
		      																"",
		      																"");
		      			if(rec.has("user"))
	      				{
		      				JSONObject userFromComment = rec.getJSONObject("user");
		      				
		      				if(userFromComment.has("id"))
		      				{
		      					comment.setUserId(userFromComment.get("id").toString());
		      				}
		      				
		      				if(userFromComment.has("name"))
		      				{
		      					comment.setUserName(userFromComment.get("name").toString());
		      				}
		      				
		      				if(userFromComment.has("nick"))
		      				{
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
	      		if(success.has("location"))
	      		{
	      			JSONObject locationObject = success.getJSONObject("location");
	      			
	      			placeInfoComplete.setLocality(locationObject.get("locality").toString());
	      			
	      			if(locationObject.has("country"))
	      			{
	      				JSONObject countrySubObject = locationObject.getJSONObject("country");
	      				placeInfoComplete.setCountry_iso(countrySubObject.get("iso").toString());
	      				placeInfoComplete.setCountry_name(countrySubObject.get("name").toString());
	      			}
	      			
	      			if(locationObject.has("continent"))
	      			{
	      				JSONObject continentSubObject = locationObject.getJSONObject("continent");
	      				placeInfoComplete.setContinent_code(continentSubObject.get("code").toString());
	      				placeInfoComplete.setContinent_name(continentSubObject.get("name").toString());
	      			}
	      		}
	      		
	      	//rating_count:
	      		if(success.has("rating_stats"))
	      		{
	      			JSONObject ratingStatsObject = success.getJSONObject("rating_stats");
	      			
	      			placeInfoComplete.setRating_count(ratingStatsObject.get("rating_count").toString());
	      		}
	      		
	      	//waiting_stats:
	      		if(success.has("waiting_stats"))
	      		{
	      			JSONObject waitingStatsObject = success.getJSONObject("waiting_stats");
	      			
	      			placeInfoComplete.setWaiting_stats_avg(waitingStatsObject.get("avg").toString());
	      			placeInfoComplete.setWaiting_stats_avg_textual(waitingStatsObject.get("avg_textual").toString());
	      			placeInfoComplete.setWaiting_stats_count(waitingStatsObject.get("count").toString());
	      		}
	      		
	      	//description:
	      		if(success.has("description"))
	      		{
	      			JSONObject descriptionObject = success.getJSONObject("description");
	      			
	      			if(descriptionObject.has("en_UK"))
	      			{
	      				JSONObject ENObject = descriptionObject.getJSONObject("en_UK");
	      				
	      				placeInfoComplete.setDescriptionENdatetime(ENObject.get("datetime").toString());
		      			placeInfoComplete.setDescriptionENfk_user(ENObject.get("fk_user").toString());
		      			placeInfoComplete.setDescriptionENdescription(ENObject.get("description").toString());
	      			}
	      		}
	      		
	      	//finally     			      		
	      		return placeInfoComplete;
	      	}
  	 	}
  	 	catch(JSONException e)        
	   	 	{
	        	 System.out.println("Error parsing parseGetPlaceCompleteDetails!!!");
	        }
		
		return null;
	}	
	
	public Object parseGetPlacesFromArea(String json)
	{
		try
	   	 {    	
	      	if(json.startsWith("{"))
	      	//this gives Error object as "success" String value is false
	      	{
	      		JSONObject success = new JSONObject(json);
	      		
				Error errorObject = new Error(
						true,
						success.get("error_description").toString()
	      									 );
	      		return errorObject;
	      	}
	      	else
	      	{
	      		JSONArray successArray = new JSONArray(json);
	      		
	      		PlaceInfoBasic[] placesFromAreaArray = null;
	      		
	      		if(successArray.length() > 0)
	      		{
	      			placesFromAreaArray = new PlaceInfoBasic[successArray.length()];
	      			
	      			for(int i = 0; i < successArray.length(); i++)
		      		{
	      				JSONObject rec = successArray.getJSONObject(i);
	      				
	      				placesFromAreaArray[i] = new PlaceInfoBasic(rec.get("id").toString(),
	      															rec.get("lat").toString(),
	      															rec.get("lon").toString(),
	      															rec.get("rating").toString());
		      		}
	      			return placesFromAreaArray;
	      		}
	      		else
	      		{
	      			return placesFromAreaArray;
	      		}
	      	}
   	 	}
   	 	catch(JSONException e)        
	   	 	{
	        	 System.out.println("Error parsing parseGetPlacesFromArea!!!");
	        }
		
		return null;
	}
	
	public Object parseGetPlacesByCountry(String json)
	{
		try
	   	 {    	
	      	if(json.startsWith("{"))
	      	//this gives Error object as "success" String value is false
	      	{
	      		JSONObject success = new JSONObject(json);
	      		
				Error errorObject = new Error(
						true,
						success.get("error_description").toString()
	      									 );
	      		return errorObject;
	      	}
	      	else
	      	{
	      		JSONArray successArray = new JSONArray(json);
	      		
	      		PlaceInfoBasic[] placesFromAreaArray = null;
	      		
	      		if(successArray.length() > 0)
	      		{
	      			placesFromAreaArray = new PlaceInfoBasic[successArray.length()];
	      			
	      			for(int i = 0; i < successArray.length(); i++)
		      		{
	      				JSONObject rec = successArray.getJSONObject(i);
	      				
	      				placesFromAreaArray[i] = new PlaceInfoBasic(rec.get("id").toString(),
	      															rec.get("lat").toString(),
	      															rec.get("lon").toString(),
	      															rec.get("rating").toString());
		      		}
	      			return placesFromAreaArray;
	      		}
	      		else
	      		{
	      			return placesFromAreaArray;
	      		}
	      	}
   	 	}
   	 	catch(JSONException e)        
	   	 	{
	        	 System.out.println("Error parsing parseGetPlacesByCountry!!!");
	        }
		
		return null;
	}
	
	public Object parseGetCountriesWithCoordinates(String json)
	{
		try
	   	 {    	
			String jsonCorrected = json.replaceFirst("\\{", "[");
			jsonCorrected = jsonCorrected.substring(0, jsonCorrected.length()-2);
			
			String arr[] = jsonCorrected.split("\\},\"");
			
			for (int i = 0; i < arr.length; i++)
			{
				arr[i] = arr[i].concat("}, ");
				arr[i] = arr[i].substring(arr[i].indexOf("{"));
			}
			
			arr[arr.length-1] = arr[arr.length-1].substring(0, arr[arr.length-1].length()-2);
			
			jsonCorrected = "[";
			
			for (int i = 0; i < arr.length; i++)
			{
				jsonCorrected = jsonCorrected.concat(arr[i]);
			}
			
			jsonCorrected = jsonCorrected.concat("]");
			
	      	if(jsonCorrected.startsWith("{"))
	      	//this gives Error object as "success" String value is false
	      	{
	      		JSONObject success = new JSONObject(json);
	      		
				Error errorObject = new Error(
						true,
						success.get("error_description").toString()
	      									 );
	      		return errorObject;
	      	}
	      	else
	      	{
	      		JSONArray successArray = new JSONArray(jsonCorrected);
	      		
	      		CountryInfoBasic[] countriesWithCoordinatesArray = null;
	      		
	      		if(successArray.length() > 0)
	      		{
	      			countriesWithCoordinatesArray = new CountryInfoBasic[successArray.length()];
	      			
	      			for(int i = 0; i < successArray.length(); i++)
		      		{
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
	      		}
	      		else
	      		{
	      			return countriesWithCoordinatesArray;
	      		}
	      	}
   	 	}
   	 	catch(JSONException e)        
	   	 	{
	        	 System.out.println("Error parsing parseGetCountriesWithCoordinates!!!");
	        }
		
		return null;
	}
}