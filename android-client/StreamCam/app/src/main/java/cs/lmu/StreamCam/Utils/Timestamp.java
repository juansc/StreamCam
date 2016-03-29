package cs.lmu.StreamCam.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by juanscarrillo on 3/28/16.
 */
public class Timestamp {

    public static String getTimestamp() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        String timestamp = df.format(new Date());

        return timestamp;
    }
}
