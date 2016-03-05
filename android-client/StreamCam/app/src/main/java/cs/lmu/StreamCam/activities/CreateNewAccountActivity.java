package cs.lmu.StreamCam.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cs.lmu.StreamCam.R;
import cs.lmu.StreamCam.services.CustomDiagnostic;


public class CreateNewAccountActivity extends AppCompatActivity {

    private EditText mUsernameText;
    private EditText mPasswordText;
    private EditText mPasswordConfirmText;
    private String mUsername;
    private String mPassword;
    private String mConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_account);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mUsernameText = (EditText) findViewById(R.id.CREATE_ACCOUNT_username);
        mPasswordText = (EditText) findViewById(R.id.CREATE_ACCOUNT_password);
        mPasswordConfirmText = (EditText) findViewById(R.id.CREATE_ACCOUNT_confirm_password);

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

        goToCameraActivityFromCreateAccount();
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

}
