package com.totsp.embiggen.host.broadcastserver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.squareup.otto.Bus;
import com.totsp.embiggen.host.App;

public class BroadcastServerService extends Service {

   // TODO add methods here so callers can send outgoing messages via the server (through this service)
   // TODO post any incoming messages here to the Bus (so clients don't have to hang off listeners and shit)

   private Looper backLooper; // background looper, only used here internally

   private BroadcastServer server;

   // Binder given to clients
   private final IBinder binder = new LocalBinder();

   /**
    * Class used for the client Binder.  Because we know this service always
    * runs in the same process as its clients, we don't need to deal with IPC.
    */
   public class LocalBinder extends Binder {
      public BroadcastServerService getService() {
         // Return this instance of BroadcastServerService so clients can call public methods
         return BroadcastServerService.this;
      }
   }

   @Override
   public void onCreate() {
      super.onCreate();

      HandlerThread thread = new HandlerThread("BackLooper", Thread.NORM_PRIORITY);
      thread.start();
      backLooper = thread.getLooper();

      Log.i(App.TAG, "BroadcastServerService onCreate");

      final App app = (App) this.getApplication();
      startServer(app.getBus(), app.getHttpServerPort());
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      stopServer();
   }

   @Override
   public IBinder onBind(Intent intent) {
      //Log.d(Constants.LOG_TAG, "BroadcastServerService BOUND");
      return binder;
   }

   @Override
   public boolean onUnbind(Intent intent) {
      //Log.d(Constants.LOG_TAG, "BroadcastServerService UN-BOUND");
      return super.onUnbind(intent);
   }

   private void startServer(final Bus bus, final int port) {
      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            if (server != null) {
               stopServer();
            }
            server = new BroadcastServer(BroadcastServerService.this, bus);
            server.start(port);
         }
      });
   }

   private void stopServer() {
      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            if (server != null) {
               server.stop();
               server = null;
            }
         }
      });
   }

   private synchronized void runOnBackThread(Runnable r) {
      new Handler(backLooper).post(r);
   }

   private synchronized void runOnMainThread(Runnable r) {
      new Handler(this.getMainLooper()).post(r);
   }
}
