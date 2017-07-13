package com.myhitchhikingspots.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.dualquo.te.hitchwiki.entities.PlaceInfoCompleteComment;
import com.myhitchhikingspots.R;
import com.myhitchhikingspots.utilities.Utils;

public class CommentsListViewAdapter extends ArrayAdapter<PlaceInfoCompleteComment> {
    private Context context;
    private ArrayList<PlaceInfoCompleteComment> comments;
    //private Typeface font;

    public CommentsListViewAdapter(Context context, ArrayList<PlaceInfoCompleteComment> commentsForMarker) {
        super(context, R.layout.rowlayout_comment, commentsForMarker);
        this.context = context;
        this.comments = commentsForMarker;

        //font = Typeface.createFromAsset(context.getAssets(), "fonts/ubuntucondensed.ttf");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        TextView commentUserNameRow = null;
        TextView commentTimestampRow = null;
        TextView commentTextRow = null;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.rowlayout_comment, parent, false);

            commentUserNameRow = (TextView) convertView.findViewById(R.id.rowlayout_comment_userName);
            commentTimestampRow = (TextView) convertView.findViewById(R.id.rowlayout_comment_timestamp);
            commentTextRow = (TextView) convertView.findViewById(R.id.rowlayout_comment_text);

            //setting proper font and size
            //commentUserNameRow.setTypeface(font);
            commentUserNameRow.setTextColor(Color.DKGRAY);

            //commentTimestampRow.setTypeface(font);
            commentTimestampRow.setTextColor(Color.DKGRAY);

            //commentTextRow.setTypeface(font);
            commentTextRow.setTextColor(Color.BLACK);

        } else {
            commentUserNameRow = (TextView) convertView.findViewById(R.id.rowlayout_comment_userName);
            commentTimestampRow = (TextView) convertView.findViewById(R.id.rowlayout_comment_timestamp);
            commentTextRow = (TextView) convertView.findViewById(R.id.rowlayout_comment_text);
        }

        PlaceInfoCompleteComment comment = comments.get(position);
        Crashlytics.setString("comment.getId", comment.getId());

        try {
            commentUserNameRow.setText(String.format(context.getString(R.string.general_author_label), comment.getUserName()));
            commentTimestampRow.setText(comment.getDatetime());
            commentTextRow.setText(Utils.stringBeautifier(comment.getComment()));

        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }

        return convertView;
    }

}