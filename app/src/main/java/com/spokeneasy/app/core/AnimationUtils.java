package com.spokeneasy.app.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.Locale;

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

    /** Animate score number counting from 0 to target, ending with a pulse. */
    public static void animateScoreCount(final android.widget.TextView textView,
                                          final int targetScore,
                                          final String format) {
        animateScoreCount(textView, targetScore, format, null);
    }

    /** Animate score counting with an optional completion callback (after pulse). */
    public static void animateScoreCount(final android.widget.TextView textView,
                                          final int targetScore,
                                          final String format,
                                          final Runnable onComplete) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final int duration = 350;
        final int interval = 25;
        final int steps = duration / interval;
        final int increment = Math.max(1, targetScore / Math.max(1, steps));
        final int[] current = {0};

        final Runnable counter = new Runnable() {
            @Override
            public void run() {
                current[0] = Math.min(current[0] + increment, targetScore);
                textView.setText(String.format(Locale.getDefault(), format, current[0]));
                if (current[0] < targetScore) {
                    handler.postDelayed(this, interval);
                } else {
                    animateScorePulse(textView);
                    if (onComplete != null) {
                        handler.postDelayed(onComplete, 400);
                    }
                }
            }
        };
        handler.post(counter);
    }

    /** One-time pulse feedback on a button press (e.g. TTS play). */
    public static void animateButtonAction(View button) {
        button.animate()
                .scaleX(1.12f)
                .scaleY(1.12f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();
    }

    public static void animateButtonPress(View button) {
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(80)
                .withEndAction(() -> button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
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
