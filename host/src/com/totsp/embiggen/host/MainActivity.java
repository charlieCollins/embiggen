package com.totsp.embiggen.host;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewCallback;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.squareup.otto.Subscribe;
import com.totsp.android.util.NetworkUtil;
import com.totsp.embiggen.host.event.DisplayMediaEvent;

/**
 * The Embiggen host.
 *
 */
final public class MainActivity extends BaseActivity {

   // TODO improve UI/UX
   // TODO help/about
   // TODO preferences for quality settings, etc?

   private ProgressBar loaderProgressBar;
   private ImageView shareImageView;
   private VideoView shareVideoView;
   private TextView hostId;
   
   protected PowerManager.WakeLock wakeLock;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      
      loaderProgressBar = (ProgressBar) findViewById(R.id.footer_progress_bar);
      shareImageView = (ImageView) findViewById(R.id.share_image);
      shareVideoView = (VideoView) findViewById(R.id.share_video);
      hostId = (TextView) findViewById(R.id.footer_hostid);

      hostId.setText(getString(R.string.hostid_prefix) + app.getHostId());
      
      PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
      wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, App.TAG);
      wakeLock.acquire();
   }

   @Override
   protected void onResume() {
      if (!NetworkUtil.connectionPresent((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))) {
         Toast.makeText(this, getString(R.string.network_connection_not_present), Toast.LENGTH_LONG).show();
         finish();
      }
      super.onResume();
   }
   
   

   @Override
   protected void onDestroy() {
      wakeLock.release();
      super.onDestroy();
   }

   @Override
   protected String getViewName() {
      return "MainActivity-Host";
   }

   // TODO wire in commands so controller can also send these, not just host keyboard
   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event) {
      switch (keyCode) {
         case KeyEvent.KEYCODE_MEDIA_PAUSE:
            if (shareVideoView.isShown() && shareVideoView.canPause()) {
               shareVideoView.pause();
            }
            return true;
         case KeyEvent.KEYCODE_MEDIA_PLAY:
            if (shareVideoView.isShown() && !shareVideoView.isPlaying()) {
               shareVideoView.start();
            }
            return true;
         case KeyEvent.KEYCODE_S: // skip when you don't have media_next
            if (shareVideoView.isShown()) {
               shareVideoView.stopPlayback();
               shareVideoView.setVisibility(View.GONE);
            }
            return true;
      }
      return super.onKeyDown(keyCode, event);
   }

   //
   // subscriptions
   //

   // TODO add displayMediaSlideShow or such (allow slideshows)

   @Subscribe
   public void displayMedia(DisplayMediaEvent e) {

      // determine media type from filename
      String fileExt = MimeTypeMap.getFileExtensionFromUrl(e.getUrlString());
      MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
      String mimeType = null;
      if (fileExt != null) {
         mimeType = mimeTypeMap.getMimeTypeFromExtension(fileExt);
      }

      Log.d(App.TAG, "MainActivity caught display media event:" + e + " fileExt:" + fileExt + " mimeType:" + mimeType);

      if (mimeType != null && mimeType.startsWith("image")) {
         shareVideoView.setVisibility(View.GONE);
         shareImageView.setVisibility(View.VISIBLE);

         loaderProgressBar.setVisibility(View.VISIBLE);
         UrlImageViewHelper.setUrlDrawable(shareImageView, e.getUrlString(), new UrlImageViewCallback() {
            @Override
            public void onLoaded(ImageView imageView, Drawable loadedDrawable, String url, boolean loadedFromCache) {
               loaderProgressBar.setVisibility(View.INVISIBLE);
            }
         });
      } else if (mimeType != null && mimeType.startsWith("video")) {
         shareVideoView.setVisibility(View.VISIBLE);
         shareImageView.setVisibility(View.GONE);

         loaderProgressBar.setVisibility(View.VISIBLE);
         shareVideoView.setVideoURI(Uri.parse(e.getUrlString()));
         shareVideoView.setMediaController(new MediaController(this));
         shareVideoView.requestFocus();
         shareVideoView.start();
         shareVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
               loaderProgressBar.setVisibility(View.INVISIBLE);               
            }
         });
      } else if (mimeType != null) {
         Toast.makeText(this, "Sorry, unknown/unsupported file type:" + mimeType, Toast.LENGTH_LONG).show();
      } else {
         Log.w(App.TAG, "NULL MIME TYPE IN DISPLAY MEDIA urlString:" + e.getUrlString());
      }
   }

   //
   // private util
   //

   /*
   private void setFullscreen(boolean fullscreen) {

   }
   */
}
