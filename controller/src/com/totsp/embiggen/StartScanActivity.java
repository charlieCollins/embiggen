package com.totsp.embiggen;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
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

public class StartScanActivity extends BaseFragmentActivity {

   // TODO make sure host is discarded if it goes away, poll/check every so often?

   private Button scan;
   private Button forgetCurrentHost;
   private Button useCurrentHost;
   private TextView version;
   private Timer timer;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.startscan_activity);

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
            app.getBroadcastClientService().startClient();
            startScan();
         }
      });

      forgetCurrentHost = (Button) findViewById(R.id.get_started_forget_current_host);
      forgetCurrentHost.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            app.getBroadcastClientService().clearHostHttpServerInfo();
            updateViews();
         }
      });

      useCurrentHost = (Button) findViewById(R.id.get_started_use_current_host);
      useCurrentHost.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            if (app.getBroadcastClientService().getHostHttpServerInfo() != null) {
               startActivity(new Intent(StartScanActivity.this, MainActivity.class));
            } else {
               Toast.makeText(StartScanActivity.this, "Error, host is unknown, please scan again", Toast.LENGTH_LONG).show();
               updateViews();
            }
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
      updateViews();
   }

   private void updateViews() {

      // based on state we update views, if we have found a host already, show it and allow RESCAN
      // if we have not found a host, show scan 

      InetSocketAddress host = null;
      if (app.getBroadcastClientService() != null) {
         host = app.getBroadcastClientService().getHostHttpServerInfo();
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
      return "StartScanActivity";
   }

   @Override
   public boolean onTouchEvent(MotionEvent event) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
         //proceed();
      }
      return super.onTouchEvent(event);
   }

   private void startScan() {

      // TODO this scanning state path stuff is a convoluted mess, clean up

      InetSocketAddress host = null;
      if (app.getBroadcastClientService() != null) {
         host = app.getBroadcastClientService().getHostHttpServerInfo();
      }

      if (host != null) {
         Toast.makeText(this, "Found Embiggen host:" + host.getHostName(), Toast.LENGTH_LONG).show();
         startActivity(new Intent(StartScanActivity.this, MainActivity.class));
      } else {
         // timer, wait on scan for X, display found/not found         

         final ProgressDialog pd = new ProgressDialog(this);

         final TimerTask scanTask = new TimerTask() {
            @Override
            public void run() {
               StartScanActivity.this.runOnUiThread(new Runnable() {
                  public void run() {
                     // NOTE when broadcast client recieves a host, it stops itself (so no need to explict stop here)
                     InetSocketAddress host = null;
                     if (app.getBroadcastClientService() != null) {
                        host = app.getBroadcastClientService().getHostHttpServerInfo();
                     }
                     if (host != null) {
                        pd.dismiss();
                        Toast.makeText(StartScanActivity.this, "Found Embiggen host:" + host.getHostName(),
                                 Toast.LENGTH_LONG).show();
                        startActivity(new Intent(StartScanActivity.this, MainActivity.class));
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
               app.getBroadcastClientService().stopClient();
            }
         });
         pd.show();

         timer.scheduleAtFixedRate(scanTask, 100, 1000);
      }
   }
}
