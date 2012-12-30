package com.totsp.embiggen.host;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ViewSwitcher;

import com.google.common.base.Optional;
import com.totsp.android.util.NetworkUtil;
import com.totsp.embiggen.host.messageserver.MessageServer;
import com.totsp.embiggen.host.util.ImageUtil;

/**
 * An Embiggen room.
 *
 * Handles all communication over Connect, drives other internal presenter components.
 */
final public class MainActivity extends BaseActivity {

   private ImageUtil imageUtil;

   private ViewSwitcher switcher;
   private RelativeLayout shareLayout;
   private ImageView shareImageView;
   private VideoView shareVideoView;

   private BroadcastReceiver networkStateReceiver;
   private Optional<Boolean> haveNetwork = Optional.absent(); // we have no idea until we look.

   // TODO MessageServer here is temp for quick test, will be in service
   private MessageServer messageServer;
   
   //
   // standard android lifecycle methods
   //

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.room);

      imageUtil = new ImageUtil(getApplicationContext());

      switcher = (ViewSwitcher) findViewById(R.id.switcher);
      shareLayout = (RelativeLayout) findViewById(R.id.share_layout);
      shareImageView = (ImageView) findViewById(R.id.share_image_view);
      shareVideoView = (VideoView) findViewById(R.id.share_video_view);

      // keep an eye on network state with internal receiver
      networkStateReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, final Intent intent) {
            Log.w("Network Listener", "Network Type Changed");
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                  boolean haveNetworkNow = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                  if (haveNetworkNow) {
                     if (haveNetwork.isPresent() && !haveNetwork.get()) {
                        Toast.makeText(MainActivity.this, getString(R.string.network_connection_restored),
                                 Toast.LENGTH_SHORT).show();
                     }
                     connect();
                  } else {
                     if (haveNetwork.isPresent() && haveNetwork.get()) {
                        Toast.makeText(MainActivity.this, getString(R.string.network_connection_lost),
                                 Toast.LENGTH_SHORT).show();
                     }
                     transitionToDisconnected();
                  }
                  haveNetwork = Optional.of(haveNetworkNow);
                  Log.i(MainActivity.class.getSimpleName(), "set mHaveNetwork to " + haveNetwork);
               }
            });
         }
      };
      
      
      // TEMPORARY, just start MessageServer and test it
      WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
      String wifiIpAddress = NetworkUtil.getWifiIpAddress(wifiMgr);
      messageServer = new MessageServer(wifiIpAddress);
      messageServer.start(MessageServer.DEFAULT_SERVER_PORT);
   }
   
   @Override
   protected synchronized void onStart() {
      super.onStart();
      Log.i(MainActivity.class.getSimpleName(), "in onStart(); haveNetwork is " + haveNetwork);
      IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
      registerReceiver(networkStateReceiver, filter);
   }
   
   @Override
   protected void onPause() {
      super.onPause();
   }

   @Override
   protected void onStop() {
      transitionToDisconnected();

      unregisterReceiver(networkStateReceiver);
      haveNetwork = Optional.absent();
      
      messageServer.stop();
      
      super.onStop();
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
   }   

   @Override
   protected String getViewName() {
      return "HostMain";
   }

   @Override
   protected void backToStart(boolean showToast, boolean finish) {
      finish();
   }

   private void transitionToConnected() {
      if (dialog.isShowing()) {
         dialog.dismiss();
      }
      /*
      ((TextView) findViewById(R.id.footer_code_view)).setText(app.getClient().getRoom().getCode());
      for (Integer id : ImmutableList.of(R.id.footer_code_view, R.id.footer_text_view)) {
         findViewById(id).setVisibility(View.VISIBLE);
      }
      */
   }

   private void transitionToDisconnected() {
      if (dialog.isShowing()) {
         dialog.dismiss();
      }
      /*
      for (Integer id : ImmutableList.of(R.id.footer_code_view, R.id.footer_text_view)) {
         findViewById(id).setVisibility(View.INVISIBLE);
      } 
      */
      Toast.makeText(this, "Network disconnected", Toast.LENGTH_LONG).show();
   }

   private void connect() {
      /*
      if (app.getClient().isConnected()) {
         transitionToConnected();
      } else {
         dialog.setMessage(getString(R.string.connecting));
         dialog.show();
         backgroundThread.submit(new Runnable() {
            @Override
            public void run() {
               app.getClient().createRoomAsHost("EmbiggenHostUser", "Embiggen-" + System.currentTimeMillis(),
                        MCRoomVisibility.DEFAULT, 0, 100);
            }
         });
      }
      */
   }

   

   /*
   @Override
   public void onCreateRoom(final IMCResult result, IMCRoom room) {
      if (result.isOk()) {
         Log.i(getClass().getSimpleName(), "Room created with code " + room.getCode());
      }
      runOnUiThread(new Runnable() {
         @Override
         public void run() {
            if (result.isOk()) {
               transitionToConnected();
            } else {
               Toast.makeText(RoomActivity.this, "ERROR creating room: " + result.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }
         }
      });
   }
   */   


   // declare and register messages we expect to receive from other Connect clients
   // (this is the apps message "protocol" in terms of ids)
   final public static class DisplayMedia {
   }

   final public static class SkipMedia {
   }

   /*
   static {
      MOVLMessage.register(ImmutableList.of(DisplayMedia.class, SkipMedia.class));
   }
   */

   // onMessage from connect listener is used to post the message to the message bus (and later @Subscribe methods will receive)
   /*
   @Override
   public void onMessage(IMCUser sender, String messageId, MCData messageData, String target) {
      super.onMessage(sender, messageId, messageData, target);
      Log.i(getClass().getSimpleName(), "Received message " + messageId + " with data " + messageData);
      for (MOVLMessage msg : MOVLMessage.newInstance(sender, messageId, messageData, target).asSet()) {
         Log.i(getClass().getSimpleName(), "dispatching message " + msg.toString() + " on background thread");
         backgroundThread.post(msg);
      }
   }

   @Subscribe
   public void handle(DisplayMedia msg) {
      Log.i(getClass().getSimpleName(), "handling " + msg.toString());
      try {

         MCData data = msg.getMessageData();
         final String type = data.getString("type");
         final String url = data.getString("url");
         Log.d(App.TAG, "displayMedia type:" + type + " url:" + url);         
         
         // photo
         if (type.equalsIgnoreCase("photo")) {
            new Thread() {
               @Override
               public void run() {
                  // TODO do this right, not like this (but can't use CACHE on main thread, no net allowed)
                  final Bitmap bitmap = imageUtil.get(url);
                  runOnUiThread(new Runnable() {
                     @Override
                     public void run() {  
                        if (switcher.getNextView().equals(shareLayout)) {
                           switcher.showNext();
                        }                        
                        
                        if (shareVideoView.getVisibility() != View.GONE) {
                           shareVideoView.setVisibility(View.GONE);
                        }
                        if (shareImageView.getVisibility() != View.VISIBLE) {
                           shareImageView.setVisibility(View.VISIBLE);
                        }                       
                        shareImageView.setImageBitmap(bitmap);
                     }
                  });
               }
            }.start();
         }
         
         // video
         if (type.equalsIgnoreCase("video")) {
            new Thread() {
               @Override
               public void run() {                  
                  runOnUiThread(new Runnable() {
                     @Override
                     public void run() {  
                        if (switcher.getNextView().equals(shareLayout)) {
                           switcher.showNext();
                        }                        
                        
                        if (shareImageView.getVisibility() != View.GONE) {
                           shareImageView.setVisibility(View.GONE);
                        }
                        if (shareVideoView.getVisibility() != View.VISIBLE) {
                           shareVideoView.setVisibility(View.VISIBLE);
                        }                       
                        // TODO videview listener and setUri, etc
                        // TODO when video ends switch back to image of nothing happening vs share layout
                     }
                  });
               }
            }.start();
         }
      } catch (final Exception e) {
         Log.e(getClass().getSimpleName(), "exception while trying to display media", e);
         Toast.makeText(this, "Error displaying media:" + e.getMessage(), Toast.LENGTH_LONG).show();
      }
   }
   
   @Subscribe
   public void handle(SkipMedia msg) {
      Log.i(getClass().getSimpleName(), "handling " + msg.toString());
      try {

         // TODO 

      } catch (final Exception e) {
         Log.e(getClass().getSimpleName(), "exception while trying to skip media", e);
         // toast, etc?
      }
   }
   */

   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event) {
      switch (keyCode) {
         case KeyEvent.KEYCODE_MEDIA_PAUSE:
            // TODO if vid playing
            return true;
         case KeyEvent.KEYCODE_MEDIA_PLAY:
            // TODO if vid playing
            return true;
         case KeyEvent.KEYCODE_MEDIA_NEXT:
            // TODO if vid playing
            return true;
         case KeyEvent.KEYCODE_S: // skip when you don't have media_next
            // TODO if vid playing
            return true;
         case KeyEvent.KEYCODE_L: // logout without menu
            // TODO make sure logged in? 
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to logout?").setCancelable(false)
                     .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                           //app.getClient().leaveRoom();
                        }
                     }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                           dialog.cancel();
                        }
                     });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
            ///case KeyEvent.KEYCODE_F:
            ///   setFullscreen(!fullscreen);
            ///   return true;

      }
      return super.onKeyDown(keyCode, event);
   }

   //
   // private util
   //

   private void setFullscreen(boolean fullscreen) {

   }

}
