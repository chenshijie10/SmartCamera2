package com.lenovo.chensj.smartcamera2.filters;

import android.opengl.GLES20;
import android.util.Log;

import com.lenovo.chensj.smartcamera2.R;
import com.lenovo.chensj.smartcamera2.filters.renderer.OpenGlUtils;

public class FreshFilter extends ColorFilterBase{
	
	private String mFilterName = "Fresh";
	private int[] inputTextureHandles = {-1,-1,-1,-1,-1};
	private int[] inputTextureUniformLocations = {-1,-1,-1,-1,-1};
	protected int mGLStrengthLocation;

	public FreshFilter(){
		super(OpenGlUtils.readShaderFromRawResource(R.raw.earlybird, FilterParam.context));
	}
	public void onInit(){
		super.onInit();
		
		for(int i=0; i < inputTextureUniformLocations.length; i++)
			inputTextureUniformLocations[i] = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture"+(2+i));
		mGLStrengthLocation = GLES20.glGetUniformLocation(getProgram(),
				"strength");
		Log.d("CHEN_DEBUG","FreshFilter onInit(), mGLStrengthLocation = "+mGLStrengthLocation);
	}
	
	public void onInitialized(){
		super.onInitialized();
		setFloat(mGLStrengthLocation, 1.0f);
	    runOnDraw(new Runnable(){
		    public void run(){
		    	inputTextureHandles[0] = OpenGlUtils.loadTexture(FilterParam.context, "filter/earlybirdcurves.png");
				inputTextureHandles[1] = OpenGlUtils.loadTexture(FilterParam.context, "filter/earlybirdoverlaymap_new.png");
				inputTextureHandles[2] = OpenGlUtils.loadTexture(FilterParam.context, "filter/vignettemap_new.png");
				inputTextureHandles[3] = OpenGlUtils.loadTexture(FilterParam.context, "filter/earlybirdblowout.png");
				inputTextureHandles[4] = OpenGlUtils.loadTexture(FilterParam.context, "filter/earlybirdmap.png");
		    }
	    });
	}
	protected void onDrawArraysAfter(){
		for(int i = 0; i < inputTextureHandles.length
				&& inputTextureHandles[i] != OpenGlUtils.NO_TEXTURE; i++){
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i+3));
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		}
	}
	  
	protected void onDrawArraysPre(){
//		Log.d("CHEN_DEBUG","inputTextureHandles[0] = "+inputTextureHandles[0]);
//		Log.d("CHEN_DEBUG","inputTextureHandles[1] = "+inputTextureHandles[1]);
//		Log.d("CHEN_DEBUG","inputTextureHandles[2] = "+inputTextureHandles[2]);
//		Log.d("CHEN_DEBUG","inputTextureHandles[3] = "+inputTextureHandles[3]);
//		Log.d("CHEN_DEBUG","inputTextureHandles[4] = "+inputTextureHandles[4]);

		for(int i = 0; i < inputTextureHandles.length 
				&& inputTextureHandles[i] != OpenGlUtils.NO_TEXTURE; i++){
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i+3) );
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandles[i]);
			GLES20.glUniform1i(inputTextureUniformLocations[i], (i+3));
			Log.d("CHEN_DEBUG","inputTextureUniformLocations["+i+"] = "+inputTextureUniformLocations[i]);
		}
	}
	@Override
	public String getFiltername() {
		// TODO Auto-generated method stub
		return mFilterName;
	}
}
