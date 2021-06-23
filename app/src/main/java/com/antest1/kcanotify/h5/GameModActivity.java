package com.antest1.kcanotify.h5;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GameModActivity extends Activity {
    private ExecutorService pool = Executors.newFixedThreadPool(5);
    WebView webView;
    public static String host = "http://www.uyan.pw/";
    private OkHttpClient client = null;
    ArrayList<String> typeDataList = new ArrayList<>();
    ArrayList<String> typeIdDataList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_webview);
        webView = findViewById(R.id.webView1);
        typeDataList.add("立绘");
        typeIdDataList.add("full");
        typeDataList.add("立绘中破");
        typeIdDataList.add("full_dmg");
        typeDataList.add("铭牌");
        typeIdDataList.add("banner");
        typeDataList.add("铭牌中破");
        typeIdDataList.add("banner_dmg");
        typeDataList.add("改修");
        typeIdDataList.add("character_full");
        typeDataList.add("改修中破");
        typeIdDataList.add("character_full_dmg");
        typeDataList.add("改造");
        typeIdDataList.add("remodel");
        typeDataList.add("改造中破");
        typeIdDataList.add("remodel_dmg");
        typeDataList.add("补给");
        typeIdDataList.add("supply_character");
        typeDataList.add("补给中破");
        typeIdDataList.add("supply_character_dmg");
        typeDataList.add("图鉴");
        typeIdDataList.add("card");
        typeDataList.add("图鉴中破");
        typeIdDataList.add("card_dmg");
        typeDataList.add("图鉴小图");
        typeIdDataList.add("character_up");
        typeDataList.add("图鉴小图中破");
        typeIdDataList.add("character_up_dmg");
        client = new OkHttpClient.Builder().build();
        webView.addJavascriptInterface(new Object(){
            @JavascriptInterface
            public void JsToJavaInterface(String modId, String title, String apiId) {
                try {
                    pool.execute(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            androidDownloadMod(modId, title, apiId);
                        }
                    }));
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        },"androidJs");
        WebSettings mWebSettings = webView.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        WebView.setWebContentsDebuggingEnabled(true);

        //设置WebChromeClient类
        webView.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
            }

            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
            }

            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return super.onConsoleMessage(consoleMessage);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                String modJson = buildModJson();
                webView.loadUrl("javascript:" + "var shipModJson = " + buildModJson() + ";");
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
                return null;
            }
        });

        webView.loadUrl(host);
    }
    public String buildModJson() {

        String result = null;
        try {
            String pathDirStr = "/KanCollCache" + File.separator + "mod";
            File modDirfile = new File(Environment.getExternalStorageDirectory(), pathDirStr);
            if (!modDirfile.exists()) {
                return "[]";
            }
            JSONArray jsonModShipArr = new JSONArray();
            File[] subFiles = modDirfile.listFiles();
            for(File f : subFiles){
                if(f.getName().endsWith("json")){
                    jsonModShipArr.put(new JSONObject(new String(readFileToBytes(f))));
                }
            }
            /*JSONObject shipObject = new JSONObject(oriData);
            JSONArray jsonShipArr = shipObject.getJSONObject("api_data").getJSONArray("api_mst_shipgraph");
            for (int i = 0; i < jsonModShipArr.length(); i++) {
                JSONObject modShipJsonObj = jsonModShipArr.getJSONObject(i);
                for (int j = 0; j < jsonShipArr.length(); j++) {
                    JSONObject shipJsonObj = jsonShipArr.getJSONObject(j);
                    if (shipJsonObj.getInt("api_id") == modShipJsonObj.getInt("api_id")) {
                        jsonShipArr.put(j, modShipJsonObj);
                        break;
                    }
                }
            }*/
            result = jsonModShipArr.toString();
        } catch (Exception e){
            e.printStackTrace();
        }
        return result;
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
    public void androidDownloadMod(String modId, String title, String apiId){
        try{
            ResponseBody serverResponse = requestServer(host + "mod/" + modId + "/config.json");
            if (serverResponse == null) {
                return;
            }
            String configStr = serverResponse.string();
            if(TextUtils.isEmpty(apiId)){
                JSONArray configJsonArr = new JSONArray(configStr);
                for(int i = 0; i<configJsonArr.length(); i++){
                    downloadUIPng(modId, configJsonArr.getString(i));
                }
            } else {
                JSONObject configJson = new JSONObject(configStr);
                saveFile(title + "_" + modId + ".json", configStr.getBytes());

                for (int i = 0; i < typeIdDataList.size(); i++) {
                    downloadShipPng(typeIdDataList.get(i), typeDataList.get(i), modId, apiId, configJson);
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(GameModActivity.this, "全部下载完成", Toast.LENGTH_LONG).show();
                    webView.loadUrl("javascript:app.$loading().close();");
                }
            });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void downloadShipPng(String typeId, String typeName, String modId, String apiId, JSONObject configJson){
        try {
            String path;
            if(typeId.equals("full") || typeId.equals("full_dmg")){
                path = "/kcs2/resources/ship/" + typeId + "/" + KanCollUtils.getShipImgName(typeId, apiId, configJson.getString("api_filename")) + ".png";
            } else {
                path = "/kcs2/resources/ship/" + typeId + "/" + KanCollUtils.getShipImgName(typeId, apiId, null) + ".png";
            }
            ResponseBody serverResponse = requestServer(host + "mod/" + modId + path);
            if (serverResponse == null) {
                return;
            }
            saveFile(path, serverResponse.bytes());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public void downloadUIPng(String modId, String path){
        try {
            ResponseBody serverResponse = requestServer(host + "mod/" + modId + path);
            if (serverResponse == null) {
                return;
            }
            saveFile(path, serverResponse.bytes());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private ResponseBody requestServer(String url){

        Log.d("KCA", url);
        Request.Builder builder = new Request.Builder().url(url);
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
        File file = new File(Environment.getExternalStorageDirectory(),"/KanCollCache/mod/" + path);
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
    protected void onResume() {
        super.onResume();
        webView.reload();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
