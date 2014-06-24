package com.tipket.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class TipNotInPlaceActivity extends Activity {
    public static final String TAG = TipNotInPlaceActivity.class.getSimpleName();

    // Tip  Constants
    protected EditText mProductName;
    protected EditText mProductComment;
    protected ImageButton mSendTipButton;
    protected ImageButton mCameraButton;
    protected ImageView mPreviewImage;
    protected ImageView mEraseImage;
    protected EditText mMerchantName;
    protected EditText mCityAddress;
    protected EditText mStreetAddress;
    protected EditText mPostCode;
    protected Bitmap mBitmapToRotate;
    protected ImageView mListOfMerchantsNames;
    protected TextView mNameOfMerchantSelected;
    protected ImageView mEraseMerchantSelected;
    private final Map<Integer, ParseObject> mMerchantsObjects = new HashMap<Integer, ParseObject>();

    protected ProgressDialog progressDialog;


    // Constants for Media Files
    public static final int TAKE_PHOTO_REQUEST = 0;
    public static final int TAKE_VIDEO_REQUEST = 1;
    public static final int PICK_PHOTO_REQUEST = 2;
    public static final int PICK_VIDEO_REQUEST = 3;
    public static final int MEDIA_TYPE_IMAGE = 4;
    public static final int MEDIA_TYPE_VIDEO = 5;
    public static final int FILE_SIZE_LIMIT = 1024*1024*10; // 10 MB

    // Media Uri
    protected Uri mMediaUri;
    protected Uri mFinalMediaUri;
    public String mFileType;

    //Progress Bar
    protected ProgressBar mProgressBar;

    //Parse User
    protected ParseUser mCurrentUser;

    //User, product and merchant variables
    protected String mUserName;
    protected String mUserFacebookId;
    protected String mProductNameString;
    protected String mMerchantNameString;
    protected String mProductCommentString;
    protected String mCityAddressString;
    protected String mStreetAddressString;
    protected String mPostCodeString;
    protected ParseGeoPoint mMyPoint;
    protected double mLatitude;
    protected double mLongitude;

    //Parse Object Merchant Info
    protected ParseObject mMerchantInfo;

    //Orientation pictures
    protected ExifInterface mExif;

    //Rotated bitmap for the preview picture
    protected Bitmap mBitmapRotated;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_not_in_place);


        //Buttons for list of merchant dialog
        mListOfMerchantsNames = (ImageView) findViewById(R.id.listOfMerchantsNames);
        mNameOfMerchantSelected = (TextView) findViewById(R.id.nameOfMerchantSelected);
        mEraseMerchantSelected = (ImageView) findViewById(R.id.eraseMerchantSelected);


        mCityAddress = (EditText) findViewById(R.id.cityAddress);
        mStreetAddress = (EditText) findViewById(R.id.streetAddress);
        mPostCode = (EditText) findViewById(R.id.postCode);
        mProductName = (EditText) findViewById(R.id.productNameField2);
        mProductComment = (EditText) findViewById(R.id.commentField2);
        mProductComment.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mSendTipButton = (ImageButton) findViewById(R.id.sedTip2);

        mMerchantName = (EditText) findViewById(R.id.merchantName2);



        //List of merchants available
        mListOfMerchantsNames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {


                progressDialog = ProgressDialog.show(TipNotInPlaceActivity.this, "",
                        getString(R.string.loading_label));

                //Querying the merchants names
                final ParseQuery<ParseObject> merchants = new ParseQuery<ParseObject>(ParseConstants.CLASS_MERCHANTS);
                merchants.orderByDescending("createdAt");
                merchants.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> merchants, ParseException e) {
                        if (e == null) {

                            progressDialog.dismiss();

                            if (merchants.size() != 0) {

                                String [] listOfMerchantsNames = new String[merchants.size()];

                                int i = 0;
                                for (ParseObject merchant : merchants) {
                                    listOfMerchantsNames[i] = StringHelper.capitalize(merchant.getString(ParseConstants.KEY_MERCHANT_NAME));
                                    mMerchantsObjects.put(i, merchant);
                                    i++;
                                }

                                final ArrayAdapter adapter = new ArrayAdapter
                                        (TipNotInPlaceActivity.this,android.R.layout.simple_list_item_1, listOfMerchantsNames);



                                AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                                        TipNotInPlaceActivity.this);
                                builderSingle.setIcon(R.drawable.launcher_logo);
                                builderSingle.setTitle(R.string.merchant_dialog_tittle);

                                builderSingle.setNegativeButton(R.string.not_listed,
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                                mListOfMerchantsNames.setVisibility(View.INVISIBLE);
                                                mMerchantName.setVisibility(View.VISIBLE);
                                                mMerchantName.setFocusable(true);
                                                mMerchantName.requestFocus();

                                                mCityAddress.setVisibility(View.VISIBLE);
                                                mStreetAddress.setVisibility(View.VISIBLE);
                                                mPostCode.setVisibility(View.VISIBLE);

                                                mMerchantInfo = null;

                                                dialog.dismiss();
                                            }
                                        });


                                builderSingle.setAdapter(adapter,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {


                                                mProductComment.setImeOptions(EditorInfo.IME_ACTION_DONE);

                                                mCityAddress.setVisibility(View.INVISIBLE);
                                                mStreetAddress.setVisibility(View.INVISIBLE);
                                                mPostCode.setVisibility(View.INVISIBLE);


                                                mMerchantInfo = mMerchantsObjects.get(i);

                                                mNameOfMerchantSelected.setVisibility(View.VISIBLE);
                                                mListOfMerchantsNames.setVisibility(View.INVISIBLE);
                                                mEraseMerchantSelected.setVisibility(View.VISIBLE);
                                                mNameOfMerchantSelected.setText(StringHelper.capitalize(mMerchantInfo.getString(ParseConstants.KEY_MERCHANT_NAME)));


                                            }
                                        });
                                builderSingle.show();

                            }else {






                            }
                        }else {

                            progressDialog.dismiss();
                            Log.d("Error fetching merchant names list", e.getMessage());

                            AlertDialog.Builder builder = new AlertDialog.Builder(TipNotInPlaceActivity.this);
                            builder.setMessage(R.string.error_fetching_merchants_names);

                            builder.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    Intent intent = new Intent(TipNotInPlaceActivity.this, TipInPlaceActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });

                            builder.setNegativeButton(R.string.add_place_name_manually, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    mListOfMerchantsNames.setVisibility(View.INVISIBLE);
                                    mMerchantName.setVisibility(View.VISIBLE);
                                    mCityAddress.setVisibility(View.VISIBLE);
                                    mStreetAddress.setVisibility(View.VISIBLE);
                                    mPostCode.setVisibility(View.VISIBLE);

                                    mMerchantInfo = null;

                                    dialog.dismiss();

                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.show();

                        }
                    }
                });



            }
        });


        //Erase merchant selected
        mEraseMerchantSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mMerchantInfo = null;
                mNameOfMerchantSelected.setVisibility(View.INVISIBLE);
                mListOfMerchantsNames.setVisibility(View.VISIBLE);
                mEraseMerchantSelected.setVisibility(View.INVISIBLE);

                mProductComment.setImeOptions(EditorInfo.IME_ACTION_NEXT);

            }
        });




        //Preview image
        mPreviewImage = (ImageView) findViewById(R.id.previewImage2);
        mEraseImage = (ImageView) findViewById(R.id.eraseImage2);

        // Retrieving user info
        mUserName = ParseUser.getCurrentUser().getUsername();
        mUserFacebookId = ParseUser.getCurrentUser().getString(ParseConstants.FACEBOOK_USER_ID);

        mCameraButton = (ImageButton) findViewById(R.id.cameraButton2);
        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(TipNotInPlaceActivity.this);
                builder.setItems(R.array.camera_choices, mDialogListener);
                AlertDialog dialog = builder.create();
                dialog.show();

            }
        });

        //Erase image selected
        mPreviewImage = (ImageView) findViewById(R.id.previewImage2);
        mPreviewImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mFinalMediaUri = null;
                mPreviewImage.setVisibility(View.INVISIBLE);
                mEraseImage.setVisibility(View.INVISIBLE);

            }
        });

        mSendTipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mProductNameString = mProductName.getText().toString().toLowerCase();
                mMerchantNameString =  mMerchantName.getText().toString().toLowerCase();
                mProductCommentString = mProductComment.getText().toString();
                mCityAddressString = mCityAddress.getText().toString();
                mStreetAddressString = mStreetAddress.getText().toString();
                mPostCodeString = mPostCode.getText().toString();

                if (mProductNameString.isEmpty() || mMerchantNameString.isEmpty() && mMerchantInfo == null){
                    AlertDialog.Builder builder = new AlertDialog.Builder(TipNotInPlaceActivity.this);
                    builder.setMessage(R.string.tip_error_message)
                            .setTitle(R.string.tip_error_tittle)
                            .setPositiveButton(android.R.string.ok, null);

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else {

                    if (mFinalMediaUri != null) {

                        if (mMerchantInfo != null){
                            new SendTip().execute();
                        }

                        else {

                            String[] cityStrings = mCityAddressString.split("\\s+");

                            StringBuffer result = new StringBuffer();
                            for (int i = 0; i < cityStrings.length; i++) {
                                result.append( cityStrings[i]+"+");
                            }
                            String completeCityStrings = result.toString();

                            String[] streetStrings = mStreetAddressString.split("\\s+");

                            StringBuffer result2 = new StringBuffer();
                            for (int i = 0; i < streetStrings.length; i++) {
                                result2.append( streetStrings[i]+"+");
                            }
                            String completeStreetStrings = result2.toString();

                            String[] postCodeStrings = mPostCodeString.split("\\s+");

                            StringBuffer result3 = new StringBuffer();
                            for (int i = 0; i < postCodeStrings.length; i++) {
                                result3.append( postCodeStrings[i]+"+");
                            }
                            String completePostCodeStrings = result3.toString();

                            if (completeCityStrings.equals("+") || completeStreetStrings.equals("+")){

                                AlertDialog.Builder builder = new AlertDialog.Builder(TipNotInPlaceActivity.this);
                                builder.setMessage(R.string.tip_missingAddress_message)
                                        .setTitle(R.string.tip_missingAddress_tittle)
                                        .setPositiveButton(android.R.string.ok, null);

                                AlertDialog dialog = builder.create();
                                dialog.show();


                            }
                            else {

                                String merchantAddress = completeStreetStrings + "+" + completeCityStrings + "+" +  completePostCodeStrings;
                                Log.v("complete String", merchantAddress);
                                GetLatLongFromAddressAndSendTip loc = new GetLatLongFromAddressAndSendTip();
                                loc.execute(merchantAddress);

                            }
                        }

                    }
                    else {

                        AlertDialog.Builder builder = new AlertDialog.Builder(TipNotInPlaceActivity.this);
                        builder.setMessage(R.string.tip_missingImage_message)
                                .setTitle(R.string.tip_missingImage_tittle)
                                .setPositiveButton(android.R.string.ok, null);

                        AlertDialog dialog = builder.create();
                        dialog.show();

                    }
                }


            }

        });
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected DialogInterface.OnClickListener mDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch (i){
                case 0: // Take picture
                    Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    mMediaUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                    if (mMediaUri == null){
                        // display an error
                        Toast.makeText(TipNotInPlaceActivity.this, R.string.error_external_storage, Toast.LENGTH_LONG).show();
                    }
                    else {
                        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
                        startActivityForResult(takePhotoIntent, TAKE_PHOTO_REQUEST);
                    }
                    break;

                case 1: // Choose picture
                    Intent choosePhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    choosePhotoIntent.setType("image/*");
                    startActivityForResult(choosePhotoIntent, PICK_PHOTO_REQUEST);

            }
        }

        private Uri getOutputMediaFileUri(int mediaType) {
            // To be safe, you should check that the SDCard is mounted
            // using Environment.getExternalStorageState() before doing this.
            if (isExternalStorageAvailable()){
                // get the URI

                // 1. Get the external storage directory
                String appName = TipNotInPlaceActivity.this.getString(R.string.app_name);
                File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appName);
                // 2. Create our subdirectory
                if (! mediaStorageDir.exists()) {
                    if (! mediaStorageDir.mkdirs()){
                        Log.e(TAG, "Failed to create directory.");
                        return null;
                    }
                }
                // 3. Create a file name
                // 4. Create the file
                File mediaFile;
                Date now = new Date();
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(now);
                String path = mediaStorageDir.getPath() + File.separator;
                if (mediaType == MEDIA_TYPE_IMAGE){
                    mediaFile = new File(path + "IMG_" + timestamp + ".jpg");

                }
                else if (mediaType == MEDIA_TYPE_VIDEO){
                    mediaFile = new File(path + "VID_" + timestamp + ".mp4");
                }
                else {
                    return null;
                }

                Log.d(TAG, "File: " + Uri.fromFile(mediaFile));


                // 5. Return the file's URI
                return Uri.fromFile(mediaFile);
            }
            else {

                return  null;
            }
        }

        private boolean isExternalStorageAvailable(){

            String state = Environment.getExternalStorageState();

            if (state.equals(Environment.MEDIA_MOUNTED)){

                return true;
            }
            else {
                return false;
            }
        }

    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){


            if (requestCode == PICK_PHOTO_REQUEST || requestCode == PICK_VIDEO_REQUEST) {

                if (data == null) {
                    Toast.makeText(this, getString(R.string.general_error), Toast.LENGTH_LONG).show();
                }
                else {

                    mMediaUri = data.getData();

                }

                Log.i(TAG, "Media URI: " + mMediaUri );
                if (requestCode == PICK_VIDEO_REQUEST) {
                    // make sure the the file is less than 10 MB
                    int fileSize = 0;

                    InputStream inputStream = null;
                    try {
                        inputStream = getContentResolver().openInputStream(mMediaUri);
                        fileSize = inputStream.available();
                    }
                    catch (FileNotFoundException e){
                        Toast.makeText(this, getString(R.string.error_opening_file), Toast.LENGTH_LONG).show();
                        return;
                    }
                    catch (IOException e){
                        Toast.makeText(this, getString(R.string.error_opening_file), Toast.LENGTH_LONG).show();
                        return;
                    }
                    finally {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            // Intentionally blank
                        }
                    }
                    if (fileSize >= FILE_SIZE_LIMIT) {
                        Toast.makeText(this, R.string.error_file_size_too_large, Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
            else {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(mMediaUri);
                sendBroadcast(mediaScanIntent);
            }

            if (mMediaUri != null) {

                // Retrieve the image orientation
                try {

                    if (requestCode == PICK_PHOTO_REQUEST) {


                        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;


                        if (isKitKat && DocumentsContract.isDocumentUri(this, mMediaUri)){

                            // Will return "image:x*"
                            String wholeID = DocumentsContract.getDocumentId(mMediaUri);

                            // Split at colon, use second item in the array
                            String id = wholeID.split(":")[1];

                            String[] column = { MediaStore.Images.Media.DATA };

                            // where id is equal to
                            String sel = MediaStore.Images.Media._ID + "=?";

                            Cursor cursor = getContentResolver().
                                    query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                            column, sel, new String[]{ id }, null);

                            String filePath = "";

                            int columnIndex = cursor.getColumnIndex(column[0]);

                            if (cursor.moveToFirst()) {
                                filePath = cursor.getString(columnIndex);
                            }

                            cursor.close();


                            mExif = new ExifInterface(filePath);

                            mBitmapToRotate = decodeFile(filePath);

                            rotatePreviewImage();


                        }else {

                            mExif = new ExifInterface(getAbsolutePath(mMediaUri));

                            mBitmapToRotate = decodeFile(getAbsolutePath(mMediaUri));

                            rotatePreviewImage();
                        }
                    }

                    else {

                        mExif = new ExifInterface(mMediaUri.getPath());

                        mBitmapToRotate = decodeFile(mMediaUri.getPath());

                        rotatePreviewImage();

                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Defining the global URI varible
                mFinalMediaUri = mMediaUri;

            }else {

                Toast.makeText(this, getString(R.string.error_processing_the_image), Toast.LENGTH_LONG).show();
            }


            String fileType;

            if (requestCode == TAKE_PHOTO_REQUEST || requestCode == PICK_PHOTO_REQUEST) {

                fileType = ParseConstants.TYPE_IMAGE;
            }

            else {
                fileType = ParseConstants.TYPE_VIDEO;
            }

            mFileType = fileType;


        }
        else if (resultCode != RESULT_CANCELED) {
            Toast.makeText(this, R.string.general_error, Toast.LENGTH_LONG).show();
        }

    }

    private void rotatePreviewImage() {
        int orientation = mExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        //Sending the image orientation to FileHelper class
        FileHelper.getOrientation(orientation);

        Matrix matrix = new Matrix();

        if (orientation == 6) {
            matrix.postRotate(90);
        }

        else if (orientation == 3) {
            matrix.postRotate(180);
        }

        else if (orientation == 8) {
            matrix.postRotate(270);
        }

        else if (orientation == 0){
            matrix.postRotate(0);
        }

        mBitmapRotated = Bitmap.createBitmap(mBitmapToRotate, 0, 0, mBitmapToRotate.getWidth(),
                mBitmapToRotate.getHeight(), matrix, true);

        // Setting the preview image with the correct orientation
        mPreviewImage.setVisibility(View.VISIBLE);
        mEraseImage.setVisibility(View.VISIBLE);
        mPreviewImage.setImageBitmap(mBitmapRotated);

    }

    public Bitmap decodeFile(String path) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            // The new size we want to scale to
            final int REQUIRED_SIZE = 70;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeFile(path, o2);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;

    }


    public String getAbsolutePath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tip_in_place, menu);
        return true;
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_tip){

            AlertDialog.Builder builder = new AlertDialog.Builder(TipNotInPlaceActivity.this);
            builder.setMessage(R.string.question_label);

            builder.setPositiveButton(R.string.yes_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(TipNotInPlaceActivity.this, TipInPlaceActivity.class);
                    startActivity(intent);

                }
            });
            builder.setNegativeButton(R.string.no_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(TipNotInPlaceActivity.this, TipNotInPlaceActivity.class);
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

            Intent intent = new Intent(TipNotInPlaceActivity.this, UserProfile2.class);
            intent.putExtra("userName", mUserName);
            intent.putExtra("userId", actualUserId);
            startActivity(intent);

        }

        return super.onOptionsItemSelected(item);
    }

    // AsyncTask for rotate an show preview image
    public class LoadPreviewImage  extends AsyncTask<Uri, Void, Void> {

        protected void onPreExecute() {

            progressDialog = ProgressDialog.show(TipNotInPlaceActivity.this, "",
                    "Processing Preview...");

        }


        // Call after onPreExecute method
        protected Void doInBackground(Uri... uris) {

            int orientation = mExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            Log.d(TAG, String.valueOf(orientation));

            //Sending the image orientation to FileHelper class
            FileHelper.getOrientation(orientation);


            //Creating a bitmap an rotate it for the image preview
            Bitmap bitmap = null;
            try {

                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mMediaUri);

            } catch (IOException e) {

                Log.d("Image preview Exception", e.getMessage());
            }

            Matrix matrix = new Matrix();

            if (orientation == 6) {
                matrix.postRotate(90);
            }

            else if (orientation == 3) {
                matrix.postRotate(180);
            }

            else if (orientation == 8) {
                matrix.postRotate(270);
            }

            else if (orientation == 0){
                matrix.postRotate(0);
            }

            mBitmapRotated = Bitmap.createBitmap(bitmap, 0, 0,bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            return null;
        }


        protected void onPostExecute(Void unused) {

            // NOTE: You can call UI Element here.

            if(mBitmapRotated != null)
            {
                // Setting the preview image with the correct orientation
                mPreviewImage.setVisibility(View.VISIBLE);
                mPreviewImage.setImageBitmap(mBitmapRotated);
                mEraseImage.setVisibility(View.VISIBLE);
                progressDialog.dismiss();
            }

        }

    }

    private class GetLatLongFromAddressAndSendTip extends AsyncTask<String, Void, Void> {

        private Exception exception;

        protected void onPreExecute() {

            Toast.makeText(TipNotInPlaceActivity.this, R.string.sending_post, Toast.LENGTH_LONG).show();

            Intent intent = new Intent(TipNotInPlaceActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }


        @Override
        protected Void doInBackground(String... strings) {

            String uri = "http://maps.google.com/maps/api/geocode/json?address=" +
                    strings[0] + "&sensor=false";
            HttpGet httpGet = new HttpGet(uri);
            HttpClient client = new DefaultHttpClient();
            HttpResponse response;
            StringBuilder stringBuilder = new StringBuilder();

            try {
                response = client.execute(httpGet);
                HttpEntity entity = response.getEntity();
                InputStream stream = entity.getContent();
                int b;
                while ((b = stream.read()) != -1) {
                    stringBuilder.append((char) b);
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject = new JSONObject(stringBuilder.toString());

                mLongitude = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lng");

                mLatitude = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lat");

                Log.d("latitude", "lat" + " " +String.valueOf(mLatitude));
                Log.d("longitude", "lon" + " " +String.valueOf(mLongitude));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //New Tip product
            final ParseObject tip = new ParseObject(ParseConstants.CLASS_TIPS);
            tip.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
            tip.put(ParseConstants.KEY_SENDER_NAME, mUserName);
            if (mUserFacebookId != null){

                tip.put(ParseConstants.KEY_FACEBOOK_SENDER_ID, mUserFacebookId);

            }
            tip.put(ParseConstants.KEY_PRODUCT_NAME, mProductNameString);
            tip.put(ParseConstants.KEY_MERCHANT_NAME, mMerchantNameString);
            tip.put(ParseConstants.KEY_PRODUCT_COMMENT, mProductCommentString);


            mMyPoint = new ParseGeoPoint(mLatitude, mLongitude);

            tip.put(ParseConstants.KEY_MERCHANT_LOCATION, mMyPoint);


            tip.put(ParseConstants.KEY_FILE_TYPE, mFileType);


            // Adding the mediaFile to the put
            byte[] fileBytes = FileHelper.getByteArrayFromFile(TipNotInPlaceActivity.this, mFinalMediaUri);

            if (fileBytes == null) {

                //No media to send

            } else {
                if (mFileType.equals(ParseConstants.TYPE_IMAGE)) {
                    fileBytes = FileHelper.reduceImageForUpload(fileBytes);
                }

                String fileName = FileHelper.getFileName(TipNotInPlaceActivity.this, mFinalMediaUri, mFileType);

                // Creating the new Parse File
                ParseFile file = new ParseFile(fileName, fileBytes);
                tip.put(ParseConstants.KEY_FILE, file);
            }

            ParseACL acl = new ParseACL();

            // Give public read an write access
            acl.setPublicReadAccess(true);
            acl.setPublicWriteAccess(true);
            tip.setACL(acl);

            //New Merchant
            ParseObject merchant = new ParseObject(ParseConstants.CLASS_MERCHANTS);
            merchant.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
            merchant.put(ParseConstants.KEY_SENDER_NAME, mUserName);
            merchant.put(ParseConstants.KEY_MERCHANT_NAME, mMerchantNameString);
            merchant.put(ParseConstants.KEY_MERCHANT_LOCATION, mMyPoint);


            // Give public read and write access
            acl.setPublicReadAccess(true);
            acl.setPublicWriteAccess(true);
            merchant.setACL(acl);

            //Setting relationship between the merchant and the product
            tip.put(ParseConstants.KEY_PRODUCT_MERCHANT_RELATION, merchant);

            try {
                tip.save();

                //The user earn 1 point for Post a Tip
                ParseUser.getCurrentUser().increment(ParseConstants.KEY_POINTS);
                try {
                    ParseUser.getCurrentUser().save();

                } catch (ParseException e1) {
                    e1.printStackTrace();

                    exception = e1;
                }

                //Setting relationship between the user and the product
                mCurrentUser = ParseUser.getCurrentUser();
                ParseRelation<ParseObject> userProductRelation = mCurrentUser.getRelation(ParseConstants.KEY_USER_PRODUCT_RELATION);
                userProductRelation.add(tip);
                try {
                    mCurrentUser.save();
                } catch (ParseException e1) {
                    e1.printStackTrace();
                    exception = e1;
                }

            } catch (ParseException e) {
                e.printStackTrace();

                exception = e;

            }

            return null;
        }

        protected void onPostExecute(Void unused) {

            // NOTE: You can call UI Element here.

            if (exception == null) {

                Toast.makeText(TipNotInPlaceActivity.this, R.string.post_send, Toast.LENGTH_LONG).show();
                Toast.makeText(TipNotInPlaceActivity.this, R.string.win_points, Toast.LENGTH_LONG).show();

            }else {

                Toast.makeText(TipNotInPlaceActivity.this, R.string.error_sending_tip, Toast.LENGTH_LONG).show();

            }
        }

    }

    // AsyncTask for send the tip
    public class SendTip extends AsyncTask<Void, Void, Void> {

        private Exception exception;

        protected void onPreExecute() {


            Toast.makeText(TipNotInPlaceActivity.this, R.string.sending_post, Toast.LENGTH_LONG).show();

            Intent intent = new Intent(TipNotInPlaceActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }


        // Call after onPreExecute method
        protected Void doInBackground(Void... finalMediaUri) {

            //New Tip product
            final ParseObject tip = new ParseObject(ParseConstants.CLASS_TIPS);
            tip.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
            tip.put(ParseConstants.KEY_SENDER_NAME, mUserName);
            if (mUserFacebookId != null){

                tip.put(ParseConstants.KEY_FACEBOOK_SENDER_ID, mUserFacebookId);

            }
            tip.put(ParseConstants.KEY_PRODUCT_NAME, mProductNameString);
            tip.put(ParseConstants.KEY_MERCHANT_NAME, mMerchantInfo.getString(ParseConstants.KEY_MERCHANT_NAME));
            tip.put(ParseConstants.KEY_PRODUCT_COMMENT, mProductCommentString);

            ParseGeoPoint merchantLocation = mMerchantInfo.getParseGeoPoint(ParseConstants.KEY_MERCHANT_LOCATION);
            tip.put(ParseConstants.KEY_MERCHANT_LOCATION, merchantLocation);
            tip.put(ParseConstants.KEY_FILE_TYPE, mFileType);


            // Adding the mediaFile to the put
            byte[] fileBytes = FileHelper.getByteArrayFromFile(TipNotInPlaceActivity.this, mFinalMediaUri);

            if (fileBytes == null) {

                //No media to send

            } else {
                if (mFileType.equals(ParseConstants.TYPE_IMAGE)) {
                    fileBytes = FileHelper.reduceImageForUpload(fileBytes);
                }

                String fileName = FileHelper.getFileName(TipNotInPlaceActivity.this, mFinalMediaUri, mFileType);

                // Creating the new Parse File
                ParseFile file = new ParseFile(fileName, fileBytes);
                tip.put(ParseConstants.KEY_FILE, file);
            }

            ParseACL acl = new ParseACL();

            // Give public read and write access
            acl.setPublicReadAccess(true);
            acl.setPublicWriteAccess(true);
            tip.setACL(acl);

            //Setting relationship between the merchant and the product
            tip.put(ParseConstants.KEY_PRODUCT_MERCHANT_RELATION, mMerchantInfo);

            try {
                tip.save();

                //The user earn 1 point for Post a Tip
                ParseUser.getCurrentUser().increment(ParseConstants.KEY_POINTS);
                try {
                    ParseUser.getCurrentUser().save();

                } catch (ParseException e1) {
                    e1.printStackTrace();

                    exception = e1;

                }

                //Setting relationship between the user and the product
                mCurrentUser = ParseUser.getCurrentUser();
                ParseRelation<ParseObject> userProductRelation = mCurrentUser.getRelation(ParseConstants.KEY_USER_PRODUCT_RELATION);
                userProductRelation.add(tip);
                try {
                    mCurrentUser.save();
                } catch (ParseException e1) {
                    e1.printStackTrace();
                    exception = e1;
                }

            } catch (ParseException e) {
                e.printStackTrace();

                exception = e;

            }

            return null;
        }

        protected void onPostExecute(Void unused) {

            if (exception == null) {

                Toast.makeText(TipNotInPlaceActivity.this, R.string.post_send, Toast.LENGTH_LONG).show();
                Toast.makeText(TipNotInPlaceActivity.this, R.string.win_points, Toast.LENGTH_LONG).show();

            } else {

                Toast.makeText(TipNotInPlaceActivity.this, R.string.error_sending_tip, Toast.LENGTH_LONG).show();

            }
        }
    }
}
