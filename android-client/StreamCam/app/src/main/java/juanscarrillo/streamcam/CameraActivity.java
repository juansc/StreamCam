package juanscarrillo.streamcam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import java.util.LinkedList;
import java.util.List;

public class CameraActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String backCameraId;
    private String frontCameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initializeCamera();
    }

    private void initializeCamera() {
        getCameraIds();
        getBackCamera();
    }

    private void getCameraIds() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] camerasOnDevice = cameraManager.getCameraIdList();
            for(String cameraId : camerasOnDevice) {
                CameraCharacteristics cameraInfo = cameraManager.getCameraCharacteristics(cameraId);

                if (cameraInfo.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    backCameraId = cameraId;
                } else if (cameraInfo.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = cameraId;
                }
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void getBackCamera() {
        try {
            cameraManager.openCamera(backCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    startPreview(camera);
                }

                @Override
                public void onDisconnected(CameraDevice camera) {

                }

                @Override
                public void onError(CameraDevice camera, int error) {

                }

            },null );
        } catch (CameraAccessException|SecurityException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(CameraDevice camera) {
        List <Surface> surfacesList = new LinkedList();

        SurfaceView mySurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder myHolder = mySurfaceView.getHolder();
        Surface mySurface = myHolder.getSurface();
        surfacesList.add(mySurface);
        camera.createCaptureSession(surfacesList, ,);
    }



}
