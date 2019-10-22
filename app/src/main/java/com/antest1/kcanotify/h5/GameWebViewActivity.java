package com.antest1.kcanotify.h5;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public class GameWebViewActivity extends GameBaseActivity {
    ActiveWebView mWebview;
    WebSettings mWebSettings;
    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36";

    private int changeCookieCnt = 0;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebview = (ActiveWebView) super.mWebview;
        if (prefs.getBoolean("background_play", true)) {
            mWebview.setActiveInBackground(true);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        if(clearCookie){
            cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    if(value){
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("clear_cookie_start", false);
                        editor.apply();
                    }
                }
            });
        }

        cookieManager.setAcceptThirdPartyCookies(mWebview, true);

        Set<Map.Entry<String, String>> voiceCookieMapSet = voiceCookieMap.entrySet();
        for(Map.Entry<String, String> voiceCookieMapEntry : voiceCookieMapSet){
            cookieManager.setCookie(voiceCookieMapEntry.getValue(), voiceCookieMapEntry.getKey());
        }
        if(changeCookie) {
            Set<Map.Entry<String, String>> dmmCookieMapSet = dmmCokieMap.entrySet();
            for (Map.Entry<String, String> dmmCookieMapEntry : dmmCookieMapSet) {
                cookieManager.setCookie(dmmCookieMapEntry.getValue(), dmmCookieMapEntry.getKey());
            }
        }
        cookieManager.flush();

        mWebSettings = mWebview.getSettings();
        mWebSettings.setUserAgentString(USER_AGENT);
        mWebSettings.setBuiltInZoomControls(true);
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // 设置与Js交互的权限
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setMediaPlaybackRequiresUserGesture(false);

        WebView.setWebContentsDebuggingEnabled(true);
        //设置WebChromeClient类
        mWebview.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
            }
            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    progressBar1.setVisibility(View.GONE);
                } else {
                    progressBar1.setVisibility(View.VISIBLE);
                    progressBar1.setProgress(newProgress);
                }
            }
        });

        //设置WebViewClient类
        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            //设置加载前的函数
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                if(view.getUrl() != null && view.getUrl().equals("http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/")) {
                    webviewContentReSize();
                }
                if(view.getUrl() != null && view.getUrl().startsWith("https://www.dmm.com/my/-/login/=")){

                    new Handler().postDelayed(new Runnable(){
                        public void run() {
                            Set<Map.Entry<String, String>> dmmCookieMapSet = dmmCokieMap.entrySet();
                            for (Map.Entry<String, String> dmmCookieMapEntry : dmmCookieMapSet) {
                                mWebview.evaluateJavascript("javascript:document.cookie = '" + dmmCookieMapEntry.getKey() + "';", new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        Log.i("KCVA", value);
                                    }
                                });
                            }
                        }
                    }, 5000);
                }
                if(view.getUrl() != null && view.getUrl().equals("http://www.dmm.com/top/-/error/area/") && changeCookie && changeCookieCnt++ < 5) {
                    Log.i("KCVA", "Change Cookie");

                    new Handler().postDelayed(new Runnable(){
                        public void run() {
                            Set<Map.Entry<String, String>> dmmCookieMapSet = dmmCokieMap.entrySet();
                            for (Map.Entry<String, String> dmmCookieMapEntry : dmmCookieMapSet) {
                                mWebview.evaluateJavascript("javascript:document.cookie = '" + dmmCookieMapEntry.getKey() + "';", new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        Log.i("KCVA", value);
                                    }
                                });
                            }
                            mWebview.loadUrl("http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/");
                        }
                    }, 5000);
                }
            }
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if(view.getUrl() != null && view.getUrl().equals("http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/")) {
                    webviewContentReSize();
                }
            }
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
                Object[] result = interceptRequest(request.getUrl(), request.getMethod(), request.getRequestHeaders());
                if (result != null) {
                    return new WebResourceResponse((String) result[0], (String) result[1], (Integer) result[2], (String) result[3], (Map<String, String>) result[4], (InputStream) result[5]);
                } else {
                    return null;
                }
            }
        });

        mWebview.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void JsToJavaInterface(String requestUrl, String param, String respData) {
                jsToJava(requestUrl, param, respData);
            }
        },"androidJs");

        mWebview.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void update(String newFps) {
                updateFpsCounter(newFps);
            }
        },"fpsUpdater");

        mWebview.loadUrl("http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/");
    }

    @Override
    public void webviewContentReSize() {
        mWebview.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('game_frame'),gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';$.querySelector('.dmm-ntgnavi').style.display='none';$.querySelector('.area-naviapp').style.display='none';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.top='0px';gs.right='0';gs.zIndex='100';gs.transformOrigin='center top';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)");
    }
    @Override
    public void webviewPause() {
        mWebview.onPause();
        mWebview.pauseTimers();
    }

    @Override
    public void webviewResume() {
        mWebview.onResume();
        mWebview.resumeTimers();
    }

    @Override
    public void webviewDestory() {
        mWebview.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
        mWebview.clearHistory();
        ((ViewGroup) mWebview.getParent()).removeView(mWebview);
        mWebview.destroy();
        mWebview = null;
    }

    @Override
    void webviewReload() {
        mWebview.reload();
    }
}
