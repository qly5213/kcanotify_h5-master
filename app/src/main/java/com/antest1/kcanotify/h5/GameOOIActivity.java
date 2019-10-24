package com.antest1.kcanotify.h5;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONObject;
import org.xwalk.core.JavascriptInterface;
import org.xwalk.core.XWalkCookieManager;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public class GameOOIActivity extends GameBaseActivity {
    XWalkView mWebview;
    XWalkSettings mWebSettings;

    // HTML5 audio is bugged in Crosswalk
    // Add "edge" to UA so that Kancolle will fallback to traditional audio playing
    private final static String USER_AGENT = "Mozilla/5.0 (Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Safari/537.36 Edge/14.14931";

    String hostName = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWebview = (XWalkView) super.mWebview;
        hostName = prefs.getString("ooi_host_name", "ooi.moe");
        if(hostName.equals("")) hostName = "ooi.moe";

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
                if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/poi")) {
                    webviewContentReSize();
                } else if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/")){
                    boolean isAutoUser = prefs.getBoolean("ooi_auto_user", false);
                    if(isAutoUser){
                        String userName = prefs.getString("dmm_user", "");
                        String pwd = prefs.getString("dmm_pwd", "");
                        mWebview.loadUrl("javascript:$(\"#login_id\").val(\""+userName+"\");$(\"#password\").val(\""+pwd+"\");document.getElementById(\"mode3\").checked = true;");
                    }
                }
            }

            //设置结束加载函数
            @Override
            public void onLoadFinished(XWalkView view, String url) {
                if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/poi")) {
                    webviewContentReSize();
                } else if(view.getUrl() != null && view.getUrl().equals("http://" + hostName + "/")){
                    boolean isAutoUser = prefs.getBoolean("ooi_auto_user", false);
                    if(isAutoUser){
                        String userName = prefs.getString("dmm_user", "");
                        String pwd = prefs.getString("dmm_pwd", "");
                        mWebview.loadUrl("javascript:$(\"#login_id\").val(\""+userName+"\");$(\"#password\").val(\""+pwd+"\");document.getElementById(\"mode3\").checked = true;");
                    }
                }
            }

            @Override
            public XWalkWebResourceResponse shouldInterceptLoadRequest(XWalkView webView, XWalkWebResourceRequest request) {
                Object[] result = interceptRequest(request.getUrl(), request.getMethod(), request.getRequestHeaders());
                if (result != null) {
                    return this.createXWalkWebResourceResponse((String) result[0], (String) result[1], (InputStream) result[5], (Integer) result[2], (String) result[3], (Map<String, String>) result[4]);
                } else {
                    return null;
                }
            }
        });

        mWebview.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void JsToJavaInterface(String requestUrl, String param, String respData) {
                loginExpire(requestUrl, respData);
                jsToJava(requestUrl, param, respData);
            }
        },"androidJs");

        mWebview.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void update(String newFps) {
                updateFpsCounter(newFps);
            }
        },"fpsUpdater");
    }

    public void loginExpire(String requestUrl, String respData){

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
    }

    @Override
    protected void onXWalkReady() {
        boolean clearCookie = prefs.getBoolean("clear_cookie_start", false);
        if(clearCookie){
            (new XWalkCookieManager()).removeAllCookie();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("clear_cookie_start", false);
            editor.apply();
        }

        XWalkCookieManager cookieManager = new XWalkCookieManager();
        cookieManager.setAcceptCookie(true);
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
        cookieManager.flushCookieStore();

        mWebSettings = mWebview.getSettings();
        mWebSettings.setUserAgentString(USER_AGENT);
        mWebSettings.setBuiltInZoomControls(true);
        mWebSettings.setCacheMode(XWalkSettings.LOAD_NO_CACHE);
/*        Properties prop = System.getProperties();
        prop.setProperty("proxySet", "true");
        prop.setProperty("proxyHost", "218.241.131.227");
        prop.setProperty("proxyPort", "6100");*/
        // 设置与Js交互的权限
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setMediaPlaybackRequiresUserGesture(false);

//        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);

        mWebview.loadUrl("http://" + hostName + "/poi");
        mWebview.onShow();
        mWebview.resumeTimers();
    }



    @Override
    public void webviewContentReSize() {
        mWebview.loadUrl("javascript:(($,_)=>{const html=$.documentElement,gf=$.getElementById(\"externalswf\"),gs=gf.style,gw=1200,gh=gw*.6;let vp=$.querySelector('meta[name=viewport]'),t=0;vp||(vp=$.createElement('meta'),vp.name='viewport',$.querySelector('head').appendChild(vp));vp.content='width='+gw;'orientation'in _&&html.webkitRequestFullscreen&&html.webkitRequestFullscreen();html.style.overflow='hidden';$.body.style.cssText='min-width:0;padding:0;margin:0;overflow:hidden;margin:0';gs.position='fixed';gs.marginRight='auto';gs.marginLeft='auto';gs.right='0';gs.zIndex='100';gs.transformOrigin='46.9% 0px 0px';if(!_.kancolleFit){const k=()=>{const w=html.clientWidth,h=_.innerHeight;w/h<1/.6?gs.transform='scale('+w/gw+')':gs.transform='scale('+h/gh+')';w<gw?gs.left='-'+(gw-w)/2+'px':gs.left='0'};_.addEventListener('resize',()=>{clearTimeout(t);t=setTimeout(k,10)});_.kancolleFit=k} kancolleFit()})(document,window)");
    }

    @Override
    public void webviewPause() {
        mWebview.onHide();
        mWebview.pauseTimers();
    }

    @Override
    public void webviewResume() {
        // It may also be called once at startup without xwalkview being ready
        if (this.isXWalkReady()) {
            mWebview.onShow();
            mWebview.resumeTimers();
        }
    }

    @Override
    public void webviewDestory() {
        mWebview.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
        ((ViewGroup) mWebview.getParent()).removeView(mWebview);
        mWebview.onDestroy();
        mWebview = null;
    }

    @Override
    void webviewReload() {
        mWebview.reload(XWalkView.RELOAD_IGNORE_CACHE);
    }
}
