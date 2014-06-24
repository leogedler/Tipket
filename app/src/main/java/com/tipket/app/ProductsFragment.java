package com.tipket.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import java.util.List;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * Created by leonardogedler on 24/02/2014.
 */
public class ProductsFragment extends ListFragment implements LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, OnRefreshListener {

    private PullToRefreshLayout mPullToRefreshLayout;

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


    // Maximum post search radius for map in kilometers
    private int SEARCH_DISTANCE;
    private int MAX_SEARCH_DISTANCE = 1000;
    private int mSearchDistance;

    // Fields for helping process map and location changes
    private Location mLastLocation = null;
    private Location mCurrentLocation = null;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;


    // Adapter for List
    protected List<ParseObject> mTips;

    // Global varible for Search
    protected String mSearchText;
    protected boolean mFilterStatus;
    protected boolean mFilterByDate;
    protected boolean mFilterByTrendy;

    protected boolean mViewStop = false;

    // View
    View mRootView;

    //Search Limit
    protected static final int mSearchLimit = 50;

    protected ParseRelation<ParseObject> mUserProducts;

    protected ParseUser mCurrentUser = ParseUser.getCurrentUser();

    protected List<ParseObject> mRelationship;

    /*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_products, container, false);

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

        //Main progress bar
        mProgressBar = (ProgressBar)mRootView.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.VISIBLE);



        return mRootView;

    }*/


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // This is the View which is created by ListFragment
        ViewGroup viewGroup = (ViewGroup) view;


        mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());


        ActionBarPullToRefresh.from(getActivity())

                .options(Options.create()
                        .scrollDistance(.40f)

                        .refreshOnUp(true).build())

                // Inserting the PullToRefreshLayout into the Fragment's ViewGroup
                .insertLayoutInto(viewGroup)

                 // Marking the ListView and it's Empty View as pullable
                 // This is because they are not dirent children of the ViewGroup
                .theseChildrenArePullable(getListView(), getListView().getEmptyView())

                .listener(this)

                .setup(mPullToRefreshLayout);




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

    }

    @Override
    public void onResume() {
        super.onResume();

        //Retrieving the Search variables
        SEARCH_DISTANCE = ((MainActivity) getActivity()).getKilometres();
        mFilterStatus = ((MainActivity) getActivity()).getFilterStatus();
        mFilterByDate = ((MainActivity) getActivity()).getSortByDate();
        mFilterByTrendy = ((MainActivity) getActivity()).getSortByTrendy();
        mSearchText = ((MainActivity) getActivity()).getSearch();

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

        // Connect to the location services client
        mLocationClient.connect();

        mViewStop = false;
    }

    /*
  * Helper method to get the Parse GEO point representation of a location
  */
    private ParseGeoPoint geoPointFromLocation(Location loc) {

        if (loc == null){

            return null;
        }
        else {
            return new ParseGeoPoint(loc.getLatitude(), loc.getLongitude());
        }
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


    /*
  * Called by Location Services when the request to connect the client finishes successfully. At
  * this point, you can request the current location or start periodic updates
  */
    public void onConnected(Bundle bundle) {

        if (TipketApplication.APPDEBUG) {
            Log.d("Connected to location services", TipketApplication.APPTAG);
        }
        mCurrentLocation = getLocation();

        if (mFilterStatus == true) {

            Toast.makeText(getActivity(), getString(R.string.search_radius_message)+ " " + SEARCH_DISTANCE + " " + "Km", Toast.LENGTH_LONG).show();
        }

        startPeriodicUpdates();
    }

    private void doListQuery() {

        Location myLoc = (mCurrentLocation == null) ? mLastLocation : mCurrentLocation;

        // Here start the ParseQuery for the list
        ParseQuery<ParseObject> listQuery  = new ParseQuery<ParseObject>(ParseConstants.CLASS_TIPS);

        // Search for something if the user wants
        if (mSearchText != null) {
            listQuery.whereContains(ParseConstants.KEY_PRODUCT_NAME, mSearchText);
            Toast.makeText(getActivity(), getString(R.string.searching_for_label)+ " " + StringHelper.capitalize(mSearchText), Toast.LENGTH_LONG).show();
        }

        if (mFilterStatus == true) {
            listQuery.whereWithinKilometers(ParseConstants.KEY_MERCHANT_LOCATION, geoPointFromLocation(myLoc), SEARCH_DISTANCE);
        }

        else {

            listQuery.whereWithinKilometers(ParseConstants.KEY_MERCHANT_LOCATION, geoPointFromLocation(myLoc), MAX_SEARCH_DISTANCE);
        }

        if (mFilterByDate == true) {
            listQuery.orderByDescending("createdAt");
        }

        if (mFilterByTrendy == true){
            listQuery.orderByDescending(ParseConstants.KEY_WISHES_COUNT);
        }

        listQuery.setLimit(mSearchLimit);
        listQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(final List<ParseObject> tips, ParseException e) {
                if (e == null) {
                    // we found messages!
                    mTips = tips;

                    if (mTips.size() != 0) {

                        //Fetching relationship user products
                        mUserProducts = mCurrentUser.getRelation(ParseConstants.KEY_USER_PRODUCT_RELATION);
                        ParseQuery<ParseObject> userProducts = mUserProducts.getQuery();
                        userProducts.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> parseObjects, ParseException e) {

                                if (e == null) {
                                    // mProgressBar.setVisibility(View.INVISIBLE);

                                    mRelationship = parseObjects;

                                    if (mViewStop == false) {

                                        if (getListView().getAdapter() == null) {

                                            TipAdapter adapter = new TipAdapter(getListView().getContext(), mTips, mRelationship);
                                            setListAdapter(adapter);


                                            mPullToRefreshLayout.setRefreshComplete();

                                        } else {
                                            // refill the adapter!
                                            ((TipAdapter) getListView().getAdapter()).refill(mTips);

                                            setListShown(true);
                                            mPullToRefreshLayout.setRefreshComplete();
                                        }
                                    }

                                } else {

                                    Log.d("Error fetching user relationship with products :", e.getMessage());
                                    errorFeedDialog();
                                }
                            }
                        });

                    } else {
                        mTips.clear();

                        setListShownNoAnimation(true);

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.empty_products_tittle);
                        builder.setMessage(R.string.empty_products_message);
                        builder.setCancelable(false);

                        builder.setPositiveButton(R.string.go_to_seach, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                Intent intent = new Intent(getActivity(), SearchActivity.class);
                                startActivity(intent);
                            }
                        });

                        builder.setNegativeButton(R.string.go_to_post, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {


                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage(R.string.question_label);

                                builder.setPositiveButton(R.string.yes_label, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        Intent intent = new Intent(getActivity(), TipInPlaceActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);

                                    }
                                });
                                builder.setNegativeButton(R.string.no_label, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        Intent intent = new Intent(getActivity(), TipNotInPlaceActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);

                                    }
                                });

                                AlertDialog dialog2 = builder.create();
                                dialog2.show();

                            }
                        });

                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                } else {
                    setListShownNoAnimation(true);

                    Log.d("Error fetching feed information :", e.getMessage());
                    errorFeedDialog();

                }
            }
        });
    }

    // Error dialog for exception parse queries
    private void errorFeedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.error_feed);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
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

            doListQuery();

        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onRefreshStarted(View view) {

        doListQuery();

    }

}
