package com.myhitchhikingspots.utilities;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.R;

public class FileDialog {
    private static final String PARENT_DIR = "..";
    private final String TAG = getClass().getName();
    private String[] fileList;
    private File currentPath;

    public interface FileSelectedListener {
        void fileSelected(File file);
    }

    public interface DirectorySelectedListener {
        void directorySelected(File directory);
    }

    private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileDialog.FileSelectedListener>();
    private ListenerList<DirectorySelectedListener> dirListenerList = new ListenerList<FileDialog.DirectorySelectedListener>();
    private WeakReference<Activity> activityRef;
    private boolean selectDirectoryOption;
    private ArrayList<String> fileEndsWith = new ArrayList<>();

    /**
     * @param activity
     * @param path
     */
    public FileDialog(Activity activity, File path, String... typesOfFilesToShow) {
        this.activityRef = new WeakReference<>(activity);

        //Add files extension that should be shown
        for (String type : typesOfFilesToShow) {
            this.fileEndsWith.add(type.toLowerCase());
        }

        if (!path.exists()) path = Environment.getExternalStorageDirectory();

        try {
            loadFileList(path);
        } catch (Exception ex) {
            Crashlytics.logException(ex);
            showErrorAlert(activity, activity.getString(R.string.general_error_dialog_title), activity.getString(R.string.general_error_message_try_again));
        }
    }

    /**
     * @return file dialog
     */
    public Dialog createFileDialog() {
        Activity activity = activityRef.get();
        if (activity == null)
            return null;

        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(currentPath.getPath());
        if (selectDirectoryOption) {
            builder.setPositiveButton(activity.getString(R.string.tools_file_dialog_select_directory_title), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Crashlytics.setString("currentPath", currentPath.getPath());
                    fireDirectorySelectedEvent(currentPath);
                }
            });
        }

        builder.setItems(fileList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String fileChosen = fileList[which];
                File chosenFile = getChosenFile(fileChosen);
                if (chosenFile.isDirectory()) {
                    try {
                        loadFileList(chosenFile);
                        dialog.cancel();
                        dialog.dismiss();
                        showDialog();
                    } catch (Exception ex) {
                        Crashlytics.logException(ex);
                        showErrorAlert(activity, activity.getString(R.string.general_error_dialog_title), activity.getString(R.string.general_error_message_try_again));
                    }
                } else fireFileSelectedEvent(chosenFile);
            }
        });

        dialog = builder.show();
        return dialog;
    }


    public void addFileListener(FileSelectedListener listener) {
        fileListenerList.add(listener);
    }

    public void removeFileListener(FileSelectedListener listener) {
        fileListenerList.remove(listener);
    }

    public void setSelectDirectoryOption(boolean selectDirectoryOption) {
        this.selectDirectoryOption = selectDirectoryOption;
    }

    public void addDirectoryListener(DirectorySelectedListener listener) {
        dirListenerList.add(listener);
    }

    public void removeDirectoryListener(DirectorySelectedListener listener) {
        dirListenerList.remove(listener);
    }

    /**
     * Show file dialog
     */
    public void showDialog() {
        createFileDialog().show();
    }

    private void fireFileSelectedEvent(final File file) {
        fileListenerList.fireEvent(new ListenerList.FireHandler<FileSelectedListener>() {
            public void fireEvent(FileSelectedListener listener) {
                listener.fileSelected(file);
            }
        });
    }

    private void fireDirectorySelectedEvent(final File directory) {
        dirListenerList.fireEvent(new ListenerList.FireHandler<DirectorySelectedListener>() {
            public void fireEvent(DirectorySelectedListener listener) {
                listener.directorySelected(directory);
            }
        });
    }

    private void loadFileList(File path) throws Exception {
        Crashlytics.log("loadFileList was called.");
        Crashlytics.setString("currentPath", path.getPath());
        this.currentPath = path;
        List<String> r = new ArrayList<String>();
        if (path.exists()) {
            if (path.getParentFile() != null) r.add(PARENT_DIR);
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    if (!sel.canRead()) return false;
                    if (selectDirectoryOption) return sel.isDirectory();
                    else {
                        if (sel.isDirectory())
                            return true;

                        Boolean doesFileEndsWith = false;
                        for (int i = 0; i < fileEndsWith.size(); i++) {
                            if (filename.toLowerCase().endsWith(fileEndsWith.get(i)))
                                doesFileEndsWith = true;
                        }
                        return doesFileEndsWith;
                    }
                }
            };
            //Get an array of abstract path names denoting the files and directories in the given directory (path) that satisfy the specified filter.
            String[] fileList1 = path.list(filter);
            // The method returns null if the abstract path does not denote a directory, or if an I/O error occurs.
            if (fileList1 == null) {
                throw new Exception("File.list(filter) has returned null.");
            }
            for (String file : fileList1) {
                r.add(file);
            }
        }
        fileList = (String[]) r.toArray(new String[]{});
    }

    private File getChosenFile(String fileChosen) {
        if (fileChosen.equals(PARENT_DIR)) return currentPath.getParentFile();
        else return new File(currentPath, fileChosen);
    }

    protected void showErrorAlert(Activity activity, String title, String msg) {
        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(activity.getString(R.string.general_ok_option), null)
                .show();
    }
}

class ListenerList<L> {
    private List<L> listenerList = new ArrayList<L>();

    public interface FireHandler<L> {
        void fireEvent(L listener);
    }

    public void add(L listener) {
        listenerList.add(listener);
    }

    public void fireEvent(FireHandler<L> fireHandler) {
        List<L> copy = new ArrayList<L>(listenerList);
        for (L l : copy) {
            fireHandler.fireEvent(l);
        }
    }

    public void remove(L listener) {
        listenerList.remove(listener);
    }

    public List<L> getListenerList() {
        return listenerList;
    }
}