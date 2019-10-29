package com.antest1.kcanotify.h5;

import android.content.res.Configuration;

public class GameOOIActivity extends GameBaseActivity {
    @Override
    protected void onXWalkReady() {
        mWebview.onReadyOoi(prefs);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);

        mWebview.webviewContentReSizeOoi();
    }
}
