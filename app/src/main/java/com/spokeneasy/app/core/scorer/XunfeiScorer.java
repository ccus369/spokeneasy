package com.spokeneasy.app.core.scorer;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.iflytek.cloud.EvaluatorListener;
import com.iflytek.cloud.EvaluatorResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvaluator;
import com.iflytek.ise.result.Result;
import com.iflytek.ise.result.xml.XmlResultParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class XunfeiScorer implements Scorer {

    private SpeechEvaluator evaluator;
    private boolean isInitialized = false;
    private String lastDetail;
    private Context appContext;

    public interface ScoreCallback {
        void onResult(int score, String detail);
        void onError(String message);
    }

    public void init(Context context) {
        this.appContext = context.getApplicationContext();
        if (evaluator == null) {
            evaluator = SpeechEvaluator.createEvaluator(appContext, null);
        }
        isInitialized = true;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void scoreAsync(String expectedText, String audioFilePath, ScoreCallback callback) {
        if (!isInitialized || evaluator == null) {
            callback.onError("XunfeiScorer not initialized");
            return;
        }

        if (evaluator.isEvaluating()) {
            evaluator.cancel();
        }

        lastDetail = "";

        // Set evaluation parameters for English sentence
        evaluator.setParameter(SpeechConstant.LANGUAGE, "en_us");
        evaluator.setParameter(SpeechConstant.ISE_CATEGORY, "read_sentence");
        evaluator.setParameter("ent", "en_vip");
        evaluator.setParameter(SpeechConstant.SUBJECT, "ise");
        evaluator.setParameter("plev", "0");
        evaluator.setParameter("ise_unite", "1"); // 百分制
        evaluator.setParameter(SpeechConstant.VAD_BOS, "5000");
        evaluator.setParameter(SpeechConstant.VAD_EOS, "3000");
        evaluator.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        evaluator.setParameter(SpeechConstant.RESULT_LEVEL, "complete");
        evaluator.setParameter(SpeechConstant.AUDIO_SOURCE, "-1"); // write audio directly

        // Create listener
        EvaluatorListener listener = new EvaluatorListener() {
            private final StringBuilder resultBuilder = new StringBuilder();

            @Override
            public void onResult(EvaluatorResult result, boolean isLast) {
                if (result != null) {
                    resultBuilder.append(result.getResultString());
                }
                if (isLast) {
                    String xmlResult = resultBuilder.toString();
                    if (!TextUtils.isEmpty(xmlResult)) {
                        parseAndCallback(xmlResult, callback, expectedText);
                    } else {
                        callback.onError("评测结果为空");
                    }
                }
            }

            @Override
            public void onError(SpeechError error) {
                String msg = error != null
                        ? "评测失败: " + error.getErrorCode() + " " + error.getErrorDescription()
                        : "评测失败";
                callback.onError(msg);
            }

            @Override
            public void onBeginOfSpeech() {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onVolumeChanged(int volume, byte[] data) {
            }

            @Override
            public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            }
        };

        // Start evaluation
        int ret = evaluator.startEvaluating(expectedText, null, listener);
        if (ret != 0) {
            callback.onError("启动评测失败，错误码: " + ret);
            return;
        }

        // Feed the audio file to evaluator on a background thread
        final File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            evaluator.cancel();
            callback.onError("录音文件不存在");
            return;
        }

        new Thread(() -> {
            try {
                Thread.sleep(200); // Wait for evaluator to be ready
                byte[] audioData = readFileBytes(audioFile);
                if (audioData.length > 0) {
                    evaluator.writeAudio(audioData, 0, audioData.length);
                }
                evaluator.stopEvaluating();
            } catch (Exception e) {
                callback.onError("写入音频失败: " + e.getMessage());
            }
        }).start();
    }

    private void parseAndCallback(String xmlResult, ScoreCallback callback, String expectedText) {
        try {
            XmlResultParser parser = new XmlResultParser();
            Result result = parser.parse(xmlResult);

            if (result != null) {
                int score = Math.round(result.total_score);

                StringBuilder detail = new StringBuilder();
                if (result.is_rejected) {
                    detail.append("检测到乱读，请认真朗读");
                } else {
                    detail.append("总分: ").append(score).append(" 分");
                    if (result.sentences != null && !result.sentences.isEmpty()) {
                        detail.append("\n").append(result.toString());
                    }
                }

                lastDetail = detail.toString();
                callback.onResult(score, detail.toString());
            } else {
                callback.onError("解析评测结果失败");
            }
        } catch (Exception e) {
            callback.onError("解析结果异常: " + e.getMessage());
        }
    }

    private byte[] readFileBytes(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] data = new byte[(int) file.length()];
            int offset = 0;
            while (offset < data.length) {
                int read = fis.read(data, offset, data.length - offset);
                if (read == -1) break;
                offset += read;
            }
            return data;
        } finally {
            fis.close();
        }
    }

    public void destroy() {
        if (evaluator != null) {
            if (evaluator.isEvaluating()) {
                evaluator.cancel();
            }
            evaluator.destroy();
            evaluator = null;
        }
        isInitialized = false;
    }

    // Legacy synchronous methods (not used by XunfeiScorer, kept for interface compatibility)

    @Override
    @Deprecated
    public int score(String expectedText, String audioFilePath) {
        return 0;
    }

    @Override
    public String getDetail() {
        return lastDetail != null ? lastDetail : "暂无评测结果";
    }
}
