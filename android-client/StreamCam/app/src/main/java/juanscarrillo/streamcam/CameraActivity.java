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
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import java.util.LinkedList;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private CameraManager cameraManager;
    private CameraDevice mCamera;
    private CameraCaptureSession mSession;
    private String backCameraId = null;
    private String frontCameraId = null;
    private int mCaptureImageFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        Log.d(TAG, "We've started the camera activity");
        setup_camera();

    }

    // Everything in the following group needs to be implemented
    // from TextureView.SurfaceTextureListener

    // *--------------------------------------------------------*
    // |                                                        |
    // |           TextureView.SurfaceTextureListener           |
    // *--------------------------------------------------------*
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height){
        try{
            setup_camera();
        }catch(CameraAccessException e){
            Log.e(TAG, "Error when setting up camera", e);
        }
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

    private void setup_camera(){
        try {
            getCameraIds();
            setupCameraFormat();
            setupCameraCaptureSize();
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

    private void setupCameraFormat() throws CameraAccessException {
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
    }

    private void setupCameraCaptureSize() throws CameraAccessException {
        
    }

}
