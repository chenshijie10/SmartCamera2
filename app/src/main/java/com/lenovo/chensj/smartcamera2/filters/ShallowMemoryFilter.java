package com.lenovo.chensj.smartcamera2.filters;

import android.opengl.GLES20;
import android.util.Log;

import com.lenovo.chensj.smartcamera2.R;
import com.lenovo.chensj.smartcamera2.filters.renderer.OpenGlUtils;

public class ShallowMemoryFilter extends ColorFilterBase{
	private String mFilterName = "ShallowMemoryFilter";
	private int saturation;
	private int type;
	//0.0 - 1.0
	private int mStrengthLocation;
	private int[] inputTextureHandles = {-1};
	private int[] inputTextureUniformLocations = {-1};
	public ShallowMemoryFilter(){
		super(OpenGlUtils.readShaderFromRawResource(R.raw.filter_nolomo, FilterParam.context));
	}
	public void onInit(){
		super.onInit();
		for(int i=0; i < inputTextureUniformLocations.length; i++)
			inputTextureUniformLocations[i] = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture"+(2+i));
		type = GLES20.glGetUniformLocation(getProgram(), "type");
		saturation = GLES20.glGetUniformLocation(getProgram(), "saturation");
        mStrengthLocation = GLES20.glGetUniformLocation(getProgram(), "strength");
	}
	
	public void onInitialized(){
		super.onInitialized();
		setFloat(mStrengthLocation, 1.0f);
		setFloat(saturation, 1.0f);
		setFloat(type, 0.01f);
	    runOnDraw(new Runnable(){
		    public void run(){
		    	inputTextureHandles[0] = OpenGlUtils.loadTexture(FilterParam.context, "filter/res_instagram_lomocolor.png");
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
		for(int i = 0; i < inputTextureHandles.length 
				&& inputTextureHandles[i] != OpenGlUtils.NO_TEXTURE; i++){
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + (i+3) );
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandles[i]);
			GLES20.glUniform1i(inputTextureUniformLocations[i], (i+3));
			Log.d("CHEN_DEBUG","inputTextureUniformLocations["+i+"] = "+inputTextureUniformLocations[i]);
		}
	}
    public void onInputSizeChanged(final int width, final int height) {
    	super.onInputSizeChanged(width, height);
    }
	@Override
	public String getFiltername() {
		// TODO Auto-generated method stub
		return mFilterName;
	}
}
