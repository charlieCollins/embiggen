package com.totsp.embiggen.host.messageserver;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import com.squareup.otto.Bus;
import com.totsp.android.util.NetworkUtil;
import com.totsp.embiggen.host.App;
import com.totsp.embiggen.host.event.DisplayMediaEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

   // TODO need to figure out how clients can select a particular server if more than one present?

   // TODO use BaseStartStop for this

   // TODO socket timeouts and options on all sockets

   // FUTURE enhance this by switching to mDNS or SSDP, etc for discovery
   // (http://4thline.org/projects/cling ?)

   public static final int DEFAULT_SERVER_PORT = 8379;

   public static final String BROADCAST_NET = "255.255.255.255";
   public static final int BROADCAST_FIXED_PORT = 8378;

   private static final int BROADCAST_FREQUENCY_MILLIS = 6000;
   private static final String EMBIGGEN_HOST = "EMBIGGEN_HOST";
   private static final char DELIMITER = '~';

   private WifiManager wifiManager;
   private Timer broadcastTimer;
   private TimerTask broadcastTimerTask;

   private DatagramSocket broadcastSocket;
   private final ExecutorService broadcastExecutor;

   private ServerSocket serverSocket;
   private final ExecutorService serverExecutor;
   
   private Context context;
   private final Bus bus; // TODO inject bus

   public MessageServer(Context context, Bus bus) {
      wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
      Log.i(App.TAG, "MessageServer instantiated");

      this.context = context;
      this.bus = bus;
      
      broadcastExecutor = Executors.newFixedThreadPool(1);
      serverExecutor = Executors.newFixedThreadPool(20);      
   }

   public void start(int port) {
      //bus.register(this);
      startBroadcasting(port);
      initServer(port);
      Log.i(App.TAG, "MessageServer started (port:" + port + ")");
   }

   public void stop() {
      //bus.unregister(this);
      stopBroadcasting();
      terminateServer();
      Log.i(App.TAG, "MessageServer stopped");
   }

   //
   // priv
   //

   private void startBroadcasting(final int port) {
      Log.v(App.TAG, "MessageServer startBroadcasting");

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
      Log.v(App.TAG, "MessageServer stopBroadcasting");

      broadcastTimerTask.cancel();
      broadcastTimer.purge();
      broadcastTimer.cancel();
      terminateBroadcastServer();
   }

   private void initBroadcastServer() {
      Log.v(App.TAG, "MessageServer initBroadcastServer");

      try {
         broadcastSocket = new DatagramSocket(BROADCAST_FIXED_PORT);
         broadcastSocket.setBroadcast(true);
      } catch (Exception e) {
         Log.e(App.TAG, "Error initiliazing broadcast server", e);
      }
   }

   private void terminateBroadcastServer() {
      Log.v(App.TAG, "MessageServer terminateBroadcastServer");

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

   private void initServer(int port) {
      Log.v(App.TAG, "MessageServer initServer");
      try {
         serverSocket = new ServerSocket(port);
         while (!serverExecutor.isShutdown()) {
            // NOTE plain socket (TCP) only RESPONDS to requests, does not expose "send" functionality
            serverExecutor.submit(new RequestResponseHandler(serverSocket.accept()));
         }
      } catch (IOException e) {
         Log.e(App.TAG, "Error starting server:" + e.getMessage(), e);
      }
   }

   private void terminateServer() {
      Log.v(App.TAG, "MessageServer terminateServer");

      serverExecutor.shutdown();
      try {
         serverExecutor.awaitTermination(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         Log.e(App.TAG, "Error stopping server:" + e.getMessage(), e);
      }
      serverExecutor.shutdownNow();

      try {
         serverSocket.close();
      } catch (Exception e) {
         Log.e(App.TAG, "Error closing socket", e);
      }
   }

   private void sendBroadcast(int port) {
      ///Log.v(App.TAG, "MessageServer sendBroadcast");

      // broadcast the server host/port          
      if (broadcastSocket == null || !broadcastSocket.isBound()) {
         initBroadcastServer();
      } else {
         // as an ode to the RPG programmers of yore, I use the tilde as a delimeter! (this sucks balls BTW)
         String wifiIpAddress = NetworkUtil.getWifiIpAddress(wifiManager);
         final String msg = EMBIGGEN_HOST + DELIMITER + wifiIpAddress + ":" + port;

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
         ///Log.v(App.TAG, "   sent broadcast:" + msg);
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

   //
   // classes
   //

   private class RequestResponseHandler implements Runnable {
      private final Socket socket;

      RequestResponseHandler(final Socket socket) throws SocketException {
         this.socket = socket;
      }

      // FUTURE break this up
      public void run() {
         try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // NOTE: expects lines, and ends request at a blank line "" (like HTTP)
            List<String> lines = new ArrayList<String>();
            String line = null;
            while ((line = in.readLine()) != null) {
               if (line.trim().equals("")) {
                  break;
               }
               lines.add(line);
            }

            if (lines.size() > 1) {
               Log.w(App.TAG, "MessageServer request lines size:" + lines.size() + " lines beyond first will be ignored");
            }

            String request = lines.get(0); // first line
            Log.d(App.TAG, "MessageServer input request:" + request);
            
            // TODO create separate handler/processor for protocol strings (right now just DISPLAY_MEDIA~)
            if (request != null && request.startsWith("DISPLAY_MEDIA~")) { 
               request.trim();
               final String urlString = request.substring(request.indexOf("~") + 1, request.length());
               Log.v(App.TAG, "MessageServer sending DISPLAY_MEDIA event, urlString:" + urlString);               		
               
               runOnMainThread(new Runnable() {
                  public void run() {
                     bus.post(new DisplayMediaEvent(urlString));               
                  }
               });               
            }

            // send ack response (future response code and error handling if content can't be served?)
            OutputStream out = null;
            try {           
               out = socket.getOutputStream();
               String response = "EMBIGGEN SERVER ACK";
               out.write(response.getBytes(), 0, response.getBytes().length);
               out.flush();
               
            } finally {

               // TODO will this leak OutputStreams, research keeping socket open (rather than re-est each time)
               // ??? keep one OutputStream around? (closing it will close socket)
               /*
               if (out != null) {
                  out.close();
               }
               */
            }            
         } catch (IOException e) {
            Log.e(App.TAG, "Error I/O exception", e);
         } finally {
            
            // TODO don't close socket, check timeouts, heartbeat, etc
            
            /*
            if (socket != null) {
               try {
                  socket.close();
               } catch (IOException e) {
                  //ignore
               }
            }
            */
         }
      }
   }
   
   // MessageServer is off the main thread, so to run stuff back on main, use this
   private synchronized void runOnMainThread(Runnable r) {
      new Handler(context.getMainLooper()).post(r);
   }
}
