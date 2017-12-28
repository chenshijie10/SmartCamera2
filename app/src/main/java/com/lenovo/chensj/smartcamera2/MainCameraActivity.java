package com.lenovo.chensj.smartcamera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.lenovo.chensj.smartcamera2.UI.MyGLSurfaceView;
import com.lenovo.chensj.smartcamera2.UI.RoiView;
import com.lenovo.chensj.smartcamera2.camera.AutoFocusStatesDealer;
import com.lenovo.chensj.smartcamera2.camera.CameraHolder;
import com.lenovo.chensj.smartcamera2.filters.FilterType;
import com.lenovo.chensj.smartcamera2.filters.renderer.MyRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainCameraActivity extends AppCompatActivity implements View.OnClickListener
        , MyRenderer.SurfaceTextureCreateListener, CameraHolder.PictureAvailableListener {

    private final String TAG = MainCameraActivity.class.getSimpleName();
    private final int MAX_PICTURE_WIDTH = 3000;
    private final int REQUEST_CODE = 1;
    private String[] mCameraIds;
    private String mCameraId;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private Handler mUIHandler = new Handler();

    private CameraHolder mCameraHolder;
    private Size mPictureSize;
    private Size mViewSize;

    //settings
    private CameraCharacteristics mCameraCharacteristics;
    private Size[] mSupportedPictrueSize;
    private Size[] mSupportedPreviewSize;

    private MyGLSurfaceView mGLSurfaceView;
    private ImageView mShutterBtn;
    private ImageView mModeListBtn;
    private ImageView mGalleryBtn;
    private RoiView mRoiView;
    private Surface mPreviewSurface = null;

    private AutoFocusStatesDealer.AutoFocusStateListener mAutoFocusStateListener = new AutoFocusStatesDealer.AutoFocusStateListener() {

        @Override
        public void onAutoFocusSuccess(CaptureResult result, boolean locked) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRoiView.setFocusState(RoiView.FocusStates.FOCUSED_LOCKED);
                }
            });
        }

        @Override
        public void onAutoFocusFail(CaptureResult result, boolean locked) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRoiView.setFocusState(RoiView.FocusStates.NOT_FOCUSED_LOCKED);
                }
            });
        }

        @Override
        public void onAutoFocusScan(CaptureResult result) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRoiView.setFocusState(RoiView.FocusStates.PASSIVE_SCAN);
                }
            });
        }

        @Override
        public void onAutoFocusInactive(CaptureResult result) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRoiView.setFocusState(RoiView.FocusStates.INACTIVE);
                }
            });
        }

        @Override
        public void onManualFocusCompleted(CaptureResult result) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRoiView.setFocusMode(RoiView.FocusMode.Continous_picture);
                    mRoiView.setFocusState(RoiView.FocusStates.FOCUSED_LOCKED);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_camera);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("DEBUG_CODE","onTouchEvent event x = "+event.getX()
                + "y = "+event.getY());
        mRoiView.setFocusMode(RoiView.FocusMode.Auto);
        mRoiView.setFocusPoints(new PointF[]{new PointF(event.getX(), event.getY())});
        mCameraHolder.autoFocus(mAutoFocusStateListener, new Point((int)event.getX(), (int)event.getY()),
                new Size(1080, 1920));
        return super.onTouchEvent(event);
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
        mRoiView = (RoiView) findViewById(R.id.roi_view);
        mRoiView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraHolder != null){
                    mRoiView.setFocusMode(RoiView.FocusMode.Continous_picture);
                    mRoiView.setVisibility(View.INVISIBLE);
                    mRoiView.setFocusPoints(new PointF[]{new PointF(mViewSize.getWidth() / 2.f,
                            mViewSize.getHeight() / 2.f)});
                    mCameraHolder.resetRegins(mAutoFocusStateListener);
                }
            }
        });
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
        mPictureSize = selectMaxSize(MAX_PICTURE_WIDTH, supportedJpegSize);
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
    @Override
    protected void onResume() {
        startBackgroundThread();
        if(mCameraHolder == null){
            mCameraHolder = new CameraHolder(this, mBackgroundHandler);
        }
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
            if(mPreviewSurface == null) {
                initViews();
            } else {
                mCameraHolder.OpenCamera(mCameraId, mPreviewSurface, mPictureSize, mAutoFocusStateListener);
            }
        }
        super.onResume();

    }

    @Override
    protected void onPause() {
        if(mCameraHolder != null) {
            mCameraHolder.closeCamera();
            mCameraHolder = null;
        }
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
        mPreviewSurface = new Surface(surfaceTexture);
        mCameraHolder.OpenCamera(mCameraId, mPreviewSurface, mPictureSize, mAutoFocusStateListener);
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
        mViewSize = new Size(viewWidth, viewHeight);
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
                mCameraHolder.takePicture(this);
                break;
            case R.id.modelist_btn:
                showModeList();
                break;
            case R.id.gallery_btn:
                turnToGallery();

        }
    }

    @Override
    public void onPictureTaken(final byte[] data) {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mGLSurfaceView.getFilterType() != FilterType.None) {
                    mGLSurfaceView.onPictureTaken(data, getWindowManager().getDefaultDisplay()
                        .getRotation());
                } else {
                    new ImageSaver(data, new File(mGLSurfaceView.PICTURES_DIRECTORY + "/"
                            + System.currentTimeMillis() + ".jpeg"));
                    Toast.makeText(
                            MainCameraActivity.this, "Photo have been saved in" + mGLSurfaceView.PICTURES_DIRECTORY + "/"

                                    + System.currentTimeMillis() + ".jpeg",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
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

        private final byte[] mJpegData;

        public ImageSaver(byte[] jpegData, File file){
            mJpegData = jpegData;
            mFile = file;
            mImage = null;
        }

        public ImageSaver(Bitmap image, File file) {
            mImage = image;
            mFile = file;
            mJpegData = null;
        }

        @Override
        public void run() {
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                if(mJpegData == null) {
                    mImage.compress(Bitmap.CompressFormat.JPEG, 80, output);
                } else {
                    output.write(mJpegData);
                }
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
