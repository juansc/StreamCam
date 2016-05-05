package cs.lmu.StreamCam.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.video.VideoQuality;

import org.json.JSONException;
import org.json.JSONObject;

import cs.lmu.StreamCam.R;
import cs.lmu.StreamCam.Utils.ConnectivityMonitor;
import cs.lmu.StreamCam.Utils.Constants;
import cs.lmu.StreamCam.Utils.Timestamp;
import cs.lmu.StreamCam.services.HTTPRequestService;
import cs.lmu.StreamCam.services.LocationService;

public class CameraActivity extends AppCompatActivity
        implements Session.Callback,
                   SurfaceHolder.Callback,
                   RtspClient.Callback{
    private static final String TAG = CameraActivity.class.getSimpleName();

    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private TextView mAddressTextView;
    private boolean isStreaming;
    private BroadcastReceiver mLocationMessageReceiver;
    SharedPreferences mPreferences;
    private CreateVideoResultReceiver mVideoResultReceiver;
    private PostLocationResultReceiver mLocationPostResultReceiver;
    private CloseVideoResultReceiver mCloseVideoResultReceiver;
    private int mCurrentVideoID;
    private Location mCurrentLocation;
    private String mAddress;
    private boolean mRequestingLocation;
    private ImageButton mLocationButton;
    private ImageButton mRecordButton;
    private ImageView mConnectivityView;
    private SurfaceView mSurfaceView;
    private Session mSession;
    private RtspClient mClient;
    private String mCurrentVideoFile;
    private ProgressBar mProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setupLocationMessageReceiver();

        mRequestingLocation = false;

        setContentView(R.layout.activity_camera);
        Log.d(TAG, "We've started the camera activity");

        mLatitudeTextView = (TextView) findViewById(R.id.latitudeValue);
        mLongitudeTextView = (TextView) findViewById(R.id.longitudeValue);
        mAddressTextView = (TextView) findViewById(R.id.addressValue);
        mLocationButton = (ImageButton) findViewById(R.id.locationButton);
        mRecordButton = (ImageButton) findViewById(R.id.CAMERA_record_button);
        mConnectivityView = (ImageView) findViewById(R.id.connectivity_icon);
        mSurfaceView = (SurfaceView) findViewById(R.id.cameraScreen);
        mProgressBar = (ProgressBar) findViewById(R.id.CAMERA_progress_bar);

        if(ConnectivityMonitor.hasConnection(this)) {
            mConnectivityView.setImageResource(R.mipmap.has_connection);
        }

        updateLocationDisplay(null, null);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mVideoResultReceiver = new CreateVideoResultReceiver(new Handler());
        mLocationPostResultReceiver = new PostLocationResultReceiver(new Handler());
        mCloseVideoResultReceiver = new CloseVideoResultReceiver(new Handler());

        mSession = SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setAudioQuality(new AudioQuality(8000, 16000))
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(0)
                .setCallback(this)
                .build();

        mClient = new RtspClient();
        mClient.setSession(mSession);
        mClient.setCallback(this);

        mSurfaceView.setAspectRatioMode(SurfaceView.ASPECT_RATIO_PREVIEW);
        mSurfaceView.getHolder().addCallback(this);

        Window window = this.getWindow();

        window.setStatusBarColor(Color.BLACK);

    }



    private void setupLocationMessageReceiver() {
        mLocationMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCurrentLocation = intent.getParcelableExtra("location");
                mAddress = intent.getStringExtra("address");
                updateLocationDisplay(mCurrentLocation, mAddress);
                postLocationToServer();
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mLocationMessageReceiver, new IntentFilter("processedLocation"));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mClient.release();
        mSession.release();
        mSurfaceView.getHolder().removeCallback(this);
    }

    @Override
    public void onSessionStarted() {
        Toast.makeText(getApplicationContext(), "Streaming Started",Toast.LENGTH_SHORT).show();
        mProgressBar.setVisibility(View.INVISIBLE);
        mRecordButton.setImageResource(R.drawable.record_square);
    }

    @Override
    public void onSessionStopped() {
        if(isStreaming) {
            Toast.makeText(getApplicationContext(), "Streaming Stopped", Toast.LENGTH_SHORT).show();
        }
        sendCloseVideoRequest();
    }

    public void sendCloseVideoRequest(){
        Intent intent = new Intent(this, HTTPRequestService.class);
        intent.putExtra("JSONRequest", createNewJSONRequestBodyToCloseVideo().toString());
        intent.putExtra("url", Constants.CLOSE_VIDEO_URL + "/" + mCurrentVideoID);
        intent.putExtra("method", Constants.POST_METHOD);
        intent.putExtra("httpReceiver", mCloseVideoResultReceiver);
        startService(intent);
        Log.e(TAG, "We've sent it to the server!");
    }



    @Override
    public void onSessionConfigured() {

    }


    @Override
    public void onBitrateUpdate(long bitrate) {
    }

    @Override
    public void onPreviewStarted() {

    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {
        switch (reason) {
            case Session.ERROR_CAMERA_ALREADY_IN_USE:
                break;
            case Session.ERROR_CAMERA_HAS_NO_FLASH:
                break;
            case Session.ERROR_INVALID_SURFACE:
                break;
            case Session.ERROR_STORAGE_NOT_READY:
                break;
            case Session.ERROR_CONFIGURATION_NOT_SUPPORTED:
                VideoQuality quality = mSession.getVideoTrack().getVideoQuality();
                Log.e(TAG,"The following settings are not supported on this phone: " +
                        quality.toString() + " " +
                        "(" + e.getMessage() + ")");
                e.printStackTrace();
                return;
            case Session.ERROR_OTHER:
                break;
        }

        if (e != null) {
            Log.e(TAG,e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void updateLocationDisplay(Location location, String address) {
        String longitudeString = "Unavailable";
        String latitudeString = "Unavailable";
        String addressString = "Unavailable";

        if(location != null) {
            longitudeString = String.valueOf(location.getLatitude());
            latitudeString = String.valueOf(location.getLongitude());
        }
        if(address != null) {
            addressString = address;
        }

        mLatitudeTextView.setText(latitudeString);
        mLongitudeTextView.setText(longitudeString);
        mAddressTextView.setText(addressString);

        Log.e(TAG, "The address has been set in TextView");
    }

    public void recordButtonHit(View view) {
        if(isStreaming){
            mClient.stopStream();
            Toast.makeText(getApplicationContext(),
                           "Streaming Stopped",
                           Toast.LENGTH_SHORT).show();
            mRecordButton.setImageResource(R.drawable.record_circle);
            if(mRequestingLocation) {
                endLocationServices();
            }
            updateLocationDisplay(null, null);
        } else {
            mRecordButton.setEnabled(false);
            mProgressBar.setVisibility(View.VISIBLE);
            mRecordButton.setImageResource(0);
            createNewVideoRequest();
        }
        isStreaming = !isStreaming;
        toggleLocationButtonEnabled();
    }


    public void toggleLocationButtonEnabled(){
        mLocationButton.setEnabled(!mLocationButton.isEnabled());
        if(mLocationButton.isEnabled()) {
            mLocationButton.setBackgroundResource(R.drawable.circular_button_shape);
        } else {
            mLocationButton.setBackgroundResource(0);
        }
    }

    public void locationButtonHit(View view) {
        if(!mRequestingLocation && !isLocationServiceEnabled()) {
            if(!mRequestingLocation){
                Log.e(TAG, "Not currently asking for location");
            }
            if(!isLocationServiceEnabled()){
                Log.e(TAG, "Location services not enabled.");
            }
            Toast.makeText(getApplicationContext(),
                           "Please enable location services.",
                           Toast.LENGTH_SHORT).show();
            Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            this.startActivity(myIntent);
            return;
        }

        mRequestingLocation = !mRequestingLocation;
        if(mRequestingLocation) {
            mLocationButton.setImageResource(R.mipmap.location_on);
        } else {
            mLocationButton.setImageResource(R.mipmap.location_off);
        }
    }

    private void beginLocationServices() {
        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra("videoID", mCurrentVideoID);
        startService(intent);
    }

    private void endLocationServices() {
        Intent intent = new Intent(this, LocationService.class);
        stopService(intent);
    }

    private void createNewVideoRequest() {
        Intent intent = new Intent(this, HTTPRequestService.class);
        intent.putExtra("JSONRequest", createNewJSONRequestBodyForNewVideo().toString());
        intent.putExtra("url", Constants.CREATE_VIDEO_URL);
        intent.putExtra("method", Constants.POST_METHOD);
        intent.putExtra("httpReceiver", mVideoResultReceiver);
        startService(intent);
    }

    private JSONObject createNewJSONRequestBodyForNewVideo() {
        JSONObject requestBody = new JSONObject();
        try{
            requestBody.put("video_timestamp", Timestamp.getTimestamp());
            requestBody.put("token", mPreferences.getString("userToken", ""));
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return requestBody;
    }

    private void setupRTSPClient () {
        mClient.setCredentials("streamcamuser", "grapefruit");
        mClient.setServerAddress("52.53.190.157", 1935);
        mClient.setStreamPath("/live/" + mCurrentVideoFile);
    }

    private void handleVideoResponse(JSONObject response) {
        int status = 0;

        try{
            status = (int) response.get("status");
        } catch(JSONException e) {
            e.printStackTrace();
        }

        if(status == 200) {
            try{
                mCurrentVideoID = (int) response.get("video_id");
                mCurrentVideoFile = response.get("file_name").toString();
                setupRTSPClient();
                mClient.startStream();
                if(mRequestingLocation) {
                    Log.e(TAG,"Began location service!");
                    beginLocationServices();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    "Unable to create video",
                    Toast.LENGTH_SHORT).show();
        }
        mRecordButton.setEnabled(true);
    }

    private void handleLocationResponse(JSONObject response) {
        int status = 0;
        String message = "";
        try{
            status = (int) response.get("status");
            message = (String) response.get("message");
        } catch(JSONException e) {
            e.printStackTrace();
        }

        if(status != 200)  {
            Toast.makeText(
                    getApplicationContext(),
                    "Unable to post location successfully, got code " + status + ": " + message,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCloseVideoResponse(JSONObject response) {
        int status = 0;
        String message = "";
        try{
            status = (int) response.get("status");
            message = (String) response.get("message");
        } catch(JSONException e) {
            e.printStackTrace();
        }

        if(status != 200) {
            Toast.makeText(
                    getApplicationContext(),
                    "Unable to close video successfully, got code " + status + ": " + message,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void postLocationToServer() {
        Intent intent = new Intent(this, HTTPRequestService.class);
        intent.putExtra("JSONRequest", createNewJSONRequestBodyForPostingLocation().toString());
        intent.putExtra("url", Constants.POST_LOCATION_URL + "/" + mCurrentVideoID);
        intent.putExtra("method", Constants.PUT_METHOD);
        intent.putExtra("httpReceiver", mLocationPostResultReceiver);
        startService(intent);
        Log.e(TAG, "We've sent it to the server!");
    }

    private JSONObject createNewJSONRequestBodyForPostingLocation () {
        JSONObject postRequest = new JSONObject();

        try {
            postRequest.put("token", mPreferences.getString("userToken", ""));


            JSONObject JSONLocation = new JSONObject();
            JSONLocation.put("address", mAddress);
            JSONLocation.put("latitude", String.valueOf(mCurrentLocation.getLatitude()));
            JSONLocation.put("longitude", String.valueOf(mCurrentLocation.getLongitude()));
            JSONLocation.put("timestamp", Timestamp.getTimestamp());

            postRequest.put("location", JSONLocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return postRequest;
    }

    private JSONObject createNewJSONRequestBodyToCloseVideo () {
        JSONObject closeVideoRequest = new JSONObject();

        try {
            closeVideoRequest.put("token", mPreferences.getString("userToken", ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return closeVideoRequest;
    }

    class CreateVideoResultReceiver extends ResultReceiver {
        public CreateVideoResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if(resultCode == Constants.SUCCESS_RESULT) {
                try{
                    Log.e(TAG, "We received a response!!!");
                    handleVideoResponse(new JSONObject(resultData.getString("JSONResponse")));
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        "There was an error making the request",
                        Toast.LENGTH_SHORT).show();
                isStreaming = false;
                mRecordButton.setImageResource(R.drawable.record_circle);
            }
        }
    }

    class CloseVideoResultReceiver extends ResultReceiver {
        public CloseVideoResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if(resultCode == Constants.SUCCESS_RESULT) {
                try{
                    Log.e(TAG, "We received a response!!!");
                    handleCloseVideoResponse(new JSONObject(resultData.getString("JSONResponse")));
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        "There was an closing the video",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    class PostLocationResultReceiver extends ResultReceiver {
        public PostLocationResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if(resultCode == Constants.SUCCESS_RESULT) {
                try{
                    Log.e(TAG, "We received a response!!!");
                    handleLocationResponse(new JSONObject(resultData.getString("JSONResponse")));
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(
                        getApplicationContext(),
                        "There was an error making the request",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSession.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mClient.stopStream();
    }

    @Override
    public void onRtspUpdate(int message, Exception e) {
        switch (message) {
            case RtspClient.ERROR_CONNECTION_FAILED:
            case RtspClient.ERROR_WRONG_CREDENTIALS:
                //mProgressBar.setVisibility(View.GONE);
                //enableUI();
                //logError(e.getMessage());
                e.printStackTrace();
                break;
        }
    }

    public boolean isLocationServiceEnabled(){
        boolean gps_enabled= false,
                network_enabled = false;

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        try{
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){ex.printStackTrace();}
        try{
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){ex.printStackTrace();}

        return gps_enabled || network_enabled;

    }

}
