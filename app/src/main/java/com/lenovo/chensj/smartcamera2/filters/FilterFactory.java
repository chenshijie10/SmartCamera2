package com.lenovo.chensj.smartcamera2.filters;

public class FilterFactory{
	
	public FilterFactory(){};
	public static BaseFilterDrawer CreateFilterDrawer(FilterType type){
		switch(type){
		case BlackAndWhiteFilter:
			return new BlackAndWhiteFilter();
		case SketchFilter:
			return new SketchFilter();
		case FreshFilter:
			return new FreshFilter();
		case ToasterFilter:
			return new ToasterFilter();
		case ShallowMemoryFilter:
			return new ShallowMemoryFilter();
		case None:
		default:
			return new CameraInputDrawer(CameraInputDrawer.Filter_None);
		}
	}
}
