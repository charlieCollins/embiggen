package com.totsp.embiggen.component.crouton;

import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

final class AnimationsBuilder {

   private static Animation slideInDownAnimation, slideOutUpAnimation;

   private static final class SlideInDownAnimationParameters {
      private SlideInDownAnimationParameters() {
      }

      public static final float FROM_X_DELTA = 0;
      public static final float TO_X_DELTA = 0;
      public static final float FROM_Y_DELTA = -50;
      public static final float TO_Y_DELTA = 0;

      public static final long DURATION = 400;
   }

   private static final class SlideOutUpAnimationParameters {
      private SlideOutUpAnimationParameters() {
      }

      public static final float FROM_X_DELTA = 0;
      public static final float TO_X_DELTA = 0;
      public static final float FROM_Y_DELTA = 0;
      public static final float TO_Y_DELTA = -50;

      public static final long DURATION = 400;
   }

   private AnimationsBuilder() {
   }

   public static Animation buildSlideInDownAnimation() {
      if (AnimationsBuilder.slideInDownAnimation == null) {
         AnimationsBuilder.slideInDownAnimation =
                  new TranslateAnimation(SlideInDownAnimationParameters.FROM_X_DELTA,
                           SlideInDownAnimationParameters.TO_X_DELTA, SlideInDownAnimationParameters.FROM_Y_DELTA,
                           SlideInDownAnimationParameters.TO_Y_DELTA);
         AnimationsBuilder.slideInDownAnimation.setDuration(SlideInDownAnimationParameters.DURATION);
      }

      return AnimationsBuilder.slideInDownAnimation;
   }

   public static Animation buildSlideOutUpAnimation() {
      if (AnimationsBuilder.slideOutUpAnimation == null) {
         AnimationsBuilder.slideOutUpAnimation =
                  new TranslateAnimation(SlideOutUpAnimationParameters.FROM_X_DELTA,
                           SlideOutUpAnimationParameters.TO_X_DELTA, SlideOutUpAnimationParameters.FROM_Y_DELTA,
                           SlideOutUpAnimationParameters.TO_Y_DELTA);
         AnimationsBuilder.slideOutUpAnimation.setDuration(SlideOutUpAnimationParameters.DURATION);
      }
      return AnimationsBuilder.slideOutUpAnimation;
   }
}
