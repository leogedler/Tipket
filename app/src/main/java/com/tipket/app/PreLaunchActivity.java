package com.tipket.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.model.GraphUser;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Arrays;
import java.util.List;


public class PreLaunchActivity extends Activity {

    public static final String TAG = PreLaunchActivity.class.getSimpleName();

    //Global variables
    protected ImageView mLoginWithFacebook;
    protected ImageView mLoginButton;
    protected ImageView mSignUpWithEMail;
    private Dialog progressDialog;
    protected String mUsername;
    protected String mFacebookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_launch);


        mLoginWithFacebook = (ImageView) findViewById(R.id.loginSignUpWithFacebook);
        mLoginButton = (ImageView) findViewById(R.id.loginImageButton);
        mSignUpWithEMail = (ImageView) findViewById(R.id.signUpWithEmail);


        mLoginWithFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onLoginButtonClicked();
            }
        });


        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(PreLaunchActivity.this, LoginActivity.class);
                startActivity(intent);

            }
        });

        mSignUpWithEMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(PreLaunchActivity.this, SignUpActivity.class);
                startActivity(intent);
                
            }
        });

    }

    private void onLoginButtonClicked() {
        PreLaunchActivity.this.progressDialog = ProgressDialog.show(
                PreLaunchActivity.this, "", "Logging in...", true);
        List<String> permissions = Arrays.asList("basic_info", "user_about_me",
                "user_relationships", "user_birthday", "user_location");
        ParseFacebookUtils.logIn(permissions, this, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                PreLaunchActivity.this.progressDialog.dismiss();
                if (user == null) {
                    Log.d(TAG, "Uh oh. The user cancelled the Facebook login.");
                    Log.d(TAG, err.getMessage());
                } else if (user.isNew()) {
                    Log.d(TAG, "User signed up and logged in through Facebook!");

                    makeMeRequest();

                } else {
                    Log.d(TAG, "User logged in through Facebook!");
                    enterIntoDispatchActivity();
                }
            }
        });
    }

    private void makeMeRequest() {
        Request request = Request.newMeRequest(ParseFacebookUtils.getSession(),
                new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null) {

                            mUsername = user.getName();
                            mFacebookId = user.getId();

                            //Up date current user information
                            ParseUser.getCurrentUser().put(ParseConstants.KEY_USERNAME, mUsername);
                            ParseUser.getCurrentUser().put(ParseConstants.FACEBOOK_USER_ID, mFacebookId);
                            ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {

                                        enterIntoTutorialActivity();

                                    } else {

                                        Log.d("Error updating user info", e.getMessage());
                                        Toast.makeText(PreLaunchActivity.this, R.string.error_updating_user_info, Toast.LENGTH_LONG).show();

                                    }
                                }
                            });

                        } else if (response.getError() != null) {
                            if ((response.getError().getCategory() == FacebookRequestError.Category.AUTHENTICATION_RETRY)
                                    || (response.getError().getCategory() == FacebookRequestError.Category.AUTHENTICATION_REOPEN_SESSION)) {
                                Log.d(TAG,"The facebook session was invalidated.");

                                ParseUser.logOut();
                                Intent intent = new Intent(PreLaunchActivity.this, LoginActivity.class);
                                startActivity(intent);

                            } else {
                                Log.d(TAG,"Some other error: "+ response.getError().getErrorMessage()
                                );
                            }
                        }
                    }
                }
        );
        request.executeAsync();
    }

    private void enterIntoTutorialActivity() {
        Intent intent = new Intent(this, TutorialActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
    }

    private void enterIntoDispatchActivity() {
        Intent intent = new Intent(this, DispatchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pre_launch, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

}
