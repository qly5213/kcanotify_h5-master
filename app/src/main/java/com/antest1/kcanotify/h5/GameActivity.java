package com.antest1.kcanotify.h5;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import andhook.lib.AndHook;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GameActivity extends AppCompatActivity {
    WebView mWebview;
    WebSettings mWebSettings;
    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36";
    private static final String[] SERVER_IP = new String[]{"203.104.209.71", "203.104.209.87", "125.6.184.215", "203.104.209.183", "203.104.209.150", "203.104.209.134", "203.104.209.167", "203.104.248.135", "125.6.189.7", "125.6.189.39", "125.6.189.71", "125.6.189.103", "125.6.189.135", "125.6.189.167", "125.6.189.215", "125.6.189.247", "203.104.209.23", "203.104.209.39", "203.104.209.55", "203.104.209.102"};

    private WebviewBroadcastReceiver webviewBroadcastReceiver = new WebviewBroadcastReceiver();

    private JSONObject jsonObj = null;
    private File cacheJsonFile = null;
    private OkHttpClient client = null;
    private SharedPreferences prefs = null;
    private boolean changeTouchEventPrefs = false;
    private int changeCookieCnt = 0;
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native int nativeInit(int version, boolean proxyEnable, String ip);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("pref", Context.MODE_PRIVATE);

        boolean proxyEnable = prefs.getBoolean("host_proxy_enable", true);
        String proxyIP = prefs.getString("host_proxy_address", "106.186.27.62");
        if(proxyEnable) {
            AndHook.ensureNativeLibraryLoaded(null);
            System.loadLibrary("xhook");
            System.loadLibrary("native-lib");
            Log.e("KCAV", "onCreate: " + "native hook result-->" + (nativeInit(Build.VERSION.SDK_INT, proxyEnable, proxyIP) == 0));
        }
        setContentView(R.layout.activity_game_webview);
        boolean hardSpeed = prefs.getBoolean("hardware_accelerated", true);
        if(hardSpeed) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
        boolean keepScreenOn = prefs.getBoolean("keep_screen_on", false);
        if(keepScreenOn){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        changeTouchEventPrefs = prefs.getBoolean("change_touch_event", true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        final View decorView = GameActivity.this.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
        });
        mWebview = findViewById(R.id.webView1);
        try {
            String cacheJsonPathStr = Environment.getExternalStorageDirectory() + "/KanCollCache/cache.json";
            String nomedia = Environment.getExternalStorageDirectory() + "/KanCollCache/.nomedia";
            cacheJsonFile = new File(cacheJsonPathStr);
            if (!cacheJsonFile.getParentFile().exists()) {
                cacheJsonFile.getParentFile().mkdirs();
            }
            if (!cacheJsonFile.exists()) {
                cacheJsonFile.createNewFile();
            }
            File nomediaFile = new File(nomedia);
            if(!nomediaFile.exists()){
                nomediaFile.createNewFile();
            }
            byte[] bytes = readFileToBytes(cacheJsonFile);
            String cacheJson = new String(bytes, "UTF-8");

            if (cacheJson != null && !cacheJson.equals("")) {
                jsonObj = new JSONObject(cacheJson);
            } else {
                jsonObj = new JSONObject();
            }
        } catch (Exception e){
            Log.e("KCVA", e.getMessage());
        }
        resetWebView(false);

        boolean clearCookie = prefs.getBoolean("clear_cookie_start", false);
        if(clearCookie){
            CookieManager.getInstance().removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    if(value){
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("clear_cookie_start", false);
                        editor.commit();
                    }
                }
            });
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(mWebview, true);
        boolean voicePlay = prefs.getBoolean("voice_play", false);
        boolean changeCookie = prefs.getBoolean("change_cookie_start", false);
        if(voicePlay){
            for (String serverIp : SERVER_IP) {
                cookieManager.setCookie(serverIp, "vol_bgm=50; domain=" + serverIp + "; path=/kcs2");
                cookieManager.setCookie(serverIp, "vol_se=50; domain=" + serverIp + "; path=/kcs2");
                cookieManager.setCookie(serverIp, "vol_voice=50; domain=" + serverIp + "; path=/kcs2");
            }
        } else {
            for (String serverIp : SERVER_IP) {
                cookieManager.setCookie(serverIp, "vol_bgm=0; domain=" + serverIp + "; path=/kcs2");
                cookieManager.setCookie(serverIp, "vol_se=0; domain=" + serverIp + "; path=/kcs2");
                cookieManager.setCookie(serverIp, "vol_voice=0; domain=" + serverIp + "; path=/kcs2");
            }
        }
        if(changeCookie){
            cookieManager.setCookie("www.dmm.com", "cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/");
            cookieManager.setCookie("www.dmm.com", "cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame/");
            cookieManager.setCookie("www.dmm.com", "cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame_s/");
            cookieManager.setCookie("www.dmm.com", "ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/");
            cookieManager.setCookie("www.dmm.com", "ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame/");
            cookieManager.setCookie("www.dmm.com", "ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame_s/");
        }
        cookieManager.flush();
        client = new OkHttpClient.Builder().build();
        mWebSettings = mWebview.getSettings();
        mWebSettings.setUserAgentString(USER_AGENT);
        mWebSettings.setBuiltInZoomControls(true);
        mWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
