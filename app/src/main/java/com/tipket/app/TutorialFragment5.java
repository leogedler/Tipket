package com.tipket.app;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by leonardogedler on 17/04/2014.
 */
public class TutorialFragment5 extends Fragment {

    protected ImageView mGoToMain;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tutorial_5_frag, container, false);

        //TextView tv = (TextView) v.findViewById(R.id.tvFragFifth);
        //tv.setText(getArguments().getString("msg"));


        mGoToMain = (ImageView) v.findViewById(R.id.goToMain);
        mGoToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getActivity(), DispatchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            }
        });



        return v;
    }

    public static TutorialFragment5 newInstance(String text) {

        TutorialFragment5 f = new TutorialFragment5();
        Bundle b = new Bundle();
        b.putString("msg", text);

        f.setArguments(b);

        return f;
    }




}
