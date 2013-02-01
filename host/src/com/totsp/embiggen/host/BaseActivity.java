package com.totsp.embiggen.host;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

public abstract class BaseActivity extends Activity {

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

   /*
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {      
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
   */
}
