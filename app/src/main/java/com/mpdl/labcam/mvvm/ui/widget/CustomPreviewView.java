package com.mpdl.labcam.mvvm.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import timber.log.Timber;

public class CustomPreviewView extends PreviewView implements View.OnTouchListener {
    public static final int DEFAULT_SCALE_RATE = 2;

    private FocusCirceView focusCirceView;
    private CustomPreviewViewControl mControl;

    private boolean multiTouch;
    private float fingerSpacing;
    private int mDownViewX, mDownViewY, dx, dy;
    private boolean useTouchFocus = true;
    private boolean touchZoomEnable = true;
    private int touchAngle = 0;
    private int scaleRate = DEFAULT_SCALE_RATE;


    public CustomPreviewView(@NonNull Context context) {
        this(context,null);
    }

    public CustomPreviewView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CustomPreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CustomPreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        focusCirceView = new FocusCirceView(context);
        setOnTouchListener(this);

    }

    public void setPreviewViewControl(CustomPreviewViewControl control) {
        this.mControl = control;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isInEditMode()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        if (height < width * getDisplay().getHeight() / getDisplay().getWidth()) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * getDisplay().getHeight() / getDisplay().getWidth(),
                            MeasureSpec.EXACTLY));
        } else {
            super.onMeasure(MeasureSpec.makeMeasureSpec(height * getDisplay().getWidth() / getDisplay().getHeight(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }

    }


    public void cleanFocus(){
        Timber.d( "cleanFocus");
        try {
            focusCirceView.deleteCanvas();
            if (focusCirceView != null) {
                ((ViewGroup)focusCirceView.getParent()).removeView(focusCirceView);
            }
        }catch (Exception e){ }
    }

    private void focus(MotionEvent motionEvent){
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        if (mControl != null){
            mControl.focus(x,y);
        }
        cleanFocus();
        if (focusCirceView!=null) {
            Timber.d("onTouch:ACTION_DOWN____mX: " + x + "__mY" + y);
            focusCirceView.myViewScaleAnimation(focusCirceView);//animation
            focusCirceView.setPoint(x,y);
            addView(focusCirceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)); //add FocusCirceView
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (event.getPointerCount() > 1) {
            multiTouch = true;
            if (!touchZoomEnable) {
                return true;
            }
            float currentFingerSpacing = getFingerSpacing(event);
            if (fingerSpacing != 0) {
                try {
                    if (mControl != null) {
                        int maxZoom = (int) (mControl.getMaxZoom() * 100);
                        int zoom = (int) (mControl.getZoom() * 100);
                        if (fingerSpacing < currentFingerSpacing && zoom < maxZoom) {
                            zoom += scaleRate;
                        } else if (currentFingerSpacing < fingerSpacing && zoom > 0) {
                            zoom -= scaleRate;
                        }
                        mControl.setZoom(zoom * 0.01f);
                    }
                } catch (Exception e) {
                    Timber.e( "onTouch error : " + e);
                }
            }
            fingerSpacing = currentFingerSpacing;
        } else {
            if (event.getPointerCount() == 1) {
                int x = (int) event.getRawX();
                int y = (int) event.getRawY();
                if (action == MotionEvent.ACTION_DOWN) {
                    mDownViewX = x;
                    mDownViewY = y;
                }
                if (action == MotionEvent.ACTION_MOVE) {
                    dx = x - mDownViewX;
                    dy = y - mDownViewY;
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (multiTouch) {
                        multiTouch = false;
                    } else {
                        if (Math.abs(dx) > 100 && touchAngle == 0) {
                            notifyMove(dx < -100);
                            return true;
                        }
                        if (Math.abs(dy) > 100 && touchAngle == 90){
                            notifyMove(dx >= -100);
                            return true;
                        }
                        if (Math.abs(dy) > 100 && touchAngle == -90){
                            notifyMove(dx <= -100);
                            return true;
                        }
                        if (useTouchFocus) {
                            focus(event);
                        }
                    }
                }
            }
        }
        return true;
    }

    private void notifyMove(boolean left) {
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


    public interface CustomPreviewViewControl {
        float getMaxZoom();
        float getMinZoom();
        float getZoom();
        void setZoom(float zoom);
        void focus(float x,float y);

    }



}
