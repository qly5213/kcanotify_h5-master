package com.antest1.kcanotify.h5;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import org.xwalk.core.JavascriptInterface;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bilibili.boxing.Boxing;
import com.bilibili.boxing.BoxingMediaLoader;
import com.bilibili.boxing.model.config.BoxingConfig;
import com.bilibili.boxing.model.entity.BaseMedia;

import org.json.JSONObject;
import org.xwalk.core.XWalkCookieManager;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkActivity;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;

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
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import customview.ConfirmDialog;
import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GameOOIActivity extends XWalkActivity {
    XWalkView mWebview;
    XWalkSettings mWebSettings;
    private ProgressBar progressBar1;
    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36";

    private WebviewBroadcastReceiver webviewBroadcastReceiver = new WebviewBroadcastReceiver();

    private JSONObject jsonObj = null;
    private File cacheJsonFile = null;
    private OkHttpClient client = null;
    private SharedPreferences prefs = null;
    private boolean changeTouchEventPrefs = false;
    String hostName = null;
    private GameOOIActivity.RotationObserver mRotationObserver;
    private TextView subtitleTextview;
    private StrokeTextView subtitleStrokeTextview;
    private ImageView chatImageView;
    private ImageView chatNewMsgImageView;
    private String nickName;
    private HashMap<String, String> serverMap;

    private boolean showDanmaku;

    private DanmakuView danmakuView;

    private DanmakuContext danmakuContext;
    private BaseDanmakuParser parser = new BaseDanmakuParser() {
        @Override
        protected IDanmakus parse() {
            return new Danmakus();
        }
    };
    private ChatDialogUtils dialogUtils;
    private boolean chatDanmuku;
    private String imageSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KcaApplication.gameOOIActivity = this;
        mRotationObserver = new GameOOIActivity.RotationObserver(new Handler());
        prefs = getSharedPreferences("pref", Context.MODE_PRIVATE);
        boolean subTitleEnable = prefs.getBoolean("voice_sub_title", false);

        if(subTitleEnable) {
            //语言字幕初始化
            SubTitleUtils.initVoiceMap();
            SubTitleUtils.initSubTitle();
        }
        boolean chatService = prefs.getBoolean("chat_service", false);
        chatDanmuku = prefs.getBoolean("chat_danmuku", false);
        if(chatService){
            serverMap = SubTitleUtils.initServiceHost();
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
        final View decorView = GameOOIActivity.this.getWindow().getDecorView();
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
        progressBar1 = (ProgressBar) findViewById(R.id.progressBar1);
        subtitleTextview = findViewById(R.id.subtitle_textview);
        subtitleStrokeTextview = findViewById(R.id.subtitle_textview_stroke);
        chatImageView = findViewById(R.id.chat_image_view);
        chatNewMsgImageView = findViewById(R.id.chat_new_msg_image_view);
        chatImageView.setImageAlpha(50);

        imageSize = getIntent().getStringExtra("imageSize");
        if(imageSize == null){
            imageSize = "100";
        }

        //弹幕
        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 10); // 滚动弹幕最大显示10行
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);

        danmakuContext = DanmakuContext.create();
        danmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 5).setDuplicateMergingEnabled(false)
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair).setDanmakuMargin(20);
        danmakuView = findViewById(R.id.danmaku_view);
        danmakuView.enableDanmakuDrawingCache(true);
        danmakuView.setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                showDanmaku = true;
                danmakuView.start();
            }

            @Override
            public void updateTimer(DanmakuTimer timer) {

            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {

            }

            @Override
            public void drawingFinished() {

            }
        });
        danmakuView.prepare(parser, danmakuContext);
        BoxingMediaLoader.getInstance().init(new BoxingPicassoLoader());
        BoxingConfig singleImgConfig = new BoxingConfig(BoxingConfig.Mode.SINGLE_IMG).withMediaPlaceHolderRes(R.drawable.ic_boxing_default_image);

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


        client = new OkHttpClient.Builder().build();
        //设置WebChromeClient类
        mWebview.setUIClient(new XWalkUIClient(mWebview) {


            //获取网站标题
            @Override
            public void onReceivedTitle(XWalkView view, String title) {
            }
        });

        //设置WebViewClient类
        mWebview.setResourceClient(new XWalkResourceClient(mWebview) {
            //获取加载进度
            @Override
            public void onProgressChanged(XWalkView view, int newProgress) {
                if (newProgress == 100) {
                    progressBar1.setVisibility(View.GONE);
                } else {
                    progressBar1.setVisibility(View.VISIBLE);
                    progressBar1.setProgress(newProgress);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(XWalkView view, String url) {
                view.loadUrl(url);
                return true;
            }
            //设置加载前的函数
            @Override
            public void onLoadStarted(XWalkView view, String url) {
                super.onLoadStarted(view, url);
            }

            //设置结束加载函数
            @Override
            public void onLoadFinished(XWalkView view, String url) {
                if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/poi")) {
                    view.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById(\"externalswf\"),gs=gf.style,gw=1200,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.right='0';gs.zIndex='100';gs.transformOrigin='46.9% 0px 0px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k} kancolleFit()})(document,window)");
                } else if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/")){
                    boolean isAutoUser = prefs.getBoolean("ooi_auto_user", false);
                    if(isAutoUser){
                        String userName = prefs.getString("dmm_user", "");
                        String pwd = prefs.getString("dmm_pwd", "");
                        mWebview.loadUrl("javascript:$(\"#login_id\").val(\""+userName+"\");$(\"#password\").val(\""+pwd+"\");$(\"#mode\").val(\"3\");");
                    }
                }
            }

//            @Override
//            public void onLoadResource(WebView view, String url) {
//                super.onLoadResource(view, url);
//                if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/poi")) {
//                    view.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById(\"externalswf\"),gs=gf.style,gw=1200,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.right='0';gs.zIndex='100';gs.transformOrigin='46.9% 0px 0px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k} kancolleFit()})(document,window)");
//                } else if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/")){
//                    boolean isAutoUser = prefs.getBoolean("ooi_auto_user", false);
//                    if(isAutoUser){
//                        String userName = prefs.getString("dmm_user", "");
//                        String pwd = prefs.getString("dmm_pwd", "");
//                        mWebview.loadUrl("javascript:$(\"#login_id\").val(\""+userName+"\");$(\"#password\").val(\""+pwd+"\");$(\"#mode\").val(\"3\");");
//                    }
//                }
//            }

//            @Override
//            public void onReceivedLoginRequest(WebView view, String realm, @Nullable String account, String args) {
//                super.onReceivedLoginRequest(view, realm, account, args);
//            }
            Handler handler = new Handler();
            Runnable dismissSubTitle = new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            subtitleTextview.setText("");
                            subtitleStrokeTextview.setText("");
                        }
                    });
                }
            };
            @Override
            public XWalkWebResourceResponse shouldInterceptLoadRequest(XWalkView webView, XWalkWebResourceRequest request) {
                Uri uri = request.getUrl();
                String path = uri.getPath();
                Log.d("KCVA", "Request  uri拦截路径uri：：" + uri);
                if(path != null && path.contains("/kcsapi/api_port/port")){
                    changeTouchEvent = false;
                } else if(path != null && (path.contains("/kcsapi/api_get_member/mapinfo") || path.contains("/kcsapi/api_get_member/mission"))){
                    changeTouchEvent = true;
                }
                if (request.getMethod().equals("GET") && path != null && (path.startsWith("/kcs2/") || path.startsWith("/kcs/"))) {
                    if(path.contains("organize_main.png") || path.contains("supply_main.png") || path.contains("remodel_main.png") || path.contains("repair_main.png") || path.contains("arsenal_main.png")){
                        changeTouchEvent = true;
                    }
                    if(path.contains("version.json")){
                        return null;
                    }
                    try {
                        //获取字幕
                        String pattern = "/kcs/sound/(.*?)/(.*?).mp3";
                        boolean isMatch = Pattern.matches(pattern, path);
                        if(isMatch && subTitleEnable){
                            String subTitle = SubTitleUtils.getSubTitle(path);
//                            Log.d("KCVA", "语音字幕：" + subTitle);
                            handler.removeCallbacks(dismissSubTitle);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    subtitleTextview.setText(subTitle);
                                    subtitleStrokeTextview.setText(subTitle);
                                }
                            });
                            handler.postDelayed(dismissSubTitle, 15000);
                        }
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
                                } else {
                                    respByte = serverResponse.bytes();
                                }
                                saveFile(path, respByte);
                                return backToWebView(path, String.valueOf(respByte.length), new ByteArrayInputStream(respByte), this);
                            } else {
                                return null;
                            }
                        }
                        if (tmp.exists()) {

                            Log.d("KCVA", "Local cache uri：：" + uri);
                            //从缓存直接返回客户端，不请求服务器
                            byte[] fileContent = readFileToBytes(tmp);
                            return backToWebView(path, String.valueOf(fileContent.length), new FileInputStream(tmp), this);
                        } else {
                            ResponseBody serverResponse = requestServer(request);
                            if(serverResponse != null){
                                byte[] respByte = null;
                                if(path.contains("/kcs2/js/main.js")){
                                    String newRespStr = serverResponse.string() + "!function(t){function r(i){if(n[i])return n[i].exports;var e=n[i]={exports:{},id:i,loaded:!1};return t[i].call(e.exports,e,e.exports,r),e.loaded=!0,e.exports}var n={};return r.m=t,r.c=n,r.p=\"\",r(0)}([function(t,r,n){n(1)(window)},function(t,r){t.exports=function(t){t.hookAjax=function(t){function r(r){return function(){var n=this.hasOwnProperty(r+\"_\")?this[r+\"_\"]:this.xhr[r],i=(t[r]||{}).getter;return i&&i(n,this)||n}}function n(r){return function(n){var i=this.xhr,e=this,o=t[r];if(\"function\"==typeof o)i[r]=function(){t[r](e)||n.apply(i,arguments)};else{var h=(o||{}).setter;n=h&&h(n,e)||n;try{i[r]=n}catch(t){this[r+\"_\"]=n}}}}function i(r){return function(){var n=[].slice.call(arguments);if(!t[r]||!t[r].call(this,n,this.xhr))return this.xhr[r].apply(this.xhr,n)}}return window._ahrealxhr=window._ahrealxhr||XMLHttpRequest,XMLHttpRequest=function(){this.xhr=new window._ahrealxhr;for(var t in this.xhr){var e=\"\";try{e=typeof this.xhr[t]}catch(t){}\"function\"===e?this[t]=i(t):Object.defineProperty(this,t,{get:r(t),set:n(t)})}},window._ahrealxhr},t.unHookAjax=function(){window._ahrealxhr&&(XMLHttpRequest=window._ahrealxhr),window._ahrealxhr=void 0},t.default=t}}]);hookAjax({onreadystatechange:function(xhr){var contentType=xhr.getResponseHeader(\"content-type\")||\"\";if(contentType.toLocaleLowerCase().indexOf(\"text/plain\")!==-1&&xhr.readyState==4&&xhr.status==200){console.log(xhr.xhr.responseURL);console.log(xhr.xhr.requestParam);console.log(xhr.responseText);window.androidJs.JsToJavaInterface(xhr.xhr.responseURL,xhr.xhr.requestParam,xhr.responseText);}},send:function(arg,xhr){xhr.requestParam=arg[0];}});";
                                    respByte = newRespStr.getBytes();
                                } else {
                                    respByte = serverResponse.bytes();
                                }
                                saveFile(path, respByte);
                                return backToWebView(path, String.valueOf(respByte.length), new ByteArrayInputStream(respByte), this);
                            } else {
                                return null;
                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();

                        return null;//异常情况，直接访问网络资源
                    }
                } else {
                    // Not KC api
                    return null;
                }
            }
        });

        mWebview.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void JsToJavaInterface(String requestUrl, String param, String respData) {
                try {
                    URL url = new URL(requestUrl);
                    if(requestUrl.contains("api_req_member/get_incentive")){
                        try {
                            JSONObject respDataJson = new JSONObject(respData.substring(7));
                            if(respDataJson.has("api_result") && respDataJson.getInt("api_result") != 1){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mWebview.loadUrl("http://" + hostName + "/");
                                    }
                                });
                                Toast.makeText(GameOOIActivity.this, "登录过期，正在跳转到登录页面！", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    KcaVpnData.renderToHander(url.getPath(), param, respData);
                    if(url.getPath().contains("/kcsapi/api_start2/getData") && subTitleEnable){
                        SubTitleUtils.initShipGraph(respData);
                    }
                    if(url.getPath().contains("/kcsapi/api_port/port") && chatService){
                        String host = url.getHost();
                        String serName = serverMap.get(host);
                        nickName = new JSONObject(respData.substring(7)).getJSONObject("api_data").getJSONObject("api_basic").getString("api_nickname") + (serName != null ? " - " + serName : "");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },"androidJs");


        if(chatService) {
            dialogUtils = new ChatDialogUtils(this, new ChatListener() {
                @Override
                public void onMessage(ChatMsgObject msgObject) {
                    if(msgObject.getMsgType() == ChatMsgObject.MsgTypeCont.MSG_TEXT) {
                        if(chatDanmuku) {
                            addDanmaku(msgObject.getMsg(), false);
                        }
                    }
                    if(!dialogUtils.isShow()) {
                        chatNewMsgImageView.setVisibility(View.VISIBLE);
                        chatNewMsgImageView.setImageAlpha(255);
                        chatImageView.setImageAlpha(255);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        chatImageView.setImageAlpha(50);
                                        chatNewMsgImageView.setImageAlpha(50);
                                    }
                                });
                            }
                        }, 5000);
                    }
                }
                @Override
                public void onSelectMsg(){
                    Boxing.of(singleImgConfig).withIntent(GameOOIActivity.this, LandScapeBoxingActivity.class).start(GameOOIActivity.this, 2);
                }
            }, imageSize);
            chatImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    chatImageView.setImageAlpha(255);
                    chatNewMsgImageView.setVisibility(View.GONE);
                    dialogUtils.showLeftChat(nickName);
                    /*new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    chatImageView.setImageAlpha(50);
                                }
                            });
                        }
                    }, 5000);*/
                }
            });
        } else {
            chatImageView.setVisibility(View.GONE);
        }
        registerReceiver(webviewBroadcastReceiver, new IntentFilter("com.antest1.kcanotify.h5.webview_reload"));
    }

    @Override
    protected void onXWalkReady() {
        boolean clearCookie = prefs.getBoolean("clear_cookie_start", false);
        if(clearCookie){
            (new XWalkCookieManager()).removeAllCookie();
        }

        hostName = prefs.getString("ooi_host_name", "ooi.moe");
        if(hostName.equals("")) hostName = "ooi.moe";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress address = InetAddress.getByName(hostName);
                    KcaConstants.hostAddressIp = address.getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        XWalkCookieManager cookieManager = new XWalkCookieManager();
        cookieManager.setAcceptCookie(true);
        boolean voicePlay = prefs.getBoolean("voice_play", false);
        String fullHostName = hostName;
        if (!fullHostName.startsWith("http")) {
            fullHostName = "https://" + fullHostName;
        }
        if(voicePlay) {
            cookieManager.setCookie(fullHostName, "vol_bgm=50; domain=" + hostName + "; path=/kcs2");
            cookieManager.setCookie(fullHostName, "vol_se=50; domain=" + hostName + "; path=/kcs2");
            cookieManager.setCookie(fullHostName, "vol_voice=50; domain=" + hostName + "; path=/kcs2");
        } else {
            cookieManager.setCookie(fullHostName, "vol_bgm=0; domain=" + hostName + "; path=/kcs2");
            cookieManager.setCookie(fullHostName, "vol_se=0; domain=" + hostName + "; path=/kcs2");
            cookieManager.setCookie(fullHostName, "vol_voice=0; domain=" + hostName + "; path=/kcs2");
        }
        cookieManager.flushCookieStore();

        mWebSettings = mWebview.getSettings();
        mWebSettings.setUserAgentString(USER_AGENT);
        mWebSettings.setBuiltInZoomControls(true);
        mWebSettings.setCacheMode(XWalkSettings.LOAD_DEFAULT);
/*        Properties prop = System.getProperties();
        prop.setProperty("proxySet", "true");
        prop.setProperty("proxyHost", "218.241.131.227");
        prop.setProperty("proxyPort", "6100");*/
        // 设置与Js交互的权限
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setMediaPlaybackRequiresUserGesture(false);

//        WebView.setWebContentsDebuggingEnabled(true);

        mWebview.loadUrl("http://" + hostName + "/poi");

        mWebview.resumeTimers();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2) {
            if(data != null){
                List<BaseMedia> medias = Boxing.getResult(data);
                if(medias.size() > 0) {
                    ConfirmDialog confirmDialog =  new ConfirmDialog(this, new ConfirmDialog.Callback() {
                        @Override
                        public void callback(int position) {
                            if(position == 1) {
                                dialogUtils.sendImage(medias.get(0).getPath());
                            }
                        }
                    });
                    confirmDialog.setContent("请确认是否发送该图片？");
                    confirmDialog.show();
                }
            }
        }
        super.onActivityReenter(resultCode, data);
    }
    private XWalkWebResourceResponse backToWebView(String path, String size, InputStream is, XWalkResourceClient client){
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
        return client.createXWalkWebResourceResponse(mimeType, null, is, 200, "OK", map);
    }

    private ResponseBody requestServer(XWalkWebResourceRequest request){
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
        if(isInMultiWindowMode) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) chatImageView.getLayoutParams();
            lp.setMargins(0, dip2px(20), 0, 0);
            chatImageView.setLayoutParams(lp);
            RelativeLayout.LayoutParams lp1 = (RelativeLayout.LayoutParams) chatNewMsgImageView.getLayoutParams();
            lp1.setMargins(dip2px(10), dip2px(15), 0, 0);
            chatNewMsgImageView.setLayoutParams(lp1);
        } else {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) chatImageView.getLayoutParams();
            lp.setMargins(0, 0, 0, 0);
            chatImageView.setLayoutParams(lp);
            RelativeLayout.LayoutParams lp1 = (RelativeLayout.LayoutParams) chatNewMsgImageView.getLayoutParams();
            lp1.setMargins(dip2px(20), dip2px(-5), 0, 0);
            chatNewMsgImageView.setLayoutParams(lp1);
        }
        resetWebView(true);
        mWebview.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById(\"externalswf\"),gs=gf.style,gw=1200,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.right='0';gs.zIndex='100';gs.transformOrigin='46.9% 0px 0px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k} kancolleFit()})(document,window)");

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
    protected void onResume() {
        mRotationObserver.startObserver();
        setScreenOrientation();
        if (danmakuView != null && danmakuView.isPrepared() && danmakuView.isPaused()) {
            danmakuView.resume();
        }
        super.onResume();
    }
    @Override
    protected void onPause() {
        mRotationObserver.stopObserver();
        if (danmakuView != null && danmakuView.isPrepared()) {
            danmakuView.pause();
        }
        super.onPause();
    }
    @Override
    protected void onStop() {
        if (!prefs.getBoolean("background_play", true)){
//            mWebview.onPause();
            mWebview.pauseTimers();
        }
        super.onStop();
    }
    @Override
    protected void onStart() {
        if (!prefs.getBoolean("background_play", true)) {
//            mWebview.onResume();
            try{
                mWebview.resumeTimers();
            } catch(java.lang.RuntimeException e) {
                if ( e.getMessage().compareTo("Crosswalk's APIs are not ready yet") == 0 ) {
                } else {
                    throw e;
                }
            }
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
//            mWebview.clearHistory();

            ((ViewGroup) mWebview.getParent()).removeView(mWebview);
            mWebview.onDestroy();
            mWebview = null;
        }
        unregisterReceiver(webviewBroadcastReceiver);
        showDanmaku = false;
        if (danmakuView != null) {
            danmakuView.release();
            danmakuView = null;
        }
        super.onDestroy();
    }

    private class WebviewBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.antest1.kcanotify.h5.webview_reload"))
                mWebview.reload(XWalkView.RELOAD_IGNORE_CACHE);
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

    private void setScreenOrientation() {
        try {
            int screenchange = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
            //是否开启自动旋转设置 1 开启 0 关闭
            if (screenchange == 1){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }
    //观察屏幕旋转设置变化，类似于注册动态广播监听变化机制
    private class RotationObserver extends ContentObserver {
        ContentResolver mResolver;

        public RotationObserver(Handler handler) {
            super(handler);
            mResolver = getContentResolver();
            // TODO Auto-generated constructor stub
        }

        //屏幕旋转设置改变时调用
        @Override
        public void onChange(boolean selfChange) {
            // TODO Auto-generated method stub
            super.onChange(selfChange);
            //更新按钮状态
            setScreenOrientation();
        }

        public void startObserver() {
            mResolver.registerContentObserver(Settings.System
                            .getUriFor(Settings.System.ACCELEROMETER_ROTATION), false,
                    this);
        }

        public void stopObserver() {
            mResolver.unregisterContentObserver(this);
        }
    }
    private void addDanmaku(String content, boolean withBorder) {
        BaseDanmaku danmaku = danmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if(content.length() > 7) {
            danmaku = danmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_FIX_TOP);
        }
        danmaku.text = content;
        danmaku.padding = 5;
        danmaku.textSize = sp2px(14);
        danmaku.textColor = Color.WHITE;
        danmaku.textShadowColor = Color.BLACK;
        danmaku.setTime(danmakuView.getCurrentTime());
        if (withBorder) {
            danmaku.borderColor = Color.GREEN;
        }
        danmakuView.addDanmaku(danmaku);
    }
    public int sp2px(float spValue) {
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }
    public int dip2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
