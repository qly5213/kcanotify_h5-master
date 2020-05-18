package com.antest1.kcanotify.h5;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
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

import static com.antest1.kcanotify.h5.KcaConstants.KCANOTIFY_DB_VERSION;

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

    @Override
    public void destroy() {
        this.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
        // TODO: explain why need to clear history
        this.clearHistory();
        ((ViewGroup) this.getParent()).removeView(this);
        super.destroy();
    }

    public void setLayoutParams(int width, int height) {
        ViewGroup.LayoutParams params = this.getLayoutParams();
        params.width = width;
        params.height = height;
        this.setLayoutParams(params);
    }

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

    public void assignActivity(GameBaseActivity activity) {
        gameActivity = activity;
    }

    public void loadGame(SharedPreferences prefs, GameConnection.Type connectionType) {
        hostNameOoi = prefs.getString("ooi_host_name", "ooi.moe");
        if(hostNameOoi.equals("")) hostNameOoi = "ooi.moe";

        if(hostNameOoi.startsWith("http")) {
            hostNameOoi = hostNameOoi + "/poi";
        } else {
            hostNameOoi = "http://" + hostNameOoi + "/poi";
        }

        boolean backgroundPri = true;
        if (prefs.getBoolean("background_play", true)) {
            this.setActiveInBackground(true);
            backgroundPri = false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setRendererPriorityPolicy(RENDERER_PRIORITY_IMPORTANT, backgroundPri);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        if(prefs.getBoolean("clear_cookie_start", false)){
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

        WebView.setWebContentsDebuggingEnabled(false);

        if(prefs.getBoolean("clear_cache_start", false)) {
            this.clearCache(true);
            this.clearHistory();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("clear_cache_start", false);
            editor.apply();
        }

        //设置WebChromeClient类
        this.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
            }

            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                gameActivity.setProgressBarProgress(newProgress);
            }

            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    KcaDBHelper helper = new KcaDBHelper(gameActivity.getApplicationContext(), null, KCANOTIFY_DB_VERSION);
                    helper.recordErrorLog("WV", consoleMessage.sourceId(), ":" + consoleMessage.lineNumber(), "", consoleMessage.message());
                }
                return super.onConsoleMessage(consoleMessage);
            }
        });

        //设置WebViewClient类
        this.setWebViewClient(new WebViewClient() {
            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                detectGameStartAndFit(view);
                detectLoginAndFill(view, prefs);
                detectAndHandleLoginError(view, prefs);
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                if(gameActivity.prefs.getBoolean("ooi_host_auth", false)) {
                    handler.proceed(gameActivity.prefs.getString("ooi_host_auth_name", ""), gameActivity.prefs.getString("ooi_host_auth_pwd", ""));
                } else {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm);
                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                detectGameStartAndFit(view);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
                inspectTouchScenarioChanges(request.getUrl(), request.getMethod(), request.getRequestHeaders());
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
                detectLoginExpireAndReload(requestUrl, respData);
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

        switch (connectionType) {
            case DMM:
                this.loadUrl(GameConnection.DMM_START_URL);
                return;
            case OOI:
                this.loadUrl(hostNameOoi);
        }
    }

    public void pauseGame() {
        this.onPause();
        this.pauseTimers();
    }

    public void resumeGame() {
        this.onResume();
        this.resumeTimers();
    }

    public void reloadGame() {
        this.reload();
    }

    public void fitGameLayout() {
        this.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('externalswf');if(!gf)return;const gs=gf.style,gw=1200,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.right='0';gs.zIndex='100';gs.transformOrigin='46.9% 0px 0px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k} kancolleFit()})(document,window)");
        this.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('game_frame');if(!gf)return;const gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';$.querySelector('.dmm-ntgnavi').style.display='none';$.querySelector('.area-naviapp').style.display='none';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.top='0px';gs.right='0';gs.zIndex='100';gs.transformOrigin='center top';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)");
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE && activeInBackground) {
            super.onWindowVisibilityChanged(View.VISIBLE);
        } else {
            super.onWindowVisibilityChanged(visibility);
        }
    }

    private void detectLoginAndFill(WebView view, SharedPreferences prefs) {
        if (view.getUrl() != null && view.getUrl().contains(hostNameOoi.substring(0, hostNameOoi.length() - 3))) {
            boolean isAutoUser = prefs.getBoolean("ooi_auto_user", false);
            if (isAutoUser) {
                String userName = prefs.getString("dmm_user", "");
                String pwd = prefs.getString("dmm_pwd", "");
                view.loadUrl("javascript:$(\"#login_id\").val(\"" + userName + "\");$(\"#password\").val(\"" + pwd + "\");");
                view.loadUrl("javascript:document.getElementById(\"mode3\").checked = true;");
            }
        }
        // For DMM
        if(view.getUrl() != null && view.getUrl().startsWith("https://www.dmm.com/my/-/login/=")){
            new Handler().postDelayed(new Runnable(){
                public void run() {
                    Set<Map.Entry<String, String>> dmmCookieMapSet = gameActivity.dmmCookieMap.entrySet();
                    for (Map.Entry<String, String> dmmCookieMapEntry : dmmCookieMapSet) {
                        view.evaluateJavascript("javascript:document.cookie = '" + dmmCookieMapEntry.getKey() + "';", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                Log.i("KCVA", value);
                            }
                        });
                    }
                }
            }, 5000);
        }
    }

    private void detectGameStartAndFit(WebView view) {
        if (view.getUrl() != null && view.getUrl().equals(hostNameOoi)) {
            fitGameLayout();
        }
        if (view.getUrl() != null && view.getUrl().equals(GameConnection.DMM_START_URL)) {
            fitGameLayout();
        }
    }

    private void detectAndHandleLoginError(WebView view, SharedPreferences prefs) {
        // Only for DMM
        if(view.getUrl() != null && view.getUrl().equals("http://www.dmm.com/top/-/error/area/") && prefs.getBoolean("change_cookie_start", false) && changeCookieCnt++ < 5) {
            Log.i("KCVA", "Change Cookie");

            new Handler().postDelayed(new Runnable(){
                public void run() {
                    Set<Map.Entry<String, String>> dmmCookieMapSet = gameActivity.dmmCookieMap.entrySet();
                    for (Map.Entry<String, String> dmmCookieMapEntry : dmmCookieMapSet) {
                        view.evaluateJavascript("javascript:document.cookie = '" + dmmCookieMapEntry.getKey() + "';", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                Log.i("KCVA", value);
                            }
                        });
                    }
                    view.loadUrl(GameConnection.DMM_START_URL);
                }
            }, 5000);
        }
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

    private void detectLoginExpireAndReload(String requestUrl, String respData){
        // For OOI only
        if(requestUrl.contains("api_req_member/get_incentive") && requestUrl.contains(hostNameOoi.substring(0, hostNameOoi.length() - 3))){
            try {
                JSONObject respDataJson = new JSONObject(respData.substring(7));
                if(respDataJson.has("api_result") && respDataJson.getInt("api_result") != 1){
                    gameActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GameWebView.this.loadUrl(hostNameOoi.substring(0, hostNameOoi.length() - 3));
                        }
                    });
                    Toast.makeText(gameActivity, "登录过期，正在跳转到登录页面！", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void inspectTouchScenarioChanges(Uri uri, String requestMethod, Map<String, String> requestHeader) {
        String path = uri.getPath();
        if (path != null) {
            // TODO: simplify the logic
            if(path.contains("/kcsapi/api_port/port")){
                changeTouchEvent = false;
            } else if(path.contains("/kcsapi/api_get_member/mapinfo") || path.contains("/kcsapi/api_get_member/mission")){
                changeTouchEvent = true;
            }
            if ("GET".equals(requestMethod) && (path.startsWith("/kcs2/") || path.startsWith("/kcs/") || path.startsWith("/gadget_html5/js/kcs_inspection.js"))) {
                if (path.contains("organize_main.png") || path.contains("supply_main.png") || path.contains("remodel_main.png") || path.contains("repair_main.png") || path.contains("arsenal_main.png")) {
                    changeTouchEvent = true;
                }
            }
        }
    }

    private boolean changeTouchEvent = false;

    private String hostNameOoi = null;

    private boolean activeInBackground = false;

    private void setActiveInBackground(boolean active) {
        activeInBackground = active;
    }

    private int changeCookieCnt = 0;

    private GameBaseActivity gameActivity = null;

    private ExecutorService pool = Executors.newFixedThreadPool(5);

    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36";
}