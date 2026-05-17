package com.spokeneasy.app.core.audio;

import android.content.Context;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.List;
import java.util.Locale;

/**
 * TTS engine check for the settings page.
 * Creates a temporary TTS instance, checks status, then shuts down.
 * On Chinese phones (Xiaomi, Huawei, etc.) the default engine may not be
 * set, so we enumerate available engines and try each one.
 */
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
        // First try: default engine
        tryEngine(context, null, (state, label) -> {
            Log.d("TtsHelper", "check: tryEngine result=" + state + " label=" + label);
            if (state != TtsState.NO_ENGINE) {
                callback.onResult(state, label);
                return;
            }
            // Default engine failed — enumerate available engines
            TextToSpeech enumTts = new TextToSpeech(context, status -> { });
            List<TextToSpeech.EngineInfo> engines = enumTts.getEngines();
            enumTts.shutdown();

            Log.d("TtsHelper", "check: engines=" + (engines != null ? engines.size() : "null"));

            if (engines != null && !engines.isEmpty()) {
                // Build a label of available engines for display
                StringBuilder sb = new StringBuilder("已安装引擎: ");
                for (int i = 0; i < engines.size(); i++) {
                    if (i > 0) sb.append(", ");
                    TextToSpeech.EngineInfo ei = engines.get(i);
                    sb.append(ei.label).append("(").append(ei.name).append(")");
                }
                final String engineList = sb.toString();
                Log.d("TtsHelper", "check: engineList=" + engineList);

                // Try each installed engine
                tryEachEngine(context, engines, 0, callback, engineList);
            } else {
                // No engines enumerated via API — try reading system default from Settings
                Log.d("TtsHelper", "check: engines empty, trying system default engine");
                trySystemDefaultEngine(context, callback);
            }
        });
    }

    /**
     * Try to read the system default TTS engine from Settings.Secure and use it.
     * On Chinese OEM ROMs (OPPO, Xiaomi, Huawei), getEngines() often returns empty,
     * but the system default engine is still available.
     */
    private static void trySystemDefaultEngine(Context context,
                                                TtsCheckCallback callback) {
        final String[] engineRef = {""};
        try {
            String val = Settings.Secure.getString(
                    context.getContentResolver(), "tts_default_synth");
            if (val != null) engineRef[0] = val;
            Log.d("TtsHelper", "trySystemDefaultEngine: tts_default_synth=" + engineRef[0]);
        } catch (Exception e) {
            Log.d("TtsHelper", "trySystemDefaultEngine: error reading settings", e);
        }

        final String defaultEngine = engineRef[0];
        if (!defaultEngine.isEmpty()) {
            tryEngine(context, defaultEngine, (state, label) -> {
                Log.d("TtsHelper", "trySystemDefaultEngine result: state=" + state + " engine=" + defaultEngine);
                if (state != TtsState.NO_ENGINE) {
                    callback.onResult(state, label != null && !label.isEmpty() ? label : defaultEngine);
                } else {
                    // Also failed — try known OEM engines as last resort
                    tryKnownOemEngines(context, 0, callback);
                }
            });
        } else {
            tryKnownOemEngines(context, 0, callback);
        }
    }

    /** Fallback list of known Chinese OEM TTS engine packages */
    private static final String[] KNOWN_OEM_ENGINES = {
        "com.oplus.ttsaccessibilityengine",  // OPPO / OnePlus
        "com.oppo.tts",                      // OPPO (older)
        "com.xiaomi.tts",                    // Xiaomi
        "com.huawei.tts",                    // Huawei
        "com.vivo.tts",                      // Vivo
    };

    private static void tryKnownOemEngines(Context context, int index,
                                            TtsCheckCallback callback) {
        if (index >= KNOWN_OEM_ENGINES.length) {
            callback.onResult(TtsState.NO_ENGINE, "");
            return;
        }

        final String engineName = KNOWN_OEM_ENGINES[index];
        Log.d("TtsHelper", "tryKnownOemEngines[" + index + "]: " + engineName);

        tryEngine(context, engineName, (state, label) -> {
            Log.d("TtsHelper", "tryKnownOemEngines[" + engineName + "] result=" + state);
            if (state != TtsState.NO_ENGINE) {
                callback.onResult(state, engineName);
            } else {
                tryKnownOemEngines(context, index + 1, callback);
            }
        });
    }

    private static void tryEngine(Context context, String engineName,
                                   TtsCheckCallback callback) {
        final TextToSpeech[] ttsRef = new TextToSpeech[1];
        final boolean[] listenerFired = {false};
        final int[] initStatus = {TextToSpeech.ERROR};

        TextToSpeech.OnInitListener listener = status -> {
            listenerFired[0] = true;
            initStatus[0] = status;
            TextToSpeech tts = ttsRef[0];
            Log.d("TtsHelper", "tryEngine listener fired: status=" + status
                    + " tts=" + tts + " engine=" + engineName);
            // On some devices (Xiaomi, Huawei) the listener fires synchronously
            // from within the TextToSpeech constructor, before ttsRef[0] is assigned.
            // In that case we return and handle it after the constructor completes.
            if (tts == null) {
                Log.d("TtsHelper", "tryEngine: tts is null (sync init), will handle post-constructor");
                return;
            }
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                tts.shutdown();
                Log.d("TtsHelper", "tryEngine: lang check result=" + result);
                reportResult(result, engineName, callback);
            } else {
                tts.shutdown();
                callback.onResult(TtsState.NO_ENGINE, engineName != null ? engineName : "");
            }
        };

        if (engineName != null) {
            ttsRef[0] = new TextToSpeech(context, listener, engineName);
        } else {
            ttsRef[0] = new TextToSpeech(context, listener);
        }

        Log.d("TtsHelper", "tryEngine post-constructor: listenerFired=" + listenerFired[0]
                + " ttsRef=" + ttsRef[0] + " initStatus=" + initStatus[0]);

        // Handle synchronous listener case (listener fired during constructor,
        // but ttsRef[0] was null at that time)
        if (listenerFired[0] && ttsRef[0] != null) {
            Log.d("TtsHelper", "tryEngine: handling sync init, status=" + initStatus[0]);
            if (initStatus[0] == TextToSpeech.SUCCESS) {
                int result = ttsRef[0].setLanguage(Locale.US);
                ttsRef[0].shutdown();
                Log.d("TtsHelper", "tryEngine sync: lang check result=" + result);
                reportResult(result, engineName, callback);
            } else {
                ttsRef[0].shutdown();
                callback.onResult(TtsState.NO_ENGINE, engineName != null ? engineName : "");
            }
        } else if (!listenerFired[0]) {
            Log.d("TtsHelper", "tryEngine: waiting for async listener...");
        }
    }

    private static void reportResult(int langResult, String engineName,
                                      TtsCheckCallback callback) {
        String ename = engineName != null ? engineName : "";
        Log.d("TtsHelper", "reportResult: langResult=" + langResult + " engine=" + ename);
        if (langResult == TextToSpeech.LANG_MISSING_DATA) {
            callback.onResult(TtsState.MISSING_DATA, ename);
        } else if (langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            callback.onResult(TtsState.NOT_SUPPORTED, ename);
        } else {
            callback.onResult(TtsState.AVAILABLE, ename);
        }
    }

    private static void tryEachEngine(Context context,
                                       List<TextToSpeech.EngineInfo> engines,
                                       int index,
                                       TtsCheckCallback callback,
                                       String engineList) {
        if (index >= engines.size()) {
            // All engines tried and none worked
            callback.onResult(TtsState.NO_ENGINE, engineList);
            return;
        }

        TextToSpeech.EngineInfo ei = engines.get(index);
        final String name = ei.name;

        final TextToSpeech[] ttsRef = new TextToSpeech[1];
        final boolean[] listenerFired = {false};
        final int[] initStatus = {TextToSpeech.ERROR};

        TextToSpeech.OnInitListener listener = status -> {
            listenerFired[0] = true;
            initStatus[0] = status;
            TextToSpeech tts = ttsRef[0];
            Log.d("TtsHelper", "tryEachEngine[" + name + "] listener: status=" + status + " tts=" + tts);
            if (tts == null) return; // handled post-constructor
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                tts.shutdown();
                Log.d("TtsHelper", "tryEachEngine[" + name + "] lang=" + result);
                reportResult(result, name, callback);
            } else {
                tts.shutdown();
                tryEachEngine(context, engines, index + 1, callback, engineList);
            }
        };

        ttsRef[0] = new TextToSpeech(context, listener, name);
        Log.d("TtsHelper", "tryEachEngine[" + name + "] post-ctor: fired=" + listenerFired[0]
                + " ttsRef=" + ttsRef[0] + " status=" + initStatus[0]);

        // Handle synchronous listener case
        if (listenerFired[0] && ttsRef[0] != null) {
            Log.d("TtsHelper", "tryEachEngine[" + name + "] handling sync init");
            if (initStatus[0] == TextToSpeech.SUCCESS) {
                int result = ttsRef[0].setLanguage(Locale.US);
                ttsRef[0].shutdown();
                Log.d("TtsHelper", "tryEachEngine[" + name + "] sync lang=" + result);
                reportResult(result, name, callback);
            } else {
                ttsRef[0].shutdown();
                tryEachEngine(context, engines, index + 1, callback, engineList);
            }
        }
    }
}
