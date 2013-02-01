package com.totsp.embiggen;

import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;

public abstract class BaseFragment extends SherlockFragment {

   protected App app;
   protected ActionBar actionBar;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      ///Log.v(App.TAG, "BaseFragment onCreate() " + this.getClass().getSimpleName());

      if (getActivity() != null) {
         this.app = (App) getActivity().getApplication();
      }

      if (getSherlockActivity() != null) {
         this.actionBar = getSherlockActivity().getSupportActionBar();
      }
   }

   @Override
   public void onResume() {
      super.onResume();
      if (app != null) {
         this.app.bus.register(this);
      }
      ///Log.v(App.TAG, "BaseFragment onResume() " + this.getClass().getSimpleName());
   }

   @Override
   public void onPause() {
      super.onPause();
      if (app != null) {
         this.app.bus.unregister(this);
      }
      ///Log.v(App.TAG, "BaseFragment onPause() " + this.getClass().getSimpleName());
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      ///Log.v(App.TAG, "BaseFragment onDestroy() " + this.getClass().getSimpleName());
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      ///Log.v(App.TAG, "BaseFragment onSaveInstanceState() " + this.getClass().getSimpleName());
   }

   @Override
   public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
      ///Log.v(App.TAG, "BaseFragment onActivityCreated() " + this.getClass().getSimpleName());
   }
}
