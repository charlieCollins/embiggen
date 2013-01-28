package com.totsp.embiggen;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

public class MainActivity extends BaseFragmentActivity {

   private static final int TAB_POSITION_GALLERY = 0;
   private static final int TAB_POSITION_HELP = 1;
   private int currentTabPosition;

   private Fragment galleryFrag;

   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.main_activity);

      galleryFrag = new LaunchGalleryFragment();

      FragmentTransaction trans = this.fragmentManager.beginTransaction();
      trans.replace(R.id.media_gridview_fragment_container, galleryFrag);
      trans.commit();
   }

   protected String getViewName() {
      return "MainActivity-Controller";
   }
}
