package com.spokeneasy.app.core.audio;

import android.media.MediaPlayer;
import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;

public class AudioRecorder {

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String currentFilePath;

    public interface AudioCallback {
        void onStart();
        void onStop(String filePath);
        void onError(String message);
    }

    public boolean startRecording(String filePath) {
        this.currentFilePath = filePath;

        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            if (mediaRecorder != null) {
                mediaRecorder.release();
            }

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(filePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                // Recording too short or already released
            }
            mediaRecorder.release();
            mediaRecorder = null;
        }
        return currentFilePath;
    }

    public void playBack(String filePath, AudioCallback callback) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.setOnCompletionListener(mp -> callback.onStop(filePath));
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                callback.onError("Playback error: " + what);
                return true;
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
            callback.onStart();
        } catch (IOException e) {
            callback.onError("Playback failed: " + e.getMessage());
        }
    }

    public void stopPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public boolean isRecording() {
        return mediaRecorder != null;
    }

    public void release() {
        stopRecording();
        stopPlayback();
    }
}
