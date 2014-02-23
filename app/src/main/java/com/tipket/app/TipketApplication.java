package com.tipket.app;

import android.app.Application;
import android.widget.Toast;

import com.parse.Parse;

/**
 * Created by leonardogedler on 23/02/2014.
 */
public class TipketApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, "eCH3rr98c2VDW9UGTd7J6rzDPptcwNJb4JkP3mzE", "4J0zn28PnBt4pfFKtqv6MTNT3SGj2ihwGwxeP7Em");
        Toast.makeText(this, R.string.welcome_message, Toast.LENGTH_LONG).show();


    }




}
