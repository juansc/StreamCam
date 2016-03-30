package cs.lmu.StreamCam.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
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

import java.io.UnsupportedEncodingException;

import cs.lmu.StreamCam.Utils.Constants;

/**
 * Created by juanscarrillo on 3/29/16.
 */
public class HTTPRequestService extends IntentService {

    protected ResultReceiver mReceiver;
    private RequestQueue mQueue;

    private static final String TAG = HTTPRequestService.class.getSimpleName();


    public HTTPRequestService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG, "We got a request!");

        mQueue = Volley.newRequestQueue(this);
        mReceiver = intent.getParcelableExtra("httpReceiver");

        String url = intent.getStringExtra("url");
        int method = intent.getIntExtra("method", Constants.GET_METHOD);

        try{
            JSONObject requestBody = new JSONObject(intent.getStringExtra("JSONRequest"));
            createRequest(url, method, requestBody);
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    private void createRequest(String url, int method, JSONObject requestBody) {

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (method, url, requestBody, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e(TAG, "We received a good response");
                        deliverResponseToReceiver(Constants.SUCCESS_RESULT, response);
                        stopSelf();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.e(TAG, "We received an error response");
                        JSONObject response = null;
                        try{
                            response = new JSONObject(new String(new String(error.networkResponse.data,"UTF-8")));
                        } catch(JSONException | UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        deliverResponseToReceiver(Constants.SUCCESS_RESULT, response);
                        stopSelf();
                    }
                });

        mQueue.add(jsObjRequest);
    }

    private void deliverResponseToReceiver(int resultCode, JSONObject response) {
        Log.e(TAG, "We will deliver to the receiver");
        Bundle bundle = new Bundle();
        if(response != null) {
            bundle.putString("JSONResponse", response.toString());
        }
        mReceiver.send(resultCode, bundle);
    }
}
