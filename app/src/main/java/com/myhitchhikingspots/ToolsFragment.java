package com.myhitchhikingspots;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.mapbox.android.core.FileUtils;
import com.myhitchhikingspots.interfaces.AsyncTaskListener;
import com.myhitchhikingspots.model.SpotDao;
import com.myhitchhikingspots.utilities.DatabaseExporter;
import com.myhitchhikingspots.utilities.DatabaseImporter;
import com.myhitchhikingspots.utilities.FilePickerDialog;
import com.myhitchhikingspots.utilities.Utils;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.app.Activity.RESULT_OK;


public class ToolsFragment extends Fragment {
    TextView mfeedbacklabel;
    View coordinatorLayout;

    SharedPreferences prefs;

    // Storage Permissions variables
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 1;

    final static int PICK_DB_REQUEST = 1;
    final static String TAG = "settings-activity";

    /**
     * Path to the exported file on the local storage.
     * Please note that at the moment when this variable is set we know that the file exists because it has been just generated.
     * Therefore we should avoid using this path in the future unless we guarantee that we check if it still exists before using it for anything (like for sharing).
     **/
    public String destinationFilePath = "";

    TextView tools_tips_description;
    ImageView tools_tips_header_img;
    Button btnShare;
    SpotsListViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(SpotsListViewModel.class);
        return inflater.inflate(R.layout.fragment_tools, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Activity activity = requireActivity();

        //prefs
        prefs = activity.getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        mfeedbacklabel = view.findViewById(R.id.feedbacklabel);
        mfeedbacklabel.setVisibility(View.GONE);

        tools_tips_description = view.findViewById(R.id.tools_tips_description);
        tools_tips_header_img = view.findViewById(R.id.tools_tips_header_img);
        btnShare = view.findViewById(R.id.btnShare);

        String strLastDownload = "";

        Long millisecondsAtNow = System.currentTimeMillis();
        Long millisecondsLastCountriesRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_COUNTRIES_DOWNLOAD, 0);
        if (millisecondsLastCountriesRefresh > 0) {
            String timePast = Utils.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsLastCountriesRefresh), activity);
            strLastDownload += "- " + String.format(getString(R.string.hwmaps_last_countriesList_update_message), timePast);
        }

        Long millisecondsLastExport = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_BACKUP, 0);
        if (millisecondsLastExport > 0) {
            if (!strLastDownload.isEmpty())
                strLastDownload += "\n";
            String timePast = Utils.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsLastExport), activity);
            strLastDownload += "- " + String.format(getString(R.string.settings_last_export_message), timePast);
        }

        Long millisecondsAtRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_HWSPOTS_DOWNLOAD, 0);
        if (millisecondsAtRefresh > 0) {
            if (!strLastDownload.isEmpty())
                strLastDownload += "\n";

            String timePast = Utils.getWaitingTimeAsString((int) TimeUnit.MILLISECONDS.toMinutes(millisecondsAtNow - millisecondsAtRefresh), activity);
            strLastDownload += "- " + String.format(getString(R.string.hwmaps_last_download_message), timePast);
        }

        if (!strLastDownload.isEmpty()) {
            mfeedbacklabel.setText(strLastDownload);
            mfeedbacklabel.setVisibility(View.VISIBLE);
        }

        view.findViewById(R.id.btnExport).setOnClickListener(this::exportButtonHandler);
        view.findViewById(R.id.btnImport).setOnClickListener(this::importButtonHandler);
        view.findViewById(R.id.btnShare).setOnClickListener(this::shareDB);

        Spinner spinner = view.findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity,
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

        coordinatorLayout = view.findViewById(R.id.coordinatiorLayout);

        view.findViewById(R.id.item_description_layout).setOnClickListener((v) -> {
            collapseExpandTextView();
        });

        ((TextView) view.findViewById(R.id.tools_tips_description))
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

    private static boolean areStoragePermissionsGranted(Activity activity) {
        // Check if we have read and write permission
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};

        return arePermissionsGranted(activity, permissions);
    }

    private static boolean arePermissionsGranted(Activity activity, String[] permissions) {
        ArrayList<String> permissionsToExplain = Utils.getPendingPermissions(activity, permissions);
        return permissionsToExplain.isEmpty();
    }

    private void requestStoragePermissions() {
        String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //If any permission is missing, show rationale
        if (arePermissionsGranted(requireActivity(), permissions))
            Toast.makeText(requireActivity(), getString(R.string.hitchwiki_maps_storage_permission_not_granted), Toast.LENGTH_LONG).show();

        requestPermissions(permissions, PERMISSIONS_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (actionToPerfomOncePermissionIsGranted == 1)
                    exportDBNow();
                else if (actionToPerfomOncePermissionIsGranted == 2)
                    importButtonHandler(null);
                actionToPerfomOncePermissionIsGranted = -1;
            } else
                Toast.makeText(requireActivity(), getString(R.string.hitchwiki_maps_storage_permission_not_granted), Toast.LENGTH_LONG).show();
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_DB_REQUEST) {
                try {
                    Uri uri = data.getData();
                    File file = FileUtils.getFile(requireContext(), uri.getPath());
                    importPickedFile(file);
                } catch (Exception e) {
                    e.printStackTrace();
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

    private static String getPath(Context context, Uri uri) {
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

    private void showErrorAlert(String title, String msg) {
        new AlertDialog.Builder(requireActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.general_ok_option), null)
                .show();
    }

    // 1 for exporting database and 2 for importing database
    private int actionToPerfomOncePermissionIsGranted = -1;

    private void importButtonHandler(View view) {
        if (!areStoragePermissionsGranted(requireActivity())) {
            actionToPerfomOncePermissionIsGranted = 2;
            requestStoragePermissions();
            return;
        }

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        //    openFilePicker(requireActivity());
        //else
        showFilePickerDialog();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void openFilePicker() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        //From Android KitKat we were already exporting spot list always as CSV files, so thi sis the most important type to be able to import here.
        i.setType("text/csv");
        i.putExtra("android.content.extra.SHOW_ADVANCED", true);
        startActivityForResult(i, PICK_DB_REQUEST);
    }

    private void showFilePickerDialog() {
        FilePickerDialog dialog = new FilePickerDialog();
        //Add listener to be called when task finished
        dialog.addListener(new AsyncTaskListener<File>() {
            @Override
            public void notifyTaskFinished(Boolean success, File file, String filePath) {
                //Start import task
                importPickedFile(file);
            }
        });
        dialog.openDialog(requireActivity(), null);
    }

    private void collapseExpandTextView() {
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

    private void importPickedFile(File fileToImport) {

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

        DatabaseImporter t = new DatabaseImporter(requireActivity(), fileToImport, shouldFixStartDates);

        //Add listener to be called when task finished
        t.addListener(new AsyncTaskListener<ArrayList<String>>() {
            @Override
            public void notifyTaskFinished(Boolean success, ArrayList<String> messages, String filePath) {
                String title = "";

                //Show toast
                if (success) {
                    title = getString(R.string.general_import_finished_successful_message);

                    Toast.makeText(requireActivity(), title, Toast.LENGTH_SHORT).show();

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

                    viewModel.reloadSpots(requireActivity());
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
    private void showFixSpotsDatetimeDialog(File fileToImport) {
        new AlertDialog.Builder(requireActivity())
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
                    //regardless of user's answer, call requireActivity() method again
                    importPickedFile(fileToImport);
                })
                .show();
    }

    private void showShareExportedDatabaseDialog() {
        new AlertDialog.Builder(requireActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.tools_database_exported_successfully_message))
                .setMessage(getString(R.string.tools_exportdb_share_dialog_message, getString(R.string.general_share_label)))
                .setNeutralButton(getString(R.string.general_share_label), (dialog, which) -> {
                    shareCSV();
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    saveCSV();
                })
                .setNegativeButton(getString(R.string.general_cancel_option), (dialog, which) -> {
                    btnShare.setVisibility(View.VISIBLE);
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
    static boolean shouldFixStartDateTimes(String fileName) throws IllegalArgumentException {
        DateTime version27_releasedOn = DateTime.parse(Constants.APP_VERSION27_WAS_RELEASED_ON_UTCDATETIME);

        //NOTE:
        // Version 27 has automatically fixed all StartDateTime of spots that have been saved before its release.
        // Databases exported before version 27 were named with the format Constants.OLD_EXPORT_CSV_FILENAME_FORMAT.

        // If fileName doesn't follow any naming format (which means it has been renamed and its name doesn't contain a datetime in any expected format), an exception will be thrown.
        DateTime file_exportedOn = Utils.extractDateTimeFromFileName(fileName, DateTimeZone.getDefault());

        //Check if fileName tells us that the database has been exported before version 27.
        return file_exportedOn.isBefore(version27_releasedOn);
    }

    private void exportButtonHandler(View view) {
        if (!areStoragePermissionsGranted(requireActivity())) {
            actionToPerfomOncePermissionIsGranted = 1;
            requestStoragePermissions();
        } else
            exportDBNow();
    }

    private void exportDBNow() {
        try {
            DatabaseExporter t = new DatabaseExporter(requireActivity());
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
                            showShareExportedDatabaseDialog();
                        }
                    } else {
                        showErrorAlert(getString(R.string.general_export_finished_failed_message), message);

                        //Create a record to track Export Database failure
                        Answers.getInstance().logCustom(new CustomEvent("Database export failed"));
                    }
                }
            });

            SpotsListViewModel viewModel = new ViewModelProvider(requireActivity()).get(SpotsListViewModel.class);
            Cursor curCSV = viewModel.rawQuery(requireActivity(), "select * from " + SpotDao.TABLENAME, null);

            t.execute(curCSV);

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

    private void shareCSV() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        //intent.putExtra(Intent.EXTRA_TEXT, "body text");
        //intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"email@example.com"});
        File file = new File(destinationFilePath);
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(requireActivity(), "Attachment Error", Toast.LENGTH_SHORT).show();
            return;
        }

        //Record usage of share option
        Answers.getInstance().logCustom(new CustomEvent("Database shared"));

        Uri uri = getPathUri(file);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, getString(R.string.settings_sharedb_button_label)));
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void saveCSV() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/csv");
        File file = new File(destinationFilePath);
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(requireActivity(), "Attachment Error", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ResolveInfo> resInfoList = requireActivity().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            requireActivity().getApplicationContext().grantUriPermission(packageName, Uri.parse(file.getPath()), Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        intent.putExtra(Intent.EXTRA_TITLE, file.getName());

        //Record usage of share option
        Answers.getInstance().logCustom(new CustomEvent("Save CSV called"));

        Uri uri = getPathUri(file); //  Uri.parse(destinationFilePath)
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, getString(R.string.settings_sharedb_button_label)));
    }

    private Uri getPathUri(File file) {
        Uri uri;
        //We have to check if it's nougat or not.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            uri = FileProvider.getUriForFile(requireActivity(), BuildConfig.APPLICATION_ID, file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    ProgressDialog progressDialog;

    private void showProgressDialog(String title, String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(requireActivity());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
        }

        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        //Remove the flag that keeps the screen from sleeping
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

}


