package cs.lmu.StreamCam.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import cs.lmu.StreamCam.R;
import cs.lmu.StreamCam.Utils.Constants;
import cs.lmu.StreamCam.Utils.CustomDiagnostic;
import cs.lmu.StreamCam.Utils.UI;
import cs.lmu.StreamCam.services.HTTPRequestService;


public class CreateNewAccountActivity extends AppCompatActivity {

    private EditText mUsernameText;
    private EditText mPasswordText;
    private EditText mPasswordConfirmText;
    private String mUsername;
    private String mPassword;
    private String mConfirmPassword;
    private SharedPreferences mPrefs;
    private CreateAccountResultReceiver mResultReceiver;

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
        mResultReceiver = new CreateAccountResultReceiver(new Handler());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
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
        } else{
            makeNewUserRequest();
        }
        UI.hideKeyboard(this);
    }

    public void makeNewUserRequest() {
        Intent intent = new Intent(this, HTTPRequestService.class);
        intent.putExtra("JSONRequest", createNewUserJSONRequest().toString());
        intent.putExtra("url", Constants.CREATE_ACCOUNT_URL);
        intent.putExtra("method", Constants.POST_METHOD);
        intent.putExtra("httpReceiver", mResultReceiver);
        startService(intent);
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

    public void handleResponse(JSONObject response){
        int status = 0;
        String message;

        try{
            status = (int) response.get("status");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch (status) {
            case 200:
                try {
                    String token = (String) response.get("token");
                    mPrefs.edit().putString("userToken", token).apply();
                    goToCameraActivityFromCreateAccount();
                    return;
                } catch (JSONException e){
                    message = "No token given.";
                }
                break;
            case 409:
                message = "That username is already taken.";
                break;
            default:
                message = "Unknown error occurred";
                break;

        }

        mUsernameText.setText("");
        mPasswordText.setText("");
        mPasswordConfirmText.setText("");
        Toast.makeText(
                getApplicationContext(),
                message,
                Toast.LENGTH_SHORT).show();
    }

    class CreateAccountResultReceiver extends ResultReceiver {
        public CreateAccountResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if(resultCode == Constants.SUCCESS_RESULT) {
                try{
                    Log.e(TAG, "We received a response!!!");
                    handleResponse(new JSONObject(resultData.getString("JSONResponse")));
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

}
