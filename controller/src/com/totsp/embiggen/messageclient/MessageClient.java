package com.totsp.embiggen.messageclient;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.totsp.embiggen.App;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Small plain Java message MessageClient for communication via sockets on Android. 
 * 
 * NOTE: This client has two sockets, used one at a time (one after the other): 
 * 1. Initially the client tries to DISCOVER a server using BROADCASTS (UDP socket).
 * 2. If a server is successfully discovered, the client shuts down the broadcast socket and opens a regular socket for messaging with server (at host:port provided by broadcast). 
 *  
 * NOTE: Format of the discovery message is as follows:
 * EMBIGGEN_HOST~1.2.3.4:1234
 * 
 * @author ccollins
 *
 */
public class MessageClient {

   // TODO finish regular socket communications after host discovered

   // TODO socket timeouts and options on all sockets

   public static final String BROADCAST_NET = "255.255.255.255";
   public static final int BROADCAST_FIXED_PORT = 8378;

   private DatagramSocket broadcastSocket;
   private final ExecutorService broadcastExecutor;
   private String hostIpAddress;
   private String hostPort;

   private Socket clientSocket;
   private final ExecutorService clientExecutor;

   private WifiManager wifiManager;
   private Context context;

   public MessageClient(Context context) {
      Log.i(App.TAG, "instantiated MessageClient");
      this.context = context;
      wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

      broadcastExecutor = Executors.newFixedThreadPool(1);
      clientExecutor = Executors.newFixedThreadPool(1);
   }

   // NOTE a caller can re-scan by simply calling stop->start

   public void start() {
      Log.i(App.TAG, "MessageClient start");
      initBroadcastClient(); // go ahead and start listening at start
   }

   public void stop() {
      Log.i(App.TAG, "MessageClient stop");
      terminateBroadcastClient();
      terminateClient();
      hostIpAddress = null;
      hostPort = null;
   }

   public InetSocketAddress getHostInetSocketAddress() {
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

   public void sendMessage(String msg) {
      /*
      if (channel != null && channel.isConnected()) {
         channel.write(msg);
      }
      */

      System.out.println("**** CLIENT SENDING MESSAGE TO SERVER");

      if (clientSocket != null && clientSocket.isConnected()) {
         // note socket.isConnected tells you whether or not socket was EVER connected (not current status)
         try {
            // currently server expects lines of input (using BufferedReader readline, which accepts /n, /r, or /r/n).
            if (!msg.endsWith("\n") && !msg.endsWith("\r") && !msg.endsWith("\r\n")) {
               msg += "\n"; 
            }
            OutputStream out = clientSocket.getOutputStream();
            out.write(msg.getBytes(), 0, msg.getBytes().length);
            out.flush();
            out.close();
            // TODO check for server ACK response? (EMBIGGEN SERVER ACK)
         } catch (IOException e) {
            Log.e(App.TAG, "Error sending message to host server", e);
            // TODO bus post socket is disconnected and allow client to choose to restart client?
         }
      } else {
         Log.w(App.TAG, "Cannot send message to host server, clientSocket is not connected");
      }

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
      Log.i(App.TAG, "MessageClient broadcast socket listening");

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

   private void initClient() {
      Log.v(App.TAG, "MessageClient initClient");

      if (hostIpAddress == null || hostPort == null) {
         throw new RuntimeException("Error cannot initClient until host has been discovred");
      }

      try {
         InetAddress addr = InetAddress.getByName(hostIpAddress);
         clientSocket = new Socket(addr, Integer.valueOf(hostPort));
         //clientSocket.setSoTimeout(timeout);
      } catch (IOException e) {
         Log.e(App.TAG, "Error starting client:" + e.getMessage(), e);
      }
   }

   private void terminateClient() {

      clientExecutor.shutdown();
      try {
         clientExecutor.awaitTermination(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         Log.e(App.TAG, "Error stopping client:" + e.getMessage(), e);
      }
      clientExecutor.shutdownNow();

      if (clientSocket != null && clientSocket.isBound()) {
         try {
            clientSocket.close();
         } catch (IOException e) {
            // TODO 
         }
      }
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
            Log.i(App.TAG, "MessageClient got packet " + data);

            if (data != null && data.length() > 0) {
               String hostPortData = data.substring(data.indexOf("~") + 1, data.length());
               ///System.out.println("****** hostPortData:" + hostPortData);
               hostIpAddress = hostPortData.substring(0, hostPortData.indexOf(":"));
               hostPort = hostPortData.substring(hostPortData.indexOf(":") + 1, hostPortData.length());

               if (hostIpAddress != null && hostPort != null) {
                  Log.e(App.TAG, "MessageClient got host data from broadcast host:" + hostIpAddress + " port:"
                           + hostPort);
                  terminateBroadcastClient();
                  initClient();
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
