package com.tipket.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

/**
 * Created by leonardogedler on 12/03/2014.
 */
public class DispatchActivity extends Activity {

    //User info
    public static String mUserName;
    public static String mUserFacebookId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);


        ParseAnalytics.trackAppOpened(getIntent());

        if (isNetworkAvailable()){
            // Check if there is current user info
            if (ParseUser.getCurrentUser() != null) {

                //Getting the most recent username and navigate to the user profile
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.getInBackground(ParseUser.getCurrentUser().getObjectId(), new GetCallback<ParseUser>() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        if (e == null) {

                            String fullName = parseUser.getString(ParseConstants.KEY_USERNAME);
                            String[] strings = fullName.split("\\s+");

                            if (strings.length > 1){
                                mUserName = strings[0] +" "+ strings[1];
                            }
                            else {
                                mUserName = strings[0];
                            }

                            ParseUser.getCurrentUser().put(ParseConstants.KEY_USERNAME, mUserName);


                            if (parseUser.getString(ParseConstants.FACEBOOK_USER_ID)!= null) {

                                mUserFacebookId = parseUser.getString(ParseConstants.FACEBOOK_USER_ID);
                                ParseUser.getCurrentUser().put(ParseConstants.FACEBOOK_USER_ID, mUserFacebookId);

                            }

                            //Start an intent for the MainActivity in activity
                            startActivity(new Intent(DispatchActivity.this, MainActivity.class));

                            //Intent intent = new Intent(DispatchActivity.this, TutorialActivity.class);
                            //startActivity(intent);


                        }

                        else {

                            Log.d("Exception fetching the global username :", e.getMessage());
                            Toast.makeText(DispatchActivity.this, R.string.network_unavailable, Toast.LENGTH_LONG).show();
                            finish();

                        }
                    }
                });

            } else {
                // Start and intent for the PreLaunch out activity
                startActivity(new Intent(this, PreLaunchActivity.class));
            }

        }
        else {
            Toast.makeText(this, R.string.network_unavailable, Toast.LENGTH_LONG).show();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 2000);

        }
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;
    }

}
