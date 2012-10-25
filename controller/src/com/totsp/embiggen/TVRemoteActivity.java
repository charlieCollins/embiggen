package com.totsp.embiggen;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;

import com.example.google.tv.anymotelibrary.client.AnymoteClientService;
import com.example.google.tv.anymotelibrary.client.AnymoteClientService.ClientListener;
import com.example.google.tv.anymotelibrary.client.AnymoteSender;

// http://code.google.com/p/googletv-android-samples/source/browse/BlackJackTVRemote/
// start from BlackJackRemoteActivity and try to make heads/tails of anymote/pairing

public class TVRemoteActivity extends Activity implements ClientListener {
   /**
    * This manages discovering, pairing and connecting to Google TV devices on
    * network.
    */
   private AnymoteClientService anymoteClientService;

   /**
    * The proxy used to send events to the server using Anymote Protocol
    */
   private AnymoteSender anymoteSender;

   /** 
    * Defines callbacks for service binding, passed to bindService() 
    */
   private ServiceConnection serviceConnection = new ServiceConnection() {
      /*
       * ServiceConnection listener methods.
       */
      public void onServiceConnected(ComponentName name, IBinder service) {
         anymoteClientService = ((AnymoteClientService.AnymoteClientServiceBinder) service).getService();
         anymoteClientService.attachClientListener(TVRemoteActivity.this);
      }

      public void onServiceDisconnected(ComponentName name) {
         anymoteClientService.detachClientListener(TVRemoteActivity.this);
         anymoteClientService = null;
      }
   };

   private TextView output;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Setup the Activity UI
      setContentView(R.layout.tvremote);

      output = (TextView) findViewById(R.id.tv_remote_output);

      //Button hit = (Button) findViewById(R.id.hit);
      //Button stand = (Button) findViewById(R.id.stand);
      //Button newgame = (Button) findViewById(R.id.newgame);

      // Setup button click listeners.
      /*
      hit.setOnClickListener(new OnClickListener() {
              @Override
          public void onClick(View v) {
              // Sends Keycode H for 'Hit'.
              sendKeyEvent(KeyEvent.KEYCODE_H);
          }
      });
      stand.setOnClickListener(new OnClickListener() {
              @Override // Sends Keycode S for 'Stand'.
          public void onClick(View v) {
              sendKeyEvent(KeyEvent.KEYCODE_S);
          }
      });
      newgame.setOnClickListener(new OnClickListener() {
              @Override // Sends Keycode N for starting new game.
          public void onClick(View v) {
              sendKeyEvent(KeyEvent.KEYCODE_N);
          }
      });
      */

      output.setText("Connecting to Google TV...");

      // Bind to the AnymoteClientService
      Intent intent = new Intent(TVRemoteActivity.this, AnymoteClientService.class);
      bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
   }

   @Override
   protected void onDestroy() {
      if (anymoteClientService != null) {
         anymoteClientService.detachClientListener(this);
      }
      unbindService(serviceConnection);
      super.onDestroy();
   }

   private void sendKeyEvent(final int keyEvent) {
      // create new Thread to avoid network operations on UI Thread
      if (anymoteSender == null) {
         Toast.makeText(TVRemoteActivity.this, "Waiting for connection", Toast.LENGTH_LONG).show();
         return;
      }
      anymoteSender.sendKeyPress(keyEvent);
   }
   
   private void sendData(final String data) {
      // create new Thread to avoid network operations on UI Thread
      if (anymoteSender == null) {
         Toast.makeText(TVRemoteActivity.this, "Waiting for connection", Toast.LENGTH_LONG).show();
         return;
      }
      anymoteSender.sendData(data);
   }

   //
   // AnymoteClientService.ClientListener methods
   //

   @Override
   public void onConnected(final AnymoteSender anymoteSender) {
      Toast.makeText(TVRemoteActivity.this, R.string.pairing_succeeded_toast, Toast.LENGTH_LONG).show();

      this.anymoteSender = anymoteSender;

      // launch TV app on Google TV through Anymote...
      final Intent tvAppIntent = new Intent("android.intent.action.MAIN");
      tvAppIntent.setComponent(new ComponentName("com.totsp.embiggen.host", "com.totsp.embiggen.host.MainActivity"));
      anymoteSender.sendIntent(tvAppIntent);

      output.setText("CONNECTED");
      
      sendData("this is a test");
   }

   @Override
   public void onDisconnected() {
      Toast.makeText(TVRemoteActivity.this, "onDisconnected", Toast.LENGTH_LONG).show();
      this.anymoteSender = null;
   }

   // R.string.pairing_failed_toast
   @Override
   public void onConnectionFailed() {
      Toast.makeText(TVRemoteActivity.this, "onConnectionFailed", Toast.LENGTH_LONG).show();
      this.anymoteSender = null;
   }
}
