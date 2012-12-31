package com.totsp.embiggen.messageclient;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.totsp.embiggen.App;

import java.net.InetSocketAddress;

public class MessageClientService extends Service {

   // TODO add methods here so callers can send outgoing messages via the client (through this service)
   // TODO post any incoming messages here to the Bus (so clients don't have to hang off listeners and shit)

   // TDOO investigate HandlerThread and back looper more, doesn't always post?
   // also keep in mind MessageClient has executors?
   private HandlerThread backThread; // background looper, only used here internally
   private Handler backHandler;

   // TODO private final Bus bus;   

   private MessageClient client;

   public class MessageClientServiceLocalBinder extends Binder {
      public MessageClientService getService() {
         return MessageClientService.this;
      }
   }

   private final IBinder binder = new MessageClientServiceLocalBinder();

   @Override
   public void onCreate() {
      super.onCreate();

      backThread = new HandlerThread("BackLooper", Thread.NORM_PRIORITY);
      backThread.start();
      backHandler = new Handler(backThread.getLooper());

      Log.d(App.TAG, "MessageClientService ON CREATE");

      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            client = new MessageClient(MessageClientService.this);
            client.start(); // don't auto start?
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

      backThread.quit();
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

   //
   // public for bound clients
   //

   public void restartClient() {
      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            if (client != null) {
               client.stop();
               client.start();
            }
         }
      });
   }

   // clients can use this to check the discovery status, if not null, host has been discovered
   public InetSocketAddress getHostInetSocketAddress() {
      return client.getHostInetSocketAddress();
   }

   public void sendMessageToHost(final String msg) {
      runOnBackThread(new Runnable() {
         @Override
         public void run() {
            if (client != null && client.getHostInetSocketAddress() != null) {
               client.sendMessage(msg);
            } else {
               Log.e(App.TAG, "Cannot send message, client is null, or has not discovered host");
            }
         }
      });
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
