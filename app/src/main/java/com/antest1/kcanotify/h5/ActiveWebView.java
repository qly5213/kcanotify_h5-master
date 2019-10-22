package com.antest1.kcanotify.h5;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

public class ActiveWebView extends WebView {

    private boolean activeInBackground = false;

    public ActiveWebView(Context context) {
        super(context);
    }

    public ActiveWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ActiveWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setActiveInBackground(boolean active) {
        activeInBackground = active;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE && activeInBackground) {
            super.onWindowVisibilityChanged(View.VISIBLE);
        } else {
            super.onWindowVisibilityChanged(visibility);
        }
    }
}