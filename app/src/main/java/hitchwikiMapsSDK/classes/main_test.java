package hitchwikiMapsSDK.classes;

import hitchwikiMapsSDK.entities.CountryInfoBasic;
import hitchwikiMapsSDK.entities.Error;
import hitchwikiMapsSDK.entities.PlaceInfoBasic;
import hitchwikiMapsSDK.entities.PlaceInfoComplete;

public class main_test {

    /**
     * @param args
     */
    public static void main(String[] args) {
        final ApiManager a = new ApiManager();

//		final APICallCompletionListener<PlaceInfoBasic> createAccountCallback = new APICallCompletionListener<PlaceInfoBasic>() 
//				{
//					@Override
//					public void onComplete(boolean success,
//							Error error,
//							PlaceInfoBasic object) 
//					{
//						if(success)
//						{
//							System.out.println("Rating is: " + object.getRating());
//						}
//						else
//						{
//							System.out.println("Error message : " + error.getErrorDescription());
//						}
//					}
//				};
//				
//		APICallCompletionListener<PlaceInfoComplete> getCompletePlaceCallback = new APICallCompletionListener<PlaceInfoComplete>() 
//				{
//					@Override
//					public void onComplete(boolean success,
//							Error error,
//							PlaceInfoComplete object) 
//					{
//						if(success)
//						{
//							System.out.println("Description is: " + object.getDescriptionENdescription());
//						}
//						else
//						{
//							System.out.println("Error message : " + error.getErrorDescription());
//						}
//					}
//				};
//				
//		APICallCompletionListener<PlaceInfoBasic[]> getPlacesByArea = new APICallCompletionListener<PlaceInfoBasic[]>() 
//				{
//					@Override
//					public void onComplete(boolean success,
//							Error error,
//							PlaceInfoBasic[] object) 
//					{
//						if(success)
//						{
//							System.out.println(object.length);
//							
//							for(int i = 0; i < object.length; i++)
//							{
////								System.out.println(object[i].getId());
////								a.getPlaceBasicDetails(Integer.valueOf(object[i].getId()), createAccountCallback);
//							}
//						}
//						else
//						{
//							System.out.println("Error message : " + error.getErrorDescription());
//						}
//					}
//				};

        APICallCompletionListener<CountryInfoBasic[]> getCountriesAndCoordinates = new APICallCompletionListener<CountryInfoBasic[]>() {
            @Override
            public void onComplete
                    (boolean success,
                     int k,
                     String s,
                     Error error,
                     CountryInfoBasic[] object) {
                if (success) {
                    System.out.println(object.length);

                    for (int i = 0; i < object.length; i++) {
                        System.out.println("country is = " + object[i].getName() + ", iso is = " + object[i].getIso());
//											a.getPlaceBasicDetails(Integer.valueOf(object[i].getId()), createAccountCallback);
                    }
                } else {
                    System.out.println("Error message : " + error.getErrorDescription());
                }
            }
        };

//		a.getPlacesFromArea(63.375129767984f,65.208716083434f,22.544799804826f,25.190063476196f, getPlacesByArea);
//		a.getPlacesByContinent("EU", getPlacesByArea);

//		a.getPlaceCompleteDetails(6874, getCompletePlaceCallback);

        a.getCountriesWithCoordinatesAndMarkersNumber(getCountriesAndCoordinates);

    }

}
