package com.tipket.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

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

public class TipInPlaceActivity extends FragmentActivity implements LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener  {

    public static final String TAG = TipInPlaceActivity.class.getSimpleName();

    // Members variables
    protected EditText mProductName;
    protected EditText mProductComment;
    protected ImageButton mSendTipButton;
    protected ImageButton mCameraButton;
    protected ImageView mPreviewImage;
    protected EditText mMerchantName;
    protected ImageView mEraseImage;
    protected Bitmap mBitmapToRotate;
    protected ImageView mListOfMerchantsNames;
    protected TextView mNameOfMerchantSelected;
    protected ImageView mEraseMerchantSelected;
    private final Map<Integer, ParseObject> mMerchantsObjects = new HashMap<Integer, ParseObject>();


    protected ProgressDialog progressDialog;


    // Global Google constant
    private GoogleMap mMap;


    /*
     * Constants for location update parameters
     */
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;

    // The update interval
    private static final int UPDATE_INTERVAL_IN_SECONDS = 5;

    // A fast interval ceiling
    private static final int FAST_CEILING_IN_SECONDS = 1;

    // Update interval in milliseconds
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = MILLISECONDS_PER_SECOND
            * UPDATE_INTERVAL_IN_SECONDS;

    // A fast ceiling of update intervals, used when the app is visible
    private static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS = MILLISECONDS_PER_SECOND
            * FAST_CEILING_IN_SECONDS;

    /*
     * Constants for handling location results
     */
    // Conversion from feet to meters
    private static final float METERS_PER_FEET = 0.3048f;

    // Conversion from kilometers to meters
    private static final int METERS_PER_KILOMETER = 1000;

    // Initial offset for calculating the map bounds
    private static final double OFFSET_CALCULATION_INIT_DIFF = 1.0;

    // Accuracy for calculating the map bounds
    private static final float OFFSET_CALCULATION_ACCURACY = 0.01f;

    // Maximum post search radius for map in kilometers
    private static final double MAX_POST_SEARCH_DISTANCE = 0.4;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    // Fields for the map radius in feet
    private float radius = 2000;
    private float lastRadius;

    // Represents the circle around a map
    private Circle mapCircle;

    // Fields for helping process map and location changes
    private int mostRecentMapUpdate = 0;
    private boolean hasSetUpInitialLocation = false;
    private Location mLastLocation = null;
    private Location mCurrentLocation = null;

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
    protected ParseGeoPoint mMyPoint;


    //Parse Object Merchant Info
    protected ParseObject mMerchantInfo;

    //Orientation pictures
    protected ExifInterface mExif;

