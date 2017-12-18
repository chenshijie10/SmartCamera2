package com.lenovo.chensj.smartcamera2.filters;

import android.opengl.GLES20;

import com.lenovo.chensj.smartcamera2.R;
import com.lenovo.chensj.smartcamera2.filters.renderer.OpenGlUtils;

public class SketchFilter extends ColorFilterBase {
    private String mFilterName = "SketchFilter";
    private int mSingleStepOffsetLocation;
    //0.0 - 1.0
    private int mStrengthLocation;

    public SketchFilter() {
        super(OpenGlUtils.readShaderFromRawResource(R.raw.sketch, FilterParam.context));
    }

    public void onInit() {
        super.onInit();

        mSingleStepOffsetLocation = GLES20.glGetUniformLocation(getProgram(), "singleStepOffset");
        mStrengthLocation = GLES20.glGetUniformLocation(getProgram(), "strength");
    }

    public void onInitialized() {
        super.onInitialized();
        setFloat(mStrengthLocation, 0.5f);
    }

    protected void onDrawArraysAfter() {

    }

    private void setTexelSize(final float w, final float h) {
        setFloatVec2(mSingleStepOffsetLocation, new float[]{1.0f / w, 1.0f / h});
    }

    protected void onDrawArraysPre() {

    }

    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
        setTexelSize(width, height);
    }

    @Override
    public String getFiltername() {
        // TODO Auto-generated method stub
        return mFilterName;
    }
}
