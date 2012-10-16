package com.totsp.embiggen.host.eventbus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

final public class BackgroundThreadEventBus extends ForwardingEventBus {

   public abstract static class Event {
   }

   final private static ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
   final private static EventBus BACKGROUND_BUS = new AsyncEventBus("Background Thread", EXECUTOR);

   BackgroundThreadEventBus() {
      super(BACKGROUND_BUS);
   }

   @Override
   public void post(Object event) {
      if (!Event.class.isAssignableFrom(event.getClass())) {
         throw new IllegalArgumentException(getClass().getSimpleName() + " only accepts events descended from "
                  + Event.class.getName());
      }
      super.post(event);
   }

   public void submit(Runnable runnable) {
      EXECUTOR.submit(runnable);
   }
}
