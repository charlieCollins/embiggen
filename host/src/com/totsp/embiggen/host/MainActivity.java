package com.totsp.embiggen.host;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewCallback;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.squareup.otto.Subscribe;
import com.totsp.embiggen.host.event.DisplayMediaEvent;

/**
 * The Embiggen host.
 *
 */
final public class MainActivity extends BaseActivity {

   // TODO improve UI/UX
   // TODO help/about
   // TODO preferences for quality settings, etc?

   private ImageView logoImageView;
   private ProgressBar loaderProgressBar;
   private ImageView shareImageView;
   private VideoView shareVideoView;

   // TODO network state receiver in App

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      logoImageView = (ImageView) findViewById(R.id.logo_image_view);
      loaderProgressBar = (ProgressBar) findViewById(R.id.footer_progress_bar);
      shareImageView = (ImageView) findViewById(R.id.share_image_view);
      shareVideoView = (VideoView) findViewById(R.id.share_video_view);
   }

   @Override
   protected String getViewName() {
      return "HostMain";
   }

   //
   // subscriptions
   //

   // TODO add displayMediaSlideShow or such (allow slideshows)

   @Subscribe
   public void displayMedia(DisplayMediaEvent e) {
      Log.d(App.TAG, "MainActivity caught display media event:" + e);

      logoImageView.setVisibility(View.GONE);

      // TODO determine if video or image, etc (now assume image)      
      shareVideoView.setVisibility(View.GONE);
      shareImageView.setVisibility(View.VISIBLE);

      loaderProgressBar.setVisibility(View.VISIBLE);
      UrlImageViewHelper.setUrlDrawable(shareImageView, e.getUrlString(), new UrlImageViewCallback() {
         @Override
         public void onLoaded(ImageView imageView, Drawable loadedDrawable, String url, boolean loadedFromCache) {
            loaderProgressBar.setVisibility(View.INVISIBLE);
         }
      });
   }

   //
   // private util
   //

   private void setFullscreen(boolean fullscreen) {

   }

   //
   // tasks
   //

   /*
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
   */
}
