package com.myhitchhikingspots.utilities;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.ToolsActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import hitchwikiMapsSDK.classes.APICallCompletionListener;
import hitchwikiMapsSDK.classes.ApiManager;
import hitchwikiMapsSDK.entities.CountryInfoBasic;

import static com.myhitchhikingspots.HitchwikiMapViewFragment.continentsContainer;

public class DownloadPlacesByCountryAsyncTask extends AsyncTask<Void, Void, String> {
    private final static String TAG = "download-places-by-country-async-task";
    private Boolean shouldDeleteExisting;
    private onPlacesDownloadedListener callback;
    private File hitchwikiStorageFolder;
    private APICallCompletionListener<CountryInfoBasic[]> getCountriesAndCoordinates;
    private static final ApiManager hitchwikiAPI = new ApiManager();
    private CountryInfoBasic[] countriesContainer;

    public interface onPlacesDownloadedListener {
        void onDownloadBegins();

        void onDownloadedFinished(String result, CountryInfoBasic[] countriesContainer);
    }

    public DownloadPlacesByCountryAsyncTask(Boolean shouldDeleteExisting, onPlacesDownloadedListener callback, File hitchwikiStorageFolder, APICallCompletionListener<CountryInfoBasic[]> getCountriesAndCoordinates) {
        this.shouldDeleteExisting = shouldDeleteExisting;
        this.callback = callback;
        this.hitchwikiStorageFolder = hitchwikiStorageFolder;
        this.getCountriesAndCoordinates = getCountriesAndCoordinates;
    }

    @Override
    protected void onPreExecute() {
        if (callback != null)
            callback.onDownloadBegins();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String doInBackground(Void... params) {
        if (isCancelled()) {
            return "Canceled";
        }

        //recreate countriesContainer, it might not be empty
        countriesContainer = new CountryInfoBasic[0];

        //folder exists, but it may be a case that file with stored markers is missing, so lets check that
        File fileCheck = new File(hitchwikiStorageFolder, Constants.HITCHWIKI_MAPS_COUNTRIES_LIST_FILE_NAME);

        String result = "";
        if ((fileCheck.exists() && fileCheck.length() == 0) || shouldDeleteExisting) {
            if (fileCheck.exists()) { //folder exists (totally expected), so lets delete existing file now
                //but its size is 0KB, so lets delete it and download markers again
                fileCheck.delete();
            }

            try {
                Crashlytics.log(Log.INFO, TAG, "Calling ApiManager getCountriesWithCoordinatesAndMarkersNumber");
                hitchwikiAPI.getCountriesWithCoordinatesAndMarkersNumber(getCountriesAndCoordinates);
                result = "countriesListDownloaded";
            } catch (Exception ex) {
                Crashlytics.logException(ex);
                result = ex.getMessage();
            }
        } else {
            countriesContainer = Utils.loadCountriesListFromLocalFile();
            if (countriesContainer == null)
                countriesContainer = new CountryInfoBasic[0];
            result = "countriesLoadedFromLocalStorage";
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        if (isCancelled())
            return;

        if (callback != null)
            callback.onDownloadedFinished(result, countriesContainer);
    }
}
