package com.totsp.embiggen.host.eventbus;

final public class EventBusStation {
   private EventBusStation() {}

   /**
    * Get an event bus which dispatches on a background thread.
    */
   public static BackgroundThreadEventBus backgroundThread() {
      return new BackgroundThreadEventBus();
   }

   /**
    * Get an event bus which dispatches on the main thread.
    * May only be called on the main thread.
    */
   public static MainThreadEventBus mainThread() {
      return new MainThreadEventBus();
   }

}
