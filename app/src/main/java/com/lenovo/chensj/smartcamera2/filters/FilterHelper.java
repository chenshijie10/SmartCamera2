package com.lenovo.chensj.smartcamera2.filters;

public class FilterHelper {
	public static String[] mFilterArray = new String[]{"None","BlackAndWhiteFilter","FreshFilter","ShallowMemoryFilter",
			"SketchFilter","ToasterFilter"};
	public static FilterType getFilterType(int position){
		switch(position){
		case 0:
			return FilterType.None;
		case 1:
			return FilterType.BlackAndWhiteFilter;
		case 2:
			return FilterType.FreshFilter;
		case 3:
			return FilterType.ShallowMemoryFilter;
		case 4:
			return FilterType.SketchFilter;
		case 5:
			return FilterType.ToasterFilter;
		default:
			return FilterType.None;
		}
	}
}
