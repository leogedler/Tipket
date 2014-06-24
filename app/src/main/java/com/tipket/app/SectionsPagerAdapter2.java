package com.tipket.app;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import java.util.Locale;

/**
 * A {@link android.support.v13.app.FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter2 extends FragmentPagerAdapter {

    private SearchActivity searchActivity;

    public SectionsPagerAdapter2(SearchActivity searchActivity, FragmentManager fm) {
        super(fm);
        this.searchActivity = searchActivity;

    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.

        switch (position) {
            case  0:
                return new SearchProductsFragment();
            case 1:
                return new SearchUsersFragment();


        }
        return null;
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Locale l = Locale.getDefault();
        switch (position) {
            case 0:
                return searchActivity.getString(R.string.title_section3).toUpperCase(l);
            case 1:
                return searchActivity.getString(R.string.title_section4).toUpperCase(l);

        }
        return null;
    }


}
