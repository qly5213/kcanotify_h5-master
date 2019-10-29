package com.antest1.kcanotify.h5;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
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
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameWebView extends WebView implements GameView{
    public GameWebView(Context context) {
        super(context);
    }

    public GameWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36";

    ExecutorService pool = Executors.newFixedThreadPool(5);

    private boolean activeInBackground = false;
    public void setActiveInBackground(boolean active) {
        activeInBackground = active;
    }

    private int changeCookieCnt = 0;
    private GameBaseActivity gameActivity = null;

    @Override
    public void assignActivity(GameBaseActivity activity) {
        gameActivity = activity;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE && activeInBackground) {
            super.onWindowVisibilityChanged(View.VISIBLE);
        } else {
            super.onWindowVisibilityChanged(visibility);
        }
    }

    @Override
    public void setLayoutParams(int width, int height) {
        ViewGroup.LayoutParams params = this.getLayoutParams();
        params.width = width;
        params.height = height;
        this.setLayoutParams(params);
    }

    boolean changeTouchEvent = false;

    public void handleTouch(MotionEvent event) {
        Log.d("touchEvent", event.getToolType(0) + ":" + event.getActionMasked());
        if(event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
            if(event.getAction() == MotionEvent.ACTION_MOVE) {
                buildMoveEvent(event);
            } else if(event.getAction() == MotionEvent.ACTION_DOWN && changeTouchEvent){
                buildMoveEvent(event);
            } else if(event.getAction() == MotionEvent.ACTION_UP && changeTouchEvent){
                buildMoveEvent(event);
            }
        }
    }

    String hostName = null;

    @Override
    public void onReadyDmm(SharedPreferences prefs) {

        if (prefs.getBoolean("background_play", true)) {
            this.setActiveInBackground(true);
        }


        boolean clearCookie = prefs.getBoolean("clear_cookie_start", false);
        if(clearCookie){
            CookieManager cookieManager = CookieManager.getInstance();
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

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(this, true);
        Set<Map.Entry<String, String>> voiceCookieMapSet = gameActivity.voiceCookieMap.entrySet();
        for(Map.Entry<String, String> voiceCookieMapEntry : voiceCookieMapSet){
            cookieManager.setCookie(voiceCookieMapEntry.getValue(), voiceCookieMapEntry.getKey());
        }
        if(prefs.getBoolean("change_cookie_start", false)) {
            Set<Map.Entry<String, String>> dmmCookieMapSet = gameActivity.dmmCookieMap.entrySet();
            for (Map.Entry<String, String> dmmCookieMapEntry : dmmCookieMapSet) {
                cookieManager.setCookie(dmmCookieMapEntry.getValue(), dmmCookieMapEntry.getKey());
            }
        }
        cookieManager.flush();

        WebSettings mWebSettings = this.getSettings();
        mWebSettings.setUserAgentString(USER_AGENT);
        mWebSettings.setBuiltInZoomControls(true);
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // 设置与Js交互的权限
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setMediaPlaybackRequiresUserGesture(false);

        WebView.setWebContentsDebuggingEnabled(true);

        //设置WebChromeClient类
        this.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
            }
            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    gameActivity.progressBar1.setVisibility(View.GONE);
                } else {
                    gameActivity.progressBar1.setVisibility(View.VISIBLE);
                    gameActivity.progressBar1.setProgress(newProgress);
                }
            }
        });

        //设置WebViewClient类
        this.setWebViewClient(new WebViewClient() {
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
                    webviewContentReSizeDmm();
                }
                if(view.getUrl() != null && view.getUrl().startsWith("https://www.dmm.com/my/-/login/=")){

                    new Handler().postDelayed(new Runnable(){
                        public void run() {
                            Set<Map.Entry<String, String>> dmmCookieMapSet = gameActivity.dmmCookieMap.entrySet();
                            for (Map.Entry<String, String> dmmCookieMapEntry : dmmCookieMapSet) {
                                GameWebView.this.evaluateJavascript("javascript:document.cookie = '" + dmmCookieMapEntry.getKey() + "';", new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        Log.i("KCVA", value);
                                    }
                                });
                            }
                        }
                    }, 5000);
                }
                if(view.getUrl() != null && view.getUrl().equals("http://www.dmm.com/top/-/error/area/") && prefs.getBoolean("change_cookie_start", false) && changeCookieCnt++ < 5) {
                    Log.i("KCVA", "Change Cookie");

                    new Handler().postDelayed(new Runnable(){
                        public void run() {
                            Set<Map.Entry<String, String>> dmmCookieMapSet = gameActivity.dmmCookieMap.entrySet();
                            for (Map.Entry<String, String> dmmCookieMapEntry : dmmCookieMapSet) {
                                GameWebView.this.evaluateJavascript("javascript:document.cookie = '" + dmmCookieMapEntry.getKey() + "';", new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        Log.i("KCVA", value);
                                    }
                                });
                            }
                            GameWebView.this.loadUrl("http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/");
                        }
                    }, 5000);
                }
            }
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if(view.getUrl() != null && view.getUrl().equals("http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/")) {
                    webviewContentReSizeDmm();
                }
            }
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
                Object[] result = gameActivity.interceptRequest(request.getUrl(), request.getMethod(), request.getRequestHeaders());
                if (result != null) {
                    return new WebResourceResponse((String) result[0], (String) result[1], (Integer) result[2], (String) result[3], (Map<String, String>) result[4], (InputStream) result[5]);
                } else {
                    return null;
                }
            }
        });

        this.addJavascriptInterface(new Object(){
            @android.webkit.JavascriptInterface
            public void JsToJavaInterface(String requestUrl, String param, String respData) {
                pool.execute(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        gameActivity.jsToJava(requestUrl, param, respData);
                    }
                }));
            }
        },"androidJs");

        this.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void update(String newFps) {
                gameActivity.updateFpsCounter(newFps);
            }
        },"fpsUpdater");

        this.loadUrl("http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/");
    }

    @Override
    public void onReadyOoi(SharedPreferences prefs) {

        if (prefs.getBoolean("background_play", true)) {
            this.setActiveInBackground(true);
        }

        hostName = prefs.getString("ooi_host_name", "ooi.moe");
        if(hostName.equals("")) hostName = "ooi.moe";

        boolean clearCookie = prefs.getBoolean("clear_cookie_start", false);
        if(clearCookie){
            CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
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

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(this, true);
        Set<Map.Entry<String, String>> voiceCookieMapSet = gameActivity.voiceCookieMap.entrySet();
        for(Map.Entry<String, String> voiceCookieMapEntry : voiceCookieMapSet){
            cookieManager.setCookie(voiceCookieMapEntry.getValue(), voiceCookieMapEntry.getKey());
        }
        if(prefs.getBoolean("change_cookie_start", false)) {
            Set<Map.Entry<String, String>> dmmCookieMapSet = gameActivity.dmmCookieMap.entrySet();
            for (Map.Entry<String, String> dmmCookieMapEntry : dmmCookieMapSet) {
                cookieManager.setCookie(dmmCookieMapEntry.getValue(), dmmCookieMapEntry.getKey());
            }
        }
        cookieManager.flush();

        WebSettings mWebSettings = this.getSettings();
        mWebSettings.setUserAgentString(USER_AGENT);
        mWebSettings.setBuiltInZoomControls(true);
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // 设置与Js交互的权限
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setMediaPlaybackRequiresUserGesture(false);

        WebView.setWebContentsDebuggingEnabled(true);

        //设置WebChromeClient类
        this.setWebChromeClient(new WebChromeClient() {


            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
            }


            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    gameActivity.progressBar1.setVisibility(View.GONE);
                } else {
                    gameActivity.progressBar1.setVisibility(View.VISIBLE);
                    gameActivity.progressBar1.setProgress(newProgress);
                }
            }
        });

        //设置WebViewClient类
        this.setWebViewClient(new WebViewClient() {
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
                if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/poi")) {
                    webviewContentReSizeOoi();
                } else if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/")){
                    boolean isAutoUser = prefs.getBoolean("ooi_auto_user", false);
                    if(isAutoUser){
                        String userName = prefs.getString("dmm_user", "");
                        String pwd = prefs.getString("dmm_pwd", "");
                        GameWebView.this.loadUrl("javascript:$(\"#login_id\").val(\""+userName+"\");$(\"#password\").val(\""+pwd+"\");document.getElementById(\"mode3\").checked = true;");
                    }
                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/poi")) {
                    webviewContentReSizeOoi();
                } else if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/")){
                    boolean isAutoUser = prefs.getBoolean("ooi_auto_user", false);
                    if(isAutoUser){
                        String userName = prefs.getString("dmm_user", "");
                        String pwd = prefs.getString("dmm_pwd", "");
                        GameWebView.this.loadUrl("javascript:$(\"#login_id\").val(\""+userName+"\");$(\"#password\").val(\""+pwd+"\");document.getElementById(\"mode3\").checked = true;");
                    }
                }
            }

            @Override
            public void onReceivedLoginRequest(WebView view, String realm, @Nullable String account, String args) {
                super.onReceivedLoginRequest(view, realm, account, args);
            }
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
                Object[] result = gameActivity.interceptRequest(request.getUrl(), request.getMethod(), request.getRequestHeaders());
                if (result != null) {
                    return new WebResourceResponse((String) result[0], (String) result[1], (Integer) result[2], (String) result[3], (Map<String, String>) result[4], (InputStream) result[5]);
                } else {
                    return null;
                }
            }
        });

        this.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void JsToJavaInterface(String requestUrl, String param, String respData) {
                loginExpire(requestUrl, respData, hostName);
                pool.execute(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        gameActivity.jsToJava(requestUrl, param, respData);
                    }
                }));
            }
        },"androidJs");

        this.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void update(String newFps) {
                gameActivity.updateFpsCounter(newFps);
            }
        },"fpsUpdater");

        this.loadUrl("http://" + hostName + "/poi");
    }

    public void pauseGame() {
        this.onPause();
        this.pauseTimers();
    }

    public void resumeGame() {
        this.onResume();
        this.resumeTimers();
    }


    public void webviewContentReSizeOoi() {
        this.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById(\"externalswf\"),gs=gf.style,gw=1200,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.right='0';gs.zIndex='100';gs.transformOrigin='46.9% 0px 0px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k} kancolleFit()})(document,window)");
    }

    public void webviewContentReSizeDmm() {
        this.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('game_frame'),gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';$.querySelector('.dmm-ntgnavi').style.display='none';$.querySelector('.area-naviapp').style.display='none';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.top='0px';gs.right='0';gs.zIndex='100';gs.transformOrigin='center top';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)");
    }

    @Override
    public void destroy() {
        this.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
        // TODO: explain why clear history
        this.clearHistory();
        ((ViewGroup) this.getParent()).removeView(this);
        super.destroy();
    }


    public void reloadGame() {
        this.reload();
    }

    private void buildMoveEvent(MotionEvent event){
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{new MotionEvent.PointerProperties()};
        event.getPointerProperties(0, pointerProperties[0]);
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{new MotionEvent.PointerCoords()};
        event.getPointerCoords(0, pointerCoords[0]);
        pointerProperties[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;
        pointerCoords[0].x -= this.getX();
        pointerCoords[0].y -= this.getY();
        long touchTime = SystemClock.uptimeMillis();
        this.onTouchEvent(MotionEvent.obtain(touchTime, touchTime, MotionEvent.ACTION_MOVE, 1,
                pointerProperties, pointerCoords, 0, 0, 0f, 0f,
                InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC, 0, InputDevice.SOURCE_TOUCHSCREEN, 0));
    }


    public void loginExpire(String requestUrl, String respData, String hostName){

        if(requestUrl.contains("api_req_member/get_incentive")){
            try {
                JSONObject respDataJson = new JSONObject(respData.substring(7));
                if(respDataJson.has("api_result") && respDataJson.getInt("api_result") != 1){
                    gameActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GameWebView.this.loadUrl("http://" + hostName + "/");
                        }
                    });
                    Toast.makeText(gameActivity, "登录过期，正在跳转到登录页面！", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}