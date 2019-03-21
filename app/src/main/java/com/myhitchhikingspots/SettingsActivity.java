package com.myhitchhikingspots;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import hitchwikiMapsSDK.classes.APICallCompletionListener;
import hitchwikiMapsSDK.classes.APIConstants;
import hitchwikiMapsSDK.classes.ApiManager;
import hitchwikiMapsSDK.entities.CountryInfoBasic;
import hitchwikiMapsSDK.entities.Error;
import hitchwikiMapsSDK.entities.PlaceInfoBasic;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.gson.Gson;
import com.myhitchhikingspots.interfaces.AsyncTaskListener;
import com.myhitchhikingspots.utilities.DatabaseExporter;
import com.myhitchhikingspots.utilities.DatabaseImporter;
import com.myhitchhikingspots.utilities.FilePickerDialog;
import com.myhitchhikingspots.utilities.PairParcelable;
import com.myhitchhikingspots.utilities.Utils;

import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** @deprecated
 * Classes extending BaseActivity are no longer in use.
 * SettingsActivity was replaced by ToolsActivity. **/
public class SettingsActivity extends BaseActivity {
    TextView mfeedbacklabel;
    CoordinatorLayout coordinatorLayout;

    Context context;
    SharedPreferences prefs;

    public SettingsActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.settings_master_layout);

        //prefs
        prefs = getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        mfeedbacklabel = (TextView) findViewById(R.id.feedbacklabel);
        mfeedbacklabel.setVisibility(View.GONE);

        String strLastDownload = "";

        Long millisecondsAtNow = System.currentTimeMillis();
        Long millisecondsLastCountriesRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_COUNTRIES_DOWNLOAD, 0);
        if (millisecondsLastCountriesRefresh > 0) {
            String timePast = Utils.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsLastCountriesRefresh), getBaseContext());
            strLastDownload += "- " + String.format(getString(R.string.settings_last_countriesList_update_message), timePast);
        }

        Long millisecondsLastExport = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_BACKUP, 0);
        if (millisecondsLastExport > 0) {
            if (!strLastDownload.isEmpty())
                strLastDownload += "\n";
            String timePast = Utils.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsLastExport), getBaseContext());
            strLastDownload += "- " + String.format(getString(R.string.settings_last_export_message), timePast);
        }

        Long millisecondsAtRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD, 0);
        if (millisecondsAtRefresh > 0) {
            if (!strLastDownload.isEmpty())
                strLastDownload += "\n";

            String timePast = Utils.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsAtRefresh), getBaseContext());
            strLastDownload += "- " + String.format(getString(R.string.settings_last_download_message), timePast);
        }

        if (!strLastDownload.isEmpty()) {
            mfeedbacklabel.setText(strLastDownload);
            mfeedbacklabel.setVisibility(View.VISIBLE);
        }

        hitchwikiStorageFolder = new File(Constants.HITCHWIKI_MAPS_STORAGE_PATH);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        context = this;

        continentsContainer = new PairParcelable[7];
        continentsContainer[0] = new PairParcelable(APIConstants.CODE_CONTINENT_ANTARTICA, getString(R.string.continent_code_antarctica));
        continentsContainer[1] = new PairParcelable(APIConstants.CODE_CONTINENT_AFRICA, getString(R.string.continent_code_africa));
        continentsContainer[2] = new PairParcelable(APIConstants.CODE_CONTINENT_ASIA, getString(R.string.continent_code_asia));
        continentsContainer[3] = new PairParcelable(APIConstants.CODE_CONTINENT_EUROPE, getString(R.string.continent_code_europe));
        continentsContainer[4] = new PairParcelable(APIConstants.CODE_CONTINENT_NORTH_AMERICA, getString(R.string.continent_code_north_america));
        continentsContainer[5] = new PairParcelable(APIConstants.CODE_CONTINENT_SOUTH_AMERICA, getString(R.string.continent_code_south_america));
        continentsContainer[6] = new PairParcelable(APIConstants.CODE_CONTINENT_AUSTRALIA, getString(R.string.continent_code_oceania));

        //Rename old Hitchwiki Maps directory to something more intuitive for the user
        if (prefs.getBoolean(Constants.PREFS_HITCHWIKI_STORAGE_RENAMED, false)) {
            File oldFolder = new File(Constants.HITCHWIKI_MAPS_STORAGE_OLDPATH);
            File newFolder = new File(Constants.HITCHWIKI_MAPS_STORAGE_PATH);
            oldFolder.renameTo(newFolder);
            prefs.edit().putBoolean(Constants.PREFS_HITCHWIKI_STORAGE_RENAMED, true).apply();
        }

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
    }

    // Storage Permissions variables
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 1;

    //persmission method.
    public static boolean isStoragePermissionsGranted(Activity activity) {
        // Check if we have read and write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        // Check if user has granted location permission
        return (writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED);
    }

    public static void requestStoragePermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSIONS_EXTERNAL_STORAGE
        );
    }

   /*  @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //locateUser();
            }
        }
    }*/


    int PICK_DB_REQUEST = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == PICK_DB_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
                try {
                    // Get the URI that points to the selected contact
                    File mFile = new File(getPath(this, data.getData()));
                    CopyChosenFile(mFile, getDatabasePath(Constants.INTERNAL_DB_FILE_NAME).getPath());
                } catch (Exception e) {
                    Crashlytics.logException(e);
                    Toast.makeText(getBaseContext(), "Can't read file.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onPause() {
        Crashlytics.log(Log.INFO, TAG, "onPause was called");
        super.onPause();

        dismissProgressDialog();
    }

    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        /*if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }*/
        String filePath = null;
        Uri _uri = uri;
        Crashlytics.log(Log.DEBUG, "", "URI = " + _uri);
        if (_uri != null && "content".equals(_uri.getScheme())) {
            Cursor cursor = context.getContentResolver().query(_uri, new String[]{android.provider.MediaStore.Files.FileColumns.DATA}, null, null, null);
            cursor.moveToFirst();
            filePath = cursor.getString(0);
            cursor.close();
        } else {
            filePath = _uri.getPath();
        }
        Crashlytics.log(Log.DEBUG, "", "Chosen path = " + filePath);
        return filePath;
    }

    public void downloadHWSpots(String selectedCodes, String dialogType) {
        String places = "";
        switch (dialogType) {
            case DIALOG_TYPE_CONTINENT:
                prefs.edit().putString(Constants.PREFS_SELECTED_CONTINENTS_TO_DOWNLOAD, selectedCodes).apply();
                prefs.edit().remove(Constants.PREFS_SELECTED_COUNTRIES_TO_DOWNLOAD).apply();
                places = "Selected continents: " + prefs.getString(Constants.PREFS_SELECTED_CONTINENTS_TO_DOWNLOAD, "");
                break;
            case DIALOG_TYPE_COUNTRY:
                prefs.edit().putString(Constants.PREFS_SELECTED_COUNTRIES_TO_DOWNLOAD, selectedCodes).apply();
                prefs.edit().remove(Constants.PREFS_SELECTED_CONTINENTS_TO_DOWNLOAD).apply();
                places = "Selected countries: " + prefs.getString(Constants.PREFS_SELECTED_COUNTRIES_TO_DOWNLOAD, "");
                break;
        }

        if (!Utils.isNetworkAvailable(this)) {
            showErrorAlert(getString(R.string.general_offline_mode_label), getString(R.string.general_network_unavailable_message));
        } else
            new downloadPlacesAsyncTask(dialogType).execute();
    }

    public void shareButtonHandler(View view) {
        //create the send intent
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);

        //start the chooser for sharing
        startActivity(Intent.createChooser(shareIntent, "Insert share chooser title here"));
    }

    public void pickFileButtonHandler(View view) {
        if (!isStoragePermissionsGranted(this))
            requestStoragePermissions(this);
        else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_DB_REQUEST);
        }
    }


    public void importButtonHandler(View view) {
        if (!isStoragePermissionsGranted(this))
            requestStoragePermissions(this);
        else {
            //Create a record to track usage of Import Database button
            Answers.getInstance().logCustom(new CustomEvent("Database imported"));

            FilePickerDialog dialog = new FilePickerDialog();
            //Add listener to be called when task finished
            dialog.addListener(new AsyncTaskListener<File>() {
                @Override
                public void notifyTaskFinished(Boolean success, File file) {
                    //Start import task
                    importPickedFile(file);
                }
            });
            dialog.openDialog(this, SettingsActivity.this);
        }
    }

    void importPickedFile(File fileToImport) {
        DatabaseImporter t = new DatabaseImporter(context, fileToImport);

        //Add listener to be called when task finished
        t.addListener(new AsyncTaskListener<ArrayList<String>>() {
            @Override
            public void notifyTaskFinished(Boolean success, ArrayList<String> messages) {
                String title = context.getString(R.string.general_import_finished_successful_message);

                if (!success)
                    title = context.getString(R.string.general_import_finished_failed_message);

                showErrorAlert(title, TextUtils.join("\n", messages));

                //Show toast
                if (success)
                    Toast.makeText(context, title, Toast.LENGTH_SHORT).show();
            }
        });

        t.execute(); //execute asyncTask to import data into database from selected file.
    }

    public void showContinentsDialog(View view) {
        if (!isStoragePermissionsGranted(this))
            requestStoragePermissions(this);
        else {
            if (continentsContainer.length == 0) {
                showErrorAlert(getString(R.string.settings_select_continents_button_label), "The list of continents are not loaded. Try to navigate out of this screen and come back agian.");
            } else {
                new showContinentsDialogAsyncTask().execute();
            }
        }
    }

    public void showCountriesDialog(View view) {
        if (!isStoragePermissionsGranted(this))
            requestStoragePermissions(this);
        else {
            if (countriesContainer == null || countriesContainer.length == 0) {
                Long millisecondsLastCountriesRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_COUNTRIES_DOWNLOAD, 0);
                //If the countries list were previously downloaded (we know that by checking if there's a date set from countries download)
                if (millisecondsLastCountriesRefresh > 0) {
                    //Load the countries list from local storage
                    new getCountriesAsyncTask(false).execute();
                } else {
                    //Ask user if they'd like to download the countries list now
                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getString(R.string.settings_countriesList_is_empty_title))
                            .setMessage(getString(R.string.settings_countriesList_is_empty_message))
                            .setPositiveButton(getResources().getString(R.string.general_download_option), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new getCountriesAsyncTask(true).execute();
                                }
                            })
                            .setNegativeButton(getString(R.string.general_cancel_option), null)
                            .show();
                }
            } else {
                new showCountriesDialogAsyncTask().execute();
            }
        }
    }


    public void exportButtonHandler(View view) {
        if (!isStoragePermissionsGranted(this))
            requestStoragePermissions(this);
        else {
            try {
                //Create a record to track usage of Export Database button
                Answers.getInstance().logCustom(new CustomEvent("Database exported"));

                DatabaseExporter t = new DatabaseExporter(this);
                //Add listener to be called when task finished
                t.addListener(new AsyncTaskListener<String>() {
                    @Override
                    public void notifyTaskFinished(Boolean success, String message) {

                        if (success) {
                            DateTime now = DateTime.now();
                            //Set date of last time an export (backup) was made
                            prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_BACKUP, now.getMillis()).apply();

                            //Show result message returned by DatabaseExporter
                            mfeedbacklabel.setText(message);
                            mfeedbacklabel.setVisibility(View.VISIBLE);

                            //Show toast
                            Toast.makeText(context, context.getString(R.string.general_export_finished_successfull_message), Toast.LENGTH_SHORT).show();
                        } else
                            showErrorAlert(context.getString(R.string.general_export_finished_failed_message), message);
                    }
                });
                t.execute();

                ((MyHitchhikingSpotsApplication) getApplicationContext()).loadDatabase();

            } catch (Exception e) {
                Crashlytics.logException(e);
                showErrorAlert(getString(R.string.general_error_dialog_title), String.format(getString(R.string.general_error_dialog_message), e.getMessage()));
            }
        }
    }


    //importing database
    private void importDB() {
        File sd = Environment.getExternalStorageDirectory();

        if (sd.canWrite()) {
            String backupDBPath = DBBackupSubdirectory + "/" + Constants.INTERNAL_DB_FILE_NAME;
            File backedupDB = new File(sd, backupDBPath);

            CopyChosenFile(backedupDB, getDatabasePath(Constants.INTERNAL_DB_FILE_NAME).getPath());

        } else
            Toast.makeText(getBaseContext(), "Can't write to SD card.",
                    Toast.LENGTH_LONG).show();
    }

    private void CopyChosenFile(File chosenFile, String destinationFilePath) {
        String destinationPath = "";
        try {
            if (chosenFile.exists()) {
                File currentDB = new File(destinationFilePath);

                FileChannel src = new FileInputStream(chosenFile).getChannel();
                FileChannel dst = new FileOutputStream(currentDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();

                Toast.makeText(getBaseContext(), "Database imported successfully.",
                        Toast.LENGTH_LONG).show();

                destinationPath = String.format("Database imported to:\n%s", currentDB.toString());
            } else {
                Toast.makeText(getBaseContext(), "No database found to be imported.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Crashlytics.logException(e);

            Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG)
                    .show();

        }
        mfeedbacklabel.setText(destinationPath);
        mfeedbacklabel.setVisibility(View.VISIBLE);
    }

    final static String DBBackupSubdirectory = "/backup";

    //exporting database
    public String exportDB(Context context) {
        //HERE'S A CODE FOUND LATER THAT CODE BE SIMPLER THAN THIS CURRENT METHOD WE'RE USING: http://www.techrepublic.com/blog/software-engineer/export-sqlite-data-from-your-android-device/

        String currentDBPath = "";
        String destinationPath = "";
        try {
            File sd = Environment.getExternalStorageDirectory();

            if (sd.canWrite()) {
                currentDBPath = context.getDatabasePath(Constants.INTERNAL_DB_FILE_NAME).getPath();

                File backupDir = new File(sd + DBBackupSubdirectory);
                boolean success = false;

                if (backupDir.exists())
                    success = true;
                else
                    success = backupDir.mkdir();

                File currentDB = new File(currentDBPath);

                if (success && currentDB.exists()) {
                    File backupDB = new File(backupDir, Constants.INTERNAL_DB_FILE_NAME);

                    //If a backup file already exists, RENAME it so that the new backup file we're generating now can use its name
                    if (backupDB.exists()) {
                        String DATE_FORMAT_NOW = "yyyy_MM_dd_HHmm-";
                        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
                        String newname = sdf.format(new Date(backupDB.lastModified())) + Constants.INTERNAL_DB_FILE_NAME;
                        backupDB.renameTo(new File(backupDir, newname));
                    }

                    backupDB = new File(backupDir, Constants.INTERNAL_DB_FILE_NAME);

                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();

                    if (backupDB.exists()) {
                        Toast.makeText(context, "Database exported successfully.",
                                Toast.LENGTH_LONG).show();

                        destinationPath = String.format("Database exported to:\n%s", backupDB.toString());
                    } else
                        Toast.makeText(context, "DB wasn't backed up.",
                                Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(context, "No database found to be exported.",
                            Toast.LENGTH_LONG).show();

            } else
                Toast.makeText(context, "Can't write to SD card.",
                        Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Crashlytics.logException(e);

            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG)
                    .show();

        }

        return destinationPath;
    }

    final String TAG = "settings-activity";

    void exportDBToCSV() {
        //Send email with CSV attached. Copied from: http://stackoverflow.com/a/5403357/1094261
        String columnString = "\"PersonName\",\"Gender\",\"Street1\",\"postOffice\",\"Age\"";
        String dataString = "\"";// + currentUser.userName + "\",\"" + currentUser.gender + "\",\"" + currentUser.street1 + "\",\"" + currentUser.postOFfice.toString() + "\",\"" + currentUser.age.toString() + "\"";
        String combinedString = columnString + "\n" + dataString;

        File file = null;
        File root = Environment.getExternalStorageDirectory();
        if (root.canWrite()) {
            File dir = new File(root.getAbsolutePath() + "/PersonData");
            dir.mkdirs();
            file = new File(dir, "Data.csv");
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                out.write(combinedString.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Uri u1 = null;
        u1 = Uri.fromFile(file);

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Person Details");
        sendIntent.putExtra(Intent.EXTRA_STREAM, u1);
        sendIntent.setType("text/html");
        startActivity(sendIntent);
    }

    /**
     * Copies your database from your local assets-folder to the just created
     * empty database in the system folder, from where it can be accessed and
     * handled. This is done by transfering bytestream.
     */
    private void copyDataBase2(String dbname) throws IOException {
        try {
            // Open your local db as the input stream
            InputStream myInput = getAssets().open(dbname);
            // Path to the just created empty db
            String outFileName = getDatabasePath(dbname).getPath();
            // Open the empty db as the output stream
            OutputStream myOutput = new FileOutputStream(outFileName);
            // transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            // Close the streams
            myOutput.flush();
            myOutput.close();
            myInput.close();
            Toast.makeText(getBaseContext(), "copied successfully",
                    Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG)
                    .show();
        }
    }

    public void copyDataBase() {

        int length;
        byte[] buffer = new byte[1024];
        String databasePath = "/BackupFolder/" + Constants.INTERNAL_DB_FILE_NAME;
        try {
            InputStream databaseInputFile = getAssets().open(Constants.INTERNAL_DB_FILE_NAME + ".db");
            OutputStream databaseOutputFile = new FileOutputStream(databasePath);

            while ((length = databaseInputFile.read(buffer)) > 0) {
                databaseOutputFile.write(buffer, 0, length);
                databaseOutputFile.flush();
            }
            databaseInputFile.close();
            databaseOutputFile.close();

            Toast.makeText(getBaseContext(), "copied successfully",
                    Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG)
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG)
                    .show();
        }

    }

    ProgressDialog progressDialog;

    private void showProgressDialog(String title, String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(SettingsActivity.this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
        }

        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        //Remove the flag that keeps the screen from sleeping
        ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

    public class showCountriesDialogAsyncTask extends AsyncTask<Void, Void, PairParcelable[]> {

        @Override
        protected void onPreExecute() {
            showProgressDialog(getString(R.string.general_loading_dialog_title), getString(R.string.general_loading_dialog_message));
        }

        @SuppressWarnings("unchecked")
        @Override
        protected PairParcelable[] doInBackground(Void... params) {
            if (isCancelled()) {
                return new PairParcelable[0];
            }

            PairParcelable[] lst = new PairParcelable[countriesContainer.length];
            for (int i = 0; i < countriesContainer.length; i++) {
                CountryInfoBasic country = countriesContainer[i];

                //Build string to show
                String c2 = country.getName();
                if (country.getPlaces() != null && !country.getPlaces().isEmpty())
                    c2 += " (" + country.getPlaces() + " " + getString(R.string.main_activity_single_spots_list_tab) + ")";

                PairParcelable item = new PairParcelable(country.getIso(), c2);
                lst[i] = item;
            }

            return lst;
        }

        @Override
        protected void onPostExecute(PairParcelable[] result) {
            if (SettingsActivity.this.isFinishing())
                return;
            showSelectionDialog(result, DIALOG_TYPE_COUNTRY);
            dismissProgressDialog();
        }
    }

    public class showContinentsDialogAsyncTask extends AsyncTask<Void, Void, PairParcelable[]> {
        @Override
        protected void onPreExecute() {
            showProgressDialog(getString(R.string.general_loading_dialog_title), getString(R.string.general_loading_dialog_message));
        }

        @SuppressWarnings("unchecked")
        @Override
        protected PairParcelable[] doInBackground(Void... params) {
            if (isCancelled()) {
                return new PairParcelable[0];
            }
            return continentsContainer;
        }

        @Override
        protected void onPostExecute(PairParcelable[] result) {
            if (SettingsActivity.this.isFinishing())
                return;
            dismissProgressDialog();
            showSelectionDialog(result, DIALOG_TYPE_CONTINENT);
        }
    }


    public class getCountriesAsyncTask extends AsyncTask<Void, Void, String> {
        Boolean shouldDeleteExisting;

        public getCountriesAsyncTask(Boolean shouldDeleteExisting) {
            this.shouldDeleteExisting = shouldDeleteExisting;
        }

        @Override
        protected void onPreExecute() {
            String strToShow = "Fetching countries list...";

            if (shouldDeleteExisting)
                strToShow = "Updating countries list...";

            showProgressDialog(getString(R.string.settings_downloadCountriesList_button_label), strToShow);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected String doInBackground(Void... params) {
            if (isCancelled()) {
                return "Canceled";
            }

            //create folder if not already created
            if (!hitchwikiStorageFolder.exists()) {
                //create folder for the first time
                hitchwikiStorageFolder.mkdirs();
                Crashlytics.log(Log.INFO, TAG, "Directory created. " + hitchwikiStorageFolder.getPath());
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
            if (SettingsActivity.this.isFinishing())
                return;

            if (!result.contentEquals("countriesLoadedFromLocalStorage"))
                saveCountriesListLocally(countriesContainer);


            if (result.contentEquals("countriesListDownloaded")) {
                //also write into prefs that markers sync has occurred
                Long currentMillis = System.currentTimeMillis();
                prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_COUNTRIES_DOWNLOAD, currentMillis).apply();

                Toast.makeText(SettingsActivity.this, "Download successful!", Toast.LENGTH_SHORT).show();
                Long millisecondsAtDownload = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_COUNTRIES_DOWNLOAD, 0);
                if (millisecondsAtDownload != 0) {
                    //convert millisecondsAtDownload to some kind of date and time text
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                    Date resultdate = new Date(millisecondsAtDownload);
                    String timeStamp = sdf.format(resultdate);

                    showCrouton(String.format(getString(R.string.general_items_downloaded_message), countriesContainer.length) + " " +
                                    String.format(getString(R.string.general_last_sync_date), timeStamp),
                            Constants.CROUTON_DURATION_5000);
                } else {
                    showCrouton(String.format(getString(R.string.general_items_downloaded_message), countriesContainer.length),
                            Constants.CROUTON_DURATION_5000);
                }
            } else if (!result.contentEquals("countriesLoadedFromLocalStorage") && !result.isEmpty())
                showErrorAlert(getString(R.string.general_error_dialog_title), String.format(getString(R.string.settings_countriesList_download_failed_message), result));

            if (result.contentEquals("countriesListDownloaded") || result.contentEquals("countriesLoadedFromLocalStorage")) {
                if (countriesContainer.length == 0) {
                    //Ask user if they'd like to download the countries list now
                    new AlertDialog.Builder(context)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getString(R.string.settings_countriesList_is_empty_title))
                            .setMessage(getString(R.string.settings_countriesList_is_empty_message))
                            .setPositiveButton(getString(R.string.general_download_option), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new getCountriesAsyncTask(true).execute();
                                }
                            })
                            .setNegativeButton(getResources().getString(R.string.general_cancel_option), null)
                            .show();
                } else
                    new showCountriesDialogAsyncTask().execute();
            }

            dismissProgressDialog();
        }
    }

    //async task to retrieve markers
    public class downloadPlacesAsyncTask extends AsyncTask<Void, Void, String> {
        String lstToDownload = "";
        String typeToDownload = "";

        public downloadPlacesAsyncTask(String type) {
            typeToDownload = type;
            switch (type) {
                case DIALOG_TYPE_CONTINENT:
                    lstToDownload = prefs.getString(Constants.PREFS_SELECTED_CONTINENTS_TO_DOWNLOAD, "");
                    break;
                case DIALOG_TYPE_COUNTRY:
                    lstToDownload = prefs.getString(Constants.PREFS_SELECTED_COUNTRIES_TO_DOWNLOAD, "");
                    break;
            }
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(getString(R.string.settings_downloadHDSpots_button_label), String.format(getString(R.string.general_downloading_something_message), lstToDownload));
        }

        @SuppressWarnings("unchecked")
        @Override
        protected String doInBackground(Void... params) {
            if (isCancelled()) {
                return "Canceled";
            }

            //create folder if not already created
            if (!hitchwikiStorageFolder.exists()) {
                //create folder for the first time
                hitchwikiStorageFolder.mkdirs();
                Crashlytics.log(Log.INFO, TAG, "Directory created. " + hitchwikiStorageFolder.getPath());
            }

            //folder exists, but it may be a case that file with stored markers is missing, so lets check that
            File fileCheck = new File(hitchwikiStorageFolder, Constants.HITCHWIKI_MAPS_MARKERS_LIST_FILE_NAME);
            fileCheck.delete();
            //recreate placesContainer, it might not be empty
            if (placesContainer == null)
                placesContainer = new ArrayList<>();
            else
                placesContainer.clear();

            String res = "nothingToSync";

            if (!lstToDownload.isEmpty()) {
                res = "spotsDownloaded";
                String[] codes = lstToDownload.split(LIST_SEPARATOR);

                switch (typeToDownload) {
                    case DIALOG_TYPE_CONTINENT:

                        for (String continentCode : codes) {
                            try {
                                Crashlytics.log(Log.INFO, TAG, "Calling ApiManager getPlacesByContinent");
                                hitchwikiAPI.getPlacesByContinent(continentCode, getPlacesByArea);
                            } catch (Exception ex) {
                                if (ex.getMessage() != null)
                                    res = ex.getMessage();
                                Crashlytics.logException(ex);
                            }
                        }
                        break;
                    case DIALOG_TYPE_COUNTRY:
                        for (String countryCode : codes) {
                            try {
                                Crashlytics.log(Log.INFO, TAG, "Calling ApiManager getPlacesByCountry");
                                hitchwikiAPI.getPlacesByCountry(countryCode, getPlacesByArea);
                            } catch (Exception ex) {
                                if (ex.getMessage() != null)
                                    res = ex.getMessage();
                                Crashlytics.logException(ex);
                            }
                        }
                        break;
                }
            }

            return res;
        }

        @Override
        protected void onPostExecute(String result) {
            if (SettingsActivity.this.isFinishing())
                return;

            if (result.contentEquals("spotsDownloaded")) {

                savePlacesListLocally(placesContainer);

                //Get current datetime in milliseconds
                Long millisecondsAtRefresh = System.currentTimeMillis();

                //also write into prefs that markers sync has occurred
                prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD, millisecondsAtRefresh).apply();

                //TODO: show how many megabytes were downloaded or saved locally

                Toast.makeText(SettingsActivity.this, getString(R.string.general_download_finished_successffull_message), Toast.LENGTH_SHORT).show();

                //convert millisecondsAtRefresh to some kind of date and time text
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                Date resultdate = new Date(millisecondsAtRefresh);
                String timeStamp = sdf.format(resultdate);

                showCrouton(String.format(getString(R.string.general_items_downloaded_message), placesContainer.size()) +
                                " " + String.format(getString(R.string.general_last_sync_date), timeStamp),
                        Constants.CROUTON_DURATION_5000);

            } else {
                if (result.contentEquals("nothingToSync")) {
                    //also write into prefs that markers sync has occurred
                    prefs.edit().remove(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD).apply();

                    showErrorAlert("Hitchwiki Maps cleared", "All spots previously downloaded from Hitchwiki Maps were deleted from your device. To download spots, select one or more continent.");
                } else if (!result.isEmpty())
                    showErrorAlert(getString(R.string.general_error_dialog_title), String.format(getString(R.string.settings_hitchwikiMapsSpots_download_failed_message), result));
            }

            dismissProgressDialog();
        }
    }


    protected static final String DIALOG_TYPE_CONTINENT = "dialog-continents-dialog_type";
    protected static final String DIALOG_TYPE_COUNTRY = "dialog-countries-dialog_type";
    protected static final String LIST_SEPARATOR = ", ";

    public static class DialogSelection2 extends DialogFragment {
        final String TAG = "dialog-selection";
        PairParcelable[] items = new PairParcelable[0];
        String dialog_type = "";
        boolean argsWereSet = false;
        String selectedCodes = "";

        void setValuesFromBundle(Bundle args) {
            items = (PairParcelable[]) args.getParcelableArray(Constants.DIALOG_STRINGLIST_BUNDLE_KEY);
            dialog_type = args.getString(Constants.DIALOG_TYPE_BUNDLE_KEY);
            argsWereSet = true;
        }

        @Override
        public void setArguments(Bundle args) {
            super.setArguments(args);
            setValuesFromBundle(args);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putParcelableArray(Constants.DIALOG_STRINGLIST_BUNDLE_KEY, items);
            outState.putString(Constants.DIALOG_TYPE_BUNDLE_KEY, dialog_type);
        }

        String[] getItemsValueArray() {
            String[] lst = new String[items.length];
            for (int i = 0; i < lst.length; i++)
                lst[i] = items[i].getValue();
            return lst;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (!argsWereSet && savedInstanceState != null)
                setValuesFromBundle(savedInstanceState);

            SharedPreferences prefs = getContext().getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

            switch (dialog_type) {
                case DIALOG_TYPE_CONTINENT:
                    selectedCodes = prefs.getString(Constants.PREFS_SELECTED_CONTINENTS_TO_DOWNLOAD, "");
                    break;
                case DIALOG_TYPE_COUNTRY:
                    selectedCodes = prefs.getString(Constants.PREFS_SELECTED_COUNTRIES_TO_DOWNLOAD, "");
                    break;
            }

            boolean[] lst = new boolean[items.length];
            for (int i = 0; i < lst.length; i++) {

                lst[i] = selectedCodes.contains(items[i].getKey());
                        /*((dialog_type.equalsIgnoreCase(DIALOG_TYPE_CONTINENT) && selectedCodes.contains(continentsContainer[i].getKey()))
                        || (dialog_type.equalsIgnoreCase(DIALOG_TYPE_COUNTRY) && selectedCodes.contains(countriesContainer[i].getIso())));*/
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            String title = "";
            if (dialog_type == DIALOG_TYPE_CONTINENT)
                title = getString(R.string.settings_select_continents_button_label);
            else if (dialog_type == DIALOG_TYPE_COUNTRY)
                title = getString(R.string.settings_select_countries_button_label);

            builder.setTitle(title)
                    .setMultiChoiceItems(getItemsValueArray(), lst, new DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int item, boolean isChecked) {
                            try {
                                String code = "";
                                if (dialog_type == DIALOG_TYPE_CONTINENT)
                                    code = continentsContainer[item].getKey();
                                else if (dialog_type == DIALOG_TYPE_COUNTRY)
                                    code = countriesContainer[item].getIso();

                                if (!isChecked) {
                                    selectedCodes = selectedCodes.replaceAll(LIST_SEPARATOR + code + LIST_SEPARATOR, "");
                                    selectedCodes = selectedCodes.replaceAll(code + LIST_SEPARATOR, "");
                                    selectedCodes = selectedCodes.replaceAll(LIST_SEPARATOR + code, "");
                                    selectedCodes = selectedCodes.replaceAll(code, "");
                                    //selectedCodes = selectedCodes.replaceAll(LIST_SEPARATOR + LIST_SEPARATOR, LIST_SEPARATOR);
                                } else if (!selectedCodes.contains(code)) {
                                    if (!selectedCodes.isEmpty())
                                        selectedCodes += LIST_SEPARATOR;
                                    selectedCodes += code;
                                }

                                Crashlytics.log(Log.INFO, TAG, "Chosen option: " + code);
                            } catch (Exception ex) {
                                Crashlytics.log(Log.INFO, TAG, "Deu mal: " + ex.getMessage());
                            }
                        }
                    })
                    .setPositiveButton(getString(R.string.general_download_option), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Crashlytics.log(Log.INFO, TAG, "READY to download!");

                            //Create a record to track usage of Download HW spots button
                            Answers.getInstance().logCustom(new CustomEvent("HW spots downloaded")
                                    .putCustomAttribute("Region", (dialog_type == DIALOG_TYPE_CONTINENT) ? "Continent" : "Country"));

                            SettingsActivity context = (SettingsActivity) getActivity();
                            context.downloadHWSpots(selectedCodes, dialog_type);
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.general_cancel_option), null);

            return builder.create();
        }

    }


    public void showSelectionDialog(PairParcelable[] result, String dialogType) {
        Bundle args = new Bundle();
        args.putParcelableArray(Constants.DIALOG_STRINGLIST_BUNDLE_KEY, result);
        args.putString(Constants.DIALOG_TYPE_BUNDLE_KEY, dialogType);

        FragmentManager fragmentManager = getSupportFragmentManager();
        DialogSelection2 dialog = new DialogSelection2();
        dialog.setArguments(args);
        dialog.show(fragmentManager, "tagSelection");
    }

    public File hitchwikiStorageFolder;
    public static List<PlaceInfoBasic> placesContainer = new ArrayList<>();
    public static CountryInfoBasic[] countriesContainer = new CountryInfoBasic[0];
    public static PairParcelable[] continentsContainer = new PairParcelable[0];
    public boolean placesContainerIsEmpty = true;
    public static final ApiManager hitchwikiAPI = new ApiManager();

    APICallCompletionListener<PlaceInfoBasic[]> getPlacesByArea = new APICallCompletionListener<PlaceInfoBasic[]>() {
        @Override
        public void onComplete(boolean success, int intParam, String stringParam, Error error, PlaceInfoBasic[] object) {
            if (success) {
                for (int i = 0; i < object.length; i++) {
                    placesContainer.add(object[i]);
                }

            } else {
                System.out.println("Error message : " + error.getErrorDescription());
            }
        }
    };


    APICallCompletionListener<CountryInfoBasic[]> getCountriesAndCoordinates = new APICallCompletionListener<CountryInfoBasic[]>() {
        @Override
        public void onComplete(boolean success, int k, String s, Error error, CountryInfoBasic[] object) {
            if (success) {
                System.out.println(object.length);
                countriesContainer = object;

            } else {
                System.out.println("Error message : " + error.getErrorDescription());
            }
        }
    };

    //crouton instead of Toast messages, because Croutons are awesome
    private void showCrouton(String croutonText, int duration) {
        Crashlytics.log(Log.DEBUG, TAG, croutonText);
        mfeedbacklabel.setText(croutonText);
        mfeedbacklabel.setVisibility(View.VISIBLE);
 /*final Crouton crouton;
        final int durationOfCrouton = duration;

        Configuration croutonConfiguration = new Configuration.Builder()
                .setDuration(durationOfCrouton)
                .setInAnimation(R.anim.push_right_in)
                .setOutAnimation(R.anim.push_left_out)
                .build();

        crouton = Crouton.makeText(this, croutonText, Style.HITCHWIKI).setConfiguration(croutonConfiguration);

        crouton.show();*/
    }

    void savePlacesListLocally(List<PlaceInfoBasic> places) {
        //in this case, we have full placesContainer, processed to fulfill Clusterkraf model requirements and all,
        //so we have to create file in storage folder and stream placesContainer into it using gson
        File fileToStoreMarkersInto = new File(hitchwikiStorageFolder, Constants.HITCHWIKI_MAPS_MARKERS_LIST_FILE_NAME);

        try {
            FileOutputStream fileOutput = new FileOutputStream(fileToStoreMarkersInto);

            Gson gsonC = new Gson();
            String placesContainerAsString = gsonC.toJson(places);

            InputStream inputStream = new ByteArrayInputStream(placesContainerAsString.getBytes("UTF-8"));

            //create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0; //used to store a temporary size of the buffer

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                //add the data in the buffer to the file in the file output stream (the file on the sd card
                fileOutput.write(buffer, 0, bufferLength);
            }

            //close the output stream when done
            fileOutput.close();

        } catch (Exception exception) {
            Crashlytics.logException(exception);
        }

    }

    void saveCountriesListLocally(CountryInfoBasic[] countriesList) {
        File file = new File(hitchwikiStorageFolder, Constants.HITCHWIKI_MAPS_COUNTRIES_LIST_FILE_NAME);

        try {
            FileOutputStream fileOutput = new FileOutputStream(file);

            Gson gsonC = new Gson();
            String placesContainerAsString = gsonC.toJson(countriesList);

            InputStream inputStream = new ByteArrayInputStream(placesContainerAsString.getBytes("UTF-8"));

            //create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0; //used to store a temporary size of the buffer

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                //add the data in the buffer to the file in the file output stream (the file on the sd card
                fileOutput.write(buffer, 0, bufferLength);
            }

            //close the output stream when done
            fileOutput.close();

        } catch (Exception exception) {
            Crashlytics.logException(exception);
        }
    }

    Snackbar snackbar;

    void showSnackbar(@NonNull CharSequence text, CharSequence action, View.OnClickListener listener) {
        String t = "";
        if (text != null && text.length() > 0)
            t = text.toString();
        snackbar = Snackbar.make(coordinatorLayout, t.toUpperCase(), Snackbar.LENGTH_LONG)
                .setAction(action, listener);

        // get snackbar view
        View snackbarView = snackbar.getView();

        // set action button color
        snackbar.setActionTextColor(Color.BLACK);

        // change snackbar text color
        int snackbarTextId = android.support.design.R.id.snackbar_text;
        TextView textView = (TextView) snackbarView.findViewById(snackbarTextId);
        if (textView != null) textView.setTextColor(Color.WHITE);


        // change snackbar background
        snackbarView.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.ic_regular_spot_color));

        snackbar.show();
    }
}


