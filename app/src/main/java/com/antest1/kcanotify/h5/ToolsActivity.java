package com.antest1.kcanotify.h5;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.google.gson.Gson;

import java.util.Locale;

import static com.antest1.kcanotify.h5.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.h5.KcaUtils.getStringPreferences;


public class ToolsActivity extends AppCompatActivity {
    Toolbar toolbar;
    static Gson gson = new Gson();
    LinearLayout view_fleetlist, view_shiplist, view_equipment, view_droplog, view_reslog, view_akashi, view_expcalc, view_expdtable, view_datasync;
    public ToolsActivity() {
        LocaleUtils.updateConfig(this);
    }

    private String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getApplicationContext(), getBaseContext(), id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_list);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getStringWithLocale(R.string.action_tools));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        view_fleetlist = findViewById(R.id.action_fleetlist);
        view_shiplist = findViewById(R.id.action_shiplist);
        view_equipment = findViewById(R.id.action_equipment);
        view_droplog = findViewById(R.id.action_droplog);
        view_reslog = findViewById(R.id.action_reslog);
        view_akashi = findViewById(R.id.action_akashi);
        view_expcalc = findViewById(R.id.action_expcalc);
        view_expdtable = findViewById(R.id.action_expdtable);

        view_fleetlist.setOnClickListener(view -> {
            Intent intent = new Intent(ToolsActivity.this, FleetInfoActivity.class);
            startActivity(intent);
        });

        view_shiplist.setOnClickListener(view -> {
            Intent intent = new Intent(ToolsActivity.this, ShipInfoActivity.class);
            startActivity(intent);
        });

        view_equipment.setOnClickListener(view -> {
            Intent intent = new Intent(ToolsActivity.this, EquipmentInfoActivity.class);
            startActivity(intent);
        });

        view_droplog.setOnClickListener(view -> {
            Intent intent = new Intent(ToolsActivity.this, DropLogActivity.class);
            startActivity(intent);
        });

        view_reslog.setOnClickListener(view -> {
            Intent intent = new Intent(ToolsActivity.this, ResourceLogActivity.class);
            startActivity(intent);
        });

        view_akashi.setOnClickListener(view -> {
            Intent intent = new Intent(ToolsActivity.this, AkashiActivity.class);
            startActivity(intent);
        });

        view_expcalc.setOnClickListener(view -> {
            Intent intent = new Intent(ToolsActivity.this, ExpCalcActivity.class);
            startActivity(intent);
        });

        view_expdtable.setOnClickListener(view -> {
            Intent intent = new Intent(ToolsActivity.this, ExpeditionTableActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        if(getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).startsWith("default")) {
            LocaleUtils.setLocale(Locale.getDefault());
        } else {
            String[] pref = getStringPreferences(getApplicationContext(), PREF_KCA_LANGUAGE).split("-");
            LocaleUtils.setLocale(new Locale(pref[0], pref[1]));
        }
        super.onConfigurationChanged(newConfig);
    }
}
