package com.tipket.app;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.android.gms.maps.model.Marker;
import com.parse.ParseGeoPoint;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by leonardogedler on 24/02/2014.
 */
public class MapViewFragment extends Fragment implements LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener  {

    private GoogleMap mMap;
    private MapFragment viewMap;

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
    // Conversion from feet to meters
    private static final float METERS_PER_FEET = 0.3048f;

    // Conversion from kilometers to meters
    private static final int METERS_PER_KILOMETER = 1000;

    // Initial offset for calculating the map bounds
    private static final double OFFSET_CALCULATION_INIT_DIFF = 1.0;

    // Accuracy for calculating the map bounds
    private static final float OFFSET_CALCULATION_ACCURACY = 0.01f;

    // Maximum results returned from a Parse query
    private static final int MAX_POST_SEARCH_RESULTS = 20;

    // Maximum post search radius for map in kilometers
    private static final int MAX_POST_SEARCH_DISTANCE = 100;


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
    private final Map<String, Marker> mapMarkers = new HashMap<String, Marker>();
    private int mostRecentMapUpdate = 0;
    private boolean hasSetUpInitialLocation = false;
    private Location mLastLocation = null;
    private Location mCurrentLocation = null;
    private static View rootView;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        if (rootView != null) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null)
                parent.removeView(rootView);
        }
        try {
            rootView = inflater.inflate(R.layout.fragment_map, container, false);
        } catch (InflateException e) {
        /* map is already there, just return view as it is */
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



            return rootView;
        }

        @Override
    public void onResume() {
        super.onResume();



            if (mLastLocation != null) {
                LatLng myLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

                updateZoom(myLatLng);

                updateCircle(myLatLng);

            }

        }


    // Fix the app crash when swipe through the tabs using MapViewFragment

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
    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {
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
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


}

