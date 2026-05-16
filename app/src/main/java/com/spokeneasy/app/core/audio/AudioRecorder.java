package com.spokeneasy.app.core.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioRecorder {

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private MediaPlayer mediaPlayer;
    private String currentFilePath;
    private Thread recordingThread;
    private volatile boolean isRecording = false;
    private ByteArrayOutputStream pcmBuffer;
    private volatile int maxAmplitude = 0;

    public interface AudioCallback {
        void onStart();
        void onStop(String filePath);
        void onError(String message);
    }

    public boolean startRecording(String filePath) {
        this.currentFilePath = filePath;

        int bufferSize = Math.max(
                AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
                SAMPLE_RATE * 2 // at least 1 second buffer
        );

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release();
            audioRecord = null;
            return false;
        }

        pcmBuffer = new ByteArrayOutputStream();
        maxAmplitude = 0;
        isRecording = true;
        audioRecord.startRecording();

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    pcmBuffer.write(buffer, 0, read);
                    // Track max amplitude for waveform visualization
                    for (int i = 0; i < read - 1; i += 2) {
                        short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                        int abs = Math.abs(sample);
                        if (abs > maxAmplitude) {
                            maxAmplitude = abs;
                        }
                    }
                }
            }
        }, "AudioRecordThread");
        recordingThread.start();

        return true;
    }

    public String stopRecording() {
        isRecording = false;

        if (recordingThread != null) {
            try {
                recordingThread.join(2000);
            } catch (InterruptedException e) {
                // ignore
            }
            recordingThread = null;
        }

        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
            } catch (IllegalStateException e) {
                // ignore
            }
            audioRecord.release();
            audioRecord = null;
        }

        // Write collected PCM data as WAV file
        if (pcmBuffer != null) {
            byte[] pcmData = pcmBuffer.toByteArray();
            writeWavFile(currentFilePath, pcmData);
            pcmBuffer = null;
        }

        return currentFilePath;
    }

    private void writeWavFile(String path, byte[] pcmData) {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            int channels = 1;
            int bitsPerSample = 16;
            int byteRate = SAMPLE_RATE * channels * bitsPerSample / 8;
            int blockAlign = channels * bitsPerSample / 8;
            int dataSize = pcmData.length;
            int fileSize = 36 + dataSize;

            // RIFF header
            fos.write("RIFF".getBytes());
            fos.write(intToLittleEndian(fileSize));
            fos.write("WAVE".getBytes());

            // fmt sub-chunk
            fos.write("fmt ".getBytes());
            fos.write(intToLittleEndian(16)); // sub-chunk size for PCM
            fos.write(shortToLittleEndian((short) 1)); // audio format 1 = PCM
            fos.write(shortToLittleEndian((short) channels));
            fos.write(intToLittleEndian(SAMPLE_RATE));
            fos.write(intToLittleEndian(byteRate));
            fos.write(shortToLittleEndian((short) blockAlign));
            fos.write(shortToLittleEndian((short) bitsPerSample));

            // data sub-chunk
            fos.write("data".getBytes());
            fos.write(intToLittleEndian(dataSize));
            fos.write(pcmData);
        } catch (IOException e) {
            new File(path).delete();
        }
    }

    private static byte[] intToLittleEndian(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF)
        };
    }

    private static byte[] shortToLittleEndian(short value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF)
        };
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
        return isRecording;
    }

    public int getMaxAmplitude() {
        return maxAmplitude;
    }

    public int getPlaybackPosition() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                return 0;
            }
        }
        return 0;
    }

    public int getPlaybackDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                return 0;
            }
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(position);
            } catch (IllegalStateException e) {
                // ignore
            }
        }
    }

    public void release() {
        stopRecording();
        stopPlayback();
    }
}
