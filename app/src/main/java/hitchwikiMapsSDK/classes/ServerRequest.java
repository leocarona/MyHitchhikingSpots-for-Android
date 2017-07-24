package hitchwikiMapsSDK.classes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ServerRequest 
{
	String json = null;
	InputStream is = null;
    JSONObject jObj;
    JSONArray jArray;
    
    //hitchwiki
    	
	public JSONObject postRequest(String url)
	{
		// Making HTTP request
		try 
		{
            // defaultHttpClient
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
//            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();
            System.out.println(is.toString());
        } 
		catch (UnsupportedEncodingException e) 
		{
           System.out.println("UnsupportedEncodingException: UnsupportedEncodingExceptio");
        } 
		catch (ClientProtocolException e) 
		{
        	System.out.println("ClientProtocolException: ClientProtocolException");
        } 
		catch (IOException e) 
		{
        	System.out.println("IOException: IOException");
        }
 
		try 
		{
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) 
            {
                sb.append(line + "n");
            }
            is.close();
            sb.deleteCharAt(sb.length()-1);
            json = sb.toString();
            System.out.println("JSON after post: "+ json);
        } 
		catch (Exception e) 
		{
        	System.out.println("Buffer Error: Error converting result " + e.toString());
        }
        
		// try parse the string to a JSON object
        try 
        {
            jObj = new JSONObject(json);
        } 
        catch (JSONException e) 
        {
            System.out.println("JSON Parser: Error parsing data " + e.toString());
        }
 
        // return JSON String
        return jObj;
	}
	
	public String postRequestString(String url)
	{// Making HTTP request
        
		try 
		{
            // defaultHttpClient
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();
            System.out.println(is.toString());    
        } 
		catch (UnsupportedEncodingException e) 
		{
           System.out.println("UnsupportedEncodingExceptio: UnsupportedEncodingExceptio");
        } 
		catch (ClientProtocolException e) 
		{
        	System.out.println("ClientProtocolException: ClientProtocolException");
        } 
		catch (IOException e) 
		{
        	System.out.println("IOException: IOException");
        }
		
		try 
		{
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) 
            {
                sb.append(line + "n");
            }
            is.close();
            sb.deleteCharAt(sb.length()-1);
            json = sb.toString();
            return json;
        } 
		catch (Exception e) 
		{
        	System.out.println("Buffer Error: Error converting result " + e.toString());
        }
        return null;
	}
	
//	public JSONObject getRequest(String url, String token, String xApiKey)
//	{
//		try 
//		{
//            // defaultHttpClient
//            DefaultHttpClient httpClient = new DefaultHttpClient();
//                                
//            HttpGet httpGet = new HttpGet(url);
//            
//            httpGet.addHeader("X-API-KEY", xApiKey.toString());
//            httpGet.addHeader("ACCESS-TOKEN", token.toString());
//            
//            HttpResponse httpResponse = httpClient.execute(httpGet);
//            HttpEntity httpEntity = httpResponse.getEntity();
//            is = httpEntity.getContent();
//            System.out.println(is.toString());
// 
//        } 
//		catch (UnsupportedEncodingException e) 
//		{
//           System.out.println("UnsupportedEncodingExceptio: UnsupportedEncodingExceptio");
//        } 
//		catch (ClientProtocolException e) 
//		{
//        	System.out.println("ClientProtocolException: ClientProtocolException");
//        } 
//		catch (IOException e) 
//		{
//        	System.out.println("IOException: IOException");
//        }
//        
//		try 
//		{
//            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
//            StringBuilder sb = new StringBuilder();
//            String line = null;
//            while ((line = reader.readLine()) != null) 
//            {
//                sb.append(line + "n");
//            }
//            is.close();
//            sb.deleteCharAt(sb.length()-1);
//            json = sb.toString();
//            System.out.println("JSON after GET: "+ json); 
//        } 
//		catch (Exception e) 
//		{
//        	System.out.println("Buffer Error: Error converting result " + e.toString());
//        }
//        
//		// try parse the string to a JSON object
//        try 
//        {
//            jObj = new JSONObject(json);
//        }
//        catch (JSONException e) 
//        {
//            System.out.println("JSON Parser: Error parsing data " + e.toString());
//        }
// 
//        // return JSON String
//        return jObj;
//	}
	
}
