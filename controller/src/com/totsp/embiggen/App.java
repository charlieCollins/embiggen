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

import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import com.squareup.otto.Bus;
import com.totsp.android.util.Installation;
import com.totsp.embiggen.messageclient.MessageClientService;
import com.totsp.embiggen.util.RuntimeLoader;

public class App extends Application {

   public static final String TAG = "Embiggen";

   protected SharedPreferences prefs;
   protected Bus bus;

   private RuntimeLoader runtimeLoader;

   private ConnectivityManager cMgr;

   private ServiceConnection httpServerServiceConnection;
   private boolean httpServerServiceBound;

   private ServiceConnection messageClientServiceConnection;
   private boolean messageClientServiceBound;

   private Tracker gaTracker;

   // prevent emul issue
   static {
      System.setProperty("java.net.preferIPv4Stack", "true");
      System.setProperty("java.net.preferIPv6Addresses", "false");
   }

   @Override
   public void onCreate() {
      super.onCreate();

      Log.i(TAG, "EMBIGGEN Application onCreate");

      runtimeLoader = new RuntimeLoader(this);
      Log.i(TAG, "   using environment " + runtimeLoader.getEnv());

      bus = new Bus();
      bus.register(this);

      //this.objectGraph = ObjectGraph.create(new EmbiggenModule());
      //this.objectGraph.inject(this);

      prefs = PreferenceManager.getDefaultSharedPreferences(this);

      cMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

      /*
      httpServiceConnection = new ServiceConnection() {
         @Override
         public void onServiceConnected(ComponentName className, IBinder binder) {
            
            httpServiceBound = true;
            Log.i(App.TAG, "http service connected");
         }

         @Override
         public void onServiceDisconnected(ComponentName comp) {
            // TODO make sure service.onDestroy cleans up sockets/etc
            httpServiceBound = false;
            Log.i(App.TAG, "http service disconnected");
         }
      };    
      Log.i(App.TAG, "calling bind http service");
      ///bindService(new Intent(this, HTTPServerService.class), connection, Context.BIND_AUTO_CREATE);       
       */

      messageClientServiceConnection = new ServiceConnection() {
         @Override
         public void onServiceConnected(ComponentName className, IBinder binder) {
            /*
            LocalBinder localBinder = (LocalBinder) binder;
            HTTPServerService service = localBinder.getService();             
             */
            messageClientServiceBound = true;
            Log.i(App.TAG, "socket service connected");
         }

         @Override
         public void onServiceDisconnected(ComponentName comp) {
            // TODO make sure service.onDestroy cleans up sockets/etc
            messageClientServiceBound = false;
            Log.i(App.TAG, "socket service disconnected");
         }
      };
      Log.i(App.TAG, "calling bind socket service");
      bindService(new Intent(this, MessageClientService.class), messageClientServiceConnection,
               Context.BIND_AUTO_CREATE);

      String gaId = runtimeLoader.getGoogleAnalyticsId();
      if (gaId != null && !gaId.trim().equals("")) {
         // NOTE intentionally NOT using analytics.xml resource because we must update settings at runtime (such as which id)
         GoogleAnalytics ga = GoogleAnalytics.getInstance(getApplicationContext());
         ///ga.setDebug(true);
         gaTracker = ga.getTracker(gaId);
         ga.setDefaultTracker(gaTracker);
         GAServiceManager.getInstance().setDispatchPeriod(60);
         gaTracker.setStartSession(true);
      }

   }

   @Override
   public void onLowMemory() {
      super.onLowMemory();
   }

   // not guaranteed to be called
   @Override
   public void onTerminate() {
      super.onTerminate();
      if (httpServerServiceBound) {
         unbindService(httpServerServiceConnection);
         httpServerServiceBound = false;
      }

      if (messageClientServiceBound) {
         unbindService(messageClientServiceConnection);
         messageClientServiceBound = false;
      }

      bus.unregister(this);

      if (gaTracker != null) {
         gaTracker.close();
      }
   }

   public SharedPreferences getPrefs() {
      return this.prefs;
   }

   public String getInstallationId() {
      return Installation.id(this);
   }

   //
   // ga
   //

   public void gaTrackView(String view) {
      if (gaTracker != null) {
         gaTracker.trackView(view);
      }
   }

   public void gaTrackEvent(String category, String action, String label) {
      if (gaTracker != null) {
         gaTracker.trackEvent(category, action, label, null);
      }
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
