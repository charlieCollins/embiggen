package com.totsp.embiggen.host.eventbus;

import java.util.concurrent.Executor;

import android.os.Handler;

import com.google.common.eventbus.AsyncEventBus;

final public class MainThreadEventBus extends ForwardingEventBus {

   public static abstract class Event {
   }

   public MainThreadEventBus() {
      super(new AsyncEventBus("Main Thread", new Executor() {
         final Handler handler = new Handler();

         @Override
         public void execute(Runnable runnable) {
            handler.post(runnable);
         }
      }));
   }

   @Override
   public void register(Object object) {
      super.register(object); 
   }

   @Override
   public void unregister(Object object) {
      super.unregister(object); 
   }

   @Override
   public void post(Object event) {
      if (!Event.class.isAssignableFrom(event.getClass())) {
         throw new IllegalArgumentException(getClass().getSimpleName() + " only accepts events descended from "
                  + Event.class.getName());
      }
      super.post(event);
   }
}
