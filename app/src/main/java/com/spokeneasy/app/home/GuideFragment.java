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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.spokeneasy.app.R;

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
                .build();
        NavHostFragment.findNavController(this)
                .navigate(R.id.learnFragment, null, navOptions);
    }

    public boolean isNavigatingAway() {
        return navigatingAway;
    }
}
