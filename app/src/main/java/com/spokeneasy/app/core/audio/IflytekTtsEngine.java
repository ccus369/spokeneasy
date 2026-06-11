package com.spokeneasy.app.core.audio;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

/**
 * iFlytek cloud TTS engine wrapper.
 * Uses SpeechSynthesizer with "catherine" (English female) voice.
 * Exposes the same interface as TTSEngine so callers are interchangeable.
 *
 * Requires network — falls back via TTSEngine to Android TTS when unavailable.
 */
public class IflytekTtsEngine {

    private static final String TAG = "IflytekTtsEngine";
    /** English female voice — clear, natural, suitable for learners */
    private static final String VOICER = "catherine";

    private SpeechSynthesizer mTts;
    private boolean isInitialized = false;
    private TTSEngine.TtsCallback callback;
    /** Guards against double-firing of callback (e.g. createSynthesizer returns null
     *  AND InitListener fires synchronously with error) */
    private boolean initCallbackFired = false;

    /**
     * Initialize iFlytek TTS engine.
     * SpeechSynthesizer.createSynthesizer must be called on the main thread.
     */
    public void init(Context context, TTSEngine.TtsCallback callback) {
        this.callback = callback;
        this.initCallbackFired = false;

        // Always use Application context for SDK calls to avoid leaking
        // Activity/Fragment contexts across configuration changes
        final Context appContext = context.getApplicationContext();

        if (Looper.myLooper() == Looper.getMainLooper()) {
            doInit(appContext);
        } else {
            new Handler(Looper.getMainLooper()).post(() -> doInit(appContext));
        }
    }

    private void doInit(Context context) {
        try {
            mTts = SpeechSynthesizer.createSynthesizer(context, code -> {
                if (initCallbackFired) return;
                initCallbackFired = true;

                if (code == ErrorCode.SUCCESS) {
                    isInitialized = true;
                    try {
                        applySettings();
                    } catch (Exception e) {
                        Log.e(TAG, "applySettings failed", e);
                        fireError("讯飞TTS参数设置失败: " + e.getMessage());
                        return;
                    }
                    Log.d(TAG, "init success, voicer=" + VOICER);
                    fireDone();
                } else {
                    Log.e(TAG, "init failed, code=" + code);
                    fireError("讯飞TTS初始化失败(code=" + code + ")");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "createSynthesizer threw exception", e);
            fireError("讯飞TTS创建异常: " + e.getMessage());
            return;
        }

        if (mTts == null && !initCallbackFired) {
            initCallbackFired = true;
            Log.e(TAG, "createSynthesizer returned null");
            fireError("讯飞TTS创建失败");
        }
    }

    private void fireDone() {
        if (callback != null) {
            callback.onDone();
        }
    }

    private void fireError(String msg) {
        if (callback != null) {
            callback.onError(msg);
        }
    }

    private void applySettings() {
        mTts.setParameter(SpeechConstant.PARAMS, null);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        mTts.setParameter(SpeechConstant.VOICE_NAME, VOICER);
        mTts.setParameter(SpeechConstant.SPEED, "50");
        mTts.setParameter(SpeechConstant.PITCH, "50");
        mTts.setParameter(SpeechConstant.VOLUME, "50");
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
    }

    /**
     * Synthesize and play the given text.
     */
    public void speak(String text) {
        if (!isInitialized || mTts == null) {
            Log.w(TAG, "speak called but not initialized");
            return;
        }

        // Stop any ongoing playback
        mTts.stopSpeaking();

        try {
            int code = mTts.startSpeaking(text, new SynthesizerListener() {
                @Override
                public void onSpeakBegin() {
                    Log.d(TAG, "speak begin");
                }

                @Override
                public void onSpeakPaused() {
                    // no-op
                }

                @Override
                public void onSpeakResumed() {
                    // no-op
                }

                @Override
                public void onBufferProgress(int percent, int beginPos, int endPos,
                                             String info) {
                    // no-op
                }

                @Override
                public void onSpeakProgress(int percent, int beginPos, int endPos) {
                    // no-op
                }

                @Override
                public void onCompleted(SpeechError error) {
                    if (error == null) {
                        Log.d(TAG, "speak completed");
                        fireDone();
                    } else {
                        Log.e(TAG, "speak error: " + error.getPlainDescription(true));
                        fireError("讯飞TTS播放失败: " + error.getPlainDescription(true));
                    }
                }

                @Override
                public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
                    if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                        String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                        Log.d(TAG, "session id=" + sid);
                    }
                }
            });

            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "startSpeaking failed, code=" + code);
                fireError("讯飞TTS合成失败(code=" + code + ")");
            }
        } catch (Exception e) {
            Log.e(TAG, "startSpeaking threw exception", e);
            fireError("讯飞TTS播放异常: " + e.getMessage());
        }
    }

    /** Stop current playback. */
    public void stop() {
        if (mTts != null) {
            mTts.stopSpeaking();
        }
    }

    /** Whether the engine is currently speaking. */
    public boolean isSpeaking() {
        return mTts != null && mTts.isSpeaking();
    }

    /** Whether the engine has been initialized successfully. */
    public boolean isInitialized() {
        return isInitialized;
    }

    /** Human-readable engine description. */
    public String getEngineName() {
        return "iflytek_cloud(" + VOICER + ")";
    }

    /** Release all resources. */
    public void shutdown() {
        stop();
        if (mTts != null) {
            mTts.destroy();
            mTts = null;
            isInitialized = false;
        }
    }
}
