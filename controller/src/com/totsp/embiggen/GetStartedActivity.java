package com.totsp.embiggen;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

public class GetStartedActivity extends BaseFragmentActivity {

   // TODO need a forgetCurrentHost path once in app (bring users back here?)
   
   // TODO make sure host is discarded if it goes away, poll/check every so often?
   
   // TODO start scan initially from here, not app 

   private Button scan;
   private Button forgetCurrentHost;
   private Button useCurrentHost;
   private TextView version;
   private Timer timer;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_started);

      ActionBar actionBar = getSupportActionBar();
      actionBar.hide();

      version = (TextView) findViewById(R.id.get_started_version);
      try {
         PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
         version.setText(pi.versionName);
      } catch (NameNotFoundException e) {
         // ignore
      }

      scan = (Button) findViewById(R.id.get_started_scan);
      scan.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            startScan();
         }
      });

      forgetCurrentHost = (Button) findViewById(R.id.get_started_forget_current_host);
      forgetCurrentHost.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            app.getMessageClientService().restartClient(); 
            updateViews(true);
         }
      });

      useCurrentHost = (Button) findViewById(R.id.get_started_use_current_host);
      useCurrentHost.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            startActivity(new Intent(GetStartedActivity.this, MainActivity.class));
         }
      });      
   }

   @Override
   protected void onPostResume() {
      super.onPostResume();
      if (timer != null) {
         timer.purge();
         timer.cancel();
      }
      timer = new Timer();
      updateViews(false);
   }

   private void updateViews(boolean restart) {

      // based on state we update views, if we have found a host already, show it and allow RESCAN
      // if we have not found a host, show scan 

      InetSocketAddress host = null;

      if (!restart && app.getMessageClientService() != null) {
         host = app.getMessageClientService().getHostInetSocketAddress();
      }

      if (host != null) {
         scan.setVisibility(View.GONE);
         forgetCurrentHost.setVisibility(View.VISIBLE);
         useCurrentHost.setVisibility(View.VISIBLE);
      } else {
         scan.setVisibility(View.VISIBLE);
         forgetCurrentHost.setVisibility(View.GONE);
         useCurrentHost.setVisibility(View.GONE);
      }
   }

   @Override
   protected void onResume() {
      super.onResume();
   }

   @Override
   protected void onPause() {
      super.onPause();
      if (timer != null) {
         timer.purge();
         timer.cancel();
      }
   }

   protected String getViewName() {
      return "GetStartedActivity";
   }

   @Override
   public boolean onTouchEvent(MotionEvent event) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
         //proceed();
      }
      return super.onTouchEvent(event);
   }

   private void startScan() {

      // TODO this scanning stuff is a convoluted mess, clena up, and use the bus
      
      InetSocketAddress host = null;
      if (app.getMessageClientService() != null) {
         host = app.getMessageClientService().getHostInetSocketAddress();
      }

      if (host != null) {
         Toast.makeText(this, "Found Embiggen host:" + host.getHostName(), Toast.LENGTH_LONG).show();
         startActivity(new Intent(GetStartedActivity.this, MainActivity.class));
      } else {
         // timer, wait on scan for X, display found/not found         

         final ProgressDialog pd = new ProgressDialog(this);

         final TimerTask scanTask = new TimerTask() {
            @Override
            public void run() {
               GetStartedActivity.this.runOnUiThread(new Runnable() {
                  public void run() {

                     InetSocketAddress host = null;

                     if (app.getMessageClientService() != null) {
                        host = app.getMessageClientService().getHostInetSocketAddress();
                     }
                     if (host != null) {
                        pd.dismiss();
                        Toast.makeText(GetStartedActivity.this, "Found Embiggen host:" + host.getHostName(),
                                 Toast.LENGTH_LONG).show();
                        startActivity(new Intent(GetStartedActivity.this, MainActivity.class));
                        cancel();
                     }
                  }
               });
            }
         };

         pd.setTitle("Scanning for Embiggen host");
         pd.setMessage("checking the network for a host (hang on a few seconds)");
         pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         pd.setCancelable(true);
         pd.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface arg) {
               scanTask.cancel();
            }
         });
         pd.show();

         timer.scheduleAtFixedRate(scanTask, 100, 1000);
      }
   }
}
