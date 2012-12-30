package com.totsp.embiggen.util;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
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
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class MessageClient {

   public static final int BROADCAST_SERVER_PORT = 8378;

   private DatagramChannelFactory datagramChannelFactory;
   private DatagramChannel datagramChannel;

   private ChannelFactory channelFactory;
   private Channel channel;

   public MessageClient() {

      System.out.println("** created MessageClient");

      initBroadcastClient();      
   }
   
   public void terminateBroadcastClient() {
      datagramChannel.close();
      datagramChannelFactory.releaseExternalResources();
   }
   
   public void terminateClient() {
      // TODO check into proper way to close all channel/factory resources
      if (channel != null && channel.isConnected()) {
         channel.close(); 
         channelFactory.releaseExternalResources();
      }
      
      // check broadcast client in case
      if (datagramChannel != null && datagramChannel.isBound()) {
         terminateBroadcastClient();
      }
   }
   
   private void initBroadcastClient() {
      // BROADCAST client      
      datagramChannelFactory = new NioDatagramChannelFactory(Executors.newCachedThreadPool());
      ConnectionlessBootstrap connectionlessBootstrap = new ConnectionlessBootstrap(datagramChannelFactory);
      connectionlessBootstrap.setOption("broadcast", "false");
      connectionlessBootstrap.setOption("receiveBufferSizePredictorFactory",
               new FixedReceiveBufferSizePredictorFactory(1024));
      connectionlessBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
         public ChannelPipeline getPipeline() {
            return Channels.pipeline(new StringDecoder(CharsetUtil.UTF_8), new StringEncoder(CharsetUtil.UTF_8),
                     new DelimiterBasedFrameDecoder(80, Delimiters.lineDelimiter()), new BroadcastClientHandler());
         }
      });
      datagramChannel = (DatagramChannel) connectionlessBootstrap.bind(new InetSocketAddress(BROADCAST_SERVER_PORT));
   }

   private void initClient(String serverHost, int serverPort) {
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
   }

   public void sendMessage(String msg) {
      if (channel != null && channel.isConnected()) {
         channel.write(msg);
      }
   }

   //
   // handlers
   //
   
   private static class ClientHandler extends SimpleChannelUpstreamHandler {

      @Override
      public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
         if (e instanceof ChannelStateEvent && ((ChannelStateEvent) e).getState() != ChannelState.INTEREST_OPS) {
            System.out.println("CLIENT handleUpstream:" + e);
         }
         super.handleUpstream(ctx, e);
      }

      @Override
      public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
         //System.out.println("CLIENT channelConnected:" + e);
      }

      @Override
      public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
         String msg = (String) e.getMessage();

         System.out.println("CLIENT messageReceived:" + msg);
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
         System.err.println("CLIENT unexpected exception from downstream:" + e.getCause().getMessage());
         e.getChannel().close();
      }
   }

   private static class BroadcastClientHandler extends SimpleChannelUpstreamHandler {

      @Override
      public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
         if (e instanceof ChannelStateEvent && ((ChannelStateEvent) e).getState() != ChannelState.INTEREST_OPS) {
            System.out.println("BROADCAST CLIENT handleUpstream:" + e);
         }
         super.handleUpstream(ctx, e);
      }

      @Override
      public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
         //System.out.println("CLIENT channelConnected:" + e);
      }

      @Override
      public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
         String msg = (String) e.getMessage();

         System.out.println("BROADCAST CLIENT messageReceived:" + msg);
         
         // TODO tokenize message and handle accordingly
         // stop broadcast client and start regular client, once we know server ip/port
         // MESSAGESERVER_BROADCAST:HOST:192.168.0.103:8379
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
         System.err.println("BROADCAST CLIENT unexpected exception from downstream:" + e.getCause().getMessage());
         e.getChannel().close();
      }
   }
}
