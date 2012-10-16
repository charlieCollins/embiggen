package com.totsp.embiggen.host.eventbus;

import com.google.common.eventbus.EventBus;

class ForwardingEventBus extends EventBus {
   final private EventBus bus;

   ForwardingEventBus(EventBus bus) {
      this.bus = bus;
   }

   @Override
   public void register(Object object) {
      bus.register(object);
   }

   @Override
   public void unregister(Object object) {
      bus.unregister(object);
   }

   @Override
   public void post(Object event) {
      bus.post(event);
   }
}
