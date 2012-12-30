package com.totsp.embiggen.messageclient;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.totsp.embiggen.App;

public class MessageClientService extends Service {

   // TODO add methods here so callers can send outgoing messages via the client (through this service)
   // TODO post any incoming messages here to the Bus (so clients don't have to hang off listeners and shit)
   
   private Looper backLooper; // background looper, only used here internally

   // TODO private final Bus bus;   

   private MessageClient client;

   // Binder given to clients
   private final IBinder binder = new LocalBinder();

   /**
    * Class used for the client Binder.  Because we know this service always
    * runs in the same process as its clients, we don't need to deal with IPC.
    */
   public class LocalBinder extends Binder {
      public MessageClientService getService() {
         // Return this instance of MessageClientService so clients can call public methods
         return MessageClientService.this;
      }
   }

   @Override
   public void onCreate() {
      super.onCreate();

      HandlerThread thread = new HandlerThread("BackLooper", Thread.NORM_PRIORITY);
      thread.start();
      backLooper = thread.getLooper();

      Log.d(App.TAG, "MessageClientService ON CREATE");
      
      // run off of main/UI Thread (service uses same thread as other components by default)
      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            client = new MessageClient(MessageClientService.this);
            client.start();
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
            }
         }
      });
   }

   @Override
   public IBinder onBind(Intent intent) {
      Log.d(App.TAG, "MessageClientService BOUND");
      return binder;
   }

   @Override
   public boolean onUnbind(Intent intent) {
      Log.d(App.TAG, "MessageClientService UN-BOUND");
      return super.onUnbind(intent);
   }

   private synchronized void runOnBackThread(Runnable r) {
      new Handler(backLooper).post(r);
   }

   private synchronized void runOnMainThread(Runnable r) {
      new Handler(this.getMainLooper()).post(r);
   }
}
