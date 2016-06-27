package org.droidplanner.android.fragments.account.editor.tool;

import android.content.Context;
import android.gesture.GestureOverlayView;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;

import org.droidplanner.android.fragments.helpers.GestureMapFragment;

/**
 * Created by Kevin on 17.05.2016.
 */
public class CustomGestureOverlayView extends GestureOverlayView {

    private Rect rect = new Rect();

    public CustomGestureOverlayView(Context context) {
        super(context);
    }

    public CustomGestureOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomGestureOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public enum GestureMode{
        FREE,
        RECTANGLE
    }

    private GestureMode currentMode = GestureMode.FREE;

    public void setMode(GestureMode mode){
        currentMode = mode;
    }

    public GestureMode getMode(){
        return currentMode;
    }

    public void drawRect(PointF point1, PointF point2) {
        this.rect.set((int)point1.x, (int)point1.y,(int)point2.x,(int)point2.y);
    }

    public void removeRect() {
        this.rect.setEmpty();
    }

    protected void onDraw(Canvas canvas) {
        Paint paint=new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        canvas.drawRect(rect, paint);

    }


}
