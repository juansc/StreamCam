package cs.lmu.StreamCam.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import cs.lmu.StreamCam.Utils.Timestamp;

/**
 * Created by juanscarrillo on 3/28/16.
 */
public class LocationPostService extends IntentService {

    private static final String TAG = LocationPostService.class.getSimpleName();
    private RequestQueue mQueue;
    SharedPreferences mPreferences;

    public LocationPostService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent){

        mQueue = Volley.newRequestQueue(this);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Location location = intent.getParcelableExtra("location");
        String address = intent.getStringExtra("address");
        int videoID = intent.getIntExtra("videoID", 0);
        String token = getToken();
        String timestamp = Timestamp.getTimestamp();

        JSONObject postRequest = createJSONPostRequest(location, address, timestamp, token);

        createPostRequest(videoID, postRequest);
    }

    public void createPostRequest(int videoID, JSONObject requestObject) {
        String url = "https://stream-cam.herokuapp.com/api/v1/manifest/" + videoID;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, requestObject, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e(TAG, "We received a response");
                        handleResponse(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.e(TAG, "We haven't received anything");
                        Toast.makeText(
                                getApplicationContext(),
                                "Unable to connect to server.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        mQueue.add(jsObjRequest);
    }

    private String getToken() {
        return mPreferences.getString("userToken","");
    }

    private void handleResponse(JSONObject response) {
        int status = 0;

        try{
            status = (int) response.get("status");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(status == 200) {
            Log.e(TAG, "Successfully appended to manifest");
        } else{
            Log.e(TAG, "We got back a response of " + status);
        }
        stopSelf();
    }

    private JSONObject createJSONPostRequest(Location location,
                                             String address,
                                             String timestamp,
                                             String token) {

        JSONObject postRequest = new JSONObject();

        try {
            postRequest.put("token", token);


            JSONObject JSONLocation = new JSONObject();
            JSONLocation.put("address", address);
            JSONLocation.put("latitude", String.valueOf(location.getLatitude()));
            JSONLocation.put("longitude", String.valueOf(location.getLongitude()));
            JSONLocation.put("timestamp", timestamp);

            postRequest.put("location", JSONLocation);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return postRequest;
    }


}
