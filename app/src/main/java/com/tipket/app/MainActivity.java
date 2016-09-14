package com.tipket.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.parse.ParseUser;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {


    public static final String TAG = MainActivity.class.getSimpleName();

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;


    // Global varible for Search Text
    public String mSearchText;

    // Global varible for Radius of Search in Kilometres
    public int mKilometres;

    // Filters
    public boolean mFilterStatus;
    protected boolean mFilterByDate;
    protected boolean mFilterByTrendy;
    protected boolean mFilterByDistance;

    //User name
    public String mUserName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUserName = ParseUser.getCurrentUser().getUsername();

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setHomeButtonEnabled(true);


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(this, getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }


    // Public method for pass the Search String to ProductsFragment and MapViewFragment
    public String getSearch() {

        if( getIntent().getExtras() != null)
        {
            mSearchText = getIntent().getExtras().getString("search");

            if (!mSearchText.isEmpty()){
                return mSearchText;
            }
        }
        return null;
    }

    // Public method for pass the Search String to ProductsFragment and MapViewFragment
    public int getKilometres() {

        if( getIntent().getExtras() != null)
        {
            mKilometres = getIntent().getExtras().getInt("kilometres");

            if (mKilometres!=0) {
                return mKilometres;
            }
        }
        return 1;
    }

    // Public method for get the filter status
    public boolean getFilterStatus() {

        if( getIntent().getExtras() != null){

            mFilterStatus = getIntent().getExtras().getBoolean("filterStatus");
            return mFilterStatus;
        }

        return false;
    }

    // Public method for Sort by date
    public  boolean getSortByDate() {

        if (getIntent().getExtras() != null){

            mFilterByDate = getIntent().getExtras().getBoolean("sortByDate");
            return mFilterByDate;
        }
        return true;
    }

    // Public method for Sort by trendy
    public  boolean getSortByTrendy() {

        if (getIntent().getExtras() != null){

            mFilterByTrendy = getIntent().getExtras().getBoolean("sortByTrendy");
            return mFilterByTrendy;
        }
        return false;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home){

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

        }

        /*
        if (id == R.id.action_logOut) {

            ParseUser.logOut();
            navigateToLogin();

        }*/

        if (id == R.id.action_tip) {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(R.string.question_label);

            builder.setPositiveButton(R.string.yes_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(MainActivity.this, TipInPlaceActivity.class);
                    startActivity(intent);

                }
            });
            builder.setNegativeButton(R.string.no_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(MainActivity.this, TipNotInPlaceActivity.class);
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

            String userId = ParseUser.getCurrentUser().getObjectId();

            Intent intent = new Intent(MainActivity.this, UserProfile2.class);
            intent.putExtra("userName", mUserName);
            intent.putExtra("userId", userId);
            startActivity(intent);

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /*
    @Override
    public void onBackPressed()
    {
        Log.d("CDA", "onBackPressed Called");
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }*/

}
