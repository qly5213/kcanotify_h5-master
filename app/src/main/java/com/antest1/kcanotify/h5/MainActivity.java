package com.antest1.kcanotify.h5;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pixplicity.htmlcompat.HtmlCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import util.UpdateAppUtils;

import static com.antest1.kcanotify.h5.KcaAlarmService.DELETE_ACTION;
import static com.antest1.kcanotify.h5.KcaApiData.loadTranslationData;
import static com.antest1.kcanotify.h5.KcaConstants.*;
import static com.antest1.kcanotify.h5.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.h5.KcaUtils.getId;
import static com.antest1.kcanotify.h5.KcaUtils.getKcIntent;
import static com.antest1.kcanotify.h5.KcaUtils.getNotificationId;
import static com.antest1.kcanotify.h5.KcaUtils.getStringFromException;
import static com.antest1.kcanotify.h5.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.h5.KcaUtils.showDataLoadErrorToast;
import static com.antest1.kcanotify.h5.LocaleUtils.getLocaleCode;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "KCAV";
    public static boolean[] warnType = new boolean[5];
    private static final int REQUEST_VPN = 1;
    public static final int REQUEST_OVERLAY_PERMISSION = 2;
    public static final int REQUEST_EXTERNAL_PERMISSION = 3;

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    AssetManager assetManager;
    KcaDBHelper dbHelper;
    Toolbar toolbar;
    KcaDownloader downloader;
    private boolean running = false;
    private AlertDialog dialogVpn = null;
    Context ctx;
    Intent kcIntent;
    ToggleButton svcbtn;
    Button kcbtn;
    ImageButton kctoolbtn;
    public ImageButton kcafairybtn;
    public static Handler sHandler;
    TextView textDescription;
    TextView textWarn, textSpecial, textResourceReset;
    Gson gson = new Gson();

    SharedPreferences prefs;
    Boolean is_kca_installed = true;
    private WindowManager windowManager;
    private BackPressCloseHandler backPressCloseHandler;
    public static void setHandler(Handler h) {
        sHandler = h;
    }

    public MainActivity() {
        LocaleUtils.updateConfig(this);
    }

    @SuppressLint("ApplySharedPref")
    private void setDefaultPreferences() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        for (String prefKey : PREFS_LIST) {
            if (!pref.contains(prefKey)) {
                Log.e("KCA", prefKey + " pref add");
                String value = SettingActivity.getDefaultValue(prefKey);
                if (value.startsWith("R.string")) {
                    editor.putString(prefKey, getString(getId(value.replace("R.string.", ""), R.string.class)));
                } else if (value.startsWith("boolean_")) {
                    editor.putBoolean(prefKey, Boolean.parseBoolean(value.replace("boolean_", "")));
                } else {
                    editor.putString(prefKey, value);
                }
            }
        }
        editor.commit();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn_main);
        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);
        setDefaultPreferences();

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        assetManager = getAssets();
        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);
        prefs.edit().putBoolean(PREF_SVC_ENABLED, KcaService.getServiceStatus()).apply();

//        downloader = KcaUtils.getInfoDownloader(getApplicationContext());
        kcIntent = getKcIntent(getApplicationContext());
        if (!BuildConfig.DEBUG) is_kca_installed = (kcIntent != null);

        int sniffer_mode = Integer.parseInt(getStringPreferences(getApplicationContext(), PREF_SNIFFER_MODE));


        kcafairybtn = findViewById(R.id.kcafairybtn);
        svcbtn = findViewById(R.id.svcbtn);
        svcbtn.setTextOff(getStringWithLocale(R.string.ma_svc_toggleoff));
        svcbtn.setTextOn(getStringWithLocale(R.string.ma_svc_toggleon));
        svcbtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this, KcaService.class);
                if (isChecked) {
                    if (is_kca_installed) {
                        if (!prefs.getBoolean(PREF_SVC_ENABLED, false)) {
                            loadTranslationData(getApplicationContext());
                            startService(intent);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.ma_toast_kancolle_not_installed), Toast.LENGTH_LONG).show();
                    }
                } else {
                    stopService(intent);
                    prefs.edit().putBoolean(PREF_SVC_ENABLED, false).apply();
                }
            }
        });

        kcbtn = findViewById(R.id.kcbtn);
        kcbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if (is_kca_installed) {
                    startActivity(kcIntent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.ma_toast_kancolle_not_installed), Toast.LENGTH_LONG).show();
                }*/
                SharedPreferences prefs = getSharedPreferences("pref", Context.MODE_PRIVATE);
                String gamePageType = prefs.getString("game_page_type", "0");
                if(gamePageType.equals("0")) {
                    Intent intent = new Intent(MainActivity.this, GameActivity.class);
                    startActivity(intent);
                    MainActivity.this.finish();
                } else {
                    Intent intent = new Intent(MainActivity.this, GameOOIActivity.class);
                    startActivity(intent);
                    MainActivity.this.finish();
                }
            }
        });

        kctoolbtn = findViewById(R.id.kcatoolbtn);
        kctoolbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ToolsActivity.class);
                startActivity(intent);
            }
        });
        kctoolbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.white), PorterDuff.Mode.SRC_ATOP);

        String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
        String fairyPath = "noti_icon_".concat(fairyIdValue);
        KcaUtils.setFairyImageFromStorage(getApplicationContext(), fairyPath, kcafairybtn, 24);
        kcafairybtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.white), PorterDuff.Mode.SRC_ATOP);
        kcafairybtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(getApplicationContext())) {
                    // Can not draw overlays: pass
                } else if (KcaService.getServiceStatus() && sHandler != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("url", KCA_API_FAIRY_RETURN);
                    bundle.putString("data", "");
                    Message sMsg = sHandler.obtainMessage();
                    sMsg.setData(bundle);
                    sHandler.sendMessage(sMsg);
                }
            }
        });

        textWarn = findViewById(R.id.textMainWarn);
        textWarn.setVisibility(View.GONE);

        /*String main_html = "";
        try {
            String locale = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE);
            InputStream ais = assetManager.open("main-".concat(getLocaleCode(locale)).concat(".html"));
            byte[] bytes = ByteStreams.toByteArray(ais);
            main_html = new String(bytes);
        } catch (IOException e) {
            main_html = "Error loading html file.";
        }*/

        textDescription = findViewById(R.id.textDescription);
