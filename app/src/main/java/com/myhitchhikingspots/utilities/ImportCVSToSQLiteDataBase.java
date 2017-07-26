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
import android.widget.Toast;

import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.model.DaoMaster;
import com.myhitchhikingspots.model.DaoSession;
import com.myhitchhikingspots.model.SpotDao;

import org.greenrobot.greendao.database.Database;

public class ImportCVSToSQLiteDataBase extends AsyncTask<String, String, String> {

    Activity activity;
    Context context;
    File file = null;
    private ProgressDialog dialog;

    public ImportCVSToSQLiteDataBase(Context context, Activity activity, File file) {
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
    protected String doInBackground(String... params) {

        String res = "";
        Log.d(getClass().getName(), file.toString());

        Integer numberOfSpotsOnCSVFile = 0, numberOfSpotsSkipped = 0, numberOfSpotsFailedImporting = 0, numberOfSpotsImported = 0;

        try {
            CSVReader reader = new CSVReader(new FileReader(file));
            String[] nextLine;

            //here I am just displaying the CSV file contents, and you can store your file content into db from while loop...
            //DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, Constants.INTERNAL_DB_FILE_NAME, null);
            DaoSession daoSession = DaoMaster.newDevSession(context, Constants.INTERNAL_DB_FILE_NAME);
            Database db = daoSession.getDatabase();
            SpotDao spotDao = daoSession.getSpotDao();

            //%1$s : table name
            //%2$ : column names sequence, on this format: "STREET, COUNTRY, NOTE"
            //%3$s : the values, in the same sequence as the column names specified in %1$s
            String sqlInsertStatement = "INSERT INTO %1$s (%2$s) VALUES (%3$s)";
            String sqlSelectStatement = "SELECT * FROM %1$s WHERE %2$s";

            ArrayList<String> columnsNameList = new ArrayList<>();
            ArrayList<String> valuesList = new ArrayList();
            ArrayList<String> comparisonsList = new ArrayList<>();

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
                    // Ignore first column (ID column)
                    for (int i = 1; i < csv_row_allvalues.length; i++) {
                        String value = DatabaseUtils.sqlEscapeString(csv_row_allvalues[i]);
                        valuesList.add(value);

                        //Notice: we must use csv_header_allvalues[i] instead of columnsNameList[i] because i starts from 1 to skip ID column
                        comparisonsList.add(csv_header_allvalues[i] + "=" + value);
                    }

                    //Set default values for IsPartOfARoute
                    if (!doesIsPartOfARouteColumnExist)
                        valuesList.add(Constants.ISPARTOFAROUTE_DEFAULT_VALUE);

                    //Set default values for IsHitchhikingSpots
                    if (!doesIsHitchhikingSpotColumnExist) {
                        //If the spot is not a destination, then set IsHitchhikingSpot to default value
                        if (isDestinationColumnIndex > -1 && !csv_row_allvalues[isDestinationColumnIndex].equals("1"))
                            valuesList.add(Constants.ISHITCHHIKINGSPOT_DEFAULT_VALUE);
                        else
                            valuesList.add("0");
                    }

                    //Try to find spots that contain exactly the same data as the current one (ignoring ID, of course)
                    Cursor duplicatedRecords = db.rawQuery(
                            String.format(sqlSelectStatement,
                                    SpotDao.TABLENAME,
                                    TextUtils.join(" AND ", comparisonsList)),
                            null);

                    //If no spot already exists with the same data as the current one, add it
                    if (duplicatedRecords != null && duplicatedRecords.getCount() == 0) {
                        // Save on database
                        db.execSQL(String.format(sqlInsertStatement,
                                SpotDao.TABLENAME,
                                TextUtils.join(", ", columnsNameList),
                                TextUtils.join(", ", valuesList)));

                        numberOfSpotsImported++;
                    } else
                        numberOfSpotsSkipped++;
                } catch (Exception ex) {
                    //Sum failure
                    numberOfSpotsFailedImporting++;
                    res += "\n" + String.format(context.getString(R.string.general_error_dialog_message), ex.getMessage());
                }

                //Clear lists
                valuesList.clear();
                comparisonsList.clear();

                //Sum spot
                numberOfSpotsOnCSVFile++;
            }

        } catch (Exception e) {
            Log.e("Error", "Error for importing file");
            res += String.format(context.getString(R.string.general_error_dialog_message), e.getMessage());
        }

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
            Log.e("Error", "Error for importing file");
            res += String.format(context.getString(R.string.general_error_dialog_message), e.getMessage());
        }

        if (!res.isEmpty())
            msgRes.add("\n-----\n" + context.getString(R.string.general_one_or_more_errors_occured) + "\n" + res);

        return TextUtils.join("\n", msgRes);
    }

    protected void onPostExecute(String data) {

        if (dialog.isShowing()) {
            dialog.dismiss();
        }

        showAlert(context.getString(R.string.settings_importdb_button_label), data);

        if (data.length() != 0) {
            //Toast.makeText(context, "File is built Successfully!" + "\n" + data, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "File fail to build", Toast.LENGTH_SHORT).show();
        }
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