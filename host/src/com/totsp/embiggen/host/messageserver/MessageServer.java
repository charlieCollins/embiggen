package com.totsp.embiggen.host.messageserver;

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

/**
 * Small plain Java message SERVER for communication via sockets on Android. 
 * 
 * NOTE: This server has two sockets: 
 * 1. One that BROADCASTS the host:port of the regular socket server for discovery purposes, 
 * 2. Another that is the regular socket server itself (where messages are sent/recieved). 
 * (Tried multicast for the discovery, but that failed on many Android devices, so just broadcast -- tiny messages.)
 * 
 * NOTE: Format of the discovery message is as follows:
 * EMBIGGEN_HOST~1.2.3.4:1234
 * 
 * @author ccollins
 *
 */
public class MessageServer {
   
   // TODO validate that device has network connectivity, and that it's LAN (and add connectivity receiver?)
   
   // TODO broadcast port is hard coded, regular server port could be random? 
   // (need to figure out how clients can select a suitable server if more than one present?)
   
   // TODO threading, all this will run on main thread as is, that is bad, very bad, and I should feel bad
   
   // TODO use BaseStartStop for this
   
   // FUTURE enhance this by switching to using the SSDP discovery protocol (still just multicast, but use defined protocol)
   // (http://4thline.org/projects/cling ?)

   public static final int DEFAULT_SERVER_PORT = 8379;

   public static final String BROADCAST_FIXED_NET = "255.255.255.255";
   public static final int BROADCAST_FIXED_PORT = 8378;

   private static final int BROADCAST_FREQUENCY_MILLIS = 6000;
   private static final String EMBIGGEN_HOST = "EMBIGGEN_HOST";
   private static final char DELIMITER = '~';

   private WifiManager wifiManager;
   private Timer broadcastTimer;
   private TimerTask broadcastTimerTask;

   private DatagramSocket broadcastSocket;   

   public MessageServer(Context context) {
      wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
      Log.i(App.TAG, "MessageServer instantiated");
   }

   public void start(int port) {
      initServer();
      startBroadcasting(port);
      Log.i(App.TAG, "MessageServer started (port:" + port + ")");
   }

   public void stop() {
      terminateServer();
      stopBroadcasting();
      Log.i(App.TAG, "MessageServer stopped");
   }

   //
   // priv
   //

   private void startBroadcasting(final int port) {
      if (broadcastTimerTask != null) {
         broadcastTimerTask.cancel();
      }
      if (broadcastTimer != null) {
         broadcastTimer.purge();
         broadcastTimer.cancel();
      }

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
      broadcastTimerTask.cancel();
      broadcastTimer.purge();
      broadcastTimer.cancel();
      terminateBroadcastServer();
   }

   private void initBroadcastServer() {
      Log.i(App.TAG, "MESSAGESERVER initBroadcastServer");

      try {
         broadcastSocket = new DatagramSocket(BROADCAST_FIXED_PORT);
         broadcastSocket.setBroadcast(true);

      } catch (Exception e) {
         Log.e(App.TAG, "Error initiliazing broadcast server", e);
      }
   }

   private void terminateBroadcastServer() {
      if (broadcastSocket != null && broadcastSocket.isBound()) {
         broadcastSocket.close();
      }
   }

   private void initServer() {
      Log.i(App.TAG, "MESSAGESERVER initServer");

      /*
      channelFactory =
               new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

      ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
      bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
         public ChannelPipeline getPipeline() {
            return Channels.pipeline(new StringDecoder(CharsetUtil.UTF_8), new StringEncoder(CharsetUtil.UTF_8),
                     new DelimiterBasedFrameDecoder(80, Delimiters.lineDelimiter()), new MessageHandler());
         }
      });
      bootstrap.setOption("child.tcpNoDelay", true);
      bootstrap.setOption("child.keepAlive", true);
      channel = bootstrap.bind(addr);
      */
   }

   private void terminateServer() {
      //ChannelGroupFuture future = CHANNEL_GROUP.close();
      //future.awaitUninterruptibly();
      //channel.close();
      //channelFactory.releaseExternalResources();
   }

   private void sendBroadcast(int port) {

      // broadcast the server host/port          
      if (broadcastSocket == null || !broadcastSocket.isBound()) {
         initBroadcastServer();
      } else {
         // as an ode to the RPG programmers of yore, I use the tilde as a delimeter! (this sucks balls BTW)
         String wifiIpAddress = NetworkUtil.getWifiIpAddress(wifiManager);
         String msg = EMBIGGEN_HOST + DELIMITER + wifiIpAddress + ":" + port;
         try {
            broadcastSocket.send(new DatagramPacket(msg.getBytes(), msg.getBytes().length, getBroadcastAddress(),
                     BROADCAST_FIXED_PORT));
         } catch (IOException e) {
            Log.e(App.TAG, "Error sending broadcast", e);
         }

         Log.i(App.TAG, "MessageServer sent status broadcast:" + msg);
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
}
