package com.totsp.embiggen;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

public abstract class BaseActivity extends SherlockActivity {

   public static final String ANALYTICS_ACCOUNT_ID = "TODO";
   public static final String DO_NOT_TRACK_ACTIVITY_ID = "DO_NOT_TRACK";

   // TODO ActionBar via ActionBarSherlock (get rid of this menu crap)
   
   //private static final int OPTIONS_MENU_LOGOUT = 0;
   //private static final int OPTIONS_MENU_HELP = 1;
   //private static final int OPTIONS_MENU_PREFS = 2;
   
   protected App app;

   protected InputMethodManager imm;
   protected ProgressDialog dialog;
   protected Dialog helpDialog;
   
   protected Handler handler;
   
   protected ActionBar actionBar;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      
      //requestWindowFeature(Window.FEATURE_NO_TITLE);
      
      app = (App) this.getApplication();
      
      actionBar = this.getSupportActionBar(); 
      actionBar.setDisplayHomeAsUpEnabled(false);
      actionBar.setDisplayShowTitleEnabled(true);
      actionBar.setDisplayUseLogoEnabled(false);

      handler = new Handler();

      dialog = new ProgressDialog(this);
      dialog.setCancelable(false);
      dialog.setMessage("");
      
      LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      helpDialog = new Dialog(this);
      helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
      View helpLayout = inflater.inflate(R.layout.dialog_help, (ViewGroup) findViewById(R.id.dialog_help_root));
      helpDialog.setContentView(helpLayout);
      Button roomNotFoundOk = (Button) helpLayout.findViewById(R.id.dialog_help_done);
      roomNotFoundOk.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) { 
            if (helpDialog.isShowing()) {
               helpDialog.dismiss();
            }
         }         
      });

      imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);      
   }  

   @Override
   protected void onStart() {      
      super.onStart();      
   }

   @Override
   protected void onResume() {
      super.onResume();

      String activityId = getActivityId();
      if ((activityId != null) && !activityId.equals("") && !activityId.equals(DO_NOT_TRACK_ACTIVITY_ID)) {
         ///tracker.trackPageView("/" + getActivityId());
         new TrackPageViewTask().execute(getActivityId());
      }

      // this somehow interferes with android:windowSoftInputMode="adjustResize"
      ///getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

      //app.addListener(this);

      ///
      /*
      // if client is not connected, make sure we are on Get Started
      if (!app.getClient().isConnected()
               && (!(this.getClass().getSimpleName().equals("GetStarted") || this.getClass().getSimpleName()
                        .equals("Login")))) {
         Log.e(App.LOG_TAG, "ERROR, not connected, go to get started");
         Intent i = new Intent(this, GetStarted.class);

         startActivity(i);
         finish();
      } 
      // if client is connected, and current class is Login, go to LaunchChooser
      if (app.getClient().isConnected()
               && (this.getClass().getSimpleName().equals("GetStarted") || this.getClass().getSimpleName()
                        .equals("Login"))) {
         Log.i(App.LOG_TAG, "Forward to LaunchChooser (we're logged in (use menu to log out))");
         startActivity(new Intent(this, LaunchChooser.class));
      }
      */
      ///
   }

   protected abstract String getActivityId();

   @Override
   protected void onPause() {
      super.onPause();
      if (dialog != null) {
         dialog.dismiss();
      }
      if (helpDialog.isShowing()) {
         helpDialog.dismiss();
      }
      //app.removeListener(this);
   }

   /*
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
     
     

      menu.add(1, BaseActivity.OPTIONS_MENU_HELP, 1, "Help").setIcon(
               android.R.drawable.ic_menu_help);
      menu.add(2, BaseActivity.OPTIONS_MENU_PREFS, 2, "Prefs").setIcon(
               android.R.drawable.ic_menu_preferences);
               
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
     
      switch (item.getItemId()) {
         case OPTIONS_MENU_LOGOUT:
            //app.getClient().leaveRoom();            
            break;
         case OPTIONS_MENU_HELP:
            helpDialog.show();
            break;
         case OPTIONS_MENU_PREFS:
            startActivity(new Intent(this, Preferences.class));
            break;
      }
      
      return false;
   }
   */

   class TrackPageViewTask extends AsyncTask<String, Void, Void> {

      public TrackPageViewTask() {
      }

      @Override
      protected Void doInBackground(String... args) {
         // TODO
         //app.getTracker().trackPageView("/" + args[0]);
         return null;
      }

      @Override
      protected void onPostExecute(Void v) {
      }
   }

   

   

}
