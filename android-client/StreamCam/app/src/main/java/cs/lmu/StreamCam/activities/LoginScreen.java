package cs.lmu.StreamCam.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import cs.lmu.StreamCam.services.CustomDiagnostic;

import cs.lmu.StreamCam.R;

public class LoginScreen extends AppCompatActivity {

    private String mUsernameString;
    private TextView mHTTPResponse;
    private EditText mUsernameText;
    private String mPasswordString;
    private EditText mPasswordText;

    private final String TAG = "nothing";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUsernameText = (EditText) findViewById(R.id.LOGIN_username_text);
        mPasswordText = (EditText) findViewById(R.id.LOGIN_password_text);
        mHTTPResponse = (TextView) findViewById(R.id.LOGIN_HTTP_response);

       /* RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://www.google.com";
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Here's where we handle the response

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // This is what we do if it doesn't work.
                    }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);*/
    }

    public void goToCameraActivity(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }

    public void goToCreateAccountActivityFromLogin(View view) {
        Intent intent = new Intent(this, CreateNewAccountActivity.class);
        startActivity(intent);
    }

    public void loginButtonClicked(View view) {
        mUsernameString = mUsernameText.getText().toString();
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
        Toast.makeText(getApplicationContext(),"Yay we can send stuff in!!!", Toast.LENGTH_SHORT).show();
    }
}