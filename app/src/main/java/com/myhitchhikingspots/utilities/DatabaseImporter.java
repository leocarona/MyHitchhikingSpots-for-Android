package com.myhitchhikingspots.utilities;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.interfaces.AsyncTaskListener;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.SpotDao;

import org.greenrobot.greendao.database.Database;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

public class DatabaseImporter extends AsyncTask<Void, Void, String> {
    private WeakReference<Context> contextRef;
    private File file = null;
    private ProgressDialog dialog;
    private AsyncTaskListener<ArrayList<String>> onFinished;
    private final String TAG = "database-importer";
    private final String TEMPORARY_COPY_DB_FILE_NAME = "temporary_db_copy.db";
    private Integer numberOfSpotsOnCSVFile = 0, numberOfSpotsSkipped = 0, numberOfSpotsFailedImporting = 0, numberOfSpotsImported = 0;
    private boolean shouldFixDateTime = false;
    private String errorMessage = "";

    //%1$s : table name
    //%2$ : column names sequence, on this format: "STREET, COUNTRY, NOTE"
    //%3$s : the values, in the same sequence as the column names on %2$s
    private String sqlInsertStatement = "INSERT INTO %1$s (%2$s) VALUES (%3$s)";
    private String sqlSelectAllStatement = "SELECT * FROM %1$s";
    private String sqlSelectDuplicatedStatement = "SELECT * FROM %1$s WHERE %2$s LIMIT 1";


    public DatabaseImporter(Context context, File file, boolean shouldFixDateTime) {
        this.contextRef = new WeakReference<>(context);
        this.file = file;
        this.shouldFixDateTime = shouldFixDateTime;
    }

    @Override
    protected void onPreExecute() {
        Context context = contextRef.get();
        if (context == null)
            return;
        dialog = new ProgressDialog(context);
        dialog.setTitle(context.getString(R.string.general_loading_dialog_message));
        dialog.setMessage(context.getString(R.string.settings_import_happening_message));
        dialog.setCancelable(false);
        dialog.setIcon(android.R.drawable.ic_dialog_info);
        dialog.show();
    }

    @Override
    protected String doInBackground(Void... params) {
        Context context = contextRef.get();
        if (context == null || isCancelled())
            return "Context was null. Activity might have been destroyed.";

        Crashlytics.log(Log.INFO, TAG, "DatabaseImporter started executing..");
        Crashlytics.setString("Chosen file", file.toString());

        String errorMessage = "";
        if (file.getName().endsWith(Constants.EXPORT_DB_AS_CSV_FILE_EXTENSION)) {
            errorMessage = importCSVFile();
        } else if (file.getName().endsWith(Constants.INTERNAL_DB_FILE_EXTENSION)) {
            errorMessage = importDBFile();
        } else
            errorMessage = context.getString(R.string.general_selected_file_type_not_supported);

        return errorMessage;
    }

    protected void onPostExecute(String errorMessage) {
        Context context = contextRef.get();
        if (context == null || isCancelled())
            return;
        ArrayList<String> msgRes = new ArrayList<>();

        try {
            if (numberOfSpotsOnCSVFile > 0 || errorMessage.isEmpty())
                msgRes.add(String.format(context.getString(R.string.settings_import_total_spots_on_selected_file), numberOfSpotsOnCSVFile));

            if (numberOfSpotsFailedImporting > 0)
                msgRes.add(String.format(context.getString(R.string.settings_import_total_not_imported), numberOfSpotsFailedImporting));

            if (numberOfSpotsSkipped > 0)
                msgRes.add(String.format(context.getString(R.string.settings_import_total_spots_skipped), numberOfSpotsSkipped));

            if (numberOfSpotsOnCSVFile > 0 || !errorMessage.isEmpty()) {
                if (numberOfSpotsOnCSVFile > 0 && numberOfSpotsOnCSVFile.equals(numberOfSpotsImported))
                    msgRes.add(String.format(context.getString(R.string.settings_import_total_successfuly_imported), context.getString(R.string.general_all)));
                else
                    msgRes.add(String.format(context.getString(R.string.settings_import_total_successfuly_imported), numberOfSpotsImported.toString()));
            }
        } catch (Exception e) {
            Log.e("Error", "Error on importing file");
            errorMessage += String.format(context.getString(R.string.general_error_dialog_message), e.getMessage());
            Crashlytics.logException(e);
        }

        if (!errorMessage.isEmpty()) {
            String errorMessageFormat = "\n%1$s\n%2$s";
            if (msgRes.size() > 0)
                errorMessageFormat = "\n-----\n%1$s\n%2$s";
            msgRes.add(String.format(errorMessageFormat, context.getString(R.string.general_one_or_more_errors_occured), errorMessage));
        }

        if (dialog.isShowing())
            dialog.dismiss();

        if (onFinished != null)
            onFinished.notifyTaskFinished(errorMessage.isEmpty(), msgRes, "");
    }

