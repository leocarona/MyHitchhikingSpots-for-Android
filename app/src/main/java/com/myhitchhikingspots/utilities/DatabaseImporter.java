package com.myhitchhikingspots.utilities;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

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
import com.myhitchhikingspots.ToolsActivity;
import com.myhitchhikingspots.interfaces.AsyncTaskListener;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.SpotDao;

import org.greenrobot.greendao.database.Database;
import org.joda.time.DateTime;

public class DatabaseImporter extends AsyncTask<Void, Void, String> {
    private Context context;
    private File file = null;
    private ProgressDialog dialog;
    private AsyncTaskListener<ArrayList<String>> onFinished;
    private final String TAG = "database-importer";
    private final String TEMPORARY_COPY_DB_FILE_NAME = "temporary_db_copy.db";
    private Integer numberOfSpotsOnCSVFile = 0, numberOfSpotsSkipped = 0, numberOfSpotsFailedImporting = 0, numberOfSpotsImported = 0;
    private boolean shouldFixDateTime = false;

    //%1$s : table name
    //%2$ : column names sequence, on this format: "STREET, COUNTRY, NOTE"
    //%3$s : the values, in the same sequence as the column names on %2$s
    private String sqlInsertStatement = "INSERT INTO %1$s (%2$s) VALUES (%3$s)";
    private String sqlSelectAllStatement = "SELECT * FROM %1$s";
    private String sqlSelectDuplicatedStatement = "SELECT * FROM %1$s WHERE %2$s LIMIT 1";


    public DatabaseImporter(Context context, File file, boolean shouldFixDateTime) {
        this.context = context;
        this.file = file;
        this.shouldFixDateTime = shouldFixDateTime;
    }

