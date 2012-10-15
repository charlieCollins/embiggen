package com.totsp.embiggen;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class GetStarted extends BaseActivity {

   public static final int SPLASH_TIMEOUT = 10000;
   
   private Button button;
   private TextView version;
   private Timer timer;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_started);
      
      version = (TextView) findViewById(R.id.get_started_version); 
      try {
         PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
         version.setText(pi.versionName);
      } catch (NameNotFoundException e) {
         // ignore
      }
      
      button = (Button) findViewById(R.id.get_started_button);
      button.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            proceed();          
         }         
      });
      
      timer = new Timer();
      timer.schedule(new TimerTask() {
         @Override
         public void run() {
            proceed();
         }
      }, SPLASH_TIMEOUT);
   }

   @Override
   protected void onResume() {
      super.onResume();
   }

   @Override
   protected void onPause() {
      super.onPause();   
      if (timer != null) {
         timer.cancel();
      }
   }
   
   protected String getActivityId() {
      return "GetStarted";
   }

   @Override
   public boolean onTouchEvent(MotionEvent event) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {         
         proceed();
      }
      return super.onTouchEvent(event);
   }   

   private void proceed() {   
      timer.cancel();
      startActivity(new Intent(GetStarted.this, Login.class));
      //startActivity(new Intent(GetStarted.this, LaunchChooser.class));
   }
}
