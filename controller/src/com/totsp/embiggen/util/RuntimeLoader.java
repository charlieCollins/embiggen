package com.totsp.embiggen.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

import com.totsp.embiggen.App;
import com.totsp.embiggen.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Load runtime env specific properties from res/raw/runtime_properties.
 * (Allowing for dev/stage/prod versions of the app.)
 * 
 * NOTE: res/raw/runtime_properties is REPLACED at build time by correct env respective file. 
 * (See runtime.properties.dev, runtime.properties.stage, and runtime.properties.prod).
 * 
 * IMPORTANT: EVERY field in runtime_properties must have a corresponding getter here with the
 * correct constant for the key in the properties file used to retrieve the value. (If you add stuff
 * to runtime_properties, you need to add a getter here [annoying, but that's how it works].)
 * 
 * @author ccollins
 * 
 */
public final class RuntimeLoader {

   private Properties props;

   public RuntimeLoader(Context context) {
      props = RuntimeLoader.loadRawProps(context, R.raw.runtime_properties);
      if (props == null) {
         throw new RuntimeException("Error loading properties from resource, cannot continue");
      }
   }

   //
   // Env name (for debug purposes only, to make sure correct props are loaded)
   //
   private static final String ENV = "env";

   public String getEnv() {
      return props.getProperty(ENV);
   }

   

   //
   // Google Analytics
   //
   private static final String GOOGLE_ANALYTICS_ID = "google.analytics.id";

   public String getGoogleAnalyticsId() {
      return props.getProperty(GOOGLE_ANALYTICS_ID);
   }

  
   //
   // priv helpers
   //

   private static Properties loadRawProps(Context context, int resId) {
      Properties props = null;
      InputStream is = null;
      try {
         Resources resources = context.getResources();
         // Check generated R file for name oy your resource
         is = resources.openRawResource(resId);
         props = new Properties();
         props.load(is);
         Log.d(App.TAG, "RuntimeLoader loaded props");
      } catch (NotFoundException e) {
         Log.e(App.TAG, "RuntimeLoader could not find resource id:" + resId);
      } catch (IOException e) {
         Log.e(App.TAG, "RuntimeLoader error", e);
      } finally {
         if (is != null) {
            try {
               is.close();
            } catch (IOException e) {
               // gulp
            }
         }
      }
      return props;
   }

}
