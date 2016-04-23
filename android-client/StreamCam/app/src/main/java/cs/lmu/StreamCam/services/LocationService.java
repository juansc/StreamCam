package cs.lmu.StreamCam.services;

import android.app.Service;
import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

import cs.lmu.StreamCam.R;
import cs.lmu.StreamCam.Utils.Constants;

/**
 * Created by juanscarrillo on 3/27/16.
 */
public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private boolean mRequestingLocationUpdates = false;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Location mCurrentLocation;
    private AddressResultReceiver mResultReceiver;
    private String mLastUpdateTime;
    private String mCurrentAddress;
    private LocalBroadcastManager mBroadcaster;
    private final float minimumDistanceBetweenLocations = 20;

    private static final String TAG = LocationService.class.getSimpleName();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "The location service has started!");
        super.onCreate();
        mResultReceiver = new AddressResultReceiver(new Handler());
        mBroadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            startTrackingLocation();
        }
        return START_NOT_STICKY;
    }

    private void startTrackingLocation() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        if(!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }

        Log.e(TAG, "We have started tracking location!");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        setupLocationRequests();

        if(mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        mCurrentLocation = getUserLocation();
        mLastLocation = mCurrentLocation;

        if(mLastLocation != null) {
            if(!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.ADDRESS_SERVICES_no_geocoder_available,
                        Toast.LENGTH_LONG).show();
                return;
            }
            startAddressIntentService();
        }

        Log.e(TAG, "We have gotten the first address!!!");
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        //Toast.makeText(getApplicationContext(), "We moved " + mLastLocation.distanceTo(mCurrentLocation) + "meters!", Toast.LENGTH_SHORT).show();
        if(mCurrentLocation.distanceTo(mLastLocation) >= minimumDistanceBetweenLocations) {
            mLastLocation = mCurrentLocation;
            startAddressIntentService();
        }
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Log.e(TAG, "Location updated!");
    }

    @Override
    public void onConnectionSuspended(int cause){

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }

    // This sets up the location requests. We need to set this up before actually getting
    // updates from the google api
    public void setupLocationRequests() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /*LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        // Check if we're ready to make requests. Usually this means that if the
        // user has not enabled location services we will ask them if they will
        // allow it.
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {

                final int REQUEST_CHECK_SETTINGS = 0x1;

                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(CameraActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });*/
    }

    public void startLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    public Location getUserLocation() {
        Location currentLocation = null;

        try {
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } catch(SecurityException e) {
            Log.e(TAG, "Unable to acquire location.");
        }

        return currentLocation;
    }

    protected void startAddressIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);
        startService(intent);
        Log.e(TAG, "Called the intent!!!");
    }


    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            mCurrentAddress = mAddressOutput;

            sendLocationToActivity();

            if(resultCode == Constants.SUCCESS_RESULT) {
                Log.e(TAG, "Found an address");
            } else{
                Log.e(TAG,"Failed to find address");
            }
        }
    }

    private void sendLocationToActivity() {
        Intent intent = new Intent("processedLocation");
        intent.putExtra("location", mCurrentLocation);
        intent.putExtra("address", mCurrentAddress);

        mBroadcaster.sendBroadcast(intent);
        Log.e(TAG, "We have sent the location!");
    }

}
