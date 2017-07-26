package com.myhitchhikingspots.utilities;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.SpotDao;

import org.greenrobot.greendao.database.Database;

public class DatabaseImporter extends AsyncTask<Void, Void, String> {

    Activity activity;
    Context context;
    File file = null;
    ProgressDialog dialog;
    final String TAG = "database-importer";
    final String TEMPORARY_COPY_DB_FILE_NAME = "temporary_copy.db";
    Integer numberOfSpotsOnCSVFile = 0, numberOfSpotsSkipped = 0, numberOfSpotsFailedImporting = 0, numberOfSpotsImported = 0;

    public DatabaseImporter(Context context, Activity activity, File file) {
        this.context = context;
        this.activity = activity;
        this.file = file;
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
        if (file.getName().endsWith(".csv")) {
            errorMessage = importCSVFile();
        } else if (file.getName().endsWith(".db")) {
            errorMessage = importDBFile();
        } else
            errorMessage = context.getString(R.string.general_selected_file_type_not_supported);

        return errorMessage;
    }

    protected void onPostExecute(String errorMessage) {
        ArrayList<String> msgRes = new ArrayList<>();

        try {
            msgRes.add(String.format(context.getString(R.string.settings_import_total_spots_on_selected_file), numberOfSpotsOnCSVFile));

            if (numberOfSpotsFailedImporting > 0)
                msgRes.add(String.format(context.getString(R.string.settings_import_total_not_imported), numberOfSpotsFailedImporting));

            if (numberOfSpotsSkipped > 0)
                msgRes.add(String.format(context.getString(R.string.settings_import_total_spots_skipped), numberOfSpotsSkipped));

            if (numberOfSpotsOnCSVFile.equals(numberOfSpotsImported))
                msgRes.add(String.format(context.getString(R.string.settings_import_total_successfuly_imported), context.getString(R.string.general_all)));
            else
                msgRes.add(String.format(context.getString(R.string.settings_import_total_successfuly_imported), numberOfSpotsImported.toString()));
        } catch (Exception e) {
            Log.e("Error", "Error on importing file");
            errorMessage += String.format(context.getString(R.string.general_error_dialog_message), e.getMessage());
            Crashlytics.logException(e);
        }

        if (!errorMessage.isEmpty())
            msgRes.add("\n-----\n" + context.getString(R.string.general_one_or_more_errors_occured) + "\n" + errorMessage);

        if (dialog.isShowing())
            dialog.dismiss();


        showAlert(context.getString(R.string.settings_importdb_button_label), TextUtils.join("\n", msgRes));
    }

    //%1$s : table name
    //%2$ : column names sequence, on this format: "STREET, COUNTRY, NOTE"
    //%3$s : the values, in the same sequence as the column names on %2$s
    String sqlInsertStatement = "INSERT INTO %1$s (%2$s) VALUES (%3$s)";
    String sqlSelectAllStatement = "SELECT * FROM %1$s";
    String sqlSelectDuplicatedStatement = "SELECT * FROM %1$s WHERE %2$s LIMIT 1";

    String importCSVFile() {
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

            // Ignore first column (ID column)
            for (int i = 1; i < csv_header_allvalues.length; i++) {
                columnsNameList.add(csv_header_allvalues[i]);

                if (csv_header_allvalues[i].equals(SpotDao.Properties.IsPartOfARoute.columnName))
                    doesIsPartOfARouteColumnExist = true;
                else if (csv_header_allvalues[i].equals(SpotDao.Properties.IsHitchhikingSpot.columnName))
                    doesIsHitchhikingSpotColumnExist = true;
                else if (csv_header_allvalues[i].equals(SpotDao.Properties.IsDestination.columnName))
                    isDestinationColumnIndex = i;
            }

            //Add a column for missing columns
            if (!doesIsPartOfARouteColumnExist)
                columnsNameList.add(SpotDao.Properties.IsPartOfARoute.columnName);
            if (!doesIsHitchhikingSpotColumnExist)
                columnsNameList.add(SpotDao.Properties.IsHitchhikingSpot.columnName);


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

                            //Notice: we must use csv_header_allvalues[i] instead of columnsNameList[i] here because i starts from 1 to skip ID column
                            comparisonsList.add(csv_header_allvalues[i] + "=" + value);
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
                    if (copyIfDoesntExist(comparisonsList, columnsNameList, valuesList))
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

        } catch (Exception e) {
            Log.e("Error", "Error for importing file");
            errorMessage += String.format(context.getString(R.string.general_error_dialog_message), e.getMessage());
            Crashlytics.logException(e);
        }

        return errorMessage;
    }

    String importDBFile() {
        String errorMessage = "";
        try {
            //Copy database into local storage
            errorMessage = Utils.copySQLiteDBIntoLocalStorage(file, TEMPORARY_COPY_DB_FILE_NAME, context);

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

                    // Ignore first column (ID column)
                    for (int i = 1; i < csv_header_allvalues.length; i++) {
                        columnsNameList.add(csv_header_allvalues[i]);

                        if (csv_header_allvalues[i].equals(SpotDao.Properties.IsPartOfARoute.columnName))
                            doesIsPartOfARouteColumnExist = true;
                        else if (csv_header_allvalues[i].equals(SpotDao.Properties.IsHitchhikingSpot.columnName))
                            doesIsHitchhikingSpotColumnExist = true;
                        else if (csv_header_allvalues[i].equals(SpotDao.Properties.IsDestination.columnName))
                            isDestinationColumnIndex = i;
                    }

                    //Add a column for missing columns
                    if (!doesIsPartOfARouteColumnExist)
                        columnsNameList.add(SpotDao.Properties.IsPartOfARoute.columnName);
                    if (!doesIsHitchhikingSpotColumnExist)
                        columnsNameList.add(SpotDao.Properties.IsHitchhikingSpot.columnName);


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

                                    //Notice: we must use csv_header_allvalues[i] instead of columnsNameList[i] here because i starts from 1 to skip ID column
                                    comparisonsList.add(csv_header_allvalues[i] + "=" + value);
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
                            if (copyIfDoesntExist(comparisonsList, columnsNameList, valuesList))
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
                }

            }
        } catch (Exception e) {
            Log.e("Error", "Error for importing file: " + e.getMessage());
            errorMessage += String.format(context.getString(R.string.general_error_dialog_message), e.getMessage());
            Crashlytics.logException(e);
        }

        return errorMessage;
    }

    Boolean copyIfDoesntExist(ArrayList<String> comparisonsList, ArrayList<String> columnsNameList, ArrayList<String> valuesList) {
        Boolean spotAdded = false;

        Database destinationDB = DaoMaster.newDevSession(context, Constants.INTERNAL_DB_FILE_NAME).getDatabase();

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

    void showAlert(String title, String data) {
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(data)
                .setNegativeButton(context.getString(R.string.general_ok_option), null)
                .show();
    }

}