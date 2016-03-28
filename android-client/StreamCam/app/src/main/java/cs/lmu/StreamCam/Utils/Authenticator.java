package cs.lmu.StreamCam.Utils;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;

/**
 * Created by juanscarrillo on 3/26/16.
 */
public class Authenticator extends AbstractAccountAuthenticator {

    private String token;
    private String username;

    public Authenticator (Context context) {
        super(context);
    }

    // Returns a Bundle that contains the Intent of the activity that can be used to
    // edit the properties
    @Override
    public Bundle editProperties (AccountAuthenticatorResponse response,
                                  String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
                             String accountType,
                             String authTokenType,
                             String [] requiredFeatures,
                             Bundle options) throws NetworkErrorException {
        return null;
    }

    // Checks that the user knows the credentials of an account
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                                     Account account,
                                     Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
                               Account account,
                               String authTokenType,
                               Bundle options) throws NetworkErrorException {
        Bundle newBundle = new Bundle();
        newBundle.putString("token",this.token);
        return newBundle;
    }

    // Provides the localized label for the given authTokenType
    @Override
    public String getAuthTokenLabel(String authTokenType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                    Account account,
                                    String authTokenType,
                                    Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
                              Account account,
                              String [] strings) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    public void setAuthToken(String token) {
        this.token = token;
    }

}
