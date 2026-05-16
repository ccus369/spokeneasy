package com.spokeneasy.app.core.audio;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** One-shot TTS engine check for the settings page. Creates a temporary TTS instance, checks status, then shuts down. */
public class TtsHelper {

    public enum TtsState {
        /** English voice data is ready */
        AVAILABLE,
        /** TTS engine exists but English voice data not downloaded */
        MISSING_DATA,
        /** TTS engine doesn't support English at all */
        NOT_SUPPORTED,
        /** No TTS engine available on device */
        NO_ENGINE
    }

    public interface TtsCheckCallback {
        void onResult(TtsState state, String engineLabel);
    }

    public static void check(Context context, TtsCheckCallback callback) {
        final String[] engineLabel = {""};
        final TextToSpeech[] ttsRef = new TextToSpeech[1];
        ttsRef[0] = new TextToSpeech(context, status -> {
            TextToSpeech tts = ttsRef[0];
            if (tts == null) {
                callback.onResult(TtsState.NO_ENGINE, "");
                return;
            }
            if (status == TextToSpeech.SUCCESS) {
                engineLabel[0] = tts.getDefaultEngine();
                int result = tts.setLanguage(Locale.US);
                tts.shutdown();

                if (result == TextToSpeech.LANG_MISSING_DATA) {
                    callback.onResult(TtsState.MISSING_DATA, engineLabel[0]);
                } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    callback.onResult(TtsState.NOT_SUPPORTED, engineLabel[0]);
                } else {
                    callback.onResult(TtsState.AVAILABLE, engineLabel[0]);
                }
            } else {
                callback.onResult(TtsState.NO_ENGINE, "");
            }
        });
    }
}
