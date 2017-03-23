package com.fpil.android.remotesensor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom view class to show color of leaf in a Yellow:Green scale
 */
public class LeafColorView extends View {
    Paint gradpainter, markerpainter;
    float markerH, markerW, markerTh;
    RectF markerBox;

    float value = 0;

    public LeafColorView(Context context) {
        super(context);
        init(null, 0);
    }

    public LeafColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public LeafColorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.LeafColorView, defStyle, 0);

        float gradW = 0.F;
        gradW = a.getDimension(R.styleable.LeafColorView_gradientWidth, gradW);
        markerH = a.getDimension(R.styleable.LeafColorView_markerHeight, 0.F);
        markerW = a.getDimension(R.styleable.LeafColorView_markerWidth, 0.F);
        markerTh = a.getFloat(R.styleable.LeafColorView_markerThickness, 0.F);
        value = a.getFloat(R.styleable.LeafColorView_initMark, .5F);

        LinearGradient grad = new LinearGradient(0.F,0.F,gradW,0.F, 0xffffff00, 0xff00ff00, Shader.TileMode.MIRROR);

        gradpainter = new Paint();
        gradpainter.setShader(grad);

        markerpainter = new Paint();
        markerpainter.setStrokeWidth(markerTh);
        markerpainter.setStyle(Paint.Style.STROKE);
        markerpainter.setColor(0xffff0000);

        markerBox = new RectF();

        a.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        canvas.drawRect(0.F,markerH + markerTh,contentWidth,contentHeight - (markerH + markerTh), gradpainter);

        markerBox.left = contentWidth*value - markerW ;
        markerBox.right = contentWidth*value + markerW;
        markerBox.top = 0.F;
        markerBox.bottom = contentHeight;

        canvas.drawRect(markerBox,markerpainter);
    }

    public void setFractionalValue(float v) {
        value = v;
        this.invalidate();
    }
}
