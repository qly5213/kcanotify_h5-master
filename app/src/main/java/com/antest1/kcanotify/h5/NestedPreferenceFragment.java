package com.antest1.kcanotify.h5;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.Map;

public class NestedPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String NESTED_TAG = "NESTED_TAG";
    public final static int FRAGMENT_ADV_NETWORK = 701;

    public String getStringWithLocale(int id) {
        return KcaUtils.getStringWithLocale(getActivity().getApplicationContext(), getActivity().getBaseContext(), id);
    }

    public static NestedPreferenceFragment newInstance(int key) {
        NestedPreferenceFragment fragment = new NestedPreferenceFragment();
        Bundle args = new Bundle();
        args.putInt(NESTED_TAG, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("pref");

        int pref_key = getArguments().getInt(NESTED_TAG);

        Log.e("KCA", "PREF_KEY " + pref_key);
        switch (pref_key) {
            case FRAGMENT_ADV_NETWORK:
                addPreferencesFromResource(R.xml.advance_network_settings);
                ((AppCompatActivity) getActivity()).getSupportActionBar()
                        .setTitle(getStringWithLocale(R.string.setting_menu_kand_title_adv_network));
                break;
            default:
                break;
        }

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Map<String, ?> allEntries = getPreferenceManager().getSharedPreferences().getAll();
        //SharedPreferences prefs = this.getActivity().getSharedPreferences("pref", MODE_PRIVATE);
        for (String key : allEntries.keySet()) {
            Preference pref = findPreference(key);
            if (pref instanceof ListPreference) {
                ListPreference etp = (ListPreference) pref;
                pref.setSummary(etp.getEntry());
            } else if (pref instanceof EditTextPreference) {
                EditTextPreference etp = (EditTextPreference) pref;
                pref.setSummary(etp.getText());
            }
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference etp = (ListPreference) pref;
            pref.setSummary(etp.getEntry());
        } else if (pref instanceof EditTextPreference) {
            EditTextPreference etp = (EditTextPreference) pref;
            pref.setSummary(etp.getText());
        }
    }
}