    public void addListener(AsyncTaskListener<ArrayList<String>> doWhenFinished) {
        onFinished = doWhenFinished;
    }

    private String importCSVFile() {
        Context context = contextRef.get();
        if (context == null)
            return "Context was null. Activity might have been destroyed.";

        try {
            CSVReader reader = new CSVReader(new FileReader(file));

            // Read header (first row)
            String[] csv_header_allvalues = reader.readNext();

            // Read header
            ArrayList<String> columnsNameList = new ArrayList<>(Arrays.asList(csv_header_allvalues));
            int idColumnIndex = columnsNameList.indexOf(SpotDao.Properties.Id.columnName);
            columnsNameList.remove(idColumnIndex);

            //NOTE:
            // For each column that must receive a default value in case such column doesn't exist in the database being imported,
            //then two steps must be followed:
            // Step 1) Add the column name to columnsNameList;
            // Step 2) Before calling copyIfDoesntExist, add the necessary default values to columnsNameList in the same order as column name was added on Step 1.

            Boolean doesIsPartOfARouteColumnExist = columnsNameList.contains(SpotDao.Properties.IsPartOfARoute.columnName);
            Boolean doesIsHitchhikingSpotColumnExist = columnsNameList.contains(SpotDao.Properties.IsHitchhikingSpot.columnName);

            Integer isDestinationColumnIndex = columnsNameList.indexOf(SpotDao.Properties.IsDestination.columnName);
            Integer startDateTimeColumnIndex = columnsNameList.indexOf(SpotDao.Properties.StartDateTime.columnName);

            // Step 1) Add the column name to columnsNameList;
            if (!doesIsPartOfARouteColumnExist)
                columnsNameList.add(SpotDao.Properties.IsPartOfARoute.columnName);
            if (!doesIsHitchhikingSpotColumnExist)
                columnsNameList.add(SpotDao.Properties.IsHitchhikingSpot.columnName);

            ArrayList<Integer> columnIndexesToSkipComparing = getColumnsToSkip(columnsNameList);

            Database destinationDB = DaoMaster.newDevSession(context, Constants.INTERNAL_DB_FILE_NAME).getDatabase();

            // Read all the rest of the lines
            for (; ; ) {
                String[] csv_row_allvalues = reader.readNext();
                if (null == csv_row_allvalues)
                    break;

                ArrayList<String> valuesList = new ArrayList<>(Arrays.asList(csv_row_allvalues));
                valuesList.remove(idColumnIndex);

                Boolean isDestination = valuesList.get(isDestinationColumnIndex).equals("1");

                // Step 2) Before calling copyIfDoesntExist, add the necessary default values to allPropertiesValues.
                ArrayList<String> defaultValues = getDefaultValuesToBeAdded(
                        doesIsPartOfARouteColumnExist,
                        doesIsHitchhikingSpotColumnExist,
                        isDestination);
                valuesList.addAll(defaultValues);

                importRow(destinationDB, context, columnsNameList, valuesList, columnIndexesToSkipComparing, startDateTimeColumnIndex);
            }

            Crashlytics.log(Log.INFO, TAG, "Successfully finished copying all spots to local database");
            destinationDB.close();
            reader.close();
        } catch (Exception e) {
            Log.e("Error", "Error for importing file");
            errorMessage += String.format(context.getString(R.string.general_error_dialog_message), e.getMessage());
            Crashlytics.logException(e);
        }

        return errorMessage;
    }

