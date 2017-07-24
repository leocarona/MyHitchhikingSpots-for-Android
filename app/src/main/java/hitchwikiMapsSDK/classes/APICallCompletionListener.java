package hitchwikiMapsSDK.classes;

import hitchwikiMapsSDK.entities.Error;

public interface APICallCompletionListener<T> 
{
	public void onComplete
	(
		boolean success, 
		int occasionalParameter,
		String stringParameter,
		Error error, 
		T object
	);
}
