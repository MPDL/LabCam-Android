package com.mpdl.labcam.mvvm.ui.widget;
 
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import androidx.annotation.Nullable;


public class FocusCirceView extends View {
 
 
    private Paint paint;
    private static final String TAG = "FocusCirceView";
    private float mX = getWidth()/2; //default
    private float mY = getHeight()/2;
    public FocusCirceView(Context context) {
        super(context);
    }
 
    public FocusCirceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
 
    public void setPoint(float x, float y){
        this.mX = x;
        this.mY = y;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        paint = new Paint();
        paint.setColor(Color.parseColor("#cccccc"));
        paint.setStyle(Paint.Style.STROKE);//Hollow circle
        paint.setStrokeWidth(4);
        Log.d(TAG, "draw: "+"width:"+getWidth()+"___height:"+getHeight());
        canvas.drawCircle(mX,mY,20,paint);
        canvas.drawCircle(mX,mY,90,paint);
    }
 
    public void deleteCanvas(){
        paint.reset();
        invalidate();
    }
 
    /***
     * Zoom animation
     */
    public void myViewScaleAnimation(View myView) {
        ScaleAnimation animation = new ScaleAnimation(1.1f, 1f, 1.1f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        animation.setDuration(300);
        animation.setFillAfter(false);
        animation.setRepeatCount(0);
        myView.startAnimation(animation);
    }
 
}