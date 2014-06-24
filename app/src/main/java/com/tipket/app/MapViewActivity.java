package com.tipket.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapViewActivity extends FragmentActivity {

    // Global Google constant
    private GoogleMap mMap;

    //Global varibles for Product Detail
    protected String mProductName;
    protected String mMerchantName;
    protected boolean mBooleanCheck;
    protected double mMerchantLatitude;
    protected double mMerchantLongitude;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);


        mProductName = getIntent().getExtras().getString("productName");
        mMerchantName = getIntent().getExtras().getString("merchantName");
        mMerchantLatitude = getIntent().getExtras().getDouble("merchantLatitude");
        mMerchantLongitude = getIntent().getExtras().getDouble("merchantLongitude");
        mBooleanCheck = getIntent().getExtras().getBoolean("booleanCheck");

        //Setting up the map
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView4)).getMap();
        mMap.setMyLocationEnabled(true);


        if (mBooleanCheck == true) {

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mMerchantLatitude, mMerchantLongitude))
                    .title(StringHelper.capitalize(mMerchantName)).icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin)));
        }else {

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mMerchantLatitude, mMerchantLongitude))
                    .title(StringHelper.capitalize(mMerchantName)).icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin_far)));

        }

            LatLng productMaker = new LatLng(mMerchantLatitude, mMerchantLongitude);

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(productMaker, 15));

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {

                    Intent intent = new Intent(MapViewActivity.this, MerchantProfile2.class);
                    intent.putExtra("merchantLatitude", mMerchantLatitude);
                    intent.putExtra("merchantLongitude", mMerchantLongitude);
                    intent.putExtra("merchantName", mMerchantName);
                    startActivity(intent);
                }
            });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

}

