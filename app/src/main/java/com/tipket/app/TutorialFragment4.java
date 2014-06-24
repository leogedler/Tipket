package com.tipket.app;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by leonardogedler on 17/04/2014.
 */
public class TutorialFragment4 extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tutorial_4_frag, container, false);

        //TextView tv = (TextView) v.findViewById(R.id.tvFragFourth);
        //tv.setText(getArguments().getString("msg"));

        return v;
    }

    public static TutorialFragment4 newInstance(String text) {

        TutorialFragment4 f = new TutorialFragment4();
        Bundle b = new Bundle();
        b.putString("msg", text);

        f.setArguments(b);

        return f;
    }




}
