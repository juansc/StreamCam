package cs.lmu.StreamCam.Utils;

import com.android.volley.Request;

/**
 * Created by juanscarrillo on 2/21/16.
 */
public final class Constants {
    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;
    public static final String PACKAGE_NAME =
            "cs.lmu.StreamCam";
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME +
            ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME +
            ".LOCATION_DATA_EXTRA";

    private static final String API_DOMAIN = "https://stream-cam.herokuapp.com/api/v1/";
    public static final String LOGIN_URL = API_DOMAIN + "authenticate";
    public static final String CREATE_ACCOUNT_URL = API_DOMAIN + "users";
    public static final String CREATE_VIDEO_URL = API_DOMAIN + "videos";

    public static final int POST_METHOD = Request.Method.POST;
    public static final int PUT_METHOD = Request.Method.PUT;
    public static final int GET_METHOD = Request.Method.GET;
}