/*        Spanned fromHtml = HtmlCompat.fromHtml(getApplicationContext(), main_html, 0);
        textDescription.setMovementMethod(LinkMovementMethod.getInstance());
        textDescription.setText(fromHtml);*/
        //Linkify.addLinks(textDescription, Linkify.WEB_URLS);

        backPressCloseHandler = new BackPressCloseHandler(this);

        /*
        ImageView specialImage = findViewById(R.id.special_image);
        specialImage.setVisibility(View.GONE);
        specialImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
            }
        });

        textSpecial = findViewById(R.id.textSpecial);
        textSpecial.setText(getStringWithLocale(R.string.special_playstore_1st));
        textSpecial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                specialImage.setVisibility(View.VISIBLE);
            }
        });
        */
    }

    public void checkVersion(){
        Handler handler = new Handler();
        Thread t = new Thread(() -> {
            downloader = KcaUtils.getH5InfoDownloader(getApplicationContext());
            final Call<String> rv_data = downloader.getH5RecentVersion();
            String response = getResultFromCall(rv_data);
            try {
                if (response != null && !KcaApplication.isCheckVersion && System.currentTimeMillis() - KcaApplication.checkVersionDate > 24 * 60 * 60 * 1000) {
                    KcaApplication.isCheckVersion = true;
                    KcaApplication.checkVersionDate = System.currentTimeMillis();
                    final JsonObject response_data = new JsonParser().parse(response).getAsJsonObject();
                    handler.post(() -> {
                        UpdateAppUtils.from(this)
                                .checkBy(UpdateAppUtils.CHECK_BY_VERSION_NAME) //更新检测方式，默认为VersionCode
                                .serverVersionCode(2641)
                                .serverVersionName(response_data.get("versionName").getAsString())
                                .apkPath(response_data.get("apkUrl").getAsString())
                                .showNotification(true) //是否显示下载进度到通知栏，默认为true
                                .updateInfo(response_data.get("apkInfo").getAsString())  //更新日志信息 String
                                .downloadBy(UpdateAppUtils.DOWNLOAD_BY_APP) //下载方式：app下载、手机浏览器下载。默认app下载
                                .isForce(false) //是否强制更新，默认false 强制更新情况下用户不同意更新则不能使用app
                                .update();
                    });
                }
            } catch (Exception e) {
                dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "version_check", "", "", getStringFromException(e));
            }
        });
        t.start();
    }
    private String getResultFromCall(Call<String> call) {
        KcaRequestThread thread = new KcaRequestThread(call);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return thread.getResult();
    }
    public void getMainHtml(){
        Handler handler = new Handler();
        Thread t = new Thread(() -> {
            downloader = KcaUtils.getH5InfoDownloader(getApplicationContext());
            String locale = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE);
            String localCode = getLocaleCode(locale);
            final Call<String> rv_data = downloader.getH5MainHtml(localCode);
            String response = getResultFromCall(rv_data);
            try {
                if (response != null) {
                    handler.post(() -> {
                        Spanned fromHtml = HtmlCompat.fromHtml(getApplicationContext(), response, 0);
                        textDescription.setMovementMethod(LinkMovementMethod.getInstance());
                        textDescription.setText(fromHtml);
                    });
                }
            } catch (Exception e) {
                dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "version_check", "", "", getStringFromException(e));
            }
        });
        t.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setCheckBtn();

        kcafairybtn = findViewById(R.id.kcafairybtn);
        String fairyIdValue = getStringPreferences(getApplicationContext(), PREF_FAIRY_ICON);
        String fairyPath = "noti_icon_".concat(fairyIdValue);
        KcaUtils.setFairyImageFromStorage(getApplicationContext(), fairyPath, kcafairybtn, 24);
        showDataLoadErrorToast(getApplicationContext(), getStringWithLocale(R.string.download_check_error));
        kcafairybtn.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                R.color.white), PorterDuff.Mode.SRC_ATOP);

        Arrays.fill(warnType, false);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_PERMISSION);
        }

        if (getBooleanPreferences(getApplicationContext(), PREF_KCA_BATTLEVIEW_USE)
                || getBooleanPreferences(getApplicationContext(), PREF_KCA_QUESTVIEW_USE)) {
            warnType[REQUEST_OVERLAY_PERMISSION] = !checkOverlayPermission();
        }
        setWarning();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setCheckBtn();
        checkVersion();
        getMainHtml();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

    public void setWarning() {
        String warnText = "";
        if (warnType[REQUEST_OVERLAY_PERMISSION]) {
            warnText = warnText.concat("\n").concat(getString(R.string.ma_toast_overay_diabled));
        }
        if (warnType[REQUEST_EXTERNAL_PERMISSION]) {
            warnText = warnText.concat("\n").concat(getString(R.string.ma_permission_external_denied));
        }

        if (warnText.length() > 0) {
            textWarn.setVisibility(View.VISIBLE);
            textWarn.setText(warnText.trim());
        } else {
            textWarn.setVisibility(View.GONE);
            textWarn.setText("");
        }
    }

    public void setCheckBtn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        svcbtn.setChecked(prefs.getBoolean(PREF_SVC_ENABLED, false));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        if (id == R.id.action_errorlog) {
            startActivity(new Intent(this, ErrorlogActivity.class));
            return true;
        }
        if (id == R.id.reload_webview) {
            sendBroadcast(new Intent("com.antest1.kcanotify.h5.webview_reload"));
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkOverlayPermission() {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(getApplicationContext()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Log.i(TAG, "onActivityResult request=" + requestCode + " result=" + resultCode + " ok=" + (resultCode == RESULT_OK));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_PERMISSION: {
                warnType[REQUEST_EXTERNAL_PERMISSION] = !(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                setWarning();
            }
        }
    }

    @Override
    public void onBackPressed() {
        backPressCloseHandler.onBackPressed();
    }

    private void checkRecentVersion() {
        String currentVersion = BuildConfig.VERSION_NAME;
        String currentDataVersion = getStringPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION);
        final Call<String> rv_data = downloader.getRecentVersion();
        rv_data.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, retrofit2.Response<String> response) {
                JsonObject response_data = new JsonObject();
                try {
                    if (response.body() != null) {
                        response_data = new JsonParser().parse(response.body()).getAsJsonObject();
                    }
                } catch (Exception e) {
                    dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "version_check", "", "", getStringFromException(e));
                }

                Log.e("KCA", response_data.toString());
                int nid = getNotificationId(NOTI_UPDATE, 0);
                Intent deleteIntent = new Intent(MainActivity.this, KcaAlarmService.class)
                        .setAction(DELETE_ACTION.concat(String.valueOf(nid)));
                startService(deleteIntent);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (KcaUtils.checkOnline(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(),
                            getStringWithLocale(R.string.sa_checkupdate_servererror),
                            Toast.LENGTH_LONG).show();
                    dbHelper.recordErrorLog(ERROR_TYPE_MAIN, "version_check", "", "", t.getMessage());
                }
                int nid = getNotificationId(NOTI_UPDATE, 0);
                Intent deleteIntent = new Intent(MainActivity.this, KcaAlarmService.class)
                        .setAction(DELETE_ACTION.concat(String.valueOf(nid)));
                startService(deleteIntent);
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("KCA", "lang: " + newConfig.getLocales().get(0).getLanguage() + " " + newConfig.getLocales().get(0).getCountry());
            KcaApplication.defaultLocale = newConfig.getLocales().get(0);
        } else {
            Log.e("KCA", "lang: " + newConfig.locale.getLanguage() + " " + newConfig.locale.getCountry());
            KcaApplication.defaultLocale = newConfig.locale;
        }
        if (getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        loadTranslationData(getApplicationContext());
        super.onConfigurationChanged(newConfig);
    }
}

