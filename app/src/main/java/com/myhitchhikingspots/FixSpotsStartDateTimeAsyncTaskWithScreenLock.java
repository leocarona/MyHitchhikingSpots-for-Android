package com.myhitchhikingspots;

import android.app.ProgressDialog;
import android.content.Context;

import com.myhitchhikingspots.model.Spot;

import java.util.List;

public class FixSpotsStartDateTimeAsyncTaskWithScreenLock extends FixSpotsStartDateTimeAsyncTask {
    private Context context;
    private ProgressDialog dialog;

    FixSpotsStartDateTimeAsyncTaskWithScreenLock(Context context, LoadSpotsAndRoutesTask.onPostExecute callback, List<Spot> spotList, Spot mCurrentWaitingSpot) {
        super(callback, spotList, mCurrentWaitingSpot);
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        dialog.setTitle(context.getString(R.string.general_loading_dialog_message));
        dialog.setMessage(context.getString(R.string.settings_fixing_date_message));
        dialog.setCancelable(false);
        dialog.setIcon(android.R.drawable.ic_dialog_info);
        dialog.show();
    }

    protected void onPostExecute(List<Spot> spotList) {
        super.onPostExecute(spotList);

        if (dialog.isShowing())
            dialog.dismiss();
    }
}
