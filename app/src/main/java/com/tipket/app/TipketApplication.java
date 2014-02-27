package com.tipket.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.parse.Parse;

/**
 * Created by leonardogedler on 24/02/2014.
 */
public class TipketApplication extends Application{

    // Debugging switch
    public static final boolean APPDEBUG = false;

    // Debugging tag for the application
    public static final String APPTAG = "Tipket";

    // Key for saving the search distance preference
    private static final String KEY_SEARCH_DISTANCE = "searchDistance";

    private static SharedPreferences preferences;

    public void onCreate() {
        Parse.initialize(this, "jI2MqfWiM1zAhYKEeOXOer2Zx0mdrSQnYPzSjuSS", "JghfqCw53l8cGnvqJEB57QIahWcplMfAo1ALOXlQ");
        preferences = getSharedPreferences("com.tipket.app", Context.MODE_PRIVATE);
    }

    public static float getSearchDistance() {
        return preferences.getFloat(KEY_SEARCH_DISTANCE, 250);
    }

    public static void setSearchDistance(float value) {
        preferences.edit().putFloat(KEY_SEARCH_DISTANCE, value).commit();
    }


}
