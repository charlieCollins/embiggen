package com.totsp.embiggen.broadcastclient;

import android.content.Context;
import android.util.Log;

import com.totsp.embiggen.App;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Small plain Java message BroadcastClient for discovery via plain broadcast socket on Android (UDP). 
 *  
 * NOTE: Format of the discovery message is as follows:
 * EMBIGGEN_HOST~INSTALL_ID~1.2.3.4~1234
 * 
 * @author ccollins
 *
 */
public class BroadcastClient {

   // TODO socket timeouts and options

   public static final String BROADCAST_NET = "255.255.255.255";
   public static final int BROADCAST_FIXED_PORT = 8378;
   private static final String DELIMITER = "~";

   private DatagramSocket broadcastSocket;
   private ExecutorService broadcastExecutor;

   private String hostId; // unique id, and shown on host screen, so user can say "yes/no" for found hosts
   private String hostIpAddress;
   private String hostPort;

   private Context context;

   public BroadcastClient(Context context) {
      Log.i(App.TAG, "instantiated BroadcastClient");
      this.context = context;
   }

   // NOTE a caller can re-scan by simply calling stop->start

   public void start() {
      Log.i(App.TAG, "BroadcastClient start");
      initBroadcastClient(); // go ahead and start listening at start
   }

   public void stop() {
      Log.i(App.TAG, "BroadcastClient stop");
      terminateBroadcastClient();
   }

   public void clearHostHttpServerInfo() {
      Log.i(App.TAG, "BroadcastClient clearHostHttpServerInfo");
      hostId = null;
      hostIpAddress = null;
      hostPort = null;
   }

   public HostHttpServerInfo getHostHttpServerInfo() {
      InetSocketAddress isa = null;
      if (hostIpAddress != null && hostPort != null && hostId != null) {
         try {
            InetAddress ia = InetAddress.getByName(hostIpAddress);
            isa = new InetSocketAddress(ia, Integer.valueOf(hostPort));
            Log.i(App.TAG, "BroadcastClient getHostHttpServerInfo:" + isa);
            return new HostHttpServerInfo(isa, hostId);
         } catch (IOException e) {
            Log.e(App.TAG, "Error getting hostInetAddress", e);
         }
      }
      return null;
   }

   //
   // priv
   //

   private void initBroadcastClient() {

      // NOTE tried multicast on Android first, had issues, punted to broadcast 
      // (and our broadcasts are tiny, so hopefully not to annoying to rest of LAN)

      // make sure all is shut down first (approp checks are in terminate)
      terminateBroadcastClient();

      try {
         broadcastSocket = new DatagramSocket(BROADCAST_FIXED_PORT);
      } catch (SocketException e) {
         Log.e(App.TAG, "Error, could not create broadcast socket", e);
         return;
      }

      broadcastExecutor = Executors.newFixedThreadPool(1);
      // don't need to loop here, broadcastSocket.receive is blocking
      try {
         broadcastExecutor.submit(new BroadcastHandler(broadcastSocket));
         Log.i(App.TAG, "BroadcastClient broadcast socket listening");
      } catch (SocketException e) {
         Log.e(App.TAG, "Error with broadcast socket", e);
      }
   }

   private void terminateBroadcastClient() {

      if (broadcastSocket != null) {
         broadcastSocket.close();
      }

      if (broadcastExecutor != null) {
         broadcastExecutor.shutdown();
         try {
            broadcastExecutor.awaitTermination(5, TimeUnit.SECONDS);
         } catch (InterruptedException e) {
            Log.e(App.TAG, "Error stopping broadcast client:" + e.getMessage(), e);
         }
         if (broadcastExecutor != null) {
            broadcastExecutor.shutdownNow();
         }
         broadcastExecutor = null;
      }
   }

   //
   // class
   //

   public static class HostHttpServerInfo {
      public final InetSocketAddress address;
      public final String id;

      public HostHttpServerInfo(InetSocketAddress address, String id) {
         super();
         this.address = address;
         this.id = id;
      }
   }

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
            Log.d(App.TAG, "BroadcastClient got packet " + data);

            if (data != null && data.length() > 0) {

               if (!data.contains(DELIMITER)) {
                  Log.e(App.TAG, "BroadcastClient got invalid data from broadcast host:" + data);
               }

               else {

                  // four parts
                  // EMBIGGEN_HOST~install_id~hostip~port
                  String[] parts = data.split(DELIMITER);

                  if (parts == null || parts.length != 4) {
                     Log.e(App.TAG, "BroadcastClient got invalid parts from data:" + Arrays.toString(parts));
                  } else {

                     hostId = parts[1];
                     hostIpAddress = parts[2];
                     hostPort = parts[3];

                     if (hostIpAddress != null && hostPort != null) {
                        Log.e(App.TAG, "BroadcastClient got host data from broadcast host:" + hostIpAddress + " port:"
                                 + hostPort);
                        // NOTE once a host broadcast is received the broadcast client STOPS ITSELF (but does not clear just found host info)
                        stop();
                     }
                  }
               }
            }

         } catch (IOException e) {
            Log.e(App.TAG, "Broadcast socket error (expected after socket is closed intentionally):" + e.getMessage());
         } finally {
            if (socket != null) {
               socket.close();
            }
         }
      }
   }
}
