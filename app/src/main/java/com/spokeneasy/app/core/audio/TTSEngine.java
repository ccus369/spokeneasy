package com.spokeneasy.app.core.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

public class TTSEngine {

    private TextToSpeech tts;
    private boolean isInitialized = false;
    private boolean languageAvailable = true;
    private boolean missingLanguageData = false;
    private String engineName = "";
    private File cacheDir;
    private MediaPlayer mediaPlayer;
    private final Queue<String> pendingSpeaks = new LinkedList<>();
    private Handler mainHandler;
    private TtsCallback callback;

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

        // Phase 1: try default engine
        initEngine(context, null, callback);
    }

    /** Try initializing with a specific engine (null = default).
     *  If it fails and there are other engines available, try them. */
    private void initEngine(Context context, String engineName, TtsCallback callback) {
        final TextToSpeech[] ttsRef = new TextToSpeech[1];
        final boolean[] listenerFired = {false};
        final int[] initStatus = {TextToSpeech.ERROR};

        TextToSpeech.OnInitListener listener = status -> {
            listenerFired[0] = true;
            initStatus[0] = status;
            TextToSpeech tts = ttsRef[0];
            // On some devices (Xiaomi, Huawei) the listener fires synchronously
            // from within the TextToSpeech constructor, before ttsRef[0] is assigned.
            if (tts == null) return; // handled post-constructor
            if (status == TextToSpeech.SUCCESS) {
                onTtsReady(tts, engineName, callback);
            } else {
                // Default engine failed — try other installed engines
                tts.shutdown();
                tryFallbackEngines(context, engineName, callback);
            }
        };

        if (engineName != null) {
            ttsRef[0] = new TextToSpeech(context, listener, engineName);
        } else {
            ttsRef[0] = new TextToSpeech(context, listener);
        }

        // Handle synchronous listener case
        if (listenerFired[0] && ttsRef[0] != null) {
            if (initStatus[0] == TextToSpeech.SUCCESS) {
                onTtsReady(ttsRef[0], engineName, callback);
            } else {
                ttsRef[0].shutdown();
                tryFallbackEngines(context, engineName, callback);
            }
        }
    }

    private void onTtsReady(TextToSpeech tts, String engineName, TtsCallback callback) {
        this.tts = tts;
        this.callback = callback;
        this.engineName = engineName != null ? engineName : tts.getDefaultEngine();
        if (this.engineName == null) this.engineName = "unknown";

        int result = tts.setLanguage(Locale.US);
        if (result == TextToSpeech.LANG_MISSING_DATA) {
            missingLanguageData = true;
            languageAvailable = false;
            tts.setLanguage(Locale.US);
            tts.setSpeechRate(0.9f);
            isInitialized = true;
            processPendingSpeaks();
            callback.onLanguageWarning("引擎 " + this.engineName + " 可能需要下载英语数据");
            callback.onDone();
        } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
            languageAvailable = false;
            tts.setLanguage(Locale.US);
            tts.setSpeechRate(0.9f);
            isInitialized = true;
            processPendingSpeaks();
            callback.onLanguageWarning("引擎 " + this.engineName + " 可能不支持英语，尝试中");
            callback.onDone();
        } else {
            tts.setSpeechRate(0.9f);
            isInitialized = true;
            processPendingSpeaks();
            callback.onDone();
        }
    }

    private void tryFallbackEngines(Context context, String failedEngine, TtsCallback callback) {
        TextToSpeech[] enumTtsRef = new TextToSpeech[1];
        final boolean[] listenerFired = {false};

        enumTtsRef[0] = new TextToSpeech(context, status -> {
            listenerFired[0] = true;
            TextToSpeech enumTts = enumTtsRef[0];
            // Guard against synchronous listener firing before assignment
            if (enumTts == null) return;
            processEngineListAndTryFallback(enumTts, context, failedEngine, callback);
        });

        if (listenerFired[0] && enumTtsRef[0] != null) {
            processEngineListAndTryFallback(enumTtsRef[0], context, failedEngine, callback);
        }
    }

    private void processEngineListAndTryFallback(TextToSpeech enumTts, Context context,
                                                  String failedEngine, TtsCallback callback) {
        List<TextToSpeech.EngineInfo> engines = enumTts.getEngines();
        enumTts.shutdown();

        if (engines == null || engines.isEmpty()) {
            // No engines enumerated — try system default engine from Settings
            trySystemDefaultEngine(context, callback);
            return;
        }

        // Store the label list for error messages
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < engines.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(engines.get(i).label);
        }
        final String engineList = sb.toString();

        // Try each engine, skip the one that already failed
        tryEngineFromList(context, engines, 0, failedEngine, callback, engineList);
    }

    /** Known Chinese OEM TTS engine packages used as fallback */
    private static final String[] KNOWN_OEM_ENGINES = {
        "com.oplus.ttsaccessibilityengine",  // OPPO / OnePlus
        "com.oppo.tts",                      // OPPO (older)
        "com.xiaomi.tts",                    // Xiaomi
        "com.huawei.tts",                    // Huawei
        "com.vivo.tts",                      // Vivo
    };

    /**
     * Try to read the system default TTS engine from Settings.Secure
     * and initialize with it. Fallback for Chinese OEM ROMs where
     * getEngines() returns empty.
     */
    private void trySystemDefaultEngine(Context context, TtsCallback callback) {
        String defaultEngine = null;
        try {
            defaultEngine = Settings.Secure.getString(
                    context.getContentResolver(), "tts_default_synth");
            Log.d("TTSEngine", "trySystemDefaultEngine: tts_default_synth=" + defaultEngine);
        } catch (Exception e) {
            Log.d("TTSEngine", "trySystemDefaultEngine: error reading settings", e);
        }

        if (defaultEngine != null && !defaultEngine.isEmpty()) {
            tryEngineDirect(context, defaultEngine, callback, true);
        } else {
            tryKnownOemEngines(context, 0, callback);
        }
    }

    private void tryKnownOemEngines(Context context, int index, TtsCallback callback) {
        if (index >= KNOWN_OEM_ENGINES.length) {
            callback.onError("All TTS engines failed");
            return;
        }

        final String engineName = KNOWN_OEM_ENGINES[index];
        Log.d("TTSEngine", "tryKnownOemEngines[" + index + "]: " + engineName);

        tryEngineDirect(context, engineName, new TtsCallback() {
            @Override public void onDone() { callback.onDone(); }
            @Override public void onError(String msg) {
                Log.d("TTSEngine", "tryKnownOemEngines[" + engineName + "] failed: " + msg);
                tryKnownOemEngines(context, index + 1, callback);
            }
            @Override public void onLanguageWarning(String msg) { callback.onLanguageWarning(msg); }
        }, false);
    }

    /**
     * Try a specific engine by name without triggering fallback enumeration.
     * Used by trySystemDefaultEngine and tryKnownOemEngines to avoid recursion.
     */
    private void tryEngineDirect(Context context, String engineName,
                                  TtsCallback callback, boolean tryOemFallback) {
        final TextToSpeech[] ttsRef = new TextToSpeech[1];
        final boolean[] listenerFired = {false};
        final int[] initStatus = {TextToSpeech.ERROR};

        TextToSpeech.OnInitListener listener = status -> {
            listenerFired[0] = true;
            initStatus[0] = status;
            TextToSpeech tts = ttsRef[0];
            if (tts == null) return;
            if (status == TextToSpeech.SUCCESS) {
                onTtsReady(tts, engineName, callback);
            } else {
                tts.shutdown();
                if (tryOemFallback) {
                    tryKnownOemEngines(context, 0, callback);
                } else {
                    callback.onError("Engine " + engineName + " failed");
                }
            }
        };

        ttsRef[0] = new TextToSpeech(context, listener, engineName);

        if (listenerFired[0] && ttsRef[0] != null) {
            if (initStatus[0] == TextToSpeech.SUCCESS) {
                onTtsReady(ttsRef[0], engineName, callback);
            } else {
                ttsRef[0].shutdown();
                if (tryOemFallback) {
                    tryKnownOemEngines(context, 0, callback);
                } else {
                    callback.onError("Engine " + engineName + " failed");
                }
            }
        }
    }

    private void tryEngineFromList(Context context,
                                    List<TextToSpeech.EngineInfo> engines,
                                    int index,
                                    String failedEngine,
                                    TtsCallback callback,
                                    String engineList) {
        if (index >= engines.size()) {
            callback.onError("All TTS engines failed: " + engineList);
            return;
        }

        TextToSpeech.EngineInfo ei = engines.get(index);
        if (ei.name.equals(failedEngine)) {
            tryEngineFromList(context, engines, index + 1, failedEngine, callback, engineList);
            return;
        }

        final TextToSpeech[] ttsRef = new TextToSpeech[1];
        final boolean[] listenerFired = {false};
        final int[] initStatus = {TextToSpeech.ERROR};

        ttsRef[0] = new TextToSpeech(context, status -> {
            listenerFired[0] = true;
            initStatus[0] = status;
            TextToSpeech tts = ttsRef[0];
            // Guard against synchronous listener firing before assignment
            if (tts == null) return; // handled post-constructor
            if (status == TextToSpeech.SUCCESS) {
                onTtsReady(tts, ei.name, callback);
            } else {
                tts.shutdown();
                tryEngineFromList(context, engines, index + 1, failedEngine, callback, engineList);
            }
        }, ei.name);

        // Handle synchronous listener case
        if (listenerFired[0] && ttsRef[0] != null) {
            if (initStatus[0] == TextToSpeech.SUCCESS) {
                onTtsReady(ttsRef[0], ei.name, callback);
            } else {
                ttsRef[0].shutdown();
                tryEngineFromList(context, engines, index + 1, failedEngine, callback, engineList);
            }
        }
    }

    /** Whether English TTS data is available on this device */
    public boolean isLanguageAvailable() {
        return languageAvailable;
    }

    /** Whether English data exists but needs download (LANG_MISSING_DATA) */
    public boolean isMissingLanguageData() {
        return missingLanguageData;
    }

    /** Returns the detected TTS engine name, e.g. \"com.iflytek.tts\" */
    public String getEngineName() {
        return engineName;
    }

    public boolean isInitialized() {
        return isInitialized;
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
                // Notify caller that speech is done (e.g. to play next sentence)
                if (callback != null) {
                    mainHandler.post(() -> callback.onDone());
                }
                // Streaming finished, now cache silently for future plays
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cacheFile.getParentFile().mkdirs();
                    tts.synthesizeToFile(text, null, cacheFile, UUID.randomUUID().toString());
                }
            }

            @Override
            public void onError(String id) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("TTS utterance error"));
                }
            }
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
