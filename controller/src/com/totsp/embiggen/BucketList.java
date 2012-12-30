package com.totsp.embiggen;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * ListView that shows photo "buckets." 
 * 
 * @author ccollins
 *
 */
public class BucketList extends BaseActivity {

   public static final String DATE_BUCKET_FORMAT = "MMMM yyyy";
   // not thread safe, but we don't have multiple (or not many) threads using this anyway
   public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_BUCKET_FORMAT);

   public static final String BUCKET_LIST_TYPE = "blt";
   public static final String BUCKET_NAME = "bn";
   public static final String BUCKET_SORT_BY = "bsb";
   public static final int PHOTOS = 1;
   public static final int VIDEOS = 2;

   // we have THREE possible "galleryBucketSortType" values from prefs
   // album_name, date_taken, date_added
   // these correlate to three values in the projection
   private String sortType;

   private ListView list;

   @Override
   public void onCreate(Bundle icicle) {
      super.onCreate(icicle);

      setContentView(R.layout.bucket_list);

      sortType = app.getPrefs().getString("galleryBucketSortType", "album_name");

      list = (ListView) findViewById(R.id.bucket_list_list);

      // switch paths based on photos or videos (get type from intent)
      Uri uri = null;
      String[] projection = null;
      String projectionColumn = null;
      final int type = getIntent().getIntExtra(BUCKET_LIST_TYPE, PHOTOS);
      if (type == PHOTOS) {
         projectionColumn = MediaStore.Images.Media.BUCKET_DISPLAY_NAME;
         if (sortType.equalsIgnoreCase("date_taken")) {
            projectionColumn = MediaStore.Images.Media.DATE_TAKEN;
         } else if (sortType.equalsIgnoreCase("date_added")) {
            projectionColumn = MediaStore.Images.Media.DATE_ADDED;
         }
         projection = new String[] { MediaStore.Images.Media._ID, projectionColumn };
         uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
      } else if (type == VIDEOS) {
         projectionColumn = MediaStore.Video.Media.BUCKET_DISPLAY_NAME;
         if (sortType.equalsIgnoreCase("date_taken")) {
            projectionColumn = MediaStore.Video.Media.DATE_TAKEN;
         } else if (sortType.equalsIgnoreCase("date_added")) {
            projectionColumn = MediaStore.Video.Media.DATE_ADDED;
         }
         projection = new String[] { MediaStore.Video.Media._ID, projectionColumn };
         uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
      } else {
         throw new IllegalStateException("ERROR, unknown bucket_list_type:" + type);
      }

      Cursor cursor = managedQuery(uri, projection, null, null, null);

      // lame hack to filter down to DISTINCT values from ContentProvider (tried adding to projection, and group by, didn't work)
      // TODO run all this crap OUTSIDE of UI thread (and indicate to user that we're building the list)
      // (especially important since this cursor will iterate over EVERY image, no way to get distinct!?!)
      List<String> listNames = new ArrayList<String>();
      if (cursor != null && cursor.moveToFirst()) {
         int nameCol = cursor.getColumnIndex(projectionColumn);
         do {
            String name = cursor.getString(nameCol);
            if (!listNames.contains(name)) {
               listNames.add(name);
            }
         } while (cursor.moveToNext());
         cursor.requery();
      }

      // TODO there are many better/faster ways to filter this, another hack
      List<String> listNamesFiltered = new ArrayList<String>();
      for (String name : listNames) {
         if (projectionColumn.startsWith("date")) {
            // convert dates to readable format
            if (name.equals("-1")) {
               name = "No Date Available";
            } else {
               try {
                  Long dateLong = Long.valueOf(name);
                  Date date = new Date(dateLong);
                  name = DateFormat.format(DATE_BUCKET_FORMAT, date).toString();
               } catch (NumberFormatException e) {
                  name = "Unable to Parse Date";
               }
            }
         }

         if (!listNamesFiltered.contains(name)) {
            listNamesFiltered.add(name);
         }
      }

      // sort by "MMMM yyyy" 
      Collections.sort(listNamesFiltered, new DateStringComparator());

      String[] bucketNames = new String[listNamesFiltered.size()];
      int count = 0;
      for (String name : listNamesFiltered) {
         bucketNames[count] = name;
         count++;
      }

      // could use this simple adapter IF we could make managedCursor distinct
      ///ListAdapter adapter =
      ///         new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor,
      ///                  new String[] { MediaStore.Images.Media.BUCKET_DISPLAY_NAME }, new int[] { android.R.id.text1 });      

      final String projectionColumnFinal = projectionColumn;
      list.setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item_1, bucketNames));

      list.setOnItemClickListener(new OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String chosenBucketName = ((TextView) view).getText().toString();
            ///Toast.makeText(BucketList.this, "bucket:" + chosenBucketName, Toast.LENGTH_SHORT).show();

            // start GalleryChooserX with bucket name passed
            Intent intent = null;
            if (type == PHOTOS) {
               intent = new Intent(BucketList.this, GalleryChooserPhotos.class);
            } else if (type == VIDEOS) {
               intent = new Intent(BucketList.this, GalleryChooserVideos.class);
            }
            intent.putExtra(BUCKET_NAME, chosenBucketName);
            intent.putExtra(BUCKET_SORT_BY, projectionColumnFinal);
            startActivity(intent);
            finish();
         }
      });

      TextView empty = new TextView(this);
      //empty.setTextColor(R.color.blue_dark);
      empty.setText(R.string.message_no_results);
      empty.setVisibility(View.GONE);
      list.setEmptyView(empty);
      ((ViewGroup) list.getParent()).addView(empty);
   }

   @Override
   protected void onResume() {
      super.onResume();
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
   }

   protected String getViewName() {
      return "AlbumList";
   }

   //
   // MOVL messaging
   //

   //
   // helpers
   //

   /**
    * Reverse of code in MediaProvider.computeBucketValues.
    */
   // bucket ids are hashcodes of lower case path/bucket name
   public static long getBucketId(String bucketName) {
      return Long.valueOf(bucketName.hashCode());
   }

   class DateStringComparator implements Comparator<String> {
      @Override
      public int compare(String object1, String object2) {

         Date date1 = null;
         Date date2 = null;

         try {
            date1 = DATE_FORMAT.parse(object1);
         } catch (ParseException e) {
         }

         try {
            date2 = DATE_FORMAT.parse(object2);
         } catch (ParseException e) {
         }

         if (date1 == null) {
            return -1;
         } else if (date2 == null) {
            return 1;
         } else {
            if (date1.before(date2)) {
               return 1;
            }
            return -1;
         }         
      }
   }
}