    @Override
    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        dialog.setTitle(context.getString(R.string.general_loading_dialog_message));
        dialog.setMessage(context.getString(R.string.settings_import_happening_message));
        dialog.setCancelable(false);
        dialog.setIcon(android.R.drawable.ic_dialog_info);
        dialog.show();
    }

    @Override
    protected String doInBackground(Void... params) {
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
            onFinished.notifyTaskFinished(errorMessage.isEmpty(), msgRes);
    }

    public void addListener(AsyncTaskListener<ArrayList<String>> doWhenFinished) {
        onFinished = doWhenFinished;
    }

    private String importCSVFile() {
        String errorMessage = "";
        try {
            ArrayList<String> columnsNameList = new ArrayList<>();
            ArrayList<String> valuesList = new ArrayList();
            ArrayList<String> comparisonsList = new ArrayList<>();

            CSVReader reader = new CSVReader(new FileReader(file));

            // Read header (first row)
            String[] csv_header_allvalues = reader.readNext();

            Boolean doesIsPartOfARouteColumnExist = false;
            Boolean doesIsHitchhikingSpotColumnExist = false;
            Integer isDestinationColumnIndex = -1;
            Integer startDateTimeColumnIndex = -1;

            // Ignore first column (ID column)
            for (int i = 1; i < csv_header_allvalues.length; i++) {
                columnsNameList.add(csv_header_allvalues[i]);

                if (csv_header_allvalues[i].equals(SpotDao.Properties.IsPartOfARoute.columnName))
                    doesIsPartOfARouteColumnExist = true;
                else if (csv_header_allvalues[i].equals(SpotDao.Properties.IsHitchhikingSpot.columnName))
                    doesIsHitchhikingSpotColumnExist = true;
                else if (csv_header_allvalues[i].equals(SpotDao.Properties.IsDestination.columnName))
                    isDestinationColumnIndex = i;
                else if (csv_header_allvalues[i].equals(SpotDao.Properties.StartDateTime.columnName))
                    startDateTimeColumnIndex = i;
            }

            //Add a column for missing columns
            if (!doesIsPartOfARouteColumnExist)
                columnsNameList.add(SpotDao.Properties.IsPartOfARoute.columnName);
            if (!doesIsHitchhikingSpotColumnExist)
                columnsNameList.add(SpotDao.Properties.IsHitchhikingSpot.columnName);


            Database destinationDB = DaoMaster.newDevSession(context, Constants.INTERNAL_DB_FILE_NAME).getDatabase();

            // Read all the rest of the lines
            for (; ; ) {
                String[] csv_row_allvalues = reader.readNext();
                if (null == csv_row_allvalues)
                    break;

                try {
                    Boolean isDestination = false;

                    // Read all columns, ignore first column (ID column)
                    for (int i = 1; i < csv_row_allvalues.length; i++) {
                        String rawValue = csv_row_allvalues[i];

                        //If rawValue is null or empty, we should consider equivalent value any null or empty value
                        if (rawValue == null || rawValue.isEmpty() || rawValue.equalsIgnoreCase("null")) {
                            comparisonsList.add("(" + csv_header_allvalues[i] + "='null' or " +
                                    csv_header_allvalues[i] + "='' or " +
                                    csv_header_allvalues[i] + " IS NULL)");

                            //Value will be copied into the local database as empty value
                            valuesList.add("''");
                        } else {
                            String value = DatabaseUtils.sqlEscapeString(rawValue);
                            String comparisonStr = csv_header_allvalues[i] + "=" + value;

                            if (startDateTimeColumnIndex == i && shouldFixDateTime) {
                                long startDateTimeInMillis = Long.valueOf(rawValue);
                                DateTime fixedDateTime = Utils.fixDateTime(startDateTimeInMillis);
                                value = DatabaseUtils.sqlEscapeString(String.valueOf(fixedDateTime.getMillis()));
                                comparisonStr += " OR " + csv_header_allvalues[i] + "=" + value;
                            }

                            //Notice: we must use csv_header_allvalues[i] instead of columnsNameList[i] here because i starts from 1 to skip ID column
                            comparisonsList.add(comparisonStr);
                            valuesList.add(value);
                        }

                        if (isDestinationColumnIndex == i && rawValue.equals("1"))
                            isDestination = true;
                    }

                    //Set default values for IsPartOfARoute
                    if (!doesIsPartOfARouteColumnExist)
                        valuesList.add(Constants.ISPARTOFAROUTE_DEFAULT_VALUE);

                    //Set default values for IsHitchhikingSpots
                    if (!doesIsHitchhikingSpotColumnExist) {
                        //If the spot is not a destination, then set IsHitchhikingSpot to default value
                        if (!isDestination)
                            valuesList.add(Constants.ISHITCHHIKINGSPOT_DEFAULT_VALUE);
                        else
                            valuesList.add("0");
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

                //Clear lists
                valuesList.clear();
                comparisonsList.clear();

                //Sum spot
                numberOfSpotsOnCSVFile++;
            }

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
        String errorMessage = "";
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
                    ArrayList<String> columnsNameList = new ArrayList<>();
                    ArrayList<String> valuesList = new ArrayList();
                    ArrayList<String> comparisonsList = new ArrayList<>();

                    Crashlytics.log(Log.INFO, TAG, "Will start copying all spots to local database");

                    // Read header
                    String[] csv_header_allvalues = cursor.getColumnNames();

                    Boolean doesIsPartOfARouteColumnExist = false;
                    Boolean doesIsHitchhikingSpotColumnExist = false;
                    Integer isDestinationColumnIndex = -1;
                    Integer startDateTimeColumnIndex = -1;

                    // Ignore first column (ID column)
                    for (int i = 1; i < csv_header_allvalues.length; i++) {
                        columnsNameList.add(csv_header_allvalues[i]);

                        if (csv_header_allvalues[i].equals(SpotDao.Properties.IsPartOfARoute.columnName))
                            doesIsPartOfARouteColumnExist = true;
                        else if (csv_header_allvalues[i].equals(SpotDao.Properties.IsHitchhikingSpot.columnName))
                            doesIsHitchhikingSpotColumnExist = true;
                        else if (csv_header_allvalues[i].equals(SpotDao.Properties.IsDestination.columnName))
                            isDestinationColumnIndex = i;
                        else if (csv_header_allvalues[i].equals(SpotDao.Properties.StartDateTime.columnName))
                            startDateTimeColumnIndex = i;
                    }

                    //Add a column for missing columns
                    if (!doesIsPartOfARouteColumnExist)
                        columnsNameList.add(SpotDao.Properties.IsPartOfARoute.columnName);
                    if (!doesIsHitchhikingSpotColumnExist)
                        columnsNameList.add(SpotDao.Properties.IsHitchhikingSpot.columnName);


                    Database destinationDB = DaoMaster.newDevSession(context, Constants.INTERNAL_DB_FILE_NAME).getDatabase();

                    // Read all the rest of the lines
                    while (cursor.moveToNext()) {
                        try {
                            Boolean isDestination = false;

                            // Read all columns, ignore first column (ID column)
                            for (int i = 1; i < cursor.getColumnCount(); i++) {
                                String rawValue = cursor.getString(i);

                                //If rawValue is null or empty, we should consider equivalent value any null or empty value
                                if (rawValue == null || rawValue.isEmpty() || rawValue.equalsIgnoreCase("null")) {
                                    comparisonsList.add("(" + csv_header_allvalues[i] + "='null' or " +
                                            csv_header_allvalues[i] + "='' or " +
                                            csv_header_allvalues[i] + " IS NULL)");

                                    //Value will be copied into the local database as empty value
                                    valuesList.add("''");
                                } else {
                                    String value = DatabaseUtils.sqlEscapeString(rawValue);
                                    String comparisonStr = csv_header_allvalues[i] + "=" + DatabaseUtils.sqlEscapeString(rawValue);

                                    if (startDateTimeColumnIndex == i && shouldFixDateTime) {
                                        long startDateTimeInMillis = Long.valueOf(rawValue);
                                        DateTime fixedDateTime = Utils.fixDateTime(startDateTimeInMillis);
                                        value = DatabaseUtils.sqlEscapeString(String.valueOf(fixedDateTime.getMillis()));
                                        comparisonStr += " OR " + csv_header_allvalues[i] + "=" + value;
                                    }

                                    //Notice: we must use csv_header_allvalues[i] instead of columnsNameList[i] here because i starts from 1 to skip ID column
                                    comparisonsList.add(comparisonStr);
                                    valuesList.add(value);
                                }

                                if (isDestinationColumnIndex == i && rawValue.equals("1"))
                                    isDestination = true;
                            }

                            //Set default values for IsPartOfARoute
                            if (!doesIsPartOfARouteColumnExist)
                                valuesList.add(Constants.ISPARTOFAROUTE_DEFAULT_VALUE);

                            //Set default values for IsHitchhikingSpots
                            if (!doesIsHitchhikingSpotColumnExist) {
                                //If the spot is not a destination, then set IsHitchhikingSpot to default value
                                if (!isDestination)
                                    valuesList.add(Constants.ISHITCHHIKINGSPOT_DEFAULT_VALUE);
                                else
                                    valuesList.add("0");
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

                        //Clear lists
                        valuesList.clear();
                        comparisonsList.clear();

                        //Sum spot
                        numberOfSpotsOnCSVFile++;
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

    Boolean copyIfDoesntExist(ArrayList<String> comparisonsList, ArrayList<String> columnsNameList, ArrayList<String> valuesList, Database destinationDB) {
        Boolean spotAdded = false;

        //Try to find spots that contain exactly the same data as the current one (ignoring ID, of course)
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


}