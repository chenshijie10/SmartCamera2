package com.lenovo.chensj.smartcamera2.camera;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by chensj12 on 2017/12/22.
 */

public class AutoFocusStatesDealer extends CameraCaptureSession.CaptureCallback {

    private static String TAG = AutoFocusStatesDealer.class.getSimpleName();
    private AutoFocusStateListener mStateListener;
    private Integer mAFMode = CaptureRequest.CONTROL_AF_MODE_AUTO;

    public interface AutoFocusStateListener {
        //对焦成功
        void onAutoFocusSuccess(CaptureResult result, boolean locked);
        //对焦失败
        void onAutoFocusFail(CaptureResult result, boolean locked);
        //正在对焦
        void onAutoFocusScan(CaptureResult result);
        //不支持对焦或af mode off
        void onAutoFocusInactive(CaptureResult result);
        //手动对焦
        void onManualFocusCompleted(CaptureResult result);
    }

    public AutoFocusStatesDealer(AutoFocusStateListener stateListener){
        mStateListener = stateListener;
    }
    @Override
    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest
            request, long timestamp, long frameNumber) {
        super.onCaptureStarted(session, request, timestamp, frameNumber);
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        super.onCaptureProgressed(session, request, partialResult);
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);
        mAFMode = request.get(CaptureRequest.CONTROL_AF_MODE);

        dealCaptureResult(result);
    }

    private void dealCaptureResult(CaptureResult result) {
        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
        Integer afMode = result.get(CaptureResult.CONTROL_AF_MODE);
        Integer lensState = result.get(CaptureResult.LENS_STATE);
        if (lensState == null) lensState = CameraMetadata.LENS_STATE_STATIONARY;
        if (afState == null) {
            Log.w(TAG, "onCaptureCompleted - missing android.control.afState !");
            return;
        } else if (afMode == null) {
            Log.w(TAG, "onCaptureCompleted - missing android.control.afMode !");
            return;
        }
        if(mAFMode != CaptureRequest.CONTROL_AF_MODE_OFF) {
            switch (afState) {
                case CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED:
                    mStateListener.onAutoFocusSuccess(result, /*locked*/true);
                    break;
                case CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
                    mStateListener.onAutoFocusFail(result, /*locked*/true);
                    break;
                case CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED:
                    mStateListener.onAutoFocusSuccess(result, /*locked*/false);
                    break;
                case CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED:
                    mStateListener.onAutoFocusFail(result, /*locked*/false);
                    break;
                case CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN:
                    mStateListener.onAutoFocusScan(result);
                    break;
                case CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN:
                    mStateListener.onAutoFocusScan(result);
                    break;
                case CaptureResult.CONTROL_AF_STATE_INACTIVE:
                    mStateListener.onAutoFocusInactive(result);
                    break;
            }
        } else {
            if (CameraMetadata.LENS_STATE_STATIONARY == lensState) {
                mStateListener.onManualFocusCompleted(result);
            }

        }
    }
    //设置焦距。
    public synchronized void setManualFocus(float focalLength, CaptureRequest.Builder
            repeatingBuilder) {
        repeatingBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureResult.CONTROL_AF_MODE_OFF);
        repeatingBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focalLength);
    }

    public void autoFocus(CaptureRequest.Builder builder){

    }

}
