package com.antest1.kcanotify.h5;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import andhook.lib.AndHook;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GameOOIActivity extends AppCompatActivity {
    WebView mWebview;
    WebSettings mWebSettings;
    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36";

    private JSONObject jsonObj = null;
    private File cacheJsonFile = null;
    private OkHttpClient client = null;
    private SharedPreferences prefs = null;
    String hostName = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("pref", Context.MODE_PRIVATE);

        setContentView(R.layout.activity_game_webview);
        boolean hardSpeed = prefs.getBoolean("hardware_accelerated", true);
        if(hardSpeed) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
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


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        GameOOIActivity.this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        mWebview = findViewById(R.id.webView1);

        try {
            String cacheJsonPathStr = Environment.getExternalStorageDirectory() + "/KanCollCache/cache.json";
            cacheJsonFile = new File(cacheJsonPathStr);
            if (!cacheJsonFile.getParentFile().exists()) {
                cacheJsonFile.getParentFile().mkdirs();
            }
            if (!cacheJsonFile.exists()) {
                cacheJsonFile.createNewFile();
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


        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(mWebview, true);
        cookieManager.setCookie(hostName, "vol_bgm=0; domain=" + hostName + "; path=/kcs2");
        cookieManager.setCookie(hostName, "vol_se=0; domain=" + hostName + "; path=/kcs2");
        cookieManager.setCookie(hostName, "vol_voice=0; domain=" + hostName + "; path=/kcs2");
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
                if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/poi")) {
                    view.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById(\"externalswf\"),gs=gf.style,gw=1200,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.right='0';gs.zIndex='100';gs.transformOrigin='46.9% 0px 0px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k} kancolleFit()})(document,window)");
                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/poi")) {
                    view.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById(\"externalswf\"),gs=gf.style,gw=1200,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.right='0';gs.zIndex='100';gs.transformOrigin='46.9% 0px 0px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k} kancolleFit()})(document,window)");
                }
            }

            @Override
            public void onReceivedLoginRequest(WebView view, String realm, @Nullable String account, String args) {
                super.onReceivedLoginRequest(view, realm, account, args);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String path = uri.getPath();
                Log.d("KCVA", "Request  uri拦截路径uri：：" + uri);
                if (request.getMethod().equals("GET") && (path.startsWith("/kcs2/") || path.startsWith("/kcs/"))) {
//                    if(!changeTouchEvent) changeTouchEvent = true;
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
                                byte[] respByte = serverResponse.bytes();
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
                            return backToWebView(path, String.valueOf(fileContent.length), new FileInputStream(tmp));
                        } else {
                            ResponseBody serverResponse = requestServer(request);
                            if(serverResponse != null){
                                byte[] respByte = serverResponse.bytes();
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

        mWebview.loadUrl("http://" + hostName + "/poi");
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
        map.put("Cache-Control", "no-cache");
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
        mWebview.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById(\'game_frame\'),gs=gf.style,gw=gf.offsetWidth,gh=gw*.6;let vp=$.querySelector(\'meta[name=viewport]\'),t=0;vp||(vp=$.createElement(\'meta\'),vp.name=\'viewport\',$.querySelector(\'head\').appendChild(vp));vp.content=\'width=\'+gw;\'orientation\'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow=\'hidden\';$.body.style.cssText=\'min-width:0;padding:0;margin:0;overflow:hidden;margin:0\';$.querySelector(\'.dmm-ntgnavi\').style.display=\'none\';$.querySelector(\'.area-naviapp\').style.display=\'none\';$.getElementById(\'ntg-recommend\').style.display=\'none\';gs.position=\'fixed\';gs.marginRight=\'auto\';gs.marginLeft=\'auto\';gs.top=\'-16px\';gs.right=\'0\';gs.zIndex=\'100\';gs.transformOrigin=\'50%25%2016px\';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform=\'scale(\'+w/gw+\')\':gs.transform=\'scale(\'+h/gh+\')\';w<gw?gs.left=\'-\'+(gw-w)/2+\'px\':gs.left=\'0\'};_.addEventListener(\'resize\',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k}kancolleFit()})(document,window)");

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

    /*boolean changeTouchEvent = false;
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d("touchEvent", event.getToolType(0) + ":" + event.getActionMasked());
        WindowManager.LayoutParams v2 = this.getWindow().getAttributes();
        this.getWindow().setAttributes(v2);
        if(event.getToolType(0) == 1 && changeTouchEvent) {
            if(event.getAction() != 2) {
            }
            else {
                MotionEvent.PointerProperties[] v12 = new MotionEvent.PointerProperties[]{new MotionEvent.PointerProperties()};
                event.getPointerProperties(0, v12[0]);
                MotionEvent.PointerCoords[] v13 = new MotionEvent.PointerCoords[]{new MotionEvent.PointerCoords()};
                event.getPointerCoords(0, v13[0]);
                v12[0].toolType = 3;
                long v8 = SystemClock.uptimeMillis();
                this.mWebview.onTouchEvent(MotionEvent.obtain(v8, v8, 2, 1, v12, v13, 0, 0, 0f, 0f, 1, 0, 8194, 0));
            }
        }
        return super.dispatchTouchEvent(event);
    }*/

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
        super.onDestroy();
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
