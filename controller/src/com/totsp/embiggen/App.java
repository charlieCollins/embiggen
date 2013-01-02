package com.totsp.embiggen;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import com.squareup.otto.Bus;
import com.totsp.android.util.Installation;
import com.totsp.embiggen.broadcastclient.BroadcastClientService;
import com.totsp.embiggen.broadcastclient.BroadcastClientService.BroadcastClientServiceLocalBinder;
import com.totsp.embiggen.util.RuntimeLoader;
import com.totsp.server.HTTPServerService;
import com.totsp.server.HTTPServerService.HTTPServerServiceLocalBinder;

public class App extends Application {

   public static final String TAG = "Embiggen";

   protected SharedPreferences prefs;
   protected Bus bus;

   private RuntimeLoader runtimeLoader;

   private ConnectivityManager cMgr;

   private static final String HTTP_SERVER_USER_AGENT = "Embiggen-Controller-HTTPD";
   public static final int HTTP_SERVER_PORT = 8999;
   private ServiceConnection httpServerServiceConnection;
   private boolean httpServerServiceBound;
   private HTTPServerService httpServerService;

   private ServiceConnection broadcastClientServiceConnection;
   private boolean broadcastClientServiceBound;
   private BroadcastClientService broadcastClientService;

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

      httpServerServiceConnection = new ServiceConnection() {
         @Override
         public void onServiceConnected(ComponentName className, IBinder binder) {
            HTTPServerServiceLocalBinder localBinder = (HTTPServerServiceLocalBinder) binder;
            httpServerService = localBinder.getService();
            httpServerService.startServer(HTTP_SERVER_USER_AGENT, HTTP_SERVER_PORT, 3, null);
            httpServerServiceBound = true;
            Log.i(App.TAG, "http service connected");
         }

         @Override
         public void onServiceDisconnected(ComponentName comp) {
            httpServerServiceBound = false;
            Log.i(App.TAG, "http service disconnected");
         }
      };
      Log.i(App.TAG, "calling bind http service");
      bindService(new Intent(this, HTTPServerService.class), httpServerServiceConnection, Context.BIND_AUTO_CREATE);

      broadcastClientServiceConnection = new ServiceConnection() {
         @Override
         public void onServiceConnected(ComponentName className, IBinder binder) {
            BroadcastClientServiceLocalBinder localBinder = (BroadcastClientServiceLocalBinder) binder;
            broadcastClientService = localBinder.getService();
            broadcastClientServiceBound = true;
            Log.i(App.TAG, "broadcast service connected");
         }

         @Override
         public void onServiceDisconnected(ComponentName comp) {
            broadcastClientServiceBound = false;
            broadcastClientService = null;
            Log.i(App.TAG, "broadcast service disconnected");
         }
      };
      Log.i(App.TAG, "calling bind socket service");
      bindService(new Intent(this, BroadcastClientService.class), broadcastClientServiceConnection,
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

      if (broadcastClientServiceBound) {
         unbindService(broadcastClientServiceConnection);
         broadcastClientServiceBound = false;
      }

      bus.unregister(this);

      if (gaTracker != null) {
         gaTracker.close();
      }
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
   // accessors
   //
   
   public SharedPreferences getPrefs() {
      return this.prefs;
   }

   public String getInstallationId() {
      return Installation.id(this);
   }
   
   // TODO don't expose entire service via app?
   public BroadcastClientService getBroadcastClientService() {
      return this.broadcastClientService;
   }
}
