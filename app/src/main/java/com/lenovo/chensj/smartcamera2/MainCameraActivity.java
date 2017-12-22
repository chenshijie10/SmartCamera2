package com.lenovo.chensj.smartcamera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.lenovo.chensj.smartcamera2.UI.MyGLSurfaceView;
import com.lenovo.chensj.smartcamera2.filters.renderer.MyRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainCameraActivity extends AppCompatActivity implements View.OnClickListener
        , MyRenderer.SurfaceTextureCreateListener {

    private final String TAG = MainCameraActivity.class.getSimpleName();
    private final int MAX_PICTURE_WIDTH = 3000;
    private final int REQUEST_CODE = 1;
    private List<Surface> mSurfaceList = new ArrayList<>();
    private String[] mCameraIds;
    private String mCameraId;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private CameraDevice mCamera;
    private CameraCaptureSession mCaptureSession;

    //settings
    private CameraCharacteristics mCameraCharacteristics;
    private Size[] mSupportedPictrueSize;
    private Size[] mSupportedPreviewSize;

    private MyGLSurfaceView mGLSurfaceView;
    private ImageView mShutterBtn;
    private ImageView mModeListBtn;
    private ImageView mGalleryBtn;

    //ae af state.
    private final int STATE_WAITING_LOCK = 1;
    private final int STATE_WAITING_PRECAPTURE = 2;
    private final int STATE_WAITING_NON_PRECAPTURE = 3;
    private int mState;

    //jpeg callback
    private ImageReader.OnImageAvailableListener mJpegCallback = new ImageReader
            .OnImageAvailableListener() {


        @Override
        public void onImageAvailable(final ImageReader reader) {
            Image jpegData = reader.acquireNextImage();
            ByteBuffer buffer = jpegData.getPlanes()[0].getBuffer();
            final byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            jpegData.close();
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    mGLSurfaceView.onPictureTaken(bytes, getWindowManager().getDefaultDisplay()
                            .getRotation());
                }
            });
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession
            .CaptureCallback() {

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            dealCaptureResult(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            dealCaptureResult(result);
        }
    };

    private void dealCaptureResult(CaptureResult result) {
        switch (mState) {
            case STATE_WAITING_LOCK: {
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                if (afState == null) {
                    captureStillPicture();
                } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                        CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        captureStillPicture();
                    } else {
                        runPrecaptureSequence();
                    }
                }
                break;
            }
            case STATE_WAITING_PRECAPTURE: {
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    mState = STATE_WAITING_NON_PRECAPTURE;
                }
                break;
            }
            case STATE_WAITING_NON_PRECAPTURE: {
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    captureStillPicture();
                }
                break;
            }
        }
    }

    private void runPrecaptureSequence() {
        mState = STATE_WAITING_PRECAPTURE;
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture() {
        CaptureRequest.Builder captureRequestBuilder = null;
        try {
            captureRequestBuilder = mCamera.createCaptureRequest(CameraDevice
                    .TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation
                    (mCameraCharacteristics,
                    this.getWindowManager().getDefaultDisplay().getRotation()));
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.addTarget(mSurfaceList.get(0));
//            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureRequestBuilder.build(), new CameraCaptureSession
                    .CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull
                        CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    unlockFocus();
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest
                .CONTROL_AF_TRIGGER_CANCEL);

        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), null,
                    mBackgroundHandler);
