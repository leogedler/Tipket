package com.tipket.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.widget.WebDialog;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


public class ProductDetail extends FragmentActivity {

    protected ProgressDialog progressDialog;

    public static final String TAG = ProductDetail.class.getSimpleName();

    // Global Google constant
    private GoogleMap mMap;

    //Members variables for Product Detail
    protected String mProductName;
    protected String mObjectId;
    protected String mMerchantName = null;
    protected TextView mMerchantNameView;
    protected TextView mProductNameView;
    protected TextView mCommentView;
    protected ImageView mImageProductView;
    protected String mImageUrl;
    protected double mMerchantLatitude;
    protected double mMerchantLongitude;
    protected String mPosterName;
    protected String mPosterId;
    protected TextView mPosterNameView;
    protected String mComment;
    protected String mPosterFacebookId;
    protected ImageView mPosterProfilePicture;
    protected ParseRelation<ParseObject> mUserProducts;
    protected ParseUser mCurrentUser = ParseUser.getCurrentUser();
    protected List<ParseObject> mRelationship;
    protected String mUserId;
    protected TextView mCountOfWishes;
    protected TextView mCountOfWishes2;
    protected int mCounter;
    protected ParseObject mCurrentProduct;
    protected URL mUrl;
    protected ImageView mShareInFacebook;
    protected String mPostDate;
    protected TextView mPostDateView;
    protected ImageView mStatusBar;
    protected TextView mStatusText;

    //User name
    protected String mUserName;

