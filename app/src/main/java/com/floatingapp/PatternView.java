package com.floatingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PatternView extends View {

    public interface OnPatternCompleteListener {
        void onComplete(String pattern);
    }

    private static final int COLS = 3;
    private static final int ROWS = 3;
    private static final int TOTAL = COLS * ROWS;

    private final float[] dotX = new float[TOTAL];
    private final float[] dotY = new float[TOTAL];
    private final boolean[] selected = new boolean[TOTAL];
    private final List<Integer> sequence = new ArrayList<>();

    private float touchX = -1, touchY = -1;
    private boolean isDrawing = false;

    private final Paint dotPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint errorPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean showError = false;
    private OnPatternCompleteListener listener;

    public PatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public PatternView(Context context) {
        super(context);
        init();
    }

    private void init() {
        dotPaint.setColor(Color.parseColor("#CBD5E1"));
        dotPaint.setStyle(Paint.Style.FILL);

        activePaint.setColor(Color.parseColor("#4F46E5"));
        activePaint.setStyle(Paint.Style.FILL);

        linePaint.setColor(Color.parseColor("#4F46E5"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        errorPaint.setColor(Color.parseColor("#EF4444"));
        errorPaint.setStyle(Paint.Style.FILL);
    }

    public void setOnPatternCompleteListener(OnPatternCompleteListener l) {
        this.listener = l;
    }

    public void clearPattern() {
        for (int i = 0; i < TOTAL; i++) selected[i] = false;
        sequence.clear();
        touchX = touchY = -1;
        isDrawing = false;
        showError = false;
        invalidate();
    }

    public void showErrorState() {
        showError = true;
        invalidate();
        postDelayed(() -> {
            showError = false;
            clearPattern();
        }, 800);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        float cellW = w / (float) COLS;
        float cellH = h / (float) ROWS;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = row * COLS + col;
                dotX[idx] = cellW * col + cellW / 2f;
                dotY[idx] = cellH * row + cellH / 2f;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint lp = showError ? errorPaint : linePaint;
        // Draw lines between selected dots
        for (int i = 0; i < sequence.size() - 1; i++) {
            int from = sequence.get(i);
            int to   = sequence.get(i + 1);
            lp.setColor(showError ? Color.parseColor("#EF4444") : Color.parseColor("#4F46E5"));
            canvas.drawLine(dotX[from], dotY[from], dotX[to], dotY[to], lp);
        }
        // Line from last dot to finger
        if (isDrawing && sequence.size() > 0 && touchX >= 0) {
            int last = sequence.get(sequence.size() - 1);
            lp.setColor(showError ? Color.parseColor("#EF4444") : Color.parseColor("#A5B4FC"));
            canvas.drawLine(dotX[last], dotY[last], touchX, touchY, lp);
        }
        // Draw dots
        float dotRadius = Math.min(getWidth(), getHeight()) / (COLS * 3.5f);
        float ringRadius = dotRadius * 1.8f;
        for (int i = 0; i < TOTAL; i++) {
            Paint p = showError
                ? errorPaint
                : (selected[i] ? activePaint : dotPaint);
            if (selected[i] && !showError) {
                Paint ring = new Paint(activePaint);
                ring.setAlpha(50);
                ring.setStyle(Paint.Style.FILL);
                canvas.drawCircle(dotX[i], dotY[i], ringRadius, ring);
            }
            canvas.drawCircle(dotX[i], dotY[i], dotRadius, p);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (showError) return true;
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clearPattern();
                isDrawing = true;
                checkDotHit(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touchX = x; touchY = y;
                checkDotHit(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                isDrawing = false;
                touchX = touchY = -1;
                if (sequence.size() >= 1 && listener != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < sequence.size(); i++) {
                        if (i > 0) sb.append("-");
                        sb.append(sequence.get(i));
                    }
                    listener.onComplete(sb.toString());
                }
                invalidate();
                break;
        }
        return true;
    }

    private void checkDotHit(float x, float y) {
        float threshold = Math.min(getWidth(), getHeight()) / (COLS * 2.5f);
        for (int i = 0; i < TOTAL; i++) {
            if (selected[i]) continue;
            float dx = x - dotX[i];
            float dy = y - dotY[i];
            if (Math.sqrt(dx * dx + dy * dy) < threshold) {
                selected[i] = true;
                sequence.add(i);
                invalidate();
                break;
            }
        }
    }
}
