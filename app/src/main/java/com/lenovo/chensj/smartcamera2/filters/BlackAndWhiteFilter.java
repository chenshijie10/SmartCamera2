package com.lenovo.chensj.smartcamera2.filters;

public class BlackAndWhiteFilter extends ColorFilterBase{
	
	private String mFilterName = "BlackandWhite";
	public BlackAndWhiteFilter(){
		super();
		mFragmentShader =
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
	}
	@Override
	protected void onDrawArraysAfter() {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void onDrawArraysPre() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getFiltername() {
		// TODO Auto-generated method stub
		return mFilterName;
	}
}
