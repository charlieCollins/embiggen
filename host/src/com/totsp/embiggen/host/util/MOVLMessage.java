package com.totsp.embiggen.host.util;

import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.totsp.embiggen.host.eventbus.BackgroundThreadEventBus;

import java.util.Map;

public abstract class MOVLMessage extends BackgroundThreadEventBus.Event {

   private static Map<String,Class<? extends MOVLMessage>> subclasses = Maps.newHashMap();
   
   public static void register(Iterable<Class<? extends MOVLMessage>> msgClasses) {
      for (Class<? extends MOVLMessage> c : msgClasses) {
         String key = c.getSimpleName().trim().toLowerCase();
         subclasses.put(key, c);
         Log.i(MOVLMessage.class.getSimpleName(), "Registered messageId " + key + " to class " + c);
      }
   }
   public static <T extends MOVLMessage> Optional<T> newInstance(String messageId) {
      String key = messageId.trim().toLowerCase();
      Log.i(MOVLMessage.class.getSimpleName(), "Getting message class for messageId " + key);
      if (subclasses.containsKey(key)) {
         Log.i(MOVLMessage.class.getSimpleName(), "Found message class " + subclasses.get(key));
         try {
            return Optional.of((T) subclasses.get(key).newInstance());
         } catch (InstantiationException e) {
            Log.e(MOVLMessage.class.getSimpleName(), "error instantiating message class", e);
         } catch (IllegalAccessException e) {
            Log.e(MOVLMessage.class.getSimpleName(), "error instantiating message class", e);
         }
      }
      Log.i(MOVLMessage.class.getSimpleName(), "No message class returnable");
      return Optional.absent();
   }

   public static <T extends MOVLMessage> Optional<T> newInstance(String sender, String messageId, String messageData, String target) {
      Optional<T> msgOpt = newInstance(messageId);
      for (T msg : msgOpt.asSet()) {
         //msg.setSender(sender);
         msg.setMessageId(messageId);
         //msg.setMessageData(messageData);
         msg.setTarget(target);
      }
      return msgOpt;
   }

   //private IMCUser sender;
   private String messageId;
   //private MCData messageData;
   private String target;


   /*
   public IMCUser getSender() {
      return sender;
   }
   */

   public String getMessageId() {
      return messageId;
   }

   //public MCData getMessageData() {
   //   return messageData;
   //}

   public String getTarget() {
      return target;
   }

   //public void setSender(IMCUser sender) {
   //   this.sender = sender;
   //}

   public void setMessageId(String messageId) {
      this.messageId = messageId;
   }

   //public void setMessageData(MCData messageData) {
   //   this.messageData = messageData;
   //}

   public void setTarget(String target) {
      this.target = target;
   }

}
