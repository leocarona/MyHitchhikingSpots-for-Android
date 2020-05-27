package com.myhitchhikingspots.utilities;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.interfaces.AsyncTaskListener;

import java.io.File;

public class FilePickerDialog {
    AsyncTaskListener<File> onFilePicked;

    public void openDialog(final Activity activity, final Context context) {
        File mPath = null;
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            mPath = activity.getExternalFilesDir(Constants.EXPORTED_DB_STORAGE_PATH);
        //else
        //    mPath = new File(Constants.SDCARD_STORAGE_PATH + Constants.EXPORTED_DB_STORAGE_PATH);
        FileDialog fileDialog = new FileDialog(activity, mPath, Constants.EXPORT_DB_AS_CSV_FILE_EXTENSION, Constants.INTERNAL_DB_FILE_EXTENSION);
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {

            @Override
            public void fileSelected(File file) {
                if (onFilePicked != null)
                    onFilePicked.notifyTaskFinished(file != null, file, file.getPath());
            }
        });
        fileDialog.showDialog();
    }

    public void addListener(AsyncTaskListener<File> doWhenFinished) {
        onFilePicked = doWhenFinished;
    }

}