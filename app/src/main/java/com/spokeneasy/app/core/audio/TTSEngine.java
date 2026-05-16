package com.spokeneasy.app.core.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

public class TTSEngine {

    private TextToSpeech tts;
    private boolean isInitialized = false;
    private boolean languageAvailable = true;
    private boolean missingLanguageData = false;
    private File cacheDir;
    private MediaPlayer mediaPlayer;
    private final Queue<String> pendingSpeaks = new LinkedList<>();
    private Handler mainHandler;

    public interface TtsCallback {
        void onDone();
        void onError(String message);
        /** Called when init succeeds but English voice data may need download */
        void onLanguageWarning(String message);
    }

    public void init(Context context, TtsCallback callback) {
        if (tts != null) {
            tts.shutdown();
        }

        cacheDir = new File(context.getCacheDir(), "tts");
        cacheDir.mkdirs();
        mainHandler = new Handler(Looper.getMainLooper());
        languageAvailable = true;
        missingLanguageData = false;

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA) {
                    // TTS engine exists but English voice data not downloaded
                    missingLanguageData = true;
                    languageAvailable = false;
                    tts.setLanguage(Locale.getDefault()); // best-effort
                    tts.setSpeechRate(0.9f);
                    isInitialized = true;
                    processPendingSpeaks();
                    callback.onLanguageWarning("需要下载英语语音数据");
                    callback.onDone();
                    return;
                } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // TTS engine doesn't support English at all
                    languageAvailable = false;
                    tts.setLanguage(Locale.getDefault());
                    tts.setSpeechRate(0.9f);
                    isInitialized = true;
                    processPendingSpeaks();
                    callback.onLanguageWarning("设备不支持英语语音播放");
                    callback.onDone();
                    return;
                }
                tts.setSpeechRate(0.9f);
                isInitialized = true;
                processPendingSpeaks();
                callback.onDone();
            } else {
                callback.onError("TTS initialization failed");
            }
        });
    }

    /** Whether English TTS data is available on this device */
    public boolean isLanguageAvailable() {
        return languageAvailable;
    }

    /** Whether English data exists but needs download (LANG_MISSING_DATA) */
    public boolean isMissingLanguageData() {
        return missingLanguageData;
    }

    public void speak(String text) {
        if (!isInitialized || tts == null) {
            pendingSpeaks.add(text);
            return;
        }

        stopPlayback();

        File cachedFile = getCachedFile(text);
        if (cachedFile.exists()) {
            playCachedFile(cachedFile, text);
            return;
        }

        // Always stream directly for reliability; cache in background after playback
        streamAndCache(text, cachedFile);
    }

    public void stop() {
        stopPlayback();
        if (tts != null) {
            tts.stop();
        }
    }

    public boolean isSpeaking() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) return true;
        return tts != null && tts.isSpeaking();
    }

    public void shutdown() {
        stopPlayback();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isInitialized = false;
        }
    }

    private void streamAndCache(String text, File cacheFile) {
        String utteranceId = UUID.randomUUID().toString();

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {}

            @Override
            public void onDone(String id) {
                // Streaming finished, now cache silently for future plays
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cacheFile.getParentFile().mkdirs();
                    tts.synthesizeToFile(text, null, cacheFile, UUID.randomUUID().toString());
                }
            }

            @Override
            public void onError(String id) {}
        });

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
    }

    private void playCachedFile(File file, String fallbackText) {
        mainHandler.post(() -> {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(file.getAbsolutePath());
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e) {
                mediaPlayer.release();
                mediaPlayer = null;
                Bundle params = new Bundle();
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                        UUID.randomUUID().toString());
                tts.speak(fallbackText, TextToSpeech.QUEUE_FLUSH, params,
                        UUID.randomUUID().toString());
            }
        });
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
    }

    private void processPendingSpeaks() {
        mainHandler.post(() -> {
            while (!pendingSpeaks.isEmpty()) {
                speak(pendingSpeaks.poll());
            }
        });
    }

    private File getCachedFile(String text) {
        return new File(cacheDir, md5(text) + ".wav");
    }

    private String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return String.valueOf(text.hashCode());
        }
    }
}
