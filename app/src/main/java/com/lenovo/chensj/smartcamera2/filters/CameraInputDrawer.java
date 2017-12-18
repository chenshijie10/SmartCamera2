package com.lenovo.chensj.smartcamera2.filters;

import java.util.LinkedList;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.util.Log;

public class CameraInputDrawer extends BaseFilterDrawer{
	String mFilterName = "none";
    private final LinkedList<Runnable> mRunOnDraw;
    public static final int Filter_None = 0;
    public static final int Filter_InputImage_drawer = 1;
	protected static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
	private String mFragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "	vec4 color =  texture2D(sTexture, vTextureCoord);\n"+
            "  gl_FragColor =color;\n" +
            "}\n";
    private final String mVertexShader =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * aPosition;\n" +
            "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
            "}\n";
    
	public CameraInputDrawer(int mode){
		Log.d("CHEN","CameraInputDrawer initVertices");
		super.initVertices();
		switch(mode){
		case Filter_None:
			mFragmentShader =
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform sampler2D sTexture;\n" +
            "void main() {\n" +
            "	vec4 color =  texture2D(sTexture, vTextureCoord);\n"+
            "  gl_FragColor =color;\n" +
            "}\n";
			break;
		case Filter_InputImage_drawer:
			mFragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "	vec4 color =  texture2D(sTexture, vTextureCoord);\n"+
            "  gl_FragColor =color;\n" +
            "}\n";
			break;
		}
		mRunOnDraw = new LinkedList<>();
		
	}
    private int mProgram;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
    private int mSamplerHandle;
	@Override
	public void loadProgram() {
		// TODO Auto-generated method stub
        mProgram = createProgram(mVertexShader, mFragmentShader);
        if (mProgram == 0) {
            return;
        }
        GLES20.glUseProgram(mProgram);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
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
	public void drawCamerainput(int surfaceTextureId, float[] mvpMatrix) {
		// TODO Auto-generated method stub
		GLES20.glUseProgram(mProgram);
		GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		runPendingOnDrawTasks();
        //liujl delete
       // GLES20.glUniform1i(mSamplerHandle, 0);
		//mvpMatrix是对纹理坐标的
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mSTMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mvpMatrix, 0);

        //liujl delete
        //mVertices.position(VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        //liujl delete
       // mVertices.position(VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mTexStride);//liujl modify
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        if(surfaceTextureId != -1){
        	GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, surfaceTextureId);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(maPositionHandle);
		GLES20.glDisableVertexAttribArray(maTextureHandle);
		if (surfaceTextureId != -1) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
		}
	}
	public final void Init(){
		onInit();
		onInitialized();
	}
	public void onInit() {
		// TODO Auto-generated method stub
		loadProgram();
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
    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }
	@Override
	public String getFiltername() {
		// TODO Auto-generated method stub
		return mFilterName;
	}
	@Override
	public void draw(int surfaceTextureId, float[] mvpMatrix) {
		// TODO Auto-generated method stub
		if(mvpMatrix == null){
			mvpMatrix = mMVPMatrix;
		}
		GLES20.glUseProgram(mProgram);
		GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		runPendingOnDrawTasks();
        //liujl delete
       // GLES20.glUniform1i(mSamplerHandle, 0);
		//mvpMatrix是对纹理坐标的
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

        //liujl delete
        //mVertices.position(VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        //liujl delete
       // mVertices.position(VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mTexStride);//liujl modify
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        if(surfaceTextureId != -1){
        	GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, surfaceTextureId);
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(maPositionHandle);
		GLES20.glDisableVertexAttribArray(maTextureHandle);
		if (surfaceTextureId != -1) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		}
	}
	
}
