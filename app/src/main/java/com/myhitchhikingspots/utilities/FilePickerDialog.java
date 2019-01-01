package com.myhitchhikingspots.utilities;

import java.io.File;

import android.app.Activity;
import android.content.Context;

import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.interfaces.AsyncTaskListener;

public class FilePickerDialog {
    AsyncTaskListener<File> onFilePicked;

    public void openDialog(final Activity activity, final Context context) {
        File mPath = new File(Constants.EXPORTED_DB_STORAGE_PATH);
        FileDialog fileDialog = new FileDialog(activity, mPath, Constants.EXPORT_DB_AS_CSV_FILE_EXTENSION, Constants.INTERNAL_DB_FILE_EXTENSION);
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