package cs.lmu.StreamCam.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


import cs.lmu.StreamCam.R;
import cs.lmu.StreamCam.services.TravelLog;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = CameraActivity.class.getSimpleName();

    private String mCameraID;
    private Size mPreviewSize;
    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private TextView mAddressTextView;
    private TravelLog mTravelLog;
    private boolean isStreaming;
    private BroadcastReceiver mLocationMessageReceiver;

    // This is the texture where we will see the video that is being recorded
    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    setupCamera(width, height);
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            };

    // Camera device that we need to request
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback
            = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreviewSession();
            Toast.makeText(getApplicationContext(), "Camera Opened!", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;

        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    // This is necessary when asking for images and video from
    // camera
    private CaptureRequest mPreviewCaptureRequest;
    private CaptureRequest.Builder mPreviewCaptureRequestBuilder;

    //
    private CameraCaptureSession mCameraCaptureSession;
    private CameraCaptureSession.CaptureCallback mSessionCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);

                }
            };

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupLocationMessageReceiver();

        mTravelLog = new TravelLog();


        setContentView(R.layout.activity_camera);
        Log.d(TAG, "We've started the camera activity");

        mTextureView = (TextureView) findViewById(R.id.surfaceView);
        mLatitudeTextView = (TextView) findViewById(R.id.latitudeValue);
        mLongitudeTextView = (TextView) findViewById(R.id.longitudeValue);
        mAddressTextView = (TextView) findViewById(R.id.addressValue);

        Intent intent = new Intent(this, LocationService.class);
        startService(intent);

    }

    private void setupLocationMessageReceiver() {
        mLocationMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Location location = intent.getParcelableExtra("location");
                String address = intent.getStringExtra("address");
                updateLocationDisplay(location, address);
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mLocationMessageReceiver, new IntentFilter("processedLocation"));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if(hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        openBackgroundThread();;
        if(mTextureView.isAvailable()){
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        closeBackgroundThread();

        super.onPause();
    }

    public void updateLocationDisplay(Location location, String address) {
        String longitudeString = "Unavailable";
        String latitudeString = "Unavailable";
        String addressString = "Unavailable";

        if(location != null) {
            longitudeString = String.valueOf(location.getLatitude());
            latitudeString = String.valueOf(location.getLongitude());
        }
        if(address != null) {
            addressString = address;
        }

        mLatitudeTextView.setText(latitudeString);
        mLongitudeTextView.setText(longitudeString);
        mAddressTextView.setText(addressString);

        Log.e(TAG, "The address has been set in TextView");
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraID: cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraID);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                /*int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean inLandscapeMode = totalRotation == 90 || totalRotation == 270;

                int rotatedWidth = width;
                int rotatedHeight = height;
                if(inLandscapeMode) {
                    rotatedHeight = width;
                    rotatedWidth = height;
                }*/

                mPreviewSize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraID = cameraID;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getPreferredPreviewSize(Size [] mapSizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        for(Size option: mapSizes) {
            // If we're in landscape view
            if(width > height) {
                if(option.getWidth() > width  && option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if(option.getWidth() > height && option.getHeight() > width) {
                    collectorSizes.add(option);
                }
            }
        }
        if(collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return mapSizes[0];
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, mBackgroundHandler);
        } catch(CameraAccessException | SecurityException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if(mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void createCameraPreviewSession() {
        try{
            // This is where we set up the preview so that we can see an image before
            // we capture it
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            mPreviewCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewCaptureRequestBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            if (mCameraDevice == null) {
                                return;
                            }
                            try {
                                mPreviewCaptureRequest = mPreviewCaptureRequestBuilder.build();
                                mCameraCaptureSession = session;
                                // We repeatedly ask for images
                                mCameraCaptureSession.setRepeatingRequest(
                                        mPreviewCaptureRequest,
                                        mSessionCallback,
                                        mBackgroundHandler
                                );
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "We failed to configure camera",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera 2 Background Thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

    }

    private void closeBackgroundThread() {
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics,int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;

    }

    public boolean isStreaming() {
        return this.isStreaming;
    }



}
