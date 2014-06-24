package com.tipket.app;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

/**
 * Created by leonardogedler on 24/02/2014.
 */
public class SearchUsersFragment extends Fragment {


    protected String mSearchUser;
    protected AutoCompleteTextView mSearchUsersEditText;
    protected ImageView mSearchButton;
    protected ArrayAdapter mAdapter;
    protected String[] mUsersNames = {" "};
    protected boolean mViewStop = false;

    View mRootView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_search_users, container, false);


        return mRootView;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSearchUsersEditText = (AutoCompleteTextView) mRootView.findViewById(R.id.searchUsersText);

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> users, ParseException e) {

                if (e == null) {

                    if (users.size() != 0) {

                        mUsersNames = new String[users.size()];

                        int i = 0;
                        for (ParseObject userName : users) {
                            mUsersNames[i] = userName.getString(ParseConstants.KEY_USERNAME);
                            i++;
                        }

                        if (mViewStop == false) {

                            mAdapter = new ArrayAdapter
                                    (getActivity(), android.R.layout.simple_list_item_1, mUsersNames);

                            mSearchUsersEditText.setAdapter(mAdapter);
                        }

                    }
                }
            }
        });

        mSearchButton = (ImageView) mRootView.findViewById(R.id.searchButton);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mSearchUser = mSearchUsersEditText.getText().toString();
                Intent intent = new Intent(getActivity(), SearchUsersActivity.class);
                intent.putExtra("userForSearch", mSearchUser);
                startActivity(intent);

            }
        });


        mSearchUsersEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean handled = false;

                if (i == EditorInfo.IME_ACTION_SEARCH){

                    mSearchUser = mSearchUsersEditText.getText().toString().trim();
                    Intent intent = new Intent(getActivity(), SearchUsersActivity.class);
                    intent.putExtra("userForSearch", mSearchUser);
                    startActivity(intent);

                    handled = true;

                }
                return handled;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        mViewStop = false;

    }

    @Override
    public void onStop() {
        super.onStop();

        mViewStop = true;

    }
}
