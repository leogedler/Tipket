package com.tipket.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseUser;
import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by leonardogedler on 31/03/2014.
 */
public class SearchUsersAdapter extends ArrayAdapter<ParseUser> {

    protected Context mContext;
    protected List<ParseUser> mUsers;
    protected String mFollowingName;
    protected URL mUrl;

    public SearchUsersAdapter(Context context, List<ParseUser> users) {
        super(context, R.layout.search_user_item, users);

        mContext = context;
        mUsers = users;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.search_user_item, null);
            holder = new ViewHolder();
            holder.followingName = (TextView) convertView.findViewById(R.id.followingNameView);
            holder.followingPicture = (ImageView) convertView.findViewById(R.id.followingPictureView);
            holder.followingPoints = (TextView) convertView.findViewById(R.id.followingPointsView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ParseUser user = mUsers.get(position);

        String fullName = user.getString(ParseConstants.KEY_USERNAME);
        String[] strings = fullName.split("\\s+");

        if (strings.length > 1){
            mFollowingName = strings[0] +" "+ strings[1];
        }
        else {
            mFollowingName = strings[0];
        }


        holder.followingName.setText(mFollowingName);

        if (user.getInt(ParseConstants.KEY_POINTS) == 1) {
            holder.followingPoints.setText(String.valueOf(user.getInt(ParseConstants.KEY_POINTS)) + " " + "point");
        }else {
            holder.followingPoints.setText(String.valueOf(user.getInt(ParseConstants.KEY_POINTS)) + " " + "points");
        }

        try {
            mUrl = new URL("https://graph.facebook.com/"+user.getString(ParseConstants.FACEBOOK_USER_ID)+"/picture?type=large&return_ssl_resources=1");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Picasso.with(mContext).load(String.valueOf(mUrl)).fit().transform(new RoundedTransformation(180, 0))
                .error(R.drawable.no_user_ico).into(holder.followingPicture);


        return convertView;
    }

    private static class ViewHolder {

        TextView followingName;
        ImageView followingPicture;
        TextView followingPoints;
    }

    public void refill(List<ParseUser> followings) {

        mUsers.clear();
        mUsers.addAll(followings);
        notifyDataSetChanged();
    }


}