/*        Properties prop = System.getProperties();
        prop.setProperty("proxySet", "true");
        prop.setProperty("proxyHost", "218.241.131.227");
        prop.setProperty("proxyPort", "6100");*/
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
//                    view.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById(\'game_frame\'),gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector(\'meta[name=viewport]\'),t=0;vp||(vp=$.createElement(\'meta\'),vp.name=\'viewport\',$.querySelector(\'head\').appendChild(vp));vp.content=\'width=\'+gw;\'orientation\'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow=\'hidden\';$.body.style.cssText=\'min-width:0;padding:0;margin:0;overflow:hidden;margin:0\';$.querySelector(\'.dmm-ntgnavi\').style.display=\'none\';$.querySelector(\'.area-naviapp\').style.display=\'none\';$.getElementById(\'ntg-recommend\').style.display=\'none\';gs.position=\'fixed\';gs.marginRight=\'auto\';gs.marginLeft=\'auto\';gs.top=\'-16px\';gs.right=\'0\';gs.zIndex=\'100\';gs.transformOrigin=\'50%25%2016px\';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform=\'scale(\'+w/gw+\')\':gs.transform=\'scale(\'+h/gh+\')\';w<gw?gs.left=\'-\'+(gw-w)/2+\'px\':gs.left=\'0\'};_.addEventListener(\'resize\',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)");
                    view.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('game_frame'),gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';$.querySelector('.dmm-ntgnavi').style.display='none';$.querySelector('.area-naviapp').style.display='none';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.top='0px';gs.right='0';gs.zIndex='100';gs.transformOrigin='center top';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)");
                }
                if(view.getUrl() != null && view.getUrl().startsWith("https://www.dmm.com/my/-/login/=")){

                    new Handler().postDelayed(new Runnable(){
                        public void run() {
                            mWebview.evaluateJavascript("javascript:document.cookie = \"cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/\";", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = \"cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame/\";", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = \"cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame_s/\";", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });

                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=osapi.dmm.com;path=/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });

                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=203.104.209.7;path=/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=www.dmm.com;path=/netgame/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie =  'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=log-netgame.dmm.com;path=/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame_s/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                        }
                    }, 5000);


                }
                if(view.getUrl() != null && view.getUrl().equals("http://www.dmm.com/top/-/error/area/") && changeCookie && changeCookieCnt++ < 5) {
                    Log.i("KCVA", "Change Cookie");

                    new Handler().postDelayed(new Runnable(){
                        public void run() {
                            mWebview.evaluateJavascript("javascript:document.cookie = \"cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/\";", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = \"cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame/\";", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = \"cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame_s/\";", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });

                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=osapi.dmm.com;path=/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });

                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=203.104.209.7;path=/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=www.dmm.com;path=/netgame/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie =  'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=log-netgame.dmm.com;path=/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.evaluateJavascript("javascript:document.cookie = 'ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame_s/';", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {
                                    Log.i("KCVA", value);
                                }
                            });
                            mWebview.loadUrl("http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/");
                        }
                    }, 5000);
                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if(view.getUrl() != null && view.getUrl().equals("http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/")) {
//                    view.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById(\'game_frame\'),gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector(\'meta[name=viewport]\'),t=0;vp||(vp=$.createElement(\'meta\'),vp.name=\'viewport\',$.querySelector(\'head\').appendChild(vp));vp.content=\'width=\'+gw;\'orientation\'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow=\'hidden\';$.body.style.cssText=\'min-width:0;padding:0;margin:0;overflow:hidden;margin:0\';$.querySelector(\'.dmm-ntgnavi\').style.display=\'none\';$.querySelector(\'.area-naviapp\').style.display=\'none\';$.getElementById(\'ntg-recommend\').style.display=\'none\';gs.position=\'fixed\';gs.marginRight=\'auto\';gs.marginLeft=\'auto\';gs.top=\'-16px\';gs.right=\'0\';gs.zIndex=\'100\';gs.transformOrigin=\'50%25%2016px\';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform=\'scale(\'+w/gw+\')\':gs.transform=\'scale(\'+h/gh+\')\';w<gw?gs.left=\'-\'+(gw-w)/2+\'px\':gs.left=\'0\'};_.addEventListener(\'resize\',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)");
                    view.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('game_frame'),gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';$.querySelector('.dmm-ntgnavi').style.display='none';$.querySelector('.area-naviapp').style.display='none';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.top='0px';gs.right='0';gs.zIndex='100';gs.transformOrigin='center top';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)");
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String path = uri.getPath();
                Log.d("KCVA", "Request  uri拦截路径uri：：" + uri);
                if(path != null && path.contains("/kcsapi/api_port/port")){
                    changeTouchEvent = false;
                } else if(path != null && (path.contains("/kcsapi/api_get_member/mapinfo") || path.contains("/kcsapi/api_get_member/mission"))){
                    changeTouchEvent = true;
                }
                if (request.getMethod().equals("GET") && path != null && (path.startsWith("/kcs2/") || path.startsWith("/kcs/") || path.startsWith("/gadget_html5/js/kcs_inspection.js"))) {
                    if(path.contains("organize_main.png") || path.contains("supply_main.png") || path.contains("remodel_main.png") || path.contains("repair_main.png") || path.contains("arsenal_main.png")){
                        changeTouchEvent = true;
                    }
                    if(path.contains("version.json")){
                        return null;
                    }
                    try {
                        String version = "0";
                        if (uri.getQueryParameter("version") != null && !uri.getQueryParameter("version").equals("")) {
                            version = uri.getQueryParameter("version");
                        }

                        String currentVersion = null;
                        if (jsonObj.has(path)) {
                            currentVersion = jsonObj.getString(path);
                        }
                        String filePath = Environment.getExternalStorageDirectory() + "/KanCollCache" + path;
                        File tmp = new File(filePath);

                        //版本不一致则删除老的cache并更新版本信息
                        if (!version.equals(currentVersion)) {
                            tmp.delete();
                            synchronized (jsonObj) {
                                jsonObj.put(path, version);
                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cacheJsonFile, false), "UTF-8"));
                                writer.write(jsonObj.toString());
                                writer.close();
                            }
                            ResponseBody serverResponse = requestServer(request);
                            if(serverResponse != null){
                                byte[] respByte = null;
                                if(path.contains("/kcs2/js/main.js")){
                                    String newRespStr = serverResponse.string() + "!function(t){function r(i){if(n[i])return n[i].exports;var e=n[i]={exports:{},id:i,loaded:!1};return t[i].call(e.exports,e,e.exports,r),e.loaded=!0,e.exports}var n={};return r.m=t,r.c=n,r.p=\"\",r(0)}([function(t,r,n){n(1)(window)},function(t,r){t.exports=function(t){t.hookAjax=function(t){function r(r){return function(){var n=this.hasOwnProperty(r+\"_\")?this[r+\"_\"]:this.xhr[r],i=(t[r]||{}).getter;return i&&i(n,this)||n}}function n(r){return function(n){var i=this.xhr,e=this,o=t[r];if(\"function\"==typeof o)i[r]=function(){t[r](e)||n.apply(i,arguments)};else{var h=(o||{}).setter;n=h&&h(n,e)||n;try{i[r]=n}catch(t){this[r+\"_\"]=n}}}}function i(r){return function(){var n=[].slice.call(arguments);if(!t[r]||!t[r].call(this,n,this.xhr))return this.xhr[r].apply(this.xhr,n)}}return window._ahrealxhr=window._ahrealxhr||XMLHttpRequest,XMLHttpRequest=function(){this.xhr=new window._ahrealxhr;for(var t in this.xhr){var e=\"\";try{e=typeof this.xhr[t]}catch(t){}\"function\"===e?this[t]=i(t):Object.defineProperty(this,t,{get:r(t),set:n(t)})}},window._ahrealxhr},t.unHookAjax=function(){window._ahrealxhr&&(XMLHttpRequest=window._ahrealxhr),window._ahrealxhr=void 0},t.default=t}}]);hookAjax({onreadystatechange:function(xhr){var contentType=xhr.getResponseHeader(\"content-type\")||\"\";if(contentType.toLocaleLowerCase().indexOf(\"text/plain\")!==-1&&xhr.readyState==4&&xhr.status==200){console.log(xhr.xhr.responseURL);console.log(xhr.xhr.requestParam);console.log(xhr.responseText);window.androidJs.JsToJavaInterface(xhr.xhr.responseURL,xhr.xhr.requestParam,xhr.responseText);}},send:function(arg,xhr){xhr.requestParam=arg[0];}});";
                                    respByte = newRespStr.getBytes();
                                } else if(path.contains("/gadget_html5/js/kcs_inspection.js")){
                                    String newRespStr = serverResponse.string() + "window.onload=function(){document.body.style.background=\"#000\";document.getElementById(\"spacing_top\").style.height=\"0px\";};";
                                    respByte = newRespStr.getBytes();
                                } else {
                                    respByte = serverResponse.bytes();
                                }
                                saveFile(path, respByte);
                                return backToWebView(path, String.valueOf(respByte.length), new ByteArrayInputStream(respByte));
                            } else {
                                return null;
                            }
                        }
                        if (tmp.exists()) {

                            Log.d("KCVA", "Local cache uri：：" + uri);
                            //从缓存直接返回客户端，不请求服务器
                            byte[] fileContent = readFileToBytes(tmp);
                            if(path.contains("/gadget_html5/js/kcs_inspection.js")){
                                String newRespStr = new String(fileContent, "utf-8") + "window.onload=function(){document.body.style.background=\"#000\";document.getElementById(\"spacing_top\").style.height=\"0px\";};";
                                fileContent = newRespStr.getBytes("utf-8");
                            }
                            return backToWebView(path, String.valueOf(fileContent.length), new ByteArrayInputStream(fileContent));
                        } else {
                            ResponseBody serverResponse = requestServer(request);
                            if(serverResponse != null){
                                byte[] respByte = null;
                                if(path.contains("/kcs2/js/main.js")){
                                    String newRespStr = serverResponse.string() + "!function(t){function r(i){if(n[i])return n[i].exports;var e=n[i]={exports:{},id:i,loaded:!1};return t[i].call(e.exports,e,e.exports,r),e.loaded=!0,e.exports}var n={};return r.m=t,r.c=n,r.p=\"\",r(0)}([function(t,r,n){n(1)(window)},function(t,r){t.exports=function(t){t.hookAjax=function(t){function r(r){return function(){var n=this.hasOwnProperty(r+\"_\")?this[r+\"_\"]:this.xhr[r],i=(t[r]||{}).getter;return i&&i(n,this)||n}}function n(r){return function(n){var i=this.xhr,e=this,o=t[r];if(\"function\"==typeof o)i[r]=function(){t[r](e)||n.apply(i,arguments)};else{var h=(o||{}).setter;n=h&&h(n,e)||n;try{i[r]=n}catch(t){this[r+\"_\"]=n}}}}function i(r){return function(){var n=[].slice.call(arguments);if(!t[r]||!t[r].call(this,n,this.xhr))return this.xhr[r].apply(this.xhr,n)}}return window._ahrealxhr=window._ahrealxhr||XMLHttpRequest,XMLHttpRequest=function(){this.xhr=new window._ahrealxhr;for(var t in this.xhr){var e=\"\";try{e=typeof this.xhr[t]}catch(t){}\"function\"===e?this[t]=i(t):Object.defineProperty(this,t,{get:r(t),set:n(t)})}},window._ahrealxhr},t.unHookAjax=function(){window._ahrealxhr&&(XMLHttpRequest=window._ahrealxhr),window._ahrealxhr=void 0},t.default=t}}]);hookAjax({onreadystatechange:function(xhr){var contentType=xhr.getResponseHeader(\"content-type\")||\"\";if(contentType.toLocaleLowerCase().indexOf(\"text/plain\")!==-1&&xhr.readyState==4&&xhr.status==200){window.androidJs.JsToJavaInterface(xhr.xhr.responseURL,xhr.xhr.requestParam,xhr.responseText);}},send:function(arg,xhr){xhr.requestParam=arg[0];}});";
                                    respByte = newRespStr.getBytes();
                                } else if(path.contains("/gadget_html5/js/kcs_inspection.js")){
                                    String newRespStr = serverResponse.string() + "window.onload=function(){document.body.style.background=\"#000\";document.getElementById(\"spacing_top\").style.height=\"0px\";};";
                                    respByte = newRespStr.getBytes();
                                }  else {
                                    respByte = serverResponse.bytes();
                                }
                                saveFile(path, respByte);
                                return backToWebView(path, String.valueOf(respByte.length), new ByteArrayInputStream(respByte));
                            } else {
                                return null;
                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();

                        return null;//异常情况，直接访问网络资源
                    }
                }
                return null;
            }
        });

        mWebview.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void JsToJavaInterface(String requestUrl, String param, String respData) {
                try {
                    URL url = new URL(requestUrl);
                    KcaVpnData.renderToHander(url.getPath(), param, respData);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        },"androidJs");

        mWebview.loadUrl("http://www.dmm.com/netgame/social/-/gadgets/=/app_id=854854/");

        registerReceiver(webviewBroadcastReceiver, new IntentFilter("com.antest1.kcanotify.h5.webview_reload"));
    }

    private WebResourceResponse backToWebView(String path, String size, InputStream is){
        String mimeType = null;
        if (path.endsWith("mp3")) {
            mimeType = "audio/mpeg";
        } else if (path.endsWith("png")) {
            mimeType = "image/png";
        } else if (path.endsWith("json")) {
            mimeType = "application/json";
        } else if (path.endsWith("js")) {
            mimeType = "application/javascript";
        }
        Map<String, String> map = new HashMap<>();
        map.put("Connection", "keep-alive");
        map.put("Server", "KanCollCache");
        map.put("Content-Length", size);
        map.put("Content-Type", mimeType);
        map.put("Cache-Control", "public");
        return new WebResourceResponse(mimeType, null, 200, "OK", map, is);
    }

    private ResponseBody requestServer(WebResourceRequest request){
        Request.Builder builder = new Request.Builder().url(request.getUrl().toString());
        Map<String, String> headerHeader = request.getRequestHeaders();
        for(Map.Entry<String, String> keySet : headerHeader.entrySet()){
            builder.addHeader(keySet.getKey(), keySet.getValue());
        }
        Request serverRequest = builder.build();

        Response response = null;
        try {
            response = client.newCall(serverRequest).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(response != null && response.isSuccessful()){
            return response.body();
        } else {
            return null;
        }
    }

    private void saveFile(String path, byte[] fileContent){
        Log.e("KCA", "Save LocalRes:" + path);
        File file = new File(Environment.getExternalStorageDirectory(),"/KanCollCache" + path);
        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if(!file.exists()) {
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file);
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write(fileContent);
            bufferedOutputStream.flush();
        } catch(Exception e){
            Log.e("KCA", e.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }


    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
        resetWebView(true);
        mWebview.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById('game_frame'),gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';$.querySelector('.dmm-ntgnavi').style.display='none';$.querySelector('.area-naviapp').style.display='none';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.top='0px';gs.right='0';gs.zIndex='100';gs.transformOrigin='center top';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)");

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void resetWebView(boolean changeMutiWindow){
        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;
        int height = outMetrics.heightPixels;
        if(changeMutiWindow){
            if(width < height) {
                int tmp = width;
                width = height;
                height = tmp;
            } else {
                int tmp = width;
                width = height;
                height = tmp / 2;
            }
        }
        double weightScale = width / 1200.0;
        double heightScale = height / 720.0;
        if(weightScale < heightScale){
            int reScaleHeight = (int)(720 * weightScale);
            ViewGroup.LayoutParams params = mWebview.getLayoutParams();
            params.width = width;
            params.height = reScaleHeight;
            mWebview.setLayoutParams(params);
        } else {
            int reScaleWeight = (int)(1200 * heightScale);
            ViewGroup.LayoutParams params = mWebview.getLayoutParams();
            params.width = reScaleWeight;
            params.height = height;
            mWebview.setLayoutParams(params);
        }
    }
    boolean changeTouchEvent = false;
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d("touchEvent", event.getToolType(0) + ":" + event.getActionMasked());
        if(event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER && changeTouchEventPrefs) {
            if(event.getAction() == MotionEvent.ACTION_MOVE) {
                buildMoveEvent(event);
            } else if(event.getAction() == MotionEvent.ACTION_DOWN && changeTouchEvent){
                buildMoveEvent(event);
            } else if(event.getAction() == MotionEvent.ACTION_UP && changeTouchEvent){
                buildMoveEvent(event);
            }
        }
        return super.dispatchTouchEvent(event);
    }


    private void buildMoveEvent(MotionEvent event){
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[]{new MotionEvent.PointerProperties()};
        event.getPointerProperties(0, pointerProperties[0]);
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[]{new MotionEvent.PointerCoords()};
        event.getPointerCoords(0, pointerCoords[0]);
        pointerProperties[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;
        pointerCoords[0].x -= mWebview.getX();
        pointerCoords[0].y -= mWebview.getY();
        long touchTime = SystemClock.uptimeMillis();
        this.mWebview.onTouchEvent(MotionEvent.obtain(touchTime, touchTime, MotionEvent.ACTION_MOVE, 1, pointerProperties, pointerCoords, 0, 0, 0f, 0f, InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC, 0, InputDevice.SOURCE_TOUCHSCREEN, 0));

    }


    @Override
    protected void onStop() {
        if (!prefs.getBoolean("background_play", true)){
            mWebview.onPause();
            mWebview.pauseTimers();
        }
        super.onStop();
    }
    @Override
    protected void onStart() {
        if (!prefs.getBoolean("background_play", true)) {
            mWebview.onResume();
            mWebview.resumeTimers();
        }
        super.onStart();
    }

    //点击返回上一页面而不是退出浏览器
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    //销毁Webview
    @Override
    protected void onDestroy() {
        if (mWebview != null) {
            mWebview.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mWebview.clearHistory();

            ((ViewGroup) mWebview.getParent()).removeView(mWebview);
            mWebview.destroy();
            mWebview = null;
        }
        unregisterReceiver(webviewBroadcastReceiver);
        super.onDestroy();
    }

    private class WebviewBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.antest1.kcanotify.h5.webview_reload"))
                mWebview.reload();
        }
    }

    private byte[] readFileToBytes(File file){
        byte[] bytes = new byte[0];
        if(!file.exists()){
            return bytes;
        }
        BufferedInputStream buf = null;
        try {
            int fileSize = (int) file.length();
            bytes = new byte[fileSize];
            buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, fileSize);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(buf != null){
                    buf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }

}
