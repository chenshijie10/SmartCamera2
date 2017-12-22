/*
 * Copyright (C) 2017 Motorola Mobility LLC
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 */

package com.lenovo.chensj.smartcamera2.camera;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

@SuppressWarnings("WeakerAccess")
public class CaptureHolder {
    private static final String TAG = CaptureHolder.class.getSimpleName();

    public String mCameraId;
    public long mSensorTimestamp;  //Pseudo timestamp, doesn't represent real time
    public long mCaptureTimestamp; //Real timestamp reflecting real time
    public CaptureRequest mRequest;
    public TotalCaptureResult mResult;
    public Image mRawImage;
    public Image mImage;

    public ByteBuffer getImageData() {
        return getImageData(false);
    }

    public ByteBuffer getImageData(boolean raw) {
        Image image = raw ? mRawImage : mImage;
        ByteBuffer data = image != null ? image.getPlanes()[0].getBuffer() : null;
        Log.d(TAG, raw ? "RAW" : "JPEG" + " callback data: " + data);
        if (data == null) Log.w(TAG, "Capture " + (raw ? "RAW" : "JPEG") + " is empty!");
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CaptureHolder that = (CaptureHolder) o;

        return (mCameraId != null ? mCameraId.equals(that.mCameraId) : that.mCameraId == null) &&
                mSensorTimestamp == that.mSensorTimestamp &&
                mCaptureTimestamp == that.mCaptureTimestamp &&
                (mRequest != null ? mRequest.equals(that.mRequest) : that.mRequest == null) &&
                (mResult != null ? mResult.equals(that.mResult) : that.mResult == null) &&
                (mRawImage != null ? mRawImage.equals(that.mRawImage) : that.mRawImage == null) &&
                (mImage != null ? mImage.equals(that.mImage) : that.mImage == null);
    }

    @Override
    public String toString() {
        return "CaptureHolder{" +
                "mCameraId=" + mCameraId +
                " mSensorTimestamp=" + mSensorTimestamp +
                " mCaptureTimestamp=" + mCaptureTimestamp +
                " mRequest=" + mRequest +
                " mResult=" + mResult +
                " mImage=" + mImage +
                " mRawImage=" + mRawImage +
                "}";
    }
}
