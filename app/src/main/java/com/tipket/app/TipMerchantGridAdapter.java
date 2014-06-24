package com.tipket.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.squareup.picasso.Picasso;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by leonardogedler on 21/03/2014.
 */
class TipMerchantGridAdapter extends BaseAdapter {
    private Context context;
    private final List<ParseObject> merchantProducts;
    protected Format mDateFormat;

    ImageView imageView;

    public TipMerchantGridAdapter(Context context, List<ParseObject> userProducts) {
        this.context = context;
        this.merchantProducts = userProducts;


    }

    public View getView(int position, View convertView, ViewGroup parent) {

        View gridView;

        if (convertView == null) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // get layout from tip_user_grid.xml
            gridView = inflater.inflate(R.layout.merchant_grid, parent, false);

        } else {
            gridView = (View) convertView;
        }

        // set image
        imageView = (ImageView) gridView.findViewById(R.id.productMerchantView2);

        final ParseObject product = merchantProducts.get(position);

        ParseFile file = product.getParseFile(ParseConstants.KEY_FILE);

        Picasso.with(context).load(file.getUrl()).error(R.drawable.error_image).into(imageView);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToProductProfile(product);
            }
        });

        mDateFormat = new SimpleDateFormat("dd MMM");

        return gridView;
    }

    @Override
    public int getCount() {
        return merchantProducts.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


    private void goToProductProfile(ParseObject tip) {
        ParseFile file = tip.getParseFile(ParseConstants.KEY_FILE);
        String imageUrl = file.getUrl();

        Intent intent = new Intent(context, ProductDetail.class);
        intent.putExtra("productName", tip.getString(ParseConstants.KEY_PRODUCT_NAME));
        intent.putExtra("merchantName", tip.getString(ParseConstants.KEY_MERCHANT_NAME));
        intent.putExtra("wishCounter", tip.getInt(ParseConstants.KEY_WISHES_COUNT));
        intent.putExtra("productId", tip.getObjectId());
        intent.putExtra("senderName", tip.getString(ParseConstants.KEY_SENDER_NAME));
        intent.putExtra("senderId", tip.getString(ParseConstants.KEY_SENDER_ID));
        intent.putExtra("imageUrl", imageUrl);
        intent.putExtra("comment", tip.getString(ParseConstants.KEY_PRODUCT_COMMENT));
        intent.putExtra("postDate",mDateFormat.format(tip.getCreatedAt()));
        ParseGeoPoint merchantLocation = tip.getParseGeoPoint(ParseConstants.KEY_MERCHANT_LOCATION);
        double merchantLatitude = merchantLocation.getLatitude();
        double merchantLongitude = merchantLocation.getLongitude();

        intent.putExtra("merchantLatitude", merchantLatitude);
        intent.putExtra("merchantLongitude", merchantLongitude);
        intent.putExtra("senderFacebookId", tip.getString(ParseConstants.KEY_FACEBOOK_SENDER_ID));

        context.startActivity(intent);
    }



}