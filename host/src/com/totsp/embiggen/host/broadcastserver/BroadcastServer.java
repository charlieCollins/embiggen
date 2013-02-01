package com.totsp.embiggen.host.broadcastserver;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.totsp.android.util.NetworkUtil;
import com.totsp.embiggen.host.App;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Small plain Java discovery SERVER using broadcast socket (UDP) on Android. 
 * (NOTE: tried multicast, had issues on Android, need to look into mDNS/SSDP, but this much simpler.)
 * 
 * NOTE: Format of the discovery message is as follows:
 * EMBIGGEN_HOST~INSTALL_ID~1.2.3.4~1234
 * 
 * @author ccollins
 *
 */
public class BroadcastServer {

   // TODO validate that device has network connectivity, and that it's LAN (and add connectivity receiver?)

   // TODO socket timeouts and options

   // FUTURE enhance this by switching to mDNS or SSDP, etc for discovery
   // (http://4thline.org/projects/cling ?)

   public static final String BROADCAST_NET = "255.255.255.255";
   public static final int BROADCAST_FIXED_PORT = 8378;

   private static final int BROADCAST_FREQUENCY_MILLIS = 7000;
   private static final String EMBIGGEN_HOST = "EMBIGGEN_HOST";
   private static final char DELIMITER = '~';

   private WifiManager wifiManager;
   private Timer broadcastTimer;
   private TimerTask broadcastTimerTask;

   private DatagramSocket broadcastSocket;
   private final ExecutorService broadcastExecutor;

   private final App app; // inject?
   private final String hostId;

   public BroadcastServer(App app) {
      
      this.app = app;
      
      hostId = app.getHostId();
      wifiManager = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
      broadcastExecutor = Executors.newFixedThreadPool(1);
      
      Log.i(App.TAG, "BroadcastServer instantiated");      
   }

   public void start(int port) {
      //bus.register(this);
      startBroadcasting(port);
      Log.i(App.TAG, "BroadcastServer started (HTTP server port to advertise:" + port + ")");
   }

   public void stop() {
      //bus.unregister(this);
      stopBroadcasting();
      Log.i(App.TAG, "BroadcastServer stopped");
   }

   //
   // priv
   //

   private void startBroadcasting(final int port) {
      Log.v(App.TAG, "BroadcastServer startBroadcasting");

      if (broadcastTimerTask != null) {
         broadcastTimerTask.cancel();
      }
      if (broadcastTimer != null) {
         broadcastTimer.purge();
         broadcastTimer.cancel();
      }

      // kind of ugly that here we use Timer rather than executor 
      // (we do it for easy frequency schedule purposes, but that's not a good excuse)
      broadcastTimer = new Timer();
      broadcastTimerTask = new TimerTask() {
         @Override
         public void run() {
            sendBroadcast(port);
         }
      };
      broadcastTimer.schedule(broadcastTimerTask, 0, BROADCAST_FREQUENCY_MILLIS);
   }

   private void stopBroadcasting() {
      Log.v(App.TAG, "BroadcastServer stopBroadcasting");

      broadcastTimerTask.cancel();
      broadcastTimer.purge();
      broadcastTimer.cancel();
      terminateBroadcastServer();
   }

   private void initBroadcastServer() {
      Log.v(App.TAG, "BroadcastServer initBroadcastServer");

      try {
         broadcastSocket = new DatagramSocket(BROADCAST_FIXED_PORT);
         broadcastSocket.setBroadcast(true);
      } catch (Exception e) {
         Log.e(App.TAG, "Error initiliazing broadcast server", e);
      }
   }

   private void terminateBroadcastServer() {
      Log.v(App.TAG, "BroadcastServer terminateBroadcastServer");

      broadcastExecutor.shutdown();
      try {
         broadcastExecutor.awaitTermination(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         Log.e(App.TAG, "Error stopping server:" + e.getMessage(), e);
      }
      broadcastExecutor.shutdownNow();

      if (broadcastSocket != null && broadcastSocket.isBound()) {
         broadcastSocket.close();
      }
   }

   private void sendBroadcast(int port) {
      // broadcast the server host/port          
      if (broadcastSocket == null || !broadcastSocket.isBound()) {
         initBroadcastServer();
      } else {
         // as an ode to the RPG programmers of yore, I use the tilde as a delimiter! 
         // (this sucks balls BTW)
         // (and note we call "getWifiIpAddress" every time here, intentionally (in case it has changed))
         String wifiIpAddress = NetworkUtil.getWifiIpAddress(wifiManager);
         final String msg =
                  EMBIGGEN_HOST + DELIMITER + hostId + DELIMITER + wifiIpAddress + DELIMITER + port;

         broadcastExecutor.submit(new Runnable() {
            public void run() {
               try {
                  broadcastSocket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, getBroadcastAddress(),
                           BROADCAST_FIXED_PORT));
               } catch (IOException e) {
                  Log.e(App.TAG, "Error sending broadcast", e);
               }
            }
         });
      }
   }

   private InetAddress getBroadcastAddress() throws IOException {
      DhcpInfo dhcp = wifiManager.getDhcpInfo();
      int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
      byte[] quads = new byte[4];
      for (int k = 0; k < 4; k++) {
         quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
      }
      return InetAddress.getByAddress(quads);
   }

   /*
   // BroadcastServer is off the main thread, so to run stuff back on main, use this
   private synchronized void runOnMainThread(Runnable r) {
      new Handler(app.getMainLooper()).post(r);
   }
   */
}
