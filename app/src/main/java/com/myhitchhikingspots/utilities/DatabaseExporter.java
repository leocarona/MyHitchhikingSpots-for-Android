package com.myhitchhikingspots.utilities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.interfaces.AsyncTaskListener;
import com.myhitchhikingspots.model.SpotDao;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class DatabaseExporter extends AsyncTask<Cursor, Void, Boolean> {
    private ProgressDialog dialog;
    private WeakReference<Context> contextRef;
    private AsyncTaskListener<String> onFinished;
    private final String TAG = "database-exporter";
    private String result = "", destinationFilePath = "";

    public DatabaseExporter(Context context) {
        this.contextRef = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
        Context context = contextRef.get();
        if (context == null || isCancelled())
            return;

        //Add flag that requests the screen to stay awake so that we prevent it from sleeping and the app doesn't go to background until we release it
        ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Set up progress dialog
        dialog = new ProgressDialog(context);
        this.dialog.setIndeterminate(true);
        this.dialog.setCancelable(false);
        this.dialog.setTitle(context.getString(R.string.settings_exportdb_button_label));
        this.dialog.setMessage(context.getString(R.string.settings_exporting_db_message));
        this.dialog.show();
    }

    protected Boolean doInBackground(Cursor... params) {
        Context context = contextRef.get();
        if (context == null || isCancelled())
            return false;
        Cursor curCSV = params[0];

        Crashlytics.log(Log.INFO, TAG, "DatabaseExporter started executing..");
        try {
            File exportDir = null;
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                exportDir = context.getExternalFilesDir(Constants.EXPORTED_DB_STORAGE_PATH);
            //else
            //    exportDir = new File(Constants.EXPORTED_DB_STORAGE_PATH);


            if (!exportDir.exists()) {
                Crashlytics.log(Log.INFO, TAG, "Directory created. " + exportDir.getPath());
                exportDir.mkdirs();
            }
            String fileName = Utils.getNewExportFileName(DateTime.now(), DateTimeZone.getDefault());

            File destinationFile = new File(exportDir, fileName);
            destinationFile.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(destinationFile));
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                String[] mySecondStringArray = new String[curCSV.getColumnNames().length];
                String[] columnNames = curCSV.getColumnNames();
                for (int i = 0; i < columnNames.length; i++) {
                    String columnName = columnNames[i];
                    if (columnName.equals(SpotDao.Properties.Latitude.columnName) || columnName.equals(SpotDao.Properties.Longitude.columnName))
                        mySecondStringArray[i] = Double.toString(curCSV.getDouble(i));
                    else
                        mySecondStringArray[i] = curCSV.getString(i);
                }
                csvWrite.writeNext(mySecondStringArray);
            }
            csvWrite.close();
            curCSV.close();

            destinationFilePath = destinationFile.getPath();

            //Build "Copied to" string so that the user knows where the exported database file was copied to
            result = String.format(context.getString(R.string.settings_exportdb_finish_successfull_message), destinationFilePath);

            return true;
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
        return false;
    }

    protected void onPostExecute(final Boolean success) {
        Context context = contextRef.get();
        if (context == null || isCancelled())
            return;

        //Remove the flag that keeps the screen from sleeping
        ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (this.dialog.isShowing()) {
            this.dialog.dismiss();
        }

        Crashlytics.setString("doInBackground result", result);

        if (onFinished != null)
            onFinished.notifyTaskFinished(success, result, destinationFilePath);
    }

    public void addListener(AsyncTaskListener<String> doWhenFinished) {
        onFinished = doWhenFinished;
    }
}