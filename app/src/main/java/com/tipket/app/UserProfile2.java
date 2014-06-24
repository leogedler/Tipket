package com.tipket.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.widget.WebDialog;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


public class UserProfile2 extends Activity {

    public static final String TAG = UserProfile2.class.getSimpleName();
    protected ProgressDialog progressDialog;

    //Global Variables
    protected String mUserName;
    protected String mUserId;
    protected TextView mUserNameView;
    protected TextView mUserScore;
    protected List<ParseObject> mProducts;
    protected ParseUser mParseUser;
    protected ParseRelation<ParseObject> mUserProducts;
    protected String mFacebookId;
    protected ImageView mUserProfilePictureView;
    protected ImageView mFollowButton;
    protected ImageView mUnfollowButton;
    protected TextView mFollowingButton;
    protected ParseUser mCurrentUser;
    protected ParseRelation<ParseUser> mFriendsRelation;
    protected ParseRelation<ParseUser> mUserFollowings;
    protected ParseRelation<ParseUser> mUserCountFollowings;
    protected ImageView mLogoutButton;
    protected URL mUrl;
    protected ImageView mInviteFacebookFriends;
    protected String mCurrentUserName;
    protected GridView mGridView;
    protected ImageView mListIconUnselected;
    protected TextView mCountOfFollowings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile2);

        progressDialog = ProgressDialog.show(UserProfile2.this, "", getString(R.string.loading_label));


        mGridView = (GridView) findViewById(R.id.gridView1);

        //Receiving the user's data
        mUserName = getIntent().getExtras().getString("userName");
        mUserId = getIntent().getExtras().getString("userId");
        mCurrentUserName = ParseUser.getCurrentUser().getUsername();

        mCountOfFollowings = (TextView) findViewById(R.id.countOfFollowings);

        //Go to user profile list view
        mListIconUnselected = (ImageView) findViewById(R.id.listUnSelected);
        mListIconUnselected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mUserId.equals(ParseUser.getCurrentUser().getObjectId())) {

                    String actualUserId = ParseUser.getCurrentUser().getObjectId();

                    Intent intent = new Intent(UserProfile2.this, UserProfile.class);
                    intent.putExtra("userName", mCurrentUserName);
                    intent.putExtra("userId", actualUserId);
                    startActivity(intent);

                }else {

                    Intent intent = new Intent(UserProfile2.this, UserProfile.class);
                    intent.putExtra("userName", mUserName);
                    intent.putExtra("userId", mUserId);
                    startActivity(intent);
                }
            }
        });


        mUserNameView = (TextView) findViewById(R.id.userNameView);
        mUserNameView.setText(StringHelper.capitalize(mUserName));
        mUserScore = (TextView) findViewById(R.id.userScore);

        //Query for the user information and products
        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        userQuery.getInBackground(mUserId, new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {

                if (e != null) {

                    progressDialog.dismiss();

                } else {

                    mParseUser = parseUser;

                    mFacebookId = mParseUser.getString(ParseConstants.FACEBOOK_USER_ID);

                    //Setting user profile picture
                    mUserProfilePictureView = (ImageView) findViewById(R.id.userProfilePicture);
                    try {
                        mUrl = new URL("https://graph.facebook.com/"+mFacebookId+"/picture?type=large&return_ssl_resources=1");
                    } catch (MalformedURLException exception) {
                        exception.printStackTrace();
                    }
                    Picasso.with(UserProfile2.this).load(String.valueOf(mUrl)).fit().transform(new RoundedTransformation(180, 0))
                            .error(R.drawable.no_user_ico).into(mUserProfilePictureView);

                    //Setting the count of points
                    if (mParseUser.getInt(ParseConstants.KEY_POINTS) == 1) {
                        mUserScore.setText(String.valueOf(mParseUser.getInt(ParseConstants.KEY_POINTS)) + " " + "point");
                    }else {
                        mUserScore.setText(String.valueOf(mParseUser.getInt(ParseConstants.KEY_POINTS)) + " " + "points");
                    }
                    //Retrieving the user products
                    mUserProducts = mParseUser.getRelation(ParseConstants.KEY_USER_PRODUCT_RELATION);
                    ParseQuery<ParseObject> userProducts = mUserProducts.getQuery();
                    userProducts.orderByDescending("updatedAt");
                    userProducts.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> parseObjects, ParseException e) {

                            if (e == null){
                                progressDialog.dismiss();

                                mProducts = parseObjects;

                                mGridView.setAdapter(new TipUserGridAdapter(UserProfile2.this, mProducts));

                            }
                            else {
                                progressDialog.dismiss();
                                Log.e(TAG, e.getMessage());

                                AlertDialog.Builder builder = new AlertDialog.Builder(UserProfile2.this);
                                builder.setMessage(R.string.error_user_profile);
                                builder.setCancelable(false);

                                builder.setPositiveButton(R.string.go_to_feed, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        Intent intent = new Intent(UserProfile2.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);

                                    }
                                });

                                AlertDialog dialog = builder.create();
                                dialog.show();

                            }
                        }
                    });

                    //Counting the user followings
                    mUserCountFollowings = mParseUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);
                    ParseQuery<ParseUser> userCountOfFollowings = mUserCountFollowings.getQuery();
                    userCountOfFollowings.countInBackground(new CountCallback() {
                        @Override
                        public void done(int i, ParseException e) {

                            mCountOfFollowings.setText(String.valueOf(i));

                        }
                    });

                }
            }
        });


        //Following and Unfollowing actions buttons

        mFollowButton = (ImageView) findViewById(R.id.followButton);
        mUnfollowButton = (ImageView) findViewById(R.id.unfollowButton);
        mFollowingButton = (TextView) findViewById(R.id.followingButton);
        mLogoutButton = (ImageView) findViewById(R.id.logOutButton);
        mInviteFacebookFriends = (ImageView) findViewById(R.id.inviteFacebookFriends);

        mCurrentUser = ParseUser.getCurrentUser();
        mFriendsRelation = mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);

        if (ParseUser.getCurrentUser().getObjectId().equals(mUserId)){

            //Go to the following list
            goToFollowingList();

            mLogoutButton.setVisibility(View.VISIBLE);
            mLogoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    ParseUser.logOut();
                    navigateToLogin();

                }
            });

            mInviteFacebookFriends.setVisibility(View.VISIBLE);
            mInviteFacebookFriends.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (ParseFacebookUtils.getSession() != null) {
                        sendRequestDialog();
                    }else {

                        AlertDialog.Builder builder = new AlertDialog.Builder(UserProfile2.this);
                        builder.setTitle(R.string.no_session_facebook_tittle);
                        builder.setMessage(R.string.no_session_facebook_message);

                        builder.setPositiveButton(R.string.go_to_sign_up_with_facebook, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                ParseUser.logOut();

                                Intent intent = new Intent(UserProfile2.this, PreLaunchActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        });

                        builder.setNegativeButton(R.string.cancel_facebook_action, new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });

                        AlertDialog dialog = builder.create();
                        dialog.show();

                    }
                }
            });

        }
        else {

            //Go to the following list
            goToFollowingList();

            //Checking for match in the relationship current user with the others users
            mUserFollowings = mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);
            ParseQuery<ParseUser> userFollowings = mUserFollowings.getQuery();
            userFollowings.findInBackground(new FindCallback<ParseUser>() {
                @Override
                public void done(List<ParseUser> friendsUsers, ParseException e) {

                    boolean currentUserFollowThisUser = false;

                    if (friendsUsers.size() != 0) {

                        for (int i = 0; i < friendsUsers.size(); i++) {

                            ParseObject friend = friendsUsers.get(i);

                            if (friend.getObjectId().equals(mUserId)){

                                currentUserFollowThisUser = true;
                                removeFollowing();
                                break;

                            }

                        }

                        if (currentUserFollowThisUser == false) {

                            addFollowing();
                        }


                    } else {

                        addFollowing();

                    }
                }
            });

        }
    }

    private void goToFollowingList() {
        mFollowingButton.setVisibility(View.VISIBLE);
        mFollowingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(UserProfile2.this, FollowingActivity.class);
                intent.putExtra("parseUserId", mUserId);
                startActivity(intent);

            }
        });
    }

    private void addFollowing() {
        mFollowButton.setVisibility(View.VISIBLE);
        mFollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mFriendsRelation.add(mParseUser);
                mCurrentUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {

                        if (e == null) {

                            Toast.makeText(UserProfile2.this, R.string.following_added, Toast.LENGTH_LONG).show();
                            mFollowButton.setVisibility(View.INVISIBLE);
                            removeFollowing();

                        } else {
                            Log.d("Error", e.getMessage());
                        }

                    }
                });

            }
        });
    }

    private void removeFollowing() {
        mUnfollowButton.setVisibility(View.VISIBLE);
        mUnfollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mFriendsRelation.remove(mParseUser);
                mCurrentUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {

                        if (e == null) {

                            Toast.makeText(UserProfile2.this, R.string.following_removed, Toast.LENGTH_LONG).show();
                            mUnfollowButton.setVisibility(View.INVISIBLE);
                            addFollowing();

                        } else {
                            Log.d("Error", e.getMessage());
                        }

                    }
                });

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user_profile2, menu);

        if (mUserId.equals(ParseUser.getCurrentUser().getObjectId())) {

            menu.findItem(R.id.user_profile).setIcon(R.drawable.user_selected);

        }
        else {

            menu.findItem(R.id.user_profile).setIcon(R.drawable.user);

        }

        return true;
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, PreLaunchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_tip) {

            AlertDialog.Builder builder = new AlertDialog.Builder(UserProfile2.this);
            builder.setMessage(R.string.question_label);

            builder.setPositiveButton(R.string.yes_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(UserProfile2.this, TipInPlaceActivity.class);
                    startActivity(intent);

                }
            });
            builder.setNegativeButton(R.string.no_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(UserProfile2.this, TipNotInPlaceActivity.class);
                    startActivity(intent);

                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

        }

        if (id == R.id.action_search){

            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        }

        if (id == R.id.user_profile){

            String actualUserId = ParseUser.getCurrentUser().getObjectId();

            Intent intent = new Intent(UserProfile2.this, UserProfile2.class);
            intent.putExtra("userName", mCurrentUserName);
            intent.putExtra("userId", actualUserId);
            startActivity(intent);

        }
        return super.onOptionsItemSelected(item);
    }

    private void sendRequestDialog() {
        Bundle params = new Bundle();
        params.putString("message", "Try Tipket");

        WebDialog requestsDialog = (
                new WebDialog.RequestsDialogBuilder(this,
                        Session.getActiveSession(),
                        params))
                .setOnCompleteListener(new WebDialog.OnCompleteListener() {

                    @Override
                    public void onComplete(Bundle values,
                                           FacebookException error) {
                        if (error != null) {
                            if (error instanceof FacebookOperationCanceledException) {
                                Toast.makeText(UserProfile2.this.getApplicationContext(),
                                        R.string.invitation_canceled,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(UserProfile2.this.getApplicationContext(),
                                        R.string.error_post,
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            final String requestId = values.getString("request");
                            if (requestId != null) {
                                Toast.makeText(UserProfile2.this.getApplicationContext(),
                                        R.string.invitation_complete,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(UserProfile2.this.getApplicationContext(),
                                        R.string.invitation_canceled,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                })
                .build();
        requestsDialog.show();
    }

}
