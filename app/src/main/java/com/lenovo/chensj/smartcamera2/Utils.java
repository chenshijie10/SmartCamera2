package com.lenovo.chensj.smartcamera2;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

	private static final String TAG = Utils.class.getSimpleName();

	private static int mScreenWidth = 0;
	private static int mScreenHeight = 0;
	private static int mViewWidth = 0;
	private static int mViewHeight = 0;
	
	private static float sPixelDensity = 1;
	public final static String SELECTED_ITEM = "SELECTED_ITEM";
	
	public static void initialize(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(metrics);
		sPixelDensity = metrics.density;
		mViewWidth = metrics.widthPixels;
		mViewHeight = metrics.heightPixels;
		Display display = wm.getDefaultDisplay();
		DisplayMetrics dm = new DisplayMetrics();
		@SuppressWarnings("rawtypes")
        Class c;
		try {
			c = Class.forName("android.view.Display");
			@SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
			method.invoke(display, dm);
			mScreenWidth = dm.widthPixels;
			mScreenHeight = dm.heightPixels;
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.d("DEBUG_CODE","mViewWidth = "+mViewWidth+", mViewHeight = "+mViewHeight
				+", mScreenWidth = "+mScreenWidth+", mScreenHeight = "+mScreenHeight);
	}

	public static boolean isNavigationBarSupported(){
		return mViewHeight != mScreenHeight;
	}

	public static int getScreenWidth() {
        return mScreenWidth;
    }
    
    public static int getScreenHeight() {
        return mScreenHeight;
    }

	/**
	 * 获取状态栏高度
	 * @param context
	 * @return StatusBar Height
	 */
	public static int getStatusBarHeight(Context context) {
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen",
				"android");
		if (resourceId > 0) {
			return context.getResources().getDimensionPixelSize(resourceId);
		}
		return 0;
	}

	/**
	 * 获取导航栏高度
	 * @param context
	 * @return NavigationBar Height
	 */
	public static int getNavigationBarHeight(Context context) {
		int resourceId;
		int rid = context.getResources().getIdentifier("config_showNavigationBar", "bool",
				"android");
		if (rid != 0) {
			resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen",
					"android");
			return context.getResources().getDimensionPixelSize(resourceId);
		} else
			return 0;
	}

	/**
	 * 显示或隐藏导航栏、状态栏
	 * @param activity
	 * @param show
	 */
	public static void setSystemUIVisible(Activity activity, boolean show) {
		if (show) {
			int uiFlags = View.SYSTEM_UI_FLAG_VISIBLE
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			activity.getWindow().getDecorView().setSystemUiVisibility(uiFlags);
		} else {
			int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			activity.getWindow().getDecorView().setSystemUiVisibility(uiFlags);
		}
	}

	/**
	 * {@link ColorStateList}.
	 *
	 * @param normal    normal color.
	 * @param highLight highLight color.
	 * @return {@link ColorStateList}.
	 */
	public static ColorStateList getColorStateList(@ColorInt int normal, @ColorInt int highLight) {
		int[][] states = new int[6][];
		states[0] = new int[]{android.R.attr.state_checked};
		states[1] = new int[]{android.R.attr.state_pressed};
		states[2] = new int[]{android.R.attr.state_selected};
		states[3] = new int[]{};
		states[4] = new int[]{};
		states[5] = new int[]{};
		int[] colors = new int[]{highLight, highLight, highLight, normal, normal, normal};
		return new ColorStateList(states, colors);
	}

	/**
	 * Time conversion.
	 *
	 * @param duration ms.
	 * @return such as: {@code 00:00:00}, {@code 00:00}.
	 */
	@NonNull
	public static String convertDuration(@IntRange(from = 1, to = Long.MAX_VALUE) long duration) {
		int hour = (int) (duration / 3600000);
		int minute = (int) ((duration - hour * 3600000) / 60000);
		int second = (int) (duration - hour * 3600000 - minute * 60000) / 1000;
		int msSecond = (int) (duration - hour * 3600000 - minute * 60000 - second * 1000);

		String hourValue = "";
		String minuteValue;
		String secondValue;
		if (hour > 0) {
			if (hour > 10) {
				hourValue = Integer.toString(hour);
			} else {
				hourValue = "0" + hour;
			}
			hourValue += ":";
		}
		if (minute > 0) {
			if (minute > 10) {
				minuteValue = Integer.toString(minute);
			} else {
				minuteValue = "0" + minute;
			}
			minuteValue += ":";
		} else {
			minuteValue = "";
		}
		if (second > 0) {
			if (second > 10) {
				secondValue = Integer.toString(second);
			} else {
				secondValue = "0" + second;
			}
		} else {
			secondValue = "00";
		}
		secondValue += ":";
		String msSecondValue = "";
		msSecond = msSecond / 10;
		if(msSecond >= 10){
			msSecondValue = Integer.toString(msSecond);
		} else {
			msSecondValue = "0" + Integer.toString(msSecond);
		}
		return "-" + hourValue + minuteValue + secondValue + msSecondValue;
	}

    /**
	 * dp转为px
	 * @param res
	 * @param dp
	 * @return
	 */
	public static int dpToPx(Resources res, int dp){
		int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
		return px;
	}
	
	/**
	 * dp转为Pixel
	 * @param dp
	 * @return
	 */
	public static int dpToPixel(int dp){
		return Math.round(sPixelDensity * dp);
	}
	
	/**
	 * 时间戳转时间
	 * @param format
	 * @param time
	 * @return
	 */
	public static String formatDate(String format, long time) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		String formatDate = sdf.format(new Date(time * 1000L));
		return formatDate;
	}
	
	/**
	 * 
	 * @param format
	 * @param oldFormat
	 * @param date
	 * @return
	 */
	public static String formatDate(String format, String oldFormat, String date) {
		SimpleDateFormat simpleFormat = new SimpleDateFormat(oldFormat);
		Date tempDate = null;
		try {
			tempDate = simpleFormat.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		simpleFormat = new SimpleDateFormat(format);
		String formatDate = simpleFormat.format(tempDate);
		return formatDate;
	}
	
	/**
	 * kb - mb -gb
	 * @param size
	 * @return
	 */
	public static String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
 
        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.2f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.2f KB", f);
        } else {
        	return String.format("%d B", size);
        }
    }

	/**
	 * 
	 * @param mss
	 * @return
	 */
	public static String formatDuration(Context context, long mss) {
		long day = 1000 * 60 * 60 * 24;
		long hour = 1000 * 60 * 60;
		long minute = 1000 * 60;
		long second = 1000;
		
		StringBuffer buffer = new StringBuffer();
		
		//if (mss >= day) {
		//	long days = mss / day; 
		//	buffer.append(days).append("天");
        //} else 
        if (mss >= hour) {
        	//long hours = (mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        	long hours = mss / (1000 * 60 * 60);
			buffer.append(hours).append(context.getString(context.getResources().getIdentifier("_duration_hour", "string", context.getPackageName())));
			mss = mss % day % hour;
        } 
        if (mss >= minute) {
        	//long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);  
        	long minutes = mss / (1000 * 60);  
			buffer.append(minutes).append(context.getString(context.getResources().getIdentifier("_duration_minute", "string", context.getPackageName())));
			mss = mss % minute;
        } 
        if (mss >= second){
        	long seconds = (mss % (1000 * 60)) / 1000;  
        	buffer.append(seconds).append(context.getString(context.getResources().getIdentifier("_duration_second", "string", context.getPackageName())));
        }
		
		return buffer.toString();
	}
	
	/**
     * 向缓存中存入视频文件缩略图
     * @param filePath
     */
	public static Bitmap putVideoThumbnailToMemoryCache(String filePath, String key) {
    	
    	if (TextUtils.isEmpty(filePath)) {
			return null;
		}
    	
    	Bitmap bitmap = createVideoThumbnailBitmap(filePath);
		
		return bitmap;
    }
	
	private static Bitmap createVideoThumbnailBitmap(String filePath) {
	    if (TextUtils.isEmpty(filePath)) {
	    	return null;
	    }
	    Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
        	retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime(-1);
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }

        return bitmap;
    }
	
	public static Bitmap scaleBitmap(Bitmap bitmap, int width, int height) {
        int newWidth = width;
        int newHeight = height;
        if (newHeight == 0) {
            newHeight = (int) (newWidth / (float) bitmap.getWidth() * bitmap.getHeight());
        }
        Bitmap result = Bitmap.createBitmap(newWidth, newHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Matrix matrix = new Matrix();
        float scaleX = 1;
        float scaleY = 1;
        scaleX = newWidth / (float) bitmap.getWidth();
        if (height != 0) {
            scaleY = newHeight / (float) bitmap.getHeight();
        } else {
            scaleY = scaleX;
        }
        matrix.postScale(scaleX, scaleY);
        canvas.drawBitmap(bitmap, matrix, null);
        return result;
    }

    /**
     *
     * @param view
     */
    public static void recycleBitmapFromImageView (View view) {
    	if (view instanceof ImageView) {
			ImageView imageView = (ImageView) view;
			recycleBitmapFromImageView(imageView);
		}
    }
    
    /**
     * 
     * @param imageView
     */
    private static void recycleBitmapFromImageView (ImageView imageView) {
    	Drawable drawable = imageView.getDrawable();
		if (drawable instanceof BitmapDrawable) {
			Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
			if (bitmap != null && !bitmap.isRecycled()) {
				imageView.setImageBitmap(null);
				bitmap.recycle();
				bitmap = null;
			}
		}
    }
    
    /**
	 * 根据一个网络连接获取bitmap图像
	 * @param uri
	 * @return bitmap
	 */
	public static Bitmap getBitmap(String uri) {
		// 显示网络上的图片
		Bitmap bitmap = null;
		try {
			URL url = new URL(uri);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.connect();
			InputStream is = conn.getInputStream();
			bitmap = BitmapFactory.decodeStream(is);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return bitmap;
	}
}
