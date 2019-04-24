package hitchwikiMapsSDK.classes;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import hitchwikiMapsSDK.entities.Error;

public class ServerRequest {
    String jsonResult = null;
    InputStream is = null;
    JSONObject jObj;
    JSONArray jArray;
    static String TAG = "server-request";

    public String postRequestString(String url) {
        jsonResult = "";
        String errorMsg = "";
        HttpURLConnection urlConnection;

        try {
            // the url we wish to connect to
            URL urlObj = new URL(url);
            // open the connection to the specified URL
            urlConnection = (HttpURLConnection) urlObj.openConnection();
            // get the response from the server in an input stream
            is = new BufferedInputStream(urlConnection.getInputStream());

            // covert the input stream to a string
            jsonResult = convertStreamToString(is);

        } catch (Exception e) {
            errorMsg = e.getMessage();
            Crashlytics.logException(e);
        }

        Crashlytics.log(Log.INFO, TAG, "Received result converted into string:" + jsonResult);

        if (!errorMsg.isEmpty()) {
            Error er = new Error(true, errorMsg);
            jObj = er.toJSONObject();
            jsonResult = er.toJSONString();
        }

        // return JSON string
        return jsonResult;
    }

    public JSONObject postRequest(String url) {
        // Making HTTP request
        Crashlytics.log(Log.INFO, TAG, "Posting to url: " + url);

        //Set jObj to null here so that if postRequestString throws any exception, jOBj won't be null
        jObj = null;

        //Call postRequestString - the result to the call to url will be stored in the variable json
        postRequestString(url);

        //If an exception was thrown, jOBj will be an Error object converted to json string
        if (jObj == null) {
            String errorMsg = "";

            // try parse the string to a JSON object
            try {
                jObj = new JSONObject(jsonResult);
            } catch (Exception e) {
                errorMsg = e.getMessage();
                Crashlytics.logException(e);
            }

            if (!errorMsg.isEmpty()) {
                Error er = new Error(true, errorMsg);
                jObj = er.toJSONObject();
                jsonResult = er.toJSONString();
            }
        }

        // return JSON object
        return jObj;
    }

    public String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
