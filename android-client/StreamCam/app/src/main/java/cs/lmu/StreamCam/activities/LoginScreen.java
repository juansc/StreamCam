package cs.lmu.StreamCam.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;

import cs.lmu.StreamCam.services.CustomDiagnostic;

import cs.lmu.StreamCam.R;

public class LoginScreen extends AppCompatActivity {

    private String mUsernameString;
    private TextView mHTTPResponse;
    private EditText mUsernameText;
    private String mPasswordString;
    private EditText mPasswordText;
    private RequestQueue mQueue;

    private final String TAG = LoginScreen.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUsernameText = (EditText) findViewById(R.id.LOGIN_username_text);
        mPasswordText = (EditText) findViewById(R.id.LOGIN_password_text);
        mHTTPResponse = (TextView) findViewById(R.id.LOGIN_HTTP_response);

       mQueue = Volley.newRequestQueue(this);
    }

    public void goToCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }

    public void goToCreateAccountActivityFromLogin(View view) {
        Intent intent = new Intent(this, CreateNewAccountActivity.class);
        startActivity(intent);
    }

    public void loginButtonClicked(View view) {
        mUsernameString = mUsernameText.getText().toString().trim();
        mPasswordString = mPasswordText.getText().toString();

        CustomDiagnostic inputsDiagnostic = inputsAreValid();

        if(!inputsDiagnostic.hasPassed()) {
            Toast.makeText(
                    getApplicationContext(),
                    inputsDiagnostic.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        createLoginRequest();
    }

    public CustomDiagnostic inputsAreValid() {
        boolean hasValidInputs = false;
        String message = "";

        if(mUsernameString.isEmpty()) {
            message = "Please provide a username.";
        } else if(mPasswordString.isEmpty()) {
            message = "Please provide a password.";
        } else if(mPasswordString.length() < 8) {
            message = "Your password must have at least 8 characters.";
        } else {
            hasValidInputs =  true;
        }

        return new CustomDiagnostic(hasValidInputs, message);

    }

    public void createLoginRequest() {
        String url = "http://10.27.196.149:3000/api/v1/authenticate";

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, createLoginJSONRequest(), new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e(TAG, "We received a response");
                        try {
                            int status = (int) response.get("status");
                            handleResponse(status);
                        } catch(org.json.JSONException e) {
                            e.printStackTrace();
                        }
                        mHTTPResponse.setText("Response: " + response.toString());
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

    public JSONObject createLoginJSONRequest() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("user", mUsernameString);
        params.put("password", mPasswordString);
        return new JSONObject(params);
    }

    public void handleResponse(int status){
        String message;
        switch (status) {
            case 200:
                goToCameraActivity();
                return;
            case 404:
                message = "Invalid login credentials.";
                break;
            default:
                message = "Unknown error occurred";
                break;

        }

        mUsernameText.setText("");
        mPasswordText.setText("");
        Toast.makeText(
                getApplicationContext(),
                message,
                Toast.LENGTH_SHORT).show();
    }
}