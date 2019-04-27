package com.myhitchhikingspots;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

/** @deprecated **/
public class SampleBasicActivity extends BaseActivity {

    //WARNING: in order to use BaseActivity the method onCreate must be overridden
    // calling first setContentView to the view you want to use
    // and then calling super.onCreate AFTER setContentView.
    // Please always make sure this is been done!
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.sample_basic_master_layout);


        /*
                TO REUSE THIS ACTIVITY FILE, REPLACE:
                setContentView(..)
        AND
                REMEMBER TO ADD AN "<activity>" TAG FOR THE NEW FILE INTO /app/src/main/AndroidManifest.xml

                AND IF YOU WISH TO USE mShouldShowLeftMenu, MAKE SURE YOU HAVE nav_view ON YOUR LAYOUT FILE
        */


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_spot_action_1);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                startActivity(new Intent(getApplicationContext(), MyLocationFragment.class));
            }
        });

        // TO USE LeftMenu (drawer) MAKE SURE YOU ALSO HAVE

        mShouldShowLeftMenu = true;
        super.onCreate(savedInstanceState);
    }
}
