package com.totsp.embiggen.broadcastclient;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.totsp.embiggen.App;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Small plain Java message BroadcastClient for discovery via plain broadcast socket on Android (UDP). 
 *  
 * NOTE: Format of the discovery message is as follows:
 * EMBIGGEN_HOST~1.2.3.4:1234
 * 
 * @author ccollins
 *
 */
public class BroadcastClient {

   // TODO socket timeouts and options

   public static final String BROADCAST_NET = "255.255.255.255";
   public static final int BROADCAST_FIXED_PORT = 8378;

   private DatagramSocket broadcastSocket;
   private final ExecutorService broadcastExecutor;
   private String hostIpAddress;
   private String hostPort;

   private WifiManager wifiManager;
   private Context context;

   public BroadcastClient(Context context) {
      Log.i(App.TAG, "instantiated BroadcastClient");
      this.context = context;
      wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

      broadcastExecutor = Executors.newFixedThreadPool(1);
   }

   // NOTE a caller can re-scan by simply calling stop->start

   public void start() {
      Log.i(App.TAG, "BroadcastClient start");
      initBroadcastClient(); // go ahead and start listening at start
   }

   public void stop() {
      Log.i(App.TAG, "BroadcastClient stop");
      terminateBroadcastClient();
      hostIpAddress = null;
      hostPort = null;
   }

   public InetSocketAddress getHostHttpServerInetSocketAddress() {
      InetSocketAddress isa = null;
      if (hostIpAddress != null && hostPort != null) {
         try {
            InetAddress ia = InetAddress.getByName(hostIpAddress);
            isa = new InetSocketAddress(ia, Integer.valueOf(hostPort));
         } catch (IOException e) {
            Log.e(App.TAG, "Error getting hostInetAddress", e);
         }
      }
      return isa;
   }

   //
   // priv
   //

   private void initBroadcastClient() {

      // NOTE tried multicast on Android first, had issues, punted to broadcast 
      // (and our broadcasts are tiny, so hopefully not to annoying to rest of LAN)

      broadcastSocket = null;
      try {
         broadcastSocket = new DatagramSocket(BROADCAST_FIXED_PORT);
      } catch (SocketException e) {
         Log.e(App.TAG, "Error, could not create broadcast socket", e);
         return;
      }

      //broadcastSocket.setBroadcast(true); don't send broadcast here, just receive
      Log.i(App.TAG, "BroadcastClient broadcast socket listening");

      // don't need to loop here, broadcastSocket.receive is blocking
      if (!broadcastExecutor.isShutdown()) {
         try {
            broadcastExecutor.submit(new BroadcastHandler(broadcastSocket));
         } catch (SocketException e) {
            e.printStackTrace();
         }
      }
   }

   private void terminateBroadcastClient() {
      Log.d(App.TAG, "terminateBroadcastClient");

      if (broadcastSocket != null && broadcastSocket.isBound()) {
         broadcastSocket.close();
      }

      broadcastExecutor.shutdown();
      try {
         broadcastExecutor.awaitTermination(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         Log.e(App.TAG, "Error stopping broadcast client:" + e.getMessage(), e);
      }
      broadcastExecutor.shutdownNow();
   }

   //
   // class
   //

   private class BroadcastHandler implements Runnable {

      private final DatagramSocket socket;

      final byte[] buffer = new byte[1024];
      final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

      BroadcastHandler(final DatagramSocket socket) throws SocketException {
         this.socket = socket;
      }

      public void run() {
         try {
            socket.receive(packet);

            // we don't care about encoding/etc for this?
            String data = new String(buffer, 0, packet.getLength());
            Log.i(App.TAG, "BroadcastClient got packet " + data);

            if (data != null && data.length() > 0) {
               String hostPortData = data.substring(data.indexOf("~") + 1, data.length());
               ///System.out.println("****** hostPortData:" + hostPortData);
               hostIpAddress = hostPortData.substring(0, hostPortData.indexOf(":"));
               hostPort = hostPortData.substring(hostPortData.indexOf(":") + 1, hostPortData.length());

               if (hostIpAddress != null && hostPort != null) {
                  Log.e(App.TAG, "BroadcastClient got host data from broadcast host:" + hostIpAddress + " port:"
                           + hostPort);
                  terminateBroadcastClient();
               }
            }

         } catch (IOException e) {
            Log.e(App.TAG, "Broadcast socket error (expected after socket is closed intentionally):" + e.getMessage());
         }

         finally {
            if (socket != null) {
               socket.close();
            }
         }
      }
   }
}
