package com.myhitchhikingspots;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

public class AboutUsFragment extends Fragment {

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about_us, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnInstagram).setOnClickListener(view1 -> startInstagram());
        view.findViewById(R.id.btnGitHub).setOnClickListener(view1 -> startGitHub());
        view.findViewById(R.id.btnFDroid).setOnClickListener(view1 -> startFDroid());
        view.findViewById(R.id.btnPlayStore).setOnClickListener(view1 -> startPlayStore());

        ((TextView) view.findViewById(R.id.textView15)).setText(getActivity().getString(R.string.about_us_how_to_contribute_text, getActivity().getString(R.string.app_name)));
    }


    public void startInstagram() {
        Uri uri = Uri.parse(getString(R.string.mhs_on_instagram));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        intent.setPackage("com.instagram.android");

        //Record user's click on the Instagram button
        Answers.getInstance().logCustom(new CustomEvent("Instagram button click"));

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.mhs_on_instagram_fallback))));
        }
    }

    public void startGitHub() {
        Uri uri = Uri.parse(getString(R.string.mhs_on_github));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        //Record user's click on the GitHub button
        Answers.getInstance().logCustom(new CustomEvent("GitHub button click"));

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.mhs_on_github_fallback))));
        }
    }

    public void startPlayStore() {
        Uri uri = Uri.parse(getString(R.string.mhs_on_playstore));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        //Record user's click on the PlayStore button
        Answers.getInstance().logCustom(new CustomEvent("PlayStore button click"));

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.mhs_on_playstore_fallback))));
        }
    }

    public void startFDroid() {
        Uri uri = Uri.parse(getString(R.string.mhs_on_fdroid));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        //Record user's click on the F-droid button
        Answers.getInstance().logCustom(new CustomEvent("F-droid button click"));

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.mhs_on_fdroid_fallback))));
        }
    }
}