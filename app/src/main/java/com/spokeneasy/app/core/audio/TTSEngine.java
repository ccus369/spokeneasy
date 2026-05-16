package com.spokeneasy.app.core.audio;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;
import java.util.UUID;

public class TTSEngine {

    private TextToSpeech tts;
    private boolean isInitialized = false;

    public interface TtsCallback {
        void onDone();
        void onError(String message);
    }

    public void init(Context context, TtsCallback callback) {
        if (tts != null) {
            tts.shutdown();
        }

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    callback.onError("TTS language not supported");
                    return;
                }
                tts.setSpeechRate(0.9f);
                isInitialized = true;
                callback.onDone();
            } else {
                callback.onError("TTS initialization failed");
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                callback.onDone();
            }

            @Override
            public void onError(String utteranceId) {
                callback.onError("TTS playback error");
            }
        });
    }

    public void speak(String text) {
        if (!isInitialized || tts == null) return;

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UUID.randomUUID().toString());
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString());
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isInitialized = false;
        }
    }
}
