package com.tipket.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.parse.SignUpCallback;

import java.util.Arrays;
import java.util.List;

public class SignUpActivity extends Activity {

    public static final String TAG = SignUpActivity.class.getSimpleName();

    protected EditText mTextUsername;
    protected EditText mTextPassword;
    protected EditText mTextEmail;
    protected ImageView mSignUpButton;
    protected ImageButton mSignUpWithFacebook;
    private Dialog progressDialog;
    protected String mUsername;
    protected String mPassword;
    protected String mEmail;
    protected String mFacebookId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_sign_up);


        mTextUsername = (EditText) findViewById(R.id.usernameField);
        mTextPassword = (EditText) findViewById(R.id.passwordField);
        mTextEmail = (EditText) findViewById(R.id.emailField);
        mSignUpButton = (ImageView) findViewById(R.id.signUp);
        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mUsername = mTextUsername.getText().toString();
                mPassword = mTextPassword.getText().toString();
                mEmail = mTextEmail.getText().toString();

                mUsername = mUsername.trim();
                mPassword = mPassword.trim();
                mEmail = mEmail.trim();

                if (mUsername.isEmpty() || mPassword.isEmpty() || mEmail.isEmpty()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                    builder.setMessage(R.string.sign_up_error_message)
                            .setTitle(R.string.sign_up_error_tittle)
                            .setPositiveButton(android.R.string.ok, null);

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else {
                    // create the new user
                    setProgressBarIndeterminateVisibility(true);

                    ParseUser newUser = new ParseUser();
                    newUser.setUsername(mUsername);
                    newUser.setPassword(mPassword);
                    newUser.setEmail(mEmail);
                    newUser.put(ParseConstants.KEY_POINTS, 0);
                    newUser.signUpInBackground(new SignUpCallback() {
                        @Override
                        public void done(ParseException e) {

                            setProgressBarIndeterminateVisibility(false);
                            if (e == null){
                                // Success!
                                enterIntoTutorialActivity();
                            }
                            else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                                builder.setMessage(e.getMessage())
                                        .setTitle(R.string.sign_up_error_tittle)
                                        .setPositiveButton(android.R.string.ok, null);

                                AlertDialog dialog = builder.create();
                                dialog.show();

                            }
                        }
                    });

                }

            }
        });

        mSignUpWithFacebook = (ImageButton) findViewById(R.id.signUpWithFacebook);
        mSignUpWithFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onLoginButtonClicked();

            }
        });

    }

    private void onLoginButtonClicked() {
        SignUpActivity.this.progressDialog = ProgressDialog.show(
                SignUpActivity.this, "", "Logging in...", true);
        List<String> permissions = Arrays.asList("basic_info", "user_about_me",
                "user_relationships", "user_birthday", "user_location");
        ParseFacebookUtils.logIn(permissions, this, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                SignUpActivity.this.progressDialog.dismiss();
                if (user == null) {
                    Log.d(TAG,"Uh oh. The user cancelled the Facebook login.");
                    Log.d(TAG, err.getMessage());
                } else if (user.isNew()) {
                    Log.d(TAG,"User signed up and logged in through Facebook!");

                    makeMeRequest();

                } else {
                    Log.d(TAG,"User logged in through Facebook!");
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
                                        Toast.makeText(SignUpActivity.this, R.string.error_updating_user_info, Toast.LENGTH_LONG).show();

                                    }
                                }
                            });

                        } else if (response.getError() != null) {
                            if ((response.getError().getCategory() == FacebookRequestError.Category.AUTHENTICATION_RETRY)
                                    || (response.getError().getCategory() == FacebookRequestError.Category.AUTHENTICATION_REOPEN_SESSION)) {
                                Log.d(TAG,"The facebook session was invalidated.");

                                ParseUser.logOut();
                                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
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
        getMenuInflater().inflate(R.menu.sign_up, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    /*
    //AsyncTask to fetch user profile picture Url and update user information
    private class HttpGetter extends AsyncTask<URL, Void, Void> {


        @Override
        protected Void doInBackground(URL... urls) {
            // TODO Auto-generated method stub
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(String.valueOf(urls[0]));

            try {
                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    Log.v("Getter", "Your data: " + builder.toString()); //response data

                    JSONObject jsonObject = new JSONObject(builder.toString());
                    JSONObject jsonChildObject = (JSONObject) jsonObject.get("picture");
                    JSONObject jsonData = (JSONObject) jsonChildObject.get("data");

                    mProfilePictureUrl = jsonData.getString("url");

                    Log.d("Profile Url:", mProfilePictureUrl);


                } else {
                    Log.e("Getter", "Failed to download file");
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            //Up date current user information
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.getInBackground(ParseUser.getCurrentUser().getObjectId(), new GetCallback<ParseUser>() {
                @Override
                public void done(ParseUser parseUser, ParseException e) {
                    if (e == null) {
                        parseUser.put(ParseConstants.KEY_USERNAME, mUsername);
                        parseUser.put(ParseConstants.FACEBOOK_USER_ID, mFacebookId);
                        parseUser.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {

                                } else {

                                }
                            }
                        });
                    }
                }


            });

        }
    }*/
}
