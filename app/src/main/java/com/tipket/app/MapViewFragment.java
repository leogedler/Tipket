package com.tipket.app;


import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.squareup.picasso.Picasso;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by leonardogedler on 24/02/2014.
 */
public class MapViewFragment extends Fragment implements LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener  {

    private GoogleMap mMap;

    protected ProgressDialog progressDialog;

    /*
  * Define a request code to send to Google Play services This code is returned in
  * Activity.onActivityResult
  */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

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
    // Conversion from kilometres to meters
    private static final int KILOMETRES_TO_METRES = 1000;

    // Conversion from kilometers to meters
    private static final int METERS_PER_KILOMETER = 1000;

    // Initial offset for calculating the map bounds
    private static final double OFFSET_CALCULATION_INIT_DIFF = 1.0;

    // Accuracy for calculating the map bounds
    private static final float OFFSET_CALCULATION_ACCURACY = 0.01f;

    // Maximum results returned from a Parse query
    private static final int MAX_POST_SEARCH_RESULTS = 50;

    // Maximum post search radius for map in kilometers
    private static final int MAX_SEARCH_DISTANCE = 1000;


    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    // Fields for the map radius in feet
    private int mRadius;
    private float lastRadius;

    // Represents the circle around a map
    private Circle mapCircle;

    // Fields for helping process map and location changes
    private final Map<String, Marker> mapMarkers = new HashMap<String, Marker>();
    private final Map<Marker, ParseObject> mapClickMarker = new HashMap<Marker, ParseObject>();
    private int mostRecentMapUpdate = 0;
    private boolean hasSetUpInitialLocation = false;
    private String selectedObjectId;
    private Location mLastLocation = null;
    private Location mCurrentLocation = null;

    // Fragment View
    private static View rootView;

    // Global varible for Search
    private String mSearchText;
    private boolean mFilterStatus;
    protected boolean mFilterByDate;
    protected boolean mFilterByTrendy;

    protected boolean mViewStop = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


      // Fix the app crash when swipe through the tabs using MapViewFragment

