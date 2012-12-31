package com.totsp.embiggen.host.messageserver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import com.totsp.embiggen.host.App;

import java.util.Random;

public class MessageServerService extends Service {

   // TODO add methods here so callers can send outgoing messages via the server (through this service)
   // TODO post any incoming messages here to the Bus (so clients don't have to hang off listeners and shit)
   
   private Looper backLooper; // background looper, only used here internally

   private MessageServer server;

   // Binder given to clients
   private final IBinder binder = new LocalBinder();

   /**
    * Class used for the client Binder.  Because we know this service always
    * runs in the same process as its clients, we don't need to deal with IPC.
    */
   public class LocalBinder extends Binder {
      public MessageServerService getService() {
         // Return this instance of MessageServerService so clients can call public methods
         return MessageServerService.this;
      }
   }

   @Override
   public void onCreate() {
      super.onCreate();
      
      HandlerThread thread = new HandlerThread("BackLooper", Thread.NORM_PRIORITY);
      thread.start();
      backLooper = thread.getLooper();

      // 8378 is the FIXED broadcast port, so range regular socket server 8379-8399
      // TODO check if ports selected are in use on same device/LAN (right now just assumes they aren't)
      Random rand = new Random();
      final int port = rand.nextInt(8399 - 8378 + 1) + 8378;
      
      System.out.println("MessageServerService ON CREATE, random port:" + port);
      
      final App app = (App) this.getApplication();
      
      // run off of main/UI Thread (service uses same thread as other components by default)
      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            server = new MessageServer(MessageServerService.this, app.getBus());
            server.start(port);
         }
      });
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      
      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            if (server != null) {
               server.stop();
            }
         }
      });
   }

   @Override
   public IBinder onBind(Intent intent) {
      //Log.d(Constants.LOG_TAG, "MessageServerService BOUND");
      return binder;
   }

   @Override
   public boolean onUnbind(Intent intent) {
      //Log.d(Constants.LOG_TAG, "MessageServerService UN-BOUND");
      return super.onUnbind(intent);
   }
   
   private synchronized void runOnBackThread(Runnable r) {
      new Handler(backLooper).post(r);
   }

   private synchronized void runOnMainThread(Runnable r) {
      new Handler(this.getMainLooper()).post(r);
   }
}
