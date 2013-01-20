package com.totsp.embiggen.host;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.analytics.tracking.android.Tracker;
import com.squareup.otto.Bus;
import com.totsp.android.util.Installation;
import com.totsp.embiggen.host.broadcastserver.BroadcastServerService;
import com.totsp.embiggen.host.event.DisplayMediaEvent;
import com.totsp.server.HTTPServerService;
import com.totsp.server.HTTPServerService.HTTPServerServiceLocalBinder;
import com.totsp.server.TextRequestCallback;

import java.util.Random;

public class App extends Application {

   public static final String TAG = "Embiggen-Host";

   private SharedPreferences prefs;

   private Bus bus;

   private static final String HTTP_SERVER_USER_AGENT = "Embiggen-Host-HTTPD";
   private ServiceConnection httpServerServiceConnection;
   private boolean httpServerServiceBound;
   private HTTPServerService httpServerService;
   private TextRequestCallback httpServerTextRequestCallback;
   private int httpServerPort;
   
   private ServiceConnection broadcastServerServiceConnection;
   private boolean broadcastServerServiceBound;

   private Tracker gaTracker;

   // prevent error on Android emul
   static {
      System.setProperty("java.net.preferIPv4Stack", "true");
      System.setProperty("java.net.preferIPv6Addresses", "false");
   }

   @Override
   public void onCreate() {
      super.onCreate();

      prefs = PreferenceManager.getDefaultSharedPreferences(this);

      bus = new Bus();

      // TODO much more robust url parsing, and define protocol, with type?
      // come up with actual protocol and parser
      httpServerTextRequestCallback = new TextRequestCallback() {
         @Override
         public void onRequest(String request) {
            Log.d(App.TAG, "Got HTTP text message:" + request);
            if (request != null && request.startsWith("?DISPLAY_MEDIA=")) {
               final String url = request.substring(request.indexOf("?DISPLAY_MEDIA=") + 15, request.length());
               runOnMainThread(new Runnable() {
                  public void run() {
                     bus.post(new DisplayMediaEvent(url));
                  }
               });
            }
         }
      };

      // HTTP server stuff (HTTP server is used for clients to simply post text messages to this host, such as "DISPLAY_MEDIA!<url>"
      // 8378 is the FIXED broadcast port, so range regular socket server 8379-8399
      // TODO check if ports selected are in use on same device/LAN (right now just assumes they aren't)   
      Random rand = new Random();
      httpServerPort = rand.nextInt(8399 - 8378 + 1) + 8378;

      httpServerServiceConnection = new ServiceConnection() {
         @Override
         public void onServiceConnected(ComponentName className, IBinder binder) {
            HTTPServerServiceLocalBinder localBinder = (HTTPServerServiceLocalBinder) binder;
            httpServerService = localBinder.getService();
            httpServerService.startServer(HTTP_SERVER_USER_AGENT, httpServerPort, 15, httpServerTextRequestCallback);
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

      // broadcast server stuff
      broadcastServerServiceConnection = new ServiceConnection() {
         @Override
         public void onServiceConnected(ComponentName className, IBinder binder) {
            broadcastServerServiceBound = true;
            Log.i(App.TAG, "broadcast service connected");
         }

         @Override
         public void onServiceDisconnected(ComponentName comp) {
            broadcastServerServiceBound = false;
            Log.i(App.TAG, "broadcast service disconnected");
         }
      };
      Log.i(App.TAG, "calling bind socket service");
      bindService(new Intent(this, BroadcastServerService.class), broadcastServerServiceConnection,
               Context.BIND_AUTO_CREATE);

      // TODO google analytics
      String gaId = null;
      /*
      if (gaId != null && !gaId.trim().equals("")) {
         // NOTE intentionally NOT using analytics.xml resource because we must update settings at runtime (such as which id)
         GoogleAnalytics ga = GoogleAnalytics.getInstance(getApplicationContext());
         ///ga.setDebug(true);
         gaTracker = ga.getTracker(gaId);
         ga.setDefaultTracker(gaTracker);
         GAServiceManager.getInstance().setDispatchPeriod(60);
         gaTracker.setStartSession(true);
      }
      */
   }

   // not guaranteed to be called, but plays nice most of the time
   @Override
   public void onTerminate() {
      super.onTerminate();

      if (broadcastServerServiceBound) {
         unbindService(broadcastServerServiceConnection);
         broadcastServerServiceBound = false;
      }

      if (httpServerServiceBound) {
         unbindService(httpServerServiceConnection);
         httpServerServiceBound = false;
      }

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
   // accessors for activities
   //

   public SharedPreferences getPrefs() {
      return this.prefs;
   }

   public String getInstallationId() {
      return Installation.id(this);
   }
   
   // using last 6 chars of unique installId as hostId (should be unique enough 99% of time)
   public String getHostId() {
      String installId = getInstallationId();      
      return installId.substring(installId.length() - 6, installId.length());
   }

   public Bus getBus() {
      return this.bus;
   }

   public int getHttpServerPort() {
      return this.httpServerPort;
   }  

   //
   // priv
   //

   private synchronized void runOnMainThread(Runnable r) {
      new Handler(getMainLooper()).post(r);
   }
}
