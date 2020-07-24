package com.myhitchhikingspots.utilities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.crashlytics.android.Crashlytics;
import com.mapbox.geojson.Point;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.model.Route;
import com.myhitchhikingspots.model.Spot;
import com.myhitchhikingspots.model.SubRoute;
import com.savvi.rangedatepicker.CalendarPickerView;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import hitchwikiMapsSDK.classes.ApiManager;
import hitchwikiMapsSDK.entities.CountryInfoBasic;
import hitchwikiMapsSDK.entities.PlaceInfoBasic;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

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

    public static CountryInfoBasic[] loadCountriesListFromLocalFile(Context context) throws Exception {
        //get markersStorageFile streamed into String, so gson can convert it into placesContainer
        String placesContainerAsString = loadFileFromLocalStorage(context, Constants.HITCHWIKI_MAPS_COUNTRIES_LIST_FILE_NAME);

        return parseGetCountriesWithCoordinates(placesContainerAsString);
    }

    static String TAG = "utils";

    public static PlaceInfoBasic[] loadHitchwikiSpotsFromLocalFile(Context context) throws Exception {
        //get markersStorageFile streamed into String, so gson can convert it into placesContainer
        String placesContainerAsString = loadFileFromLocalStorage(context, Constants.HITCHWIKI_MAPS_MARKERS_LIST_FILE_NAME);

        Crashlytics.log(Log.INFO, TAG, "Calling ApiManager getPlacesByContinenFromLocalFile");
        Crashlytics.setString("placesContainerAsString", placesContainerAsString);

        if (!placesContainerAsString.isEmpty())
            return new ApiManager().getPlacesByContinenFromLocalFile(placesContainerAsString);

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

    /**
     * Get a name for a file exported on the given date and timezone. The format of this date will follow the format specified by Constants.EXPORT_CSV_FILENAME_FORMAT.
     *
     * @param date The datetime when the file is/was generated.
     * @param zone The desired timezone to name the file. It is recommended to use DateTimeZone.getDefault() when exporting a file for the user, so he gets it named on his local timezone.
     **/
    public static String getNewExportFileName(DateTime date, DateTimeZone zone) {
        String dt = DateTimeFormat.forPattern(Constants.EXPORT_CSV_FILENAME_FORMAT).withZone(zone).print(date.toInstant());
        return dt + Constants.INTERNAL_DB_FILE_NAME + Constants.EXPORT_DB_AS_CSV_FILE_EXTENSION;
    }

    /**
     * Extract the datetime from the name of a file exported before version 27. An exception will be thrown if fileName doesn't follow the format specified by Constants.OLD_EXPORT_CSV_FILENAME_FORMAT.
     *
     * @param fileName The name of the file with a datetime of when it was generated. e.g. '2019_01_01_1200-my-hitchhiking-spots.csv'
     * @param zone     The default timezone on the device when the file name has been generated. E.g. if zone corresponds to Sao Paulo, then we'd get '2019_01_01 12:00-2:00' because -2 was the local offset (BRST) from UTC on that date.
     * @return A datetime instance in the specified zone.
     **/
    protected static DateTime extractDateTimeFromOldFileName(String fileName, DateTimeZone zone) {
        //Number of chars in the name of files that have been exported before app version 27. Refer to: Constants.OLD_EXPORT_CSV_FILENAME_FORMAT
        int numOfCharsInFileNames = 16; //"yyyy_MM_dd_HHmm-".length();
        String formattedDateTime = fileName.substring(0, numOfCharsInFileNames);
        return DateTimeFormat.forPattern(Constants.OLD_EXPORT_CSV_FILENAME_FORMAT).withZone(zone).parseDateTime(formattedDateTime);
    }

    /**
     * Extract the datetime from the name of a file exported after version 27. An exception will be thrown if fileName doesn't follow the format specified by Constants.EXPORT_CSV_FILENAME_FORMAT.
     *
     * @param fileName The name of the file with a datetime of when it was generated. e.g. '2019_01_01_1200-0000#my-hitchhiking-spots.csv'
     * @param zone     The timezone in which you want the extracted datetime.
     * @return A datetime instance in the specified zone.
     **/
    protected static DateTime extractDateTimeFromNewFileName(String fileName, DateTimeZone zone) {
        //Number of chars in the name of files that have been exported after app version 27. Refer to: Constants.EXPORT_CSV_FILENAME_FORMAT
        int numOfCharsInNewFileNames = 21; //"yyyy_MM_dd_HHmm-0000#".length();
        String formattedDateTime = fileName.substring(0, numOfCharsInNewFileNames);

        return DateTimeFormat.forPattern(Constants.EXPORT_CSV_FILENAME_FORMAT).withZone(zone).parseDateTime(formattedDateTime);
    }

    /**
     * Extract the datetime from a file name.
     * An exception will be thrown if fileName doesn't follow any of the two formats
     * Constants.EXPORT_CSV_FILENAME_FORMAT or Constants.OLD_EXPORT_CSV_FILENAME_FORMAT.
     *
     * @param fileName The name of the file with a datetime of when it was generated.
     *                 e.g. '2019_01_01_1200-my-hitchhiking-spots.csv',
     *                 '2019_01_01_1200-0000#my-hitchhiking-spots.csv',
     *                 '2019_01_01_1200-0000#anything-else.csv'.
     * @param zone     The timezone of the device when the file has been generated.
     * @return A datetime instance in the specified zone.
     **/
    public static DateTime extractDateTimeFromFileName(String fileName, DateTimeZone zone) throws IllegalArgumentException {
        try {
            return extractDateTimeFromNewFileName(fileName, zone);
        } catch (Exception ex) {
        }

        try {
            return extractDateTimeFromOldFileName(fileName, zone);
        } catch (Exception ex) {
        }

        throw new IllegalArgumentException("No datetime could be extracted from the given file name (" + fileName + ")",
                new Throwable("The specified name of file doesn't seem to have a datetime in any acceptable format. " +
                        "An acceptable file name would start with: '" +
                        Constants.EXPORT_CSV_FILENAME_FORMAT + "' or '" + Constants.OLD_EXPORT_CSV_FILENAME_FORMAT + "'."));
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

    public static String loadFileFromLocalStorage(Context context, String fileName) throws Exception {
        Crashlytics.setString("Name of the file to load", fileName);
        File markersStorageFolder = null;
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            markersStorageFolder = context.getExternalFilesDir(Constants.HITCHWIKI_MAPS_STORAGE_PATH);
        //else
        //    markersStorageFolder = new File(Constants.SDCARD_STORAGE_PATH + Constants.HITCHWIKI_MAPS_STORAGE_PATH);

        File fl = new File(markersStorageFolder, fileName);
        FileInputStream fin = new FileInputStream(fl);
        String result = Utils.convertStreamToString(fin);
        fin.close();

        return result;
    }

    public static String getMHSExternalFilePath(Context context, String path) {
        return context.getExternalFilesDir(null) + Constants.SDCARD_STORAGE_PATH + path;
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
        return dateTimeToString(dt, getDateTimeFormat(dt, ", "));
    }

    @NonNull
    public static String dateTimeToString(DateTime dt, String dateFormat) {
        try {
            if (dt != null)
                return DateTimeFormat.forPattern(dateFormat).print(dt.toInstant());
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }

        return "";
    }

    public static String getDateTimeFormat(DateTime dt, String separator) {
        if (dt != null) {
            String dateFormat = "dd";

            //Check whether displaying the year would be useful.
            // If so, month should be number, if not, number should be such as "jan.", "ago.", etc
            if (dt.getYearOfEra() != DateTime.now(DateTimeZone.UTC).getYearOfEra())
                dateFormat += "/MM/yyyy";
            else
                dateFormat += "/MMM";

            dateFormat += "'" + separator + "'HH:mm";
            return dateFormat;
        }
        return "";
    }

    /**
     * Force the timezone of a DateTime instance to UTC timezone. No other data is changed or converted.
     * E.g. If localDateTime is '1970-01-01 10:00 BRL', then the return will be '1970-01-01 10:00 UTC'.
     **/
    public static DateTime forceTimeZoneToUTC(DateTime localDateTime) {
        return new DateTime(localDateTime.getYearOfEra(),
                localDateTime.getMonthOfYear(),
                localDateTime.getDayOfMonth(),
                localDateTime.getHourOfDay(),
                localDateTime.getMinuteOfHour(),
                DateTimeZone.UTC
        );
    }

    public static boolean sameDate(DateTime d1, DateTime d2) {
        return sameDate(d1.toDate(), d2.toDate());
    }

    public static boolean sameDate(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal1.setTime(d1);
        Calendar cal2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal2.setTime(d2);

        return CalendarPickerView.sameDate(cal1, cal2);
    }

    public static boolean containsDate(List<Calendar> selectedCals, Calendar cal) {
        return CalendarPickerView.containsDate(selectedCals, cal);
    }

    public static boolean containsDate(List<Date> selectedDates, Date date) {
        for (Date selectedDate : selectedDates) {
            if (sameDate(date, selectedDate)) {
                return true;
            }
        }
        return false;
    }

    public static Calendar getCalendarAtMidnight(DateTime date) {
        return getCalendarAtMidnight(date.toDate());
    }

    public static Calendar getCalendarAtMidnight(Date date) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        // Remove hour, minute, seconds and milliseconds data from the date
        setMidnight(cal);
        return cal;
    }

    /**
     * Clears out the hours/minutes/seconds/millis of a Calendar.
     */
    public static void setMidnight(Calendar cal) {
        cal.set(HOUR_OF_DAY, 0);
        cal.set(MINUTE, 0);
        cal.set(SECOND, 0);
        cal.set(MILLISECOND, 0);
    }

    /**
     * If the local date and time at this very moment is '1970-01-01 10:00 BRL',
     * then the return will be '1970-01-01 10:00 UTC'.
     **/
    public static DateTime getLocalDateTimeNowAsUTC() {
        return forceTimeZoneToUTC(DateTime.now());
    }

    public static DateTime fixDateTime(Long millis) {
        DateTime dtInLocalTime = new DateTime(millis);
        return Utils.forceTimeZoneToUTC(dtInLocalTime);
    }

    public static void parseDBSpotList(@NonNull List<Spot> spotList, @NonNull List<Route> routes, @NonNull List<Spot> singleSpots) {
        Route route = new Route();
        route.spots = new ArrayList<>();
        route.subRoutes = new ArrayList<>();
        SubRoute subRoute = new SubRoute();
        subRoute.points = new ArrayList<>();

        for (int i = 0; i < spotList.size(); i++) {
            Spot spot = spotList.get(i);

            boolean isPartOfARoute = spot.getIsPartOfARoute() != null && spot.getIsPartOfARoute();
            boolean isHitchhikingSpot = spot.getIsHitchhikingSpot() != null && spot.getIsHitchhikingSpot();
            boolean isGotARide = isHitchhikingSpot && (spot.getAttemptResult() != null && spot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE);
            boolean isNotHitchhikedFromHere = spot.getIsNotHitchhikedFromHere() != null && spot.getIsNotHitchhikedFromHere();
            boolean isDestination = spot.getIsDestination() != null && spot.getIsDestination();
            boolean isLastSpotOfTheList = i == spotList.size() - 1;
            boolean isLastSpotOfARoute = isDestination || isLastSpotOfTheList;

            if (!isPartOfARoute)
                singleSpots.add(spot);
            else {
                route.spots.add(spot);

                if (subRoute.points.isEmpty())
                    subRoute.isHitchhikingRoute = isGotARide;

                subRoute.points.add(Point.fromLngLat(spot.getLongitude(), spot.getLatitude()));

                boolean isNextSpotGotARide = false;
                boolean isNextSpotSameSubRoute = false;
                boolean isNextSpotADestination = false;
                boolean isNextSpotPartOfARoute = false;

                if (!isLastSpotOfTheList) {
                    Spot nextSpot = spotList.get(i + 1);
                    boolean isNextSpotHitchhikingSpot = nextSpot.getIsHitchhikingSpot() != null && nextSpot.getIsHitchhikingSpot();
                    boolean isNextSpotNotHitchhikedFromHere = nextSpot.getIsNotHitchhikedFromHere() == null ? false : nextSpot.getIsNotHitchhikedFromHere();
                    isNextSpotGotARide = isNextSpotHitchhikingSpot && (nextSpot.getAttemptResult() != null && nextSpot.getAttemptResult() == Constants.ATTEMPT_RESULT_GOT_A_RIDE);
                    isNextSpotSameSubRoute = (!isNextSpotHitchhikingSpot && !isNextSpotNotHitchhikedFromHere) || subRoute.isHitchhikingRoute == isNextSpotGotARide;
                    isNextSpotADestination = nextSpot.getIsDestination() == null ? false : nextSpot.getIsDestination();
                    isNextSpotPartOfARoute = nextSpot.getIsPartOfARoute() == null ? false : nextSpot.getIsPartOfARoute();
                    isLastSpotOfARoute |= isNextSpotPartOfARoute != isPartOfARoute;
                    //Copy next spot so that we know which was the last spot of this subRoute
                    if (!isNextSpotSameSubRoute && !isNextSpotADestination && !isLastSpotOfARoute)
                        subRoute.points.add(Point.fromLngLat(nextSpot.getLongitude(), nextSpot.getLatitude()));
                }

                boolean isSameSubRoute = !isHitchhikingSpot || subRoute.isHitchhikingRoute == isGotARide;

                //If this spot isn't part of the sub route being built, then add the sub route being built to the list of sub routes and start a new sub route
                if (!isSameSubRoute || (!isNextSpotSameSubRoute && !isNextSpotADestination) || isLastSpotOfARoute)
                    route.subRoutes.add(subRoute);

                //It's the last spot on the route, then add this route to the list and start a new route
                if (isLastSpotOfARoute) {
                    routes.add(route);

                    route = new Route();
                    route.spots = new ArrayList<>();
                    route.subRoutes = new ArrayList<>();
                }

                if (isDestination || (!isNextSpotSameSubRoute && !isNextSpotADestination)) {
                    subRoute = new SubRoute();
                    subRoute.points = new ArrayList<>();
                }
            }
        }
    }

    public static ArrayList<String> getPendingPermissions( Activity activity, String[] permissions){
        ArrayList<String> permissionsToExplain = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                permissionsToExplain.add(permission);
            }
        }
        return permissionsToExplain;
    }
}
