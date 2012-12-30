package com.totsp.embiggen.host.messageserver;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class MessageServer {

   public static final int DEFAULT_SERVER_PORT = 8379;
   public static final int BROADCAST_SERVER_PORT = 8378;

   private static final int BROADCAST_FREQUENCY_MILLIS = 6000;

   // TODO server needs to pick an available port
   // TODO server needs to BROADCAST periodically what host/port it is?    

   private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup();

   private ChannelFactory channelFactory;

   private DatagramChannelFactory datagramChannelFactory;
   private DatagramChannel datagramChannel;

   private String wifiAddress;

   public MessageServer(String wifiAddress) {

      //Log.i(Constants.LOG_TAG, "ANDROID MESSAGE SERVER INSTANTIATED");
      System.out.println("MessageServer instantiated, wifiAddress:" + wifiAddress);

      this.wifiAddress = wifiAddress;
   }

   private void startBroadcastTimer(final InetSocketAddress addr) {
      Timer timer = new Timer();
      TimerTask timerTask = new TimerTask() {
         @Override
         public void run() {
            sendBroadcast(addr);
         }
      };
      timer.schedule(timerTask, 0, BROADCAST_FREQUENCY_MILLIS);
   }

   private void stopBroadcastTimer() {
      // TODO
   }

   public void start(int port) {

      // TODO validate that device has network connectivity, and that it's LAN (and add connectivity receiver?)

      InetSocketAddress addr = new InetSocketAddress(port);

      initServer(addr);

      initBroadcastServer();
      startBroadcastTimer(addr);

      System.out.println("MessageServer started (port:" + port + ")");
   }

   public void stop() {

      terminateServer();

      terminateBroadcastServer();
      stopBroadcastTimer();

      System.out.println("MessageServer stopped");
   }

   private void initBroadcastServer() {
      // broadcast resources
      datagramChannelFactory = new NioDatagramChannelFactory(Executors.newCachedThreadPool());
      ConnectionlessBootstrap b = new ConnectionlessBootstrap(datagramChannelFactory);
      b.setOption("broadcast", "true");
      b.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));
      b.setPipelineFactory(new ChannelPipelineFactory() {
         public ChannelPipeline getPipeline() {
            return Channels.pipeline(new StringDecoder(CharsetUtil.UTF_8), new StringEncoder(CharsetUtil.UTF_8),
                     new DelimiterBasedFrameDecoder(80, Delimiters.lineDelimiter()), new MessageHandler());
         }
      });
      datagramChannel = (DatagramChannel) b.bind(new InetSocketAddress(0));
   }

   private void terminateBroadcastServer() {
      datagramChannel.close();
      datagramChannelFactory.releaseExternalResources();
   }

   private void initServer(InetSocketAddress addr) {
      //
      // regular message stuff
      //
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

      bootstrap.bind(addr);
   }

   private void terminateServer() {
      ChannelGroupFuture future = CHANNEL_GROUP.close();
      future.awaitUninterruptibly();
      channelFactory.releaseExternalResources();
   }

   private void sendBroadcast(InetSocketAddress addr) {
      // broadcast the server host/port          
      if (datagramChannel != null && datagramChannel.isBound()) {
         String msg = "MESSAGESERVER_BROADCAST:HOST:" + wifiAddress + ":" + addr.getPort();
         datagramChannel.write(msg, new InetSocketAddress("255.255.255.255", 8889));
         System.out.println("MessageServer sent status broadcast:" + msg);
      } else {
         System.out.println("datagramChannel null or not connected");
      }
   }

   //
   // handlers
   // 

   private static class MessageHandler extends SimpleChannelUpstreamHandler {

      public MessageHandler() {
      }

      @Override
      public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
         CHANNEL_GROUP.add(e.getChannel());
      }

      @Override
      public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
         if (e instanceof ChannelStateEvent && ((ChannelStateEvent) e).getState() != ChannelState.INTEREST_OPS) {
            System.out.println("SERVER handleUpStream:" + e.toString());
         }
         super.handleUpstream(ctx, e);
      }

      @Override
      public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

         String msg = (String) e.getMessage();

         System.out.println("SERVER messageReceived:" + msg);

         //ctx.getChannel().write("back from server BAR FOO!");

         //transferredMessages.incrementAndGet();
         //e.getChannel().write(e.getMessage());
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
         System.err.println("SERVER unexpected exception from downstream:" + e.getCause().getMessage());
         e.getCause().printStackTrace();
         e.getChannel().close();
      }
   }
}
