package com.lenovo.chensj.smartcamera2.UI;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.lenovo.chensj.smartcamera2.R;

/**
 * Created by chensj12 on 2017/12/25.
 */

public class RoiView extends android.support.v7.widget.AppCompatImageView{

    private boolean mActiveFocus;
    private Context mContext;
    private FocusMode mFocusMode = FocusMode.Continous_picture;
    OnClickListener mOnClickListener;

    public void setOnClickListener(OnClickListener clickListener) {
        mOnClickListener = clickListener;
    }

    public enum FocusMode {Auto, Continous_picture}
    public enum FocusStates {FOCUSED_LOCKED, NOT_FOCUSED_LOCKED, ACTIVE_SCAN, PASSIVE_SCAN, INACTIVE};

    public RoiView(Context context) {
        super(context);
        mContext = context;
    }

    public RoiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setFocusMode(FocusMode focusMode){
        mFocusMode = focusMode;
    }

    public void setFocusPoints(PointF[] points){
        //todo: supported multi points focus
        if(points == null || points.length == 0){
            return;
        }
        PointF focusPoint = points[0];
        setX(focusPoint.x - getWidth()/2.f);
        setY(focusPoint.y - getHeight()/2.f);
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mOnClickListener.onClick(null);
                break;

        }
        return true;
    }

    public void setFocusState(FocusStates focusState){
        switch(focusState){
            case INACTIVE:
                mActiveFocus = true;
                setVisibility(INVISIBLE);
                break;
            case PASSIVE_SCAN:
                mActiveFocus = true;
                setImageResource(R.drawable.btn_expose_normal);
                setVisibility(VISIBLE);
                break;
            case ACTIVE_SCAN:
                // todo: show at the clicking point
                mActiveFocus = true;
                setImageResource(R.drawable.btn_expose_normal);
                setVisibility(VISIBLE);
                break;
            case FOCUSED_LOCKED:
                if(!mActiveFocus){
                    return;
                }
                mActiveFocus = false;
                setImageResource(R.drawable.btn_expose_active);
                setVisibility(VISIBLE);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mFocusMode == FocusMode.Continous_picture)
                        setVisibility(INVISIBLE);
                    }
                }, 500);
                break;
            case NOT_FOCUSED_LOCKED:
                if(!mActiveFocus){
                    return;
                }
                mActiveFocus = false;
                setImageResource(R.drawable.btn_expose_normal);
                setVisibility(VISIBLE);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mFocusMode == FocusMode.Continous_picture)
                        setVisibility(INVISIBLE);
                    }
                }, 200);
                break;
        }
    }
}
