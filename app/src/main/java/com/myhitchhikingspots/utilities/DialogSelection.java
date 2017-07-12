package com.myhitchhikingspots.utilities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.myhitchhikingspots.Constants;

/**
 * Created by leoboaventura on 13/06/2017.
 */

public class DialogSelection extends DialogFragment {
    final String TAG = "dialog-selection";
    PairParcelable[] items = new PairParcelable[0];

    boolean argsWereSet = false;

    void setValuesFromBundle(Bundle args) {
        items =(PairParcelable[]) args.getParcelableArray(Constants.DIALOG_STRINGLIST_BUNDLE_KEY);
        argsWereSet = true;
    }

    String[] getItemsValueArray() {
        String[] lst = new String[items.length];
        for (int i = 0; i < lst.length; i++)
            lst[i] = items[i].getValue();
        return lst;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        setValuesFromBundle(args);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!argsWereSet && savedInstanceState != null)
            setValuesFromBundle(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Selection")
                .setMultiChoiceItems(getItemsValueArray(), null, new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int item, boolean isChecked) {
                        Crashlytics.log(Log.INFO, TAG, "Chosen option: " + items[item]);
                        getContext();
                    }
                });

        return builder.create();
    }
}