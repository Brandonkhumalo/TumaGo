package com.techmania.tumago_driver.helpers;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.transition.AutoTransition;
import androidx.transition.Fade;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import android.view.Gravity;

import com.techmania.tumago_driver.R;

public class AnimHelper {

    /**
     * Smooth auto-transition for any visibility change in a container.
     * Call this BEFORE changing visibility on child views.
     */
    public static void beginTransition(ViewGroup parent) {
        AutoTransition transition = new AutoTransition();
        transition.setDuration(250);
        TransitionManager.beginDelayedTransition(parent, transition);
    }

    /**
     * Fade transition — good for showing/hiding overlays, status messages.
     */
    public static void beginFade(ViewGroup parent) {
        Fade fade = new Fade();
        fade.setDuration(250);
        TransitionManager.beginDelayedTransition(parent, fade);
    }

    /**
     * Slide a view in from a given edge with fade.
     * Call this BEFORE setting the view to VISIBLE.
     */
    public static void beginSlide(ViewGroup parent, int gravity) {
        TransitionSet set = new TransitionSet();
        set.addTransition(new Slide(gravity).setDuration(300));
        set.addTransition(new Fade().setDuration(250));
        set.setOrdering(TransitionSet.ORDERING_TOGETHER);
        TransitionManager.beginDelayedTransition(parent, set);
    }

    /**
     * Show a view with a slide-up animation (good for bottom cards/panels).
     */
    public static void slideUp(View view) {
        if (view.getVisibility() == View.VISIBLE) return;
        Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.slide_up);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(anim);
    }

    /**
     * Hide a view with a slide-down animation.
     */
    public static void slideDown(View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.slide_down);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }
        });
        view.startAnimation(anim);
    }

    /**
     * Show a view with a fade-in animation.
     */
    public static void fadeIn(View view) {
        if (view.getVisibility() == View.VISIBLE) return;
        Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_in);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(anim);
    }

    /**
     * Hide a view with a fade-out animation.
     */
    public static void fadeOut(View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        Animation anim = AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_out);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }
        });
        view.startAnimation(anim);
    }
}
