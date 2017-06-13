package com.myhitchhikingspots;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.dualquo.te.hitchwiki.classes.APICallCompletionListener;
import com.dualquo.te.hitchwiki.classes.APIConstants;
import com.dualquo.te.hitchwiki.classes.ApiManager;
import com.dualquo.te.hitchwiki.entities.CountryInfoBasic;
import com.dualquo.te.hitchwiki.entities.Error;
import com.dualquo.te.hitchwiki.entities.PlaceInfoBasic;
import com.google.gson.Gson;
import com.myhitchhikingspots.model.SpotDao;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.fabric.sdk.android.services.common.Crash;

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

        //exportDB();
        //importDB();

        //prefs
        prefs = getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        mfeedbacklabel = (TextView) findViewById(R.id.feedbacklabel);
        mfeedbacklabel.setVisibility(View.GONE);

        String strLastDownload = "";

        Long millisecondsAtNow = System.currentTimeMillis();
        Long millisecondsLastExport = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_BACKUP_SYNC, 0);
        if (millisecondsLastExport > 0) {
            strLastDownload += String.format("- Last export was done %s ago.",
                    SpotListAdapter.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsLastExport)));
        }

        Long millisecondsAtRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD, 0);
        if (millisecondsAtRefresh > 0) {
            if (!strLastDownload.isEmpty())
                strLastDownload += "\n";

            strLastDownload += String.format("- Last download was done %s ago.",
                    SpotListAdapter.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsAtRefresh)));
        }

        if (!strLastDownload.isEmpty()) {
            mfeedbacklabel.setText(strLastDownload);
            mfeedbacklabel.setVisibility(View.VISIBLE);
        }

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        context = this;

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
    }

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
                    CopyChosenFile(mFile, getDatabasePath(Constants.dbName).getPath());
                } catch (Exception e) {
                    Crashlytics.logException(e);
                    Toast.makeText(getBaseContext(), "Can't read file.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
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

    public void downloadHWSpotsButtonHandler(View view) {
        new retrievePlacesAsyncTask(true).execute("");
    }

    public void shareButtonHandler(View view) {
        //create the send intent
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);

        //start the chooser for sharing
        startActivity(Intent.createChooser(shareIntent, "Insert share chooser title here"));
    }

    public void pickFileButtonHandler(View view) {
        if (!dbExported) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Database not backed up")//getResources().getString(R.string.spot_form_delete_dialog_message_text)
                    .setMessage("For safety reasons, you must export the current database first before importing a new one.")
                  /*  .setPositiveButton(getResources().getString(R.string.settings_exportdb_button_label), new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            exportDB();

                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("*");
                            startActivityForResult(intent, PICK_DB_REQUEST);
                        }
                    })*/
                    .setNegativeButton("OK", null)
                    .show();
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_DB_REQUEST);
        }
    }


    public void importButtonHandler(View view) {
        if (!dbExported) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Database not backedup")//getResources().getString(R.string.spot_form_delete_dialog_message_text)
                    .setMessage("You must export the current database first before importing a new one.")
                    /*.setPositiveButton(getResources().getString(R.string.settings_exportdb_button_label), new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            exportDB();
                            importDB();
                        }
                    })*/
                    .setNegativeButton("OK", null)
                    .show();
        } else
            importDB();
    }

    boolean dbExported = false;

    public void exportButtonHandler(View view) {
        try {
            new ExportDatabaseCSVTask().execute();

            /*String dbExportResult = exportDB(getBaseContext());
            dbExported = !dbExportResult.isEmpty();
            mfeedbacklabel.setText(dbExportResult);*/

            //copyDataBase2(Constants.dbName);

            ((MyHitchhikingSpotsApplication) getApplicationContext()).loadDatabase();

        } catch (Exception e) {
            Crashlytics.logException(e);
        }
    }

    //importing database
    private void importDB() {
        File sd = Environment.getExternalStorageDirectory();

        if (sd.canWrite()) {
            String backupDBPath = DBBackupSubdirectory + "/" + Constants.dbName;
            File backedupDB = new File(sd, backupDBPath);

            CopyChosenFile(backedupDB, getDatabasePath(Constants.dbName).getPath());

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
                currentDBPath = context.getDatabasePath(Constants.dbName).getPath();

                File backupDir = new File(sd + DBBackupSubdirectory);
                boolean success = false;

                if (backupDir.exists())
                    success = true;
                else
                    success = backupDir.mkdir();

                File currentDB = new File(currentDBPath);

                if (success && currentDB.exists()) {
                    File backupDB = new File(backupDir, Constants.dbName);

                    //If a backup file already exists, RENAME it so that the new backup file we're generating now can use its name
                    if (backupDB.exists()) {
                        String DATE_FORMAT_NOW = "yyyy_MM_dd_HHmm-";
                        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
                        String newname = sdf.format(new Date(backupDB.lastModified())) + Constants.dbName;
                        backupDB.renameTo(new File(backupDir, newname));
                    }

                    backupDB = new File(backupDir, Constants.dbName);

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


    public class ExportDatabaseCSVTask extends AsyncTask<String, Void, Boolean> {
        private final ProgressDialog dialog = new ProgressDialog(SettingsActivity.this);

        @Override
        protected void onPreExecute() {
            ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            this.dialog.setTitle(getString(R.string.settings_exportdb_button_label));
            this.dialog.setMessage("Your spots list will be saved on your phone as CSV file..");
            this.dialog.show();
        }

        protected Boolean doInBackground(final String... args) {
            Crashlytics.log(Log.INFO, TAG, "ExportDatabaseCSVTask started executing..");
            MyHitchhikingSpotsApplication appContext = ((MyHitchhikingSpotsApplication) getApplicationContext());

            File exportDir = new File(Environment.getExternalStorageDirectory(), "/MyHitchhikingSpots/");

            if (!exportDir.exists()) {
                Crashlytics.log(Log.INFO, TAG, "Directory created. " + exportDir.getPath());
                exportDir.mkdirs();
            }

            String DATE_FORMAT_NOW = "yyyy_MM_dd_HHmm-";
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
            String fileName = sdf.format(new Date()) + Constants.dbName + ".csv";

            File file = new File(exportDir, fileName);
            try {
                file.createNewFile();
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                Cursor curCSV = appContext.rawQuery("select * from " + SpotDao.TABLENAME, null);
                csvWrite.writeNext(curCSV.getColumnNames());
                while (curCSV.moveToNext()) {
                    String arrStr[] = null;
                    String[] mySecondStringArray = new String[curCSV.getColumnNames().length];
                    for (int i = 0; i < curCSV.getColumnNames().length; i++) {
                        mySecondStringArray[i] = curCSV.getString(i);
                    }
                    csvWrite.writeNext(mySecondStringArray);
                }
                csvWrite.close();
                curCSV.close();

                result = "Copied to:\n" + file.getPath();
                Crashlytics.log(Log.DEBUG, TAG, result);

                return true;
            } catch (IOException e) {
                dbExported = false;
                Crashlytics.logException(e);
                return false;
            }
        }

        String result = "";

        protected void onPostExecute(final Boolean success) {
            dbExported = success;
            ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
            if (success) {
                mfeedbacklabel.setText(result);
                mfeedbacklabel.setVisibility(View.VISIBLE);

                Long currentMillis = System.currentTimeMillis();
                prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_BACKUP_SYNC, currentMillis).commit();

                Toast.makeText(SettingsActivity.this, "Export successful!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SettingsActivity.this, "Export failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public class CSVWriter {

        private PrintWriter pw;

        private char separator;

        private char quotechar;

        private char escapechar;

        private String lineEnd;

        /**
         * The character used for escaping quotes.
         */
        public static final char DEFAULT_ESCAPE_CHARACTER = '"';

        /**
         * The default separator to use if none is supplied to the constructor.
         */
        public static final char DEFAULT_SEPARATOR = ',';

        /**
         * The default quote character to use if none is supplied to the
         * constructor.
         */
        public static final char DEFAULT_QUOTE_CHARACTER = '"';

        /**
         * The quote constant to use when you wish to suppress all quoting.
         */
        public static final char NO_QUOTE_CHARACTER = '\u0000';

        /**
         * The escape constant to use when you wish to suppress all escaping.
         */
        public static final char NO_ESCAPE_CHARACTER = '\u0000';

        /**
         * Default line terminator uses platform encoding.
         */
        public static final String DEFAULT_LINE_END = "\n";

        /**
         * Constructs CSVWriter using a comma for the separator.
         *
         * @param writer the writer to an underlying CSV source.
         */
        public CSVWriter(Writer writer) {
            this(writer, DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHARACTER,
                    DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
        }

        /**
         * Constructs CSVWriter with supplied separator, quote char, escape char and line ending.
         *
         * @param writer     the writer to an underlying CSV source.
         * @param separator  the delimiter to use for separating entries
         * @param quotechar  the character to use for quoted elements
         * @param escapechar the character to use for escaping quotechars or escapechars
         * @param lineEnd    the line feed terminator to use
         */
        public CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd) {
            this.pw = new PrintWriter(writer);
            this.separator = separator;
            this.quotechar = quotechar;
            this.escapechar = escapechar;
            this.lineEnd = lineEnd;
        }

        /**
         * Writes the next line to the file.
         *
         * @param nextLine a string array with each comma-separated element as a separate
         *                 entry.
         */
        public void writeNext(String[] nextLine) {

            if (nextLine == null)
                return;

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < nextLine.length; i++) {

                if (i != 0) {
                    sb.append(separator);
                }

                String nextElement = nextLine[i];
                if (nextElement == null)
                    continue;
                if (quotechar != NO_QUOTE_CHARACTER)
                    sb.append(quotechar);
                for (int j = 0; j < nextElement.length(); j++) {
                    char nextChar = nextElement.charAt(j);
                    if (escapechar != NO_ESCAPE_CHARACTER && nextChar == quotechar) {
                        sb.append(escapechar).append(nextChar);
                    } else if (escapechar != NO_ESCAPE_CHARACTER && nextChar == escapechar) {
                        sb.append(escapechar).append(nextChar);
                    } else {
                        sb.append(nextChar);
                    }
                }
                if (quotechar != NO_QUOTE_CHARACTER)
                    sb.append(quotechar);
            }

            sb.append(lineEnd);
            pw.write(sb.toString());

        }

        /**
         * Flush underlying stream to writer.
         *
         * @throws IOException if bad things happen
         */
        public void flush() throws IOException {

            pw.flush();

        }

        /**
         * Close the underlying stream writer flushing any buffered content.
         *
         * @throws IOException if bad things happen
         */
        public void close() throws IOException {
            pw.flush();
            pw.close();
        }

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
            } catch (FileNotFoundException e) {
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
        String databasePath = "/BackupFolder/" + Constants.dbName;
        try {
            InputStream databaseInputFile = getAssets().open(Constants.dbName + ".db");
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

    //async task to retrieve markers
    public class retrievePlacesAsyncTask extends AsyncTask<String, Void, String> {
        private final ProgressDialog loadingDialog = new ProgressDialog(SettingsActivity.this);
        Boolean shouldDeleteExisting;

        public retrievePlacesAsyncTask(Boolean shouldDeleteExisiting) {
            this.shouldDeleteExisting = shouldDeleteExisiting;
        }

        @Override
        protected void onPreExecute() {
            String strToShow = "Downloading spots from Hitchwiki Maps...";

            ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (shouldDeleteExisting)
                strToShow = "Updating spots from Hitchwiki Maps...";

            try {
                loadingDialog.setTitle(getString(R.string.settings_downloadHDSpots_button_label));
                loadingDialog.setMessage(strToShow);

                if (!loadingDialog.isShowing())
                    loadingDialog.show();
            } catch (Exception ex) {
                String msg = ex.getMessage();
                Crashlytics.logException(ex);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected String doInBackground(String... params) {
           /* PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            wl.acquire();*/

            //this boolean is used for mkdir down in the code, so it's not useless as it seems
            boolean dummySuccessAtCreatingFolder = false;

            if (isCancelled()) {
                return "Canceled";
            }

            //check if there's folder where we store file with markers stored
            markersStorageFolder = new File
                    (
                            Environment.getExternalStorageDirectory() +
                                    "/" +
                                    "Android/data/" +
                                    context.getPackageName() +
                                    Constants.FOLDERFORSTORINGMARKERS
                    );

            //create "/markersStorageFolder" if not already created
            if (!markersStorageFolder.exists()) {
                //create folder for the first time
                dummySuccessAtCreatingFolder = markersStorageFolder.mkdir();

                //as folder didn't exist, this is the first time we download markers, so proceed with downloading them
                //retrieving markers per continent, as specified in API
                hitchwikiAPI.getPlacesByContinent("EU", getPlacesByArea);
                hitchwikiAPI.getPlacesByContinent("AS", getPlacesByArea);
                hitchwikiAPI.getPlacesByContinent("AF", getPlacesByArea);
                hitchwikiAPI.getPlacesByContinent("NA", getPlacesByArea);
                hitchwikiAPI.getPlacesByContinent("SA", getPlacesByArea);
                hitchwikiAPI.getPlacesByContinent("AN", getPlacesByArea);
                hitchwikiAPI.getPlacesByContinent("OC", getPlacesByArea);

                //we will put complete placesContainer into a file in this newly created folder once we go
                //to onPostExecute, using gson converter to JSON and streaming it into a file
                return "folderDidntExist";
            } else {
                //folder exists, but it may be a case that file with stored markers is missing, so lets check that
                File fileCheck = new File(markersStorageFolder, Constants.FILE_NAME_FOR_STORING_MARKERS);

                if (!fileCheck.exists()) {
                    //so file is missing, app has to download markers, like above
                    //as folder didn't exist, this is the first time we download markers, so proceed with downloading them
                    //retrieving markers per continent, as specified in API
                    hitchwikiAPI.getPlacesByContinent("EU", getPlacesByArea);
                    hitchwikiAPI.getPlacesByContinent("AS", getPlacesByArea);
                    hitchwikiAPI.getPlacesByContinent("AF", getPlacesByArea);
                    hitchwikiAPI.getPlacesByContinent("NA", getPlacesByArea);
                    hitchwikiAPI.getPlacesByContinent("SA", getPlacesByArea);
                    hitchwikiAPI.getPlacesByContinent("AN", getPlacesByArea);
                    hitchwikiAPI.getPlacesByContinent("OC", getPlacesByArea);

                    //we will put complete placesContainer into a file in this newly created folder once we go
                    //to onPostExecute, using gson converter to JSON and streaming it into a file
                    return "folderDidntExist";
                } else {
                    if (fileCheck.length() == 0 || shouldDeleteExisting) {
                        //security check if folder isn't deleted in the meantime (since hitchwiki app was started)
                        if (!markersStorageFolder.exists()) {
                            //create folder again
                            dummySuccessAtCreatingFolder = markersStorageFolder.mkdir();
                        } else {
                            //folder exists (totally expected), so lets delete existing file now
                            //but its size is 0KB, so lets delete it and download markers again
                            fileCheck.delete();
                        }

                        //recreate placesContainer, it might not be empty
                        if (placesContainer != null) {
                            placesContainer = null;
                            placesContainer = new ArrayList<PlaceInfoBasic>();
                        }

                        //this boolean will trigger marker placing in onCameraChange method
                        placesContainerIsEmpty = true;

                        //retrieving markers per continent, as specified in API
                        hitchwikiAPI.getPlacesByContinent("EU", getPlacesByArea);
                        hitchwikiAPI.getPlacesByContinent("AS", getPlacesByArea);
                        hitchwikiAPI.getPlacesByContinent("AF", getPlacesByArea);
                        hitchwikiAPI.getPlacesByContinent("NA", getPlacesByArea);
                        hitchwikiAPI.getPlacesByContinent("SA", getPlacesByArea);
                        hitchwikiAPI.getPlacesByContinent("AN", getPlacesByArea);
                        hitchwikiAPI.getPlacesByContinent("OC", getPlacesByArea);

                        //we will put complete placesContainer into a file in this newly created folder once we go
                        //to onPostExecute, using gson converter to JSON and streaming it into a file
                        return "folderDidntExist";
                    } else {
                        //proceed with streaming this file into String and converting it by gson to placesContainer
                        //then continue the logic from getPlacesByArea listener

                        File fl = new File(markersStorageFolder, Constants.FILE_NAME_FOR_STORING_MARKERS);
                        FileInputStream fin;
                        try {
                            fin = new FileInputStream(fl);

                            //get markersStorageFile streamed into String, so gson can convert it into placesContainer
                            String placesContainerAsString = Utils.convertStreamToString(fin);

                            fin.close();

                            PlaceInfoBasic[] placesContainerFromFile =
                                    hitchwikiAPI.getPlacesByContinenFromLocalFile(placesContainerAsString);

                            placesContainer.clear();

                            for (int i = 0; i < placesContainerFromFile.length; i++) {
                                placesContainer.add(placesContainerFromFile[i]);
                            }

                            //prepare everything that Clusterkraf needs
                            //buildMarkerModels(placesContainer);

                            //now build array list of inputPoints
                            //buildInputPoints();
                        } catch (FileNotFoundException exception) {
                            exception.printStackTrace();
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }

                        return "folderExisted";
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.contentEquals("folderDidntExist")) {
                //in this case, we have full placesContainer, processed to fulfill Clusterkraf model requirements and all,
                //so we have to create file in storage folder and stream placesContainer into it using gson
                File fileToStoreMarkersInto = new File(markersStorageFolder, Constants.FILE_NAME_FOR_STORING_MARKERS);


               /* //update date in optionsMenu
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                Date resultdate = new Date(currentMillis);
                optionsMenuRefreshDate.setText(sdf.format(resultdate));*/

                try {
                    FileOutputStream fileOutput = new FileOutputStream(fileToStoreMarkersInto);

                    Gson gsonC = new Gson();
                    String placesContainerAsString = gsonC.toJson(placesContainer);

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

                    //continue with clusterkraf, as file is written and markers are stored
                    //initClusterkraf();

                    //this boolean will trigger marker placing in onCameraChange method
                    placesContainerIsEmpty = false;

                } catch (FileNotFoundException exception) {
                    exception.printStackTrace();
                } catch (UnsupportedEncodingException exception) {
                    exception.printStackTrace();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            } else {
                //in this case, we processed already existing storage file, so we go on with initClusterkraf
                //initClusterkraf();

                //this boolean will trigger marker placing in onCameraChange method
                placesContainerIsEmpty = false;
            }


            ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            loadingDialog.dismiss();

            if (result.contentEquals("spotsDownloaded")) {
                //also write into prefs that markers sync has occurred
                Long currentMillis = System.currentTimeMillis();
                prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD, currentMillis).commit();

                Toast.makeText(SettingsActivity.this, "Download successful!", Toast.LENGTH_SHORT).show();
                Long millisecondsAtRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD, 0);
                if (millisecondsAtRefresh != 0) {
                    //convert millisecondsAtRefresh to some kind of date and time text
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                    Date resultdate = new Date(millisecondsAtRefresh);
                    String timeStamp = sdf.format(resultdate);

                    showCrouton(String.format("%s spots were downloaded. Last sync was on %s.",
                            placesContainer.size(),
                            timeStamp),
                            Constants.CROUTON_DURATION_5000);
                } else {
                    showCrouton(String.format("%s spots were downloaded.",
                            placesContainer.size()),
                            Constants.CROUTON_DURATION_5000);
                }
            } else if (result.contentEquals("nothingToSync"))
                showErrorAlert("Hitchwiki Maps cleared", "All spots previously downloaded from Hitchwiki Maps were deleted from your device. To download spots, select one or more continent.");
            else if (!result.contentEquals("spotsLoadedFromLocalStorage") && !result.isEmpty())
                showErrorAlert("An error occurred", "An exception occurred while trying to download spots from Hitchwiki Maps.");
        }
    }
        }
    }

    public File markersStorageFolder;
    public static List<PlaceInfoBasic> placesContainer = new ArrayList<PlaceInfoBasic>();
    public boolean placesContainerIsEmpty = true;
    public static final ApiManager hitchwikiAPI = new ApiManager();

    APICallCompletionListener<PlaceInfoBasic[]> getPlacesByArea = new APICallCompletionListener<PlaceInfoBasic[]>() {
        @Override
        public void onComplete(boolean success, int intParam, String stringParam, Error error, PlaceInfoBasic[] object) {
            if (success) {
                for (int i = 0; i < object.length; i++) {
                    placesContainer.add(object[i]);
                }

                //prepare everything that Clusterkraf needs
                //buildMarkerModels(placesContainer);

                //now build array list of inputPoints
                //buildInputPoints();
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

                for (int i = 0; i < object.length; i++) {
                    System.out.println("country is = " + object[i].getName() + ", iso is = " + object[i].getIso());
//											a.getPlaceBasicDetails(Integer.valueOf(object[i].getId()), createAccountCallback);
                }
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


