package com.lenovo.chensj.smartcamera2.filters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public abstract class BaseFilterDrawer {

    private static final String TAG = "BaseFilterDrawer";
    private static final int FLOAT_SIZE_BYTES = 4;
    protected static final int VERTICES_DATA_STRIDE_BYTES = 2 * FLOAT_SIZE_BYTES;//liujl modify
    protected float[] mSTMatrix = new float[16];
    protected float[] mMVPMatrix = new float[16];
//liujl add begin
    private static final float RECTANGLE_COORDS[] = {
        -1.0f, -1.0f,   // 0 bottom left
        1.0f, -1.0f,   // 1 bottom right
        -1.0f, 1.0f,   // 2 top left
        1.0f, 1.0f,   // 3 top right
};
    protected static final float RECTANGLE_TEX_COORDS[] = {
        0.0f, 0.0f,     // 0 bottom left
        1.0f, 0.0f,     // 1 bottom right
        0.0f, 1.0f,     // 2 top left
        1.0f, 1.0f      // 3 top right
};
// liujl add end

    protected FloatBuffer mVertices;
    protected FloatBuffer mTexStride;
    protected int mIntputWidth;
    protected int mIntputHeight;
    public BaseFilterDrawer(){
    	initVertices();
    	Log.d("CHEN","BaseFilterDrawer initVertices");
    }
    public void initVertices() {
    	
        mVertices = ByteBuffer.allocateDirect(RECTANGLE_COORDS.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(RECTANGLE_COORDS).position(0);
        mTexStride = ByteBuffer.allocateDirect(RECTANGLE_TEX_COORDS.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTexStride.put(RECTANGLE_TEX_COORDS).position(0);
        Matrix.setIdentityM(mSTMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }
    protected abstract void loadProgram();
//liujl delete
//        mSamplerHandle = GLES20.glGetUniformLocation(mProgram, "sTexture");
//        if (mSamplerHandle == -1) {
//            throw new RuntimeException("Could not get attrib location for sTexture");
//        }
    public abstract void draw(int surfaceTextureId, float []mvpMatrix);

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                //Log.e(TAG, "Could not compile shader " + shaderType + ":");
               // Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    public int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        Log.d(TAG,"CHEN_DEBUG,loadProgram vertexShader = "+vertexShader);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        Log.d(TAG,"CHEN_DEBUG,loadProgram pixelShader = "+pixelShader);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, pixelShader);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
               //Log.e(TAG, "Could not link program: ");
                //Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }
    public abstract void Init();
    public abstract void destroy();
    public void onInputSizeChanged(final int width, final int height) {
        mIntputWidth = width;
        mIntputHeight = height;
    }
	public abstract String getFiltername();
}
