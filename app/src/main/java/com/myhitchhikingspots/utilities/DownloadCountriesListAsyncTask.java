package com.myhitchhikingspots.utilities;

import android.os.AsyncTask;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.Constants;

import java.io.File;

import hitchwikiMapsSDK.classes.APICallCompletionListener;
import hitchwikiMapsSDK.classes.APIConstants;
import hitchwikiMapsSDK.classes.ApiManager;
import hitchwikiMapsSDK.classes.JSONParser;
import hitchwikiMapsSDK.classes.ServerRequest;
import hitchwikiMapsSDK.entities.CountryInfoBasic;
import hitchwikiMapsSDK.entities.Error;

public class DownloadCountriesListAsyncTask extends AsyncTask<Void, Void, Boolean> {
    private final static String TAG = "download-places-by-country-async-task";
    private onPlacesDownloadedListener callback;
    private Object resultObject = null;
    private String errMsg = "";

    public interface onPlacesDownloadedListener {
        void onDownloadBegins();

        void onDownloadedFinished(String result, CountryInfoBasic[] countriesContainer);
    }

    public DownloadCountriesListAsyncTask(onPlacesDownloadedListener callback) {
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        if (callback != null)
            callback.onDownloadBegins();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Boolean doInBackground(Void... params) {
        if (isCancelled()) {
            return false;
        }

        try {
            Crashlytics.log(Log.INFO, TAG, "Calling ApiManager getCountriesWithCoordinatesAndMarkersNumber");

            //we use ServerRequest for post and get methods
            ServerRequest mServerRequest = new ServerRequest();

            String response = mServerRequest.postRequestString
                    (
                            APIConstants.ENDPOINT_PREFIX
                                    + APIConstants.LIST_OF_COUNTRIES_AND_COORDINATES
                    );

            JSONParser parser = new JSONParser();

            resultObject = parser.parseGetCountriesWithCoordinates(response);

            if (resultObject == null || resultObject.getClass().isAssignableFrom(Error.class)) {
                errMsg = "Unable to download countries list. Please try again later and if the problem persists, let us know!";

                if (resultObject != null && resultObject.getClass().isAssignableFrom(Error.class))
                    errMsg = ((Error) resultObject).getErrorDescription();

                //NOTE: The right thing to do here would be to call onComplete, but
                // all the logic in the apps (Hitchwiki Maps app and MyHitchhikingSpots app) that call the old version of this method
                // were developed considering that an exception would be thrown if an error
                // would happen in parseGetCountriesWithCoordinates, so we better throw an exception here.
                //-----callback.onComplete(false, -1, "", (Error) resultObject, null);
                //throw new RuntimeException(errMsg);

                return false;
            }

            return true;
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean isCountriesDownloaded) {
        if (isCancelled())
            return;

        if (callback != null) {
            if (!isCountriesDownloaded)
                callback.onDownloadedFinished(errMsg, null);
            else
                callback.onDownloadedFinished("countriesListDownloaded", (CountryInfoBasic[]) resultObject);
        }
    }
}
