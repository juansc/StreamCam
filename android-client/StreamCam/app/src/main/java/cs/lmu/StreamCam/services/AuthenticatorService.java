package cs.lmu.StreamCam.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by juanscarrillo on 3/26/16.
 */
/* This service is started by the sync adapter framework and is used to
 * start the Authenticator class. This is a BOUND service, which will die when
 * the thing that was bound to dies.
 */

public class AuthenticatorService extends Service {
    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }

}
