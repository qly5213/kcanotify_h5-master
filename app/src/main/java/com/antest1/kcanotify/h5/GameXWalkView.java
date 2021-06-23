package com.antest1.kcanotify.h5;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.widget.Toast;

import org.json.JSONObject;
import org.xwalk.core.JavascriptInterface;
import org.xwalk.core.XWalkCookieManager;
import org.xwalk.core.XWalkHttpAuthHandler;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.antest1.kcanotify.h5.KcaConstants.KCANOTIFY_DB_VERSION;

public class GameXWalkView extends XWalkView implements GameView {
    public GameXWalkView(Context context) {
        super(context);
    }

    public GameXWalkView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameXWalkView(Context context, Activity activity) {
        super(context, activity);
    }

    public void destroy() {
        this.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
        ((ViewGroup) this.getParent()).removeView(this);
        super.onDestroy();
    }

    public void setLayoutParams(int width, int height) {
        ViewGroup.LayoutParams params = this.getLayoutParams();
        params.width = width;
        params.height = height;
        this.setLayoutParams(params);
    }

    public void handleTouch(MotionEvent event) {
        // No need to handle touch specifically
        // All the mouse hover simulations are done in JavaScript
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

        if(prefs.getBoolean("clear_cookie_start", false)){
            (new XWalkCookieManager()).removeAllCookie();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("clear_cookie_start", false);
            editor.apply();
        }

        XWalkCookieManager cookieManager = new XWalkCookieManager();
        cookieManager.setAcceptCookie(true);
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
        cookieManager.flushCookieStore();

        XWalkSettings mWebSettings = this.getSettings();
        mWebSettings.setUserAgentString(USER_AGENT);
        mWebSettings.setBuiltInZoomControls(false);
        mWebSettings.setCacheMode(XWalkSettings.LOAD_NO_CACHE);
        // 设置与Js交互的权限
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setMediaPlaybackRequiresUserGesture(false);

        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, false);


        if(prefs.getBoolean("clear_cache_start", false)) {
            this.clearCache(true);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("clear_cache_start", false);
            editor.apply();
        }

        //设置WebChromeClient类
        this.setUIClient(new XWalkUIClient(this) {
            //获取网站标题
            @Override
            public void onReceivedTitle(XWalkView view, String title) {
            }

            @Override
            public boolean onConsoleMessage(XWalkView view, String message, int lineNumber, String sourceId, XWalkUIClient.ConsoleMessageType messageType){
                if (messageType == XWalkUIClient.ConsoleMessageType.ERROR) {
                    KcaDBHelper helper = new KcaDBHelper(gameActivity.getApplicationContext(), null, KCANOTIFY_DB_VERSION);
                    helper.recordErrorLog("CW", sourceId, ":" + lineNumber, "", message);
                }
                return super.onConsoleMessage(view, message, lineNumber, sourceId, messageType);
            }
        });

