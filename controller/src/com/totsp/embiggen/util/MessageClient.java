package com.totsp.embiggen.util;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.totsp.embiggen.App;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

// TODO see TODOs in MessageServer, threading, check net conn, BaseStartStop, etc

// TODO once client gets broadcast that a server is around, stop broadcast reciever, start reg socket

/**
 * Small plain Java message CLIENT for communication via sockets on Android. 
 * 
 * NOTE: This client has two sockets at various times: 
 * 1. Initially the client tries to DISCOVER a server using BROADCASTS.
 * 2. If a server is successfully discovered, the client shuts down the broadcast socket and opens a regular socket for messaging with server (at host:port provided by broadcast). 
 *  
 * NOTE: Format of the discovery message is as follows:
 * EMBIGGEN_HOST~1.2.3.4:1234
 * 
 * @author ccollins
 *
 */
public class MessageClient {

   public static final String BROADCAST_FIXED_NET = "255.255.255.255";
   public static final int BROADCAST_FIXED_PORT = 8378;

   private DatagramSocket broadcastSocket;

   private WifiManager wifiManager;

   public MessageClient(Context context) {
      Log.i(App.TAG, "** created MessageClient");
      wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
   }

   public void start() {
      initBroadcastClient();
      // broadcast reception will initClient (init regular client)
   }

   public void stop() {
      terminateBroadcastClient();
      terminateClient();
   }

   public void sendMessage(String msg) {
      /*
      if (channel != null && channel.isConnected()) {
         channel.write(msg);
      }
      */
   }

   //
   // priv
   //

   private void initBroadcastClient() {

      // NOTE tried multicast on Android first, had issues, punted to broadcast 
      // (and our broadcasts are tiny, so hopefully not to annoying to rest of LAN)

      try {
         broadcastSocket = new DatagramSocket(BROADCAST_FIXED_PORT);
         //broadcastSocket.setBroadcast(true); don't send broadcast here, just receive
         Log.i(App.TAG, "CLIENT broadcast socket listening");

         byte[] buffer = new byte[1024];
         DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
         while (true) {
            broadcastSocket.receive(packet);
            // we don't care about encoding/etc for this?
            String data = new String(buffer, 0, packet.getLength());
            Log.i(App.TAG, "CLIENT got packet " + data);
         }
      } catch (Exception e) {
         Log.e(App.TAG, "Error initializing broadcast client", e);
      }
   }

   private void terminateBroadcastClient() {
      if (broadcastSocket != null && broadcastSocket.isBound()) {
         broadcastSocket.close();
      }
   }

   private void initClient(String serverHost, int serverPort) {

      /*
      channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
               Executors.newCachedThreadPool());
      ClientBootstrap bootstrap =
               new ClientBootstrap(channelFactory);
      bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
         public ChannelPipeline getPipeline() throws Exception {
            return Channels.pipeline(new StringDecoder(CharsetUtil.UTF_8), new StringEncoder(CharsetUtil.UTF_8),
                     new DelimiterBasedFrameDecoder(80, Delimiters.lineDelimiter()), new ClientHandler());
         }
      });

      ChannelFuture future = bootstrap.connect(new InetSocketAddress(serverHost, serverPort));
      channel = future.awaitUninterruptibly().getChannel();
      if (!future.isSuccess()) {
         future.getCause().printStackTrace();
         bootstrap.releaseExternalResources();
         throw new RuntimeException("CLIENT Error, could not connect to server:" + serverHost + " " + serverPort);
      }
      */
   }

   public void terminateClient() {

      /*
      if (channel != null && channel.isConnected()) {
         channel.close(); 
         channelFactory.releaseExternalResources();
      }
      
      // check broadcast client in case
      if (datagramChannel != null && datagramChannel.isBound()) {
         terminateBroadcastClient();
      }
      */
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

}
