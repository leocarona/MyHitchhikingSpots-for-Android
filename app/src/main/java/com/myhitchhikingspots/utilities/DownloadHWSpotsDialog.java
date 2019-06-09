package com.myhitchhikingspots.utilities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.myhitchhikingspots.Constants;
import com.myhitchhikingspots.R;

public class DownloadHWSpotsDialog extends DialogFragment {
    final String TAG = "dialog-selection";
    PairParcelable[] items = new PairParcelable[0];
    String dialog_type = "";
    boolean argsWereSet = false;
    String selectedCodes = "";

    public static final String DIALOG_TYPE_CONTINENT = "dialog-continents-dialog_type";
    public static final String DIALOG_TYPE_COUNTRY = "dialog-countries-dialog_type";
    public static final String LIST_SEPARATOR = ", ";

    private DownloadHWSpotsDialogListener callback;

    public interface DownloadHWSpotsDialogListener {
        void onDownloadConfirmClicked(String selectedCodes, String dialog_type);

        String getContinentsContainer(int item);

        String getCountryContainer(int item);
    }

    public DownloadHWSpotsDialog(DownloadHWSpotsDialogListener callback) {
        this.callback = callback;
    }

    void setValuesFromBundle(Bundle args) {
        items = (PairParcelable[]) args.getParcelableArray(Constants.DIALOG_STRINGLIST_BUNDLE_KEY);
        dialog_type = args.getString(Constants.DIALOG_TYPE_BUNDLE_KEY);
        argsWereSet = true;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setValuesFromBundle(args);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray(Constants.DIALOG_STRINGLIST_BUNDLE_KEY, items);
        outState.putString(Constants.DIALOG_TYPE_BUNDLE_KEY, dialog_type);
    }

    String[] getItemsValueArray() {
        String[] lst = new String[items.length];
        for (int i = 0; i < lst.length; i++)
            lst[i] = items[i].getValue();
        return lst;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!argsWereSet && savedInstanceState != null)
            setValuesFromBundle(savedInstanceState);

        SharedPreferences prefs = getContext().getSharedPreferences(Constants.PACKAGE_NAME, Context.MODE_PRIVATE);

        switch (dialog_type) {
            case DIALOG_TYPE_CONTINENT:
                selectedCodes = prefs.getString(Constants.PREFS_SELECTED_CONTINENTS_TO_DOWNLOAD, "");
                break;
            case DIALOG_TYPE_COUNTRY:
                selectedCodes = prefs.getString(Constants.PREFS_SELECTED_COUNTRIES_TO_DOWNLOAD, "");
                break;
        }

        boolean[] lst = new boolean[items.length];
        for (int i = 0; i < lst.length; i++) {
            lst[i] = selectedCodes.contains(items[i].getKey());
                        /*((dialog_type.equalsIgnoreCase(DIALOG_TYPE_CONTINENT) && selectedCodes.contains(continentsContainer[i].getKey()))
                        || (dialog_type.equalsIgnoreCase(DIALOG_TYPE_COUNTRY) && selectedCodes.contains(countriesContainer[i].getIso())));*/
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String title = "";
        if (dialog_type.equals(DIALOG_TYPE_CONTINENT))
            title = getString(R.string.settings_select_continents_button_label);
        else if (dialog_type.equals(DIALOG_TYPE_COUNTRY))
            title = getString(R.string.settings_select_countries_button_label);

        builder.setTitle(title)
                .setMultiChoiceItems(getItemsValueArray(), lst, new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int item, boolean isChecked) {
                        try {
                            String code = "";
                            if (dialog_type.equals(DIALOG_TYPE_CONTINENT))
                                code = callback.getContinentsContainer(item);
                            else if (dialog_type.equals(DIALOG_TYPE_COUNTRY))
                                code = callback.getCountryContainer(item);

                            if (!isChecked) {
                                selectedCodes = selectedCodes.replaceAll(LIST_SEPARATOR + code + LIST_SEPARATOR, "");
                                selectedCodes = selectedCodes.replaceAll(code + LIST_SEPARATOR, "");
                                selectedCodes = selectedCodes.replaceAll(LIST_SEPARATOR + code, "");
                                selectedCodes = selectedCodes.replaceAll(code, "");
                                //selectedCodes = selectedCodes.replaceAll(LIST_SEPARATOR + LIST_SEPARATOR, LIST_SEPARATOR);
                            } else if (!selectedCodes.contains(code)) {
                                if (!selectedCodes.isEmpty())
                                    selectedCodes += LIST_SEPARATOR;
                                selectedCodes += code;
                            }

                            Crashlytics.log(Log.INFO, TAG, "Chosen option: " + code);
                        } catch (Exception ex) {
                            Crashlytics.log(Log.INFO, TAG, "Deu mal: " + ex.getMessage());
                        }
                    }
                })
                .setPositiveButton(getString(R.string.general_download_option), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Crashlytics.log(Log.INFO, TAG, "READY to download!");

                        //Create a record to track usage of Download HW spots button
                        Answers.getInstance().logCustom(new CustomEvent("HW spots downloaded")
                                .putCustomAttribute("Region", (dialog_type == DIALOG_TYPE_CONTINENT) ? "Continent" : "Country"));

                        callback.onDownloadConfirmClicked(selectedCodes, dialog_type);
                    }
                })
                .setNegativeButton(getResources().getString(R.string.general_cancel_option), null);

        return builder.create();
    }

}