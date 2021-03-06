package cs.lmu.StreamCam.activities;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import cs.lmu.StreamCam.Utils.ConnectivityMonitor;
import cs.lmu.StreamCam.Utils.Constants;
import cs.lmu.StreamCam.Utils.CustomDiagnostic;

import cs.lmu.StreamCam.R;
import cs.lmu.StreamCam.Utils.UI;
import cs.lmu.StreamCam.services.HTTPRequestService;

public class LoginScreen extends AppCompatActivity {

    private String mUsernameString;
    private EditText mUsernameText;
    private String mPasswordString;
    private ProgressBar mProgressBar;
    private EditText mPasswordText;
    private SharedPreferences mPrefs;
    private LoginResultReceiver mResultReceiver;
    private Button mLoginButton;
    private Button mCreateAccountButton;
    private final String TAG = LoginScreen.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUsernameText = (EditText) findViewById(R.id.LOGIN_username_text);
        mPasswordText = (EditText) findViewById(R.id.LOGIN_password_text);
        mProgressBar = (ProgressBar) findViewById(R.id.LOGIN_progress_bar);
        mLoginButton = (Button) findViewById(R.id.LOGIN_login_button);
        mCreateAccountButton = (Button) findViewById(R.id.LOGIN_create_account_button);


        mResultReceiver = new LoginResultReceiver(new Handler());
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    }

    public void loginButtonClicked(View view) {
        if(!ConnectivityMonitor.hasConnection(this)) {
            Toast.makeText(getApplicationContext(),
                           "No internet connection.",
                            Toast.LENGTH_SHORT).show();
            return;
        }
        mUsernameString = mUsernameText.getText().toString().trim();
        mPasswordString = mPasswordText.getText().toString();

        CustomDiagnostic inputsDiagnostic = inputsAreValid();

        if(!inputsDiagnostic.hasPassed()) {
            Toast.makeText(
                    getApplicationContext(),
                    inputsDiagnostic.getMessage(),
                    Toast.LENGTH_SHORT).show();
        } else{
            createLoginRequest();
            deactivateButtons();
            mProgressBar.setVisibility(View.VISIBLE);
        }
        UI.hideKeyboard(this);

    }

    public void deactivateButtons() {
        mLoginButton.setEnabled(false);
        mCreateAccountButton.setEnabled(false);
    }

    public void activateButtons() {
        mLoginButton.setEnabled(true);
        mCreateAccountButton.setEnabled(true);
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

    private void createLoginRequest() {
        Intent intent = new Intent(this, HTTPRequestService.class);
        intent.putExtra("JSONRequest", createLoginJSONRequest().toString());
        intent.putExtra("httpReceiver", mResultReceiver);
        intent.putExtra("method", Constants.POST_METHOD);
        intent.putExtra("url", Constants.LOGIN_URL);
        startService(intent);

        Log.e(TAG, "Created a login request");
    }

    private JSONObject createLoginJSONRequest() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("user", mUsernameString);
        params.put("password", mPasswordString);
        return new JSONObject(params);
    }

    public void handleResponse(JSONObject response){
        int status = 0;
        String message;

        try {
            status = (int) response.get("status");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch (status) {
            case 200:
                try {
                    String token = (String) response.get("token");
                    mPrefs.edit().putString("userToken", token).apply();
                    goToCameraActivity();
                    return;
                } catch (JSONException e){
                    message = "No token given.";
                }
                break;
            case 401:
                message = "Incorrect password";
                break;
            case 404:
                message = "Username does not exist.";
                mUsernameText.setText("");
                break;
            default:
                message = "Unknown error occurred";
                mUsernameText.setText("");
                break;
        }

        mProgressBar.setVisibility(View.INVISIBLE);
        mPasswordText.setText("");
        Toast.makeText(
                getApplicationContext(),
                message,
                Toast.LENGTH_SHORT).show();
        activateButtons();
    }

    class LoginResultReceiver extends ResultReceiver {
        public LoginResultReceiver(Handler handler) {
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