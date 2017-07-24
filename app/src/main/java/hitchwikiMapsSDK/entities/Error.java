package hitchwikiMapsSDK.entities;

public class Error 
{	
//	In case of error
//	If API produces an error, it returns "error":"true" and possible error description.
//
//	JSON Example where calling /api/?place=351 didn't find a place with this ID:
//
//	{
//		"error":"true",
//		"error_description":"Place not found."
//	}
	
	private boolean error;
	private String errorDescription;
	
	public Error(boolean error, String errorDescription) 
	{
		this.error = error;
		this.errorDescription = errorDescription;
	}

	//getters and setters
	public boolean isError() 
	{
		return error;
	}
	
	public void setError(boolean error) 
	{
		this.error = error;
	}
	
	public String getErrorDescription() 
	{
		return errorDescription;
	}
	
	public void setErrorDescription(String errorDescription) 
	{
		this.errorDescription = errorDescription;
	}
}