        if (rootView != null) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null)
                parent.removeView(rootView);
        }
        try {
            rootView = inflater.inflate(R.layout.fragment_map, container, false);
        } catch (InflateException e) {

        }

        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        // Set the update interval
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_SECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(FAST_CEILING_IN_SECONDS);

        // Create a new location client, using the enclosing class to handle callbacks.
        mLocationClient = new LocationClient(getActivity(), this, this);


        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapView)).getMap();


        // Enable the current location "blue dot"
        mMap.setMyLocationEnabled(true);

        /* Set up the camera change handler */
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            public void onCameraChange(CameraPosition position) {
                // When the camera changes, update the query
                doMapQuery();
            }
        });
            return rootView;
        }


    @Override
    public void onResume() {
        super.onResume();

        mRadius = ((MainActivity) getActivity()).getKilometres();
        mFilterStatus = ((MainActivity) getActivity()).getFilterStatus();
        mFilterByDate = ((MainActivity) getActivity()).getSortByDate();
        mFilterByTrendy = ((MainActivity) getActivity()).getSortByTrendy();

            if (mLastLocation != null) {
                LatLng myLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

                updateZoom(myLatLng);

                if (mFilterStatus == true) {
                    updateCircle(myLatLng);
                }

            }
        }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


    @Override
    public void onStop() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }

        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();
        super.onStop();

        mViewStop = true;
    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {
        super.onStart();

        mViewStop = false;

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
        float desiredOffsetInMeters = mRadius * KILOMETRES_TO_METRES;
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
                            new CircleOptions().center(myLatLng).radius(mRadius * KILOMETRES_TO_METRES));
            int baseColor = Color.DKGRAY;
            mapCircle.setStrokeColor(baseColor);
            mapCircle.setStrokeWidth(2);
            mapCircle.setFillColor(Color.argb(50, Color.red(baseColor), Color.green(baseColor),
                    Color.blue(baseColor)));
        }
        mapCircle.setCenter(myLatLng);
        mapCircle.setRadius(mRadius * KILOMETRES_TO_METRES); // Convert radius in feet to meters.
    }

    /*
  * Handle results returned to this Activity by other Activities started with
  * startActivityForResult(). In particular, the method onConnectionFailed() in
  * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to start
  * an Activity that handles Google Play services problems. The result of this call returns here,
  * to onActivityResult.
  */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        if (TipketApplication.APPDEBUG) {
                            // Log the result
                            Log.d(TipketApplication.APPTAG, "Connected to Google Play services");
                        }

                        break;

                    // If any other result was returned by Google Play services
                    default:
                        if (TipketApplication.APPDEBUG) {
                            // Log the result
                            Log.d(TipketApplication.APPTAG, "Could not connect to Google Play services");
                        }
                        break;
                }

                // If any other request code was received
            default:
                if (TipketApplication.APPDEBUG) {
                    // Report that this Activity received an unknown requestCode
                    Log.d(TipketApplication.APPTAG, "Unknown request code received for the activity");
                }
                break;
        }
    }

    /*
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());

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

    @Override
    public void onDisconnected() {



    }

    /*
   * Report location updates to the UI.
   */
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        if (mLastLocation != null
                && geoPointFromLocation(location)
                .distanceInKilometersTo(geoPointFromLocation(mLastLocation)) < 0.05) {
            // If the location hasn't changed by more than 50 meters, ignore it.
            return;
        }else {
            mLastLocation = location;
            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (!hasSetUpInitialLocation) {
                // Zoom to the current location.
                updateZoom(myLatLng);
                hasSetUpInitialLocation = true;
            }
            // Update map radius indicator
            if (mFilterStatus == true) {
                updateCircle(myLatLng);
            }

            doMapQuery();
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /*
   * Set up the query to update the map view
    */
        private void doMapQuery() {

        final int myUpdateNumber = ++mostRecentMapUpdate;
        Location myLoc = (mCurrentLocation == null) ? mLastLocation : mCurrentLocation;
        // If location info isn't available, clean up any existing markers
        if (myLoc == null) {
            cleanUpMarkers(new HashSet<String>());
            return;
        }
        final ParseGeoPoint myPoint = geoPointFromLocation(myLoc);
        mSearchText = ((MainActivity) getActivity()).getSearch();
        // Create the map Parse query
        ParseQuery<ParseObject> mapQuery = new ParseQuery<ParseObject>(ParseConstants.CLASS_TIPS);

        // Set up additional query filters
        if (mSearchText != null) {
             // Filter Search
            mapQuery.whereContains(ParseConstants.KEY_PRODUCT_NAME, mSearchText);
         }

        if (mFilterStatus == true) {

            mapQuery.whereWithinKilometers(ParseConstants.KEY_MERCHANT_LOCATION, myPoint, mRadius);
        }
        else {
            mapQuery.whereWithinKilometers(ParseConstants.KEY_MERCHANT_LOCATION, myPoint, MAX_SEARCH_DISTANCE);
        }

        if (mFilterByDate == true) {
            mapQuery.orderByDescending("createdAt");
        }

        if (mFilterByTrendy == true){
            mapQuery.orderByDescending(ParseConstants.KEY_WISHES_COUNT);
        }

        mapQuery.setLimit(MAX_POST_SEARCH_RESULTS);
        // Kick off the query in the background
        mapQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e != null) {

                    if (TipketApplication.APPDEBUG) {
                        Log.d(TipketApplication.APPTAG, "An error occurred while querying for map posts.", e);
                    }
                    return;
                }
        /*
         * Make sure we're processing results from
         * the most recent update, in case there
         * may be more than one in progress.
        */
                if (myUpdateNumber != mostRecentMapUpdate) {
                    return;
                }
                // Posts to show on the map
                Set<String> toKeep = new HashSet<String>();
                // Loop through the results of the search
                for (final ParseObject tips : objects) {

                    // Add this tip_in_place to the list of map pins to keep
                    toKeep.add(tips.getObjectId());
                    // Check for an existing marker for this post
                    final Marker oldMarker = mapMarkers.get(tips.getObjectId());
                    // Set up the map marker's location
                    MarkerOptions markerOpts =
                            new MarkerOptions().position(new LatLng(tips.getParseGeoPoint(ParseConstants.KEY_MERCHANT_LOCATION).getLatitude(), tips
                                    .getParseGeoPoint(ParseConstants.KEY_MERCHANT_LOCATION).getLongitude()));

                    // Checking for distance filter
                    if (mFilterStatus == true) {

                        // Set up the marker properties based on if it is within the search radius
                        if (tips.getParseGeoPoint(ParseConstants.KEY_MERCHANT_LOCATION).distanceInKilometersTo(myPoint) > mRadius * KILOMETRES_TO_METRES
                                / METERS_PER_KILOMETER) {
                            // Check for an existing out of range marker
                            if (oldMarker != null) {
                                if (oldMarker.getSnippet() == null) {
                                    // Out of range marker already exists, skip adding it
                                    continue;
                                } else {
                                    // Marker now out of range, needs to be refreshed
                                    oldMarker.remove();
                                }
                            }
                        /*
                        // Display a red marker with a predefined title and no snippet
                        markerOpts =
                                markerOpts.title(tips.getString(ParseConstants.KEY_PRODUCT_NAME))
                                        .snippet(tips.getString(ParseConstants.KEY_MERCHANT_NAME))
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));*/
                        } else {
                            // Check for an existing in range marker
                            if (oldMarker != null) {
                                if (oldMarker.getSnippet() != null) {
                                    // In range marker already exists, skip adding it
                                    continue;
                                } else {
                                    // Marker now in range, needs to be refreshed
                                    oldMarker.remove();
                                }
                            }
                            // Display a green marker with the post information
                            markerOpts =
                                    markerOpts.title(StringHelper.capitalize(tips.getString(ParseConstants.KEY_PRODUCT_NAME)))
                                            .snippet(StringHelper.capitalize(tips.getString(ParseConstants.KEY_MERCHANT_NAME)))
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin));

                        }
                    }
                    else {
                        // Check for an existing in range marker
                        if (oldMarker != null) {
                            if (oldMarker.getSnippet() != null) {
                                // In range marker already exists, skip adding it
                                continue;
                            } else {
                                // Marker now in range, needs to be refreshed
                                oldMarker.remove();
                            }
                        }
                        // Display a green marker with the post information
                        markerOpts =
                                markerOpts.title(StringHelper.capitalize(tips.getString(ParseConstants.KEY_PRODUCT_NAME)))
                                        .snippet(StringHelper.capitalize(tips.getString(ParseConstants.KEY_MERCHANT_NAME)))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin));

                    }

                    // Add a new marker
                    Marker marker = mMap.addMarker(markerOpts);
                    mapMarkers.put(tips.getObjectId(), marker);
                    mapClickMarker.put(marker, tips);

                    if (tips.getObjectId().equals(selectedObjectId)) {
                        marker.showInfoWindow();

                        selectedObjectId = null;

                    }

                    if (mViewStop == false) {

                        mMap.setInfoWindowAdapter(new CustomWindowAdapter(getActivity().getLayoutInflater(), mapClickMarker, getActivity()));
                        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                            @Override
                            public boolean onMarkerClick(final Marker marker) {

                                marker.showInfoWindow();

                                final Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        marker.showInfoWindow();

                                    }
                                }, 700);

                                return true;
                            }
                        });

                        //Click on info window in a market
                        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                            @Override
                            public void onInfoWindowClick(Marker marker) {

                                ParseObject tipObject = mapClickMarker.get(marker);

                                String productName = tipObject.getString(ParseConstants.KEY_PRODUCT_NAME);
                                String merchantName = tipObject.getString(ParseConstants.KEY_MERCHANT_NAME);
                                String productId = tipObject.getObjectId();
                                String senderName = tipObject.getString(ParseConstants.KEY_SENDER_NAME);
                                String senderId = tipObject.getString(ParseConstants.KEY_SENDER_ID);
                                String comment = tipObject.getString(ParseConstants.KEY_PRODUCT_COMMENT);
                                String senderFacebookId = tipObject.getString(ParseConstants.KEY_FACEBOOK_SENDER_ID);
                                ParseGeoPoint merchantLocation = tipObject.getParseGeoPoint(ParseConstants.KEY_MERCHANT_LOCATION);
                                double merchantLatitude = merchantLocation.getLatitude();
                                double merchantLongitude = merchantLocation.getLongitude();
                                int wishCounter = tipObject.getInt(ParseConstants.KEY_WISHES_COUNT);
                                Format dateFormat = new SimpleDateFormat("dd MMM");

                                ParseFile file = tipObject.getParseFile(ParseConstants.KEY_FILE);
                                String imageUrl = file.getUrl();

                                Intent intent = new Intent(getActivity(), ProductDetail.class);
                                intent.putExtra("productName", productName);
                                intent.putExtra("merchantName", merchantName);
                                intent.putExtra("senderName", senderName);
                                intent.putExtra("senderId", senderId);
                                intent.putExtra("wishCounter", wishCounter);
                                intent.putExtra("productId", productId);
                                intent.putExtra("imageUrl", imageUrl);
                                intent.putExtra("comment", comment);
                                intent.putExtra("senderFacebookId", senderFacebookId);
                                intent.putExtra("merchantLatitude", merchantLatitude);
                                intent.putExtra("merchantLongitude", merchantLongitude);
                                intent.putExtra("postDate", dateFormat.format(tipObject.getCreatedAt()));
                                startActivity(intent);
                            }


                        });
                    }


                }
                // Clean up old markers.
                cleanUpMarkers(toKeep);
            }
        });
    }

    /*
  * Helper method to clean up old markers
    */
    private void cleanUpMarkers(Set<String> markersToKeep) {
        for (String objId : new HashSet<String>(mapMarkers.keySet())) {
            if (!markersToKeep.contains(objId)) {
                Marker marker = mapMarkers.get(objId);
                marker.remove();
                mapMarkers.get(objId).remove();
                mapMarkers.remove(objId);
            }
        }
    }


}

class CustomWindowAdapter implements GoogleMap.InfoWindowAdapter {
    LayoutInflater mInflater;
    Map<Marker, ParseObject> imageStringMapMarker;
    Context context;

    public CustomWindowAdapter(LayoutInflater i,  Map<Marker, ParseObject> imageStringMapMarker2, Context context ){
        mInflater = i;
        imageStringMapMarker = imageStringMapMarker2;
    }

    @Override
    public View getInfoContents(final Marker marker) {

        View v = mInflater.inflate(R.layout.map_general_info, null);

        ImageView ivThumbnail = (ImageView) v.findViewById(R.id.mapPreviewImage);
        ParseObject mapMarker = imageStringMapMarker.get(marker);

        ParseFile file = mapMarker.getParseFile(ParseConstants.KEY_FILE);

        Picasso.with(context).load(Uri.parse(file.getUrl())).placeholder(R.drawable.loading).into(ivThumbnail);

        return v;

    }

    @Override
    public View getInfoWindow(Marker marker) {
        // TODO Auto-generated method stub
        return null;
    }
}

