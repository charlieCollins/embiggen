package com.totsp.embiggen.host;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public abstract class BaseActivity extends Activity {

   public static final String ANALYTICS_ACCOUNT_ID = "TODO";
   public static final String DO_NOT_TRACK_ACTIVITY_ID = "DO_NOT_TRACK";

   private static final int OPTIONS_MENU_LOGOUT = 0;

   protected App app;

   protected InputMethodManager imm;
   protected ProgressDialog dialog;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      requestWindowFeature(Window.FEATURE_NO_TITLE);

      app = (App) this.getApplication();

      dialog = new ProgressDialog(this);
      dialog.setCancelable(false);
      dialog.setMessage("");

      imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
   }

   @Override
   protected void onResume() {
      super.onResume();
      app.getBus().register(this);
   }

   protected abstract String getViewName();

   @Override
   protected void onPause() {
      super.onPause();
      if (dialog != null) {
         dialog.dismiss();
      }
      app.getBus().unregister(this);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      /*
      if (app.getClient().isConnected()) {
         menu.add(0, BaseActivity.OPTIONS_MENU_LOGOUT, 0, "Leave/Logout").setIcon(
                  android.R.drawable.ic_menu_close_clear_cancel);
      } 
      */
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case OPTIONS_MENU_LOGOUT:
            //app.getClient().leaveRoom();
            break;
      }
      return false;
   }

   protected void backToStart(boolean showToast, boolean finish) {
      if (showToast) {
         Toast.makeText(this, "Left room, going back to Start", Toast.LENGTH_SHORT).show();
      }
      Log.i(App.TAG, "backToStart invoked, navigating to main Activity");
      Intent intent = new Intent(Intent.ACTION_MAIN);
      intent.addCategory(Intent.CATEGORY_LAUNCHER);
      startActivity(intent);
      if (finish) {
         finish();
      }
   }
}
