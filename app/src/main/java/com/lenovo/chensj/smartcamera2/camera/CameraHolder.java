package com.lenovo.chensj.smartcamera2.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.lenovo.chensj.smartcamera2.Utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chensj12 on 2017/12/22.
 */

public class CameraHolder {

    //越大越流畅，但是占用内存会增大
    private static final int ZSL_BUFFER_CNT = ImageCaptureManager.ZSL_DEPTH + 2;
    private static final int JPEG_BUFFER_CNT = 3;

    private final String TAG = CameraHolder.class.getSimpleName();
    private String mCameraId;

    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;

    private PictureAvailableListener mPictureAvailableListener;

    private Context mContext;
    private Handler mBackgroundHandler;
    private ImageReader mFullYuvImageReader;
    private ImageReader mJpegImageReader;

    public static final MeteringRectangle[] DEFAULT_ROI_RECT = new MeteringRectangle[]{
            new MeteringRectangle(0, 0, 0, 0, 0)};

    public interface PictureAvailableListener{
        void onPictureTaken(byte[] date);
    }
    private ImageReader.OnImageAvailableListener fullYuvImageListener = new ImageReader
            .OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            // todo add image to input surface.
            Image image = reader.acquireNextImage();
            ImageCaptureManager.onZslImage(image);
        }
    };

    private ImageReader.OnImageAvailableListener JpegAvailableCallback = new ImageReader
            .OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            // todo add image to input surface.
            Image image = reader.acquireNextImage();
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[byteBuffer.remaining()];
            byteBuffer.get(data);
            image.close();
            mPictureAvailableListener.onPictureTaken(data);
        }
    };

    private class ReprocCaptureCallback extends AutoFocusStatesDealer {

        public ReprocCaptureCallback(AutoFocusStateListener stateListener) {
            super(stateListener);
        }
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            ImageCaptureManager.onZslCaptureCompleted(request, result);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull
                CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    }

    private Size getInputSize() {
        StreamConfigurationMap map = getCameraCharacteristics(mCameraId).get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] inputSizes = map.getInputSizes(ImageFormat.YUV_420_888);
        return inputSizes[0];
    }

    private ImageReader createFullYuvImageReader() {
        Size size = getInputSize();
        ImageReader imageReader = ImageReader.newInstance(size.getWidth(),
                size.getHeight(), ImageFormat.YUV_420_888, ZSL_BUFFER_CNT);
        imageReader.setOnImageAvailableListener(fullYuvImageListener, mBackgroundHandler);
        return imageReader;
    }

    public CameraHolder(Context context, Handler handler) {
        mContext = context;
        mBackgroundHandler = handler;
    }

    public String getmCameraId() {
        return mCameraId;
    }

    public CameraDevice getmCameraDevice() {
        return mCameraDevice;
    }

    public CaptureRequest.Builder getmCaptureRequest() {
        return mCaptureRequestBuilder;
    }

    public CaptureRequest.Builder getmPreviewRequest() {
        return mPreviewRequestBuilder;
    }

    public CameraCaptureSession getmCameraCaptureSession() {
        return mCameraCaptureSession;
    }

    public void setmCameraDevice(CameraDevice mCameraDevice) {
        this.mCameraDevice = mCameraDevice;
    }

    public void setmCaptureRequest(CaptureRequest.Builder mCaptureRequest) {
        this.mPreviewRequestBuilder = mCaptureRequest;
    }

    public void setmPreviewRequest(CaptureRequest.Builder mPreviewRequest) {
        this.mPreviewRequestBuilder = mPreviewRequest;
    }

    public void setmCameraCaptureSession(CameraCaptureSession mCameraCaptureSession) {
        this.mCameraCaptureSession = mCameraCaptureSession;
    }

    public void OpenCamera(String cameraId, final Surface previewSurface, Size pictureSize,
                           final AutoFocusStatesDealer.AutoFocusStateListener listener) {
        if(mJpegImageReader == null || mJpegImageReader.getWidth() != pictureSize.getWidth()
                || mJpegImageReader.getHeight() != pictureSize.getHeight()){
            if(mJpegImageReader != null){
                mJpegImageReader.close();
                mJpegImageReader = null;
            }
            mJpegImageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(),
                    ImageFormat.JPEG, JPEG_BUFFER_CNT);
            mJpegImageReader.setOnImageAvailableListener(JpegAvailableCallback, mBackgroundHandler);
        }
        mCameraId = cameraId;
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) !=
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
                    mCameraDevice = camera;
                    startPreview(camera, previewSurface, listener);
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
    public void closeCamera() {
        if (null != mCameraCaptureSession) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mFullYuvImageReader) {
            mFullYuvImageReader.close();
            mFullYuvImageReader = null;
        }
        if (null != mJpegImageReader) {
            mJpegImageReader.close();
            mJpegImageReader = null;
        }
        ImageCaptureManager.closeReprocWriter(mCameraId);
    }

    public CameraCharacteristics getCameraCharacteristics(String cameraId) {
        CameraCharacteristics cameraCharacteristics = null;
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return cameraCharacteristics;
    }

    public boolean hasCapability(String cameraId, int capability) {
        int[] capabilities = getCameraCharacteristics(cameraId).get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        for(int cab : capabilities){
            if(cab == capability){
                return true;
            }
        }
        return false;
    }

    public void startPreview(CameraDevice camera, Surface previewSurface, final AutoFocusStatesDealer.AutoFocusStateListener listener) {
        try {
            mPreviewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(previewSurface);
            if (hasCapability(mCameraId, CameraMetadata
                    .REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING)){
                mFullYuvImageReader = createFullYuvImageReader();
                mPreviewRequestBuilder.addTarget(mFullYuvImageReader.getSurface());
                List<Surface> surfaceList = new ArrayList<>();
                surfaceList.add(previewSurface);
                surfaceList.add(mFullYuvImageReader.getSurface());
                surfaceList.add(mJpegImageReader.getSurface());
                Size inputsize = getInputSize();
                InputConfiguration reprocInputConfig =
                        new InputConfiguration(inputsize.getWidth(), inputsize.getHeight(),
                                ImageFormat.YUV_420_888);
                camera.createReprocessableCaptureSession(reprocInputConfig,
                        surfaceList, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                Log.d(TAG, "reprocess capture session configure succeed");
                                mCameraCaptureSession = session;
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                try {
                                    mCameraCaptureSession.setRepeatingRequest
                                            (mPreviewRequestBuilder.build(),
                                                    new ReprocCaptureCallback(listener),
                                                    mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                                ImageCaptureManager.setupReprocSurfaceWriter
                                        (mCameraCaptureSession.getInputSurface(),
                                                mCameraId);
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                Log.d(TAG, "reprocess capture session ConfigureFailed");
                            }
                        }, mBackgroundHandler);
            } else {
                List<Surface> surfaceList = new ArrayList<>();
                surfaceList.add(mJpegImageReader.getSurface());
                surfaceList.add(previewSurface);
                camera.createCaptureSession(surfaceList,
                        new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                mCameraCaptureSession = session;
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                CaptureRequest previewRequest = mPreviewRequestBuilder.build();
                                try {
                                    mCameraCaptureSession.setRepeatingRequest(previewRequest,
                                            new AutoFocusStatesDealer(listener) {
                                            }, mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                            }
                        }, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public void takePicture(PictureAvailableListener listener){
        mPictureAvailableListener = listener;
        lockFocus(true);
    }

    public void resetRegins(final AutoFocusStatesDealer.AutoFocusStateListener listener){
        applyRoi(mPreviewRequestBuilder, DEFAULT_ROI_RECT, DEFAULT_ROI_RECT);
        try {
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                    new ReprocCaptureCallback(listener), mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void autoFocus(final AutoFocusStatesDealer.AutoFocusStateListener listener, Point touchPoint,
                          Size viewSize) {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest
                .CONTROL_AF_TRIGGER_START);
        final MeteringRectangle[] regions = getRegions(new PointF(touchPoint), 100, 100, viewSize);
        applyRoi(mPreviewRequestBuilder, regions, regions);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        try {
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), new AutoFocusStatesDealer(new AutoFocusStatesDealer.AutoFocusStateListener() {

                        @Override
                        public void onAutoFocusSuccess(CaptureResult result, boolean locked) {
                            listener.onAutoFocusSuccess(result, locked);
                            cancelAutoFocus(listener);
                        }

                        @Override
                        public void onAutoFocusFail(CaptureResult result, boolean locked) {
                            listener.onAutoFocusFail(result, locked);
                            cancelAutoFocus(listener);
                        }

                        @Override
                        public void onAutoFocusScan(CaptureResult result) {
                            listener.onAutoFocusScan(result);
                        }

                        @Override
                        public void onAutoFocusInactive(CaptureResult result) {
                            listener.onAutoFocusInactive(result);
                        }

                        @Override
                        public void onManualFocusCompleted(CaptureResult result) {
                            listener.onManualFocusCompleted(result);
                        }
                    }),
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void cancelAutoFocus(AutoFocusStatesDealer.AutoFocusStateListener listener) {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

        try {
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                    new ReprocCaptureCallback(listener), mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setRepeatingRequest failed, errMsg: " + e.getMessage());
        }
    }

    private MeteringRectangle[] getRegions(PointF touchPoint, int regionsW, int regionsH, Size viewSize) {
        Rect activeRect = getCameraCharacteristics(mCameraId).get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Log.d("DEBUG_CODE","activeRect = "+activeRect.toString());
        PointF worldCoord = Utils.convertToWorldCoords(touchPoint, viewSize);
        PointF sensorCoord = Utils.convertToSensorCoords(Utils.getActiveSensorRect(getCameraCharacteristics(mCameraId), viewSize),
                worldCoord, viewSize);
        int width = Math.abs(regionsW * activeRect.width() / viewSize.getWidth());
        int height = Math.abs(regionsH * activeRect.height() / viewSize.getHeight());
        int rectX = Math.min(Math.max(activeRect.left + width/2, (int)sensorCoord.x), activeRect.right - width/2);
        int rectY = Math.min(Math.max(activeRect.top + height/2, (int)sensorCoord.y), activeRect.bottom - height/2);
        int weight = 1;
        MeteringRectangle[] rectangles = new MeteringRectangle[]{new MeteringRectangle(rectX, rectY,
                width, height, weight)};
        return rectangles;
    }

    public void applyRoi(CaptureRequest.Builder builder, MeteringRectangle[] aeRegions,
                         MeteringRectangle[] afRegions) {
        if (getCameraCharacteristics(mCameraId).get(
                CameraCharacteristics.CONTROL_MAX_REGIONS_AE) > 0) {
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, aeRegions);
        }
        if (getCameraCharacteristics(mCameraId).get(
                CameraCharacteristics.CONTROL_MAX_REGIONS_AF) > 0) {
            builder.set(CaptureRequest.CONTROL_AF_REGIONS, afRegions);
        }
    }

    public void lockFocus(final boolean Capture) {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest
                .CONTROL_AF_TRIGGER_START);
        try {
            mCameraCaptureSession.capture(mPreviewRequestBuilder.build(), new AutoFocusStatesDealer(
                            new AutoFocusStatesDealer.AutoFocusStateListener() {
                                @Override
                                public void onAutoFocusSuccess(CaptureResult result, boolean locked) {
                                    if(Capture) {
                                        captureStillPicture();
                                    }
                                }

                                @Override
                                public void onAutoFocusFail(CaptureResult result, boolean locked) {
                                    if(Capture) {
                                        captureStillPicture();
                                    }
                                }

                                @Override
                                public void onAutoFocusScan(CaptureResult result) {

                                }

                                @Override
                                public void onAutoFocusInactive(CaptureResult result) {

                                }

                                @Override
                                public void onManualFocusCompleted(CaptureResult result) {

                                }
                            }),
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void unlockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest
                .CONTROL_AF_TRIGGER_CANCEL);
        try {
            mCameraCaptureSession.capture(mPreviewRequestBuilder.build(), new AutoFocusStatesDealer(
                            new AutoFocusStatesDealer.AutoFocusStateListener() {
                                @Override
                                public void onAutoFocusSuccess(CaptureResult result, boolean locked) {

                                }

                                @Override
                                public void onAutoFocusFail(CaptureResult result, boolean locked) {

                                }

                                @Override
                                public void onAutoFocusScan(CaptureResult result) {

                                }

                                @Override
                                public void onAutoFocusInactive(CaptureResult result) {

                                }

                                @Override
                                public void onManualFocusCompleted(CaptureResult result) {

                                }
                            }),
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture() {
        if (hasCapability(mCameraId, CameraMetadata
                .REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING)) {
            TotalCaptureResult result = ImageCaptureManager.queueZslCapture();
            try {
                mCaptureRequestBuilder = mCameraDevice.createReprocessCaptureRequest(result);
                mCaptureRequestBuilder.addTarget(mJpegImageReader.getSurface());
                mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), new CameraCaptureSession

                        .CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull
                            CaptureRequest request, @NonNull TotalCaptureResult result) {
                        unlockFocus();
                        super.onCaptureCompleted(session, request, result);
                    }
                }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            try {
                mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                mCaptureRequestBuilder.addTarget(mJpegImageReader.getSurface());
                mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), new CameraCaptureSession

                        .CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull
                            CaptureRequest request, @NonNull TotalCaptureResult result) {
                        unlockFocus();
                        super.onCaptureCompleted(session, request, result);
                    }
                }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }
    }
}
