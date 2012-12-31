package com.totsp.embiggen.host;

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

import com.google.analytics.tracking.android.Tracker;
import com.squareup.otto.Bus;
import com.totsp.android.util.Installation;
import com.totsp.embiggen.host.messageserver.MessageServerService;

public class App extends Application {

   public static final String TAG = "Embiggen-Host";

   private ConnectivityManager cMgr;
   private SharedPreferences prefs;
   
   protected Bus bus;

   private ServiceConnection messageServerServiceConnection;
   private boolean messageServerServiceBound;

   private Tracker gaTracker;

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

      bus = new Bus();
      bus.register(this);
      
      messageServerServiceConnection = new ServiceConnection() {
         @Override
         public void onServiceConnected(ComponentName className, IBinder binder) {
            /*
            LocalBinder localBinder = (LocalBinder) binder;
            HTTPServerService service = localBinder.getService();             
             */
            messageServerServiceBound = true;
            Log.i(App.TAG, "socket service connected");
         }

         @Override
         public void onServiceDisconnected(ComponentName comp) {
            // TODO make sure service.onDestroy cleans up sockets/etc
            messageServerServiceBound = false;
            Log.i(App.TAG, "socket service disconnected");
         }
      };
      Log.i(App.TAG, "calling bind socket service");
      bindService(new Intent(this, MessageServerService.class), messageServerServiceConnection,
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

      if (messageServerServiceBound) {
         unbindService(messageServerServiceConnection);
         messageServerServiceBound = false;
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
   // accessors for activities
   //

   public SharedPreferences getPrefs() {
      return this.prefs;
   }

   public String getInstallationId() {
      return Installation.id(this);
   }

   public Bus getBus() {
      return this.bus;
   }

   public void setBus(Bus bus) {
      this.bus = bus;
   }  
}
