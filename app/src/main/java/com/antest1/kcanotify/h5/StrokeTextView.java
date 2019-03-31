package com.antest1.kcanotify.h5;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

public class StrokeTextView extends AppCompatTextView
{
    private int mStrokeColor = Color.WHITE;
    private float mStrokeWidth = 0;

    public StrokeTextView(Context context) {
        this(context, null);
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStrokeColor = Color.BLACK;
        mStrokeWidth = 5;

        setTextColor(mStrokeColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 只有描边
        TextPaint paint = getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mStrokeWidth);
        super.onDraw(canvas);
    }
}
