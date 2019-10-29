package com.antest1.kcanotify.h5;

import android.content.SharedPreferences;
import android.view.MotionEvent;

public interface GameView {
    void setLayoutParams(int width, int height);

    void handleTouch(MotionEvent event);

    // TODO: use the callback approach to decouple further
    void assignActivity(GameBaseActivity activity);

    // TODO: use injection to config connection
    void onReadyDmm(SharedPreferences prefs);
    void onReadyOoi(SharedPreferences prefs);

    void pauseGame();

    void resumeGame();

    void reloadGame();

    void destroy();

    // TODO: use injection to config connection
    void webviewContentReSizeDmm();
    void webviewContentReSizeOoi();
}