//            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null,
//                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestFullScreen();
        setContentView(R.layout.activity_main_camera);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager
                        .PERMISSION_GRANTED) {
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
        mGLSurfaceView.setPictureTakenProcessListener(new MyGLSurfaceView
                .PictureTakenProcessListener() {


            @Override
            public void FilterPictureTaken(final Bitmap bitmap, final String Path) {
                mBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (bitmap != null) {
                            Toast.makeText(
                                    MainCameraActivity.this, "Photo have been saved in" + Path,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainCameraActivity.this, "Photo failed to save in" +
                                            Path,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
                mBackgroundHandler.post(new ImageSaver(bitmap, new File(Path)));

            }
        });
        measureParam(Utils.getScreenWidth(), Utils.getScreenHeight());

        //向上移动NavigationBar的高度，避免重合
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.bottom_btn_contain);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) relativeLayout
                .getLayoutParams();
        layoutParams.bottomMargin = Utils.getNavigationBarHeight(this);
        relativeLayout.setLayoutParams(layoutParams);

        mShutterBtn = (ImageView) findViewById(R.id.shut_btn);
        mShutterBtn.setOnClickListener(this);
        mModeListBtn = (ImageView) findViewById(R.id.modelist_btn);
        mModeListBtn.setOnClickListener(this);
        mGalleryBtn = (ImageView) findViewById(R.id.gallery_btn);
        mGalleryBtn.setOnClickListener(this);
        setupPictureSize();
    }

    private void setupPictureSize() {
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mCameraId = mCameraIds[0];
        try {
            mCameraCharacteristics = manager.getCameraCharacteristics(mCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StreamConfigurationMap map = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] supportedJpegSize = map.getOutputSizes(ImageFormat.JPEG);
        mSupportedPictrueSize = supportedJpegSize;
        Size[] supportedPreviewSize = map.getOutputSizes(SurfaceTexture.class);
        mSupportedPreviewSize = supportedPreviewSize;
        Size pictureSize = selectMaxSize(MAX_PICTURE_WIDTH, supportedJpegSize);
        if (mImageReader == null) {
            mImageReader = ImageReader.newInstance(pictureSize.getWidth(),
                    pictureSize.getHeight(), ImageFormat.JPEG, 3);
        }
        mImageReader.setOnImageAvailableListener(mJpegCallback, mBackgroundHandler);
    }

    private Size selectMaxSize(int max_picture_width, Size[] supportedJpegSize) {
        Size selectedSize = new Size(0, 0);
        for (Size size : supportedJpegSize) {
            if (size.getWidth() < max_picture_width && size.getWidth() > selectedSize.getWidth()) {
                selectedSize = size;
            }
        }
        return selectedSize;
    }

    private void requestFullScreen() {
    }

    @Override
    protected void onResume() {
        startBackgroundThread();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //进入到这里代表没有权限.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission
                    .READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                    REQUEST_CODE);
        } else {
            initViews();
        }
        super.onResume();

    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.destroy();
            mGLSurfaceView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture) {
        surfaceTexture.setDefaultBufferSize(Utils.getScreenHeight(), Utils.getScreenWidth());
        mSurfaceList.add(new Surface(surfaceTexture));
        OpenCamera(mCameraId);
    }

    private void OpenCamera(String cameraId) {
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
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
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCamera = camera;
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

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCamera) {
            mCamera.close();
            mCamera = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void startPreview(CameraDevice camera) {
        try {
            mPreviewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mSurfaceList.get(0));
            camera.createCaptureSession(Arrays.asList(mSurfaceList.get(0), mImageReader
                            .getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCaptureSession = session;
                            // Auto focus should be continuous for camera preview.
                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            CaptureRequest previewRequest = mPreviewRequestBuilder.build();
                            try {
                                session.setRepeatingRequest(previewRequest, new
                                        CameraCaptureSession.CaptureCallback() {
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

    private void takePicture() {
        lockFocus();
    }

    private void lockFocus() {
        mState = STATE_WAITING_LOCK;
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest
                .CONTROL_AF_TRIGGER_START);

        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
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
        Log.d("DEBUG_CODE", "mGLSurfaceView w = " + mGLSurfaceView.getLayoutParams().width
                + ", H = " + mGLSurfaceView.getLayoutParams().height);
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
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics
                .LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.shut_btn:
                takePicture();
                break;
            case R.id.modelist_btn:
                showModeList();
                break;
            case R.id.gallery_btn:
                turnToGallery();

        }
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Bitmap mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        public ImageSaver(Bitmap image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                mImage.compress(Bitmap.CompressFormat.JPEG, 80, output);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void turnToGallery() {
    }

    private void showModeList() {
    }
}
