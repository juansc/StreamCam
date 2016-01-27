package juanscarrillo.streamcam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.AbsListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private CameraManager cameraManager;
    private CameraCaptureSession mSession;
    private CameraCharacteristics mCharacteristics;
    private String backCameraId = null;
    private String frontCameraId = null;
    private String mCameraID;
    private Size mPreviewSize;
    private int mCaptureImageFormat;
    private Surface mRawCaptureSurface, mJpegCaptureSurface, mPreviewSurface;

    // My textureview
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

    // Camera device
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

    private CaptureRequest mPreviewCaptureRequest;
    private CaptureRequest.Builder mPreviewCaptureRequestBuilder;

    private CameraCaptureSession mCameraCaptureSession;
    private CameraCaptureSession.CaptureCallback mSessionCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);

                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        Log.d(TAG, "We've started the camera activity");
        mTextureView = (TextureView) findViewById(R.id.surfaceView);

    }

    @Override
    public void onResume() {
        super.onResume();
        if(mTextureView.isAvailable()){

        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
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
            cameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, null);
        } catch(CameraAccessException | SecurityException e) {
            e.printStackTrace();
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
                            if(mCameraDevice == null) {
                                return;
                            }
                            try {
                                mPreviewCaptureRequest =  mPreviewCaptureRequestBuilder.build();
                                mCameraCaptureSession = session;
                                // We repeatedly ask for images
                                mCameraCaptureSession.setRepeatingRequest(
                                        mPreviewCaptureRequest,
                                        mSessionCallback,
                                        null
                                );
                            } catch(CameraAccessException e) {
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

/*    // Everything in the following group needs to be implemented
    // from TextureView.SurfaceTextureListener

    // *--------------------------------------------------------*
    // |                                                        |
    // |           TextureView.SurfaceTextureListener           |
    // *--------------------------------------------------------*
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        setup_camera(surface);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        mSession = null;
        return true;
    }

    // Empty body
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Nothing here
    }

    // Empty body
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Nothing here
    }

    // ==================================================================

    private void setup_camera(SurfaceTexture surface){
        try {
            getCameraIds();
            setupCameraFormat(surface);

        }
        catch (CameraAccessException e) {
            Log.e(TAG, "Getting error setting up camera" ,e);
        }
    }

    private void getCameraIds() throws CameraAccessException  {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String[] camerasOnDevice = cameraManager.getCameraIdList();
        for(String cameraId : camerasOnDevice) {
            CameraCharacteristics cameraInfo = cameraManager.getCameraCharacteristics(cameraId);

            if (cameraInfo.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                backCameraId = cameraId;
            } else if (cameraInfo.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                frontCameraId = cameraId;
            }
        }
        if(backCameraId == null || frontCameraId == null) {
            throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Couldn't find suitable camera");
        }

        Log.d(TAG, "Back Camera is " + backCameraId + " and front camera is " + frontCameraId);
    }

    private void setupCameraFormat(SurfaceTexture surface) throws CameraAccessException {
        CameraCharacteristics cc = cameraManager.getCameraCharacteristics(backCameraId);
        StreamConfigurationMap streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        boolean supportsRaw = false, supportsJPEG = false;
        for(int format : streamConfigs.getOutputFormats()) {
            if(format == ImageFormat.RAW_SENSOR) {
                supportsRaw = true;
            } else if(format == ImageFormat.JPEG) {
                supportsJPEG = true;
            }
        }

        if(supportsRaw) {
            mCaptureImageFormat = ImageFormat.RAW_SENSOR;
        } else if(supportsJPEG) {
            mCaptureImageFormat = ImageFormat.JPEG;
        } else {
            throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Could not find supported image format");
        }

        setupImageReaders(streamConfigs, surface);

        try {
            cameraManager.openCamera(backCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG,"We opened the camera");
                    mCamera = camera;
                    initPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {

                }
            }, null);
        } catch( SecurityException e) {
            Log.e(TAG, "Unable to open camera");
        }

    }

    private void setupImageReaders(StreamConfigurationMap streamConfigs, SurfaceTexture surface) throws CameraAccessException {
        Size rawSize = streamConfigs.getOutputSizes(ImageFormat.RAW_SENSOR)[0];
        Size jpegSize = streamConfigs.getOutputSizes(ImageFormat.JPEG)[0];

        Size [] previewSizes = streamConfigs.getOutputSizes(SurfaceTexture.class);
       // mPreviewSize = findOptimalPreviewSize(previewSizes, rawSize);
        //if(mPreviewSize == null) {
        //    throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "Unable to find optimal size");
       // }

        mPreviewSurface = new Surface(surface);

        ImageReader rawReader = ImageReader.newInstance(rawSize.getWidth(), rawSize.getHeight(),
                ImageFormat.RAW_SENSOR, 1);
        rawReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //
            }
        }, null);
        mRawCaptureSurface = rawReader.getSurface();

        ImageReader jpegReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(),
                ImageFormat.JPEG, 1);
        jpegReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //new SaveJpegTask(Camera2Activity.this, mPhotoDir, reader.acquireLatestImage()).execute();
            }
        }, null);
        mJpegCaptureSurface = jpegReader.getSurface();

    }

    private void initPreview() {
        // scale preview size to fill screen width
//        int screenWidth = getResources().getDisplayMetrics().widthPixels;
//        float previewRatio = mPreviewSize.getWidth() / ((float) mPreviewSize.getHeight());
//        int previewHeight = Math.round(screenWidth * previewRatio);
//        AbsListView.LayoutParams params = mPreviewView.getLayoutParams();
//        params.width = screenWidth;
//        params.height = previewHeight;

        List<Surface> surfaces = Arrays.asList(mPreviewSurface, mRawCaptureSurface, mJpegCaptureSurface);
        try {
            mCamera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mSession = session;
                    Log.d(TAG,"We should be seeing something");
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "Failed to create camera capture session", e);
        }
    }

    private void updatePreview() {
        try {
            if (mCamera == null || mSession == null) {
                return;
            }
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mPreviewSurface);

            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);

//            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, ...)
//            builder.set(CaptureRequest.SENSOR_SENSITIVITY, ...)
//            builder.set(CaptureRequest.CONTROL_AWB_MODE, ...)
//            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, ...)
//            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ...)
//            etc...

            mSession.setRepeatingRequest(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    // if desired, we can get updated auto focus & auto exposure values here from 'result'
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to start preview");
        }
    }*/

}