    private String importDBFile() {
        Context context = contextRef.get();
        if (context == null)
            return "Context was null. Activity might have been destroyed.";

        try {
            //Build path to databases directory
            String destinationFilePath = Utils.getLocalStoragePathToFile(TEMPORARY_COPY_DB_FILE_NAME, context);
            Crashlytics.setString("destinationFilePath", destinationFilePath);

            //Copy database into local storage
            errorMessage = Utils.copySQLiteDBIntoLocalStorage(file, destinationFilePath, context);

            //If no error happened while copying the database
            if (errorMessage.isEmpty()) {
                //Get temporary db as Database object
                Database originDB = DaoMaster.newDevSession(context, TEMPORARY_COPY_DB_FILE_NAME).getDatabase();

                //Try to find spots that contain exactly the same data as the current one (ignoring ID, of course)
                Cursor cursor = originDB.rawQuery(
                        String.format(sqlSelectAllStatement,
                                SpotDao.TABLENAME),
                        null);

                if (cursor != null) {
                    Crashlytics.log(Log.INFO, TAG, "Will start copying all spots to local database");

                    // Read header
                    String[] csv_header_allvalues = cursor.getColumnNames();

                    // Read header
                    ArrayList<String> columnsNameList = new ArrayList<>(Arrays.asList(csv_header_allvalues));
                    int idColumnIndex = columnsNameList.indexOf(SpotDao.Properties.Id.columnName);
                    columnsNameList.remove(idColumnIndex);

                    //NOTE:
                    // For each column that must receive a default value in case such column doesn't exist in the database being imported,
                    //then two steps must be followed:
                    // Step 1) Add the column name to columnsNameList;
                    // Step 2) Before calling copyIfDoesntExist, add the necessary default values to columnsNameList in the same order as column name was added on Step 1.

                    Boolean doesIsPartOfARouteColumnExist = columnsNameList.contains(SpotDao.Properties.IsPartOfARoute.columnName);
                    Boolean doesIsHitchhikingSpotColumnExist = columnsNameList.contains(SpotDao.Properties.IsHitchhikingSpot.columnName);

                    Integer isDestinationColumnIndex = columnsNameList.indexOf(SpotDao.Properties.IsDestination.columnName);
                    Integer startDateTimeColumnIndex = columnsNameList.indexOf(SpotDao.Properties.StartDateTime.columnName);

                    // Step 1) Add the column name to columnsNameList;
                    if (!doesIsPartOfARouteColumnExist)
                        columnsNameList.add(SpotDao.Properties.IsPartOfARoute.columnName);
                    if (!doesIsHitchhikingSpotColumnExist)
                        columnsNameList.add(SpotDao.Properties.IsHitchhikingSpot.columnName);

                    ArrayList<Integer> columnIndexesToSkipComparing = getColumnsToSkip(columnsNameList);

                    Database destinationDB = DaoMaster.newDevSession(context, Constants.INTERNAL_DB_FILE_NAME).getDatabase();

                    // Read all the rest of the lines
                    while (cursor.moveToNext()) {
                        String[] csv_row_allvalues = readRow(cursor);

                        ArrayList<String> valuesList = new ArrayList<>(Arrays.asList(csv_row_allvalues));
                        valuesList.remove(idColumnIndex);

                        Boolean isDestination = valuesList.get(isDestinationColumnIndex).equals("1");

                        // Step 2) Before calling copyIfDoesntExist, add the necessary default values to allPropertiesValues.
                        ArrayList<String> defaultValues = getDefaultValuesToBeAdded(
                                doesIsPartOfARouteColumnExist,
                                doesIsHitchhikingSpotColumnExist,
                                isDestination);
                        valuesList.addAll(defaultValues);

                        importRow(destinationDB, context, columnsNameList, valuesList, columnIndexesToSkipComparing, startDateTimeColumnIndex);
                    }

                    Crashlytics.log(Log.INFO, TAG, "Successfully finished copying all spots to local database");
                    destinationDB.close();
                }
                cursor.close();
                originDB.close();
                if (!context.deleteDatabase(TEMPORARY_COPY_DB_FILE_NAME)) {
                    errorMessage += "For some reason the temporary copy of the database could not be deleted.";
                    Crashlytics.log(Log.WARN, TAG, "For some reason the temporary copy of the database could not be deleted.");
                }
            }
        } catch (Exception e) {
            Log.e("Error", "Error importing file: " + e.getMessage());
            errorMessage += String.format(context.getString(R.string.general_error_dialog_message), e.getMessage());
            Crashlytics.logException(e);
        }

        return errorMessage;
    }


