package com.spokeneasy.app.core.audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class AudioWaveformView extends View {

    private static final int BAR_COUNT = 36;
    private static final float MIN_BAR_HEIGHT = 3f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] amplitudes = new float[BAR_COUNT];
    private int writeIndex = 0;
    private int state = 0; // 0=idle, 1=recording, 2=playing

    public AudioWaveformView(Context context) {
        super(context);
    }

    public AudioWaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AudioWaveformView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAmplitude(float amp) {
        float clamped = Math.min(1f, Math.max(0f, amp));
        amplitudes[writeIndex] = clamped;
        writeIndex = (writeIndex + 1) % BAR_COUNT;
        postInvalidateOnAnimation();
    }

    public void setState(int newState) {
        this.state = newState;
        if (state == 0) {
            for (int i = 0; i < BAR_COUNT; i++) amplitudes[i] = 0;
        }
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float midY = h / 2f;

        if (w <= 0 || h <= 0) return;

        float barWidth = w / (BAR_COUNT * 1.8f);
        float gap = w / (BAR_COUNT * 1.8f * 0.8f);

        switch (state) {
            case 1:
                paint.setColor(0xFF4CAF50);
                break;
            case 2:
                paint.setColor(0xFFFF9800);
                break;
            default:
                paint.setColor(0xFFE0E0E0);
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
