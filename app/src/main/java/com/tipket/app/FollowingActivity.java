package com.tipket.app;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import java.util.List;


public class FollowingActivity extends ListActivity {

    public static final String TAG = FollowingActivity.class.getSimpleName();

    //Global Variables
    protected List<ParseUser> mFriendsUsers;
    protected ParseRelation<ParseUser> mUserFollowings;
    protected String mParseUserId;
    protected ParseUser mParseUser;
    protected TextView mNoFriends;
    protected String mFollowingName;
    protected String mUserName;
    protected boolean mViewStop = false;

    protected ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);

        mUserName = ParseUser.getCurrentUser().getUsername();
        progressDialog = ProgressDialog.show(FollowingActivity.this, "", getString(R.string.loading_label));

        //Checking for match in the relationship current user with the others users
        mParseUserId = getIntent().getExtras().getString("parseUserId");


        //Query for the user information and relationship
        ParseQuery<ParseUser> userQuery = ParseUser.getQuery();
        userQuery.getInBackground(mParseUserId, new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {

                if (parseUser == null) {

                } else {

                    mParseUser = parseUser;
                    mUserFollowings = mParseUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);
                    ParseQuery<ParseUser> userFollowings = mUserFollowings.getQuery();
                    userFollowings.addDescendingOrder(ParseConstants.KEY_POINTS);
                    userFollowings.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> friendsUsers, ParseException e) {

                            if (e == null){
                                progressDialog.dismiss();

                                mFriendsUsers = friendsUsers;

                                if (mFriendsUsers.size() != 0){

                                    String[] friendsNames = new String[mFriendsUsers.size()];
                                    int i = 0;
                                    for (ParseObject friend : mFriendsUsers) {
                                        friendsNames[i] = friend.getString(ParseConstants.KEY_USERNAME);
                                        i++;
                                    }

                                    if (mViewStop == false) {

                                        if (getListView().getAdapter() == null) {

                                            FollowingAdapter adapter = new FollowingAdapter(getListView().getContext(), mFriendsUsers);
                                            setListAdapter(adapter);
                                        } else {
                                            // refill the adapter!
                                            ((FollowingAdapter) getListView().getAdapter()).refill(mFriendsUsers);

                                        }
                                    }
                                }

                                else {
                                    progressDialog.dismiss();
                                    mFriendsUsers.clear();
                                    mNoFriends = (TextView) findViewById(R.id.noUserFriends);
                                    mNoFriends.setVisibility(View.VISIBLE);
                                }

                            }
                            else {
                                progressDialog.dismiss();
                                Log.d("Error fetching user followings :", e.getMessage());
                            }


                        }
                    });
                }
            }

        });


    }

    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) {
        super.onListItemClick(l, v, position, id);

        progressDialog = ProgressDialog.show(FollowingActivity.this, "",
                getString(R.string.loading_label));

        //Getting the most recent username and navigate to the user profile
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.getInBackground(ParseUser.getCurrentUser().getObjectId(), new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (e == null){

                    ParseObject friend = mFriendsUsers.get(position);
                    String fullFriendName = friend.getString(ParseConstants.KEY_USERNAME);
                    String[] strings = fullFriendName.split("\\s+");

                    if (strings.length > 1){
                        mFollowingName = strings[0] +" "+ strings[1];
                    }
                    else {
                        mFollowingName = strings[0];
                    }
                    String friendId = friend.getObjectId();

                    progressDialog.dismiss();

                    Intent intent = new Intent(FollowingActivity.this, UserProfile2.class);
                    intent.putExtra("userName", mFollowingName);
                    intent.putExtra("userId", friendId);
                    startActivity(intent);

                }
                else {

                    progressDialog.dismiss();
                    Log.d(TAG, e.getMessage());

                }

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.following, menu);
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

    @Override
    protected void onStart() {
        super.onStart();

        mViewStop = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mViewStop = true;

    }
}
