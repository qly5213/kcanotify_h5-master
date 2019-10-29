package com.antest1.kcanotify.h5;

import android.content.res.Configuration;

public class GameActivity extends GameBaseActivity {
    @Override
    protected void onXWalkReady() {
        mWebview.onReadyDmm(prefs);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);

        mWebview.webviewContentReSizeDmm();
    }
}