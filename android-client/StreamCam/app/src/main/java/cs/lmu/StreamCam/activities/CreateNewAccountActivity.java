package cs.lmu.StreamCam.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;

import cs.lmu.StreamCam.R;
import cs.lmu.StreamCam.services.CustomDiagnostic;


public class CreateNewAccountActivity extends AppCompatActivity {

    private EditText mUsernameText;
    private EditText mPasswordText;
    private EditText mPasswordConfirmText;
    private String mUsername;
    private String mPassword;
    private String mConfirmPassword;
    private RequestQueue mQueue;

    private static final String TAG = CreateNewAccountActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_account);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUsernameText = (EditText) findViewById(R.id.CREATE_ACCOUNT_username);
        mPasswordText = (EditText) findViewById(R.id.CREATE_ACCOUNT_password);
        mPasswordConfirmText = (EditText) findViewById(R.id.CREATE_ACCOUNT_confirm_password);
        mQueue = Volley.newRequestQueue(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void goToCameraActivityFromCreateAccount() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    public void createAccountButtonClicked(View view) {
        mUsername = mUsernameText.getText().toString().trim();
        mPassword = mPasswordText.getText().toString();
        mConfirmPassword = mPasswordConfirmText.getText().toString();

        CustomDiagnostic inputsDiagnostic = inputsAreValid();

        if(!inputsDiagnostic.hasPassed()) {
            Toast.makeText(
                    getApplicationContext(),
                    inputsDiagnostic.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        makeNewUserRequest();

    }

    public void makeNewUserRequest() {
        String url = "http://10.27.196.149:3000/api/v1/users";

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, createNewUserJSONRequest(), new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //
                        try {
                            int status = (int) response.get("status");
                            handleResponse(status);
                        } catch(org.json.JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                    }
                });

        mQueue.add(jsObjRequest);
    }

    public CustomDiagnostic inputsAreValid() {
        boolean hasValidInputs = false;
        String message = "";

        if (mUsername.isEmpty()) {
            message = "Please provide a username.";
        } else if (mPassword.isEmpty()) {
            message = "Please provide a password.";
        } else if (mConfirmPassword.isEmpty()) {
            message = "Please confirm your password";
        } else if (!mPassword.equals(mConfirmPassword)) {
            message = "Your passwords do not match";
        } else if (mPassword.length() < 8) {
            message = "Your password must have at least 8 characters.";
        } else{
            hasValidInputs =  true;
        }

        return new CustomDiagnostic(hasValidInputs, message);
    }

    public JSONObject createNewUserJSONRequest() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("user",mUsername);
        params.put("password", mPassword);
        return new JSONObject(params);
    }

    public void handleResponse(int status){
        String message;
        switch (status) {
            case 200:
                goToCameraActivityFromCreateAccount();
                return;
            case 409:
                message = "That username is already taken.";
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
