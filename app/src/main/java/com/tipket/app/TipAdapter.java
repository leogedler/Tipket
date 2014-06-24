package com.tipket.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by leonardogedler on 05/03/2014.
 */
public class TipAdapter extends ArrayAdapter<ParseObject> {

    protected Context mContext;
    protected List<ParseObject> mTips;
    protected List<ParseObject> mRelationship;
    protected ParseUser mCurrentUser = ParseUser.getCurrentUser();
    protected String mObjectId;
    protected String mSenderId;
    protected String mSenderName;
    protected String mUserId;
    protected ParseRelation<ParseObject> mUserProducts;
    protected int mCounter;
    protected int mCounter2;
    protected int mCounter3;
    protected ProgressDialog progressDialog;
    protected URL mUrl;
    protected Format mDateFormat;

    public TipAdapter(Context context, List<ParseObject> tips, List<ParseObject> relationship) {
        super(context, R.layout.tip_item, tips);

        mContext = context;
        mTips = tips;
        mRelationship = relationship;


    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.tip_item, null);
            holder = new ViewHolder();
            holder.productName = (TextView) convertView.findViewById(R.id.productNameView);
            holder.posterName = (TextView) convertView.findViewById(R.id.posterNameView);
            holder.merchantNameView = (TextView) convertView.findViewById(R.id.merchantNameView);
            holder.productView = (ImageView) convertView.findViewById(R.id.productListView);
            holder.mWishIco = (ImageView) convertView.findViewById(R.id.wishIco);
            holder.mWishIco2 = (ImageView) convertView.findViewById(R.id.wishIco2);
            holder.mUserProfilePictureView = (ImageView) convertView.findViewById(R.id.userProfilePicture2);
            holder.mCountOfWishes = (TextView) convertView.findViewById(R.id.countOfWishes);
            holder.mCountOfWishes2 = (TextView) convertView.findViewById(R.id.countOfWishes2);
            holder.mProgressBar = (ProgressBar) convertView.findViewById(R.id.progressBar4);
            holder.mPostDateView = (TextView) convertView.findViewById(R.id.postDate);
            holder.mStatusBar = (ImageView) convertView.findViewById(R.id.statusBar);
            holder.mStatusText = (TextView) convertView.findViewById(R.id.statusText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final ParseObject tip = mTips.get(position);
        mSenderId = tip.getString(ParseConstants.KEY_SENDER_ID);
        mSenderName = tip.getString(ParseConstants.KEY_SENDER_NAME);
        mObjectId = tip.getObjectId();
        mUserId = mCurrentUser.getObjectId();
        mCounter = tip.getInt(ParseConstants.KEY_WISHES_COUNT);
        mCurrentUser = ParseUser.getCurrentUser();

        mDateFormat = new SimpleDateFormat("dd MMM");

        String postDate = mDateFormat.format(tip.getCreatedAt());

        holder.mPostDateView.setText(postDate);
        holder.posterName.setText(mSenderName);
        holder.productName.setText(StringHelper.capitalize(tip.getString(ParseConstants.KEY_PRODUCT_NAME)));
        holder.merchantNameView.setText(" " + "@" + " " + StringHelper.capitalize(tip.getString(ParseConstants.KEY_MERCHANT_NAME)));
        holder.mCountOfWishes2.setVisibility(View.INVISIBLE);
        holder.mWishIco2.setVisibility(View.INVISIBLE);
        holder.mWishIco.setVisibility(View.VISIBLE);
        holder.mProgressBar.setVisibility(View.VISIBLE);

        // Wish Counter
        if (mCounter == 0) {

            holder.mCountOfWishes.setText(" ");

        }
        else {

            holder.mCountOfWishes.setVisibility(View.VISIBLE);
            if (tip.getInt(ParseConstants.KEY_WISHES_COUNT) == 1) {
                holder.mCountOfWishes.setText(String.valueOf(tip.getInt(ParseConstants.KEY_WISHES_COUNT)) + " " + "wish");
            }else {
                holder.mCountOfWishes.setText(String.valueOf(tip.getInt(ParseConstants.KEY_WISHES_COUNT)) + " " + "wishes");
            }
        }

        try {
            mUrl = new URL("https://graph.facebook.com/"+tip.getString(ParseConstants.KEY_FACEBOOK_SENDER_ID)+"/picture?type=large&return_ssl_resources=1");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Picasso.with(mContext).load(String.valueOf(mUrl)).fit().transform(new RoundedTransformation(140, 0))
                .error(R.drawable.no_user_ico)
                .into(holder.mUserProfilePictureView);

        ParseFile file = tip.getParseFile(ParseConstants.KEY_FILE);

        Picasso.with(mContext).load(file.getUrl())
                .error(R.drawable.error_image).into(holder.productView, new Callback() {
            @Override
            public void onSuccess() {
                holder.mProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError() {
                holder.mProgressBar.setVisibility(View.INVISIBLE);
            }
        });

        holder.productView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                goToProductProfile(tip);

            }
        });


