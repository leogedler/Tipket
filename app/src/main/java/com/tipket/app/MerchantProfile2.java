package com.tipket.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;


public class MerchantProfile2 extends Activity {

    public static final String TAG = MerchantProfile2.class.getSimpleName();
    protected ProgressDialog progressDialog;


    //Global Variables
    protected String mMerchantName;
    protected double mMerchantLatitude;
    protected double mMerchantLongitude;
    protected ParseObject mMerchant;
    protected List<ParseObject> mMerchantProducts;
    protected TextView mMerchantHeaderName;
    protected GridView mGridView;
    protected ImageView mListIcon;

    //User name
    protected String mUserName;

    // Global Google constant
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_profile2);

        mUserName = ParseUser.getCurrentUser().getUsername();

        mGridView = (GridView) findViewById(R.id.gridView2);

        progressDialog = ProgressDialog.show(MerchantProfile2.this, "", getString(R.string.loading_label));


        mMerchantName = getIntent().getExtras().getString("merchantName");
        mMerchantLatitude = getIntent().getExtras().getDouble("merchantLatitude");
        mMerchantLongitude = getIntent().getExtras().getDouble("merchantLongitude");
        mMerchantHeaderName = (TextView) findViewById(R.id.merchantHeaderName);

        mMerchantHeaderName.setText(StringHelper.capitalize(mMerchantName));

        //Setting up the map
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView6)).getMap();
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                Intent intent = new Intent(MerchantProfile2.this, MapViewActivity.class);
                intent.putExtra("merchantName", mMerchantName);
                intent.putExtra("merchantLatitude", mMerchantLatitude);
                intent.putExtra("merchantLongitude", mMerchantLongitude);
                intent.putExtra("productName", "No product");
                intent.putExtra("booleanCheck", false);
                startActivity(intent);

            }
        });


        //Go to merchant profile list view
        mListIcon = (ImageView) findViewById(R.id.listMerchantUnSelected);
        mListIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MerchantProfile2.this,MerchantProfile.class);
                intent.putExtra("merchantName", mMerchantName);
                intent.putExtra("merchantLatitude", mMerchantLatitude);
                intent.putExtra("merchantLongitude", mMerchantLongitude);
                startActivity(intent);
            }
        });



        ParseGeoPoint merchantLoc = new ParseGeoPoint(mMerchantLatitude, mMerchantLongitude);

        // Fetching Merchant Info
        ParseQuery<ParseObject> merchantObject = new ParseQuery<ParseObject>(ParseConstants.CLASS_MERCHANTS);
        merchantObject.whereWithinKilometers(ParseConstants.KEY_MERCHANT_LOCATION, merchantLoc, 0.01);
        merchantObject.whereEqualTo(ParseConstants.KEY_MERCHANT_NAME, mMerchantName);
        merchantObject.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (e == null){

                   mMerchant = parseObject;

                    final ParseQuery<ParseObject> merchantProducts = ParseQuery.getQuery(ParseConstants.CLASS_TIPS);
                    merchantProducts.whereEqualTo(ParseConstants.KEY_PRODUCT_MERCHANT_RELATION, mMerchant);
                    merchantProducts.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> parseObjects, ParseException e) {
                            if (e == null){
                                progressDialog.dismiss();

                                mMerchantProducts = parseObjects;

                                if (mMerchantProducts.size() !=0 ){

                                    mGridView.setAdapter(new TipMerchantGridAdapter(MerchantProfile2.this, mMerchantProducts));

                                }
                                else {
                                    progressDialog.dismiss();
                                    mMerchantProducts.clear();
                                }

                            }
                            else {
                                progressDialog.dismiss();
                                Log.d("Error fetching products", e.getMessage());

                                AlertDialog.Builder builder = new AlertDialog.Builder(MerchantProfile2.this);
                                builder.setMessage(R.string.error_merchant_profile);
                                builder.setCancelable(false);

                                builder.setPositiveButton(R.string.go_to_feed, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        Intent intent = new Intent(MerchantProfile2.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);

                                    }
                                });

                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }

                        }

                    });

                }
                else {
                    progressDialog.dismiss();
                    Log.d("Error fetching merchant info", e.getMessage());

                    AlertDialog.Builder builder = new AlertDialog.Builder(MerchantProfile2.this);
                    builder.setMessage(R.string.error_merchant_profile);
                    builder.setCancelable(false);

                    builder.setPositiveButton(R.string.go_to_feed, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            Intent intent = new Intent(MerchantProfile2.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        mMap.addMarker(new MarkerOptions().position(new LatLng(mMerchantLatitude, mMerchantLongitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin_far)));

        LatLng loc = new LatLng(mMerchantLatitude, mMerchantLongitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                marker.hideInfoWindow();

                Intent intent = new Intent(MerchantProfile2.this, MapViewActivity.class);
                intent.putExtra("merchantName", mMerchantName);
                intent.putExtra("merchantLatitude", mMerchantLatitude);
                intent.putExtra("merchantLongitude", mMerchantLongitude);
                intent.putExtra("productName", "No product");
                intent.putExtra("booleanCheck", false);
                startActivity(intent);

                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.merchant_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_tip) {

            AlertDialog.Builder builder = new AlertDialog.Builder(MerchantProfile2.this);
            builder.setMessage(R.string.question_label);

            builder.setPositiveButton(R.string.yes_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(MerchantProfile2.this, TipInPlaceActivity.class);
                    startActivity(intent);

                }
            });
            builder.setNegativeButton(R.string.no_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(MerchantProfile2.this, TipNotInPlaceActivity.class);
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

            Intent intent = new Intent(MerchantProfile2.this, UserProfile2.class);
            intent.putExtra("userName", mUserName);
            intent.putExtra("userId", actualUserId);
            startActivity(intent);

        }
        return super.onOptionsItemSelected(item);
    }

}
