package com.totsp.embiggen;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

import com.totsp.embiggen.util.MessageClient;

public class Login extends BaseActivity {

   // require wifi or not, emulator does not support wifi, so set to true to use in emul
   private boolean testMode;

   private EditText code;
   private Button loginJoin;

   private MessageClient messageClient;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.login);

      code = (EditText) findViewById(R.id.login_code);
      loginJoin = (Button) findViewById(R.id.login_join);

      loginJoin.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            //advance();
            startActivity(new Intent(Login.this, LaunchChooser.class));
         }
      });

      code.setOnKeyListener(new OnKeyListener() {
         @Override
         public boolean onKey(View arg0, int keycode, KeyEvent arg2) {
            if (keycode == KeyEvent.KEYCODE_ENTER) {
               startActivity(new Intent(Login.this, LaunchChooser.class));
               return true;
            }
            return false;
         }
      });

      testMode = app.getPrefs().getBoolean("testMode", false);

   }

   @Override
   protected void onStart() {
      messageClient = new MessageClient(this);
      messageClient.start();
      
      super.onStart();
   }

   @Override
   protected void onStop() {
      messageClient.stop();
      
      super.onStop();
   }

   @Override
   protected void onResume() {
      super.onResume();

      // determine if WiFi is enabled or not, if not prompt user to enable it  
      boolean wifiEnabled = false;
      if (!app.wifiConnectionPresent() && !testMode) {
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle("WiFi is not enabled").setMessage("Go to settings and enable WiFi (or retry)?")
                  .setCancelable(true).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                        startActivity(intent);
                     }
                  }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                     }
                  }).setNeutralButton("Retry", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        startActivity(new Intent(Login.this, Login.class));
                        finish();
                     }
                  });
         AlertDialog alert = builder.create();
         alert.show();
      } else {
         wifiEnabled = true;
      }

      // determine if external storage is available
      boolean extStorageEnabled = true;
      // TODO check for ext storage avail, or not?
      // we use EXTERNAL_CONTENT_URI, so presumably ext storage must be avail?
      /*
      boolean mExternalStorageAvailable = false;
      boolean mExternalStorageWriteable = false;
      String state = Environment.getExternalStorageState();

      if (Environment.MEDIA_MOUNTED.equals(state)) {
      // We can read and write the media
      mExternalStorageAvailable = mExternalStorageWriteable = true;
      } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      // We can only read the media
      mExternalStorageAvailable = true;
      mExternalStorageWriteable = false;
      } else {
      // Something else is wrong. It may be one of many other states, but all we need
      //  to know is we can neither read nor write
      mExternalStorageAvailable = mExternalStorageWriteable = false;
      }       
       */

      if (wifiEnabled && extStorageEnabled) {
         loginJoin.setEnabled(true);
      } else {
         loginJoin.setEnabled(false);
      }
   }

   protected String getViewName() {
      return "Login";
   }

   private void advance() {
      if (code.getText().toString().trim() != null && code.getText().toString().trim().length() == 5) {
         //Log.d(App.TAG, "JOINING ROOM");
         dialog.setMessage("Joining...");
         dialog.show();
         // was joinRoomAsController
         // TODO anymore wiring         
      }

      /*
       final AccountManager manager = AccountManager.get(this);
      final Account[] accounts = manager.getAccountsByType("com.google");
      final int size = accounts.length;
      String[] names = new String[size];
      for (int i = 0; i < size; i++) {
      names[i] = accounts[i].name;
      }
       */
   }

   //
   // Override ONLY IMOVLConnect delegate methods you're interested in (BaseActivity base class will handle rest)
   //

   // NOTE onLogin and onError are handled by base Activity

   /*
   @Override
   public void onJoinRoom(final IMCResult res, final IMCRoom room) {
      // must run back on UI thread, since MOVLConnect client makes network calls off of main thread
      this.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            if (res != null && res.getCode() == 0) {
               dialog.dismiss();

               startActivity(new Intent(Login.this, LaunchChooser.class));

               // TODO tv app doesn't seem to support this, but web tester does? (never get onMessage)
              
               finish();

            } else {

               dialog.dismiss();

               String message = res.getMessage();
               Log.e(App.TAG, "ERROR connecting:" + message);

               if (message.contains("cannot locate room")) {
                  Toast.makeText(
                           Login.this,
                           "You have entered an invalid TV Code, or there is a network problem. Please try again.",
                           Toast.LENGTH_LONG).show();
               } else {
                  if (message.equalsIgnoreCase("connecting")) {
                     Toast.makeText(Login.this, "Still connecting, please wait momentarily.", Toast.LENGTH_SHORT)
                              .show();
                  } else {
                     Toast.makeText(Login.this, "There was an error connecting (" + message + "), please try again.",
                              Toast.LENGTH_SHORT).show();
                  }
               }

               
            }
         }
      });
      
   }
   */

   /*
   @Override
   public void onMessage(IMCUser sender, final String messageId, final MCData messageData, String target) {
      super.onMessage(sender, messageId, messageData, target);
      // must run back on UI thread, since MOVLConnect client makes network calls off of main thread
      this.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            if (messageId.equals("onTestConnection")) {
               if (messageData.containsKey("result") && messageData.getInt("result") == 0) {
                  startActivity(new Intent(Login.this, LaunchChooser.class));
               } else {
                  Toast.makeText(Login.this, "Error, host failed in connection test to controller", Toast.LENGTH_SHORT)
                           .show();
               }
            }
         }
      });
   }
   */

}