        holder.productName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                goToProductProfile(tip);
            }
        });


        holder.merchantNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToMerchantProfile(tip);
            }
        });



        holder.posterName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToUserProfile(tip);
            }
        });

        holder.mUserProfilePictureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToUserProfile(tip);
            }
        });

        mCounter2 = 0;
        mCounter3 = 0;

        //Wish button
        if (mUserId.equals(mSenderId)){

            holder.mWishIco.setImageResource(R.drawable.wish_selected);
            holder.mCountOfWishes.setVisibility(View.VISIBLE);
            holder.mCountOfWishes2.setVisibility(View.INVISIBLE);

            holder.mWishIco.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Toast.makeText(mContext, R.string.wish_already_taken, Toast.LENGTH_LONG).show();

                }
            });

        }

        else {

            holder.mWishIco.setImageResource(R.drawable.wish_empty);

            if (mRelationship.size() != 0) {

                for (int i = 0; i < mRelationship.size(); i++) {

                    ParseObject relation = mRelationship.get(i);

                    if (relation.getObjectId().equals(mObjectId)) {

                        holder.mWishIco.setImageResource(R.drawable.wish_selected);

                        removeWish(holder, tip);
                        break;

                    }
                    else {

                        addNewWish(holder, tip);

                    }
                }
            }else {

                addNewWish(holder, tip);

            }
        }


        //Status Bar
        if(tip.getInt(ParseConstants.KEY_WISHES_COUNT)>=0 && tip.getInt(ParseConstants.KEY_WISHES_COUNT)<=10) {
            holder.mStatusBar.setImageResource(R.drawable.freezing);
            holder.mStatusText.setText("Freezing");
        }else if(tip.getInt(ParseConstants.KEY_WISHES_COUNT)>=11 && tip.getInt(ParseConstants.KEY_WISHES_COUNT)<=30){
            holder.mStatusBar.setImageResource(R.drawable.cold);
            holder.mStatusText.setText("Cold");
        }else if(tip.getInt(ParseConstants.KEY_WISHES_COUNT)>=31 && tip.getInt(ParseConstants.KEY_WISHES_COUNT)<=50){
            holder.mStatusBar.setImageResource(R.drawable.warm);
            holder.mStatusText.setText("Warming up");
        }else if(tip.getInt(ParseConstants.KEY_WISHES_COUNT)>=51 && tip.getInt(ParseConstants.KEY_WISHES_COUNT)<=80){
            holder.mStatusBar.setImageResource(R.drawable.hot);
            holder.mStatusText.setText("Hot");
        }else if(tip.getInt(ParseConstants.KEY_WISHES_COUNT)>=81){
            holder.mStatusBar.setImageResource(R.drawable.fire);
            holder.mStatusText.setText("On Fire!!");
        }




        return convertView;

    }

    private void removeWish(final ViewHolder holder, final ParseObject tip) {
        holder.mWishIco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (mCounter3 != 1) {

                    holder.mWishIco.setVisibility(View.INVISIBLE);
                    holder.mWishIco2.setVisibility(View.VISIBLE);
                    holder.mWishIco2.setBackgroundResource(R.drawable.wish_empty);

                    mCounter3 = 1;

                    holder.mCountOfWishes.setVisibility(View.INVISIBLE);

                    int subCounter = tip.getInt(ParseConstants.KEY_WISHES_COUNT);
                    if (subCounter-1 == 0){
                        holder.mCountOfWishes2.setText(" ");
                    }
                    else {
                        if (tip.getInt(ParseConstants.KEY_WISHES_COUNT)-1 == 1) {
                            holder.mCountOfWishes2.setText(String.valueOf(tip.getInt(ParseConstants.KEY_WISHES_COUNT) - 1) + " " + "wish");
                        }else {
                            holder.mCountOfWishes2.setText(String.valueOf(tip.getInt(ParseConstants.KEY_WISHES_COUNT) - 1) + " " + "wishes");
                        }
                    }
                    holder.mCountOfWishes2.setVisibility(View.VISIBLE);

                    Toast.makeText(mContext, R.string.product_removed, Toast.LENGTH_LONG).show();

                    //Decrement the wish count of the product
                    tip.increment(ParseConstants.KEY_WISHES_COUNT, -1);
                    tip.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                Log.d("Wish Count error:", e.getMessage());
                            }

                        }
                    });

                    //Removing relationship between the user and the wish product
                    ParseRelation<ParseObject> userProductRelation = mCurrentUser.getRelation(ParseConstants.KEY_USER_PRODUCT_RELATION);
                    userProductRelation.remove(tip);
                    mCurrentUser.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null){

                                //Fetching the new relationship user products
                                mUserProducts = mCurrentUser.getRelation(ParseConstants.KEY_USER_PRODUCT_RELATION);
                                ParseQuery<ParseObject> userProducts = mUserProducts.getQuery();
                                userProducts.findInBackground(new FindCallback<ParseObject>() {
                                    @Override
                                    public void done(List<ParseObject> parseObjects, ParseException e) {

                                        if (e == null){
                                            mRelationship = parseObjects;
                                        }
                                        else {

                                            Log.d("Relationship Error:", e.getMessage());
                                        }

                                    }
                                });
                            }
                            else {

                                Log.d("Relationship Error:", e.getMessage());
                            }
                        }
                    });
                }


            }
        });
    }

    private void addNewWish(final ViewHolder holder, final ParseObject tip) {
        holder.mWishIco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mCounter2 != 1) {


                    holder.mWishIco.setVisibility(View.INVISIBLE);
                    holder.mWishIco2.setVisibility(View.VISIBLE);
                    holder.mWishIco2.setBackgroundResource(R.drawable.wish_selected);

                    mRelationship.add(tip);
                    mCounter2 = 1;

                    holder.mCountOfWishes.setVisibility(View.INVISIBLE);
                    if (tip.getInt(ParseConstants.KEY_WISHES_COUNT) + 1 == 1) {
                        holder.mCountOfWishes2.setText(String.valueOf(tip.getInt(ParseConstants.KEY_WISHES_COUNT) + 1) + " " + "wish");
                    }else {
                        holder.mCountOfWishes2.setText(String.valueOf(tip.getInt(ParseConstants.KEY_WISHES_COUNT) + 1) + " " + "wishes");
                    }
                    holder.mCountOfWishes2.setVisibility(View.VISIBLE);

                    Toast.makeText(mContext, R.string.added_to_list, Toast.LENGTH_LONG).show();

                    //Incrementing the wish count of the product
                    tip.increment(ParseConstants.KEY_WISHES_COUNT);
                    tip.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                Log.d("Wish Count error:", e.getMessage());
                            }

                        }
                    });

                    //Setting relationship between the user and the wish product
                    ParseRelation<ParseObject> userProductRelation = mCurrentUser.getRelation(ParseConstants.KEY_USER_PRODUCT_RELATION);
                    userProductRelation.add(tip);
                    mCurrentUser.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {

                                Log.d("Relationship Error:", e.getMessage());

                            }
                        }
                    });

                } else {

                }

            }
        });
    }

    private void goToMerchantProfile(ParseObject tip) {
        Intent intent = new Intent(mContext ,MerchantProfile2.class);
        intent.putExtra("merchantName", tip.getString(ParseConstants.KEY_MERCHANT_NAME));
        ParseGeoPoint merchantLocation = tip.getParseGeoPoint(ParseConstants.KEY_MERCHANT_LOCATION);
        double merchantLatitude = merchantLocation.getLatitude();
        double merchantLongitude = merchantLocation.getLongitude();

        intent.putExtra("merchantLatitude", merchantLatitude);
        intent.putExtra("merchantLongitude", merchantLongitude);
        mContext.startActivity(intent);
    }

    private void goToProductProfile(ParseObject tip) {
        ParseFile file = tip.getParseFile(ParseConstants.KEY_FILE);
        String imageUrl = file.getUrl();

        Intent intent = new Intent(mContext, ProductDetail.class);
        intent.putExtra("productName", tip.getString(ParseConstants.KEY_PRODUCT_NAME));
        intent.putExtra("merchantName", tip.getString(ParseConstants.KEY_MERCHANT_NAME));
        intent.putExtra("wishCounter", tip.getInt(ParseConstants.KEY_WISHES_COUNT));
        intent.putExtra("productId", tip.getObjectId());
        intent.putExtra("senderName", tip.getString(ParseConstants.KEY_SENDER_NAME));
        intent.putExtra("senderId", tip.getString(ParseConstants.KEY_SENDER_ID));
        intent.putExtra("imageUrl", imageUrl);
        intent.putExtra("comment", tip.getString(ParseConstants.KEY_PRODUCT_COMMENT));
        intent.putExtra("postDate", mDateFormat.format(tip.getCreatedAt()));
        ParseGeoPoint merchantLocation = tip.getParseGeoPoint(ParseConstants.KEY_MERCHANT_LOCATION);
        double merchantLatitude = merchantLocation.getLatitude();
        double merchantLongitude = merchantLocation.getLongitude();

        intent.putExtra("merchantLatitude", merchantLatitude);
        intent.putExtra("merchantLongitude", merchantLongitude);
        intent.putExtra("senderFacebookId", tip.getString(ParseConstants.KEY_FACEBOOK_SENDER_ID));

        mContext.startActivity(intent);
    }

    private void goToUserProfile(ParseObject tip) {

        Intent intent = new Intent(mContext, UserProfile2.class);
        intent.putExtra("userName", tip.getString(ParseConstants.KEY_SENDER_NAME));
        intent.putExtra("userId", tip.getString(ParseConstants.KEY_SENDER_ID));
        mContext.startActivity(intent);

    }

    private static class ViewHolder {

        TextView productName;
        TextView posterName;
        TextView merchantNameView;
        ImageView productView;
        ImageView mWishIco;
        ImageView mWishIco2;
        ImageView mUserProfilePictureView;
        TextView mCountOfWishes;
        TextView mCountOfWishes2;
        ProgressBar mProgressBar;
        TextView mPostDateView;
        ImageView mStatusBar;
        TextView mStatusText;

    }

    public void refill(List<ParseObject> tips) {

        mTips.clear();
        mTips.addAll(tips);
        notifyDataSetChanged();
    }


    public boolean isEnabled(int position)
    {
        return false;
    }


}


