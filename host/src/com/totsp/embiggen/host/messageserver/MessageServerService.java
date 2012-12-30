package com.totsp.embiggen.host.messageserver;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class MessageServerService extends Service {

   public static final int PORT = 8998;

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
      // run off of main/UI Thread (service uses same thread as other components by default)
      new Thread() {
         @Override
         public void run() {
            server = new MessageServer(null);
            try {
               server.start(PORT);
               //Log.i(Constants.LOG_TAG, "HTTP SERVER STARTED, LISTENING ON PORT:" + PORT);
            } catch (Exception e) {
               //Log.e(Constants.LOG_TAG, "ERROR can't start HTTP server", e);
            }
         }
      }.start();
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      try {
         server.stop();
         //Log.i(Constants.LOG_TAG, "HTTP SERVER STOPPED");
      } catch (Exception e) {
         //Log.e(Constants.LOG_TAG, "ERROR can't stop HTTP server", e);
      }
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
}
