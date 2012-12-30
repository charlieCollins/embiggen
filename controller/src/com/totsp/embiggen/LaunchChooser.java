package com.totsp.embiggen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class LaunchChooser extends BaseActivity {

   private Button photo;
   private Button video;

   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.launch_chooser);

      photo = (Button) findViewById(R.id.launchchooser_button_photo);
      photo.setOnClickListener(new OnClickListener() {
         public void onClick(View arg0) {
            ///Intent intent = new Intent();
            ///intent.setType("image/*");
            ///intent.setAction(Intent.ACTION_PICK);            
            ///startActivityForResult(intent, SELECT_PHOTO);
            //startActivity(new Intent(LaunchChooser.this, GalleryChooserPhotos.class));
            Intent intent = new Intent(LaunchChooser.this, BucketList.class);
            intent.putExtra(BucketList.BUCKET_LIST_TYPE, BucketList.PHOTOS);
            startActivity(intent);
         }
      });

      video = (Button) findViewById(R.id.launchchooser_button_video);
      video.setOnClickListener(new OnClickListener() {
         public void onClick(View arg0) {
            //Intent intent = new Intent();
            //intent.setType("video/*");
            //intent.setAction(Intent.ACTION_PICK);
            //startActivityForResult(intent, SELECT_VIDEO);
            //startActivity(new Intent(LaunchChooser.this, GalleryChooserVideos.class));
            Intent intent = new Intent(LaunchChooser.this, BucketList.class);
            intent.putExtra(BucketList.BUCKET_LIST_TYPE, BucketList.VIDEOS);
            startActivity(intent);
         }
      });
   }
   
   protected String getViewName() {
      return "LaunchChooser";
   }

   /*
   public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (resultCode == RESULT_OK) {
         Uri selectedUri = data.getData();            
         if (requestCode == SELECT_PHOTO) {            
            Intent i = new Intent(this, ChosenPhoto.class);
            i.putExtra("chosenUri", selectedUri.toString());            
            startActivity(i);
         } else if (requestCode == SELECT_VIDEO) {
            Intent i = new Intent(this, ChosenVideo.class);
            i.putExtra("chosenUri", selectedUri.toString());            
            startActivity(i);
         } 
      }
   }
   */
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = this.getSupportMenuInflater();
      inflater.inflate(R.menu.chooser, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.menu_photos:
            break;
         case R.id.menu_videos:
            break;
         case R.id.menu_help:
            helpDialog.show();
            break;
         case R.id.menu_prefs:
            startActivity(new Intent(this, Preferences.class));
            break;         
         default:
            break;
      }
      return super.onOptionsItemSelected(item);
   }  
}
