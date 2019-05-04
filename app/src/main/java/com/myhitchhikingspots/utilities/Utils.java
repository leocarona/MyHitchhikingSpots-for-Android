package com.myhitchhikingspots.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.ErrorManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import hitchwikiMapsSDK.classes.ApiManager;
import hitchwikiMapsSDK.entities.CountryInfoBasic;
import hitchwikiMapsSDK.entities.PlaceInfoBasic;

import com.myhitchhikingspots.BuildConfig;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.model.Spot;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
    //metric radius
    public static double radiusToMeters(double radius) {
        return ((radius / 0.015) * 1700);
    }

    //testing if markers are within range:
    public static boolean isInRectangle(double centerX, double centerY, double radius, double x, double y) {
        return x >= centerX - radius && x <= centerX + radius
                && y >= centerY - radius && y <= centerY + radius;
    }

    // test if coordinate (x, y) is within a radius from coordinate (centerX, centerY)
    public static boolean isPointInCircle(double centerX, double centerY, double radius, double x, double y) {
        if (isInRectangle(centerX, centerY, radius, x, y)) {
            double dx = centerX - x;
            double dy = centerY - y;
            dx *= dx;
            dy *= dy;
            double distanceSquared = dx + dy;
            double radiusSquared = radius * radius;
            return distanceSquared <= radiusSquared;
        }
        return false;
    }

    //calculation of distance between two gps points using Haversine formula
    //http://en.wikipedia.org/wiki/Haversine_formula
    @SuppressLint("UseValueOf")
    public static double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 3958.75;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double dist = earthRadius * c;

        int meterConversion = 1609;

        //result returns in meters
        return (dist * meterConversion);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean hasActiveInternetConnection(Context context) {
        if (isNetworkAvailable(context)) {
            try {
                HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
                urlc.setRequestProperty("User-Agent", "Test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                return (urlc.getResponseCode() == 200);
            } catch (IOException e) {
                Log.e("active internet check", "Error checking internet connection", e);
            }
        } else {
            Log.d("active internet check", "No network available!");
        }
        return false;
    }

    //converts Stream to String
    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        reader.close();

        return sb.toString();
    }

    //removes &quot; artefact strings from String values, also possibly other stuff
    public static String stringBeautifier(String stringToGetCorrected) {
        String quotesArtefact = "&quot;";
        String quotesArtefactCorrected = "\"";

        stringToGetCorrected = stringToGetCorrected.replaceAll(quotesArtefact, quotesArtefactCorrected);

        return stringToGetCorrected;
    }

    //comparing values
    public static double getMax(double a, double b) {
        if (a > b) {
            return a;
        }
        return b;
    }

    //comparing values
    public static double getMin(double a, double b) {
        if (a < b) {
            return a;
        }
        return b;
    }

    public static CountryInfoBasic[] parseGetCountriesWithCoordinates(String json) {
        //NOTE: hitchwikiMapsSDK.classes.JSONParser also provides a parseGetCountriesWithCoordinates, but it was creating an issue on the json parameter when it tried to fix something on it - maybe it tried to fix an old issue that no longer exist?

        try {
            JSONArray var9 = new JSONArray(json);
            CountryInfoBasic[] countriesWithCoordinatesArray = null;
            if (var9.length() <= 0) {
                return countriesWithCoordinatesArray;
            } else {
                countriesWithCoordinatesArray = new CountryInfoBasic[var9.length()];

                for (int i = 0; i < var9.length(); ++i) {
                    JSONObject rec = var9.getJSONObject(i);
                    countriesWithCoordinatesArray[i] = new CountryInfoBasic(rec.get("iso").toString(), rec.get("name").toString(), rec.get("places").toString(), rec.get("lat").toString(), rec.get("lon").toString());
                }

                return countriesWithCoordinatesArray;
            }
        } catch (JSONException var8) {
            System.out.println("Error parsing parseGetCountriesWithCoordinates!!!");
            return null;
        }
    }

    public static CountryInfoBasic[] loadCountriesListFromLocalFile() {

        try {
            //get markersStorageFile streamed into String, so gson can convert it into placesContainer
            String placesContainerAsString = loadFileFromLocalStorage(Constants.HITCHWIKI_MAPS_COUNTRIES_LIST_FILE_NAME);

            return parseGetCountriesWithCoordinates(placesContainerAsString);
        } catch (Exception exception) {
            Crashlytics.logException(exception);
        }

        return new CountryInfoBasic[0];
    }

    static String TAG = "utils";

    public static PlaceInfoBasic[] loadHitchwikiSpotsFromLocalFile() {

        try {
            //get markersStorageFile streamed into String, so gson can convert it into placesContainer
            String placesContainerAsString = loadFileFromLocalStorage(Constants.HITCHWIKI_MAPS_MARKERS_LIST_FILE_NAME);

            Crashlytics.log(Log.INFO, TAG, "Calling ApiManager getPlacesByContinenFromLocalFile");
            Crashlytics.setString("placesContainerAsString", placesContainerAsString);

            if (!placesContainerAsString.isEmpty())
                return new ApiManager().getPlacesByContinenFromLocalFile(placesContainerAsString);
        } catch (Exception exception) {
            Crashlytics.logException(exception);
        }

        return new PlaceInfoBasic[0];
    }


    public static Boolean shouldLoadCurrentView(SharedPreferences sharedPreferences) {
        Boolean result = false;

        if (DateUtils.isToday(sharedPreferences.getLong(Constants.PREFS_TIMESTAMP_OF_LAST_OFFLINE_MODE_WARN, 0))) {
            result = sharedPreferences.getBoolean(Constants.PREFS_OFFLINE_MODE_SHOULD_LOAD_CURRENT_VIEW, false);
        } else {
            sharedPreferences.edit().remove(Constants.PREFS_TIMESTAMP_OF_LAST_OFFLINE_MODE_WARN).apply();
            sharedPreferences.edit().remove(Constants.PREFS_OFFLINE_MODE_SHOULD_LOAD_CURRENT_VIEW).apply();
        }

        return result;
    }

    public static String getExportFileName(Date date) {
        String DATE_FORMAT_NOW = "yyyy_MM_dd_HHmm-";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return String.format("%s.csv", sdf.format(date) + Constants.INTERNAL_DB_FILE_NAME);
    }

    public static String getLocalStoragePathToFile(String destinationFileName, Context context) {
        //The fuild condition >=17 was copied from https://stackoverflow.com/q/37699373/1094261

        //Build path to databases directory
        String destinationFilePath = "";
        if (android.os.Build.VERSION.SDK_INT >= 17)
            destinationFilePath = String.format("%1$s/databases/%2$s", context.getApplicationInfo().dataDir, destinationFileName);
        else
            destinationFilePath = String.format("data/data/%1$s/databases/%2$s", context.getPackageName(), destinationFileName);

        Crashlytics.setString("destinationFilePath", destinationFilePath);
        return destinationFilePath;
    }

    public static String copySQLiteDBIntoLocalStorage(String originFileName, String destinationFilePath, Context context) {
        Crashlytics.setString("originFileName", originFileName);
        Crashlytics.setString("destinationFileName", destinationFilePath);

        //Get external storage directory
        File origin = Environment.getExternalStorageDirectory();
        Crashlytics.setString("origin", origin.getAbsolutePath());

        //Get File object for the origin database
        File originFile = new File(origin, originFileName);

        return copySQLiteDBIntoLocalStorage(originFile, destinationFilePath, context);
    }

    public static String copySQLiteDBIntoLocalStorage(File originFile, String destinationFilePath, Context context) {
        String errorMessage = "";
        try {
            //Get File object for the destination database
            File destinationFile = new File(destinationFilePath);

            Crashlytics.log(Log.INFO, TAG, "destinationFilePath: " + destinationFilePath);

            // if (destinationFile.canWrite()) {
            Crashlytics.log(Log.INFO, TAG, "Will start copying originFile content into destinationFile.");

            //Copy originFile content into destinationFile
            if (originFile.exists()) {
                FileChannel src = new FileInputStream(originFile).getChannel();
                FileChannel dst = new FileOutputStream(destinationFile).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
            }

            Crashlytics.log(Log.INFO, TAG, "Successfuly copied originFile content into destinationFile.");
           /* } else {
                Crashlytics.log(Log.WARN, TAG, "User doesn't have permission to write a file to destination.");
                errorMessage = context.getString(R.string.general_not_enough_permission);
                if (BuildConfig.DEBUG)
                    errorMessage += "\n\nPath: " + destinationFile.getAbsolutePath();
            }*/
        } catch (Exception e) {
            Crashlytics.logException(e);
            errorMessage = "\nError happened while trying to make a local copy of your file:\n'" + e.getMessage() + "'";
        }
        return errorMessage;
    }

    public static String loadFileFromLocalStorage(String fileName) {
        Crashlytics.setString("Name of the file to load", fileName);
        File markersStorageFolder = new File(Constants.HITCHWIKI_MAPS_STORAGE_PATH);

        String result = "";
        File fl = new File(markersStorageFolder, fileName);
        try {
            FileInputStream fin = new FileInputStream(fl);
            result = Utils.convertStreamToString(fin);
            fin.close();
        } catch (Exception exception) {
            Crashlytics.logException(exception);
        }

        return result;
    }

    public static Spot convertToSpot(PlaceInfoBasic place) {
        Spot spot = new Spot();
        if (place.getId() != null)
            spot.setId(Long.valueOf(place.getId()));
        if (place.getLat() != null)
            spot.setLatitude(Double.parseDouble(place.getLat()));
        if (place.getLon() != null)
            spot.setLongitude(Double.parseDouble(place.getLon()));
        if (place.getRating() != null)
            spot.setHitchability(Integer.valueOf(place.getRating()));
        spot.setAttemptResult(Constants.ATTEMPT_RESULT_GOT_A_RIDE);
        spot.setIsHitchhikingSpot(true);

        /*Setting attributes to false here is useless - we've just created the new Spot therefore its attributes are by default false
        spot.setIsWaitingForARide(false);
        spot.setIsPartOfARoute(false);
        spot.setIsDestination(false);*/
        return spot;
    }

    public static String getRatingOrDefaultAsString(Context context, int rating) {
        if (rating == 0)
            return context.getString(R.string.map_infoview_spot_type_not_evaluated);
        else
            return Utils.getRatingAsString(context, Utils.findTheOpposite(rating));
    }

    public static String getRatingAsString(Context context, Integer rating) {
        String res = "";
        switch (rating) {
            case 1:
                res = context.getString(R.string.hitchability_senseless);
                break;
            case 2:
                res = context.getString(R.string.hitchability_bad);
                break;
            case 3:
                res = context.getString(R.string.hitchability_average);
                break;
            case 4:
                res = context.getString(R.string.hitchability_good);
                break;
            case 5:
                res = context.getString(R.string.hitchability_very_good);
                break;
           /* default:
                res = getResources().getString(R.string.hitchability_no_answer);
                break;*/
        }
        return res;
    }

    public static Integer findTheOpposite(Integer rating) {
        //NOTE: For sure there should be a math formula to find this result, I just didn't feel like using
        // more time on this so why not a switch until you make it better =)
        Integer res = 0;
        switch (rating) {
            case 1:
                res = 5;
                break;
            case 2:
                res = 4;
                break;
            case 3:
                res = 3;
                break;
            case 4:
                res = 2;
                break;
            case 5:
                res = 1;
                break;
        }
        return res;
    }

    public static ArrayList<String> spotLocationToList(Spot spot) {
        ArrayList<String> loc = new ArrayList();

        //Add city
        if (spot.getCity() != null && !spot.getCity().trim().isEmpty() && !spot.getCity().equals("null"))
            loc.add(spot.getCity().trim());

        //Add state
        if (spot.getState() != null && !spot.getState().trim().isEmpty() && !spot.getState().equals("null"))
            loc.add(spot.getState().trim());

        //Add country code or country name
        if (spot.getCountryCode() != null && !spot.getCountryCode().trim().isEmpty() && !spot.getCountryCode().equals("null"))
            loc.add(spot.getCountryCode().trim());
        else if (spot.getCountry() != null && !spot.getCountry().trim().isEmpty() && !spot.getCountry().equals("null"))
            loc.add(spot.getCountry().trim());

        //If only 2 or less were added, add the street in the begining of the list
        if (loc.size() <= 2 && spot.getStreet() != null && !spot.getStreet().trim().isEmpty() && !spot.getStreet().equals("null")) {
            ArrayList<String> loc2 = new ArrayList();

            //Add street
            loc2.add(spot.getStreet().trim());

            //Add all the others after adding the street, so that the street is written first
            loc2.addAll(loc);

            //Replace loc list for loc2
            loc = loc2;
        }
        return loc;
    }

    @NonNull
    public static String getWaitingTimeAsString(Integer waitingTime, Context context) {
        int weeks = waitingTime / 7 / 24 / 60;
        int days = waitingTime / 24 / 60;
        int hours = waitingTime / 60 % 24;
        int minutes = waitingTime % 60;
        String dateFormated = "";

        if (weeks > 0)
            days = days % 7;

        if (weeks > 0)
            dateFormated += String.format(context.getString(R.string.general_weeks_label), weeks);

        if ((days > 0 || hours > 0 || minutes > 0) && !dateFormated.isEmpty())
            dateFormated += " ";

        if (days > 0 || ((hours > 0 || minutes > 0) && !dateFormated.isEmpty()))
            dateFormated += String.format(context.getString(R.string.general_days_label), days);

        if ((hours > 0 || minutes > 0) && !dateFormated.isEmpty())
            dateFormated += " ";

        if (hours > 0 || (minutes > 0 && !dateFormated.isEmpty()))
            dateFormated += String.format(context.getString(R.string.general_hours_label), hours);

        if (minutes > 0 && !dateFormated.isEmpty())
            dateFormated += " ";

        if (minutes == 0 && dateFormated.isEmpty())
            dateFormated += context.getString(R.string.general_seconds_label);
        else if (minutes > 0 || dateFormated.isEmpty())
            dateFormated += String.format(context.getString(R.string.general_minutes_label), minutes);

        return dateFormated;
    }

    @NonNull
    public static String dateTimeToString(DateTime dt) {
        return dateTimeToString(dt, ", ");
    }

    @NonNull
    public static String dateTimeToString(DateTime dt, String separator) {
        if (dt != null) {
            String dateFormat = "dd/MMM'" + separator + "'HH:mm";

            if (Locale.getDefault() == Locale.US)
                dateFormat = "MMM/dd'" + separator + "'HH:mm";

            try {
                return DateTimeFormat.forPattern(dateFormat).print(dt.toInstant());
            } catch (Exception ex) {
                Crashlytics.setString("date", dt.toString());
                Crashlytics.log(Log.WARN, "dateTimeToString", "Err msg: " + ex.getMessage());
                Crashlytics.logException(ex);
            }
        }
        return "";
    }
}
