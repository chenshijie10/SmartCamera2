package com.lenovo.chensj.smartcamera2.filters.renderer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.lenovo.chensj.smartcamera2.UI.MyGLSurfaceView;
import com.lenovo.chensj.smartcamera2.filters.BaseFilterDrawer;
import com.lenovo.chensj.smartcamera2.filters.CameraInputDrawer;
import com.lenovo.chensj.smartcamera2.filters.FilterFactory;
import com.lenovo.chensj.smartcamera2.filters.FilterType;

public class MyRenderer implements GLSurfaceView.Renderer,
		SurfaceTexture.OnFrameAvailableListener {
	private BaseFilterDrawer drawer;
	private CameraInputDrawer mCameraInputDrawer;
	private MyGLSurfaceView mGLview;
	private FilterType mFilterType = FilterType.None;
	public int mSurfaceWidth;
	public int mSurfaceHeight;
	private float[] mMMatrix = new float[16];
	private float[] mMMatrix1 = new float[16];
	private float[] mOldMMatrix = new float[16];
	private float[] mRmatrix = new float[16];
	private float[] mLmatrix = new float[16];
	private int mSurfaceTextId;
	//fbo
    private FilterFbo mFbo;
    private int mTexutureIDArrary[] = new int[4];
    private Texture2DRenderer mLastRender = new Texture2DRenderer();
    
	private SurfaceTexture mSurfaceTexture;
	private boolean mUpdateSurface = false;
	private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
	private SurfaceTextureCreateListener mListener;
	private int mRotation = 0;
	private int CameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
	private int preCameraId = CameraId;
	private boolean mSkipFrame = false;
	private int mNum = 0;

	public MyRenderer(MyGLSurfaceView view) {
		mGLview = view;
//		drawer = new BlackAndWhiteFilter();
		drawer = FilterFactory.CreateFilterDrawer(mFilterType);
		mCameraInputDrawer = new CameraInputDrawer(CameraInputDrawer.Filter_InputImage_drawer);
		mFbo=new FilterFbo();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// TODO Auto-generated method stub
		Matrix.setIdentityM(mMMatrix, 0);
		Matrix.setIdentityM(mMMatrix1, 0);
		GLES20.glEnable(GLES20.GL_TEXTURE0);
//		Matrix.rotateM(mMMatrix1, 0, 270, 0, 0, 1);
//		Matrix.rotateM(mMMatrix, 0, 90, 0, 0, 1);
		// Matrix.rotateM(mMMatrix, 0, 180, 0, 1, 0);
		// Matrix.rotateM(mMMatrix, 0, 30, 0, 0, 1);
		// Matrix.translateM(mMMatrix, 1, 1, 0, 0);
		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		mSurfaceTextId = textures[0];
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mSurfaceTextId);

		// Can't do mipmapping with camera source
		GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		// Clamp to edge is the only option
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		/*
		 * Create the SurfaceTexture that will feed this textureID, and pass it
		 * to the camera
		 */
		if (mSurfaceTexture != null) {
			mSurfaceTexture.release();
			mSurfaceTexture = null;
		}

		if (mSurfaceTexture == null) {
			mSurfaceTexture = new SurfaceTexture(mSurfaceTextId);
			mSurfaceTexture.setOnFrameAvailableListener(this);
			drawer.Init();
			mLastRender.loadProgram();
			mCameraInputDrawer.Init();
		}

		if (mListener != null) {
			mListener.onSurfaceTextureCreated(mSurfaceTexture);
		}

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// TODO Auto-generated method stub
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		drawer.onInputSizeChanged(width, height);
		GLES20.glViewport(0, 0, width - 0, height - 0);
		float ratio = (float) width / height;
		Matrix.frustumM(mRmatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        if(mFbo!=null){
            mFbo.createFbo(width, height, GLES20.GL_TEXTURE_2D);
            mTexutureIDArrary[0] = mFbo.getTextureId();
            for(int i=0;i<3;i++){
                mTexutureIDArrary[i+1] = mFbo.createTexture(width, height, GLES20.GL_TEXTURE_2D);
            }
        }
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		// TODO Auto-generated method stub
//		if (CameraId != preCameraId) {
//			Matrix.rotateM(mMMatrix, 0, mRotation, 0, 1, 0);
//			preCameraId = CameraId;
//		}
		Matrix.setLookAtM(mLmatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.5f, 0.0f);
		// 计算投影和视图变换
		// Matrix.multiplyMM(mMMatrix, 0, mRmatrix, 0, mLmatrix, 0);
		// Matrix.rotateM(mMMatrix, 0, 90, 0, 0, 1);
        if(mFbo!=null){
            mFbo.bindFbo(GLES20.GL_TEXTURE0);
        }
		synchronized (this) {
			if (mUpdateSurface) {
				mSurfaceTexture.updateTexImage();
				GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
				GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
				mSurfaceTexture.getTransformMatrix(mMMatrix);
				mUpdateSurface = false;
			}
		}
		mCameraInputDrawer.drawCamerainput(mSurfaceTextId, mMMatrix);
        if(mFbo!=null){
            mFbo.unBindFbo(GLES20.GL_TEXTURE0);
        }
		// Log.d("MyRender", "draw is displayed");
//        mLastRender.drawTexture2D(mTexutureIDArrary[0]);
        drawer.draw(mTexutureIDArrary[0], mMMatrix1);
//        drawer.drawTexture2D(mTexutureIDArrary[0]);
//		GLES20.glEnable(GLES20.GL_BLEND);
//		GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_ONE_MINUS_SRC_COLOR);
//		// GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,
//		// GLES20.GL_ONE_MINUS_SRC_ALPHA);
//		// GLES20. glEnable(GLES20.GL_TEXTURE_2D);
//		 mLastRender.drawTexture2D(mGLview.makesurfaceTextureId());
//		GLES20.glDisable(GLES20.GL_BLEND);
		// drawer.draw(mSurfaceTextId ,mMMatrix);

	}
	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
		// TODO Auto-generated method stub
		mUpdateSurface = true;
		mGLview.requestRender();
		// Log.d("CHEN","onFrameAvailable");
	}

	public SurfaceTexture getSurfaceTexture() {
		return mSurfaceTexture;
	}

	public void setSurfaceTextureCreateListener(SurfaceTextureCreateListener l) {
		mListener = l;
	}

	public interface SurfaceTextureCreateListener {
		/**
		 * 用于在SurfaceTexture调用onSurfaceCreated时，回调给activity
		 */
		public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture);
	}

	public void Rotation(int i) {
		// TODO Auto-generated method stub
		mRotation = i;
	}

	public void skipFrame(int num) {
		mSkipFrame = true;
		mNum = num;
	}

	public void setCameraId(int currentId) {
		// TODO Auto-generated method stub
		CameraId = currentId;
	}
	public void destroy(){
		drawer.destroy();
	}

	public void setFilterType(FilterType filterType) {
		// TODO Auto-generated method stub
		if(mFilterType!=filterType){
			mFilterType = filterType;
			mGLview.queueEvent(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					if(drawer != null){
						drawer.destroy();
						drawer = null;
					}
					drawer = FilterFactory.CreateFilterDrawer(mFilterType);
					drawer.Init();
					drawer.onInputSizeChanged(mSurfaceWidth, mSurfaceHeight);
				}
				
			});

		}
	}
	
}
