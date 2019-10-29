package com.antest1.kcanotify.h5;

import android.view.MotionEvent;

public interface GameView {

    void setLayoutParams(int width, int height);

    void handleTouch(MotionEvent event);
}
