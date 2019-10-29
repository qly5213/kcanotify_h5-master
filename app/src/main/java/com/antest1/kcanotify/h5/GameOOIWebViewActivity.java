package com.antest1.kcanotify.h5;

import android.content.res.Configuration;
import android.os.Bundle;

public class GameOOIWebViewActivity extends GameBaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWebview.onReadyOoi(prefs);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);

        mWebview.webviewContentReSizeOoi();
    }
}
