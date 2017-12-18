package com.lenovo.chensj.smartcamera2.filters;

import java.nio.FloatBuffer;
import java.util.LinkedList;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

public abstract class ColorFilterBase extends BaseFilterDrawer{
	private static final String TAG = "ColorFilterBase";
    private final LinkedList<Runnable> mRunOnDraw;
	protected static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
	protected String mFragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "void main() {\n" +
            "	vec4 color =  texture2D(inputImageTexture, textureCoordinate);\n"+
            " 	float grey = 0.299*color.r + 0.587*color.g  + 0.114*color.b;\n"+
            "	color.g = grey;\n"+
            "	color.r = grey;\n"+
            "	color.b =grey;\n"+
            "  gl_FragColor =color;\n" +
            "}\n";
    private final String mVertexShader =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 textureCoordinate;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * aPosition;\n" +
            "  textureCoordinate = (uSTMatrix * aTextureCoord).xy;\n" +
            "}\n";
    
	public ColorFilterBase(){
		super();
		mRunOnDraw = new LinkedList<>();
	}
	public ColorFilterBase(final String fragmentShader){
		super();
		Log.d("YYYYY","fragmentShader = "+fragmentShader);
		mFragmentShader = fragmentShader;
		mRunOnDraw = new LinkedList<>();
	}
    private int mProgram;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
    private int mSamplerHandle = -1;
	@Override
	public void loadProgram() {
		// TODO Auto-generated method stub
        mProgram = createProgram(mVertexShader, mFragmentShader);
        Log.d(TAG,"CHEN_DEBUG,loadProgram mProgram = "+mProgram);
        if (mProgram == 0) {
            return;
        }
        GLES20.glUseProgram(mProgram);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        mSamplerHandle = GLES20.glGetUniformLocation(mProgram, "inputImageTexture");
        Log.d(TAG,"CHEN_DEBUG,loadProgram mSamplerHandle = "+mSamplerHandle);
        if (mSamplerHandle == -1) {
        	throw new RuntimeException("Could not get attrib location for inputImageTexture");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
            }
        }
    public int getProgram() {
        return mProgram;
    }
	@Override
	public void draw(int surfaceTextureId, float[] mvpMatrix) {
		// TODO Auto-generated method stub
		GLES20.glUseProgram(mProgram);
		GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		runPendingOnDrawTasks();
        GLES20.glUniform1i(mSamplerHandle, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, surfaceTextureId);
        //mvpMatrix变换针对顶点坐标
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mTexStride);//liujl modify
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(maPositionHandle);
		GLES20.glDisableVertexAttribArray(maTextureHandle);
        onDrawArraysAfter();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
	}
	protected abstract void onDrawArraysAfter();
	protected abstract void onDrawArraysPre();
	public abstract String getFiltername();
	public final void Init(){
		onInit();
		onInitialized();
	}
	public void onInit() {
		// TODO Auto-generated method stub
		try{
			loadProgram();
		}
		catch(RuntimeException e){
			Log.d(TAG,"CHEN_DEBUG, loadProgram"+e.getMessage());
		}
		
	}
	public void onInitialized() {
		// TODO Auto-generated method stub
	}

	public final void destroy() {
	    GLES20.glDeleteProgram(mProgram);
	    onDestroy();
	}
	
	protected void onDestroy() {
	}
	
    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }
    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }
    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }
    public void onInputSizeChanged(final int width, final int height) {
        mIntputWidth = width;
        mIntputHeight = height;
    }
    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }
	
}
