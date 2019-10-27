package com.antest1.kcanotify.h5;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bilibili.boxing.Boxing;
import com.bilibili.boxing.BoxingMediaLoader;
import com.bilibili.boxing.model.config.BoxingConfig;
import com.bilibili.boxing.model.entity.BaseMedia;

import org.json.JSONObject;
import org.xwalk.core.XWalkActivity;

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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import andhook.lib.AndHook;
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

public abstract class GameBaseActivity extends XWalkActivity {

    private static final String[] SERVER_IP = new String[]{"203.104.209.71", "203.104.209.87", "125.6.184.215", "203.104.209.183", "203.104.209.150", "203.104.209.134", "203.104.209.167", "203.104.248.135", "125.6.189.7", "125.6.189.39", "125.6.189.71", "125.6.189.103", "125.6.189.135", "125.6.189.167", "125.6.189.215", "125.6.189.247", "203.104.209.23", "203.104.209.39", "203.104.209.55", "203.104.209.102"};


    ExecutorService pool = Executors.newFixedThreadPool(5);
    private GameBaseActivity.WebviewBroadcastReceiver webviewBroadcastReceiver = new GameBaseActivity.WebviewBroadcastReceiver();
    private GameBaseActivity.RotationObserver mRotationObserver;
    protected SharedPreferences prefs = null;
    private boolean chatDanmuku;
    private HashMap<String, String> serverMap;
    private boolean battleResultVibrate;
    private boolean changeTouchEventPrefs = false;

    protected View mWebview;
    protected ProgressBar progressBar1;
    private TextView fpsCounter;
    private TextView subtitleTextview;
    private StrokeTextView subtitleStrokeTextview;
    private ImageView chatImageView;
    private ImageView chatNewMsgImageView;

    private String imageSize;
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

    private OkHttpClient client = null;


    private JSONObject jsonObj = null;
    private File cacheJsonFile = null;

    protected boolean clearCookie;
    protected boolean voicePlay;
    protected boolean changeCookie;
    protected HashMap<String, String> voiceCookieMap;
    protected HashMap<String, String> dmmCokieMap;

    boolean changeTouchEvent = false;
    private boolean subTitleEnable;
    private Handler subtitleHandler;
    private Runnable dismissSubTitle;
    private boolean chatService;
    private String nickName;
    private boolean changeWebview;

    public native String stringFromJNI();

    public native int nativeInit(int version, boolean proxyEnable, String ip);

    @Override
    protected void onXWalkReady() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // For some (mainly Samsung) devices, opening app in pop up window mode will re-create a second instant
        // but the system is still keeping/using resources of the old instant for a while
        // In this case, we do not finish the old view
        if(KcaApplication.gameActivity != null && KcaApplication.gameActivity.getClass() != this.getClass()){
            // Remove last game activity only after changing important settings (DMM/OOI, Crosswalk/WebView)
            KcaApplication.gameActivity.finish();
        }
        KcaApplication.gameActivity = this;
        mRotationObserver = new GameBaseActivity.RotationObserver(new Handler());
        prefs = getSharedPreferences("pref", Context.MODE_PRIVATE);

