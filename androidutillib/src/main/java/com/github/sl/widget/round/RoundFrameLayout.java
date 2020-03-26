package com.github.sl.widget.round;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * @package: com.mirstone.baselib.widget.round
 * @fileName: RoundFrameLayout
 * @data: 2018/8/14 14:52
 * @author: ShiLiang
 * @describe:
 */
public class RoundFrameLayout extends FrameLayout {
    private BaseRoundView baseFilletView;

    public RoundFrameLayout(Context context) {
        super(context);
        init(null);
    }

    public RoundFrameLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RoundFrameLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        baseFilletView = new BaseRoundView(this, attrs);
    }

    @Override
    public void draw(Canvas canvas) {
        baseFilletView.draw(canvas);
        super.draw(canvas);
        baseFilletView.drawOff(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        baseFilletView.onSizeChanged(w, h);
    }
}
