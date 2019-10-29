package com.antest1.kcanotify.h5;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

import org.xwalk.core.XWalkView;

public class GameXWalkView extends XWalkView implements GameView {
    public GameXWalkView(Context context) {
        super(context);
    }

    public GameXWalkView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameXWalkView(Context context, Activity activity) {
        super(context, activity);
    }


    @Override
    public void setLayoutParams(int width, int height) {
        ViewGroup.LayoutParams params = this.getLayoutParams();
        params.width = width;
        params.height = height;
        this.setLayoutParams(params);
    }

    @Override
    public void handleTouch(MotionEvent event) {
        // No need to handle touch specifically
        // All the mouse hover simulations are done in JavaScript
    }
}
