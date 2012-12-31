package com.totsp.embiggen.host.event;

public class DisplayMediaEvent {

   private final String urlString;

   public DisplayMediaEvent(String urlString) {
      super();
      this.urlString = urlString;
   }

   public String getUrlString() {
      return this.urlString;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("DisplayMediaEvent [urlString=");
      builder.append(this.urlString);
      builder.append("]");
      return builder.toString();
   }
}
