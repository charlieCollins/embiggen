package com.totsp.embiggen;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.totsp.android.util.Screen;

public class Preferences extends PreferenceActivity {

   private CheckBoxPreference testMode;
   private Preference showScreenInfo;
   private ListPreference galleryBucketSortType;

   @Override
   protected void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      addPreferencesFromResource(R.layout.preferences);

      // handle to preferences doesn't come from findViewById!
      testMode = (CheckBoxPreference) getPreferenceScreen().findPreference("testMode");
      showScreenInfo = (Preference) getPreferenceScreen().findPreference("showScreenInfo");
      galleryBucketSortType = (ListPreference) getPreferenceScreen().findPreference("galleryBucketSortType");

      showScreenInfo.setOnPreferenceClickListener(new OnPreferenceClickListener() {
         @Override
         public boolean onPreferenceClick(Preference arg0) {
            Screen screen = new Screen(Preferences.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(Preferences.this);
            builder.setTitle("Screen info summary (debug)").setMessage(screen.summaryText())
                     .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                           dialog.cancel();
                        }
                     });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
         }
      });

      setCheckboxSummary(testMode);
      setListSummary(galleryBucketSortType);

      // listen to see if user changes pref, so we can update display of current value
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
         @Override
         public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("testMode")) {
               setCheckboxSummary(testMode);
            } else if (key.equals("galleryBucketSortType")) {
               setListSummary(galleryBucketSortType);
            }
         }
      });
   }

   private void setCheckboxSummary(CheckBoxPreference pref) {
      if (pref.isChecked()) {
         pref.setSummary("Enabled");
      } else {
         pref.setSummary("Disabled");
      }
   }

   private void setListSummary(ListPreference pref) {
      pref.setSummary(pref.getEntry());
   }
}