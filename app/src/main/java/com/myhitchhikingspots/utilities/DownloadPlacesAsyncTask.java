package com.myhitchhikingspots.utilities;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.ToolsActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import hitchwikiMapsSDK.classes.APICallCompletionListener;
import hitchwikiMapsSDK.entities.Error;
import hitchwikiMapsSDK.entities.PlaceInfoBasic;

//async task to retrieve markers
public class DownloadPlacesAsyncTask extends AsyncTask<Void, Void, String> {
    private final static String TAG = "download-places-async-task";
    private String lstToDownload = "";
    private String typeToDownload = "";
    private File hitchwikiStorageFolder;
    private onPlacesDownloadedListener callback;
    private APICallCompletionListener<PlaceInfoBasic[]> getPlacesByArea;

    public interface onPlacesDownloadedListener {
        void onDownloadedFinished(String result);
    }

    public DownloadPlacesAsyncTask(File hitchwikiStorageFolder, String type, String lstToDownload, APICallCompletionListener<PlaceInfoBasic[]> getPlacesByArea, onPlacesDownloadedListener callback) {
        this.typeToDownload = type;
        this.lstToDownload = lstToDownload;
        this.hitchwikiStorageFolder = hitchwikiStorageFolder;
        this.callback = callback;
        this.getPlacesByArea = getPlacesByArea;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String doInBackground(Void... params) {
        if (isCancelled()) {
            return "Canceled";
        }

        //folder exists, but it may be a case that file with stored markers is missing, so lets check that
        File fileCheck = new File(hitchwikiStorageFolder, Constants.HITCHWIKI_MAPS_MARKERS_LIST_FILE_NAME);
        fileCheck.delete();

        String res = "nothingToSync";

        try {
            if (!lstToDownload.isEmpty()) {
                res = "spotsDownloaded";
                String[] codes = lstToDownload.split(DownloadHWSpotsDialog.LIST_SEPARATOR);

                switch (typeToDownload) {
                    case DownloadHWSpotsDialog.DIALOG_TYPE_CONTINENT:

                        for (String continentCode : codes) {
                            Crashlytics.log(Log.INFO, TAG, "Calling ApiManager getPlacesByContinent");
                            ToolsActivity.hitchwikiAPI.getPlacesByContinent(continentCode, getPlacesByArea);
                        }
                        break;
                    case DownloadHWSpotsDialog.DIALOG_TYPE_COUNTRY:
                        for (String countryCode : codes) {
                            Crashlytics.log(Log.INFO, TAG, "Calling ApiManager getPlacesByCountry");
                            ToolsActivity.hitchwikiAPI.getPlacesByCountry(countryCode, getPlacesByArea);

                        }
                        break;
                }
            }
        } catch (Exception ex) {
            if (ex.getMessage() != null)
                res = ex.getMessage();
            Crashlytics.logException(ex);
        }
        return res;
    }

    @Override
    protected void onPostExecute(String result) {
        if (isCancelled())
            return;

        if (callback != null)
            callback.onDownloadedFinished(result);
    }
}