    void importRow(Database destinationDB, Context context, ArrayList<String> columnsNameList, ArrayList<String> valuesList, ArrayList<Integer> columnIndexesToSkipComparing, int startDateTimeColumnIndex) {
        try {
            ArrayList<String> comparisonsList = new ArrayList<>();

            // Read all columns ignoring the ones that should be skipped
            for (int i = 0; i < valuesList.size(); i++) {
                String rawValue = (valuesList.get(i) == null || valuesList.get(i).equalsIgnoreCase("null")) ? "" : valuesList.get(i);
                String propertyEscapedValue = DatabaseUtils.sqlEscapeString(rawValue);

                //Ignore the columns that should be skipped
                if (!columnIndexesToSkipComparing.contains(i)) {
                    String propertyName = columnsNameList.get(i);
                    String comparisonStr = getBasicComparisonString(propertyName, propertyEscapedValue);

                    //If should fix date time, then fix it and update the value of propertyEscapedValue
                    if (!rawValue.isEmpty()) {
                        if (startDateTimeColumnIndex == i && shouldFixDateTime) {
                            propertyEscapedValue = getFixedStartDateTime(Long.valueOf(rawValue));
                            comparisonStr += String.format(" OR %1s = %2$s", propertyName, propertyEscapedValue);
                        } else if (propertyName.equals(SpotDao.Properties.Latitude.columnName) || propertyName.equals(SpotDao.Properties.Longitude.columnName)) {
                            //Latitude and Longitude are values of type Double. When exporting these values, it is possible that these
                            //Double values were converted into type Float. To cover that scenario, we've opted for using the following workaround.
                            //Ex: If the latitude -43.934732423 was converted into -43.934 while exporting,
                            // then our condition here will be "latitude BETWEEN -43.934 AND -44.934".
                            comparisonStr = String.format("%1$s BETWEEN %2$s AND %2$s + 1", propertyName, rawValue);
                        }
                    }

                    //Add comparison that will help identifying whether this spot should be imported
                    comparisonsList.add(comparisonStr);
                }

                //Update the value of the property to be saved if the spot should be imported
                valuesList.set(i, propertyEscapedValue);
            }

            //Copy spot if it doesn't already exist in the local DB. If it exists, skip it so that it doesn't duplicate.
            if (copyIfDoesntExist(comparisonsList, columnsNameList, valuesList, destinationDB))
                numberOfSpotsImported++;
            else
                numberOfSpotsSkipped++;
        } catch (Exception ex) {
            //Sum failure
            numberOfSpotsFailedImporting++;
            errorMessage += "\n" + String.format(context.getString(R.string.general_error_dialog_message), ex.getMessage());
        }

        //Sum spot
        numberOfSpotsOnCSVFile++;
    }

