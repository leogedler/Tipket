package com.tipket.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by leonardogedler on 21/03/2014.
 */
public class TipMerchantAdapter extends ArrayAdapter<ParseObject> {

    protected Context mContext;
    protected List<ParseObject> mProducts;
    protected Format mDateFormat;

    public TipMerchantAdapter(Context context, List<ParseObject> products) {
        super(context, R.layout.merchant_tip, products);

        mContext = context;
        mProducts = products;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.merchant_tip, null);
            holder = new ViewHolder();
            holder.productName = (TextView) convertView.findViewById(R.id.productMerchantNameView);
            holder.merchantNameView = (TextView) convertView.findViewById(R.id.merchantNameView);
            holder.productView = (ImageView) convertView.findViewById(R.id.productMerchantView);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar6);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        mDateFormat = new SimpleDateFormat("dd MMM");

        holder.progressBar.setVisibility(View.VISIBLE);

        final ParseObject product = mProducts.get(position);
        holder.productName.setText(StringHelper.capitalize(product.getString(ParseConstants.KEY_PRODUCT_NAME)));
        holder.merchantNameView.setText(" " + "@" + " " + StringHelper.capitalize(product.getString(ParseConstants.KEY_MERCHANT_NAME)));

        ParseFile file = product.getParseFile(ParseConstants.KEY_FILE);

        Picasso.with(mContext).load(file.getUrl()).error(R.drawable.error_image)
                .into(holder.productView, new Callback() {
                    @Override
                    public void onSuccess() {
                        holder.progressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError() {
                        holder.progressBar.setVisibility(View.INVISIBLE);
                    }
                });

        holder.productView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToProductProfile(product);
            }
        });


        return convertView;

    }

    private static class ViewHolder {

        TextView productName;
        TextView merchantNameView;
        ImageView productView;
        ProgressBar progressBar;
    }

    public void refill(List<ParseObject> tips) {

        mProducts.clear();
        mProducts.addAll(tips);
        notifyDataSetChanged();
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
        intent.putExtra("postDate",mDateFormat.format(tip.getCreatedAt()));
        ParseGeoPoint merchantLocation = tip.getParseGeoPoint(ParseConstants.KEY_MERCHANT_LOCATION);
        double merchantLatitude = merchantLocation.getLatitude();
        double merchantLongitude = merchantLocation.getLongitude();

        intent.putExtra("merchantLatitude", merchantLatitude);
        intent.putExtra("merchantLongitude", merchantLongitude);
        intent.putExtra("senderFacebookId", tip.getString(ParseConstants.KEY_FACEBOOK_SENDER_ID));

        mContext.startActivity(intent);
    }

    public boolean isEnabled(int position)
    {
        return false;
    }


}
