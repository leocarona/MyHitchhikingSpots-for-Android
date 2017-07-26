package com.myhitchhikingspots.utilities;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import com.myhitchhikingspots.Constants;

public class MyCSVFileReader {

    public static void openDialogToReadCSV(final Activity activity, final Context context) {
        File mPath = new File(Constants.EXPORTED_DB_STORAGE_PATH);
        FileDialog fileDialog = new FileDialog(activity, mPath);
        fileDialog.setFileEndsWith(".csv");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {

            @Override
            public void fileSelected(File file) {
                new ImportCVSToSQLiteDataBase(context, activity, file).execute(); //execute asyncTask to import data into database from selected file.
            }
        });
        fileDialog.showDialog();
    }

}