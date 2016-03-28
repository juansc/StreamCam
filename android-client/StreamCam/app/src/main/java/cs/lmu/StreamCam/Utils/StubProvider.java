package cs.lmu.StreamCam.Utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by juanscarrillo on 3/26/16.
 */
public class StubProvider extends ContentProvider {

    /* This indicates that the provider loaded correctly. We
     * don't have one so just say it loaded correctly anways.
     */
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public  String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String [] projection,
                        String selection, String [] selectionArgs,
                        String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    public int update(Uri uri, ContentValues values, String selection, String [] selectionArgs) {
        return 0;
    }



}
