package com.totsp.embiggen.component;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.totsp.embiggen.App;
import com.totsp.embiggen.R;

public class ImageAdapter extends BaseAdapter {

   private static String PHOTO_ID = MediaStore.Images.Media._ID;
   private static String VIDEO_ID = MediaStore.Video.Media._ID;

   private Context context;
   private Cursor cursor;
   private boolean isPhoto;
   private BitmapFactory.Options opts;

   private Drawable defaultDrawable;
   
   private LayoutInflater inflater;

   public ImageAdapter(Context context, Cursor cursor, boolean isPhoto) {
      this.context = context;
      this.cursor = cursor;
      this.isPhoto = isPhoto;
      this.opts = new BitmapFactory.Options();
      opts.inSampleSize = 2;
      this.defaultDrawable = context.getResources().getDrawable(android.R.drawable.ic_menu_gallery);
      this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
   }

   public int getCount() {
      return cursor.getCount();
   }

   public Object getItem(int position) {
      return null;
   }

   public long getItemId(int position) {
      
      if (cursor == null || cursor.isClosed()) {
         return -1;
      }
      
      cursor.moveToPosition(position);
      int columnIndex = -1;
      if (isPhoto) {
         columnIndex = cursor.getColumnIndex(PHOTO_ID);
      } else {
         columnIndex = cursor.getColumnIndex(VIDEO_ID);
      }
      if (columnIndex != -1) {
         long mediaId = cursor.getLong(columnIndex);
         return mediaId;
      }
      return -1;
   }

   public View getView(int position, View convertView, ViewGroup parent) {
      ///Log.d(App.TAG, "getView POSITION:" + position);
      
      ImageView view = null;

      if (convertView == null) {         
         view = (ImageView) inflater.inflate(R.layout.gallery_item, parent, false);
         view.setImageDrawable(context.getResources().getDrawable(android.R.drawable.ic_menu_gallery));
         //view.setScaleType(ImageView.ScaleType.FIT_CENTER);
         //view.setScaleType(ImageView.ScaleType.CENTER_CROP);
         //view.setPadding(4, 4, 4, 4);
         //view.setLayoutParams(new Gallery.LayoutParams(100, 75));
         //view.setAdjustViewBounds(true);
      } else {
         view = (ImageView) convertView;
      }

      long mediaId = getItemId(position);
      if (mediaId != -1) {
         view.setTag(mediaId);
         new LoadTask(mediaId, view, isPhoto).execute();
      } else {
         Log.w(App.TAG, "getView imageId == -1, cannot populate");
      }

      return view;
   }

   class LoadTask extends AsyncTask<Void, Void, Bitmap> {

      private long mediaId;
      private ImageView updateView;
      private boolean isPhoto;

      public LoadTask(long mediaId, ImageView updateView, boolean isPhoto) {
         this.mediaId = mediaId;
         this.updateView = updateView;
         this.isPhoto = isPhoto;
      }      
      
      @Override
      protected void onPreExecute() {         
         super.onPreExecute();
         updateView.setImageDrawable(defaultDrawable);
      }

      protected Bitmap doInBackground(Void... args) {
         if (isCancelled()) {
            return null;
         }
         return getThumb(mediaId);
      }

      @Override
      protected void onPostExecute(Bitmap b) {
         
         ///Log.d(App.TAG, "*** onPostExecute mediaId:" + mediaId);
         
         if (b == null) {
            ///Log.i(App.TAG, "ImageLoader onPostExecute bitmap NULL, largePreview set empty");
            updateView.setImageDrawable(context.getResources().getDrawable(android.R.drawable.ic_menu_gallery));
            updateView.invalidate();
            return;
         }

         if (!isCancelled()) {
            if ((Long) updateView.getTag() == mediaId) {
               ///Log.d(App.TAG, "*** doing update for mediaId");
               ///updateView.setImageBitmap(ImageHelper.getRoundedCornerBitmap(b, 16));               
               updateView.setImageBitmap(b);               
            } else {
               ///Log.d(App.TAG, "*** ignoring update for mediaId, tag does not match");
            }            
         } else {
            b.recycle();
         }
      }

      public void cancel() {
         super.cancel(true);
      }

      private Bitmap getThumb(long mediaId) {
         if (isPhoto) {
            return getPhotoThumb(mediaId);
         } else {
            return getVideoThumb(mediaId);
         }
      }

      private Bitmap getPhotoThumb(long mediaId) {
         return MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), mediaId,
                  MediaStore.Images.Thumbnails.MINI_KIND, opts);
      }

      private Bitmap getVideoThumb(long mediaId) {
         return MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(), mediaId,
                  MediaStore.Video.Thumbnails.MINI_KIND, opts);
      }
   }
}
