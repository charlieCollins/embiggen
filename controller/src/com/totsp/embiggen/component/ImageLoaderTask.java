package com.totsp.embiggen.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import com.totsp.embiggen.App;

import java.io.IOException;
import java.io.InputStream;

public class ImageLoaderTask extends AsyncTask<Uri, Void, Bitmap> {

   private static final int SAMPLE_SIZE = 4;
   
   private BitmapFactory.Options opts;
   private ImageView modifyView;
   private Context context;
   private boolean isPhoto;

   public ImageLoaderTask(ImageView modifyView, Context context, boolean isPhoto) {
      
      this.modifyView = modifyView;
      this.context = context;
      this.isPhoto = isPhoto;

      opts = getOpts();
   }

   protected Bitmap doInBackground(Uri... args) {
      if (isCancelled()) {
         return null;
      }
      ///Log.d(App.TAG, "ImageLoader doInBackground uri:" + args[0]);
      Bitmap bitmap = loadBitmap(args[0], opts);
      return bitmap;
   }

   @Override
   protected void onPostExecute(Bitmap b) {
      if (b == null) {
         ///Log.i(App.TAG, "ImageLoader onPostExecute bitmap NULL, largePreview set empty");
         modifyView.setImageDrawable(context.getResources().getDrawable(android.R.drawable.ic_menu_gallery));
         modifyView.invalidate();
         return;
      }

      if (!isCancelled() && !opts.mCancel) {         
         if (isPhoto) {
            //modifyView.setImageBitmap(ImageHelper.getRoundedCornerBitmap(b, 12));
            modifyView.setImageBitmap(b);
         } else {
            //modifyView.setImageBitmap(ImageHelper.getRoundedCornerBitmap(b, 6));
            modifyView.setImageBitmap(b);
         }
      } else {
         b.recycle();
      }
   }

   public void cancel() {
      opts.requestCancelDecode();
      super.cancel(true);
   }

   private Bitmap loadBitmap(Uri uri, BitmapFactory.Options opts) {
      ///Log.d(App.TAG, "loadBitmap uri:" + uri);
      Bitmap bitmap = null;
      try {
         if (isPhoto) {
            bitmap = decodeStream(uri, opts);            
         } else {
            // for the video the only still image we have is the thumbnail
            bitmap =
                     MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(),
                              Long.valueOf(uri.getLastPathSegment()), MediaStore.Video.Thumbnails.MICRO_KIND, null);
         }
      } catch (OutOfMemoryError e) {
         Log.e(App.TAG, "OOME trying to loadBitmap in ImageLoaderTask:" + e.getMessage());
      } catch (RuntimeException e) {
         Log.e(App.TAG, "RTE trying to loadBitmap in ImageLoaderTask:" + e.getMessage());
      }
      return bitmap;
   }

   private Bitmap decodeStream(Uri uri, BitmapFactory.Options opts){
      Bitmap b = null;
      try {
         /*
          //Decode image size
          BitmapFactory.Options o = new BitmapFactory.Options();
          o.inJustDecodeBounds = true;

          InputStream is = context.getContentResolver().openInputStream(uri);
          BitmapFactory.decodeStream(is, null, o);
          is.close();

          int scale = 1;
          if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
              scale = (int)Math.pow(2, (int) Math.round(Math.log(IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
          }
          */

          InputStream is = context.getContentResolver().openInputStream(uri);
          b = BitmapFactory.decodeStream(is, null, opts);
          is.close();
      } catch (IOException e) {
         Log.e(App.TAG, "IOE trying to decodeStream in ImageLoaderTask:" + e.getMessage());
      }
      return b;
  }
   
   private static BitmapFactory.Options getOpts() {
      BitmapFactory.Options opts = new BitmapFactory.Options();
      opts = new BitmapFactory.Options();
      //opts.inDither = false;
      opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
      opts.inSampleSize = SAMPLE_SIZE;
      opts.inPurgeable = true;
      return opts;
   }
   
}