    //Wish buttons
    protected ImageView mWishIco;
    protected ImageView mWishIco2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);


        mUserName = ParseUser.getCurrentUser().getUsername();

        mProductName = getIntent().getExtras().getString("productName");
        mObjectId = getIntent().getExtras().getString("productId");
        mMerchantName = getIntent().getExtras().getString("merchantName");
        mPosterName = getIntent().getExtras().getString("senderName");
        mPosterId = getIntent().getExtras().getString("senderId");
        mImageUrl = getIntent().getExtras().getString("imageUrl");
        mMerchantLatitude = getIntent().getExtras().getDouble("merchantLatitude");
        mMerchantLongitude = getIntent().getExtras().getDouble("merchantLongitude");
        mComment = getIntent().getExtras().getString("comment");
        mPosterFacebookId = getIntent().getExtras().getString("senderFacebookId");
        mCounter = getIntent().getExtras().getInt("wishCounter");
        mPostDate = getIntent().getExtras().getString("postDate");
        mUserId = mCurrentUser.getObjectId();


        mWishIco = (ImageView) findViewById(R.id.wishIco3);
        mWishIco2 = (ImageView) findViewById(R.id.wishIco4);

        mCountOfWishes = (TextView) findViewById(R.id.countOfWishes3);
        mCountOfWishes2 = (TextView) findViewById(R.id.countOfWishes4);

        mCountOfWishes2.setVisibility(View.INVISIBLE);
        mWishIco2.setVisibility(View.INVISIBLE);
        mWishIco.setVisibility(View.VISIBLE);


        if (mCounter == 0) {

            mCountOfWishes.setText(" ");

        }
        else {

            mCountOfWishes.setVisibility(View.VISIBLE);
            if (mCounter == 1) {
                mCountOfWishes.setText(String.valueOf(mCounter) + " " +"wish");
            }else {
                mCountOfWishes.setText(String.valueOf(mCounter) + " " +"wishes");
            }

        }

        // Fetching the productInfo
        ParseQuery<ParseObject> countQuery = new ParseQuery(ParseConstants.CLASS_TIPS);
        countQuery.getInBackground(mObjectId, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {

                if (e == null) {
                    mCurrentProduct = parseObject;

                    //Wish button
                    if (mUserId.equals(mPosterId)){


                        mWishIco.setBackgroundResource(R.drawable.wish_selected);
                        mCountOfWishes.setVisibility(View.VISIBLE);
                        mCountOfWishes2.setVisibility(View.INVISIBLE);

                        mWishIco.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                Toast.makeText(ProductDetail.this, R.string.wish_already_taken, Toast.LENGTH_LONG).show();

                            }
                        });

                    }
                    else {

                        //Fetching relationship user products
                        mUserProducts = mCurrentUser.getRelation(ParseConstants.KEY_USER_PRODUCT_RELATION);
                        ParseQuery<ParseObject> userProducts = mUserProducts.getQuery();
                        userProducts.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> parseObjects, ParseException e) {
                                if (e == null) {

                                    mRelationship = parseObjects;

                                    if (mRelationship.size() != 0) {

                                        mWishIco.setImageResource(R.drawable.wish_empty);

                                        for (int i = 0; i < mRelationship.size(); i++) {

                                            ParseObject relation = mRelationship.get(i);

                                            if (relation.getObjectId().equals(mObjectId)) {

                                                mWishIco.setImageResource(R.drawable.wish_selected);

                                                removeWish();
                                                break;

                                            } else {

                                                addNewWish();

                                            }
                                        }

                                    }else {
                                        addNewWish();

                                    }
                                }
                            }
                        });

                    }

                }
                else {
                    Log.d ("Problem fetching product:", e.getMessage());
                }
            }
        });

        //Setting up the map
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView3)).getMap();
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                Intent intent = new Intent(ProductDetail.this, MapViewActivity.class);
                intent.putExtra("merchantLatitude", mMerchantLatitude);
                intent.putExtra("merchantLongitude", mMerchantLongitude);
                intent.putExtra("merchantName", mMerchantName);
                intent.putExtra("productName", mProductName);
                intent.putExtra("booleanCheck", true);
                startActivity(intent);

            }
        });


        mPosterProfilePicture = (ImageView) findViewById(R.id.posterProfilePicture);

        try {
            mUrl = new URL("https://graph.facebook.com/"+mPosterFacebookId+"/picture?type=large&return_ssl_resources=1");
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
        }
        Picasso.with(this).load(String.valueOf(mUrl)).fit().transform(new RoundedTransformation(150, 0))
                .error(R.drawable.no_user_ico).into(mPosterProfilePicture);

        mPosterProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                goToUserProfile();

            }
        });

        mProductNameView = (TextView) findViewById(R.id.productNameDetail);
        mProductNameView.setText(StringHelper.capitalize(mProductName));
        mMerchantNameView = (TextView) findViewById(R.id.merchantNameLabel);
        mMerchantNameView.setText("@"+ StringHelper.capitalize(mMerchantName));
        mCommentView = (TextView) findViewById(R.id.comment);
        mCommentView.setText(mComment);
        mPostDateView = (TextView) findViewById(R.id.postDateProductDetail);
        mPostDateView.setText(mPostDate);


        mImageProductView = (ImageView) findViewById(R.id.productDetailView);
        Picasso.with(this).load(mImageUrl).error(R.drawable.error_image).placeholder(R.drawable.loading).into(mImageProductView);

        mPosterNameView = (TextView) findViewById(R.id.posterUserView);
        mPosterNameView.setText(mPosterName);
        mPosterNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                goToUserProfile();

            }
        });

        mMerchantNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToMerchantProfile();
                Log.d("Merchant Name :" , mMerchantName);
            }
        });

        mShareInFacebook = (ImageView) findViewById(R.id.shareByFacebook);
        mShareInFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ParseFacebookUtils.getSession() != null) {
                    publishFeedDialog();
                }else {

                    AlertDialog.Builder builder = new AlertDialog.Builder(ProductDetail.this);
                    builder.setTitle(R.string.no_session_facebook_tittle);
                    builder.setMessage(R.string.no_session_facebook_message);

                    builder.setPositiveButton(R.string.go_to_sign_up_with_facebook, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            ParseUser.logOut();

                            Intent intent = new Intent(ProductDetail.this, PreLaunchActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    });

                    builder.setNegativeButton(R.string.cancel_facebook_action, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                }

            }
        });

        //Status Bar
        mStatusBar = (ImageView) findViewById(R.id.statusBar2);
        mStatusText = (TextView) findViewById(R.id.statusText2);
        if(mCounter>=0 && mCounter<=10) {
            mStatusBar.setImageResource(R.drawable.freezing);
            mStatusText.setText("Freezing");
        }else if(mCounter>=11 && mCounter<=30){
            mStatusBar.setImageResource(R.drawable.cold);
            mStatusText.setText("Cold");
        }else if(mCounter>=31 && mCounter<=50){
            mStatusBar.setImageResource(R.drawable.warm);
            mStatusText.setText("Warming up");
        }else if(mCounter>=51 && mCounter<=80){
            mStatusBar.setImageResource(R.drawable.hot);
            mStatusText.setText("Hot");
        }else if(mCounter>=81){
            mStatusBar.setImageResource(R.drawable.fire);
            mStatusText.setText("On Fire!!");
        }

    }

    private void removeWish() {
        mWishIco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mWishIco.setVisibility(View.INVISIBLE);
                mWishIco2.setVisibility(View.VISIBLE);
                mWishIco2.setImageResource(R.drawable.wish_empty);

                mCountOfWishes.setVisibility(View.INVISIBLE);

                int subCounter = mCounter;
                if (subCounter - 1 == 0) {
                    mCountOfWishes2.setText(" ");
                } else {
                    if (mCounter -1 == 1) {
                        mCountOfWishes2.setText(String.valueOf(mCounter - 1)+ " " + "wish");
                    }else {
                        mCountOfWishes2.setText(String.valueOf(mCounter - 1)+ " " + "wishes");
                    }
                }
                mCountOfWishes2.setVisibility(View.VISIBLE);

                Toast.makeText(ProductDetail.this, R.string.product_removed, Toast.LENGTH_LONG).show();

                //Decrement the wish count of the product
                mCurrentProduct.increment(ParseConstants.KEY_WISHES_COUNT, -1);
                mCurrentProduct.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Log.d("Wish Count error:", e.getMessage());
                        }

                    }
                });

                //Removing relationship between the user and the wish product
                ParseRelation<ParseObject> userProductRelation = mCurrentUser.getRelation(ParseConstants.KEY_USER_PRODUCT_RELATION);
                userProductRelation.remove(mCurrentProduct);
                mCurrentUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {

                        } else {

                            Log.d("Relationship Error:", e.getMessage());
                        }
                    }
                });


                Intent intent = new Intent(ProductDetail.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            }
        });
    }

    private void addNewWish() {
        mWishIco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mWishIco.setVisibility(View.INVISIBLE);
                mWishIco2.setVisibility(View.VISIBLE);
                mWishIco2.setBackgroundResource(R.drawable.wish_selected);

                mRelationship.add(mCurrentProduct);

                mCountOfWishes.setVisibility(View.INVISIBLE);
                if (mCounter + 1 == 1) {
                    mCountOfWishes2.setText(String.valueOf(mCounter + 1) + " " + "wish");
                }else {
                    mCountOfWishes2.setText(String.valueOf(mCounter + 1) + " " + "wishes");
                }
                mCountOfWishes2.setVisibility(View.VISIBLE);

                Toast.makeText(ProductDetail.this, R.string.added_to_list, Toast.LENGTH_LONG).show();

                //Incrementing the wish count of the product
                mCurrentProduct.increment(ParseConstants.KEY_WISHES_COUNT);
                mCurrentProduct.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            Log.d("Wish Count error:", e.getMessage());
                        }

                    }
                });

                //Setting relationship between the user and the wish product
                ParseRelation<ParseObject> userProductRelation = mCurrentUser.getRelation(ParseConstants.KEY_USER_PRODUCT_RELATION);
                userProductRelation.add(mCurrentProduct);
                mCurrentUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {

                            Log.d("Relationship Error:", e.getMessage());

                        }
                    }
                });

                Intent intent = new Intent(ProductDetail.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            }
        });
    }

    private void goToMerchantProfile() {

        Intent intent = new Intent(this,MerchantProfile2.class);
        intent.putExtra("merchantName", mMerchantName);
        intent.putExtra("merchantLatitude", mMerchantLatitude);
        intent.putExtra("merchantLongitude", mMerchantLongitude);
        startActivity(intent);

    }

    private void goToUserProfile() {

        Intent intent = new Intent(ProductDetail.this, UserProfile2.class);
        intent.putExtra("userName", mPosterName);
        intent.putExtra("userId", mPosterId);
        startActivity(intent);

    }

    @Override
    protected void onResume() {
        super.onResume();

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(mMerchantLatitude, mMerchantLongitude))
                .title(mMerchantName).icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin)));

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                marker.hideInfoWindow();

                Intent intent = new Intent(ProductDetail.this, MapViewActivity.class);
                intent.putExtra("merchantLatitude", mMerchantLatitude);
                intent.putExtra("merchantLongitude", mMerchantLongitude);
                intent.putExtra("merchantName", mMerchantName);
                intent.putExtra("productName", mProductName);
                intent.putExtra("booleanCheck", true);
                startActivity(intent);


                return true;
            }
        });

        LatLng productMaker = new LatLng(mMerchantLatitude, mMerchantLongitude);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(productMaker, 15));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.product_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_tip) {

            AlertDialog.Builder builder = new AlertDialog.Builder(ProductDetail.this);
            builder.setMessage(R.string.question_label);

            builder.setPositiveButton(R.string.yes_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(ProductDetail.this, TipInPlaceActivity.class);
                    startActivity(intent);

                }
            });
            builder.setNegativeButton(R.string.no_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(ProductDetail.this, TipNotInPlaceActivity.class);
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

            String actualUserId = ParseUser.getCurrentUser().getObjectId();

            Intent intent = new Intent(ProductDetail.this, UserProfile2.class);
            intent.putExtra("userName", mUserName);
            intent.putExtra("userId", actualUserId);
            startActivity(intent);

        }
        return super.onOptionsItemSelected(item);
    }

    private void publishFeedDialog() {
        Bundle params = new Bundle();
        params.putString("name", "Look what i found in Tipket");
        params.putString("caption", "at" + " " + StringHelper.capitalize(mMerchantName));
        params.putString("description", StringHelper.capitalize(mProductName));
        params.putString("link", "http://www.tipket.com");
        params.putString("picture", mImageUrl);
        //params.putString("picture", "http://www.tipket.com/Images/logo2.png");

        WebDialog feedDialog = (
                new WebDialog.FeedDialogBuilder(this,
                        Session.getActiveSession(),
                        params))
                .setOnCompleteListener(new WebDialog.OnCompleteListener() {

                    @Override
                    public void onComplete(Bundle values,
                                           FacebookException error) {
                        if (error == null) {
                            // When the story is posted, echo the success
                            // and the post Id.
                            final String postId = values.getString("post_id");
                            if (postId != null) {
                                Toast.makeText(ProductDetail.this,
                                        R.string.post_done,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // User clicked the Cancel button
                                Toast.makeText(ProductDetail.this.getApplicationContext(),
                                        R.string.post_canceled,
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else if (error instanceof FacebookOperationCanceledException) {
                            // User clicked the "x" button
                            Toast.makeText(ProductDetail.this.getApplicationContext(),
                                    R.string.post_canceled,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Generic, ex: network error
                            Toast.makeText(ProductDetail.this.getApplicationContext(),
                                    R.string.error_post,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                })
                .build();
        feedDialog.show();
    }

}
