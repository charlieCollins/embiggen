package com.totsp.embiggen.broadcastclient;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.totsp.embiggen.App;
import com.totsp.embiggen.broadcastclient.BroadcastClient.HostHttpServerInfo;

public class BroadcastClientService extends Service {

   // TODO add methods here so callers can send outgoing messages via the client (through this service)
   // TODO post any incoming messages here to the Bus (so clients don't have to hang off listeners and shit)

   private HandlerThread backThread; // background looper, only used here internally
   private Handler backHandler;

   // TODO private final Bus bus;   

   private BroadcastClient client;

   public class BroadcastClientServiceLocalBinder extends Binder {
      public BroadcastClientService getService() {
         return BroadcastClientService.this;
      }
   }

   private final IBinder binder = new BroadcastClientServiceLocalBinder();

   @Override
   public void onCreate() {
      super.onCreate();

      backThread = new HandlerThread("BackLooper", Thread.NORM_PRIORITY);
      backThread.start();
      backHandler = new Handler(backThread.getLooper());

      Log.d(App.TAG, "BroadcastClientService ON CREATE");

      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            client = new BroadcastClient(BroadcastClientService.this);
         }
      });
   }

   @Override
   public void onDestroy() {
      super.onDestroy();

      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            if (client != null) {
               client.stop();
               client.clearHostHttpServerInfo();
            }
         }
      });

      backThread.quit();
   }

   @Override
   public IBinder onBind(Intent intent) {
      Log.d(App.TAG, "BroadcastClientService BOUND");
      return binder;
   }

   @Override
   public boolean onUnbind(Intent intent) {
      Log.d(App.TAG, "BroadcastClientService UN-BOUND");
      return super.onUnbind(intent);
   }

   //
   // public for bound components
   //

   public void startClient() {
      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            client.start();
         }
      });
   }

   public void stopClient() {
      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            if (client != null) {
               client.stop();
            }
         }
      });
   }

   public void clearHostHttpServerInfo() {
      if (client != null) {
         client.clearHostHttpServerInfo();
      }
   }

   // clients can use this to check the discovery status, if not null, host has been discovered
   public HostHttpServerInfo getHostHttpServerInfo() {
      if (client != null) {
         return client.getHostHttpServerInfo();
      }
      return null;
   }

   //
   // priv
   //

   // NOTE this is a SINGLE background thread, if runnables posted to it block, then any subsequent requests block!
   private synchronized void runOnBackThread(Runnable r) {
      backHandler.post(r);
   }

   /*
   private synchronized void runOnMainThread(Runnable r) {
      new Handler(this.getMainLooper()).post(r);
   }
   */
}
