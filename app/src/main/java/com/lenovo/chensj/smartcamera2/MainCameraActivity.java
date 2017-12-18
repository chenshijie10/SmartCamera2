package com.lenovo.chensj.smartcamera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.lenovo.chensj.smartcamera2.filters.renderer.MyRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainCameraActivity extends AppCompatActivity implements MyRenderer.SurfaceTextureCreateListener {


    //    private OpenCameraThread mOpenCameraThread;
    private final String TAG = MainCameraActivity.class.getSimpleName();
    private final int REQUEST_CODE = 1;
    private MyGLSurfaceView mGLSurfaceView;
    private List<Surface> mSurfaceList = new ArrayList<>();
    private String[] mCameraIds;
    private String mCameraId;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;
    private CaptureRequest mPreviewRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestFullScreen();
        setContentView(R.layout.activity_main_camera);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //进入到这里代表没有权限.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_CODE);
        } else {
            initViews();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //用户同意授权
                    initViews();
                } else {
                    //用户拒绝授权
                    finish();
                }
                break;
        }
    }

    private void initViews() {
        Utils.initialize(this);
        mGLSurfaceView = (MyGLSurfaceView) findViewById(R.id.camera_preview);
        mGLSurfaceView.setSurfaceTextureCreateListener(this);
        measureParam(Utils.getScreenWidth(), Utils.getScreenHeight());

        //向上移动NavigationBar的高度，避免重合
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.bottom_btn_contain);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) relativeLayout.getLayoutParams();
        layoutParams.bottomMargin = Utils.getNavigationBarHeight(this);
        relativeLayout.setLayoutParams(layoutParams);
    }

    private void requestFullScreen() {
    }

    @Override
    protected void onResume() {
        startBackgroundThread();
        super.onResume();

    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture) {
        surfaceTexture.setDefaultBufferSize(Utils.getScreenHeight(), Utils.getScreenWidth());
        mSurfaceList.add(new Surface(surfaceTexture));
        OpenCamera();
    }

    private void OpenCamera() {
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        try {
            mCameraId = mCameraIds[0];
            manager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    startPreview(camera);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {

                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void startPreview(CameraDevice camera){
        CaptureRequest.Builder builder = null;
        try {
            builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        builder.addTarget(mSurfaceList.get(0));
        mPreviewRequest = builder.build();
        camera.createCaptureSession(Arrays.asList(mSurfaceList.get(0)),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            session.setRepeatingRequest(mPreviewRequest, new CameraCaptureSession.CaptureCallback() {
                            }, mBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                    }
                }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void measureParam(int previewWidth, int previewHeight) {
        mGLSurfaceView.requestLayout();
        float scaleW = previewWidth * 1.0f / (Utils.getScreenWidth());
        float scaleH = previewHeight * 1.0f / (Utils.getScreenHeight());
        float scale = Math.max(scaleW, scaleH);
        int viewWidth = (int) (previewWidth / scale);
        int viewHeight = (int) (previewHeight / scale);
        Log.d(TAG, "measureParam(): scale.WH is (" + scaleW + ", " + scaleH
                + "), scale is " + scale + ", View.WH is (" + viewWidth + ", "
                + viewHeight + ");");

        ViewGroup.LayoutParams params = mGLSurfaceView.getLayoutParams();
        params.width = viewWidth;
        params.height = viewHeight;
        mGLSurfaceView.setLayoutParams(params);
        Log.d("DEBUG_CODE","mGLSurfaceView w = "+mGLSurfaceView.getLayoutParams().width
                +", H = "+mGLSurfaceView.getLayoutParams().height);
    }
    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }

}