    //Rotated bitmap for the preview picture
    protected Bitmap mBitmapRotated;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_tip_in_place);

        //Merchant Name
        mMerchantName = (EditText) findViewById(R.id.merchantName);
        mNameOfMerchantSelected = (TextView) findViewById(R.id.nameOfMerchantSelected);
        mEraseMerchantSelected = (ImageView) findViewById(R.id.eraseMerchantSelected);


        //Preview image
        mPreviewImage = (ImageView) findViewById(R.id.previewImage);

        mEraseImage = (ImageView) findViewById(R.id.eraseImage);

        //Button for list of merchant dialog
        mListOfMerchantsNames = (ImageView) findViewById(R.id.listOfMerchantsNames);

        // Retrieving user info
        mUserName = ParseUser.getCurrentUser().getUsername();
        mUserFacebookId = ParseUser.getCurrentUser().getString(ParseConstants.FACEBOOK_USER_ID);

        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        // Set the update interval
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_SECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(FAST_CEILING_IN_SECONDS);

        // Create a new location client, using the enclosing class to handle callbacks.
        mLocationClient = new LocationClient(this, this, this);

        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView2)).getMap();

        //Set Progress Bar for wait the current location
        setProgressBarIndeterminateVisibility(true);

        // Enable the current location "blue dot"
        mMap.setMyLocationEnabled(true);

        mCameraButton = (ImageButton) findViewById(R.id.cameraButton);
        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(TipInPlaceActivity.this);
                builder.setItems(R.array.camera_choices, mDialogListener);
                AlertDialog dialog = builder.create();
                dialog.show();

            }
        });

        //Erase image selected
        mPreviewImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mFinalMediaUri = null;
                mPreviewImage.setVisibility(View.INVISIBLE);
                mEraseImage.setVisibility(View.INVISIBLE);

            }
        });


        //List of merchants available
        mListOfMerchantsNames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {


                progressDialog = ProgressDialog.show(TipInPlaceActivity.this, "",
                        getString(R.string.loading_label));


                ParseGeoPoint myLoc = geoPointFromLocation(mCurrentLocation);

                //Querying the merchants names
                final ParseQuery<ParseObject> merchants = new ParseQuery<ParseObject>(ParseConstants.CLASS_MERCHANTS);
                merchants.whereWithinKilometers(ParseConstants.KEY_MERCHANT_LOCATION, myLoc, MAX_POST_SEARCH_DISTANCE);
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
                                        (TipInPlaceActivity.this,android.R.layout.simple_list_item_1, listOfMerchantsNames);




                                AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                                        TipInPlaceActivity.this);
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


                                                mMerchantInfo = null;

                                                dialog.dismiss();
                                            }
                                        });


                                builderSingle.setAdapter(adapter,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {


                                                mMerchantInfo = mMerchantsObjects.get(i);

                                                mNameOfMerchantSelected.setVisibility(View.VISIBLE);
                                                mListOfMerchantsNames.setVisibility(View.INVISIBLE);
                                                mEraseMerchantSelected.setVisibility(View.VISIBLE);
                                                mNameOfMerchantSelected.setText(StringHelper.capitalize(mMerchantInfo.getString(ParseConstants.KEY_MERCHANT_NAME)));

                                                dialogInterface.dismiss();

                                            }
                                        });
                                builderSingle.show();

                            }else {

                                AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                                        TipInPlaceActivity.this);
                                builderSingle.setIcon(R.drawable.launcher_logo);
                                builderSingle.setTitle(R.string.no_merchant_around_tittle);
                                builderSingle.setMessage(R.string.no_merchant_around_message);
                                builderSingle.setPositiveButton(R.string.add_no_place, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog2, int i) {

                                        mListOfMerchantsNames.setVisibility(View.INVISIBLE);
                                        mMerchantName.setVisibility(View.VISIBLE);
                                        mMerchantName.setFocusable(true);
                                        mMerchantName.requestFocus();


                                        mMerchantInfo = null;

                                        dialog2.dismiss();

                                    }
                                });
                                builderSingle.show();

                            }
                        }else {

                            progressDialog.dismiss();
                            Log.d("Error fetching merchant names list", e.getMessage());

                            AlertDialog.Builder builder = new AlertDialog.Builder(TipInPlaceActivity.this);
                            builder.setMessage(R.string.error_fetching_merchants_names);

                            builder.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    Intent intent = new Intent(TipInPlaceActivity.this, TipInPlaceActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });

                            builder.setNegativeButton(R.string.add_place_name_manually, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    mListOfMerchantsNames.setVisibility(View.INVISIBLE);
                                    mMerchantName.setVisibility(View.VISIBLE);

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


        mEraseMerchantSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mMerchantInfo = null;
                mNameOfMerchantSelected.setVisibility(View.INVISIBLE);
                mListOfMerchantsNames.setVisibility(View.VISIBLE);
                mEraseMerchantSelected.setVisibility(View.INVISIBLE);

            }
        });




        mProductName = (EditText) findViewById(R.id.productNameField);
        mProductComment = (EditText) findViewById(R.id.commentField);
        mSendTipButton = (ImageButton) findViewById(R.id.sedTip);
        mSendTipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Only allow posts if we have a location
                Location myLoc = (mCurrentLocation == null) ? mLastLocation : mCurrentLocation;
                if (myLoc == null) {
                    Toast.makeText(TipInPlaceActivity.this, R.string.wait_for_location, Toast.LENGTH_LONG).show();
                    return;
                }

                else {

                    mMyPoint = geoPointFromLocation(myLoc);

                    mProductNameString = mProductName.getText().toString().toLowerCase();
                    mMerchantNameString =  mMerchantName.getText().toString().toLowerCase();
                    mProductCommentString = mProductComment.getText().toString();

                    if (mProductNameString.isEmpty() || mMerchantNameString.isEmpty() && mMerchantInfo == null){

                        AlertDialog.Builder builder = new AlertDialog.Builder(TipInPlaceActivity.this);
                        builder.setMessage(R.string.tip_error_message)
                                .setTitle(R.string.tip_error_tittle)
                                .setPositiveButton(android.R.string.ok, null);

                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                    else {

                        if (mFinalMediaUri != null) {

                            new SendTip().execute();
                        }
                        else {

                            AlertDialog.Builder builder = new AlertDialog.Builder(TipInPlaceActivity.this);
                            builder.setMessage(R.string.tip_missingImage_message)
                                    .setTitle(R.string.tip_missingImage_tittle)
                                    .setPositiveButton(android.R.string.ok, null);

                            AlertDialog dialog = builder.create();
                            dialog.show();

                        }
                    }

                }
            }

        });
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
                        Toast.makeText(TipInPlaceActivity.this, R.string.error_external_storage, Toast.LENGTH_LONG).show();
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
                String appName = TipInPlaceActivity.this.getString(R.string.app_name);
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

                    } else {

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



    public String getAbsolutePath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
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


    @Override
    protected void onResume() {
        super.onResume();

        if (mLastLocation != null) {
            LatLng myLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            updateZoom(myLatLng);

            updateCircle(myLatLng);
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }

        // After disconnect() is called, the client is considered "dead".


        mLocationClient.disconnect();
        super.onStop();
    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Connect to the location services client
        mLocationClient.connect();

    }

    /*
   * In response to a request to start updates, send a request to Location Services
   */
    private void startPeriodicUpdates() {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    /*
   * In response to a request to stop updates, send a request to Location Services
   */
    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
    }

    /*
   * Zooms the map to show the area of interest based on the search radius
   */
    private void updateZoom(LatLng myLatLng) {


        // Get the bounds to zoom to
        LatLngBounds bounds = calculateBoundsWithCenter(myLatLng);
        // Zoom to the given bounds
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 5));

        //Turn off Progress Bar for wait the current location
        setProgressBarIndeterminateVisibility(false);
    }

    /*
   * Helper method to calculate the bounds for map zooming
   */
    public LatLngBounds calculateBoundsWithCenter(LatLng myLatLng) {
        // Create a bounds
        LatLngBounds.Builder builder = LatLngBounds.builder();

        // Calculate east/west points that should to be included
        // in the bounds
        double lngDifference = calculateLatLngOffset(myLatLng, false);
        LatLng east = new LatLng(myLatLng.latitude, myLatLng.longitude + lngDifference);
        builder.include(east);
        LatLng west = new LatLng(myLatLng.latitude, myLatLng.longitude - lngDifference);
        builder.include(west);

        // Calculate north/south points that should to be included
        // in the bounds
        double latDifference = calculateLatLngOffset(myLatLng, true);
        LatLng north = new LatLng(myLatLng.latitude + latDifference, myLatLng.longitude);
        builder.include(north);
        LatLng south = new LatLng(myLatLng.latitude - latDifference, myLatLng.longitude);
        builder.include(south);

        return builder.build();
    }

    /*
   * Helper method to calculate the offset for the bounds used in map zooming
   */
    private double calculateLatLngOffset(LatLng myLatLng, boolean bLatOffset) {
        // The return offset, initialized to the default difference
        double latLngOffset = OFFSET_CALCULATION_INIT_DIFF;
        // Set up the desired offset distance in meters
        float desiredOffsetInMeters = radius * METERS_PER_FEET;
        // Variables for the distance calculation
        float[] distance = new float[1];
        boolean foundMax = false;
        double foundMinDiff = 0;
        // Loop through and get the offset
        do {
            // Calculate the distance between the point of interest
            // and the current offset in the latitude or longitude direction
            if (bLatOffset) {
                Location.distanceBetween(myLatLng.latitude, myLatLng.longitude, myLatLng.latitude
                        + latLngOffset, myLatLng.longitude, distance);
            } else {
                Location.distanceBetween(myLatLng.latitude, myLatLng.longitude, myLatLng.latitude,
                        myLatLng.longitude + latLngOffset, distance);
            }
            // Compare the current difference with the desired one
            float distanceDiff = distance[0] - desiredOffsetInMeters;
            if (distanceDiff < 0) {
                // Need to catch up to the desired distance
                if (!foundMax) {
                    foundMinDiff = latLngOffset;
                    // Increase the calculated offset
                    latLngOffset *= 2;
                } else {
                    double tmp = latLngOffset;
                    // Increase the calculated offset, at a slower pace
                    latLngOffset += (latLngOffset - foundMinDiff) / 2;
                    foundMinDiff = tmp;
                }
            } else {
                // Overshot the desired distance
                // Decrease the calculated offset
                latLngOffset -= (latLngOffset - foundMinDiff) / 2;
                foundMax = true;
            }
        } while (Math.abs(distance[0] - desiredOffsetInMeters) > OFFSET_CALCULATION_ACCURACY);
        return latLngOffset;
    }

    /*
   * Displays a circle on the map representing the search radius
   */
    private void updateCircle(LatLng myLatLng) {
        if (mapCircle == null) {
            mapCircle =
                    mMap.addCircle(
                            new CircleOptions().center(myLatLng).radius(radius * METERS_PER_FEET));
            int baseColor = Color.DKGRAY;
            mapCircle.setStrokeColor(baseColor);
            mapCircle.setStrokeWidth(2);
            mapCircle.setFillColor(Color.argb(50, Color.red(baseColor), Color.green(baseColor),
                    Color.blue(baseColor)));
        }
        mapCircle.setCenter(myLatLng);
        mapCircle.setRadius(radius * METERS_PER_FEET); // Convert radius in feet to meters.
    }


    /*
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            if (TipketApplication.APPDEBUG) {
                // In debug mode, log the status
                Log.d(TipketApplication.APPTAG, "Google play services available");
            }
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog

            return false;
        }
    }

    /*
  * Get the current location
  */
    private Location getLocation() {
        // If Google Play Services is available
        if (servicesConnected()) {
            // Get the current location
            return mLocationClient.getLastLocation();
        } else {

            return null;
        }
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

            AlertDialog.Builder builder = new AlertDialog.Builder(TipInPlaceActivity.this);
            builder.setMessage(R.string.question_label);

            builder.setPositiveButton(R.string.yes_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(TipInPlaceActivity.this, TipInPlaceActivity.class);
                    startActivity(intent);

                }
            });
            builder.setNegativeButton(R.string.no_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(TipInPlaceActivity.this, TipNotInPlaceActivity.class);
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

            Intent intent = new Intent(TipInPlaceActivity.this, UserProfile2.class);
            intent.putExtra("userName", mUserName);
            intent.putExtra("userId", actualUserId);
            startActivity(intent);


        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (TipketApplication.APPDEBUG) {
            Log.d("Connected to location services", TipketApplication.APPTAG);
        }

        mCurrentLocation = getLocation();
        startPeriodicUpdates();

    }

    /*
  * Helper method to get the Parse GEO point representation of a location
  */
    private ParseGeoPoint geoPointFromLocation(Location loc) {
        return new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());
    }

    /*
  * Report location updates to the UI.
  */
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        if (mLastLocation != null
                && geoPointFromLocation(location)
                .distanceInKilometersTo(geoPointFromLocation(mLastLocation)) < 0.01) {
            // If the location hasn't changed by more than 10 meters, ignore it.
            return;
        }
        mLastLocation = location;
        LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (!hasSetUpInitialLocation) {
            // Zoom to the current location.
            updateZoom(myLatLng);
            hasSetUpInitialLocation = true;
        }
        // Update map radius indicator
        updateCircle(myLatLng);


    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    // AsyncTask for send the tip
    public class SendTip extends AsyncTask<Void, Void, Void> {

        private Exception exception = null;

        protected void onPreExecute() {

            Toast.makeText(TipInPlaceActivity.this, R.string.sending_post, Toast.LENGTH_LONG).show();

            Intent intent = new Intent(TipInPlaceActivity.this, MainActivity.class);
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
            tip.put(ParseConstants.KEY_PRODUCT_COMMENT, mProductCommentString);

            if (mMerchantInfo != null){

                tip.put(ParseConstants.KEY_MERCHANT_NAME, mMerchantInfo.getString(ParseConstants.KEY_MERCHANT_NAME));
                ParseGeoPoint merchantLocation = mMerchantInfo.getParseGeoPoint(ParseConstants.KEY_MERCHANT_LOCATION);
                tip.put(ParseConstants.KEY_MERCHANT_LOCATION, merchantLocation);

            }
            else {
                tip.put(ParseConstants.KEY_MERCHANT_NAME, mMerchantNameString);
                tip.put(ParseConstants.KEY_MERCHANT_LOCATION, mMyPoint);
            }

            tip.put(ParseConstants.KEY_FILE_TYPE, mFileType);


            // Adding the mediaFile to the put
            byte[] fileBytes = FileHelper.getByteArrayFromFile(TipInPlaceActivity.this, mFinalMediaUri);

            if (fileBytes == null) {

                //No media to send

            } else {
                if (mFileType.equals(ParseConstants.TYPE_IMAGE)) {
                    fileBytes = FileHelper.reduceImageForUpload(fileBytes);
                }

                String fileName = FileHelper.getFileName(TipInPlaceActivity.this, mFinalMediaUri, mFileType);

                // Creating the new Parse File
                ParseFile file = new ParseFile(fileName, fileBytes);
                tip.put(ParseConstants.KEY_FILE, file);
            }

            ParseACL acl = new ParseACL();

            // Give public read and write access
            acl.setPublicReadAccess(true);
            acl.setPublicWriteAccess(true);
            tip.setACL(acl);


            if (mMerchantInfo == null) {

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

            }
            else {

                //Setting relationship between the merchant and the product
                tip.put(ParseConstants.KEY_PRODUCT_MERCHANT_RELATION, mMerchantInfo);
            }
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

                    Toast.makeText(TipInPlaceActivity.this, R.string.post_send, Toast.LENGTH_LONG).show();
                    Toast.makeText(TipInPlaceActivity.this, R.string.win_points, Toast.LENGTH_LONG).show();

                }else {

                    Toast.makeText(TipInPlaceActivity.this, R.string.error_sending_tip, Toast.LENGTH_LONG).show();

                }
        }
    }


    // AsyncTask for rotate an show preview image
    public class LoadPreviewImage  extends AsyncTask<Uri, Void, Void> {


        protected void onPreExecute() {

            progressDialog = ProgressDialog.show(TipInPlaceActivity.this, "",
                    "Processing Preview...");
        }

        // Call after onPreExecute method
        protected Void doInBackground(Uri... uris) {

            int orientation = mExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            Log.d(TAG, String.valueOf(orientation));

            //Sending the image orientation to FileHelper class
            FileHelper.getOrientation(orientation);


             //Creating a bitmap an rotate it for the image preview
            /*
            Bitmap bitmap = null;
            try {

                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uris[0]);

            } catch (IOException e) {

                Log.d("Image preview Exception", e.getMessage());
            }*/

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

                mBitmapRotated = Bitmap.createBitmap(mBitmapToRotate, 0, 0,mBitmapToRotate.getWidth(), mBitmapToRotate.getHeight(), matrix, true);

            return null;
        }


        protected void onPostExecute(Void unused) {

            // NOTE: You can call UI Element here.

            if(mBitmapRotated != null)
            {
                // Setting the preview image with the correct orientation
                mPreviewImage.setVisibility(View.VISIBLE);
                mEraseImage.setVisibility(View.VISIBLE);
                mPreviewImage.setImageBitmap(mBitmapRotated);
                progressDialog.dismiss();
            }

        }

    }
}


