package com.lenovo.chensj.smartcamera2.camera;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageWriter;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImageCaptureManager {
    private static final String TAG = ImageCaptureManager.class.getSimpleName();
    public static final int ZSL_DEPTH = 4;
    private static final int DEFAULT_REPROC_WRITE_BUFFER_CNT = 7;
    private Map<String, ReprocWriterHolder> mReprocWriterMap = new HashMap<>(1);
    private final Handler mHandler;
    private CaptureHolder mReprocCaptureHolder;
    private String mCameraId;

    private final Map<Long, CaptureHolder> mZslCaptureMap =
            new LinkedHashMap<Long, CaptureHolder>(ZSL_DEPTH) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, CaptureHolder> eldest) {
                    //当size大于ZSL_DEPTH，删除最老的缓存
                    boolean result = this.size() > ZSL_DEPTH;
                    if (result && eldest.getValue() != null && eldest.getValue().mImage != null) {
                        Log.v(TAG, "releasing zslImage timestamp:" +
                              eldest.getValue().mImage.getTimestamp());
                        eldest.getValue().mImage.close();
                    }
                    return result;
                }
            };
    private final Map<Long, TotalCaptureResult> mZslResultMap =
            new LinkedHashMap<Long, TotalCaptureResult>(ZSL_DEPTH) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, TotalCaptureResult> eldest) {
                    return this.size() > ZSL_DEPTH;
                }
            };

    private ImageCaptureManager(){
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }
    private static final class LazyLoader {
        private static final ImageCaptureManager INSTANCE = new ImageCaptureManager();
    }

    private static ImageCaptureManager getInstance() {
        return LazyLoader.INSTANCE;
    }
    public static void setupReprocSurfaceWriter(Surface surface, String cameraId) {
        Log.d(TAG, "setupReprocSurfaceWriter cid:" + cameraId);
        ImageCaptureManager instance = getInstance();
        instance.mCameraId = cameraId;
        ReprocWriterHolder holder = new ReprocWriterHolder();
        synchronized (holder.mImageWriterLock) {
            int reprocWriteBufferCnt = DEFAULT_REPROC_WRITE_BUFFER_CNT;
                Log.d(TAG, "cameraId:" + cameraId +
                        " MCF_REPROC_BUFFER_CNT:" + reprocWriteBufferCnt);
            holder.mImageWriter = ImageWriter.newInstance(surface, reprocWriteBufferCnt);
            holder.mImageWriter.setOnImageReleasedListener(new ImageWriter.OnImageReleasedListener() {
                @Override
                public void onImageReleased(ImageWriter writer) {
                    Log.d(TAG, "ImageWriter released image");
                }
            }, instance.mHandler);
        }
        instance.mReprocWriterMap.put(cameraId, holder);
    }

    public static void closeReprocWriter(String cameraId) {
        Log.d(TAG, "closeReprocWriter cid:" + cameraId);
        ImageCaptureManager instance = getInstance();
        ReprocWriterHolder holder = instance.mReprocWriterMap.get(cameraId);
        if (holder != null) {
            synchronized (holder.mImageWriterLock) {
                if (holder.mImageWriter != null) {
                    holder.mImageWriter.close();
                    holder.mImageWriter = null;
                }
            }
        }
        instance.mReprocWriterMap.remove(cameraId);
        //todo: release cache image.
        clearZslImages();
    }

    public static synchronized void onZslImage(Image image) {
        if (image == null) return;
        ImageCaptureManager instance = getInstance();
        Long timestamp = image.getTimestamp();
        TotalCaptureResult result = instance.mZslResultMap.get(timestamp);
        CaptureHolder holder = new CaptureHolder();
        holder.mImage = image;
        if (result != null) {
            Log.v(TAG, "onZslImage storing image in zsl map for timestamp:" + timestamp);
            holder.mRequest = result.getRequest();
            holder.mResult = result;
            holder.mSensorTimestamp = timestamp;
        } else {
            //RX image before result
        }
        instance.mZslCaptureMap.put(timestamp, holder);
    }

    public static synchronized TotalCaptureResult queueZslCapture() {
        Log.d(TAG, "queueZslCapture");
        ImageCaptureManager instance = getInstance();
        if (instance.mZslCaptureMap.isEmpty()) {
            Log.d(TAG, "ZSL queue is empty");
            return null;
        }
        CaptureHolder result = null;
        for (CaptureHolder holder : instance.mZslCaptureMap.values()) {
            if (holder.mImage != null && holder.mResult != null && holder.mRequest != null) {
                result = holder;
                break;
            }
        }
        if (result != null) {
            instance.mReprocCaptureHolder = instance.mZslCaptureMap.remove(result.mSensorTimestamp);
            if (instance.mReprocCaptureHolder == null) {
                Log.e(TAG, "Attempting to remove recently found capture failed!");
                return null;
            }
            instance.mReprocWriterMap.get(instance.mCameraId).mImageWriter.queueInputImage(
                    instance.mReprocCaptureHolder.mImage);
            return instance.mReprocCaptureHolder.mResult;
        }
        return null;
    }

    public static synchronized boolean onZslCaptureCompleted(CaptureRequest captureRequest,
                                                             TotalCaptureResult totalCaptureResult) {
        ImageCaptureManager instance = getInstance();
        Long timestamp = totalCaptureResult.get(CaptureResult.SENSOR_TIMESTAMP);
        instance.mZslResultMap.put(timestamp, totalCaptureResult);
        //We may get the image before the result, populate if this condition occurs
        CaptureHolder holder = instance.mZslCaptureMap.get(timestamp);
        if (holder != null) {
            //RX result after image
            holder.mRequest = captureRequest;
            holder.mResult = totalCaptureResult;
        }
        return true;
    }

    private static synchronized void clearZslImages() {
        Log.d(TAG, "clearZslImages");
        ImageCaptureManager instance = getInstance();

        //Release and clear ZSL capture map
        for (CaptureHolder holder : instance.mZslCaptureMap.values()) releaseImages(holder);
        instance.mZslCaptureMap.clear();
        instance.mZslResultMap.clear();
    }

    private static void releaseImages(CaptureHolder holder) {
        if (holder != null && (holder.mImage != null || holder.mRawImage != null)) {
            if (holder.mImage != null) {
                holder.mImage.close();
                holder.mImage = null;
                Log.d(TAG, "mImage closed");
            }
            if (holder.mRawImage != null) {
                holder.mRawImage.close();
                holder.mRawImage = null;
                Log.d(TAG, "mRawImage closed");
            }
        }
    }

    private static class ReprocWriterHolder {
        ImageWriter mImageWriter;
        final Object mImageWriterLock = new Object();
    }
}
