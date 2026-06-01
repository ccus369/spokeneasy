package com.spokeneasy.app.core.audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.spokeneasy.app.R;

public class AudioWaveformView extends View {

    public static final int STATE_IDLE = 0;
    public static final int STATE_RECORDING = 1;
    public static final int STATE_PLAYING = 2;
    public static final int STATE_SUCCESS = 3;

    private static final int BAR_COUNT = 36;
    private static final float MIN_BAR_HEIGHT = 3f;
    private static final long SUCCESS_DURATION_MS = 800L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint successPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path checkPath = new Path();
    private final float[] amplitudes = new float[BAR_COUNT];
    private int writeIndex = 0;
    private int state = STATE_IDLE;
    private final Runnable resetRunnable = () -> setState(STATE_IDLE);

    public AudioWaveformView(Context context) {
        super(context);
        init();
    }

    public AudioWaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioWaveformView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        successPaint.setColor(ContextCompat.getColor(getContext(), R.color.score_excellent));
        successPaint.setStyle(Paint.Style.STROKE);
        successPaint.setStrokeCap(Paint.Cap.ROUND);
        successPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setAmplitude(float amp) {
        float clamped = Math.min(1f, Math.max(0f, amp));
        amplitudes[writeIndex] = clamped;
        writeIndex = (writeIndex + 1) % BAR_COUNT;
        postInvalidateOnAnimation();
    }

    public void setState(int newState) {
        removeCallbacks(resetRunnable);
        this.state = newState;
        if (state == STATE_IDLE) {
            for (int i = 0; i < BAR_COUNT; i++) amplitudes[i] = 0;
        }
        postInvalidateOnAnimation();
    }

    /** Show a green checkmark for ~800ms, then return to idle. */
    public void showSuccess() {
        removeCallbacks(resetRunnable);
        state = STATE_SUCCESS;
        postInvalidateOnAnimation();
        postDelayed(resetRunnable, SUCCESS_DURATION_MS);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float midY = h / 2f;

        if (w <= 0 || h <= 0) return;

        // Success state — draw a green checkmark instead of bars
        if (state == STATE_SUCCESS) {
            float strokeWidth = Math.min(w, h) * 0.18f;
            successPaint.setStrokeWidth(strokeWidth);
            checkPath.reset();
            checkPath.moveTo(w * 0.30f, h * 0.55f);
            checkPath.lineTo(w * 0.45f, h * 0.72f);
            checkPath.lineTo(w * 0.70f, h * 0.32f);
            canvas.drawPath(checkPath, successPaint);
            return;
        }

        float barWidth = w / (BAR_COUNT * 1.8f);
        float gap = w / (BAR_COUNT * 1.8f * 0.8f);

        switch (state) {
            case 1:
                paint.setColor(ContextCompat.getColor(getContext(), R.color.waveform_recording));
                break;
            case 2:
                paint.setColor(ContextCompat.getColor(getContext(), R.color.waveform_playing));
                break;
            default:
                paint.setColor(ContextCompat.getColor(getContext(), R.color.waveform_idle));
                break;
        }

        float totalBar = barWidth * BAR_COUNT + gap * (BAR_COUNT - 1);
        float startX = (w - totalBar) / 2f;

        for (int i = 0; i < BAR_COUNT; i++) {
            int idx = (writeIndex + i) % BAR_COUNT;
            float barHeight = Math.max(MIN_BAR_HEIGHT, amplitudes[idx] * h * 0.85f);
            float x = startX + i * (barWidth + gap);
            canvas.drawRect(x, midY - barHeight / 2f, x + barWidth, midY + barHeight / 2f, paint);
        }
    }
}
