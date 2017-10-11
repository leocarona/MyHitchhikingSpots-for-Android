package com.myhitchhikingspots.utilities;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;

import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.interfaces.AsyncTaskListener;

public class FilePickerDialog {
    AsyncTaskListener<File> onFilePicked;

    public void openDialog(final Activity activity, final Context context) {
        File mPath = new File(Constants.EXPORTED_DB_STORAGE_PATH);
        FileDialog fileDialog = new FileDialog(activity, mPath, ".csv", ".db");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {

            @Override
            public void fileSelected(File file) {
                if (onFilePicked != null)
                    onFilePicked.notifyTaskFinished(file != null, file);
            }
        });
        fileDialog.showDialog();
    }

    public void addListener(AsyncTaskListener<File> doWhenFinished) {
        onFilePicked = doWhenFinished;
    }

}