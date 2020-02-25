package com.antest1.kcanotify.h5;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.webkit.WebView;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;

import java.util.Locale;

import static com.antest1.kcanotify.h5.KcaConstants.PREF_KCA_LANGUAGE;

@AcraCore(buildConfigClass = BuildConfig.class)
/*@AcraMailSender(
        mailTo = "",
        reportAsFile = false
)*/

public class KcaApplication extends MultiDexApplication {
    public static Locale defaultLocale;
    public static Activity gameActivity;
    public static boolean isCheckVersion = false;
    public static long checkVersionDate = 0;
    private static final String PROCESSNAME = "com.antest1.kcanotify.h5";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        ACRA.init(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        String language, country;
        defaultLocale = Locale.getDefault();
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        String[] pref_locale = pref.getString(PREF_KCA_LANGUAGE, "").split("-");

        if (pref_locale.length == 2) {
            if (pref_locale[0].equals("default")) {
                LocaleUtils.setLocale(defaultLocale);
            } else {
                language = pref_locale[0];
                country = pref_locale[1];
                LocaleUtils.setLocale(new Locale(language, country));
            }
        } else {
            pref.edit().remove(PREF_KCA_LANGUAGE).apply();
            LocaleUtils.setLocale(defaultLocale);
        }

        LocaleUtils.updateConfig(this, getBaseContext().getResources().getConfiguration());
        ACRA.init(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = getProcessName();
            if (!PROCESSNAME.equals(processName)) {
                WebView.setDataDirectorySuffix("modWeb");
            }
        }
    }

    private static KcaApplication application;
    public static KcaApplication getInstance(){
        return application;
    }
}
