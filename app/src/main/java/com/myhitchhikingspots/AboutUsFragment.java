package com.myhitchhikingspots;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class AboutUsFragment extends android.support.v4.app.Fragment {

    MainActivity activity;
    ImageButton btnInstagram, btnGitHub, btnFDroid, btnPlayStore;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about_us, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnInstagram = view.findViewById(R.id.btnInstagram);
        btnGitHub = view.findViewById(R.id.btnGitHub);
        btnFDroid = view.findViewById(R.id.btnFDroid);
        btnPlayStore = view.findViewById(R.id.btnPlayStore);

        btnInstagram.setOnClickListener(view1 -> startInstagram());
        btnGitHub.setOnClickListener(view1 -> startGitHub());
        btnFDroid.setOnClickListener(view1 -> startFDroid());
        btnPlayStore.setOnClickListener(view1 -> startPlayStore());
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