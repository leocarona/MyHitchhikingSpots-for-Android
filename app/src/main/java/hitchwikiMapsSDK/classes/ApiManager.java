package hitchwikiMapsSDK.classes;

import org.json.JSONObject;

import hitchwikiMapsSDK.entities.CountryInfoBasic;
import hitchwikiMapsSDK.entities.PlaceInfoBasic;
import hitchwikiMapsSDK.entities.Error;
import hitchwikiMapsSDK.entities.PlaceInfoComplete;

public class ApiManager 
{
	private ServerRequest mServerRequest;
	
	public ApiManager()
	{
		//we use ServerRequest for post and get methods
		mServerRequest= new ServerRequest();
	}
	
	public void getPlaceBasicDetails(final int id, final APICallCompletionListener<PlaceInfoBasic> callback)
	{		
		Runnable r = new Runnable() 
		{	 
			@Override
			public void run() 
			{
				JSONObject response= mServerRequest.postRequest
						(
						APIConstants.ENDPOINT_PREFIX + 
						APIConstants.PLACE_INFO_BASIC_PREFIX + 
						String.valueOf(id) + 
						APIConstants.PLACE_INFO_BASIC_POSTFIX
						);
				
				JSONParser parser = new JSONParser();
				
				Object resultObject = parser.parseGetPlaceBasicDetails(response);
				
				if(resultObject != null)
				{
					if(resultObject.getClass().isAssignableFrom(Error.class))
					{
						callback.onComplete(false, -1, "", (Error)resultObject, null);
					}
					else
					{
						callback.onComplete(true, -1, "", null, (PlaceInfoBasic)resultObject);
					}
				}
			}
		};
		
		r.run();
	}
	
	public void getPlaceCompleteDetails(final int id, final APICallCompletionListener<PlaceInfoComplete> callback)
	{		
		Runnable r = new Runnable() 
		{	 
			@Override
			public void run() 
			{
				JSONObject response= mServerRequest.postRequest
						(
						APIConstants.ENDPOINT_PREFIX + 
						APIConstants.PLACE_INFO_BASIC_PREFIX + 
						String.valueOf(id)
						);
				
				JSONParser parser = new JSONParser();
				
				Object resultObject = parser.parseGetPlaceCompleteDetails(response);
				
				if(resultObject != null)
				{
					if(resultObject.getClass().isAssignableFrom(Error.class))
					{
						callback.onComplete(false, -1, "", (Error)resultObject, null);
					}
					else
					{
						callback.onComplete(true, -1, "", null, (PlaceInfoComplete)resultObject);
					}
				}
			}
		};
		
		r.run();
	}
	
	public void getPlacesFromArea(final float latFrom, 
								  final float latTo, 
								  final float lonFrom, 
								  final float lonTo, 
								  final APICallCompletionListener<PlaceInfoBasic[]> callback)
	{		
		Runnable r = new Runnable() 
		{	 
			@Override
			public void run() 
			{
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
				
				if(resultObject != null)
				{
					if(resultObject.getClass().isAssignableFrom(Error.class))
					{
						callback.onComplete(false, -1, "", (Error)resultObject, null);
					}
					else
					{
						callback.onComplete(true, -1, "", null, (PlaceInfoBasic[])resultObject);
					}
				}
			}
		};
		
		r.run();
	}
	
	public void getPlacesByCountry
			 (
			  final String countryIsoCode, 
			  final APICallCompletionListener<PlaceInfoBasic[]> callback
			 )
	{
		Runnable r = new Runnable() 
		{
			@Override
			public void run() 
			{
				String response = mServerRequest.postRequestString
							(
								APIConstants.ENDPOINT_PREFIX
								+ APIConstants.LIST_PLACES_BY_COUNTRY
								+ countryIsoCode
							);

				JSONParser parser = new JSONParser();

				Object resultObject = parser.parseGetPlacesByCountry(response);

				if (resultObject != null) 
				{
					if (resultObject.getClass().isAssignableFrom(Error.class)) 
					{
						callback.onComplete(false, -1, "", (Error) resultObject, null);
					} 
					else 
					{
						callback.onComplete(true, -1, "", null, (PlaceInfoBasic[]) resultObject);
					}
				}
			}
		};

		r.run();
	}
	
	public void getPlacesByContinent
			 (
			  final String continentCode, 
			  final APICallCompletionListener<PlaceInfoBasic[]> callback
			 )
	 {
		Runnable r = new Runnable() 
		{
			@Override
			public void run() 
			{
				String response = mServerRequest.postRequestString
						(
								APIConstants.ENDPOINT_PREFIX
								+ APIConstants.LIST_PLACES_BY_CONTINENT
								+ continentCode
						);

				JSONParser parser = new JSONParser();

				Object resultObject = parser.parseGetPlacesByCountry(response);

				if (resultObject != null) 
				{
					if (resultObject.getClass().isAssignableFrom(Error.class)) 
					{
						callback.onComplete(false, -1, continentCode, (Error) resultObject, null);
					} 
					else 
					{
						callback.onComplete(true, -1, continentCode, null,	(PlaceInfoBasic[]) resultObject);
					}
				}
			}
		};

		r.run();
	}
	
	public PlaceInfoBasic[] getPlacesByContinenFromLocalFile(String responseFromLocalFile)
	{
		JSONParser parser = new JSONParser();

		Object resultObject = parser.parseGetPlacesByCountry(responseFromLocalFile);

		return (PlaceInfoBasic[]) resultObject;
	}
	
	public void getCountriesWithCoordinatesAndMarkersNumber
	 (
	  final APICallCompletionListener<CountryInfoBasic[]> callback
	 )
	{
		Runnable r = new Runnable() 
		{
			@Override
			public void run() 
			{
				String response = mServerRequest.postRequestString
						(
								APIConstants.ENDPOINT_PREFIX
								+ APIConstants.LIST_OF_COUNTRIES_AND_COORDINATES
						);
		
				JSONParser parser = new JSONParser();
		
				Object resultObject = parser.parseGetCountriesWithCoordinates(response);
		
				if (resultObject != null) 
				{
					if (resultObject.getClass().isAssignableFrom(Error.class)) 
					{
						callback.onComplete(false, -1, "", (Error) resultObject, null);
					} 
					else 
					{
						callback.onComplete(true, -1, "", null,	(CountryInfoBasic[]) resultObject);
					}
				}
			}
		};
	
	r.run();
	}
	
	
}
