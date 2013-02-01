package com.totsp.embiggen;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public abstract class BaseFragmentActivity extends SherlockFragmentActivity {

   protected App app;
   protected ActionBar actionBar;
   protected FragmentManager fragmentManager;
   protected ProgressDialog progressDialog;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      ///Log.v(App.TAG, "BaseFragmentActivity onCreate() " + this.getClass().getSimpleName());
      this.app = (App) this.getApplication();

      this.actionBar = this.getSupportActionBar();
      this.actionBar.setDisplayShowTitleEnabled(false);
      this.actionBar.setLogo(R.drawable.logo);

      this.fragmentManager = this.getSupportFragmentManager();

      this.progressDialog = new ProgressDialog(this);
      this.progressDialog.setCancelable(false);
      this.progressDialog.setMessage("");
   }

   @Override
   protected void onResume() {
      super.onResume();
      this.app.bus.register(this);
      ///Log.v(App.TAG, "BaseFragmentActivity onPause() " + this.getClass().getSimpleName());
   }

   @Override
   protected void onPause() {
      super.onPause();
      ///Log.v(App.TAG, "BaseFragmentActivity onPause() " + this.getClass().getSimpleName());
      if (progressDialog != null) {
         progressDialog.dismiss();
      }
      this.app.bus.unregister(this);
   }

   @Override
   protected void onStart() {
      super.onStart();

      // ga
      app.gaTrackView(getViewName());
   }

   @Override
   protected void onStop() {
      super.onStop();
   }

   // used for google analytics
   protected abstract String getViewName();

   protected boolean isEmpty(TextView t) {
      if ((t == null) || (t.getText() == null)) {
         return true;
      }
      if (t.getText().toString().trim().equals("")) {
         return true;
      }
      return false;
   }
}
