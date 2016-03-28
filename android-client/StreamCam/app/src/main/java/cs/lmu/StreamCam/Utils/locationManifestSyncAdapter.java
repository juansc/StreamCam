package cs.lmu.StreamCam.Utils;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

/**
 * Created by juanscarrillo on 3/26/16.
 */
public class locationManifestSyncAdapter extends AbstractThreadedSyncAdapter {

    public locationManifestSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }


    public locationManifestSyncAdapter(Context context,
                                       boolean autoInitialize,
                                       boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {



    }
}
