package com.totsp.embiggen.host;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.totsp.android.util.Installation;

public class App extends Application  {

   public static final String LOG_TAG = "Embiggen-Host";

   private ConnectivityManager cMgr;
   private SharedPreferences prefs;
   
   protected GoogleAnalyticsTracker tracker;

   // prevent error on Android emul
   static {
      System.setProperty("java.net.preferIPv4Stack", "true");
      System.setProperty("java.net.preferIPv6Addresses", "false");
   }

   @Override
   public void onCreate() {
      super.onCreate();

      cMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
      prefs = PreferenceManager.getDefaultSharedPreferences(this);

      // Google Analytics
      tracker = GoogleAnalyticsTracker.getInstance();
      tracker.startNewSession(BaseActivity.ANALYTICS_ACCOUNT_ID, 20, this);    
   }

   // not guaranteed to be called, but plays nice most of the time
   @Override
   public void onTerminate() {
      super.onTerminate();


      ///tracker.stopSession();
   }

   //
   // accessors for activities
   //
   public GoogleAnalyticsTracker getTracker() {
      return this.tracker;
   }  

   public SharedPreferences getPrefs() {
      return this.prefs;
   }

   public String getInstallationId() {
      return Installation.id(this);
   }   

   //
   // util/helpers for app
   //   
   public boolean connectionPresent() {
      NetworkInfo netInfo = cMgr.getActiveNetworkInfo();
      if ((netInfo != null) && (netInfo.getState() != null)) {
         return netInfo.getState().equals(State.CONNECTED);
      }
      return false;
   }

   public boolean wifiConnectionPresent() {
      NetworkInfo netInfo = cMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      return netInfo.isConnectedOrConnecting();
   }

   public String getWifiIpAddress() {
      if (wifiConnectionPresent()) {
         WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
         WifiInfo wifiInfo = wifiManager.getConnectionInfo();
         int ipAddress = wifiInfo.getIpAddress();
         return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
                  (ipAddress >> 24 & 0xff));
      }
      return null;
   }
}
