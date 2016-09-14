package com.tipket.app;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by leonardogedler on 24/02/2014.
 */
public class SearchProductsFragment extends Fragment {

    protected ImageView mSearchButton;
    protected AutoCompleteTextView mSearchText;
    protected TextView mKilometresTextView;
    protected SeekBar mSeekBar;
    protected int mKilometres;
    protected ImageView mFilterButton;
    protected ImageView mRemoveFilterButton;
    protected CheckBox mSortByDate;
    protected CheckBox mSortByTrendy;
    protected TextView mSearchLabel;
    protected boolean mFilterStatus = false;
    protected boolean mFilterByDate = true;
    protected boolean mFilterByTrendy = false;
    protected ArrayAdapter mAdapter;
    protected String[] mFilteredArray = {" "};
    protected boolean mViewStop = false;


    //User name
    protected String mUserName;

    protected ProgressDialog progressDialog;



    View mRootView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_search_products, container, false);





        return mRootView;

    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mUserName = ParseUser.getCurrentUser().getUsername();

        mSearchText = (AutoCompleteTextView) mRootView.findViewById(R.id.searchText);

        //Querying the products names
        final ParseQuery<ParseObject> products = new ParseQuery<ParseObject>(ParseConstants.CLASS_TIPS);
        products.orderByDescending("createdAt");
        products.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> productsNames, ParseException e) {
                if (e == null) {

                    if (productsNames.size() != 0) {

                        String [] listOfProductsNames = new String[productsNames.size()];

                        int i = 0;
                        for (ParseObject merchant : productsNames) {
                            listOfProductsNames[i] = merchant.getString(ParseConstants.KEY_PRODUCT_NAME);
                            i++;
                        }

                        Set<String> stringSet = new HashSet<String>(Arrays.asList(listOfProductsNames));
                        mFilteredArray = stringSet.toArray(new String[0]);

                        if (mViewStop == false) {
                            mAdapter = new ArrayAdapter
                                    (getActivity(), android.R.layout.simple_list_item_1, mFilteredArray);

                            mSearchText.setAdapter(mAdapter);
                        }
                    }
                }
            }
        });

        //Sort Check boxes
        mSortByDate = (CheckBox) mRootView.findViewById(R.id.sortByDate);
        mSortByTrendy = (CheckBox) mRootView.findViewById(R.id.sortByTrendy);
        mSortByDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mSortByDate.isChecked()){
                    mSortByTrendy.setChecked(false);
                    mFilterByTrendy = false;
                    mFilterByDate = true;
                }else {
                    mSortByDate.setChecked(true);
                }

            }
        });

        mSortByTrendy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mSortByTrendy.isChecked()){
                    mSortByDate.setChecked(false);
                    mFilterByDate = false;
                    mFilterByTrendy = true;
                }else {
                    mSortByTrendy.setChecked(true);
                }

            }
        });

        // SeekBar for searching radius definition
        mSeekBar = (SeekBar) mRootView.findViewById(R.id.seekBar);
        mKilometresTextView = (TextView) mRootView.findViewById(R.id.kilometresTextView);
        mSearchLabel = (TextView) mRootView.findViewById(R.id.searchLabel);

        // Filter buttons options
        mFilterButton = (ImageView) mRootView.findViewById(R.id.filterButton);
        mRemoveFilterButton = (ImageView) mRootView.findViewById(R.id.hideFilterButton);
        mFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mFilterButton.setVisibility(View.INVISIBLE);
                mRemoveFilterButton.setVisibility(View.VISIBLE);
                mSeekBar.setVisibility(View.VISIBLE);
                mSearchLabel.setVisibility(View.VISIBLE);
                mKilometresTextView.setVisibility(View.VISIBLE);
                mFilterStatus = true;

            }
        });

        mRemoveFilterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mFilterButton.setVisibility(View.VISIBLE);
                mRemoveFilterButton.setVisibility(View.INVISIBLE);
                mSeekBar.setVisibility(View.INVISIBLE);
                mSearchLabel.setVisibility(View.INVISIBLE);
                mKilometresTextView.setVisibility(View.INVISIBLE);
                mFilterStatus = false;

            }
        });

        // Initialize the textView with '1'
        mKilometresTextView.setText(mSeekBar.getProgress()+1 + "/" + "10 Km");

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progress = 1;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                progress = i+1;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Display the value in textView
                mKilometresTextView.setText(progress + "/" + "10 Km");

                mKilometres = progress;

            }
        });

        mSearchButton = (ImageView) mRootView.findViewById(R.id.searchButton);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String searchText = mSearchText.getText().toString().toLowerCase();

                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra("search", searchText);
                intent.putExtra("kilometres", mKilometres);
                intent.putExtra("filterStatus", mFilterStatus);
                intent.putExtra("sortByDate", mFilterByDate);
                intent.putExtra("sortByTrendy", mFilterByTrendy);
                startActivity(intent);
            }
        });

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean handled = false;

                if (i == EditorInfo.IME_ACTION_SEARCH){

                    String searchText = mSearchText.getText().toString().toLowerCase().trim();

                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.putExtra("search", searchText);
                    intent.putExtra("kilometres", mKilometres);
                    intent.putExtra("filterStatus", mFilterStatus);
                    intent.putExtra("sortByDate", mFilterByDate);
                    intent.putExtra("sortByTrendy", mFilterByTrendy);
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
