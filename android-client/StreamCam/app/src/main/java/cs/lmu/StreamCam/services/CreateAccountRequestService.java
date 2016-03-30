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

import cs.lmu.StreamCam.Utils.Constants;

/**
 * Created by juanscarrillo on 3/29/16.
 */
public class CreateAccountRequestService extends IntentService {

    protected ResultReceiver mReceiver;
    private RequestQueue mQueue;

    private static final String TAG = CreateAccountRequestService.class.getSimpleName();


    public CreateAccountRequestService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.e(TAG, "We got a request!");
        mQueue = Volley.newRequestQueue(this);
        mReceiver = intent.getParcelableExtra("loginReceiver");
        try{
            JSONObject requestBody = new JSONObject(intent.getStringExtra("JSONRequest"));
            createLoginRequest(requestBody);
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    private void createLoginRequest(JSONObject requestBody) {
        String url = Constants.CREATE_ACCOUNT_URL;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, requestBody, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e(TAG, "We received a response");
                        deliverResponseToReceiver(Constants.SUCCESS_RESULT, response);
                        stopSelf();
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
                        deliverResponseToReceiver(Constants.FAILURE_RESULT, null);
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
