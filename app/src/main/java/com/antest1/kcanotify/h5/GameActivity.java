package com.antest1.kcanotify.h5;

import android.content.res.Configuration;

public class GameActivity extends GameBaseActivity {
    @Override
    protected void onXWalkReady() {
        gameView.loadGame(prefs, GameConnection.Type.DMM);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);

        gameView.fitGameLayout();
    }

    int getLayoutResID() {
        return R.layout.activity_game_webview;
    }
}