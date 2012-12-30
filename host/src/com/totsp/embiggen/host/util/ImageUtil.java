package com.totsp.embiggen.host.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.util.Log;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.totsp.embiggen.host.App;
import com.totsp.embiggen.host.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public final class ImageUtil {

   // TODO validate and review this, consider using built in cache file locations after X memory is used? 
   // also move to MOVL utils if we think this is useful

   // argh, want to make the cache static, but need resources, needs more work
   
   private final LoadingCache<String, Bitmap> URL_BITMAP_CACHE = CacheBuilder.newBuilder().maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<String, Bitmap>() {
               public Bitmap load(String urlString) {
                  return downloadBitmap(urlString);
               }
            });

   private Context context;

   public ImageUtil(Context context) {
      this.context = context;
   }

   public Bitmap get(String url) {
      return URL_BITMAP_CACHE.getUnchecked(url);
   }  

   // does NOT throw exceptions, so cache users should getUnchecked
   private Bitmap downloadBitmap(String urlString) {
      // TODO should we recreate the client every time, or will newInstance handle not actually giving us a new instance?
      final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
      final HttpGet getRequest = new HttpGet(urlString);

      try {
         HttpResponse response = client.execute(getRequest);
         final int statusCode = response.getStatusLine().getStatusCode();
         if (statusCode != HttpStatus.SC_OK) {
            Log.w(App.TAG, "Error " + statusCode + " while retrieving bitmap from " + urlString);
         }

         final HttpEntity entity = response.getEntity();
         if (entity != null) {
            InputStream inputStream = null;
            try {
               inputStream = entity.getContent();
               // TODO decode with inJustDecodeBounds first, to get sizes, then get correct inSampleSize
               BitmapFactory.Options options = new BitmapFactory.Options();
               options.inSampleSize = 8;
               final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
               return bitmap;
            } finally {
               if (inputStream != null) {
                  inputStream.close();
               }
               entity.consumeContent();
            }
         }
      } catch (Exception e) {
         getRequest.abort();
         Log.w(App.TAG, "Error while retrieving bitmap from " + urlString, e);
      } finally {
         if (client != null) {
            client.close();
         }
      }
      // default, if all else fails, return the missing image image
      return BitmapFactory.decodeResource(context.getResources(), R.drawable.missing_image);
   }
   
   /*
   private Bitmap decodeStream(InputStream is) {
      Bitmap b = null;

      //Decode image size
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inJustDecodeBounds = true;

      BitmapFactory.decodeStream(is, null, o);
      //fis.close();

      int scale = 1;
      if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
         scale =
                  (int) Math.pow(
                           2,
                           (int) Math.round(Math.log(IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth))
                                    / Math.log(0.5)));
      }

      //Decode with inSampleSize
      BitmapFactory.Options o2 = new BitmapFactory.Options();
      o2.inSampleSize = scale;
      b = BitmapFactory.decodeStream(is, null, o2);

      return b;
   }
   */
}