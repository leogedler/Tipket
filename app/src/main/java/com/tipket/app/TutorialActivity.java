package com.tipket.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;


public class TutorialActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        ViewPager pager = (ViewPager) findViewById(R.id.viewPager);
        pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int pos) {
            switch(pos) {

                case 0: return TutorialFragment1.newInstance("Swipe please");
                case 1: return TutorialFragment2.newInstance("Swipe please");
                case 2: return TutorialFragment3.newInstance("Swipe please");
                case 3: return TutorialFragment4.newInstance("Swipe please");
                case 4: return TutorialFragment5.newInstance("Swipe please");

                default: return TutorialFragment5.newInstance("TutorialFragment5, Default");
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    }

}
