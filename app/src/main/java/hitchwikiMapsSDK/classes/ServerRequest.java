package hitchwikiMapsSDK.classes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import hitchwikiMapsSDK.entities.Error;

public class ServerRequest 
{
	String json = null;
	InputStream is = null;
    JSONObject jObj;
    JSONArray jArray;
    
    //hitchwiki
    	
	public JSONObject postRequest(String url)
	{
        String errorMsg = "";

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
		catch (Exception e)
		{
            errorMsg = e.getMessage();
        }

         if(errorMsg.isEmpty()) {
             try {
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
                 StringBuilder sb = new StringBuilder();
                 String line = null;
                 while ((line = reader.readLine()) != null) {
                     sb.append(line + "n");
                 }
                 is.close();
                 sb.deleteCharAt(sb.length() - 1);
                 json = sb.toString();
                 System.out.println("JSON after post: " + json);
             } catch (Exception e) {
                 errorMsg = e.getMessage();
             }
         }

        if(errorMsg.isEmpty()) {
            // try parse the string to a JSON object
            try {
                jObj = new JSONObject(json);
            } catch (JSONException e) {
                errorMsg = e.getMessage();
            }
        }

        if(!errorMsg.isEmpty())
            jObj = new Error(true, errorMsg).toJSONObject();
            // return JSON object
        return jObj;
	}
	
	public String postRequestString(String url)
	{// Making HTTP request

        String errorMsg = "";
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
		catch (Exception e)
		{
            errorMsg = e.getMessage();
        }

        if(errorMsg.isEmpty()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "n");
                }
                is.close();
                sb.deleteCharAt(sb.length() - 1);
                json = sb.toString();
            } catch (Exception e) {
                errorMsg = "Buffer Error: Error converting result.\n\"" + e.getMessage() + "\"";
            }
        }

        if(!errorMsg.isEmpty())
            json = new Error(true, errorMsg).toJSONString();
        // return JSON string
        return json;
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
