package com.antest1.kcanotify.h5;

import android.content.res.Configuration;
import android.os.Bundle;

public class GameWebViewActivity extends GameBaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameView.onReadyDmm(prefs);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);

        gameView.fitGameLayout();
    }

    int getLayoutResID() {
        return R.layout.activity_game_webview_ori;
    }
}
