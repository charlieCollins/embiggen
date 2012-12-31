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

import java.net.InetSocketAddress;

public class MessageClientService extends Service {

   // TODO add methods here so callers can send outgoing messages via the client (through this service)
   // TODO post any incoming messages here to the Bus (so clients don't have to hang off listeners and shit)

   private Looper backLooper; // background looper, only used here internally

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
      if (client != null && client.getHostInetSocketAddress() != null) {
         runOnBackThread(new Runnable() {
            @Override
            public void run() {
               client.sendMessage(msg);
            }
         });
      } else {
         Log.e(App.TAG, "Cannot send message, client is null, or has not discovered host");
      }
   }

   //
   // priv
   //

   private synchronized void runOnBackThread(Runnable r) {
      new Handler(backLooper).post(r);
   }

   /*
   private synchronized void runOnMainThread(Runnable r) {
      new Handler(this.getMainLooper()).post(r);
   }
   */
}
