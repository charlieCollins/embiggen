package com.totsp.embiggen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.totsp.android.util.NetworkUtil;
import com.totsp.embiggen.component.crouton.Crouton;
import com.totsp.embiggen.component.crouton.Style;
import com.totsp.server.util.SimpleHttpClient;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;

public class LaunchGalleryFragment extends BaseFragment {

   public static final int PICK_IMAGE = 1;

   private Button launchButton;
   private ImageView selectedThumbnail;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.launch_gallery_fragment, container, false);
      selectedThumbnail = (ImageView) rootView.findViewById(R.id.selected_thumbnail);
      launchButton = (Button) rootView.findViewById(R.id.launch_button);
      launchButton.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            startActivityForResult(new Intent(Intent.ACTION_PICK,
                     android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI), PICK_IMAGE);
         }
      });
      return rootView;
   }

   @Override
   public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
   }

   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent intent) {
      super.onActivityResult(requestCode, resultCode, intent);

      Log.d(App.TAG, "ONACTIVITYRESULT: requestCode:" + requestCode + " resultCode:" + resultCode);

      switch (requestCode) {
         case PICK_IMAGE:
            if (resultCode == Activity.RESULT_OK) {
               Uri selectedImageUri = intent.getData();
               Log.d(App.TAG, "selectedImageUri:" + selectedImageUri);
               new ProcessSelectedItemTask(getActivity()).execute(selectedImageUri);
            }
      }
   }

   private void sendChoiceToHost(String filePath) {
      // replace spaces so that URL end is encoded (don't encode entire thing though)
      if (filePath.contains(" ")) {
         filePath = filePath.replace(" ", "+");
      }

      InetSocketAddress hostAddr = app.getBroadcastClientService().getHostHttpServerInfo();
      if (hostAddr == null) {
         Log.e(App.TAG, "Cannot send message to host, host not known at this time");
         return;
      }

      String localUrl =
               "http://"
                        + NetworkUtil.getWifiIpAddress((WifiManager) getActivity().getSystemService(
                                 Context.WIFI_SERVICE)) + ":" + App.HTTP_SERVER_PORT + filePath;
      try {
         localUrl = URLEncoder.encode(localUrl, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         // ignore?
      }

      final String urlToSendMessageToHost =
               "http://" + hostAddr.getHostName() + ":" + hostAddr.getPort() + "?DISPLAY_MEDIA=" + localUrl;

      Log.d(App.TAG, "sendChoiceToHost filePath:" + filePath);
      Log.d(App.TAG, "sendChoiceToHost invoking URL:" + urlToSendMessageToHost);

      // send DISPLAY_MEDIA message to host via HTTP
      // NOTE gotta use real URLs now, but that's not so bad ;)
      new Thread() {
         public void run() {
            // don't care about response, this is just to send msg to server
            SimpleHttpClient.get(urlToSendMessageToHost);
         }
      }.start();
   }

   //
   // private
   //
   private static final String[] FILE_PATH_COLUMN = { MediaStore.Images.Media.DATA };

   private class ProcessSelectedItemTask extends AsyncTask<Uri, Void, String> {

      private Context context;

      public ProcessSelectedItemTask(Context context) {
         this.context = context;
      }

      @Override
      protected void onPreExecute() {
      }

      @Override
      protected String doInBackground(Uri... args) {
         String filePath = null;
         Cursor cursor = null;
         try {
            cursor = context.getContentResolver().query(args[0], FILE_PATH_COLUMN, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(FILE_PATH_COLUMN[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
         } catch (Exception e) {

         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
         return filePath;
      }

      @Override
      protected void onPostExecute(String result) {
         Log.d(App.TAG, "GetImageFilePathTask returned:" + result);
         if (result != null) {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(result, options);
            int height = options.outHeight;
            int width = options.outWidth;
            //String imageType = options.outMimeType;
            options.inSampleSize = calculateInSampleSize(options, width, height);

            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(result, options);
            selectedThumbnail.setImageBitmap(bitmap);            
            
            Crouton.makeText(getActivity(), "Sent selected item to host", Style.INFO).show();

            sendChoiceToHost(result);
         }
      }
   }

   private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
      // Raw height and width of image
      final int height = options.outHeight;
      final int width = options.outWidth;
      int inSampleSize = 1;

      if (height > reqHeight || width > reqWidth) {
         if (width > height) {
            inSampleSize = Math.round((float) height / (float) reqHeight);
         } else {
            inSampleSize = Math.round((float) width / (float) reqWidth);
         }
      }
      return inSampleSize;
   }
}