        //设置WebViewClient类
        this.setResourceClient(new XWalkResourceClient(this) {
            //获取加载进度
            @Override
            public void onProgressChanged(XWalkView view, int newProgress) {
                gameActivity.setProgressBarProgress(newProgress);
            }

            @Override
            public void onReceivedHttpAuthRequest(XWalkView view, XWalkHttpAuthHandler handler, String host, String realm) {
                if(gameActivity.prefs.getBoolean("ooi_host_auth", false)) {
                    handler.proceed(gameActivity.prefs.getString("ooi_host_auth_name", ""), gameActivity.prefs.getString("ooi_host_auth_pwd", ""));
                } else {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm);
                }
            }

            //设置加载前的函数
            @Override
            public void onLoadStarted(XWalkView view, String url) {
                super.onLoadStarted(view, url);
                detectGameStartAndFit(view);
            }

            //设置结束加载函数
            @Override
            public void onLoadFinished(XWalkView view, String url) {
                detectGameStartAndFit(view);
                detectLoginAndFill(view, prefs);
                detectAndHandleLoginError(view, prefs);
            }

            @Override
            public XWalkWebResourceResponse shouldInterceptLoadRequest(XWalkView webView, XWalkWebResourceRequest request) {
                Object[] result = gameActivity.interceptRequest(request.getUrl(), request.getMethod(), request.getRequestHeaders());
                if (result != null) {
                    return this.createXWalkWebResourceResponse((String) result[0], (String) result[1], (InputStream) result[5], (Integer) result[2], (String) result[3], (Map<String, String>) result[4]);
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

        this.onShow();
        this.resumeTimers();
    }

    public void pauseGame() {
        this.onHide();
        this.pauseTimers();
    }

    public void resumeGame() {
        // It may also be called once at startup without XWalkView being ready
        if (this.gameActivity.isXWalkReady()) {
            this.onShow();
            this.resumeTimers();
        }
    }

    public void reloadGame() {
        // TODO: check if normal reload is enough
        this.reload(XWalkView.RELOAD_IGNORE_CACHE);
    }

    public void fitGameLayout() {
        this.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('externalswf');if(!gf)return;const gs=gf.style,gw=1200,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.right='0';gs.zIndex='100';gs.transformOrigin='46.9% 0px 0px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k} kancolleFit()})(document,window)");
        this.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('game_frame');if(!gf)return;const gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';$.querySelector('.dmm-ntgnavi').style.display='none';$.querySelector('.area-naviapp').style.display='none';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.top='0px';gs.right='0';gs.zIndex='100';gs.transformOrigin='center top';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)");
    }

    private void detectGameStartAndFit(XWalkView view) {
        if (view.getUrl() != null && view.getUrl().contains(hostNameOoi)) {
            fitGameLayout();
        }
        if (view.getUrl() != null && view.getUrl().equals(GameConnection.DMM_START_URL)) {
            fitGameLayout();
        }
    }

    private void detectLoginAndFill(XWalkView view, SharedPreferences prefs) {
        if (view.getUrl() != null && (view.getUrl().startsWith("https://www.dmm.com/my/-/login/=") || view.getUrl().startsWith("https://accounts.dmm.com/service/login/password"))) {
            boolean isAutoUser = prefs.getBoolean("ooi_auto_user", false);
            if(isAutoUser){
                String userName = prefs.getString("dmm_user", "");
                String pwd = prefs.getString("dmm_pwd", "");
                view.loadUrl("javascript:$(\"#login_id\").val(\""+userName+"\");$(\"#password\").val(\""+pwd+"\");");
            }
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

        // For OOI
        if (view.getUrl() != null && view.getUrl().contains(hostNameOoi.substring(0, hostNameOoi.length() - 3))) {
            boolean isAutoUser = prefs.getBoolean("ooi_auto_user", false);
            if (isAutoUser) {
                String userName = prefs.getString("dmm_user", "");
                String pwd = prefs.getString("dmm_pwd", "");
                view.loadUrl("javascript:$(\"#login_id\").val(\"" + userName + "\");$(\"#password\").val(\"" + pwd + "\");");
                view.loadUrl("javascript:document.getElementById(\"mode3\").checked = true;");
            }
        }
    }

    private void detectAndHandleLoginError(XWalkView view, SharedPreferences prefs) {
        // For DMM only
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

    private void detectLoginExpireAndReload(String requestUrl, String respData){
        // For OOI only
        if(requestUrl.contains("api_req_member/get_incentive") && requestUrl.contains(hostNameOoi.substring(0, hostNameOoi.length() - 3))){
            try {
                JSONObject respDataJson = new JSONObject(respData.substring(7));
                if(respDataJson.has("api_result") && respDataJson.getInt("api_result") != 1){
                    gameActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadUrl(hostNameOoi.substring(0, hostNameOoi.length() - 3));
                        }
                    });
                    Toast.makeText(gameActivity, "登录过期，正在跳转到登录页面！", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private int changeCookieCnt = 0;

    private String hostNameOoi = null;

    private GameBaseActivity gameActivity = null;

    private ExecutorService pool = Executors.newFixedThreadPool(5);

    // HTML5 audio is bugged in Crosswalk
    // Add "edge" to UA so that Kancolle will fallback to traditional audio playing
    private final static String USER_AGENT = "Mozilla/5.0 (Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36 Edge/14.14931";
}
