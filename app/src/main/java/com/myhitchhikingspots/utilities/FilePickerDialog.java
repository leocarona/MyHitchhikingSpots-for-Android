package com.myhitchhikingspots.utilities;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import com.myhitchhikingspots.Constants;

public class FilePickerDialog {

    public static void openDialog(final Activity activity, final Context context) {
        File mPath = new File(Constants.EXPORTED_DB_STORAGE_PATH);
        FileDialog fileDialog = new FileDialog(activity, mPath);
        fileDialog.addFileEndsWith(".csv");
        fileDialog.addFileEndsWith(".db");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {

            @Override
            public void fileSelected(File file) {
                new DatabaseImporter(context, activity, file).execute(); //execute asyncTask to import data into database from selected file.
            }
        });
        fileDialog.showDialog();
    }

}