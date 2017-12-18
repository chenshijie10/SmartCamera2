package com.lenovo.chensj.smartcamera2;

import java.nio.IntBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;

import com.lenovo.chensj.smartcamera2.filters.BaseFilterDrawer;
import com.lenovo.chensj.smartcamera2.filters.FilterFactory;
import com.lenovo.chensj.smartcamera2.filters.FilterParam;
import com.lenovo.chensj.smartcamera2.filters.FilterType;
import com.lenovo.chensj.smartcamera2.filters.renderer.MyRenderer;
import com.lenovo.chensj.smartcamera2.filters.renderer.OpenGlUtils;

public class MyGLSurfaceView extends GLSurfaceView {

    private BaseFilterDrawer drawer;
    private BaseFilterDrawer mCameraInputDrawer;
    public MyRenderer mRender;
    public FilterType mFilterType = FilterType.FreshFilter;
    float[] mMatrix = new float[16];
    public final static String PICTURES_DIRECTORY = Environment
            .getExternalStorageDirectory().toString() + "/DCIM/" + "UCam";
    private PictureTakenProcessListener mPictureTakenProcess = null;

    public interface PictureTakenProcessListener {
        void FilterPictureTaken(Bitmap bitmap, String Path);

        void FilterPictureSaveDone(Boolean result, String Path);
    }

    ;

    public void setFilterType(FilterType filterType) {
        mFilterType = filterType;
        mRender.setFilterType(mFilterType);
    }

    private class SaveFilterPhoto extends AsyncTask<Bitmap, Integer, Boolean> {
        String mPicFilePath = null;

        @Override
        protected Boolean doInBackground(Bitmap... params) {
            // TODO Auto-generated method stub
            if (params[0] == null) {
                return false;
            }
            mPicFilePath = PICTURES_DIRECTORY + "/GLCamera"
                    + System.currentTimeMillis() + ".jpeg";
            mPictureTakenProcess.FilterPictureTaken(params[0], mPicFilePath);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            mPictureTakenProcess.FilterPictureSaveDone(result, mPicFilePath);
            super.onPostExecute(result);
        }


    }

    public MyGLSurfaceView(Context context) {
        super(context);
        init(context);
        // TODO Auto-generated constructor stub
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // TODO Auto-generated method stub
        FilterParam.context = this.getContext();
        this.setEGLContextClientVersion(2);// @see android.opengl.GLSurfaceView
        this.mRender = new MyRenderer(this);
        this.setRenderer(this.mRender);//@see android.opengl.GLSurfaceView
        this.setZOrderMediaOverlay(true);//@see android.opengl.GLSurfaceView
        this.setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void setPictureTakenProcessListener(PictureTakenProcessListener
                                                       pictureTakenProcessListener) {
        mPictureTakenProcess = pictureTakenProcessListener;
    }

    public int makesurfaceTextureId() {
        // TODO Auto-generated method stub
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        int SurfaceTextId = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, SurfaceTextId);

        // Can't do mipmapping with camera source
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        // Clamp to edge is the only option
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(this.getContext().getResources(), R.drawable
                .grey, options);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        // Log.d("MyRender", "SurfaceTextId is Created");
        return SurfaceTextId;
    }

    public void skipframe(int num) {
        this.mRender.skipFrame(num);
    }

    public void setSurfaceTextureCreateListener(MyRenderer.SurfaceTextureCreateListener l) {
        if (mRender != null) {
            mRender.setSurfaceTextureCreateListener(l);
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mRender.getSurfaceTexture();
    }

    public void Rotation(int i) {
        // TODO Auto-generated method stub
        mRender.Rotation(i);
    }

    public void setCameraId(int currentId) {
        // TODO Auto-generated method stub
        mRender.setCameraId(currentId);
    }

    public void destroy() {
        if (mRender != null) {
            mRender.destroy();
        }
        mRender = null;
    }

    public void onPictureTaken(byte[] data, final int Oritention) {
        final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        queueEvent(new Runnable() {
            @Override
            public void run() {
                final Bitmap photo = drawPhoto(bitmap, Oritention);
                GLES20.glViewport(0, 0, mRender.mSurfaceWidth, mRender.mSurfaceHeight);
                if (photo != null)
                    Log.d("CHEN_DEBUG", "drawPhoto finish and go to save");
                new SaveFilterPhoto().execute(photo);
            }
        });
    }

    public Bitmap drawPhoto(Bitmap bitmap, int Rotation) {
        Matrix.setIdentityM(mMatrix, 0);
        Matrix.rotateM(mMatrix, 0, Rotation, 0, 0, 1);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.d("CHEN_DEBUG", "drawPhoto width = " + width + ", height = " + height);
        int[] mFrameBuffers = new int[1];
        int[] mFrameBufferTextures = new int[1];
        GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
        GLES20.glGenTextures(1, mFrameBufferTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

        GLES20.glViewport(0, 0, width, height);
        int textureId = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, true);
        Log.d("CHEN_DEBUG", "drawPhoto, textureId = " + textureId);
        if (drawer == null) {
            drawer = FilterFactory.CreateFilterDrawer(mFilterType);
            drawer.Init();
            drawer.onInputSizeChanged(width, height);
        }
        if (mCameraInputDrawer == null) {
            mCameraInputDrawer = FilterFactory.CreateFilterDrawer(FilterType.None);
            mCameraInputDrawer.Init();
            mCameraInputDrawer.onInputSizeChanged(width, height);
        }
//		mCameraInputDrawer.draw(textureId, null);
        drawer.draw(textureId, mMatrix);
        IntBuffer ib = IntBuffer.allocate(width * height);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.copyPixelsFromBuffer(ib);


        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
        GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);
        return result;
    }
}
