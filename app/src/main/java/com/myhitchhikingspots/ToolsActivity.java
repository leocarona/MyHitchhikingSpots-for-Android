package com.myhitchhikingspots;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.TextViewCompat;
import androidx.lifecycle.ViewModelProvider;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.myhitchhikingspots.interfaces.AsyncTaskListener;
import com.myhitchhikingspots.model.SpotDao;
import com.myhitchhikingspots.utilities.DatabaseExporter;
import com.myhitchhikingspots.utilities.DatabaseImporter;
import com.myhitchhikingspots.utilities.FilePickerDialog;
import com.myhitchhikingspots.utilities.Utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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
import java.util.concurrent.TimeUnit;


public class ToolsActivity extends AppCompatActivity {
    TextView mfeedbacklabel;
    View coordinatorLayout;

    SharedPreferences prefs;

    // Storage Permissions variables
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 1;

    final static int PICK_DB_REQUEST = 1;
    final static String DBBackupSubdirectory = "/backup";
    final static String TAG = "settings-activity";

    /**
     * Path to the exported file on the local storage.
     * Please note that at the moment when this variable is set we know that the file exists because it has been just generated.
     * Therefore we should avoid using this path in the future unless we guarantee that we check if it still exists before using it for anything (like for sharing).
     **/
    public String destinationFilePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tools_layout);

        //prefs
        prefs = getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        mfeedbacklabel = (TextView) findViewById(R.id.feedbacklabel);
        mfeedbacklabel.setVisibility(View.GONE);

        //Set the toolbar as the app bar for this activity.
        setSupportActionBar(findViewById(R.id.toolbar));

        String strLastDownload = "";

        Long millisecondsAtNow = System.currentTimeMillis();
        Long millisecondsLastCountriesRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_COUNTRIES_DOWNLOAD, 0);
        if (millisecondsLastCountriesRefresh > 0) {
            String timePast = Utils.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsLastCountriesRefresh), this);
            strLastDownload += "- " + String.format(getString(R.string.hwmaps_last_countriesList_update_message), timePast);
        }

        Long millisecondsLastExport = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_BACKUP, 0);
        if (millisecondsLastExport > 0) {
            if (!strLastDownload.isEmpty())
                strLastDownload += "\n";
            String timePast = Utils.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsLastExport), this);
            strLastDownload += "- " + String.format(getString(R.string.settings_last_export_message), timePast);
        }

        Long millisecondsAtRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD, 0);
        if (millisecondsAtRefresh > 0) {
            if (!strLastDownload.isEmpty())
                strLastDownload += "\n";

            String timePast = Utils.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsAtRefresh), this);
            strLastDownload += "- " + String.format(getString(R.string.hwmaps_last_download_message), timePast);
        }

        if (!strLastDownload.isEmpty()) {
            mfeedbacklabel.setText(strLastDownload);
            mfeedbacklabel.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btnExport).setOnClickListener(this::exportButtonHandler);
        findViewById(R.id.btnImport).setOnClickListener(this::importButtonHandler);

        Spinner spinner = findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.startup_fragment_options, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        // Set current default fragment (use 1 - My Maps if none has been selected yet)
        String defaultFragmentClassName = prefs.getString(Constants.PREFS_DEFAULT_STARTUP_FRAGMENT, "");
        spinner.setSelection(getSelectedSpinnerItemIndexForFragment(defaultFragmentClassName));
        // Set the select listener
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                prefs.edit().putString(Constants.PREFS_DEFAULT_STARTUP_FRAGMENT, getFragmentClassNameForSelectedSpinnerItem(pos)).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        coordinatorLayout = findViewById(R.id.coordinatiorLayout);

        findViewById(R.id.item_description_layout).setOnClickListener((v) -> {
            collapseExpandTextView();
        });

        ((TextView) findViewById(R.id.tools_tips_description))
                .setText(getString(R.string.tools_tips_description, getString(R.string.settings_exportdb_button_label), getString(R.string.settings_importdb_button_label)));
    }

    private static String getFragmentClassNameForSelectedSpinnerItem(int spinnerItemIndex) {
        if (spinnerItemIndex == 0)
            return DashboardFragment.class.getName();
        else if (spinnerItemIndex == 2)
            return HitchwikiMapViewFragment.class.getName();
        else return MyMapsFragment.class.getName();
    }


    private static int getSelectedSpinnerItemIndexForFragment(String fragmentClassName) {
        if (fragmentClassName.equals(DashboardFragment.class.getName()))
            return 0;
        else if (fragmentClassName.equals(HitchwikiMapViewFragment.class.getName()))
            return 2;
        else return 1;
    }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_EXTERNAL_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (actionToPerfomOncePermissionIsGranted == 1)
                exportDBNow();
            else if (actionToPerfomOncePermissionIsGranted == 2)
                showFilePickerDialog();
            actionToPerfomOncePermissionIsGranted = -1;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                    Toast.makeText(this, "Can't read file.",
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

    protected void showErrorAlert(String title, String msg) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.general_ok_option), null)
                .show();
    }

    public void shareButtonHandler(View view) {
        //create the send intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        //start the chooser for sharing
        startActivity(Intent.createChooser(shareIntent, "Insert share chooser title here"));
    }

    // 1 for exporting database and 2 for importing database
    int actionToPerfomOncePermissionIsGranted = -1;

    public void importButtonHandler(View view) {
        if (!isStoragePermissionsGranted(this)) {
            actionToPerfomOncePermissionIsGranted = 2;
            requestStoragePermissions(this);
        } else
            showFilePickerDialog();
    }

    void showFilePickerDialog() {
        FilePickerDialog dialog = new FilePickerDialog();
        //Add listener to be called when task finished
        dialog.addListener(new AsyncTaskListener<File>() {
            @Override
            public void notifyTaskFinished(Boolean success, File file, String filePath) {
                //Start import task
                importPickedFile(file);
            }
        });
        dialog.openDialog(this, this);
    }

    void collapseExpandTextView() {
        TextView tools_tips_description = findViewById(R.id.tools_tips_description);
        ImageView tools_tips_header_img = findViewById(R.id.tools_tips_header_img);
        if (tools_tips_description.getVisibility() == View.GONE) {
            // it's collapsed - expand it
            tools_tips_description.setVisibility(View.VISIBLE);
            tools_tips_header_img.setImageResource(R.drawable.ic_expand_less_black_24dp);
        } else {
            // it's expanded - collapse it
            tools_tips_description.setVisibility(View.GONE);
            tools_tips_header_img.setImageResource(R.drawable.ic_expand_more_black_24dp);
        }

        ObjectAnimator animation = ObjectAnimator.ofInt(tools_tips_description, "maxLines", TextViewCompat.getMaxLines(tools_tips_description));
        animation.setDuration(200).start();
    }

    boolean shouldFixStartDates = false;
    boolean userAlreadyAnswered = false;

    void importPickedFile(File fileToImport) {

        try {
            shouldFixStartDates = shouldFixStartDateTimes(fileToImport.getName());
        } catch (Exception ex) {
            //The file has probably been renamed, so we were not able to extract the datetime when it was generated.
            shouldFixStartDates = false;
        }

        //If startDateTime of the spots should get fixed AND we haven't asked whether the user wants to fix it now or not, then display dialog.
        if (shouldFixStartDates && !userAlreadyAnswered) {
            //Ask user whether they want to get the start datetimes of the spots being imported fixed
            showFixSpotsDatetimeDialog(fileToImport);
            return;
        }

        DatabaseImporter t = new DatabaseImporter(this, fileToImport, shouldFixStartDates);

        //Add listener to be called when task finished
        t.addListener(new AsyncTaskListener<ArrayList<String>>() {
            @Override
            public void notifyTaskFinished(Boolean success, ArrayList<String> messages, String filePath) {
                String title = "";

                //Show toast
                if (success) {
                    title = getString(R.string.general_import_finished_successful_message);

                    prefs.edit().putBoolean(Constants.PREFS_MYSPOTLIST_WAS_CHANGED, true).apply();

                    Toast.makeText(getBaseContext(), title, Toast.LENGTH_SHORT).show();

                    String eventName = "Database import success";
                    if (shouldFixStartDates) {
                        eventName += " - ";
                        if (userAlreadyAnswered)
                            eventName += "StartDateTime have been fixed by user's choice";
                        else
                            eventName += "StartDateTime have been automatically fixed";
                    }
                    //Create a record to track database import
                    Answers.getInstance().logCustom(new CustomEvent(eventName));

                } else {
                    title = getString(R.string.general_import_finished_failed_message);

                    Answers.getInstance().logCustom(new CustomEvent("Database import failed"));
                }

                showErrorAlert(title, TextUtils.join("\n", messages));

                //Reset this flag so that we verify whether we need to ask the user again if he chooses to import a second file
                userAlreadyAnswered = false;
                shouldFixStartDates = false;
            }
        });

        t.execute(); //execute asyncTask to import data into database from selected file.
    }

    /**
     * Ask user if he/she would like us to fix the StartDateTime of all spots being imported.
     **/
    void showFixSpotsDatetimeDialog(File fileToImport) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.settings_automatically_fix_spots_datetime_title))
                .setMessage(getString(R.string.settings_automatically_fix_spots_datetime_message))
                .setPositiveButton(getString(R.string.general_yes_option), (dialog, which) -> {
                    userAlreadyAnswered = true;
                    //if positive answer from user
                    shouldFixStartDates = true;
                    //regardless of user's answer, call this method again
                    importPickedFile(fileToImport);
                })
                .setNegativeButton(getString(R.string.general_no_option), (dialog, which) -> {
                    userAlreadyAnswered = true;
                    //if negative answer from user
                    shouldFixStartDates = false;
                    //regardless of user's answer, call this method again
                    importPickedFile(fileToImport);
                })
                .show();
    }

    void showDatabaseExportedSuccessfullyDialog() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.general_done_message))
                .setMessage(getString(R.string.tools_database_exported_successfully_message))
                .setNeutralButton(getString(R.string.general_ok_option), (dialog, which) -> {
                    showShareExportedDatabaseDialog();
                })
                .show();
    }

    void showShareExportedDatabaseDialog() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.settings_sharedb_button_label))
                .setMessage(getString(R.string.tools_exportdb_share_dialog_message, getString(R.string.general_share_label)))
                .setPositiveButton(getString(R.string.general_share_label), (dialog, which) -> {
                    shareCSV();
                })
                .setNegativeButton(getString(R.string.general_cancel_option), (dialog, which) -> {
                    findViewById(R.id.btnShare).setVisibility(View.VISIBLE);
                })
                .show();
    }

    /**
     * Check whether the StartDateTime of the spots being imported need to be fixed.
     * If we can't determine when the file being imported was generated, an exception will be thrown.
     *
     * @return True if we were able to check that the file being imported was generated before My Hitchhiking Spots version 26.
     * False if we were able to check that it has been exported by version 27 or after - what means that the spots StartDateTime do not need to be fixed.
     **/
    public static boolean shouldFixStartDateTimes(String fileName) throws IllegalArgumentException {
        DateTime version27_releasedOn = DateTime.parse(Constants.APP_VERSION27_WAS_RELEASED_ON_UTCDATETIME);

        //NOTE:
        // Version 27 has automatically fixed all StartDateTime of spots that have been saved before its release.
        // Databases exported before version 27 were named with the format Constants.OLD_EXPORT_CSV_FILENAME_FORMAT.

        // If fileName doesn't follow any naming format (which means it has been renamed and its name doesn't contain a datetime in any expected format), an exception will be thrown.
        DateTime file_exportedOn = Utils.extractDateTimeFromFileName(fileName, DateTimeZone.getDefault());

        //Check if fileName tells us that the database has been exported before version 27.
        return file_exportedOn.isBefore(version27_releasedOn);
    }

    public void exportButtonHandler(View view) {
        if (!isStoragePermissionsGranted(this)) {
            actionToPerfomOncePermissionIsGranted = 1;
            requestStoragePermissions(this);
        } else
            exportDBNow();
    }

    void exportDBNow() {
        try {
            DatabaseExporter t = new DatabaseExporter(this);
            //Add listener to be called when task finished
            t.addListener(new AsyncTaskListener<String>() {
                @Override
                public void notifyTaskFinished(Boolean success, String message, String exportedFilePath) {

                    if (success) {
                        DateTime now = DateTime.now(DateTimeZone.UTC);
                        //Set date of last time an export (backup) was made
                        prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_BACKUP, now.getMillis()).apply();

                        //Show result message returned by DatabaseExporter
                        mfeedbacklabel.setText(message);
                        mfeedbacklabel.setVisibility(View.VISIBLE);

                        //Create a record to track Export Database success
                        Answers.getInstance().logCustom(new CustomEvent("Database export success"));

                        if (!exportedFilePath.equals("")) {
                            destinationFilePath = exportedFilePath;
                            showDatabaseExportedSuccessfullyDialog();
                        }
                    } else {
                        showErrorAlert(getString(R.string.general_export_finished_failed_message), message);

                        //Create a record to track Export Database failure
                        Answers.getInstance().logCustom(new CustomEvent("Database export failed"));
                    }
                }
            });

            SpotsListViewModel viewModel = new ViewModelProvider(this).get(SpotsListViewModel.class);
            Cursor curCSV = viewModel.rawQuery(this, "select * from " + SpotDao.TABLENAME, null);

            t.execute(curCSV);

            viewModel.loadWaitingSpot(this);

        } catch (Exception e) {
            Crashlytics.logException(e);
            showErrorAlert(getString(R.string.general_error_dialog_title), String.format(getString(R.string.general_error_dialog_message), e.getMessage()));
        }
    }

    public void shareDB(View v) {
        try {
            shareCSV();
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }
    }

    void shareCSV() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        //intent.putExtra(Intent.EXTRA_TEXT, "body text");
        //intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"email@example.com"});
        File file = new File(destinationFilePath);
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(this, "Attachment Error", Toast.LENGTH_SHORT).show();
            return;
        }

        //Record usage of share option
        Answers.getInstance().logCustom(new CustomEvent("Database shared"));

        Uri uri = getPathUri(file);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, getString(R.string.settings_sharedb_button_label)));
    }

    Uri getPathUri(File file) {
        Uri uri;
        //We have to check if it's nougat or not.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    //importing database
    private void importDB() {
        File sd = Environment.getExternalStorageDirectory();

        if (sd.canWrite()) {
            String backupDBPath = DBBackupSubdirectory + "/" + Constants.INTERNAL_DB_FILE_NAME;
            File backedupDB = new File(sd, backupDBPath);

            CopyChosenFile(backedupDB, getDatabasePath(Constants.INTERNAL_DB_FILE_NAME).getPath());

        } else
            Toast.makeText(this, "Can't write to SD card.",
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

                Toast.makeText(this, "Database imported successfully.",
                        Toast.LENGTH_LONG).show();

                destinationPath = String.format("Database imported to:\n%s", currentDB.toString());
            } else {
                Toast.makeText(this, "No database found to be imported.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Crashlytics.logException(e);

            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    .show();

        }
        mfeedbacklabel.setText(destinationPath);
        mfeedbacklabel.setVisibility(View.VISIBLE);
    }

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
                        String DATE_FORMAT_NOW = Constants.EXPORT_CSV_FILENAME_FORMAT;
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
            Toast.makeText(this, "copied successfully",
                    Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    .show();
        }
    }

    public void copyDataBase() {

        int length;
        byte[] buffer = new byte[1024];
        String databasePath = "/BackupFolder/" + Constants.INTERNAL_DB_FILE_NAME;
        try {
            InputStream databaseInputFile = getAssets().open(Constants.INTERNAL_DB_FILE_NAME + Constants.INTERNAL_DB_FILE_EXTENSION);
            OutputStream databaseOutputFile = new FileOutputStream(databasePath);

            while ((length = databaseInputFile.read(buffer)) > 0) {
                databaseOutputFile.write(buffer, 0, length);
                databaseOutputFile.flush();
            }
            databaseInputFile.close();
            databaseOutputFile.close();

            Toast.makeText(this, "copied successfully",
                    Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
                    .show();
        }

    }

    ProgressDialog progressDialog;

    private void showProgressDialog(String title, String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
        }

        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        //Remove the flag that keeps the screen from sleeping
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

}