    String[] readRow(Cursor cursor) {
        String[] values = new String[cursor.getColumnCount()];
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String value = cursor.getString(i);
            values[i] = value;
        }
        return values;
    }

    Boolean copyIfDoesntExist(ArrayList<String> comparisonsList, ArrayList<String> columnsNameList, ArrayList<String> valuesList, Database destinationDB) {
        Boolean spotAdded = false;

        //Try to find spots that contain exactly the same data as the current one
        Cursor duplicatedRecords = destinationDB.rawQuery(
                String.format(sqlSelectDuplicatedStatement,
                        SpotDao.TABLENAME,
                        TextUtils.join(" AND ", comparisonsList)),
                null);

        //If no spot already exists with the same data as the current one, add it
        if (duplicatedRecords != null && duplicatedRecords.getCount() == 0) {
            // Save on database
            destinationDB.execSQL(String.format(sqlInsertStatement,
                    SpotDao.TABLENAME,
                    TextUtils.join(", ", columnsNameList),
                    TextUtils.join(", ", valuesList)));

            spotAdded = true;
        }

        return spotAdded;
    }

    private static String getBasicComparisonString(String propertyName, String propertyValue) {
        String comparisonStr = "";

        //If rawValue is null or empty, we should consider equivalent value any null or empty value
        if (propertyValue.isEmpty() || propertyValue.equals("''") || propertyValue.equalsIgnoreCase("'null'"))
            comparisonStr = String.format("(%1$s = 'null' or %1$s = '' or %1$s IS NULL)", propertyName);
        else
            comparisonStr = String.format("%1$s = %2$s", propertyName, propertyValue);

        return comparisonStr;
    }

    private static String getFixedStartDateTime(long startDateTimeInMillis) {
        DateTime fixedDateTime = Utils.fixDateTime(startDateTimeInMillis);
        return DatabaseUtils.sqlEscapeString(String.valueOf(fixedDateTime.getMillis()));
    }

    private static ArrayList<String> getDefaultValuesToBeAdded(boolean doesIsPartOfARouteColumnExist, boolean doesIsHitchhikingSpotColumnExist, boolean isDestination) {
        ArrayList<String> defaultValues = new ArrayList<>();
        //Set default values for IsPartOfARoute
        if (!doesIsPartOfARouteColumnExist)
            defaultValues.add(Constants.ISPARTOFAROUTE_DEFAULT_VALUE);

        //Set default values for IsHitchhikingSpots
        if (!doesIsHitchhikingSpotColumnExist) {
            //If the spot is not a destination, then set IsHitchhikingSpot to default value
            if (!isDestination)
                defaultValues.add(Constants.ISHITCHHIKINGSPOT_DEFAULT_VALUE);
            else
                defaultValues.add("0");
        }
        return defaultValues;
    }

    private static ArrayList<Integer> getColumnsToSkip(ArrayList<String> allPropertiesName) {
        ArrayList<Integer> skipComparingColumns = new ArrayList<>();

        for (int i = 0; i < allPropertiesName.size(); i++) {
            String propertyName = allPropertiesName.get(i);

            //We don't want to compare Ids, AuthorUserNames and none of the variables related to the spot's geolocation.
            if (propertyName.equals(SpotDao.Properties.Id.columnName) ||
                    propertyName.equals(SpotDao.Properties.Street.columnName) ||
                    propertyName.equals(SpotDao.Properties.City.columnName) ||
                    propertyName.equals(SpotDao.Properties.State.columnName) ||
                    propertyName.equals(SpotDao.Properties.Country.columnName) ||
                    propertyName.equals(SpotDao.Properties.CountryCode.columnName) ||
                    propertyName.equals(SpotDao.Properties.GpsResolved.columnName) ||
                    propertyName.equals(SpotDao.Properties.IsReverseGeocoded.columnName) ||
                    propertyName.equals(SpotDao.Properties.Zip.columnName) ||
                    propertyName.equals(SpotDao.Properties.AuthorUserName.columnName)) {
                //Value will be copied into the local database as empty value
                skipComparingColumns.add(i);
            }
        }
        return skipComparingColumns;
    }
}