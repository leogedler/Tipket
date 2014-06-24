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
import com.parse.ParseUser;

import java.util.List;


public class SearchUsersActivity extends ListActivity {

    public static final String TAG = SearchUsersActivity.class.getSimpleName();

    //Global Variables
    protected List<ParseUser> mUsers;
    protected String mUserForSearch;
    protected TextView mNoUser;
    protected String mFollowingName;
    protected String mUserName;
    protected boolean mViewStop = false;

    protected ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users);

        mUserName = ParseUser.getCurrentUser().getUsername();
        progressDialog = ProgressDialog.show(SearchUsersActivity.this, "", getString(R.string.loading_label));

        //Checking for match in the relationship current user with the others users
        mUserForSearch = getIntent().getExtras().getString("userForSearch");


        //Query for the user
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereContains(ParseConstants.KEY_USERNAME, mUserForSearch);
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> users, ParseException e) {

                if (e == null){
                    progressDialog.dismiss();

                    mUsers = users;

                    if (mUsers.size() != 0){

                        String[] UsersNames = new String[mUsers.size()];
                        int i = 0;
                        for (ParseObject user : mUsers) {
                            UsersNames[i] = user.getString(ParseConstants.KEY_USERNAME);
                            i++;
                        }

                        if (mViewStop == false) {

                            if (getListView().getAdapter() == null) {

                                SearchUsersAdapter adapter = new SearchUsersAdapter(getListView().getContext(), mUsers);
                                setListAdapter(adapter);
                            } else {
                                // refill the adapter!
                                ((FollowingAdapter) getListView().getAdapter()).refill(mUsers);

                            }
                        }
                    }

                    else {
                        progressDialog.dismiss();
                        mUsers.clear();
                        mNoUser = (TextView) findViewById(R.id.noUser);
                        mNoUser.setVisibility(View.VISIBLE);
                    }

                }
                else {
                    progressDialog.dismiss();
                    Log.d("Error fetching user followings :", e.getMessage());
                }


            }
        });


    }

    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) {
        super.onListItemClick(l, v, position, id);

        progressDialog = ProgressDialog.show(SearchUsersActivity.this, "",
                getString(R.string.loading_label));

        //Getting the most recent username and navigate to the user profile
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.getInBackground(ParseUser.getCurrentUser().getObjectId(), new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (e == null){

                    ParseObject friend = mUsers.get(position);
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

                    Intent intent = new Intent(SearchUsersActivity.this, UserProfile2.class);
                    intent.putExtra("userName", mFollowingName);
                    intent.putExtra("userId", friendId);
                    startActivity(intent);

                }
                else {

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