        subTitleEnable = prefs.getBoolean("voice_sub_title", false);
        if(subTitleEnable) {
            //语言字幕初始化
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SubTitleUtils.initVoiceMap();
                    SubTitleUtils.initSubTitle();
                }
            }).start();
        }

        chatService = prefs.getBoolean("chat_service", false);
        chatDanmuku = prefs.getBoolean("chat_danmuku", false);
        if(chatService){
            serverMap = SubTitleUtils.initServiceHost();
        }

        battleResultVibrate = prefs.getBoolean("battle_result_vibrate", true);

        //proxy init
        boolean proxyEnable = prefs.getBoolean("host_proxy_enable", false);
        String proxyIP = prefs.getString("host_proxy_address", "167.179.91.86");
        if(proxyEnable) {
            AndHook.ensureNativeLibraryLoaded(null);
            System.loadLibrary("xhook");
            System.loadLibrary("native-lib");
            Log.e("KCAV", "onCreate: " + "native hook result-->" + (nativeInit(Build.VERSION.SDK_INT, proxyEnable, proxyIP) == 0));
        }

        changeWebview = prefs.getBoolean("change_webview", false);
        if(changeWebview) {
            setContentView(R.layout.activity_game_webview);
        } else {
            setContentView(R.layout.activity_game_webview_ori);
        }

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
        final View decorView = GameBaseActivity.this.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
        });

        imageSize = getIntent().getStringExtra("imageSize");
        if(imageSize == null){
            imageSize = "100";
        }

        mWebview = findViewById(R.id.webView1);
        progressBar1 = (ProgressBar) findViewById(R.id.progressBar1);
        fpsCounter = (TextView) findViewById(R.id.fps_counter);
        subtitleTextview = findViewById(R.id.subtitle_textview);
        subtitleStrokeTextview = findViewById(R.id.subtitle_textview_stroke);
        chatImageView = findViewById(R.id.chat_image_view);
        chatNewMsgImageView = findViewById(R.id.chat_new_msg_image_view);
        chatImageView.setImageAlpha(50);

        initDanMuKu();
        initCache();

        resetWebView(false);

        client = new OkHttpClient.Builder().build();

        clearCookie = prefs.getBoolean("clear_cookie_start", false);
        voicePlay = prefs.getBoolean("voice_play", false);
        changeCookie = prefs.getBoolean("change_cookie_start", false);
        initCookieData();


        subtitleHandler = new Handler();
        dismissSubTitle = new Runnable() {
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

        initChat();

        registerReceiver(webviewBroadcastReceiver, new IntentFilter("com.antest1.kcanotify.h5.webview_reload"));
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
            webviewPause();
        }
        super.onStop();
    }
    abstract public void webviewPause();


    @Override
    protected void onStart() {
        if (!prefs.getBoolean("background_play", true)) {
            webviewResume();
        }
        super.onStart();
    }
    abstract public void webviewResume();

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
            webviewDestory();
        }
        unregisterReceiver(webviewBroadcastReceiver);
        showDanmaku = false;
        if (danmakuView != null) {
            danmakuView.release();
            danmakuView = null;
        }
        super.onDestroy();
    }
    abstract public void webviewDestory();

    /**
     * danmuku init
     */
    public void initDanMuKu(){
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
    }

    /**
     * cache init
     */
    public void initCache(){
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
    }

    /**
     * cookie init
     */
    private void initCookieData(){
        voiceCookieMap = new HashMap<>();
        int vol = voicePlay ? 50 : 0;

        // Add volume cookies for DMM
        for (String serverIp : SERVER_IP) {
            voiceCookieMap.put("vol_bgm=" + vol + "; domain=" + serverIp + "; path=/kcs2", "http://" + serverIp);
            voiceCookieMap.put("vol_se=" + vol + "; domain=" + serverIp + "; path=/kcs2", "http://" + serverIp);
            voiceCookieMap.put("vol_voice=" + vol + "; domain=" + serverIp + "; path=/kcs2", "http://" + serverIp);
        }

        // Add volume cookies for OOI
        String hostName = prefs.getString("ooi_host_name", "ooi.moe");
        if(hostName.equals("")) hostName = "ooi.moe";
        String fullHostName = hostName;
        if (!fullHostName.startsWith("http")) {
            // XWalkCookieManager requires full URL instead of just domain
            fullHostName = "https://" + fullHostName;
        }
        voiceCookieMap.put("vol_bgm=" + vol + "; domain=" + hostName + "; path=/kcs2", fullHostName);
        voiceCookieMap.put("vol_se=" + vol + "; domain=" + hostName + "; path=/kcs2", fullHostName);
        voiceCookieMap.put("vol_voice=" + vol + "; domain=" + hostName + "; path=/kcs2", fullHostName);


        dmmCokieMap = new HashMap<>();
        dmmCokieMap.put("cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/", "www.dmm.com");
        dmmCokieMap.put("cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame/", "www.dmm.com");
        dmmCokieMap.put("cklg=welcome;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame_s/", "www.dmm.com");
        dmmCokieMap.put("ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/", "www.dmm.com");
        dmmCokieMap.put("ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame/", "www.dmm.com");
        dmmCokieMap.put("ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=.dmm.com;path=/netgame_s/", "www.dmm.com");
        dmmCokieMap.put("ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=osapi.dmm.com;path=/", "www.dmm.com");
        dmmCokieMap.put("ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=203.104.209.7;path=/", "www.dmm.com");
        dmmCokieMap.put("ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=www.dmm.com;path=/netgame/", "www.dmm.com");
        dmmCokieMap.put("ckcy=1;expires=Sun, 09 Feb 2029 09:00:09 GMT;domain=log-netgame.dmm.com;path=/", "www.dmm.com");
    }

    public void initChat(){
        if(chatService) {
            BoxingMediaLoader.getInstance().init(new BoxingPicassoLoader());
            BoxingConfig singleImgConfig = new BoxingConfig(BoxingConfig.Mode.SINGLE_IMG).withMediaPlaceHolderRes(R.drawable.ic_boxing_default_image);
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
                    Boxing.of(singleImgConfig).withIntent(GameBaseActivity.this, LandScapeBoxingActivity.class).start(GameBaseActivity.this, 2);
                }

            }, imageSize);
            chatImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chatNewMsgImageView.setVisibility(View.GONE);
                    dialogUtils.showLeftChat(nickName);
                }
            });
        } else {
            chatImageView.setVisibility(View.GONE);
        }
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
        webviewContentReSize();
    }
    abstract public void webviewContentReSize();

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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

    private class WebviewBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.antest1.kcanotify.h5.webview_reload"))
                webviewReload();
        }
    }
    abstract void webviewReload();

    public Object[] interceptRequest(Uri uri, String requestMethod, Map<String, String> requestHeader){
        String path = uri.getPath();
        final long startTime = System.nanoTime();
        Log.d("KCVA", "requesting  uri：" + uri);
        if(path != null && path.contains("/kcsapi/api_port/port")){
            changeTouchEvent = false;
        } else if(path != null && (path.contains("/kcsapi/api_get_member/mapinfo") || path.contains("/kcsapi/api_get_member/mission"))){
            changeTouchEvent = true;
        }

        if(battleResultVibrate && path != null && (path.contains("battle_result") || path.contains("battleresult"))){
            Vibrator vib = (Vibrator) GameBaseActivity.this.getSystemService(Service.VIBRATOR_SERVICE);
            vib.vibrate(200);
        }

        if ("GET".equals(requestMethod) && path != null && (path.startsWith("/kcs2/") || path.startsWith("/kcs/") || path.startsWith("/gadget_html5/js/kcs_inspection.js"))) {
            if(path.contains("organize_main.png") || path.contains("supply_main.png") || path.contains("remodel_main.png") || path.contains("repair_main.png") || path.contains("arsenal_main.png")){
                changeTouchEvent = true;
            }
            if(path.contains("version.json") || path.contains("index.php")){
                return null;
            }
            try {
                //获取字幕
                String pattern = "/kcs/sound/(.*?)/(.*?).mp3";
                boolean isMatch = Pattern.matches(pattern, path);
                if(isMatch && subTitleEnable){
                    String subTitle = SubTitleUtils.getSubTitle(path);
                    subtitleHandler.removeCallbacks(dismissSubTitle);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            subtitleTextview.setText(subTitle);
                            subtitleStrokeTextview.setText(subTitle);
                        }
                    });
                    subtitleHandler.postDelayed(dismissSubTitle, 15000);
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
                }
                if (tmp.exists()) {
                    //从缓存直接返回客户端，不请求服务器
                    long length = 0;
                    InputStream inputStream;

                    if(path.contains("/gadget_html5/js/kcs_inspection.js")){
                        byte[] fileContent = readFileToBytes(tmp);
                        String newRespStr = new String(fileContent, "utf-8") + "window.onload=function(){document.body.style.background=\"#000\";document.getElementById(\"spacing_top\").style.height=\"0px\";};";
                        fileContent = newRespStr.getBytes("utf-8");

                        length = fileContent.length;
                        inputStream = new ByteArrayInputStream(fileContent);
                    } else if(path.contains("/kcs2/js/main.js")) {
                        // Inject code after caching, so it wont require re-downloading main.js after switching touch mode
                        byte[] fileContent = readFileToBytes(tmp);
                        if (prefs.getBoolean("show_fps_counter", false)) {
                            fileContent = injectFpsUpdater(fileContent);
                        }
                        fileContent = injectTickerTimingMode(fileContent);
                        if (changeTouchEventPrefs && changeWebview) {
                            fileContent = injectTouchLogic(fileContent);
                        }

                        length = fileContent.length;
                        inputStream = new ByteArrayInputStream(fileContent);
                    } else {
                        // Nothing to inject to file content
                        // Avoid extra reading and writing the stream
                        length = tmp.length();
                        inputStream = new FileInputStream(tmp);
                    }

                    Object[] response = backToWebView(path, String.valueOf(length), inputStream);
                    final long duration = System.nanoTime() - startTime;
                    Log.d("KCVA", "Local cache uri："  + uri + " after " + duration/1000 + "us");
                    return response;
                } else {
                    ResponseBody serverResponse = requestServer(uri, requestHeader);
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

                        // Inject code after caching, so it wont require re-downloading main.js after switching touch mode
                        if(path.contains("/kcs2/js/main.js")) {
                            if (prefs.getBoolean("show_fps_counter", false)) {
                                respByte = injectFpsUpdater(respByte);
                            }
                            respByte = injectTickerTimingMode(respByte);
                            if (changeTouchEventPrefs && changeWebview) {
                                respByte = injectTouchLogic(respByte);
                            }
                        }
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

    public Object[] backToWebView(String path, String size, InputStream is){
        String mimeType = null;
        if (path.endsWith("mp3")) {
            mimeType = "audio/mpeg";
        } else if (path.endsWith("png")) {
            mimeType = "image/png";
        } else if (path.endsWith("json")) {
            mimeType = "application/json";
        } else if (path.endsWith("js")) {
            mimeType = "application/javascript";
        } else if (path.endsWith("css")) {
            mimeType = "text/css";
        } else {
            mimeType = "text/html";
        }
        Map<String, String> map = new HashMap<>();
        map.put("Connection", "keep-alive");
        map.put("Server", "KanCollCache");
        map.put("Content-Length", size);
        map.put("Content-Type", mimeType);
        map.put("Cache-Control", "public");
        return new Object[]{mimeType, null, 200, "OK", map, is};
    }

    private ResponseBody requestServer(Uri uri, Map<String, String> headerHeader){
        Request.Builder builder = new Request.Builder().url(uri.toString());
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

    public void jsToJava(String requestUrl, String param, String respData){
        try {
            URL url = new URL(requestUrl);
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

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (!changeWebview) {
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
    private void addDanmaku(String content, boolean withBorder) {
        if(danmakuView == null){
            return;
        }
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

    private byte[] injectTouchLogic(byte[] mainJs){
        // Convert byte[] to String
        String s = new String(mainJs, StandardCharsets.UTF_8);


        // Replace the mouseout and mouseover event name to custom name
        s = s.replace("over:n.pointer?\"pointerover\":\"mouseover\"", "over:\"touchover\"");
        s = s.replace("out:n.pointer?\"pointerout\":\"mouseout\"", "out:\"touchout\"");

        // Add code patch inspired by https://github.com/pixijs/pixi.js/issues/616
        // Only trigger touchout when there is another object start touchover
        s +=    "function patchInteractionManager () {\n" +
                "  var proto = PIXI.interaction.InteractionManager.prototype;\n" +
                "\n" +
                "  function extendMethod (method, extFn) {\n" +
                "    var old = proto[method];\n" +
                "    proto[method] = function () {\n" +
                "      old.call(this, ...arguments);\n" +
                "      extFn.call(this, ...arguments);\n" +
                "    };\n" +
                "  }\n" +
                "\n" +
                "  extendMethod('onTouchMove', function () {\n" +
                "    this.didMove = true;\n" +
                "  });\n" +
                "\n" +
                "  proto.update = mobileUpdate;\n" +
                "\n" +
                "  function mobileUpdate(deltaTime) {\n" +
                "    if (!this.interactionDOMElement) {\n" +
                "      return;\n" +
                "    }\n" +
                "    if(this.didMove) {\n" +
                "      this.didMove = false;\n" +
                "      return;\n" +
                "    }\n" +
                "    if (this.eventData.data && (this.eventData.type == 'touchmove' || this.eventData.type == 'touchstart')) {\n" +
                "      window.__eventData = this.eventData;\n" +
                "      this.processInteractive(this.eventData, this.renderer._lastObjectRendered, this.processTouchOverOut, true);\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  extendMethod('processTouchMove', function(displayObject, hit) {\n" +
                "      this.processTouchOverOut('processTouchMove', displayObject, hit);\n" +
                "  });\n" +
                "  extendMethod('processTouchStart', function(displayObject, hit) {\n" +
                "      this.processTouchOverOut('processTouchStart', displayObject, hit);\n" +
                "  });\n" +
                "\n" +
                "  proto.processTouchOverOut = function (interactionEvent, displayObject, hit) {\n" +
                "    if(hit) {\n" +
                "      if(!displayObject.__over) {\n" +
                "        displayObject.__over = true;\n" +
                "        proto.dispatchEvent( displayObject, 'touchover', window.__eventData);\n" +
                "      }\n" +
                "    } else {\n" +
                "        if(displayObject.__over && interactionEvent.target != displayObject) {\n" +
                "            displayObject.__over = false;\n" +
                "            proto.dispatchEvent( displayObject, 'touchout', window.__eventData);\n" +
                "        }\n" +
                "    }\n" +
                "  };\n" +
                "}\n" +
                "patchInteractionManager();";

        // Convert back to bytes
        return s.getBytes();
    }

    private byte[] injectFpsUpdater(byte[] mainJs){
        // Convert byte[] to String
        String s = new String(mainJs, StandardCharsets.UTF_8);

        // Add code patch inspired by https://github.com/pixijs/pixi.js/issues/616
        s +=    "const times = [];\n" +
                "var lastTimeFps = 0;\n" +
                "function refreshLoop() {\n" +
                "  window.requestAnimationFrame(() => {\n" +
                "    const now = performance.now();\n" +
                "    while (times.length > 0 && times[0] <= now - 1000) {\n" +
                "      times.shift();\n" +
                "    }\n" +
                "    times.push(now);\n" +
                "    if (lastTimeFps != times.length) {\n" +
                "      lastTimeFps = times.length;\n" +
                "      window.fpsUpdater.update(times.length);\n" +
                "    }\n" +
                "    refreshLoop();\n" +
                "  });\n" +
                "}\n" +
                "\n" +
                "refreshLoop();";

        // Convert back to bytes
        return s.getBytes();
    }

    private byte[] injectTickerTimingMode(byte[] mainJs){
        // Convert byte[] to String
        String s = new String(mainJs, StandardCharsets.UTF_8);

        // Replace the ticker timing mode to keep animation smooth even with a lot of events (i.e. touch taps)
        s = s.replace("createjs.Ticker.TIMEOUT", "createjs.Ticker.RAF");

        // Convert back to bytes
        return s.getBytes();
    }


    public void updateFpsCounter(String newFps) {
       fpsCounter.setText(newFps);
   }
}
