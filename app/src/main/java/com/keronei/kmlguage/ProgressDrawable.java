package com.keronei.kmlguage;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

class ProgressDrawable extends Drawable {
    private static final int NUM_RECTS = 82;
    Paint mPaint = new Paint();

    Paint textPaint = new Paint();

    @Override
    protected boolean onLevelChange(int level) {
        invalidateSelf();
        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        int cali = 0;
        textPaint.setColor(0xffbbbbbb);
        textPaint.setTextSize(40);

        int level = getLevel();
        Rect b = getBounds();
        float width = b.width();

        for (int i = 0; i < NUM_RECTS; i++) {

            float left = width * i / NUM_RECTS;
            float right = left + 0.9f * (width / NUM_RECTS);
            mPaint.setColor((i + 1) * 10000 / NUM_RECTS <= level ? 0xFF00FF00 : 0xff888888);
            canvas.drawRect(left, 0, right, b.bottom - 90, mPaint);

            if ((i + 1) % 10 == 0) {
                canvas.drawText(String.valueOf(cali + 1), left - 1f, b.bottom - 56, textPaint);
                cali += 1;

                if (i == 70) {
                    canvas.drawText("x 1000", left - 30f, b.bottom - 20, textPaint);
                }

            }

        }


    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}