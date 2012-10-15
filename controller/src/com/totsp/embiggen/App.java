package com.totsp.embiggen;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.totsp.android.util.Installation;
import com.totsp.server.HTTPServerService;

public class App extends Application {

   public static final String LOG_TAG = "Embiggen-Controller";
   
   private ConnectivityManager cMgr;
   
   private SharedPreferences prefs;
   
   private ServiceConnection connection;
   private boolean bound;
   
   protected GoogleAnalyticsTracker tracker;

   // prevent emul bug
   static {
      System.setProperty("java.net.preferIPv4Stack", "true");
      System.setProperty("java.net.preferIPv6Addresses", "false");
   }

   @Override
   public void onCreate() {
      super.onCreate();

      // this can only used with Android 2.3 or later
      /*
      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
      */

      cMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
      prefs = PreferenceManager.getDefaultSharedPreferences(this);

      // TODO wire in anymote
      
      connection = new ServiceConnection() {
         @Override
         public void onServiceConnected(ComponentName className, IBinder binder) {
            //LocalBinder localBinder = (LocalBinder) binder;
            //service = localBinder.getService();
            // get ref to service here if need to call methods on it directly
            bound = true;
            Log.i(App.LOG_TAG, "service connected");
         }

         @Override
         public void onServiceDisconnected(ComponentName arg0) {
            // TODO make sure service.onDestroy cleans up sockets/etc
            bound = false;
            Log.i(App.LOG_TAG, "service disconnected");
         }
      };
      Intent intent = new Intent(this, HTTPServerService.class);
      Log.i(App.LOG_TAG, "calling bind service");
      bindService(intent, connection, Context.BIND_AUTO_CREATE);   
      
      tracker = GoogleAnalyticsTracker.getInstance();
      tracker.startNewSession(BaseActivity.ANALYTICS_ACCOUNT_ID, 20, this);
   }

   // not guaranteed to be called
   @Override
   public void onTerminate() {
      super.onTerminate();
      if (bound) {
         unbindService(connection);
         bound = false;
      }
      
      tracker.stopSession();
      
      
   }

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
