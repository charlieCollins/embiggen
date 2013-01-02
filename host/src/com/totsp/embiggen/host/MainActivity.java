package com.totsp.embiggen.host;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;
import android.widget.ViewSwitcher;

import com.squareup.otto.Subscribe;
import com.totsp.embiggen.host.event.DisplayMediaEvent;
import com.totsp.embiggen.host.util.ImageUtil;

/**
 * The Embiggen host.
 *
 */
final public class MainActivity extends BaseActivity {

   // TODO url/image cache 
   private ImageUtil imageUtil;

   private ViewSwitcher switcher;
   private RelativeLayout shareLayout;
   private ImageView shareImageView;
   private VideoView shareVideoView;

   private BroadcastReceiver networkStateReceiver;

   // TODO check network state, and network state listener

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      imageUtil = new ImageUtil(this);

      switcher = (ViewSwitcher) findViewById(R.id.switcher);
      shareLayout = (RelativeLayout) findViewById(R.id.share_layout);
      shareImageView = (ImageView) findViewById(R.id.share_image_view);
      //shareVideoView = (VideoView) findViewById(R.id.share_video_view);
   }

   @Override
   protected void onStart() {
      super.onStart();
      app.getBus().register(this);
      //Log.i(MainActivity.class.getSimpleName(), "in onStart(); haveNetwork is " + haveNetwork);
      //IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
      //registerReceiver(networkStateReceiver, filter);
   }

   @Override
   protected void onPause() {
      super.onPause();
      app.getBus().unregister(this);
   }

   @Override
   protected void onStop() {
      //unregisterReceiver(networkStateReceiver);
      //haveNetwork = Optional.absent();
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

   //
   // subscriptions
   //

   @Subscribe
   public void displayMedia(DisplayMediaEvent e) {
      Log.d(App.TAG, "MainActivity caught display media event:" + e);

      View nextView = switcher.getNextView();
      // RelativeLayout is the layout with ImageView/VideoView
      // (and other option on switcher is just ImageView, which is logo when nothing is playing/shown)
      if (nextView instanceof RelativeLayout) {
         switcher.showNext();
      }
      new GetImageTask().execute(e.getUrlString());
   }

   //
   // private util
   //

   private void setFullscreen(boolean fullscreen) {

   }

   //
   // tasks
   //

   private class GetImageTask extends AsyncTask<String, Void, Bitmap> {

      public GetImageTask() {
      }

      @Override
      protected void onPreExecute() {
      }

      @Override
      protected Bitmap doInBackground(String... args) {
         return imageUtil.get(args[0]);
      }

      @Override
      protected void onPostExecute(Bitmap result) {
         shareImageView.setImageBitmap(result);
      }
   }
}
