package com.spokeneasy.app.home;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.spokeneasy.app.R;
import com.spokeneasy.app.core.AnimationUtils;

public class GuideFragment extends Fragment {

    private static final String PREFS_NAME = "guide_prefs";
    private static final String KEY_GUIDE_SHOWN = "guide_shown";
    private boolean navigatingAway = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (prefs.getBoolean(KEY_GUIDE_SHOWN, false)) {
            navigatingAway = true;
            view.post(this::navigateToMain);
            return;
        }

        // Staggered entry animation — title, subtitle, 3 features, button (200ms apart)
        View title = view.findViewById(R.id.guide_title);
        View subtitle = view.findViewById(R.id.guide_subtitle);
        View feature1 = view.findViewById(R.id.guide_feature_1);
        View feature2 = view.findViewById(R.id.guide_feature_2);
        View feature3 = view.findViewById(R.id.guide_feature_3);
        View btnStart = view.findViewById(R.id.btn_start);

        View[] elements = {title, subtitle, feature1, feature2, feature3, btnStart};
        for (View el : elements) {
            el.setAlpha(0f);
            el.setScaleX(0.8f);
            el.setScaleY(0.8f);
        }

        for (int i = 0; i < elements.length; i++) {
            final int index = i;
            title.postDelayed(() -> AnimationUtils.animateReveal(elements[index]), index * 200L);
        }

        view.findViewById(R.id.btn_start).setOnClickListener(v -> requestPermission());
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onGuideDone();
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            onGuideDone();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 100) {
            onGuideDone();
        }
    }

    private void onGuideDone() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_GUIDE_SHOWN, true).apply();
        navigateToMain();
    }

    private void navigateToMain() {
        if (!isAdded()) return;
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.guideFragment, true)
                .setEnterAnim(R.anim.scale_fade_in)
                .setExitAnim(R.anim.scale_fade_out)
                .build();
        NavHostFragment.findNavController(this)
                .navigate(R.id.learnFragment, null, navOptions);
    }

    public boolean isNavigatingAway() {
        return navigatingAway;
    }
}
