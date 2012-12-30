package com.totsp.embiggen;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.totsp.android.util.NetworkUtil;
import com.totsp.embiggen.component.ImageAdapter;
import com.totsp.embiggen.component.ImageLoaderTask;
import com.totsp.server.HTTPServerService;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * Gallery widget Activity that loads images from MediaStore. 
 * 
 * TODO - lame gallery/widget chooser here, rewrite UI.
 * 
 * @author ccollins
 *
 */
public class GalleryChooserPhotos extends BaseActivity {

   private Cursor cursor;
   private ImageLoaderTask loader;
   protected OnTouchListener onTouchListener;

   private Gallery gallery;
   private ImageView largePreview;
   private ImageView thumb;

   private LinearLayout controls;

   private Animation anim;

   ///private Bitmap mainBitmap;
   private Drawable borderOn;
   private Drawable borderOff;

   private View prevSelectedView;

   @Override
   public void onCreate(Bundle icicle) {
      super.onCreate(icicle);

      setContentView(R.layout.gallery_chooser);

      borderOn = getResources().getDrawable(R.drawable.border_on);
      borderOff = getResources().getDrawable(R.drawable.border_off);

      controls = (LinearLayout) findViewById(R.id.gallery_chooser_controls);
      controls.setVisibility(View.GONE);

      largePreview = (ImageView) findViewById(R.id.gallery_chooser_image);

      thumb = (ImageView) findViewById(R.id.gallery_chooser_thumb);
      gallery = (Gallery) findViewById(R.id.gallery_chooser_gallery);

      gallery.setCallbackDuringFling(false);
      gallery.setOnItemSelectedListener(new OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

            if (thumb.isShown()) {
               thumb.setVisibility(View.GONE);
            }

            if (loader != null && loader.getStatus() != ImageLoaderTask.Status.FINISHED) {
               loader.cancel();
            }

            if (id != -1) {
               if (prevSelectedView != null) {
                  prevSelectedView.setBackgroundDrawable(borderOff);
               }
               prevSelectedView = v;
               v.setBackgroundDrawable(borderOn);

               Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
               Log.v(App.TAG, "onItemSelected uri:" + uri);
               loader = (ImageLoaderTask) new ImageLoaderTask(largePreview, getApplicationContext(), true).execute(uri);
            }
         }

         @Override
         public void onNothingSelected(AdapterView<?> parent) {
         }
      });

      Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
      String[] projection = { MediaStore.Images.Media._ID };
      String selectedBucketName = getIntent().getStringExtra(BucketList.BUCKET_NAME);
      String projectionColumn = getIntent().getStringExtra(BucketList.BUCKET_SORT_BY);

      //Log.d(App.TAG, "using projectionColumn:" + projectionColumn);
      //Log.d(App.TAG, "using selectedBucketName:" + selectedBucketName);

      String selection = null;
      String[] selectionArgs = null;

      if (projectionColumn != null && selectedBucketName != null) {

         // when using dates we need to convert from display string back to range we can use in query
         if (projectionColumn.startsWith("date")) {
            if (selectedBucketName.equalsIgnoreCase("No Date Available")) {
               selection = projectionColumn + " = -1";
               selectionArgs = new String[0];
            } else {
               // convert date format to date range for selection query                        
               try {
                  Date date = BucketList.DATE_FORMAT.parse(selectedBucketName);
                  Calendar cal = Calendar.getInstance();
                  cal.setTime(date);
                  long startMonthStamp = cal.getTimeInMillis();
                  cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                  long endMonthStamp = cal.getTimeInMillis();
                  selection = projectionColumn + " >= ? AND " + projectionColumn + " < ?";
                  selectionArgs = new String[2];
                  selectionArgs[0] = String.valueOf(startMonthStamp);
                  selectionArgs[1] = String.valueOf(endMonthStamp);
               } catch (ParseException e) {
                  // ignore, leave selection null
                  Log.e(App.TAG, "Unable to parse selectedBucketName into date:" + selectedBucketName
                           + " (no query will be used, all content returned)");
               }
            }
         } else {
            selection = projectionColumn + " = ?";
            selectionArgs = new String[1];
            selectionArgs[0] = selectedBucketName;
         }
      }

      // TODO get all the db stuff off the main thread
      if (selection != null) {
         cursor = managedQuery(uri, projection, selection, selectionArgs, MediaStore.Images.Media._ID + " DESC");
      } else {
         cursor = managedQuery(uri, projection, null, null, MediaStore.Images.Media._ID + " DESC");
      }
      if (cursor != null) {
         cursor.moveToFirst();
         ImageAdapter adapter = new ImageAdapter(this, cursor, true);
         if (adapter.getCount() == 0) {
            Toast.makeText(this, getString(R.string.message_no_content), Toast.LENGTH_LONG).show();
            finish();
            return;
         }
         gallery.setAdapter(adapter);
      } else {
         Toast.makeText(GalleryChooserPhotos.this, getString(R.string.message_gallery_empty), Toast.LENGTH_LONG).show();
      }

      anim = AnimationUtils.loadAnimation(this, R.anim.swipe_up);

      // gesture detection
      final GestureDetector gestureDetector = new GestureDetector(new SwipeGestureDetector());
      onTouchListener = new OnTouchListener() {
         @Override
         public boolean onTouch(View v, MotionEvent event) {
            if (gestureDetector.onTouchEvent(event)) {
               return true;
            }
            return false;
         }
      };
      largePreview.setOnTouchListener(onTouchListener);
   }

   @Override
   protected void onResume() {
      super.onResume();
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
      if (loader != null && loader.getStatus() != ImageLoaderTask.Status.FINISHED) {
         loader.cancel(true);
         loader = null;
      }
   }

   protected String getViewName() {
      return "GalleryChooserPhotos";
   }

   //
   // helpers
   //

   private void sendChoice(long imageId) {
      Log.d(App.TAG, "sendChoice imageId:" + imageId);

      String filePath = getPath(imageId);

      // replace spaces so that URL end is encoded (don't encode entire thing though)
      if (filePath.contains(" ")) {
         filePath = filePath.replace(" ", "+");
      }

      String wifiIpAddress = NetworkUtil.getWifiIpAddress((WifiManager) getSystemService(Context.WIFI_SERVICE));

      String url = "http://" + wifiIpAddress + ":" + HTTPServerService.PORT + filePath;

      Log.d(App.TAG, "sendChoice filePath:" + filePath);
      Log.d(App.TAG, "sendChoice serving URL:" + url);

      // TODO anymore send choice here

      //app.getClient().sendToHosts("displayMedia", data);
   }

   private String getPath(long imageId) {

      String projection = MediaStore.Images.Media.DATA;
      String selection = MediaStore.Images.Media._ID + "=" + imageId;

      Cursor cursor =
               managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] { projection }, selection, null,
                        null);
      String filePath = null;
      if (cursor != null) {
         cursor.moveToFirst();
         int columnIndex = cursor.getColumnIndex(projection);
         if (columnIndex != -1) {
            filePath = cursor.getString(columnIndex);
         }
      }
      return filePath;
   }

   // TODO messaging FROM server
   /*
      @Override
      public void onMessage(IMCUser sender, final String messageId, final MCData messageData, String target) {
         super.onMessage(sender, messageId, messageData, target);
         // must run back on UI thread, since MOVLConnect client makes network calls off of main thread
         this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
               if (messageId.equals("onDisplayMedia")) {
                  //Log.i(App.TAG, "onDisplayMedia:" + messageData);
                  if (messageData.containsKey("result") && messageData.getInt("result") == 0) {
                     thumb.setImageDrawable(getResources().getDrawable(R.drawable.thumbs_up));
                     thumb.setVisibility(View.VISIBLE);
                  } else {
                     Log.e(App.TAG, "ERROR display media failed (see data for reason)");
                     thumb.setImageDrawable(getResources().getDrawable(R.drawable.thumbs_down));
                     thumb.setVisibility(View.VISIBLE);
                  }
               }
            }
         });
      }
      */

   //
   // addtl classes
   //

   // gesture detector
   private class SwipeGestureDetector extends SimpleOnGestureListener {

      DisplayMetrics dm = getResources().getDisplayMetrics();

      int REL_SWIPE_MIN_DISTANCE = (int) (50 * dm.densityDpi / 160.0f); // distance
      int REL_SWIPE_MAX_OFF_PATH = (int) (50 * dm.densityDpi / 160.0f); // off path
      int REL_SWIPE_THRESHOLD_VELOCITY = (int) (50 * dm.densityDpi / 160.0f); // velocity

      @Override
      public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

         final float leftRight = e1.getX() - e2.getX();
         final float upDown = e1.getY() - e2.getY();

         try {

            // make sure path is straight is enough
            if (leftRight > REL_SWIPE_MAX_OFF_PATH) {
               return false;
            }

            // check velocity is enough
            if (Math.abs(velocityY) < REL_SWIPE_THRESHOLD_VELOCITY) {
               return false;
            }

            // look for down->up swipes
            if (upDown > REL_SWIPE_MIN_DISTANCE) {

               //Uri selectedViewUri = (Uri) gallery.getSelectedView().getTag();
               long position = gallery.getSelectedItemPosition();

               // TODO don't go back to cursor (and don't use UI thread)
               cursor.moveToPosition((int) position);
               int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
               if (columnIndex != -1) {
                  long imageId = cursor.getLong(columnIndex);
                  sendChoice(imageId);
                  largePreview.startAnimation(anim);
               }

               return true;
            }

            return false;
         } catch (Exception e) {
            Log.e(App.TAG, "ERROR processing swipe:" + e.getMessage(), e);
            return false;
         }
      }

      @Override
      public boolean onDown(MotionEvent e) {
         return true;
      }

      @Override
      public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
         return super.onScroll(e1, e2, distanceX, distanceY);
      }
   }
}
