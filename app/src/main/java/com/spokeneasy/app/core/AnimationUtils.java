package com.spokeneasy.app.core;

import android.content.Context;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class AnimationUtils {

    public static void animateCardPress(View card) {
        card.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(100)
                .withEndAction(() -> card.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();
    }

    public static void animateReveal(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    public static void animateScorePulse(View scoreView) {
        scoreView.setScaleX(0.5f);
        scoreView.setScaleY(0.5f);
        scoreView.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> scoreView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();
    }

    public static void animateListItem(View itemView, int delay, Context context) {
        itemView.setAlpha(0f);
        itemView.setTranslationY(dpToPx(context, 30));
        itemView.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(400)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private static int dpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
